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
    private MulticlassClassifier mc;
    Set<String> goodtypes = null;
    private List<Pair<FeatureVector, Integer>> training_ins = new ArrayList<>();
//    public ExecutorService executor = Executors.newSingleThreadExecutor();
    private ExecutorService executor = Executors.newFixedThreadPool(8);
    private ExecutorService executor1 = Executors.newFixedThreadPool(10);
    private String lang;
    private List<String> label2type = new ArrayList<>();

    public NERClassifier(String lang){
        fm = new NERFeatureManager(lang);
        SparseAveragedPerceptron.Parameters p = new SparseAveragedPerceptron.Parameters();
        p.learningRate = 0.1;
        p.thickness = 1.0;
        SparseAveragedPerceptron baseLTU = new SparseAveragedPerceptron(p);
        mc = new MulticlassClassifier(baseLTU);
//        MentionReader mr = new MentionReader();
//        try {
//            goodtypes = mr.getTopTrainTypes(2).entrySet().stream().flatMap(x -> x.getValue().stream()).collect(toSet());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }


    public void loadMultiEmbed(String lang){

        fm.we.loadMultiDBNew(lang);
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
//                int[] idx = fv.getIdx();
//                double[] values = fv.getValue();
//                for(int j = 0; j < idx.length; j++){
//                    out += "\t"+idx[j]+":"+values[j];
//                }
                out += "\n";
//                System.out.println("c: "+(System.currentTimeMillis() - start)+" "+fv.maxIdx());
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

    public void addTrainingInstances(List<QueryDocument> docs){
        System.out.println("Adding Traning data");
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions) {
                if (m.getType() == null) m.setType("O");
                if (m.is_ne) continue;
                if ((!m.getWikiTitle().startsWith("NIL") || !m.getMid().startsWith("NIL")) && !m.is_stop) {
                    if(m.ner_features == null) {
                        System.out.println("nil features " + m.getMention() + " " + m.getDocID());
                        System.exit(-1);
                    }
                    FeatureVector fv = fm.getFV(m.ner_features);
//                        int label = m.is_ne_gold? 1:2;
                    int label = type2Label(m.getType());
                    training_ins.add(new Pair<>(fv, label));
                }
            }
        }
    }

    public void train(){
        System.out.println("#training ins: "+training_ins.size());
        mc.learn(training_ins, 30);
        training_ins = null;
//        mc.writeModel("nerclassifier.model");
    }

    public void train(String dir) {
        System.out.println("Training NER classifier...");
        Gson gson = new Gson();
        //String dir = "/shared/bronte/ctsai12/multilingual/2015data/ner-training-cache3/";
        File df = new File(dir);
        List<Pair<FeatureVector, Integer>> train_data = new ArrayList<>();
        int cnt = 0;
        for(File f: df.listFiles()){
            System.out.println(cnt++);
            try {
                String json = FileUtils.readFileToString(f);
                QueryDocument doc = gson.fromJson(json, QueryDocument.class);
                for(int j = 0; j < doc.mentions.size(); j++){
                    ELMention m = doc.mentions.get(j);
                    if(m.getType()==null) m.setType("O");
                    if((!m.getWikiTitle().startsWith("NIL") || !m.getMid().startsWith("NIL")) && !m.is_stop){
//                        if(m.getMention().equals("USA") || m.getMid().contains("09c7w0")){
//                            System.out.println(m.getMention()+" "+m.getWikiTitle()+" "+m.getMid()+" "+m.is_ne_gold);
//                        }
                        FeatureVector fv = getFeatureVector(doc.mentions, j, true);
//                        int label = m.is_ne_gold? 1:2;
                        int label = type2Label(m.getType());
                        train_data.add(new Pair<>(fv, label));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Learning...");
        mc.learn(train_data, 30);
    }

    private int type2Label(String type){

        int label = label2type.indexOf(type);
        if(label == -1){
            label2type.add(type);
            label = label2type.size()-1;
        }
        return label;

//        if(type.equals("O"))
//            return 0;
//        else if(type.equals("B-PER"))
//            return 1;
//        else if(type.equals("B-ORG"))
//            return 2;
//        else if(type.equals("B-LOC"))
//            return 3;
//        else if(type.equals("B-MISC"))
//            return 4;
//        else if(type.equals("I-PER"))
//            return 5;
//        else if(type.equals("I-ORG"))
//            return 6;
//        else if(type.equals("I-LOC"))
//            return 7;
//        else if(type.equals("I-MISC"))
//            return 8;
//
//        System.out.println("Unknow entity type: "+type);
//        System.exit(-1);
//        return -1;
    }

    private String label2Type(int label){
        return label2type.get(label);
//        if(label == 0)
//            return "O";
//        else if(label == 1)
//            return "B-PER";
//        else if(label == 2)
//            return "B-ORG";
//        else if(label == 3)
//            return "B-LOC";
//        else if(label == 4)
//            return "B-MISC";
//        else if(label == 5)
//            return "I-PER";
//        else if(label == 6)
//            return "I-ORG";
//        else if(label == 7)
//            return "I-LOC";
//        else if(label == 8)
//            return "I-MISC";
//        else
//            return "O";

//        System.out.println("Unknown label "+label);
//        System.exit(-1);
//        return null;
    }

    public void labelMentions(List<QueryDocument> docs){
//        waitFeatureExecutor();
        for(QueryDocument doc: docs){
            for(int j = 0; j < doc.mentions.size(); j++){
                ELMention m = doc.mentions.get(j);
                if(m.is_ne) continue;
                if(m.is_stop){
                    m.is_ne = false;
                    continue;
                }

                if(m.getMid().startsWith("NIL") && m.getWikiTitle().startsWith("NIL")) {
                    m.is_ne = false;
                    continue;
                }

//                Set<String> types = new HashSet<>(qm.lookupTypeFromMid(m.getMid()));
//                types.retainAll(goodtypes);
//                if(types.size()>0){
//                    m.is_ne = true;
//                    continue;
//                }

//                FeatureVector fv = getFeatureVector(doc.mentions, j, false);
                FeatureVector fv = fm.getFV(m.ner_features);
                int label = mc.getLabel(fv);
                m.pred_type = label2Type(label);
                m.is_ne = !m.pred_type.equals("O");
            }
        }
    }

    private Map<String, Double> getFeatureMap(List<ELMention> mentions, int idx, boolean train){
        int window = 3;
        List<ELMention> before_mentions = new ArrayList<>();
        List<ELMention> after_mentions = new ArrayList<>();
        ELMention m = mentions.get(idx);
        for(int i = idx - 1; i >=0 && before_mentions.size() < window; i --) {
//            if(m.ngram == mentions.get(i).ngram)
            before_mentions.add(mentions.get(i));
        }
        for(int i = idx+1; i < mentions.size() && after_mentions.size() < window; i++) {
//            if(m.ngram == mentions.get(i).ngram)
            after_mentions.add(mentions.get(i));
        }
        return fm.getFeatureMap(m, before_mentions, after_mentions, train);
    }

    private FeatureVector getFeatureVector(List<ELMention> mentions, int idx, boolean train){
        Map<String, Double> map = getFeatureMap(mentions, idx, train);
        return fm.getFV(map);
    }

    public static void main(String[] args) {

        NERClassifier bc = new NERClassifier(null);
        String dir = "/shared/bronte/ctsai12/multilingual/2015data/ner-training-cache-uni-linker/";
        bc.train(dir);
//        bc.getExampleCache();
//        bc.getNegativeExamples();
//        bc.train();
//        System.out.println(bc.isTheTypes("m.0hf35"));
        System.exit(-1);
    }


}
