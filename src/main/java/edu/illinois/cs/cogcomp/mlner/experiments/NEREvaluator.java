package edu.illinois.cs.cogcomp.mlner.experiments;

import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 3/1/16.
 */
public class NEREvaluator {


    private int correct = 0, pp = 0, gp = 0, total = 0;
    private int correct1 = 0, pp1 = 0, gp1 = 0, total1 = 0;
    private int correct2 = 0, correct3 = 0, correct4 = 0, correct5=0;
    private String name;
//    private MentionTypeClassifier ntc = new MentionTypeClassifier();
    public NEREvaluator(String name){
        this.name = name;
//        ntc.train("/shared/bronte/ctsai12/multilingual/2015data/conll-en-train1/uni/");
    }


    public void eval(List<ELMention> mentions){
        for(ELMention m: mentions){
//            String type = ntc.getType(m);
//            m.pred_type = "B-"+type;
            if(m.is_ne!=m.is_ne_gold)
                System.out.println(m.getMention()+" "+m.getStartOffset()+" "+m.getEndOffset()+" "+m.is_ne+" "+m.is_ne_gold+" "+m.getWikiTitle()+" "+m.gold_mid);
            if(m.is_ne_gold)
                gp++;
            if(m.is_ne)
                pp++;
            if(m.is_ne_gold && m.is_ne) {
                correct++;
                if(m.pred_type.equals(m.getType()))
                    correct3++;
            }
            total++;
        }
    }

    public List<ELMention> getPredictions(List<ELMention> mentions){

        List<ELMention> ret = new ArrayList<>();
        String surface = null;
        List<String> types = new ArrayList<>();
        int start = -1, end = -1;
        int ngram = 0;
        for(ELMention m: mentions){
            if(m.is_ne){
                if(surface == null){
                    surface = m.getMention();
                    start = m.getStartOffset();
                    end = m.getEndOffset();
                }
                else{
                    surface += " "+m.getMention();
                    end = m.getEndOffset();
                }
                types.add(m.pred_type.substring(2));
                ngram += m.ngram;
            }
            else{
                if(surface != null){
                    ELMention p = new ELMention("", start, end);
                    p.setMention(surface);
                    p.types = types;
                    p.ngram = ngram;
                    ret.add(p);
                    surface = null; start = -1; end = -1;
                    types = new ArrayList<>();
                    ngram = 0;
                }
            }
        }

        if(surface != null){
            ELMention p = new ELMention("", start, end);
            p.setMention(surface);
            p.types = types;
            p.ngram = ngram;
            ret.add(p);
        }

        return ret;
    }


    public void printResults(){
        System.out.println("Token Level");
        double prec = (double) correct / pp;
        double reca = (double) correct / gp;
        double f1 = 2 * prec * reca / (prec + reca);
        System.out.println("#correct "+correct+" #preds:"+pp+" #golds:"+gp+" "+total);
        System.out.println("precision: "+prec+" recall: "+reca+" f1: "+f1);
        prec = (double) correct3 / pp;
        reca = (double) correct3 / gp;
        f1 = 2 * prec * reca / (prec + reca);
        System.out.println("#correct "+correct3);
        System.out.println("precision: "+prec+" recall: "+reca+" f1: "+f1);

        System.out.println("Phrase Level");
        prec = (double) correct1 / pp1;
        reca = (double) correct1 / gp1;
        f1 = 2 * prec * reca / (prec + reca);
        System.out.println("#correct "+correct1+" #preds:"+pp1+" #golds:"+gp1);
        System.out.println("precision: "+prec+" recall: "+reca+" f1: "+f1);

        prec = (double) correct2 / pp1;
        reca = (double) correct2 / gp1;
        f1 = 2 * prec * reca / (prec + reca);
        System.out.println("#correct + type: "+correct2);
        System.out.println("precision: "+prec+" recall: "+reca+" f1: "+f1);

        prec = (double) correct4 / pp1;
        reca = (double) correct4 / gp1;
        f1 = 2 * prec * reca / (prec + reca);
        System.out.println("#correct + type: "+correct4);
        System.out.println("precision: "+prec+" recall: "+reca+" f1: "+f1);

//        prec = (double) correct5 / pp1;
//        reca = (double) correct5 / gp1;
//        f1 = 2 * prec * reca / (prec + reca);
//        System.out.println("#correct + type: "+correct5);
//        System.out.println("precision: "+prec+" recall: "+reca+" f1: "+f1);
    }
}
