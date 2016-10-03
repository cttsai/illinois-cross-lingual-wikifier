package edu.illinois.cs.cogcomp.xlwikifier.experiments;

import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifier;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.WikiDataReader;
import edu.illinois.cs.cogcomp.transliteration.Transliterator;
import edu.illinois.cs.cogcomp.mlner.classifier.BinaryTypeClassifier;
import edu.illinois.cs.cogcomp.mlner.classifier.FiveTypeClassifier;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.QueryMQL;
import edu.illinois.cs.cogcomp.xlwikifier.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by ctsai12 on 1/20/16.
 */
public class TransliterationExp {

    private WikiDataReader wiki_reader;
    private Evaluator eval;
    private Transliterator tl;
    private LangLinker ll;
    private QueryMQL qm;
    private FiveTypeClassifier typec;
    private BinaryTypeClassifier btc;
    public TransliterationExp(){
        wiki_reader = new WikiDataReader();
        eval = new Evaluator();
        ll = new LangLinker();
//        qm = new QueryMQL();
//        typec = new FiveTypeClassifier();
//        typec.train(false);
//        btc = new BinaryTypeClassifier();
//		btc.train(false);
    }

    public void run(String lang){
        String fblang = lang;
        if(lang.equals("zh")) fblang = "zh-cn";
//        tl = new Transliterator(lang);

//        System.out.println("Running "+lang+" wikipedia experiments...");
//        Ranker ranker = trainRanker(lang);
        List<QueryDocument> docs = wiki_reader.readTestData(lang).subList(0,100);
        for(QueryDocument doc: docs){
//            System.out.println(doc.getDocID());
            for(ELMention m: doc.mentions){
                String mid = FreeBaseQuery.getMidFromTitle(m.gold_wiki_title, fblang);
                Set<String> types = FreeBaseQuery.getCoarseTypeSet(mid);
//                System.out.println(m.gold_wiki_title+" "+mid+" "+types);
                if(types.contains("person") || types.contains("location") || types.contains("organiztion")){
                    System.out.println(m.getMention()+" "+m.gold_enwiki_title);
                }
            }
        }
//        transformDocs(docs, lang);
//        CrossLingualWikifier wikifier = new CrossLingualWikifier("en", new WikiCandidateGenerator(true));
//        wikifier.setRanker(ranker);
//        wikifier.wikify(docs);
//        eval.evaluateWikiTitle(docs);
    }

    private Ranker trainRanker(String lang){
        Ranker ranker = new Ranker("en");
        String model = Utils.getTime()+"."+lang+".trans";
        List<QueryDocument> docs = wiki_reader.readTrainData(lang);
        transformDocs(docs, lang);
        WikiCandidateGenerator wcg = new WikiCandidateGenerator(true);
        wcg.genCandidates(docs, "en");
        ranker.train(docs, model);
        return ranker;
    }

    private void transformDocs(List<QueryDocument> docs, String lang){
        System.out.println("Transliterating mentions...");
        System.out.println("before: "+docs.stream().flatMap(x -> x.mentions.stream()).count());
        int cnt = 0;
        for(QueryDocument doc: docs){
            if(cnt++%100 == 0) System.out.println(cnt+"\r");
            List<ELMention> filtered = new ArrayList<>();
            for(ELMention mention: doc.mentions){

                if(!goodMention(mention, lang))
                    continue;

                String[] tokens = mention.getMention().split("\\s+");
                if(tokens.length == 1){
                    mention.trans = tl.getEngTransCands(tokens[0]);
                }
                else {
                    String en_mention = "";
                    for (String token : tokens) {
//                        String tt = tl.getEngTrans(token);
//                        if (tt != null) en_mention += tt + " ";
                    }
                    en_mention = en_mention.trim();
                    if(!en_mention.isEmpty()){
                        mention.trans = new ArrayList<>();
                        mention.trans.add(en_mention);
                    }
                }

                if(mention.trans!=null && mention.trans.size()>0){
//                    System.out.println(mention.getMention()+" --> "+en_mention);
//                    mention.setMention(en_mention);
                    filtered.add(mention);
                }
            }
            doc.mentions = filtered;
        }
        System.out.println("after: "+docs.stream().flatMap(x -> x.mentions.stream()).count());
    }

    private String formatTitle(String title){
        String tmp = "";
        for(String token: title.split("_")){
            if(token.length()==0) continue;
            tmp+=token.substring(0,1).toUpperCase();
            if(token.length()>1)
                tmp+=token.substring(1,token.length());
            tmp+="_";
        }
        return tmp.substring(0, tmp.length()-1);
    }

    public boolean goodMention(ELMention mention, String lang){
        String en_title = ll.translateToEn(mention.gold_wiki_title, lang);
//        System.out.println("en title: "+en_title);
        if(en_title == null) return false;

        String mid = qm.lookupMidFromTitle(formatTitle(en_title), "en");
//        System.out.println("mid: "+mid);
        if(mid == null) return false;

        mention.gold_wiki_title = en_title;
        boolean ist = btc.isTheTypes(mid);
//        System.out.println("is type: "+ist);
        if(!ist) return false;

        String type = typec.getCoarseType(mid);
//        System.out.println(type);
        if(!type.equals("PER")) return false;

        return true;
    }

    public static void main(String[] args) {
        TransliterationExp te = new TransliterationExp();
        FreeBaseQuery.loadDB(true);
        te.run("es");
    }
}
