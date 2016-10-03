package edu.illinois.cs.cogcomp.mlner.classifier;

import LBJ3.learn.SparseAveragedPerceptron;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.*;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.MentionReader;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.TACReader;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import structure.MulticlassClassifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 9/12/15.
 */
public class FiveTypeClassifier {

    private FeatureManager fm;
    private List<String> label2type = new ArrayList<>();
    private MulticlassClassifier mc;

    public FiveTypeClassifier(){
        fm = new FeatureManager();
        SparseAveragedPerceptron.Parameters p = new SparseAveragedPerceptron.Parameters();
        p.learningRate = 0.1;
        p.thickness = 1;
        SparseAveragedPerceptron baseLTU = new SparseAveragedPerceptron(p);
        mc = new MulticlassClassifier(baseLTU);
        if(!FreeBaseQuery.isloaded())
            FreeBaseQuery.loadDB(true);
    }

    public void train(boolean dev){
        MentionReader mr = new MentionReader();
        List<ELMention> mentions = mr.readTrainingMentions();
        Map<String, List<ELMention>> id2ms = mentions.stream().filter(x -> !x.gold_mid.startsWith("NIL"))
                .collect(groupingBy(x -> x.gold_mid, toList()));
        Set<String> spa_ids = mentions.stream().filter(x -> x.getLanguage().equals("SPA"))
                .filter(x -> !x.gold_mid.startsWith("NIL"))
                .map(x -> x.gold_mid).collect(toSet());
//        System.out.println("#ids "+id2ms.keySet().size());
//        System.out.println("# spa ids "+spa_ids.size());

        List<Pair<FeatureVector, Integer>> train_data = new ArrayList<>();
        List<Pair<FeatureVector, Integer>> dev_data = new ArrayList<>();
        for(String id: id2ms.keySet()) {
            Set<String> types = id2ms.get(id).stream().map(x -> x.getType()).collect(toSet());
            if (types.size() > 1)
                continue;
            String type = (String) types.toArray()[0];
            int label = getLabel(type);
            FeatureVector fv = fm.getFV(id);
            if(spa_ids.contains(id))
                dev_data.add(new Pair<>(fv, label));
            else
                train_data.add(new Pair<>(fv, label));
        }
        if(!dev) train_data.addAll(dev_data);
//        System.out.println("#train ins: "+train_data.size());
//        System.out.println("#dev ins: "+dev_data.size());
//        System.out.println("#classes: "+label2type.size());
//        System.out.println(train_data.stream().collect(groupingBy(x -> x.getSecond(), counting())));
//        System.out.println(dev_data.stream().collect(groupingBy(x -> x.getSecond(), counting())));

//        System.out.println("Training on Mids of English and Chinese docs");
        mc.learn(train_data, 100);

        System.out.println("Test on Mids of Spanish docs");
        mc.test(dev_data);
//        System.out.println(dev_data.stream().map(x -> mc.getLabel(x.getFirst()))
//                .collect(groupingBy(x -> x, counting())));
    }


    public String getCoarseType(String mid){
        FeatureVector fv = fm.getFV(mid);
        int label = mc.getLabel(fv);
        return this.label2type.get(label);
    }

    public int getLabel(String type){
        if(!label2type.contains(type)){
            label2type.add(type);
            return label2type.size()-1;
        }
        return label2type.indexOf(type);
    }



    public static void main(String[] args) {
        FiveTypeClassifier c = new FiveTypeClassifier();
        c.train(false);

        List<QueryDocument> docs = TACReader.loadESDocsWithPlainMentions(false);
        int correct = 0, total = 0;
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                if(!m.gold_mid.startsWith("NIL")) {
                    String type = c.getCoarseType(m.gold_mid);
                    if(type.equals(m.getType()))
                        correct++;
                    total++;
                }
            }
        }

        System.out.println("Accuracy: "+(double)correct/total+" "+total);

    }
}
