package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.mlner.CrossLingualNER;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
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

    public static void init(String l){

        if(!l.equals(lang)) {
            if(ConfigParameters.db_path == null){
                ConfigParameters param = new ConfigParameters();
                param.getPropValues();
            }

            if(!FreeBaseQuery.isloaded())
                FreeBaseQuery.loadDB(true);

            logger.info("Setting up xlwikifier for language: "+l);
            lang = l;
            wcg = new WikiCandidateGenerator();
//            if(ranker != null) ranker.closeDBs();
            ranker = Ranker.loadPreTrainedRanker(lang, ConfigParameters.model_path+"/ranker/default/"+lang+"/ranker.model");
            ranker.fm.ner_mode = false;
//            if(ll!=null) ll.closeDB();
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
        CrossLingualNER.init("en", false);
        String text = "Xinhua News Agency, Shanghai, April 3rd, by reporter Jierong Zhou  Recently, HSBC has moved its Shanghai branch to the China Shipping Mansion in the Pudong Lujiazui financial trading district, becoming the third foreign capital bank to be approved to operate RMB business and shift to Pudong.   At the moment, Shanghai already has 8 foreign capital banks approved to operate RMB business. They are the US's Citibank, Hong Kong's HSBC, Japan's Tokyo Mitsubishi Bank, Japan's Industrial Bank, the Shanghai Branch of the Standard and Chartered Bank, Shanghai's BNP Paris Bank, the Shanghai Branch of Japan's Dai-Ichi Kangyo Bank and the Shanghai Branch of Japan's Sanwa Bank.  According to regulations, these 8 banks will all move to Pudong soon.   After the policy was formally announced for the Pudong Lujiazui Financial Trade District to allow foreign capital banks to operate RMB business, more than 40 foreign capital banks and financial institutions have submitted applications to establish branches in Pudong.   When selecting locations in Lujiazui, some foreign financial institutions do not mind the real estate prices, but focus more on whether the class of the building is compatible with the standing of the company.  The Franklin Templeton Company, whose headquarters are in California, the US, and has registered capital of 130 billion US dollars all around the world, decided to establish its local China headquarters in Pudong.  The president of the company and the Asian regional president personally surveyed Pudong and decided to choose office locations in the Shanghai Securities Building, which has a floor rate of more than 2700 US dollars per square meter.  Now, the Templeton Company has formally signed a contract, and bought the floor area of the entire 18th floor of the Securities Building.   Currently, foreign-capital banks and financial institutions such as Hong Kong's HSBC, the Shanghai Branch of the Japan Industrial Bank, the Holland Co-operation Bank, Shanghai Branch of the Belgium Credit Bank, etc., have successively settled down in the Lujiazui Financial Trading District.  Soon, a large number of domestic and foreign financial institutions, and large companies will also come to successively settle down, including the finance street which has already formed in the Waitan district, and the Shanghai central commercial district framework that is forming.  (End)";
//        QueryDocument doc = CrossLingualNER.annotate("Louis van Gaal , Endonezya maçı sonrasında oldukça ses getirecek açıklamalarda bulundu ."); // from DF_FTR_TUR_0514802_20140900
        QueryDocument doc = CrossLingualNER.annotate(text);
        CrossLingualWikifier.init("en");
        CrossLingualWikifier.wikify(doc);
        doc.mentions.forEach(x -> System.out.println(x.getMention()+" "+x.getWikiTitle()));

    }
}
