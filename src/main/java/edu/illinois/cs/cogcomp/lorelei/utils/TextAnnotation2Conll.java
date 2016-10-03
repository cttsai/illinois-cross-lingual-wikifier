package edu.illinois.cs.cogcomp.lorelei.utils;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.SerializationHelper;
import edu.illinois.cs.cogcomp.lorelei.EmbeddingClassifier;
import edu.illinois.cs.cogcomp.lorelei.UZExp;
import edu.illinois.cs.cogcomp.mlner.experiments.conll.ColumnFormatReader;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;
//import org.slf4j.impl.Log4jLoggerAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;


/**
 * Created by ctsai12 on 6/6/16.
 */
public class TextAnnotation2Conll {

    public static boolean filter = false;

//    private static WordEmbedding we = new WordEmbedding();
    private static String lang;
    private static int nnew = 0;
    private static EmbeddingClassifier ec;

    public static Set<String> stops;

    public static void writeTaToConll(TextAnnotation ta, QueryDocument doc, String outdir){


        String conll = TextAnnotation2Conll.taToConll(doc.getTextAnnotation(), doc);

        try {
//            System.out.println("Writing to "+outdir+"/"+ta.getId());
            if(!filter || !conll.trim().isEmpty())
                FileUtils.writeStringToFile(new File(outdir, ta.getId()), conll, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static Map<String, Integer> typecnt = new HashMap<>();

    static{
        typecnt.put("PER", 0);
        typecnt.put("GPE", 0);
        typecnt.put("LOC", 0);
        typecnt.put("ORG", 0);
    }

    public static void annotateByEmbedding(TextAnnotation ta){


        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);

        for(Sentence sen: ta.sentences()){
//            for(int i = sen.getStartSpan(); i < sen.getEndSpan()-1; i++){
//                if(stops.contains(ta.getToken(i).toLowerCase()) || stops.contains(ta.getToken(i+1).toLowerCase())) continue;
//                List<Constituent> cons = nerview.getConstituentsCoveringSpan(i, i+2);
//                if(cons.size() == 0){
//
//                    List<String> context = new ArrayList<>();
//
//                    if(i > 0) {
//                        context.add(ta.getToken(i - 1));
//                    }
//
//                    if(i > 1){
//                        context.add(ta.getToken(i - 2));
//                    }
//
//                    if(i < sen.getEndSpan()-2) {
//                            context.add(ta.getToken(i + 2));
//                    }
//
//                    if(i < sen.getEndSpan()-3) {
//                            context.add(ta.getToken(i + 3));
//                    }
//
//                    String label = ec.classify(context, 0.6);
//                    if(!label.equals("O")) {
//                        Constituent c = new Constituent(label, ViewNames.NER_CONLL, nerview.getTextAnnotation(), i, i + 2);
//                        nerview.addConstituent(c);
//                        nnew++;
//                    }
//                }
//            }
            for(int i = sen.getStartSpan(); i < sen.getEndSpan(); i++){
                if(stops.contains(ta.getToken(i).toLowerCase())) continue;
                List<Constituent> cons = nerview.getConstituentsCoveringToken(i);
                if(cons.size() == 0){

                    List<String> context = new ArrayList<>();

                    if(i > 0) {
                            context.add(ta.getToken(i - 1));
                    }

                    if(i > 1){
                            context.add(ta.getToken(i - 2));

                    }

                    if(i < ta.size()-1) {
                            context.add(ta.getToken(i + 1));
                    }

                    if(i < ta.size()-2) {
                            context.add(ta.getToken(i + 2));
                    }

                    String label = ec.classify(context, 0.6);
                    if(!label.equals("O")) {
                        Constituent c = new Constituent(label, ViewNames.NER_CONLL, nerview.getTextAnnotation(), i, i + 1);
                        nerview.addConstituent(c);
                        nnew++;
                    }
                }
            }
        }
    }

    public static void addRandomLabel(TextAnnotation ta){

        Random random = new Random();

        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);

        Map<Constituent, Double> newcons = new HashMap<>();

        for(Constituent con: nerview.getConstituents()){
            if(con.getLabel().equals("LOC")){
                int start = con.getStartSpan();
                if(start == 0) continue;
                String prevtoken = ta.getToken(start - 1);
                if(stops.contains(prevtoken.toLowerCase())) continue;
                List<Constituent> prevcon = nerview.getConstituentsCoveringToken(start - 1);
                if(prevcon.size() == 0){

                    List<String> context = new ArrayList<>();
//                    context_vec.add(we.getWordVector(ta.getToken(start), lang));
                    if(start-1>0) {
                        context.add(ta.getToken(start - 2));
                    }

                    String label = ec.classify(context, 0.1);
                    if(label.equals("GPE")) {
                        Constituent c = new Constituent("GPE", ViewNames.NER_CONLL, nerview.getTextAnnotation(), start - 1, start);
                        nerview.addConstituent(c);
                        nnew++;
                    }

//                    float r = random.nextFloat();
//                    if(r<0.1) {
//                        Constituent c = new Constituent("GPE", ViewNames.NER_CONLL, nerview.getTextAnnotation(), start - 1, start);
//                        nerview.addConstituent(c);
//                    }
                }
            }
        }

//        for(Constituent c: newcons.keySet()){
//            if(newcons.get(c) > 0.4) {
//                nerview.addConstituent(c);
//                nnew++;
//            }
//        }

    }

    /**
     * doc is used to recover wikifier features. If you don't need those features, input null.
     * @param ta
     * @param doc
     * @return
     */
    public static String taToConll(TextAnnotation ta, QueryDocument doc){

//        addRandomLabel(ta);

        String out = "";
        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
        for(Sentence sen: ta.sentences()){
            String sen_out = "";
            int ent_cnt = 0;
            for(int i = sen.getStartSpan(); i < sen.getEndSpan(); i++){
                String label = "O";
                List<Constituent> cons = nerview.getConstituentsCoveringToken(i);
                if(cons.size()>0){
                    Constituent c = cons.get(0);
                    if(!c.getLabel().equals("O")) {
                        if (c.getStartSpan() == i) {
//                            typecnt.put(c.getLabel(), typecnt.get(c.getLabel())+1);
                            label = "B-" + c.getLabel();
                            ent_cnt++;
                        }
                        else
                            label = "I-" + c.getLabel();
                    }
                }

                sen_out += label+"\tx\tx\tx\tx\t"+ta.getToken(i)+"\tx\tx\tx\tx";

                if(doc != null){
                    if(!doc.wikifier_features.get(i).isEmpty())
                        sen_out += "\t"+doc.wikifier_features.get(i);
                }

                sen_out += "\n";
            }

            if(!filter || ent_cnt >0) {
                out += sen_out+"\n";
            }
        }
        return out;
    }

    public static Set<String> loadOWords() {

        int cut = 45;
        String brown = "/shared/experiments/ctsai12/workspace/brown-cluster/uz-c300-min5/paths";
        Set<String> owords = null;
        try {
            Set<String> cids = LineIO.read("/shared/preprocessed/ctsai12/brownClusterGazetteerCountsUzbek.txt")
                    .subList(0, cut)
                    .stream().map(x -> x.split("\\s+")[0])
                    .collect(Collectors.toSet());

            owords = LineIO.read(brown).stream()
                    .map(x -> x.split("\\s+"))
                    .filter(x -> cids.contains(x[0]))
                    .map(x -> x[1].toLowerCase())
                    .collect(Collectors.toSet());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Loaded "+owords.size()+" O words");
        return owords;
    }

    public static String filterByBrown(TextAnnotation ta, QueryDocument doc, Set<String> owords){


        int window = 3;
        String out = "";
        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
        List<Constituent> mentions = nerview.getConstituents();
        int idx = 0;
        while(idx < mentions.size()) {

            // find the start and end of consecitive mentions
            Constituent mstart = mentions.get(idx);
            int start = mstart.getStartSpan();
            int end = mstart.getEndSpan();
            int sid = mstart.getSentenceId();
            for (idx++; idx < mentions.size(); idx++) {
                Constituent m = mentions.get(idx);
                if (m.getSentenceId() != sid) {
                    break;
                }

                if (m.getStartSpan() - end > 3) {
                    break;
                }

                end = m.getEndSpan();
            }

            String sen_out = "";
            boolean bad = false;
            for(int i = Math.max(0, start-window); i < Math.min(ta.size(), end+window); i++){
                if(ta.getSentenceId(i)!=sid) continue;
                if(i == start-1 && !owords.contains(ta.getToken(i).toLowerCase())){
                    bad = true;
                    break;
                }
                if(i == end && !owords.contains(ta.getToken(i).toLowerCase())){
                    bad = true;
                    break;
                }

                String label = "O";
                List<Constituent> over = nerview.getConstituentsCoveringToken(i);
                if(over.size()>0){
                    Constituent c = over.get(0);
                    if(!c.getLabel().equals("O")) {
                        if (c.getStartSpan() == i) {
                            label = "B-" + c.getLabel();
                        }
                        else
                            label = "I-" + c.getLabel();
                    }
                }

                sen_out += label+"\tx\tx\tx\tx\t"+ta.getToken(i)+"\tx\tx\tx\tx";

                if(doc != null){
                    if(!doc.wikifier_features.get(i).isEmpty())
                        sen_out += "\t"+doc.wikifier_features.get(i);
                }

                sen_out += "\n";
            }
            if(!bad)
                out += sen_out+"\n";
        }
        return out;
    }

    public static String taToConllWindow(TextAnnotation ta, QueryDocument doc){


        int window = 3;
        String out = "";
        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
        List<Constituent> mentions = nerview.getConstituents();
        int idx = 0;
        while(idx < mentions.size()) {

            // find the start and end of consecitive mentions
            Constituent mstart = mentions.get(idx);
            int start = mstart.getStartSpan();
            int end = mstart.getEndSpan();
            int sid = mstart.getSentenceId();
            for (idx++; idx < mentions.size(); idx++) {
                Constituent m = mentions.get(idx);
                if (m.getSentenceId() != sid) {
                    break;
                }

                if (m.getStartSpan() - end > 3) {
                    break;
                }

                end = m.getEndSpan();
            }

            String sen_out = "";
            for(int i = Math.max(0, start-window); i < Math.min(ta.size(), end+window); i++){
                if(ta.getSentenceId(i)!=sid) continue;
                String label = "O";
                List<Constituent> over = nerview.getConstituentsCoveringToken(i);
                if(over.size()>0){
                    Constituent c = over.get(0);
                    if(!c.getLabel().equals("O")) {
                        if (c.getStartSpan() == i) {
                            label = "B-" + c.getLabel();
                        }
                        else
                            label = "I-" + c.getLabel();
                    }
                }

                sen_out += label+"\tx\tx\tx\tx\t"+ta.getToken(i)+"\tx\tx\tx\tx";

                if(doc != null){
                    if(!doc.wikifier_features.get(i).isEmpty())
                        sen_out += "\t"+doc.wikifier_features.get(i);
                }

                sen_out += "\n";
            }
            out += sen_out+"\n";
        }
        return out;
    }

    public static void outputCorrections(String indir, String origdir, String outdir) throws IOException {

        Map<String, Set<String>> correct = new HashMap<>();
        correct.put("GPE", new HashSet<>());
        correct.put("LOC", new HashSet<>());
        correct.put("PER", new HashSet<>());
        correct.put("ORG", new HashSet<>());
        correct.put("O", new HashSet<>());
        Map<String, Set<String>> newm = new HashMap<>();
        newm.put("GPE", new HashSet<>());
        newm.put("LOC", new HashSet<>());
        newm.put("PER", new HashSet<>());
        newm.put("ORG", new HashSet<>());
        ArabicToULY au = new ArabicToULY();
        String out = "";
        Set<String> newout = new HashSet<>();
        for(File f: (new File(indir)).listFiles()) {
            int origname = Integer.parseInt(f.getName());
            if(origdir.equals("/shared/corpora/ner/eval/ta/set0-NW1-ann6-gaz") && origname >= 8) // hack
                origname++;

            TextAnnotation ta = SerializationHelper.deserializeTextAnnotationFromFile(f.getAbsolutePath());
            TextAnnotation ta1 = SerializationHelper.deserializeTextAnnotationFromFile(origdir + "/" + origname);
            SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
            SpanLabelView nerview_orig = (SpanLabelView) ta1.getView(ViewNames.NER_CONLL);
            List<Constituent> nes = nerview.getConstituents();
            List<Constituent> nes_orig = nerview_orig.getConstituents();
            if(ta.size() != ta1.size()) {
                System.out.println("TA size don't match");
                System.out.println(f.getName()+" "+nes.size()+" "+nes_orig.size());
            }
            for (int i = 0; i < nes.size(); i++) {

                int j;
                for(j = 0; j <nes_orig.size(); j++){
                    if(nes_orig.get(j).getStartCharOffset() == nes.get(i).getStartCharOffset()
                            && nes_orig.get(j).getEndCharOffset() == nes.get(i).getEndCharOffset())
                        break;
                }

                int start = nes.get(i).getStartSpan();
                int end = nes.get(i).getEndSpan();
                String surface = Arrays.asList(ta1.getTokensInSpan(start, end)).stream().collect(joining(" "));
                String type = nes.get(i).getLabel();

                List<Constituent> overlap = nerview_orig.getConstituentsCoveringSpan(start, end);

                if(j == nes_orig.size()){ // span changes
                    newm.get(type).add(surface.toLowerCase());
                    out=au.getULYPhrase(nes.get(i).getSurfaceForm())+"|||"+nes.get(i).getLabel();
                    for(Constituent o: overlap)
                        out += "\t"+au.getULYPhrase(o.getSurfaceForm())+"|||"+o.getLabel();
                    newout.add(out);
                }
                else{
                    if(!nes_orig.get(j).getLabel().equals(type)){ // a correction
                        correct.get(type).add(au.getULYPhrase(surface.toLowerCase())+"|||"+nes_orig.get(j).getLabel());
//                        System.out.println(au.getULYPhrase(nes.get(i).getSurfaceForm())+" "+nes.get(i).getLabel());
//                        overlap.forEach(x -> System.out.println("\t"+au.getULYPhrase(x.getSurfaceForm())+" "+x.getLabel()));
//                        overlap.forEach(x -> System.out.println("\t"+x.getSurfaceForm()+" "+x.getLabel()));
                    }
                }
            }

            for (int i = 0; i < nes_orig.size(); i++) {

                List<Constituent> overlap = nerview.getConstituentsCoveringSpan(nes_orig.get(i).getStartSpan(), nes_orig.get(i).getEndSpan());
                if(overlap.size()==0){
                    correct.get("O").add(au.getULYPhrase(nes_orig.get(i).getSurfaceForm().toLowerCase())+"|||"+nes_orig.get(i).getLabel());
//                    correct.get("O").add(nes_orig.get(i).getSurfaceForm().toLowerCase()+"|||"+nes_orig.get(i).getLabel());
                }
            }

        }


        for(String type: correct.keySet()){
            FileUtils.writeStringToFile(new File(outdir, type+".correct"), correct.get(type).stream().collect(joining("\n")), "UTF-8");
        }
//        for(String type: newm.keySet()){
//            FileUtils.writeStringToFile(new File(outdir, type+".new"), newm.get(type).stream().collect(joining("\n")), "UTF-8");
//        }
        FileUtils.writeStringToFile(new File(outdir, "new"), newout.stream().collect(joining("\n")), "UTF-8");
    }

    public static void filterConll(String indir, String outdir){

        ColumnFormatReader reader = new ColumnFormatReader();

        filter = true;

        Set<String> owords = loadOWords();

        int cnt = 0;
        for(File f: new File(indir).listFiles()){
            if(cnt++%1000 == 0) System.out.println(cnt);
            QueryDocument doc = reader.readFile(f);

            if(!doc.getTextAnnotation().hasView(ViewNames.NER_CONLL))
                Conll2TextAnnotation.populatNERView(doc);

//            addRandomLabel(doc.getTextAnnotation());
//            annotateByEmbedding(doc.getTextAnnotation());
            String conll = TextAnnotation2Conll.taToConllWindow(doc.getTextAnnotation(), doc);
//            String conll = TextAnnotation2Conll.filterByBrown(doc.getTextAnnotation(), doc, owords);
            try {
                if(!conll.trim().isEmpty())
                    FileUtils.writeStringToFile(new File(outdir, doc.getDocID()), conll, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        System.out.println("nnew "+nnew);

    }

    public static void main(String[] args) {

//        String indir = "/shared/corpora/ner/eval/ta/newNW-SWM2-annotation-NI2";
//        indir = "/shared/corpora/ner/eval/ta/afterni-NW1-ann8-annotation-NI";
//        String origdir = "/shared/corpora/ner/eval/ta/newNW-SWM2";
//        origdir = "/shared/corpora/ner/eval/ta/afterni-NW1-ann8";
//        String outdir = "/shared/corpora/ner/gazetteers/ug/NI3-1";
//        outdir = "/shared/corpora/ner/gazetteers/ug/NI1-2";
//        try {
//            outputCorrections(indir, origdir, outdir);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        stops = UZExp.getUzStopwords();
        ec = new EmbeddingClassifier("uz-ner1");

//        String indir = "/shared/corpora/ner/lorelei/uz/ctsai/VOA-gaznew";
        String indir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/m-key-new";

        indir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/mono-ensemble3";
//        String outdir = "/shared/corpora/ner/lorelei/uz/ctsai/VOA-window3-emb0.6";
        String outdir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/mono-ensemble3-window3";
        filterConll(indir, outdir);
    }
}
