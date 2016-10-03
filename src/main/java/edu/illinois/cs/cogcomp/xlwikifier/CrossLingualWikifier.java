package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ctsai12 on 1/17/16.
 */
public class CrossLingualWikifier {

    private static WikiCandidateGenerator wcg;
    private static Ranker ranker;
    private static Logger logger = LoggerFactory.getLogger(CrossLingualWikifier.class);
    private static String lang;
    private static LangLinker ll;


    public CrossLingualWikifier(String lang, WikiCandidateGenerator wcg){
        this.lang = lang;
        setCandidateGenerator(wcg);
    }

    public static void setLang(String l){
        if(!l.equals(lang)) {
            logger.info("Setting up xlwikifier for language: "+l);
            lang = l;
            wcg = new WikiCandidateGenerator();
            if(ranker != null) ranker.closeDBs();
            ranker = Ranker.loadPreTrainedRanker(lang, "models/ranker/default/"+lang+"/ranker.model");
            ranker.fm.ner_mode = false;
            if(ll!=null) ll.closeDB();
            ll = new LangLinker();
        }
    }

    public void setRanker(Ranker ranker){
        this.ranker = ranker;
    }

    public void setCandidateGenerator(WikiCandidateGenerator wcg){
        this.wcg = wcg;
    }


    public void wikify(List<QueryDocument> docs){
        if(this.wcg == null || this.ranker == null){
            logger.error("Ranker or Candidate Generator is not set");
        }

        wcg.genCandidates(docs, lang);
        ranker.setWikiTitleByModel(docs);
    }

    /**
     * This is only used in demo now
     * @param doc
     */
    public static void wikify(QueryDocument doc){
        List<QueryDocument> docs = new ArrayList<>();
        docs.add(doc);
        wcg.genCandidates(docs, lang);
        ranker.setWikiTitleByModel(docs);
//        te.solveByWikiTitle(docs, lang);

        // get the English title
        if(!lang.equals("en")) {
            for (ELMention m : doc.mentions) {
                if (!m.getWikiTitle().startsWith("NIL")) {
                    String ent = ll.translateToEn(m.getWikiTitle(), lang);
                    if (ent != null)
                        m.en_wiki_title = ent;
                }
            }
        }
        else{
            for(ELMention m: doc.mentions)
                m.en_wiki_title = m.getWikiTitle();
        }
    }

    public static void main(String[] args) {
        CrossLingualWikifier.setLang("en");

    }
}
