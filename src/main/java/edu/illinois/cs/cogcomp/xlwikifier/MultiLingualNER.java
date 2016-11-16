package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.BrownClusters;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.ExpressiveFeaturesAnnotator;
import edu.illinois.cs.cogcomp.ner.ExpressiveFeatures.Gazetteers;
import edu.illinois.cs.cogcomp.ner.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel1;
import edu.illinois.cs.cogcomp.ner.LbjFeatures.NETaggerLevel2;
import edu.illinois.cs.cogcomp.ner.LbjTagger.Data;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NERDocument;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.ner.LbjTagger.ParametersForLbjCode;
import edu.illinois.cs.cogcomp.ner.ParsingProcessingData.TaggedDataReader;
import edu.illinois.cs.cogcomp.ner.config.NerBaseConfigurator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.Language;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.mlner.NERUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static edu.illinois.cs.cogcomp.ner.LbjTagger.Parameters.readAndLoadConfig;

/**
 * Generate NER annotations using the Annotator API.
 * The input TextAnnotation has to be tokenized.
 *
 * Created by ctsai12 on 10/24/16.
 */
public class MultiLingualNER extends Annotator {


    private final Logger logger = LoggerFactory.getLogger(MultiLingualNER.class);

    private Language language;
    private NERUtils nerutils;
    private ParametersForLbjCode parameters;
    private NETaggerLevel1 taggerLevel1;
    private NETaggerLevel2 taggerLevel2;
    private BrownClusters brownclusters;
    private Gazetteers gazetteers;


    /**
     * Language has to be set before initialization.
     *
     * @param lang       the target language
     * @param configFile a global configuration for entire cross-lingual wikification. It contains paths to the
     *                   NER models of each language
     * @throws IOException
     */
    public MultiLingualNER(Language lang, String configFile) throws IOException {
        super(lang.getNERViewName(), new String[]{}, true, new ResourceManager(configFile));

        this.language = lang;

        // set all config properties of cross-lingual wikifier
        ConfigParameters.setPropValues(configFile);

        doInitialize();
    }

    @Override
    public void initialize(ResourceManager rm) {

        logger.info("Initializing MultiLingualNER...");

        String lang = this.language.getShortName();

        nerutils = new NERUtils(lang);

        try {

            logger.info("loading NER config from " + ConfigParameters.ner_models.get(lang));
            ResourceManager ner_rm = new ResourceManager(ConfigParameters.ner_models.get(lang));
            NerBaseConfigurator baseConfigurator = new NerBaseConfigurator();

            // Save the parameters and brown clusters for this language. These resources are language specific.
            this.parameters = readAndLoadConfig(baseConfigurator.getConfig(ner_rm), false);
            this.brownclusters = BrownClusters.get();
//            this.gazetteers = GazetteersFactory.get();

        } catch (IOException e) {
            e.printStackTrace();
        }
        taggerLevel1 = new NETaggerLevel1(this.parameters.pathToModelFile + ".level1", this.parameters.pathToModelFile + ".level1.lex");
        taggerLevel2 = new NETaggerLevel2(this.parameters.pathToModelFile + ".level2", this.parameters.pathToModelFile + ".level2.lex");

    }



    @Override
    public void addView(TextAnnotation textAnnotation) {

        QueryDocument doc = new QueryDocument("");
        doc.text = textAnnotation.getText();
        doc.setTextAnnotation(textAnnotation);

        annotate(doc);

        SpanLabelView nerView = new SpanLabelView(getViewName(), textAnnotation);

        Set<String> seen = new HashSet<>();
        for (ELMention m : doc.mentions) {
            int start = textAnnotation.getTokenIdFromCharacterOffset(m.getStartOffset());
            int end = textAnnotation.getTokenIdFromCharacterOffset(m.getEndOffset() - 1) + 1;
            if(!seen.contains(start+"_"+end)) {
                nerView.addSpanLabel(start, end, m.getType(), 1d);
                seen.add(start+"_"+end);
            }
        }

        textAnnotation.addView(getViewName(), nerView);
    }

    /**
     * The real work from illinois-ner happens here
     *
     * @param doc
     * @return
     */
    public void annotate(QueryDocument doc) {

        // use the language-specific parameters and brown clusters
        ParametersForLbjCode.currentParameters = this.parameters;

        System.out.println(ParametersForLbjCode.currentParameters.language);
        BrownClusters.set(brownclusters);
//        GazetteersFactory.set(gazetteers);

        // Wikify all n-grams and extract features based on Wikipedia titles
        // doc.mentions stores tokens now
        if (ParametersForLbjCode.currentParameters.featuresToUse.containsKey("WikifierFeatures"))
            nerutils.wikifyNgrams(doc);
        else {
            doc.mentions = nerutils.getNgramMentions(doc, 1);
        }

        Data nerdata = doc2NERData(doc);
        try {
            ExpressiveFeaturesAnnotator.annotate(nerdata);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Vector<Data> data = new Vector<>();
        data.addElement(nerdata);

        if (ParametersForLbjCode.currentParameters.labelsToIgnoreInEvaluation != null)
            data.elementAt(0).setLabelsToIgnore(ParametersForLbjCode.currentParameters.labelsToIgnoreInEvaluation);
        if (ParametersForLbjCode.currentParameters.labelsToAnonymizeInEvaluation != null)
            data.elementAt(0).setLabelsToAnonymize(ParametersForLbjCode.currentParameters.labelsToAnonymizeInEvaluation);

        try {
            Decoder.annotateDataBIO(data.elementAt(0), taggerLevel1, taggerLevel2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        doc.mentions = extractPredictions(data.elementAt(0).documents.get(0), doc.getText());
    }

    /**
     * Convert internal datastructure (QueryDocument) to the one used in illinois-ner
     *
     * @param doc
     * @return
     */
    private Data doc2NERData(QueryDocument doc) {
//        logger.info("Converting document datastructure...");

        Vector<Vector<String>> tokens = new Vector<>();
        TextAnnotation ta = doc.getTextAnnotation();
        tokens.add(new Vector<>());
        ArrayList<LinkedVector> sentences = new ArrayList<>();
        int psen = -1;
        sentences.add(new LinkedVector());
        for (ELMention m : doc.mentions) {
            if(m.getSurface().trim().isEmpty()) continue;
            int tokenid = ta.getTokenIdFromCharacterOffset(m.getStartOffset());
            int senid = ta.getSentenceId(tokenid);
            if (senid != psen) {
                sentences.add(new LinkedVector());
                psen = senid;
            }
            NEWord word = new NEWord(new Word(m.getSurface()), null, "unlabeled");
            word.start = m.getStartOffset();
            word.end = m.getEndOffset();
            String[] wikif = new String[m.ner_features.size()];
            int i = 0;
            for (String key : m.ner_features.keySet()) {
                wikif[i++] = key;
            }
            word.wikifierFeatures = wikif;
            NEWord.addTokenToSentence(sentences.get(sentences.size()-1), word);
        }
        TaggedDataReader.connectSentenceBoundaries(sentences);
        NERDocument nerdoc = new NERDocument(sentences, "consoleInput");
        return new Data(nerdoc);
    }

    /**
     * After making predictions, extract results and store in the internal datastructure
     *
     * @param nerdoc
     * @param text   the original text of document
     * @return
     */
    private List<ELMention> extractPredictions(NERDocument nerdoc, String text) {
        List<ELMention> ret = new ArrayList<>();

        for (LinkedVector sen : nerdoc.sentences) {
            int start = -1, end = -1;
            String pretype = "O";
            for (int j = 0; j < sen.size(); j++) {
                NEWord w = (NEWord) sen.get(j);
                if (w.neTypeLevel2.contains("-")) {   // a NE token
                    String type = w.neTypeLevel2.substring(2);
                    if (!type.equals(pretype)) {  // type is different from the previous token
                        if (start != -1) {
                            ELMention m = new ELMention("", start, end);
                            m.setSurface(text.substring(start, end));
                            m.setType(pretype);
                            ret.add(m);
                        }
                        start = w.start;
                        end = w.end;
                        pretype = type;
                    } else {   // extend the mention
                        end = w.end;
                    }
                } else {
                    if (start != -1) {
                        ELMention m = new ELMention("", start, end);
                        m.setSurface(text.substring(start, end));
                        m.setType(pretype);
                        ret.add(m);
                        start = -1;
                        end = -1;
                        pretype = "O";
                    }
                }
            }
        }
        return ret;
    }

}
