package edu.illinois.cs.cogcomp.xlwikifier.experiments;

import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifier;
import edu.illinois.cs.cogcomp.xlwikifier.core.WordEmbedding;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.TACReader;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.WikiDataReader;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.DocumentReader;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.MentionReader;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.mlner.classifier.MentionTypeClassifier;
import edu.illinois.cs.cogcomp.mlner.classifier.FiveTypeClassifier;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreebaseSearch;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.QueryMQL;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.SearchResult;
import edu.illinois.cs.cogcomp.xlwikifier.Utils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 1/19/16.
 * This class re-produces the results on TAC data in our NAACL paper (Table 5)
 */
public class TAC2015Exp {
    private Evaluator eval;
    private MentionReader mr;
    private DocumentReader dr;
    private WordEmbedding we;
    private FreebaseSearch fb = new FreebaseSearch();
    private QueryMQL qm = new QueryMQL();
    private LangLinker ll = new LangLinker();

    public TAC2015Exp(){
        eval = new Evaluator();
        mr = new MentionReader();
        dr = new DocumentReader();
    }

    public Ranker trainRanker(String lang, int n_docs, double ratio){
        Ranker ranker = new Ranker(lang);
        ranker.fm.ner_mode = false;
//        ranker.fm.use_foreign_title = false; // only for tac exps
        String model = Utils.getTime()+"."+lang+".tac";
        List<QueryDocument> docs = dr.readWikiDocsNew(lang, 0, n_docs);
//        for(QueryDocument doc: docs) {
//            doc.mentions = doc.mentions.stream().filter(x -> ranker.qualifiedMention(x, lang)).collect(toList());
//        }

        FreeBaseQuery.loadDB(true);
        WikiCandidateGenerator wcg = new WikiCandidateGenerator(true);
        wcg.genCandidates(docs, lang);
        wcg.selectMentions(docs, ratio);
        ranker.train(docs, model);
//        train_docs = docs;
        return ranker;
    }

    public List<ELMention> extractAuthors(List<QueryDocument> docs){
        List<ELMention> ret = new ArrayList<>();
        for(QueryDocument doc: docs){
            Pattern pattern = Pattern.compile(" author=\"(.*?)\"");

            Matcher matcher = pattern.matcher(doc.getXmlText());
            while(matcher.find()){
                String mention = matcher.group(1);
                int start = matcher.start(1);
                int end = matcher.end(1);
                while(mention.startsWith(" ")) {
                    start++;
                    mention = mention.substring(1);
                }
                while(mention.endsWith(" ")) {
                    end--;
                    mention = mention.substring(0, mention.length()-1);
                }

                ELMention m = new ELMention(doc.getDocID(), start, end);
                m.setMention(mention);
                m.setType("PER");
                m.setMid("NIL");
                ret.add(m);
            }
        }
        System.out.println("Extracted "+ret.size()+" authors in xml");
        return ret;
    }

    /**
     * Only used in Spanish
     * @param docs
     * @param golds
     * @return
     */
    private List<ELMention> getGoldAuthors(List<QueryDocument> docs, List<ELMention> golds){
        List<ELMention> am = extractAuthors(docs);

        Map<String, List<ELMention>> key2m = golds.stream().collect(groupingBy(x -> x.getStartOffset() + "_" + x.getEndOffset() + "_" + x.getDocID()));
        List<ELMention> ret = new ArrayList<>();

        for(ELMention a: am){
            String key = a.getStartOffset()+"_"+a.getEndOffset()+"_"+a.getDocID();
            if(key2m.containsKey(key)) {
                a.gold_mid = key2m.get(key).get(0).gold_mid;
                a.setMid("NIL");
                ret.add(a);
            }
        }
        System.out.println("Get "+ret.size()+" matched gold authors");
        return ret;
    }

    /**
     * Set the Mids by the wikified titles
     */
    public void solveByWikiTitle(List<QueryDocument> docs, String lang){
        boolean search = true;
        System.out.println("Solving by wikipedia titles...");
        for(QueryDocument doc: docs) {
            for (ELMention m : doc.mentions) {

                if (m.getWikiTitle().startsWith("NIL")){
                    m.setMid("NIL");
                }
                else {
                    String mid = getMidByWikiTitle(m.getWikiTitle(), fb, qm, ll, lang, search);
                    if (mid != null)
                        m.setMid(mid);
                    else
                        m.setMid("NIL");
                }

//                for(WikiCand c: m.getCandidates()){
//                    String mid = getMidByWikiTitle(c.getTitle(), fb, qm, ll, lang, search);
//                    c.orig_title = c.title;
//                    if(mid!=null)
//                        c.title = mid;
//                    else
//                        c.title = "NIL";
//                }
            }
        }
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

    public String getMidByWikiTitle(String title, FreebaseSearch fb, QueryMQL qm, LangLinker ll, String lang, boolean search){
        if(title.trim().isEmpty())
            return null;
        if(title.startsWith("NIL"))
            return null;
        String ent = ll.translateToEn(title, lang);
        title = formatTitle(title);
        if(lang.equals("zh")) lang = "zh-cn";
        String mid = qm.lookupMidFromTitle(title, lang);
        if (mid != null) {
            mid = mid.substring(1).replaceAll("/", ".");
            return mid;
        }

        if(ent!=null){
            ent = formatTitle(ent);
            mid = qm.lookupMidFromTitle(ent, "en");
            if (mid != null) {
                mid = mid.substring(1).replaceAll("/", ".");
                return mid;
            }
        }

        if(search && (!lang.equals("uz") && !lang.equals("ha") && !lang.equals("bn")
            && !lang.equals("yo")&& !lang.equals("ta"))) {
            List<SearchResult> answers = fb.lookup(title, lang, "");
            if (answers.size() > 0) {
                return answers.get(0).getMid().substring(1).replaceAll("/", ".");
            }
        }

//        if(search) {
//            List<SearchResult> answers = fb.lookup(title, "en", "");
//            if (answers.size() > 0) {
//                return answers.get(0).getMid().substring(1).replaceAll("/", ".");
//            }
//        }
//
//        if(ent!=null)
//            if(search) {
//                List<SearchResult> answers = fb.lookup(ent, "en", "");
//                if (answers.size() > 0) {
//                    return answers.get(0).getMid().substring(1).replaceAll("/", ".");
//                }
//            }

        return null;
    }


    private List<QueryDocument> loadSpanishDocsWithMentions(boolean train){
        List<QueryDocument> docs;
        if(train)
            docs = dr.readSpanishDocuments();
        else
            docs = dr.readSpanishTestDocuments();
        for(QueryDocument doc: docs) {
            List<ELMention> nm = new ArrayList<>();
            List<ELMention> authors = new ArrayList<>();
            Set<String> seen_off = new HashSet<>();
            for(ELMention m: doc.mentions){
                Pair<Integer, Integer> offsets = doc.getPlainOffsets(m);
                if(offsets!=null) {
//                    if(seen_off.contains(offsets.getFirst()+"_"+offsets.getSecond()))
//                        continue;
                    m.setStartOffset(offsets.getFirst());
                    m.setEndOffset(offsets.getSecond());
                    nm.add(m);
                    seen_off.add(offsets.getFirst()+"_"+offsets.getSecond());
                }
                else{
                    if(!m.getGoldMid().startsWith("NIL"))
                        System.out.println("Failed to map mention: "+m.getMention()+" "+m.getType());
                    else{
                        authors.add(m);
                    }
                }
            }

            List<ELMention> nnm = new ArrayList<>();
            List<ELMention> na = new ArrayList<>();
            Set<String> anames = authors.stream().map(x -> x.getMention().toLowerCase()).collect(toSet());
            for(ELMention m: nm){
                if(anames.contains(m.getMention().toLowerCase()))
                    na.add(m);
                else
                    nnm.add(m);
            }

            authors.addAll(na);

            doc.mentions = nnm;
            doc.authors = authors;
        }
        return docs;
    }

    public void runZH(){
//        we = new WordEmbedding();
//        we.loadMultiDBNew("zh");
        Ranker ranker = trainRanker("zh", 1000, 4);
//        Ranker ranker = trainRankerTACZH();

        List<QueryDocument> docs = TACReader.loadZHDocsWithPlainMentions(false);

        CrossLingualWikifier wikifier = new CrossLingualWikifier("zh", new WikiCandidateGenerator(true));
        wikifier.setRanker(ranker);
        wikifier.wikify(docs);
        solveByWikiTitle(docs, "zh");

        FiveTypeClassifier fiveTypeClassifier = new FiveTypeClassifier();
        fiveTypeClassifier.train(false);
        MentionTypeClassifier mc = new MentionTypeClassifier("zh");
        mc.train();
        for(QueryDocument doc: docs){
            for(ELMention mention: doc.mentions) {
                if (!mention.getMid().startsWith("NIL")) {
                    mention.pred_type = fiveTypeClassifier.getCoarseType(mention.getMid());
                }
                else
                    mention.pred_type = mc.predictType(mention, doc.plain_text);
            }
        }

        for(QueryDocument doc: docs){
            doc.authors.forEach(x -> x.setMid("NIL"));
            for(ELMention m: doc.authors){
                boolean get = false;
                for(ELMention m1: doc.authors){
                    if(!m.getMention().equals(m1.getMention()) && m1.getMention().contains(m.getMention())){
                        m.pred_type = "GPE";
                        get = true;
                        break;
                    }
                }
                if(!get) m.pred_type = "PER";
            }
//            doc.authors.forEach(x -> x.pred_type = "PER");
            doc.mentions.addAll(doc.authors);
        }

        eval.evaluateMid(docs);
    }
    public void runES(){
//        we = new WordEmbedding();
//        we.loadMultiDBNew("es");
        Ranker ranker = trainRanker("es", 1000, 3);
//        Ranker ranker = trainRankerTACSPA();

        List<ELMention> mentions = mr.readMentionCache("/shared/bronte/ctsai12/multilingual/2015data/gold.test.mentions.cands");
        List<ELMention> gold_mentions = mr.readTestMentionsSpa();

//        Set<String> keyset = mentions.stream().map(x -> x.getStartOffset() + " " + x.getEndOffset() + " " + x.getDocID()).collect(toSet());
//        List<ELMention> authors = gold_mentions.stream().filter(x -> !keyset.contains(x.getStartOffset() + " " + x.getEndOffset() + " " + x.getDocID())).collect(toList());
        for(ELMention m: gold_mentions){
            m.setStartOffset(m.getStartOffset()+39);
            m.setEndOffset(m.getEndOffset()+39);
        }
        List<QueryDocument> docs = dr.readSpanishTestDocuments();
        List<ELMention> authors = getGoldAuthors(docs, gold_mentions);
        for(QueryDocument doc: docs) {
            doc.mentions = mentions.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(toList());
            List<ELMention> nm = new ArrayList<>();
            for(ELMention m: doc.mentions){
                m.setStartOffset(m.getStartOffset()+39);
                m.setEndOffset(m.getEndOffset()+39);
                Pair<Integer, Integer> offsets = doc.getPlainOffsets(m);
                if(offsets!=null) {
                    m.setStartOffset(offsets.getFirst());
                    m.setEndOffset(offsets.getSecond());
                    nm.add(m);
                }
            }
            doc.mentions = nm;
        }

//        List<QueryDocument> docs = loadSpanishDocsWithMentions(false);

//        ranker.setMidByModel(docs, "es");

        WikiCandidateGenerator wcg = new WikiCandidateGenerator(true);
        wcg.genCandidates(docs, "es");
//        QueryMQL qm = new QueryMQL();
//        for(QueryDocument doc: docs){
//            for(ELMention m: doc.mentions){
////                if(m.getCandidates().size()==0)
//                    setCandsByFBSearch(m, "es", qm, we);
//            }
//        }

        ranker.setWikiTitleByModel(docs);
        solveByWikiTitle(docs, "es");

		FiveTypeClassifier fiveTypeClassifier = new FiveTypeClassifier();
		fiveTypeClassifier.train(false);
        MentionTypeClassifier mc = new MentionTypeClassifier("es");
        mc.train();
		for(QueryDocument doc: docs){
			for(ELMention mention: doc.mentions) {
				if (!mention.getMid().startsWith("NIL")) {
					mention.pred_type = fiveTypeClassifier.getCoarseType(mention.getMid());
				}
                else
                    mention.pred_type = mc.predictType(mention, doc.plain_text);
			}
		}
//        for(QueryDocument doc: docs) {
//            doc.authors.forEach(x -> x.pred_type = "PER");
//            for(ELMention m: doc.authors){
//                if(!m.getType().equals("PER"))
//                    System.out.println("not per: "+m.getMention()+" "+m.getType()+" "+m.gold_mid);
//            }
//            doc.authors.forEach(x -> x.setMid("NIL"));
//            doc.mentions.addAll(doc.authors);
//        }

        authors.forEach(x -> x.pred_type = "PER");
        final List<ELMention> finalAuthors1 = authors;
        docs.forEach(x -> x.mentions.addAll(finalAuthors1.stream().filter(y -> y.getDocID().equals(x.getDocID())).collect(toList())));
        eval.evaluateMid(docs);
    }

    public static void main(String[] args) {
        TAC2015Exp te = new TAC2015Exp();
        te.runES();
        te.runZH();
    }

}
