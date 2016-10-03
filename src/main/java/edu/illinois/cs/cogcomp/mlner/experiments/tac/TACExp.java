package edu.illinois.cs.cogcomp.mlner.experiments.tac;

import edu.illinois.cs.cogcomp.tokenizers.StanfordAnalyzer;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.TAC2015Exp;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.DocumentReader;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.MentionReader;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.mlner.core.NERClassifier;
import edu.illinois.cs.cogcomp.mlner.experiments.NEREvaluator;
import edu.illinois.cs.cogcomp.mlner.core.Utils;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.QueryMQL;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 3/2/16.
 */
public class TACExp {

    private DocumentReader dr = new DocumentReader();
    private MentionReader mr = new MentionReader();
    private Utils utils;
    public TACExp(){
        utils = new Utils();
        utils.setLang("es");
    }

    public List<QueryDocument> loadTrainingDocsWithMentions(){
        List<QueryDocument> docs = dr.readSpanishDocuments();
        List<ELMention> mentions = mr.readTrainingMentionsSpa();
        for(QueryDocument doc: docs){
            doc.mentions = mentions.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
        }
        QueryMQL qm = new QueryMQL();
        for(QueryDocument doc: docs) {
            List<ELMention> nm = new ArrayList<>();
            Set<String> seen_off = new HashSet<>();
            for(ELMention m: doc.mentions){
                Pair<Integer, Integer> offsets = doc.getPlainOffsets(m);
                if(offsets!=null) {
                    if(seen_off.contains(offsets.getFirst()+"_"+offsets.getSecond()))
                        continue;
                    m.setStartOffset(offsets.getFirst());
                    m.setEndOffset(offsets.getSecond());
                    utils.setWikiTitleFromMid(qm, "es", m);
                    nm.add(m);
                    seen_off.add(offsets.getFirst()+"_"+offsets.getSecond());
                }
            }
            doc.mentions = nm;
        }
        return docs;
    }

    public List<QueryDocument> loadTestDocsWithMentions(){
        List<QueryDocument> docs = dr.readSpanishTestDocuments();
        List<ELMention> mentions = mr.readTestMentionsSpa();
        for(QueryDocument doc: docs) {
            doc.mentions = mentions.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(toList());
        }
        for(QueryDocument doc: docs) {
            List<ELMention> nm = new ArrayList<>();
            Set<String> seen_off = new HashSet<>();
            for(ELMention m: doc.mentions){
                m.setStartOffset(m.getStartOffset()+39);
                m.setEndOffset(m.getEndOffset()+39);
                Pair<Integer, Integer> offsets = doc.getPlainOffsets(m);
                if(offsets!=null) {
                    if(seen_off.contains(offsets.getFirst()+"_"+offsets.getSecond()))
                        continue;
                    m.setStartOffset(offsets.getFirst());
                    m.setEndOffset(offsets.getSecond());
                    nm.add(m);
                    seen_off.add(offsets.getFirst()+"_"+offsets.getSecond());
                }
                else{
                    if(!m.getGoldMid().startsWith("NIL"))
                        System.out.println("Failed to map mention: "+m.getMention());
                }
            }
            doc.mentions = nm;
        }
        return docs;
    }

    public void runESNER(){
        List<QueryDocument> docs = loadTestDocsWithMentions();

//        List<QueryDocument> docs = loadTrainingDocsWithMentions();

//        utils.setNgramMention(docs, null, false, 1);
//        utils.setBigramMention(docs, null, false);
//        utils.setTrigramMention(docs, null);

        TAC2015Exp tac = new TAC2015Exp();
        Ranker ranker = tac.trainRanker("es", 1000, 4);
//        Linker linker = tac.trainLinker("es", 1000, 2000, 4, ranker);

//        Linker linker = new Linker(ranker);
//        linker.trainLinker(tac.train_docs);
//        Ranker ranker = trainRankerTACSPA();

        WikiCandidateGenerator wcg = new WikiCandidateGenerator(true);
        NERClassifier nc = new NERClassifier(null);
//		String dir = "/shared/bronte/ctsai12/multilingual/2015data/ner-training-cache-bi-nos/";
        String dir = "/shared/bronte/ctsai12/multilingual/2015data/ner-training-cache-bi-new/";
//        String dir = "/shared/bronte/ctsai12/multilingual/2015data/ner-training-cache3/";
//        nc.train(dir);

        NERClassifier uni_classifier = new NERClassifier(null);
//        dir = "/shared/bronte/ctsai12/multilingual/2015data/ner-training-cache3/";
//        dir = "/shared/bronte/ctsai12/multilingual/2015data/ner-training-cache-uni-new/";
        dir = "/shared/bronte/ctsai12/multilingual/2015data/ner-training-cache-uni-linker/";
        uni_classifier.train(dir);

//        NERClassifier tri_classifier = new NERClassifier();
//        dir = "/shared/bronte/ctsai12/multilingual/2015data/ner-training-cache-tri-new/";
//        tri_classifier.train(dir);



        NEREvaluator bi_eval = new NEREvaluator("bigram");
        NEREvaluator uni_eval = new NEREvaluator("unigram");
        for(int i = 0; i < docs.size(); i++){
            System.out.println("========= Doc "+i+"==========");
            System.out.println(docs.get(i).getDocID());
            List<QueryDocument> tmp_docs = new ArrayList<>();
            tmp_docs.add(docs.get(i));

            // wikify trigrams
//            wcg.genCandidates(tmp_docs, "es");
//            ranker.setWikiTitleByModel(tmp_docs);
//            utils.setMidByWikiTitle(tmp_docs, "es");
//            tri_classifier.labelMentions(tmp_docs);

            // wikify bigrams
//            utils.setBigramMention(tmp_docs, null, true);
//            wcg.genCandidates(tmp_docs, "es");
//            ranker.setWikiTitleByModel(tmp_docs);
//            utils.setMidByWikiTitle(tmp_docs, "es");
//            nc.labelMentions(tmp_docs);

            // wikify unigrams
//            utils.setUnigramMention(tmp_docs, null, true);
//            bi_eval.eval(tmp_docs.get(0).mentions);
//            bi_eval.printResults();
            wcg.genCandidates(tmp_docs, "es");
            ranker.setWikiTitleByModel(tmp_docs);
//            linker.apply(tmp_docs);
            utils.setMidByWikiTitle(tmp_docs, "es");
            uni_classifier.labelMentions(tmp_docs);
            uni_eval.eval(tmp_docs.get(0).mentions);
            uni_eval.printResults();

            docs.set(i, null);
        }
    }

    public void runStanfordNER(){
        StanfordAnalyzer sa = new StanfordAnalyzer();
        List<QueryDocument> docs = loadTestDocsWithMentions();
        List<ELMention> mentions = new ArrayList<>(); //FreeLingNER.extract1(docs);
        int correct = 0, pp = 0, gp = 0;
        for(QueryDocument doc: docs){
            List<ELMention> preds = mentions.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(toList());
            List<ELMention> golds = doc.mentions;
            TextAnnotation ta = sa.getTextAnnotation(doc.plain_text);
            for(int i = 0; i < ta.getTokens().length; i++){

                IntPair offset = ta.getTokenCharacterOffset(i);
                boolean is_ne = false, is_ne_gold = false;
                for(ELMention gold: golds){
                    if(offset.getFirst() >= gold.getStartOffset()
                            && offset.getSecond() <= gold.getEndOffset()){
                        is_ne_gold = true;
                        break;
                    }
                }
                for(ELMention pred: preds){
                    if(offset.getFirst() >= pred.getStartOffset()
                            && offset.getSecond() <= pred.getEndOffset()){
                        is_ne = true;
                        break;
                    }
                }

                if(is_ne && is_ne_gold) correct++;
                if(is_ne) pp++;
                if(is_ne_gold) gp++;
            }
        }
        double prec = (double) correct / pp;
        double reca = (double) correct / gp;
        double f1 = 2 * prec * reca / (prec + reca);
        System.out.println("#correct "+correct+" #preds:"+pp+" #golds:"+gp);
        System.out.println("precision: "+prec+" recall: "+reca+" f1: "+f1);
    }

    public void genTrainingCache(){
        List<QueryDocument> docs = loadTrainingDocsWithMentions();
        String dir = "/shared/bronte/ctsai12/multilingual/2015data/ner-training-cache-uni-linker";
//        utils.genNERTrainingCache(docs, null, dir);
    }

    public static void main(String[] args) {
        TACExp te = new TACExp();
        te.runESNER();
//        te.genTrainingCache();
    }
}
