package edu.illinois.cs.cogcomp.mlner.core;

import com.google.gson.Gson;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.core.StopWord;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.Transliterator;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.xlel21.TransLookUp;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import org.apache.commons.io.FileUtils;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreebaseSearch;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.QueryMQL;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.SearchResult;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 3/2/16.
 */
public class Utils {

    public Tokenizer tokenizer;
    public Set<String> stops;
    public String lang;
    private ExecutorService executor;
    private Transliterator tr;
    private TransLookUp tlu;
    private LangLinker ll = new LangLinker();
    private WikiCandidateGenerator en_wcg = new WikiCandidateGenerator(true);

    public Utils(){
//        tr = new Transliterator(lang);
//        tlu = new TransLookUp(lang);
    }

    public void setLang(String lang){
        this.lang = lang;
        tokenizer = MultiLingualTokenizer.getTokenizer(lang);
        stops = StopWord.getStopWords(lang);
    }


    public List<ELMention> getNgramMentions(QueryDocument doc, TextAnnotation ta, boolean con, int n){

        List<ELMention> mentions = new ArrayList<>();
        List<ELMention> preds = null;
        List<ELMention> golds = null;

        if (con) {
            preds = doc.mentions.stream().filter(x -> x.is_ne).collect(toList());
            golds = doc.golds;
        } else {
            golds = doc.mentions;
            doc.golds = golds;
        }

        for (int i = 0; i < ta.getTokens().length - n - 1; i++) {
            if (ta.getSentenceId(i) != ta.getSentenceId(i + n - 1))
                continue;
            IntPair offset = ta.getTokenCharacterOffset(i);
            IntPair offset1 = ta.getTokenCharacterOffset(i + n - 1);
            ELMention m = new ELMention(doc.getDocID(), offset.getFirst(), offset1.getSecond());
            String surface = ta.getToken(i);
            for (int j = i+1; j < i+n; j++)
                surface += " " + ta.getToken(j);
            m.setMention(surface);
            m.ngram = n;
            m.is_stop = false;
            if (n == 1 && stops.contains(surface.toLowerCase()))
                m.is_stop = true;
            m.is_ne_gold = false;
            m.setType("O");
            for (ELMention gold : golds) {
                if (m.getStartOffset() >= gold.getStartOffset()
                        && m.getEndOffset() <= gold.getEndOffset()) {
                    m.is_ne_gold = true;
                    m.setType(gold.getType());
                    m.gold_mid = gold.gold_mid;
                    break;
                }
            }

            m.is_ne = false;
            m.pred_type = "O";
            if (con) {
                for (ELMention pred : preds) {
                    if (m.getStartOffset() >= pred.getStartOffset()
                            && m.getEndOffset() <= pred.getEndOffset()) {
                        m.is_ne = true;
                        m.setMid(pred.getMid());
                        m.setCandidates(pred.getCandidates());
                        m.setMidVec(pred.getMidVec());
                        m.setWikiTitle(pred.getWikiTitle());
                        m.pred_type = pred.pred_type;
                        break;
                    }
                }
            }
            mentions.add(m);
        }

        mentions = mentions.stream().sorted((x1, x2) -> Integer.compare(x1.getStartOffset(), x2.getStartOffset()))
                .collect(toList());
        return mentions;
    }

    public List<ELMention> getNgramMentions(QueryDocument doc, int n){
        List<ELMention> mentions = new ArrayList<>();
        TextAnnotation ta = doc.getTextAnnotation();
        if(ta == null) return mentions;
        for (int i = 0; i < ta.getTokens().length - n + 1; i++) {
            if(ta.getSentenceId(i)!=ta.getSentenceId(i+n-1))
                continue;
            IntPair offset = ta.getTokenCharacterOffset(i);
            IntPair offset1 = ta.getTokenCharacterOffset(i + n -1);
            ELMention m = new ELMention(doc.getDocID(), offset.getFirst(), offset1.getSecond());
            String token = ta.getToken(i);
            int idx = token.indexOf("'");
            if(idx >=0 )  token = token.substring(0, idx);
            String surface = token;
            for(int j = i+1; j < i+n; j++) {
                token = ta.getToken(j);
                idx = token.indexOf("'");
                if(idx >=0 )  token = token.substring(0, idx);
                if(lang.equals("zh"))
                    surface += token;
                else
                    surface += " " + token;
            }
            m.setMention(surface);
            m.ngram = n;
            m.is_stop = false;
            List<String> tokens = Arrays.asList(surface.toLowerCase().split("\\s+"));
            if(tokens.stream().filter(x -> stops.contains(x)).count() == tokens.size())
                m.is_stop = true;
            m.is_ne_gold = false;
            m.setType("O");

            m.is_ne = false;
            m.pred_type = "O";
            mentions.add(m);
        }

        mentions = mentions.stream().sorted((x1, x2) -> Integer.compare(x1.getStartOffset(), x2.getStartOffset()))
                .collect(toList());
        mentions.forEach(x -> x.setLanguage(lang));
        return mentions;
    }


    public void setNgramMention(List<QueryDocument> docs, List<TextAnnotation> tas, int n){
        for(int k = 0; k < docs.size(); k++){
            QueryDocument doc = docs.get(k);
//            if(doc == null) continue;
            TextAnnotation ta;
            if(tas == null)
                ta = tokenizer.getTextAnnotation(doc.plain_text);
            else
                ta = tas.get(k);
            List<ELMention> mentions = new ArrayList<>();


            for (int i = 0; i < ta.getTokens().length - n + 1; i++) {
                if(ta.getSentenceId(i)!=ta.getSentenceId(i+n-1))
                    continue;
                IntPair offset = ta.getTokenCharacterOffset(i);
                IntPair offset1 = ta.getTokenCharacterOffset(i + n -1);
                ELMention m = new ELMention(doc.getDocID(), offset.getFirst(), offset1.getSecond());
                String surface = ta.getToken(i);
                for(int j = i+1; j < i+n; j++)
                    surface += " "+ta.getToken(j);
                m.setMention(surface);
                m.ngram = n;
                m.is_stop = false;
                List<String> tokens = Arrays.asList(surface.toLowerCase().split("\\s+"));
                if(tokens.stream().filter(x -> stops.contains(x)).count() == tokens.size())
                    m.is_stop = true;
                m.is_ne_gold = false;
                m.setType("O");
                for(ELMention gold: doc.golds){
                    if(m.getStartOffset() >= gold.getStartOffset()
                            && m.getEndOffset() <= gold.getEndOffset()) {
                        m.is_ne_gold = true;
                        m.setType(gold.getType());
                        m.gold_mid = gold.gold_mid;
                        break;
                    }
                }

                m.is_ne = false;
                m.pred_type = "O";
                mentions.add(m);
            }

            mentions = mentions.stream().sorted((x1, x2) -> Integer.compare(x1.getStartOffset(), x2.getStartOffset()))
                    .collect(toList());
            mentions.forEach(x -> x.setLanguage(lang));
            doc.mentions = mentions;
        }
    }


    public void wikifyMentions(QueryDocument doc, int n, WikiCandidateGenerator wcg, Ranker ranker){
        for(ELMention m: doc.mentions){
            if(!m.getWikiTitle().startsWith("NIL")) continue;
            if(m.ner_features.size()>0) continue;
            String surface = m.getMention().toLowerCase();
            List<WikiCand> cands = wcg.getCandsBySurface(surface, lang, false);
            if(cands.size() == 0 && n==1 && surface.length()>0){
                String init = m.getMention().substring(0, 1);
                if(!init.toLowerCase().equals(init)) {
                    cands.addAll(wcg.getCandidateByWord(surface, lang, 6));
                }
            }

            if(cands.size() == 0 && !lang.equals("en")){
                if(n > 1){
                    cands.addAll(en_wcg.getCandsBySurface(surface, "en", false));
                }
                else if(surface.length()>0){
                    String init = m.getMention().substring(0, 1);
                    if(!init.toLowerCase().equals(init)){   // capitalized
                        cands.addAll(en_wcg.getCandsBySurface(surface, "en", false));
                        if(cands.size()==0)
                            cands.addAll(en_wcg.getCandidateByWord(surface, "en", 6));
                    }
                }
            }

//            if(n==1) {
//                boolean allcap = true;
////                String[] tokens = m.getMention().split("\\s+");
////                for (String token : tokens) {
////                    String init = token.substring(0, 1);
////                    if (!init.toLowerCase().equals(init))
////                        allcap = false;
////                }
//                if (allcap) {   // capitalized
//                    if (cands.size() == 0 ) {
//                        cands = en_wcg.getCandsByTransliteration(surface, lang);
//
//                        List<WikiCand> cands_ = new ArrayList<>();
//
//                        for(WikiCand cand: cands){
//                            List<String> types = FreeBaseQuery.getTypesFromTitle(cand.title, "en");
//                            if (types.contains("people.person") || types.contains("location.location") ||
//                                    types.contains("organization.organization"))
//                                cands_.add(cand);
//                            if(cands_.size()==2) break;
//                        }
////                        System.out.println("Trans "+m.getMention()+" gets "+cands.size()+" cands");
//                        m.getCandidates().addAll(cands_);
//                    }
//                }
//            }

            m.setCandidates(cands);
        }

//        ranker.setWikiTitleByModel(doc);
        ranker.setWikiTitleByTopCand(doc);


        // just keep top 2 cands to reduce cache size
        for(ELMention m: doc.mentions){
            List<WikiCand> cands = m.getCandidates();
            m.setCandidates(cands.subList(0, Math.min(cands.size(),2)));
        }
        setMidByWikiTitle(doc);

    }


    public void genNERTrainingCache(List<QueryDocument> docs, String dir, int n, WikiCandidateGenerator wcg, Ranker ranker){
        System.out.println("#docs "+docs.size());
        int n_thread = 10;
        executor = Executors.newFixedThreadPool(n_thread);
        for(int i = 0; i < docs.size(); i++){
            QueryDocument doc = docs.get(i);
//            if(!doc.getDocID().equals("SPA_NW_001278_20130122_F00014N0S")) continue;
            doc.mentions = getNgramMentions(doc, n);
            wikifyMentions(docs.get(i), n, wcg, ranker);
            executor.execute(new DocumentWorker(docs.get(i), dir));
//            docs.set(i, null);
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public class DocumentWorker implements Runnable {

        private QueryDocument doc;
        private Gson gson;
        private String dir;

        public DocumentWorker(QueryDocument doc, String dir) {
            this.doc = doc;
            this.gson = new Gson();
            this.dir = dir;
        }

        @Override
        public void run() {
            String json = gson.toJson(doc, QueryDocument.class);
            try {
                FileUtils.writeStringToFile(new File(dir, doc.getDocID()), json, "UTF-8");
                doc.mentions = new ArrayList<>();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void getEnCands(List<QueryDocument> docs){
        for(QueryDocument doc: docs){
            getEnCands(doc);
        }
    }

    public void getEnCands(QueryDocument doc){
        for (ELMention m : doc.mentions) {
            if (m.getCandidates().size() == 0) {
                List<WikiCand> cands = en_wcg.getCandsBySurface(m.getMention().toLowerCase(), "en", false);
                if (cands.size() > 0) {
                    System.out.println("get cands from en");
                    m.setCandidates(cands);
//                    m.gold_lang = "en";
                }
            }
        }
    }

    public void getTransCands(QueryDocument doc, WikiCandidateGenerator wcg){
        for (ELMention m : doc.mentions) {
            if (m.getCandidates().size() == 0) {
                String surface = m.getMention().toLowerCase();
                List<String> poss_tran = new ArrayList<>();
                String tran = tlu.LookupTrans(surface);
                if(tran!=null) poss_tran.add(tran);
                if (poss_tran.size()==0) {
                    poss_tran.addAll(tr.getEngTransCands(surface));
                }

                List<WikiCand> cands = new ArrayList<>();
                poss_tran.forEach(x -> cands.addAll(wcg.getCandidate1(x, "en")));
                m.getCandidates().addAll(cands);
                m.gold_lang = "en";
            }
        }
    }

    public void setMidByWikiTitle(List<QueryDocument> docs, String lang){
        System.out.println("Solving by wikipedia titles...");
        for(QueryDocument doc: docs) {
            setMidByWikiTitle(doc);
        }

    }


    public void setMidByWikiTitle(QueryDocument doc){
        boolean search = false;
        for (ELMention m : doc.mentions) {
            if(m.is_ne) continue;
            if(m.ngram>1) search = false;
            else search = true;
//            search = false;

            if (m.getWikiTitle().startsWith("NIL")){
                m.setMid("NIL");
            }

            for (WikiCand c : m.getCandidates()) {
                String mid = getMidByWikiTitle(c.getTitle(), ll, c.lang, search);
                c.orig_title = c.title;
                if (mid != null)
                    c.title = mid;
                else
                    c.title = "NIL";
            }

            if(m.getCandidates().size()>0){
                m.setMid(m.getCandidates().get(0).title);
            }

        }
    }

    public void setMidByWikiTitleUZ(List<QueryDocument> docs){
        System.out.println("Solving by wikipedia titles...");
        boolean search = false;
        FreebaseSearch fb = new FreebaseSearch();
        QueryMQL qm = new QueryMQL();
        LangLinker lluz = new LangLinker();
        lluz.loadDB("uz");
        for(QueryDocument doc: docs) {
            for (ELMention m : doc.mentions) {
                if(m.is_ne) continue;
                if(m.ngram>1) search = false;
                else search = true;

                for(WikiCand c: m.getCandidates()){
                    String en_title = ll.translateToEn(m.getWikiTitle(), c.lang);
                    if(en_title == null) en_title = "NIL";

                    String uz_title = "NIL";
                    if(lang.equals("uz")) uz_title = m.getWikiTitle();
                    else {
                        if (!en_title.startsWith("NIL"))
                            uz_title = lluz.translateFromEn(en_title, "uz");
                        if (uz_title == null) uz_title = "NIL";
                    }

                    String mid = getMidByWikiTitleUZ(uz_title, en_title, fb, qm, search);

                    c.orig_title = uz_title;
                    if(mid!=null)
                        c.title = mid;
                    else
                        c.title = "NIL";
                    c.lang = "uz";
                }

                if(m.getCandidates().size()>0){
                    m.setMid(m.getCandidates().get(0).title);
                    m.setWikiTitle(m.getCandidates().get(0).orig_title);
                }
            }
        }
        lluz.closeDB();
    }

    public String getMidByWikiTitleUZ(String uz_title, String en_title, FreebaseSearch fb, QueryMQL qm, boolean search){
        if(uz_title.trim().isEmpty())
            return null;
        if(uz_title.startsWith("NIL"))
            return null;
        uz_title = formatTitle(uz_title);
        String mid = qm.lookupMidFromTitle(uz_title, "uz");
        if (mid != null) {
            mid = mid.substring(1).replaceAll("/", ".");
            return mid;
        }

        if(en_title!=null){
            en_title = formatTitle(en_title);
            mid = qm.lookupMidFromTitle(en_title, "en");
            if (mid != null) {
                mid = mid.substring(1).replaceAll("/", ".");
                return mid;
            }
        }

        if(search) {
//            System.out.println(title+" "+lang);
            List<SearchResult> answers = fb.lookup(uz_title, "uz", "");
            if (answers.size() > 0) {
                return answers.get(0).getMid().substring(1).replaceAll("/", ".");
            }
        }

        return null;
    }

    public String getMidByWikiTitle(String title, LangLinker ll, String lang, boolean search){
        if(title.trim().isEmpty())
            return null;
        if(title.startsWith("NIL"))
            return null;
        String ent = ll.translateToEn(title, lang);
        title = formatTitle(title);
        if(lang.equals("zh")) lang = "zh-cn";

        String mid = FreeBaseQuery.getMidFromTitle(title, lang);
        if(mid != null) return mid; // in the m.12345 format
//        String mid = qm.lookupMidFromTitle(title, lang);
//        if (mid != null) {
//            mid = mid.substring(1).replaceAll("/", ".");
//            return mid;
//        }

        if(ent!=null){
            ent = formatTitle(ent);
            mid = FreeBaseQuery.getMidFromTitle(ent, "en");
            if(mid!=null) return mid;
//            mid = qm.lookupMidFromTitle(ent, "en");
//            if (mid != null) {
//                mid = mid.substring(1).replaceAll("/", ".");
//                return mid;
//            }
        }

//        if(search && (!lang.equals("uz") && !lang.equals("ha") && !lang.equals("bn")
//            && !lang.equals("yo")&& !lang.equals("ta") && !lang.equals("ug"))) {
////            System.out.println(title+" "+lang);
//            List<SearchResult> answers = fb.lookup(title, lang, "");
//            if (answers.size() > 0) {
//                return answers.get(0).getMid().substring(1).replaceAll("/", ".");
//            }
//        }

        return null;
    }

    public String formatTitle(String title){
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


    public void setWikiTitleFromMid(QueryMQL qm, String lang, ELMention m){
        String mid = m.getGoldMid();
        if(mid.startsWith("NIL"))
            m.gold_wiki_title = "NIL";
        else {
//            List<String> titles = qm.lookupWikiTitleFromMid(mid, lang);

            List<String> titles = FreeBaseQuery.getTitlesFromMid(mid, lang);

            if (titles != null && titles.size() > 0) {
                m.gold_wiki_title = titles.get(0);
                m.gold_lang = lang;
                return;
            }

//            titles = qm.lookupWikiTitleFromMid(mid, "en");
            titles = FreeBaseQuery.getTitlesFromMid(mid, "en");

            if(titles != null && titles.size()>0){
                m.gold_wiki_title = titles.get(0);
                m.gold_lang = "en";
            }
            else{
                m.gold_wiki_title = "NIL";
                System.out.println("Couldn't map mid "+mid);
            }
        }
    }

    public void setBIOGolds(List<QueryDocument> docs){
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                boolean get = false;
                for(ELMention gold: doc.golds){
                    if(m.getStartOffset() >= gold.getStartOffset()
                            && m.getEndOffset() <= gold.getEndOffset()){
                        if(m.getStartOffset() == gold.getStartOffset())
                            m.setType("B-"+gold.getType());
                        else
                            m.setType("I-"+gold.getType());
                        get = true;
                        break;
                    }
                }
                if(!get)
                    m.setType("O");
            }
        }
    }

    public void propFeatures(QueryDocument doc, List<ELMention> prevms){
        if(prevms == null) return;
        int pidx;
        for(ELMention m: doc.mentions){
            for(pidx = 0 ;pidx < prevms.size(); pidx++){
                ELMention pm = prevms.get(pidx);
                if(pm.getStartOffset() >= m.getEndOffset()) break;

                if(m.getStartOffset() >= pm.getStartOffset() &&
                        m.getEndOffset() <= pm.getEndOffset()){

                    String BI = "";
                    if(m.getStartOffset() > pm.getStartOffset()) BI = "I-";
                    else BI="B-";

                    for(String fname: pm.ner_features.keySet()){
                        if(fname.contains("TOPFBTYPETOKEN-person"))
                            m.ner_features.put(BI+"TOPFBTYPETOKEN-person",1.0);
                        if(fname.contains("TOPFBTYPETOKEN-location"))
                            m.ner_features.put(BI+"TOPFBTYPETOKEN-location",1.0);
                        if(fname.contains("TOPFBTYPETOKEN-organization"))
                            m.ner_features.put(BI+"TOPFBTYPETOKEN-organization",1.0);
                    }
                }
            }
        }

    }

    public void propFeatures(List<QueryDocument> docs, Map<String, List<ELMention>> did2ms){
        System.out.print("Propogating features...");
        for(QueryDocument doc: docs){
            if(did2ms.containsKey(doc.getDocID())) {
                List<ELMention> prevms = did2ms.get(doc.getDocID());
                propFeatures(doc, prevms);
            }
        }
        System.out.println("Done");
    }

    public void filterMentions(List<QueryDocument> docs, Map<String, List<ELMention>> id2preds){
        System.out.print("Filtering mentions...");
        for(QueryDocument doc: docs){
            if(!id2preds.containsKey(doc.getDocID())){
                continue;
            }
            List<ELMention> preds = id2preds.get(doc.getDocID());
            for(ELMention m: doc.mentions){
                for (ELMention pred : preds) {
                    if (m.getStartOffset() >= pred.getStartOffset()
                            && m.getEndOffset() <= pred.getEndOffset()) {
                        m.is_ne = true;
                        m.setMid(pred.getMid());
                        m.ngram = pred.ngram;
                        m.setCandidates(pred.getCandidates());
                        m.setMidVec(pred.getMidVec());
                        m.setWikiTitle(pred.getWikiTitle());
                        m.setType(pred.getType());
                        m.pred_type = pred.pred_type;
                        break;
                    }
                }
            }
        }
        System.out.println("Done");
    }
}
