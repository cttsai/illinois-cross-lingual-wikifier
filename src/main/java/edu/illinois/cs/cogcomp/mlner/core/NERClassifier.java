package edu.illinois.cs.cogcomp.mlner.core;

import LBJ3.learn.SparseAveragedPerceptron;
import com.google.gson.Gson;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;
import structure.MulticlassClassifier;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.joining;

/**
 * It's only used for generating wikifier features.
 * We use illinois-ner as the classifier instead.
 * Created by ctsai12 on 9/29/15.
 */
public class NERClassifier {
    private NERFeatureManager fm;
    private ExecutorService executor = Executors.newFixedThreadPool(8);
    private ExecutorService executor1 = Executors.newFixedThreadPool(10);

    public NERClassifier(String lang){
        fm = new NERFeatureManager(lang);
    }

    public void extractNERFeatures(QueryDocument doc){
        for(int j = 0; j < doc.mentions.size(); j++){
            ELMention m = doc.mentions.get(j);
            if(m.ner_features.size() > 0)
                continue;
            if(NumberUtils.isNumber(m.getMention().trim()))
                continue;
            Map<String, Double> map = getFeatureMap(doc.mentions, j, true);
            for(String key: map.keySet()){
                m.ner_features.put(key, map.get(key));

            }
        }
    }

    public class FeatureExtractor implements Runnable {

        private QueryDocument doc;
        private int n;
//        private NERClassifier nc;

        public FeatureExtractor(QueryDocument doc, int n) {
            this.doc = doc;
            this.n = n;
//            this.nc = new NERClassifier();
        }

        @Override
        public void run() {
            extractNERFeatures(doc);
        }
    }




    public class FeatureWriter implements Runnable {

        private QueryDocument doc;
        private TextAnnotation ta;
        private String outpath;
        private boolean train;

        public FeatureWriter(QueryDocument doc, TextAnnotation ta, String outpath, boolean train) {
            this.doc = doc;
            this.ta = ta;
            this.outpath = outpath;
            this.train = train;
        }

        @Override
        public void run() {
            String out = "";

            if(doc.mentions == null){
                System.out.println("mentions null! "+doc.getDocID());
            }
            if(ta == null){
                System.out.println("ta null! "+doc.getDocID());
            }
            if(ta.getTokens() == null)
                System.out.println("tokens null! "+doc.getDocID());
            if(ta.getTokens().length!=doc.mentions.size()){
                System.out.println("Size doesn't match!");
                System.out.println(ta.getTokens().length+" "+doc.mentions.size());
            }
            if(doc.wikifier_features!=null && doc.wikifier_features.size()!=ta.getTokens().length){
                System.out.println("Wikifier features size doesn't match");
                System.out.println(ta.getTokens().length+" "+doc.wikifier_features.size());
            }

            int midx = 0;
            for(int i = 0; i < ta.getTokens().length; i++){
                IntPair offsets = ta.getTokenCharacterOffset(i);
                ELMention m = null;
                int mention_id = -1;
                for(int j = midx; j < doc.mentions.size(); j++) {
                    ELMention mention = doc.mentions.get(j);
                    if (offsets.getFirst() == mention.getStartOffset() && offsets.getSecond() == mention.getEndOffset()) {
                        m = mention;
                        mention_id = j;
                        midx = j+1;
                        break;
                    }
                }
//                System.out.println("a: "+(System.currentTimeMillis() - start));
//                start = System.currentTimeMillis();

                if(m == null){
                    System.out.println("cound't match "+offsets+" "+ta.getToken(i));
                    continue;
                }

                if(i>0 && ta.getSentenceId(i) != ta.getSentenceId(i-1)) {
                    out += "\n";
                }

//                Map<String, Double> fmap = getFeatureMap(doc.mentions, mention_id, train);
                Map<String, Double> fmap = m.ner_features;
//                System.out.println("b: "+(System.currentTimeMillis() - start));
//                start = System.currentTimeMillis();
                out += m.getType()+"\tx\tx\tx\tx";
                out += "\t"+m.getMention()+"\tx\tx\tx\tx";
                if(doc.wikifier_features!=null && !doc.wikifier_features.get(i).trim().isEmpty())
                    out += "\t"+doc.wikifier_features.get(i);
                for(String key: fmap.keySet())
                        out += "\t"+key+":"+fmap.get(key);
                out += "\n";
            }

            try {
                FileUtils.writeStringToFile(new File(outpath, doc.getDocID()), out, "utf-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
//            doc = null;
//            ta = null;
        }
    }

    public void writeFeatures(List<QueryDocument> docs, List<TextAnnotation> tas, String outfile, boolean train){
        System.out.println("Writing features to "+outfile+"...");
        Map<String, TextAnnotation> did2ta = new HashMap<>();
        for(TextAnnotation ta: tas) {
            did2ta.put(ta.getId(), ta);
        }
        executor1 = Executors.newFixedThreadPool(20);
        for(QueryDocument doc: docs) {
            executor1.execute(new FeatureWriter(doc, did2ta.get(doc.getDocID()), outfile, train));
        }
        executor1.shutdown();
        try {
            executor1.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void extractFeatures(List<QueryDocument> docs, int n){
        System.out.print("Extracting features...");
        executor = Executors.newFixedThreadPool(10);
        for(QueryDocument doc: docs){
            executor.execute(new FeatureExtractor(doc, n));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Done");
    }

    private Map<String, Double> getFeatureMap(List<ELMention> mentions, int idx, boolean train){
        int window = 3;
        List<ELMention> before_mentions = new ArrayList<>();
        List<ELMention> after_mentions = new ArrayList<>();
        ELMention m = mentions.get(idx);
        for(int i = idx - 1; i >=0 && before_mentions.size() < window; i --) {
            before_mentions.add(mentions.get(i));
        }
        for(int i = idx+1; i < mentions.size() && after_mentions.size() < window; i++) {
            after_mentions.add(mentions.get(i));
        }
        return fm.getFeatureMap(m, before_mentions, after_mentions, train);
    }

    public static void main(String[] args) {

    }


}
