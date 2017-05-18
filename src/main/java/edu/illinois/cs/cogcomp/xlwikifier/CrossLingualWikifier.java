package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.CoreferenceView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.xlwikifier.mlner.NERUtils;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.PostProcessing;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.SurfaceClustering;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.core.WikiCandidateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * Generate cross-lingual wikification annotations using the Annotator API.
 * The language specific NER view (Language.getNERViewName()) is required in the input TextAnnotation.
 *
 * Created by ctsai12 on 10/26/16.
 */
public class CrossLingualWikifier extends Annotator {

    private final Logger logger = LoggerFactory.getLogger(CrossLingualWikifier.class);
    private Language language;
    private WikiCandidateGenerator wcg;
    private Ranker ranker;
    private NERUtils nerutils;
    private String ner_view;

    private List<QueryDocument> processed_docs = new ArrayList<>(); // for cross-doc coref
    public QueryDocument result; // saving results in this datastructure for the demo

    /**
     * @param lang the target language
     * @param configFile a global configuration file, see config/xlwikifier-tac.config for example
     * @throws IOException
     */
    public CrossLingualWikifier(Language lang, String configFile) throws IOException {

        super(lang.name()+"_WIKIFIERVIEW", new String[]{}, true, new ResourceManager(configFile));

        this.language = lang;

        this.ner_view = lang.name()+"_NERVIEW";

        ConfigParameters.setPropValues(configFile);

        doInitialize();
    }


    @Override
    public void initialize(ResourceManager resourceManager) {

        logger.info("Initializing CrossLingualWikifier...");
        String lang = this.language.getCode();

        wcg = new WikiCandidateGenerator(lang, true);
        ranker = Ranker.loadPreTrainedRanker(lang, ConfigParameters.ranker_models.get(lang));
        ranker.setNERMode(false);

        nerutils = new NERUtils(lang);

        logger.info("Initialization done");
    }

    @Override
    public void addView(TextAnnotation textAnnotation) {

        if (!textAnnotation.hasView(ner_view)) {
            logger.error(ner_view + " is required");
        }

        QueryDocument doc = ta2QueryDoc(textAnnotation, ner_view);

        PostProcessing.cleanSurface(doc);

        annotate(doc);

        PostProcessing.fixPerAnnotation(doc);

        SurfaceClustering.cluster(doc.mentions);

        doc.mentions = doc.mentions.stream()
                .sorted(Comparator.comparingInt(ELMention::getStartOffset))
                .collect(Collectors.toList());

        doc.mentions.forEach(x -> System.out.println(x));

        // save the result, which is used in generating demo output
        this.result = doc;

        CoreferenceView corefview = new CoreferenceView(getViewName(), textAnnotation);

        // cluster mentions by the English Wikipedia title or FreeBase MID
        Map<String, List<ELMention>> title2mentions = null;
        if(ConfigParameters.target_kb.equals("freebase")) {
             title2mentions = doc.mentions.stream()
                    .collect(groupingBy(x -> x.getMid()));
        }
        else if(ConfigParameters.target_kb.equals("enwiki")){
            title2mentions = doc.mentions.stream()
                    .collect(groupingBy(x -> x.getEnWikiTitle()));
        }

        for (String title : title2mentions.keySet()) {

            if ( null == title || "".equals(title) ) {
                logger.warn("skipping empty title -- presumably, nothing in freebase for some mention in doc {}'", doc.getDocID() );
                continue;
            }
            // sort mentions in a cluster by the length of surface forms
            List<ELMention> len_sort = title2mentions.get(title).stream()
                    .sorted((x1, x2) -> Integer.compare(x2.getSurface().length(), x1.getSurface().length()))
                    .collect(Collectors.toList());

            List<Constituent> cons = new ArrayList<>();
            for (ELMention m : len_sort) {
                int start = textAnnotation.getTokenIdFromCharacterOffset(m.getStartOffset());
                int end = textAnnotation.getTokenIdFromCharacterOffset(m.getEndOffset() - 1) + 1;
                List<WikiCand> elmCandidates = m.getCandidates();
                Map<String, Double> titleScores = new HashMap<>();


                logger.debug("mention Title: {}; {} candidates:", title, elmCandidates.size());

                for ( WikiCand cand : elmCandidates ) {
                    if (null == cand )
                        continue;
                    
//                    logger.debug("Candidate: {}", cand.toString());
                    if ( null != cand.getOrigTitle()) {
                        String candTitle = nerutils.translateToEn(cand.getOrigTitle());
                        titleScores.put(candTitle, cand.getScore());
                    }
                }

                if ( titleScores.isEmpty() ) // could happen if there are only null candidates
                    titleScores.put(title, 1.0 ); // NIL mention

                Constituent c = new Constituent(titleScores, getViewName(), textAnnotation, start, end);
                cons.add(c);
            }

            // the longest mentions is the canonical mention
            corefview.addCorefEdges(cons.get(0), cons);
        }

        textAnnotation.addView(getViewName(), corefview);
    }

    /**
     * Convert textAnnotation to the internal data structure
     * @param textAnnotation
     * @return
     */
    public static QueryDocument ta2QueryDoc(TextAnnotation textAnnotation, String nerViewName){
        QueryDocument doc = new QueryDocument(textAnnotation.getId());
        doc.setTextAnnotation(textAnnotation);
        doc.text = textAnnotation.getText();
        for (Constituent c : textAnnotation.getView(nerViewName)) {
            ELMention m = new ELMention("", c.getStartCharOffset(), c.getEndCharOffset());
            m.setSurface(c.getSurfaceForm());
            m.setType(c.getLabel());
            doc.mentions.add(m);
        }
        return doc;
    }

    /**
     * Real job happens here: generating title candidates and then rank them.
     * @param doc
     */
    public void annotate(QueryDocument doc) {
        wcg.genCandidates(doc);

        //ranker.setWikiTitleByModel(doc);
        ranker.setWikiTitleByCoherenceModel(doc);
        //ranker.setWikiTitleByTopCand(doc);

        nerutils.setEnWikiTitle(doc);

        nerutils.setMidByWikiTitle(doc);

        if(ConfigParameters.use_search)
            PostProcessing.wikiSearchSolver(doc,language.getCode());

        doc.mentions.forEach(x -> x.cleanFeatureCache());
    }
}
