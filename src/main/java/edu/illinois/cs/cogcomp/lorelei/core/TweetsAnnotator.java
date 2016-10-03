package edu.illinois.cs.cogcomp.lorelei.core;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.lorelei.utils.Conll2TextAnnotation;
import edu.illinois.cs.cogcomp.lorelei.utils.TextAnnotation2Conll;
import edu.illinois.cs.cogcomp.mlner.experiments.conll.ColumnFormatReader;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by ctsai12 on 7/12/16.
 */
public class TweetsAnnotator {
    private static Map<String, Set<String>> gaz = new HashMap<>();
    public static void annotate(QueryDocument doc) {

        TextAnnotation ta = doc.getTextAnnotation();
        if (!ta.hasView(ViewNames.NER_CONLL))
            Conll2TextAnnotation.populatNERView(doc);
        if(!doc.getDocID().contains("_SN_")) return; // not a tweet

        tagAt(ta);
        tagHashtag(ta);
    }

    public static void tagAt(TextAnnotation ta){
        SpanLabelView ner_view = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);

        for(int i = 0; i < ta.size(); i++){
            String token = ta.getToken(i);
            if(!token.startsWith("@")) continue;

            token = token.substring(1).toLowerCase();
            if(token.length() < 3) continue;

            if(ner_view.getConstituentsCoveringToken(i).size() > 0) continue;

            String type = "O";
            if(gaz.get("ORG").contains(token) || token.endsWith("com") || token.substring(0, token.length()-1).endsWith("com")) {
                System.out.println("ORG " + token);
                type = "ORG";
            }else {
                System.out.println("PER " + token);
                type = "PER";

            }
            if(!type.equals("O")) {
                Constituent c = new Constituent(type, ViewNames.NER_CONLL, ta, i, i + 1);
                ner_view.addConstituent(c);
            }
        }
    }

    public static void tagHashtag(TextAnnotation ta){
        SpanLabelView ner_view = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);

        for(int i = 0; i < ta.size(); i++){
            String token = ta.getToken(i);
            if(!token.startsWith("#")) continue;

            token = token.substring(1).toLowerCase();
            if(token.length() < 3) continue;

            if(ner_view.getConstituentsCoveringToken(i).size() > 0) continue;

            String type = "O";
            if(gaz.get("ORG").contains(token) || token.endsWith("com") || token.substring(0, token.length()-1).endsWith("com")) {
                System.out.println("ORG " + token);
                type = "ORG";
            }else if(gaz.get("GPE").contains(token)){
                System.out.println("GPE " + token);
                type = "GPE";
            }else if(gaz.get("PER").contains(token)){
                System.out.println("PER " + token);
                type = "PER";
            }
            if(!type.equals("O")) {
                Constituent c = new Constituent(type, ViewNames.NER_CONLL, ta, i, i + 1);
                ner_view.addConstituent(c);
            }
        }
    }

    public static void annotate(String dir, String outdir){

        RuleBasedAnnotator rule = new RuleBasedAnnotator();
        ColumnFormatReader reader = new ColumnFormatReader();

        for(String type: RuleBasedAnnotator.entitytypes)
            gaz.put(type, rule.getGazetteers(type));

        if(gaz.get("GPE").contains("uyghur")) {
            System.out.println("what");
            System.exit(-1);
        }

        for(File f: (new File(dir)).listFiles()){
            QueryDocument doc = reader.readFile(f);
            annotate(doc);

            String conll = TextAnnotation2Conll.taToConll(doc.getTextAnnotation(), doc);

            try {
                FileUtils.writeStringToFile(new File(outdir, f.getName()), conll, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        String indir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/e-gpe";
        String outdir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/e-gpe-tw";

        TweetsAnnotator.annotate(indir, outdir);
        RuleBasedAnnotator.compairConll(indir, outdir);
    }
}
