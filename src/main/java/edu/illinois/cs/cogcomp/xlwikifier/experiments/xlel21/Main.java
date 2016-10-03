package edu.illinois.cs.cogcomp.xlwikifier.experiments.xlel21;

import com.google.gson.Gson;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.Transliterator;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import org.apache.commons.io.FileUtils;
import edu.illinois.cs.cogcomp.mlner.classifier.BinaryTypeClassifier;
import edu.illinois.cs.cogcomp.mlner.classifier.FiveTypeClassifier;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.QueryMQL;
import edu.illinois.cs.cogcomp.xlwikifier.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 1/24/16.
 */
public class Main {

    private XLELReader reader;
    private TACKnowledgeBase kb;
    private WikiCandidateGenerator wcg;
    private QueryMQL qm;
    private BinaryTypeClassifier btc;
    private FiveTypeClassifier fc;
    private Transliterator tr;
    private TransLookUp tlu;
    private LangLinker ll = new LangLinker();
    private Map<String, ELMention> cand_cache = new HashMap<>();
    private Gson gson = new Gson();
    public Main(){
        reader = new XLELReader();
        kb = TACKnowledgeBase.defaultInstance();
        wcg = new WikiCandidateGenerator();
        qm = new QueryMQL();
        fc = new FiveTypeClassifier();
        fc.train(false);
        btc = new BinaryTypeClassifier();
        btc.train(false);
    }


    public void run(String lang){

        tr = new Transliterator(lang);
        List<QueryDocument> docs = reader.readDocs(lang, "train");
//        setCandsByTrans(docs, lang);
        setCands(docs, lang);
        candsToJson(docs, lang);
//        setCandsFromCache(docs, lang);
        Ranker ranker = new Ranker(lang);
        String model = Utils.getTime()+"."+lang+".xlel";
        ranker.train(docs, model);
//        checkGoldVec(docs, ranker);
//        Linker linker = new Linker(ranker);
//        linker.trainLinker(docs);

        docs = reader.readDocs(lang, "eval");
//        setCandsByTrans(docs, lang);
//        checkGoldVec(docs, ranker);
        setCands(docs, lang);
        candsToJson(docs, lang);
//        setCandsFromCache(docs, lang);
        ranker.setWikiTitleByModel(docs);

        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                m.trans = new ArrayList<>();
//                if(m.getCandidates().size()>0){
//                    m.trans.add(m.getCandidates().get(0).query_surface);
//                }
                if(!m.getWikiTitle().startsWith("NIL"))
                    m.trans.add(m.getWikiTitle().replaceAll("_"," "));
            }
        }
        evalTransliteration(docs, lang);

//        linker.apply(docs);
        System.out.println();
//        eval.evaluateWikiTitle(docs);
        evaluate(docs);
        tr.printSeqQueries();

    }

    private void setTransliteration(String lang){

        List<QueryDocument> docs = reader.readDocs(lang, "eval");
        tr = new Transliterator(lang);
        tlu = new TransLookUp(lang);

        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){

                m.trans = new ArrayList<>();

                String tran = tlu.LookupTrans(m.getMention());
                if(tran != null){
                    m.trans.add(tran);
                }
                else{
                    List<String> ts = tr.getEngTransCands(m.getMention());
                    m.trans.addAll(ts);
                }
            }
        }

        evalTransliteration(docs, lang);


    }

    private void evalTransliteration(List<QueryDocument> docs, String lang){

        int correct = 0, total = 0, contain = 0;
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                if(m.en_gold_trans != null){
                    total++;
                    if(m.trans.size()>0 && m.en_gold_trans.toLowerCase().equals(m.trans.get(0)))
                        correct++;

                    if(m.trans.contains(m.en_gold_trans.toLowerCase()))
                        contain++;
                    else{
                        System.out.println(m.getMention()+"-->"+m.trans+" gold:"+m.en_gold_trans);
                    }
                }
            }
        }

        System.out.println("Transliteration Accuracy: "+(double)correct/total);
        System.out.println("Transliteration Coverage: "+(double)contain/total);

    }

    private void checkGoldVec(List<QueryDocument> docs, Ranker ranker){

        int nn = 0, nn1 = 0, nn2 = 0;
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                if(ranker.fm.we.getTitleVector(m.gold_wiki_title, m.gold_lang) == null)
                    nn++;
//                if(ranker.fm.we.getWikiDocMentionVec(m.gold_wiki_title, m.gold_lang) == null)
//                    nn1++;
//                if(ranker.fm.we.getWikiDocTitleVec(m.gold_wiki_title, m.gold_lang) == null)
//                    nn2++;
            }
        }
        System.out.println("#null: "+nn+" "+nn1+" "+nn2);
    }

    private void candsToJson(List<QueryDocument> docs, String lang){
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                String path = "/shared/dickens/ctsai12/multilingual/cache/cands/"+lang+"/"+m.getID();
                String out = m.getCandidates().stream().map(x -> gson.toJson(x)).collect(joining("\n"));
                try {
                    FileUtils.writeStringToFile(new File(path), out, "UTF-8");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<WikiCand> loadCandCache(String mid, String lang){
        String path = "/shared/dickens/ctsai12/multilingual/cache/cands/"+lang+"/"+mid;
        String str = null;
        try {
            str = FileUtils.readFileToString(new File(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<WikiCand> cands = new ArrayList<>();
        if(str.trim().isEmpty()) return cands;
//        System.out.println(mid);
        for(String json: str.split("\n")){
            WikiCand c = gson.fromJson(json, WikiCand.class);
//            System.out.println(c.title+" "+c.lang);
            if(c.lang == null){
                System.out.println(mid);
                System.exit(-1);
            }
            cands.add(c);
        }
        return cands;
    }

    private void setCandsFromCache(List<QueryDocument> docs, String lang){
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                List<WikiCand> cands = loadCandCache(m.getID(), lang);
                m.setCandidates(cands);
            }
        }
    }


    private void setCandsByTrans(List<QueryDocument> docs, String lang){
        List<ELMention> mentions = docs.stream().flatMap(x -> x.mentions.stream()).collect(toList());
        wcg.tli = tr;
        tlu = new TransLookUp(lang);
        int c = 0;
        for(ELMention m: mentions){
            if(c++%10 == 0) System.out.print(c+"\r");

            String surface = m.getMention().toLowerCase();

            if(cand_cache.containsKey(surface)){
                m.setCandidates(copyFromCache(surface));
//                m.trans = cand_cache.get(surface).trans;
                continue;
            }

            String tran;
            List<String> poss_tran = new ArrayList<>();
            tran = tlu.LookupTrans(surface);
            if(tran!=null) poss_tran.add(tran);

            if (poss_tran.size()==0) {
                poss_tran.addAll(tr.getEngTransCands(surface));
            }
            if(poss_tran.size() == 0)
                poss_tran.add(surface);

            List<WikiCand> cands = new ArrayList<>();
            poss_tran.forEach(x -> cands.addAll(wcg.getCandidate1(x, "en")));
            m.getCandidates().addAll(cands);
            List<WikiCand> filtered = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for(WikiCand cand: m.getCandidates()){
                if(seen.contains(cand.title))
                    continue;
                else{
                    seen.add(cand.title);
                    if(kb.getEntryByTitle(cand.title) == null)
                        continue;
                    if(!isPER(cand.title, "en"))
                        continue;
                    filtered.add(cand);
                }
            }
            filtered = filtered.stream().sorted((x1, x2) -> Double.compare(x2.getScore(), x1.getScore())).collect(Collectors.toList());
            m.setCandidates(filtered);
            cand_cache.put(m.getMention().toLowerCase(), m);
        }
    }


    private void setCands(List<QueryDocument> docs, String lang){
        List<ELMention> mentions = docs.stream().flatMap(x -> x.mentions.stream()).collect(toList());
        wcg.tli = tr;
        tlu = new TransLookUp(lang);
        System.out.println("Querying foreign cands by foreign mentions...");
        for(ELMention m: mentions){
            List<WikiCand> cands = wcg.getCandsBySurface(m.getMention().toLowerCase(), lang, true);
            cands.addAll(wcg.getCandidateByWord(m.getMention().toLowerCase(), lang, 10));
            cands.forEach(x -> x.orig_title = x.title);
            cands.forEach(x -> x.title = ll.translateToEn(x.title, lang));
            cands = cands.stream().filter(x -> x.title != null).collect(Collectors.toList());
            cands.forEach(x -> x.lang = "en");
            m.getCandidates().addAll(cands);
//            m.setEngMention(m.getMention());
        }
        System.out.println("Querying en cands by foreign mentions...");
        for(ELMention m: mentions){
//            if(m.getCandidates().size() == 0) {
                List<WikiCand> cands = wcg.getCandsBySurface(m.getMention().toLowerCase(), "en", true);
                cands.addAll(wcg.getCandidateByWord(m.getMention().toLowerCase(), "en", 10));
                m.getCandidates().addAll(cands);
//                m.setEngMention(m.getMention());
//            }
        }
        System.out.println("Querying en cands by transliteration...");
        int c = 0;
        for(ELMention m: mentions){
            if(c++%10 == 0) System.out.print(c+"\r");

            String surface = m.getMention().toLowerCase();

            if(cand_cache.containsKey(surface)){
                m.setCandidates(copyFromCache(surface));
//                m.trans = cand_cache.get(surface).trans;
                continue;
            }

            String tran;
            List<String> poss_tran = new ArrayList<>();
//            if(m.getCandidates().size() == 0) {
                tran = tlu.LookupTrans(surface);
                if(tran!=null) poss_tran.add(tran);

                if (poss_tran.size()==0) {
//                    tran = tr.getEngTrans(m.getMention().toLowerCase());
                    poss_tran.addAll(tr.getEngTransCands(surface));
                }
                if(poss_tran.size() == 0)
                    poss_tran.add(surface);

                List<WikiCand> cands = new ArrayList<>();
                poss_tran.forEach(x -> cands.addAll(wcg.getCandidate1(x, "en")));
                m.getCandidates().addAll(cands);
//            }
            List<WikiCand> filtered = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            for(WikiCand cand: m.getCandidates()){
                if(seen.contains(cand.title))
                    continue;
                else{
                    seen.add(cand.title);
                    if(kb.getEntryByTitle(cand.title) == null)
                        continue;
                    if(!isPER(cand.title, "en"))
                        continue;
                    filtered.add(cand);
                }
            }
//            filtered.forEach(x -> x.src = "trans");
            filtered = filtered.stream().sorted((x1, x2) -> Double.compare(x2.getScore(), x1.getScore())).collect(Collectors.toList());
            m.setCandidates(filtered);
            cand_cache.put(m.getMention().toLowerCase(), m);
        }
    }

    private List<WikiCand> copyFromCache(String key){
        List<WikiCand> ret = new ArrayList<>();
        for(WikiCand c: cand_cache.get(key).getCandidates()){
            WikiCand nc = new WikiCand(c.title, c.score);
            nc.psgivent = c.psgivent;
            nc.ptgivens = c.ptgivens;
            nc.lang = c.lang;
            nc.src = c.src;
            nc.query_surface = c.query_surface;
            ret.add(nc);
        }
        return ret;
    }

    private void evaluate(List<QueryDocument> docs){

//        System.out.println("Evaluating candidates...");
        int non_nil = 0, non_nil_get = 0, non_nil_top = 0;
        int nil = 0, has_cand = 0, nil_correct = 0;
        for(QueryDocument doc: docs) {
            for (ELMention m : doc.mentions) {
                if (!m.gold_wiki_title.startsWith("NIL")) {
                    non_nil++;
                    if (m.getWikiTitle().toLowerCase().equals(m.gold_wiki_title))
                            non_nil_top++;
                    Set<String> cand_titles = m.getCandidates().stream().map(x -> x.title.toLowerCase()).collect(toSet());
                    if(cand_titles.contains(m.gold_wiki_title))
                        non_nil_get++;
                }
                else{
                    nil++;
                    if(m.getCandidates().size()>0)
                        has_cand++;
                    if(m.getWikiTitle().startsWith("NIL"))
                        nil_correct++;
                }

                if(!m.getWikiTitle().equals(m.gold_wiki_title)){
                    System.out.println("Mention: "+m.getMention()+" gold:"+m.gold_wiki_title);
                    for(WikiCand cand: m.getCandidates()) {
                        if(cand.title.equals(m.gold_wiki_title))
                            System.out.print("\t**");
                        else
                            System.out.print("\t  ");
                        System.out.println(cand.title + " " + cand.score);//+" "+cand.ranker_feats.get("PRESCORE"));
                    }
                }
            }
        }

        System.out.println("#NIL: "+nil+" have cands: "+(float)has_cand/nil+" correct:"+(float)nil_correct/nil);
        System.out.println("#Non NIL: "+non_nil+" have gold: "+(float)non_nil_get/non_nil+" top: "+(float)non_nil_top/non_nil);
        System.out.println("Overall: "+(float)(nil_correct+non_nil_top)/(non_nil+nil));
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

    private boolean isPER(String title, String lang){
        String mid = FreeBaseQuery.getMidFromTitle(title, lang);
        Set<String> types = FreeBaseQuery.getCoarseTypeSet(mid);
        return types.contains("person") || types.contains("people");
//        if(mid == null) return true;
//        boolean ist = btc.isTheTypes(mid);
//        if(!ist) return false;
//        String type = fc.getCoarseType(mid);
//        if(!type.equals("PER")) return false;
//        return true;
    }

    public static void main(String[] args) {
        FreeBaseQuery.loadDB(true);
        Main exp = new Main();
//        exp.checkCandGen(args[0]);
        exp.run(args[0]);
//        exp.setTransliteration(args[0]);

//        XLELReader r = new XLELReader();
//        List<QueryDocument> traindocs = r.readDocs("el", "train");
//        Set<String> trainset = traindocs.stream().flatMap(x -> x.mentions.stream().map(y -> x.getDocID()+"_"+y.getMention())).collect(Collectors.toSet());
//        Set<String> testset = r.readDocs("el", "eval").stream().flatMap(x -> x.mentions.stream().map(y -> x.getDocID()+"_"+y.getMention())).collect(toSet());
//        trainset.retainAll(testset);
//        System.out.println(trainset.size());
    }
}
