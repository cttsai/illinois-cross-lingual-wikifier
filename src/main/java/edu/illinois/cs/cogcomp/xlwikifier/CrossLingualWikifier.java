package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.annotation.Annotator;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.CoreferenceView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.Language;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.core.WikiCandidateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * Created by ctsai12 on 10/26/16.
 */
public class CrossLingualWikifier extends Annotator {

    private final Logger logger = LoggerFactory.getLogger(CrossLingualWikifier.class);
    private Language language;
    private WikiCandidateGenerator wcg;
    private Ranker ranker;
    private LangLinker ll;
    public QueryDocument result; // saving results in this datastructure for the demo

    public CrossLingualWikifier(Language lang, String configFile) throws IOException {

        super(lang.getWikifierViewName(), new String[]{}, true, new ResourceManager(configFile));

        this.language = lang;

        doInitialize();
    }

    @Override
    public void initialize(ResourceManager resourceManager) {
        String lang = this.language.toString().toLowerCase();

        if (!FreeBaseQuery.isloaded())
            FreeBaseQuery.loadDB(true);

        wcg = new WikiCandidateGenerator(lang, true);
        ranker = Ranker.loadPreTrainedRanker(lang, ConfigParameters.model_path + "/ranker/default/" + lang + "/ranker.model");
        ranker.setNERMode(false);
        ll = new LangLinker();

    }

    @Override
    public void addView(TextAnnotation textAnnotation) {

        if (!textAnnotation.hasView(language.getNERViewName())) {
            logger.error(language.getNERViewName() + " is required");
        }

        QueryDocument doc = ta2QueryDoc(textAnnotation);

        annotate(doc);

        CoreferenceView corefview = new CoreferenceView(getViewName(), textAnnotation);

        // cluster mentions by the English Wikipedia title
        Map<String, List<ELMention>> title2mentions = doc.mentions.stream()
                .collect(groupingBy(x -> x.en_wiki_title));

        for (String title : title2mentions.keySet()) {

            // sort mentions in a cluster by the length of surface forms
            List<ELMention> len_sort = title2mentions.get(title).stream()
                    .sorted((x1, x2) -> Integer.compare(x2.getSurface().length(), x1.getSurface().length()))
                    .collect(Collectors.toList());

            List<Constituent> cons = new ArrayList<>();
            for (ELMention m : len_sort) {
                int start = textAnnotation.getTokenIdFromCharacterOffset(m.getStartOffset());
                int end = textAnnotation.getTokenIdFromCharacterOffset(m.getEndOffset() - 1) + 1;
                Constituent c = new Constituent(title, getViewName(), textAnnotation, start, end);
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
    private QueryDocument ta2QueryDoc(TextAnnotation textAnnotation){
        QueryDocument doc = new QueryDocument(textAnnotation.getId());
        doc.plain_text = textAnnotation.getText();
        for (Constituent c : textAnnotation.getView(language.getNERViewName())) {
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
        ranker.setWikiTitleByModel(doc);

        String lang = language.toString().toLowerCase();
        // get the English title
        if (!lang.equals("en")) {
            for (ELMention m : doc.mentions) {
                if (!m.getWikiTitle().startsWith("NIL")) {
                    String ent = ll.translateToEn(m.getWikiTitle(), lang);
                    if (ent != null)
                        m.en_wiki_title = ent;
                }
            }
        } else {
            for (ELMention m : doc.mentions)
                m.en_wiki_title = m.getWikiTitle();
        }

        // save the result, which is used in generating demo output
        this.result = doc;
    }
}
