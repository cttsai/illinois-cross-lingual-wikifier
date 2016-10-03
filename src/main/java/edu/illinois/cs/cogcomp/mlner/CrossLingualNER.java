package edu.illinois.cs.cogcomp.mlner;

import edu.illinois.cs.cogcomp.LbjNer.ExpressiveFeatures.ExpressiveFeaturesAnnotator;
import edu.illinois.cs.cogcomp.LbjNer.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.LbjNer.LbjFeatures.NETaggerLevel1;
import edu.illinois.cs.cogcomp.LbjNer.LbjFeatures.NETaggerLevel2;
import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.*;
import edu.illinois.cs.cogcomp.LbjNer.ParsingProcessingData.TaggedDataReader;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.mlner.core.NERClassifier;
import edu.illinois.cs.cogcomp.mlner.core.Utils;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by ctsai12 on 4/24/16.
 */
public class CrossLingualNER {

    private static String lang;
    public static Tokenizer tokenizer;
    private static Utils utils;
    private static WikiCandidateGenerator wcg;
    private static Ranker ranker;
    private static NERClassifier nc;
    private static String configpath = "/shared/experiments/ctsai12/workspace/xlwikifier/config/ner/";
    public static boolean transfer = true;

    private static Logger logger = LoggerFactory.getLogger(CrossLingualNER.class);

    public static void setLang(String l, boolean trans){
        if(!l.equals(lang) || trans != transfer) {
            transfer = trans;
            lang = l;
            logger.info("Setting lang in xlner: " + lang);
            tokenizer = MultiLingualTokenizer.getTokenizer(lang);
            utils = new Utils();
            utils.setLang(lang);
            wcg = new WikiCandidateGenerator(true);
            if(ranker != null) ranker.closeDBs();
            ranker = Ranker.loadPreTrainedRanker(lang, "models/ranker/ner/" + lang+"/ranker.model");
            ranker.fm.ner_mode = true;
            nc = new NERClassifier(lang);
            try {
                if(!transfer || lang.equals("en"))
                    Parameters.readConfigAndLoadExternalData(configpath+"mono/"+lang+".config", false);
                else
                    Parameters.readConfigAndLoadExternalData(configpath+"transfer/"+lang+".config", false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Wikify n-grams and generate NER features
     * @param doc
     */
    public static void wikifyNgrams(QueryDocument doc){
        logger.info("Wikifying n-grams...");


        List<ELMention> golds = doc.mentions;

        List<ELMention> prevm = new ArrayList<>();
        for(int n = 4; n >0; n--) {
            doc.mentions = utils.getNgramMentions(doc, n);
            utils.propFeatures(doc, prevm);
//            if(golds.size() > 0)
//                setWikiTitleByGolds(doc, surface2m);
            utils.wikifyMentions(doc, n, wcg, ranker);
            nc.extractNERFeatures(doc);
            prevm = doc.mentions;
        }
    }

    public static Data doc2NERData(QueryDocument doc){
        logger.info("Converting document datastructure...");

        Vector<Vector<String>> tokens = new Vector<>();
        TextAnnotation ta = doc.getTextAnnotation();
        tokens.add(new Vector<>());
        Vector<LinkedVector> sentences =new Vector<>();
        int psen = -1;
        sentences.add(new LinkedVector());
        for(ELMention m: doc.mentions){
//            System.out.println(m.getWikiTitle()+" "+m.getMid());
            int tokenid = ta.getTokenIdFromCharacterOffset(m.getStartOffset());
            int senid = ta.getSentenceId(tokenid);
            if(senid != psen){
                sentences.add(new LinkedVector());
                psen = senid;
            }
            NEWord word=new NEWord(new Word(m.getMention()),null,"unlabeled");
            word.char_start = m.getStartOffset();
            word.char_end = m.getEndOffset();
            String[] wikif = new String[m.ner_features.size()];
            int i = 0;
            for(String key: m.ner_features.keySet()){
                wikif[i++] = key+":"+m.ner_features.get(key);
            }
            word.wikifierfeats = wikif;
            NEWord.addTokenToSentence(sentences.lastElement(), word);
        }
        TaggedDataReader.connectSentenceBoundaries(sentences);
        NERDocument nerdoc = new NERDocument(sentences, "consoleInput");
        return new Data(nerdoc);
    }

    public static List<ELMention> extractPredictions(NERDocument nerdoc, String text){
        List<ELMention> ret = new ArrayList<>();

        for(LinkedVector sen: nerdoc.sentences){
            int start = -1, end = -1;
            String pretype = "O";
            for(int j = 0; j < sen.size(); j++){
                NEWord w = (NEWord) sen.get(j);
                if(w.neTypeLevel2.contains("-")){   // a NE token
                    String type = w.neTypeLevel2.substring(2);
                    if(!type.equals(pretype)){  // type is different from the previous token
                        if(start != -1){
                            ELMention m = new ELMention("", start, end);
                            m.setMention(text.substring(start, end));
                            m.setType(pretype);
                            ret.add(m);
                        }
                        start = w.char_start;
                        end = w.char_end;
                        pretype = type;
                    }
                    else{   // extend the mention
                        end=w.char_end;
                    }
                }
                else{
                    if(start != -1){
                        ELMention m = new ELMention("", start, end);
                        m.setMention(text.substring(start, end));
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

    public static NERDocument annotate(QueryDocument doc){
        if(ParametersForLbjCode.currentParameters.featuresToUse.containsKey("Wikifier"))
            wikifyNgrams(doc);
        else{
            doc.mentions = utils.getNgramMentions(doc, 1);
        }

        Data nerdata = doc2NERData(doc);
        ExpressiveFeaturesAnnotator.train = false;
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

        NETaggerLevel1 taggerLevel1 = new NETaggerLevel1(ParametersForLbjCode.currentParameters.pathToModelFile + ".level1", ParametersForLbjCode.currentParameters.pathToModelFile + ".level1.lex");
        NETaggerLevel2 taggerLevel2 = new NETaggerLevel2(ParametersForLbjCode.currentParameters.pathToModelFile + ".level2", ParametersForLbjCode.currentParameters.pathToModelFile + ".level2.lex");

        try {
            Decoder.annotateDataBIO(data.elementAt(0), taggerLevel1, taggerLevel2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data.elementAt(0).documents.get(0);
    }

    public static QueryDocument annotate(String text){
        logger.info("Running multilingual NER...");

        if(lang == null){
            logger.error("lang not set!");
            return null;
        }

        QueryDocument doc = new QueryDocument("");
        doc.plain_text = text;
        TextAnnotation ta = tokenizer.getTextAnnotation(text);
        doc.setTextAnnotation(ta);
        NERDocument nerdoc = annotate(doc);
        List<ELMention> mentions = extractPredictions(nerdoc, doc.plain_text);
//        mentions.forEach(x -> System.out.println(x+" "+x.getType()));
        doc.mentions = mentions;
        return doc;
    }

    public static void main(String[] args) {
        CrossLingualNER.setLang("es", true);
        QueryDocument doc = CrossLingualNER.annotate("Louis van Gaal , Endonezya maçı sonrasında oldukça ses getirecek açıklamalarda bulundu ."); // from DF_FTR_TUR_0514802_20140900
//        CrossLingualWikifier.setLang("tr");
//        CrossLingualWikifier.wikify(doc);
//        doc.mentions.forEach(x -> System.out.println(x.getMention()+" "+x.getWikiTitle()));

    }
}
