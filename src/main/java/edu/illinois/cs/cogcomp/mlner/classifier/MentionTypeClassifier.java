package edu.illinois.cs.cogcomp.mlner.classifier;

import LBJ3.learn.SparseAveragedPerceptron;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.TAC2015Exp;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.TACReader;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import structure.MulticlassClassifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ctsai12 on 9/12/15.
 */
public class MentionTypeClassifier {

    private MentionTypeFeatureManager fm;
    private List<String> label2type = new ArrayList<>();
    private MulticlassClassifier mc;

    public MentionTypeClassifier(String lang){
        fm = new MentionTypeFeatureManager(lang);
        SparseAveragedPerceptron.Parameters p = new SparseAveragedPerceptron.Parameters();
        p.learningRate = 0.1;
        p.thickness = 1;
        SparseAveragedPerceptron baseLTU = new SparseAveragedPerceptron(p);
        mc = new MulticlassClassifier(baseLTU);
        if(!FreeBaseQuery.isloaded())
            FreeBaseQuery.loadDB(true);
    }

    public void train(){
        List<QueryDocument> docs = TACReader.loadENDocsWithPlainMentions(true);
        List<Pair<FeatureVector, Integer>> train_data = new ArrayList<>();
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                int label = getLabel(m.getType());
                FeatureVector fv = fm.getFV(m, doc.plain_text);
                train_data.add(new Pair<>(fv, label));
            }
        }
        mc.learn(train_data, 100);
    }

    public void trainZH(){
        TAC2015Exp te = new TAC2015Exp();
        List<QueryDocument> docs = TACReader.loadZHDocsWithPlainMentions(true);

        List<Pair<FeatureVector, Integer>> train_data = new ArrayList<>();
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                int label = getLabel(m.getType());
                FeatureVector fv = fm.getFV(m, doc.plain_text);
                train_data.add(new Pair<>(fv, label));
            }
        }
        mc.learn(train_data, 100);
    }

    public void test(List<QueryDocument> docs){
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                String type = getType(m, doc.plain_text);
                m.pred_type = type;
            }
        }
    }

    public String predictType(ELMention m, String text){
        return getType(m, text);
    }

    public void evalTypes(List<QueryDocument> docs){
        int correct = 0, total = 0;
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                if(m.gold_mid.startsWith("NIL")) {
                    if (m.getType().equals(m.pred_type))
                        correct++;
                    total++;
                }
            }
        }
        System.out.println("Accuracy: "+(double)correct/total+" "+total);
    }



    public String getType(ELMention m, String text){
        FeatureVector fv = fm.getFV(m, text);
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
        MentionTypeClassifier c = new MentionTypeClassifier("zh");
//        c.train();
//        TACExp te = new TACExp();
//        List<QueryDocument> docs = te.loadTestDocsWithMentions();
//        c.test(docs);
//        c.evalTypes(docs);

        c.trainZH();
        TAC2015Exp te = new TAC2015Exp();
        List<QueryDocument> docs = TACReader.loadZHDocsWithPlainMentions(false);
//        Map<String, Integer> typecnt = new HashMap<>();
//        int cnt= 0, t = 0;
//        for(QueryDocument doc: docs){
//            for(ELMention m: doc.authors){
//                for(ELMention m1: doc.authors){
//                    if(!m.getMention().equals(m1.getMention()) && m1.getMention().contains(m.getMention())){
//                        String type = c.getType(m, doc.plain_text);
//                        if(type.equals(m.getType()))
//                            cnt++;
//                        t++;
//
//                        break;
//                    }
//                }
//            }
//        }
//        System.out.println(cnt+" "+t);
        c.test(docs);
        c.evalTypes(docs);

    }
}
