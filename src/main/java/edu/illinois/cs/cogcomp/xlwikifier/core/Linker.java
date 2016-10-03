package edu.illinois.cs.cogcomp.xlwikifier.core;

import LBJ3.learn.SparseAveragedPerceptron;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;
import structure.MulticlassClassifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by ctsai12 on 2/11/16.
 */
public class Linker {

    private Ranker ranker;
    private MulticlassClassifier mc;
    private LinkerFeatureManager lfm;

    public Linker(Ranker ranker){
        this.ranker = ranker;
        lfm = new LinkerFeatureManager();
        SparseAveragedPerceptron.Parameters p = new SparseAveragedPerceptron.Parameters();
        p.learningRate = 0.1;
        p.thickness = 1.0;
        SparseAveragedPerceptron baseLTU = new SparseAveragedPerceptron(p);
        mc = new MulticlassClassifier(baseLTU);
    }

    public void trainLinker(List<QueryDocument> docs){
        ranker.setWikiTitleByModel(docs);

        List<Pair<FeatureVector, Integer>> examples = new ArrayList<>();
        int pos = 0, neg = 0;
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                if(m.getCandidates().size() == 0) continue;
                FeatureVector fv = lfm.getFV(m, doc);
//                if(!m.gold_wiki_title.startsWith("NIL")){
                if(m.getWikiTitle().toLowerCase().equals(m.gold_wiki_title.toLowerCase())){
                    examples.add(new Pair(fv, 1));
                    pos++;
                }
                else{
                    examples.add(new Pair(fv, 0));
                    neg++;
                }
            }
        }
        System.out.println("Linker: pos"+pos+" neg:"+neg);
        Collections.shuffle(examples);
//        writeSVMFile(examples, "linker.svm");
//        fiveFoldCV(examples);
        mc.learn(examples, 20);
//        mc.writeModel("linker-model");
    }

    private void fiveFoldCV(List<Pair<FeatureVector, Integer>> examples){
        int fold_size = examples.size() / 5;
        for(int i = 0; i < 5; i++){
            List<Pair<FeatureVector, Integer>> test_exp =  new ArrayList<>();
            List<Pair<FeatureVector, Integer>> train_exp =  new ArrayList<>();
            for(int j = 0; j < examples.size(); j++) {
                if(j>=i * fold_size && j < (i + 1) * fold_size){
                    test_exp.add(examples.get(j));
                }
                else train_exp.add(examples.get(j));
            }
            mc.learn(train_exp, 100);
            mc.test(test_exp);
        }
    }

    private void writeSVMFile(List<Pair<FeatureVector, Integer>> examples, String filename){
        String out = "";
        for(Pair<FeatureVector, Integer> exp: examples){
            int[] idx = exp.getFirst().getIdx();
            double[] val = exp.getFirst().getValue();
            int label = exp.getSecond();
            if(idx.length == 0 ) continue;
            out += label;
            for (int i = 0; i < idx.length; i++) {
                double v = val[i];
                out += " " + idx[i] + ":" + String.format("%.7f", v);
            }
            out += "\n";
        }

        try {
            FileUtils.writeStringToFile(new File(filename), out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void apply(List<QueryDocument> docs){
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
//                if(m.getCandidates().size()>0 && m.getCandidates().get(0).ranker_feats.get("PRESCORE") > 0.5)
//                    continue;
                FeatureVector fv = lfm.getFV(m, doc);
                int label = mc.getLabel(fv);
                if(label == 0)
                    m.setWikiTitle("NIL");
            }
        }
    }

    public void applyThresholdLinker(List<QueryDocument> docs){
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                if(m.getCandidates().size()>0 && m.getCandidates().get(0).score < 0.4)
                    m.setWikiTitle("NIL");
            }
        }
    }
}
