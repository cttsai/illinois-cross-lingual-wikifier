package edu.illinois.cs.cogcomp.lorelei.utils;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.mlner.experiments.conll.ColumnFormatReader;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class convert conll format NER annotations to TextAnnotation with NER_CONLL view.
 * In LORELEI, this is used for preparing documents for the NI interface.
 *
 * Created by ctsai12 on 6/3/16.
 */
public class Conll2TextAnnotation {


    public static void populatNERView(QueryDocument doc){
        TextAnnotation ta = doc.getTextAnnotation();
        SpanLabelView emptyview = new SpanLabelView(ViewNames.NER_CONLL, "UserSpecified", ta, 1d);
        ta.addView(ViewNames.NER_CONLL, emptyview);

        for(ELMention m: doc.mentions){
            int start = ta.getTokenIdFromCharacterOffset(m.getStartOffset());
            int end = ta.getTokenIdFromCharacterOffset(m.getEndOffset() - 1);
            Constituent c = new Constituent(m.getType(), ViewNames.NER_CONLL, ta, start, end+1);
            emptyview.addConstituent(c);
        }
//        System.out.println(ta.getId());
//        SpanLabelView view = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
//        for(Constituent c: view.getConstituents()){
//            System.out.println(c.getSurfaceForm()+" "+c.getStartCharOffset()+" "+c.getEndCharOffset());
//            System.out.println(ta.getText().substring(c.getStartCharOffset(), c.getEndCharOffset()));
//            System.out.println(ta.getToken(ta.getTokenIdFromCharacterOffset(c.getStartCharOffset())));
//            System.out.println(ta.getToken(ta.getTokenIdFromCharacterOffset(c.getEndCharOffset()-1)));
//        }
    }

    public static void run(String indir, String outdir){

        ColumnFormatReader cr = new ColumnFormatReader();
        cr.readDir(indir, true);

        List<QueryDocument> docs = cr.docs;
        docs.forEach(x -> populatNERView(x));
        //docs.forEach(x -> RuleBasedAnnotator.annotate(x)); // added mentions match entries in the gazetteers
        Set<String> seen = new HashSet<>();

        int doc_cnt = 0;
        while(docs.size() > 0){
            if(doc_cnt == 300) break;

            // select the most distinct doc
            double max = -1;
            int idx = -1;
            for(int i = 0; i < docs.size(); i++){
                QueryDocument doc = docs.get(i);
                if(doc.getDocID().contains("_DF_")) continue;
                SpanLabelView view = (SpanLabelView) doc.getTextAnnotation().getView(ViewNames.NER_CONLL);
                Set<String> mention_set = view.getConstituents().stream().map(x -> x.getSurfaceForm().toLowerCase()).collect(Collectors.toSet());
//                int nm = doc.getTextAnnotation().getText().length();
                mention_set.removeAll(seen);
                double unseen = (double)mention_set.size();
                if(unseen > max){
                    max = unseen;
                    idx = i;
                }
            }

            if(max == 0) break;
            QueryDocument doc = docs.get(idx);
            System.out.println(doc.getDocID()+" max "+max);
            SpanLabelView view = (SpanLabelView) doc.getTextAnnotation().getView(ViewNames.NER_CONLL);
            Set<String> ms = view.getConstituents().stream().map(x -> x.getSurfaceForm().toLowerCase()).collect(Collectors.toSet());
            seen.addAll(ms);
            try {
                SerializationHelper.serializeTextAnnotationToFile(doc.getTextAnnotation(), outdir+"/"+doc_cnt, true);
                doc_cnt++;
            } catch (IOException e) {
                e.printStackTrace();
            }
            docs.remove(idx);
        }
    }

    public static void main(String[] args) {
//        String indir = "/shared/corpora/ner/wikifier-features/zh/Test-dryrun0-transfer-annotated1";
//        String indir = "/shared/corpora/ner/wikifier-features/ug/eval-set0-NW1-norank-ann6-filter";
//        String indir = "/shared/corpora/ner/wikifier-features/ug/iter10-NW-correct-uly-zhper1-zhgpe1-keywordgpeloc1-org-country-region";
        String indir = "/shared/corpora/ner/wikifier-features/ug/newNW-SWM2";
        String outdir = "/shared/corpora/ner/eval/ta/newNW-SWM2";
        run(indir, outdir);
//        try {
//            TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(outdir + "/CMN_DF_000196_20040906_C00005A1H");
//            SpanLabelView view = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
//            for(Constituent c: view.getConstituents()){
//                System.out.println(c.getSurfaceForm()+" "+c.getStartCharOffset()+" "+c.getEndCharOffset());
//                System.out.println(ta.getText().substring(c.getStartCharOffset(), c.getEndCharOffset()));
//                System.out.println(ta.getTokenIdFromCharacterOffset(c.getStartCharOffset()));
//                System.out.println(ta.getTokenIdFromCharacterOffset(c.getEndCharOffset()-1));
//            }
//
//            String[] text = ta.getTokenizedText().split(" ");
////            String[] text = ta.getTokens();
//            for(Constituent c : view.getConstituents()){
//                int start = c.getStartSpan();
//                int end = c.getEndSpan();
//                System.out.println(c.getSurfaceForm());
//                System.out.println("\t"+text[start]+" "+start);
//                System.out.println("\t"+text[end-1]+" "+(end-1));
////                text[start] = String.format("<span class='%s pointer' id='cons-%d-%d'>%s", c.getLabel(), start, end, text[start]);
////                text[end-1] += "</span>";
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
