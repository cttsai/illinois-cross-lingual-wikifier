package edu.illinois.cs.cogcomp.lorelei;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Sentence;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.lorelei.utils.Conll2TextAnnotation;
import edu.illinois.cs.cogcomp.lorelei.utils.TextAnnotation2Conll;
import edu.illinois.cs.cogcomp.mlner.experiments.conll.ColumnFormatReader;
import edu.illinois.cs.cogcomp.lorelei.core.RuleBasedAnnotator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 7/24/16.
 */
public class UZExp {


    public static void removeLabels(String indir, String outdir){
        ColumnFormatReader reader = new ColumnFormatReader();
        for(File f: new File(indir).listFiles()) {
            QueryDocument doc = reader.readFile(f);
            TextAnnotation ta = doc.getTextAnnotation();
            SpanLabelView emptyview = new SpanLabelView(ViewNames.NER_CONLL, "UserSpecified", ta, 1d);
            ta.addView(ViewNames.NER_CONLL, emptyview);

            TextAnnotation2Conll.writeTaToConll(ta, doc, outdir);
        }

    }

    public static void printWord2VecData() throws IOException {

        String dir = "/shared/corpora/ner/lorelei/uz/ctsai/mono-gaz1";
        dir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/mono-gpe-uly1";

        String outfile = "/shared/corpora/ner/lorelei/uz/ctsai/mono.text.new";
        outfile = "/shared/corpora/ner/wikifier-features/ug/cp3/final/mono.ner1";

        BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
        ColumnFormatReader reader = new ColumnFormatReader();
        int cnt = 0;
        for(File f: new File(dir).listFiles()) {
            if(cnt++%1000 == 0) System.out.println(cnt);
            QueryDocument doc = reader.readFile(f);
            TextAnnotation ta = doc.getTextAnnotation();

            Conll2TextAnnotation.populatNERView(doc);

            SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);

            List<Constituent> nes = nerview.getConstituents();

            for(int j = 0; j < ta.getNumberOfSentences(); j++) {
                Sentence sen = ta.getSentence(j);
                int sen_start = sen.getStartSpan();
                int sen_end = sen.getEndSpan();
                List<Constituent> overlap = nerview.getConstituentsCoveringSpan(sen_start, sen_end);
                for (Constituent c : overlap) {
                    int start = c.getStartSpan();
                    int end = c.getEndSpan();
                    String out = "";
                    for (int i = sen_start; i < sen_end; i++) {
                        if (i < start || i >= end) {
                            out += ta.getToken(i) + " ";
                        } else if (i == start) {
                            out += "TYPE:" + c.getLabel() + " ";
                        }
                    }
                    bw.write(out.trim() + "\n");
                }

                bw.write(Arrays.asList(sen.getTokens()).stream().collect(joining(" "))+"\n");
            }

//            int nes_idx = 0;
//
//            String out = "";
//            for(int i = 0; i < ta.size(); i++){
//                if(nes_idx >= nes.size() || i < nes.get(nes_idx).getStartSpan())
//                    out += " "+ta.getToken(i);
//                else if( i == nes.get(nes_idx).getEndSpan()-1){ // the last token of a mention
//                    out += " TYPE:"+nes.get(nes_idx).getLabel();
//                    nes_idx++;
//                }
//            }
//            bw.write(out.trim()+"\n");
        }

        bw.close();


    }

    public static void countConsecMentions(){
        String traindir = "/shared/corpora/ner/lorelei/uz/ctsai/VOA-gaznew-filter";
        ColumnFormatReader reader = new ColumnFormatReader();

        Map<String, List<String>> prevtags = new HashMap<>();
        Map<String, List<String>> nexttags = new HashMap<>();
        Map<String, Integer> tagcnt = new HashMap<>();
        Map<String, Integer> tagcnt1 = new HashMap<>();
        RuleBasedAnnotator.entitytypes.forEach(x -> prevtags.put(x, new ArrayList<>()));
        RuleBasedAnnotator.entitytypes.forEach(x -> nexttags.put(x, new ArrayList<>()));
        RuleBasedAnnotator.entitytypes.forEach(x -> tagcnt.put(x, 0));
        RuleBasedAnnotator.entitytypes.forEach(x -> tagcnt1.put(x, 0));
        for(File f: new File(traindir).listFiles()) {
            QueryDocument doc = reader.readFile(f);
            Conll2TextAnnotation.populatNERView(doc);

            TextAnnotation ta = doc.getTextAnnotation();
            SpanLabelView ner = (SpanLabelView) doc.getTextAnnotation().getView(ViewNames.NER_CONLL);

            for(Constituent cons: ner.getConstituents()){
                String type = cons.getLabel();

                if(cons.getStartSpan()>0) {
                    List<Constituent> prevcon = ner.getConstituentsCoveringToken(cons.getStartSpan() - 1);
                    if(prevcon.size()>0)
                        prevtags.get(type).add(prevcon.get(0).getLabel());

                    tagcnt.put(type, tagcnt.get(type)+1);
                }

                if(cons.getEndSpan()<ta.size()) {
                    List<Constituent> nextcon = ner.getConstituentsCoveringToken(cons.getEndSpan());
                    if(nextcon.size()>0)
                        nexttags.get(type).add(nextcon.get(0).getLabel());

                    tagcnt1.put(type, tagcnt.get(type)+1);
                }
            }
        }

        for(String type: tagcnt.keySet()){
            System.out.println(type);
            for(String t: tagcnt.keySet()){
                long prev = prevtags.get(type).stream().filter(x -> x.equals(t)).count();
                System.out.println("\t"+t+" "+prev+" "+tagcnt.get(type)+" "+String.format("%.2f", 100*(float)prev/tagcnt.get(type)));
            }
            for(String t: tagcnt.keySet()){
                long prev = nexttags.get(type).stream().filter(x -> x.equals(t)).count();
                System.out.println("\t"+t+" "+prev+" "+tagcnt1.get(type)+" "+String.format("%.2f", 100*(float)prev/tagcnt1.get(type)));
            }
        }
    }

    public static void countWords(){

        Map<String, Integer> wordcnt = new HashMap<>();
        try {
            for(String line: LineIO.read("/shared/corpora/ner/lorelei/uz/mono-plain")){
                for(String token: line.toLowerCase().split("\\s+")){
                    if(!wordcnt.containsKey(token))
                        wordcnt.put(token,1);
                    else
                        wordcnt.put(token, wordcnt.get(token)+1);
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<String> top = wordcnt.entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue()))
                .map(x -> x.getKey())
                .collect(Collectors.toList()).subList(0, 100);

        try {
            FileUtils.writeStringToFile(new File("stopwords.uz"), top.stream().collect(joining("\n")), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Set<String> getUzStopwords(){
        Set<String> stopwords = new HashSet<>();
        try {
            stopwords.addAll(LineIO.read("stopwords.uz"));
            stopwords.addAll(LineIO.read("stopwords/stopwords.uz"));
            stopwords.addAll(LineIO.read("stopwords/puncs"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return stopwords;
    }

    public static void w2vExp(){

        Set<String> stopwords = getUzStopwords();

        EmbeddingClassifier ec = new EmbeddingClassifier("uz-ner1");

        String lang = "uz-ner1";

        String traindir = "/shared/corpora/ner/lorelei/uz/Test";


        Map<String, Integer> type2correct = new HashMap<>();
        RuleBasedAnnotator.entitytypes.forEach(x -> type2correct.put(x, 0));
        Map<String, Integer> type2total = new HashMap<>();
        RuleBasedAnnotator.entitytypes.forEach(x -> type2total.put(x, 0));

        ColumnFormatReader reader = new ColumnFormatReader();
        int correct = 0, total = 0;
        for(File f: new File(traindir).listFiles()) {
            QueryDocument doc = reader.readFile(f);
            Conll2TextAnnotation.populatNERView(doc);

            TextAnnotation ta = doc.getTextAnnotation();
            SpanLabelView ner = (SpanLabelView) doc.getTextAnnotation().getView(ViewNames.NER_CONLL);

            for (Constituent con : ner.getConstituents()) {
                System.out.println("Gold: "+con.getLabel());
                total++;

                List<String> context = new ArrayList<>();
                if(con.getStartSpan()>0){
                    context.add(ta.getToken(con.getStartSpan()-1));
                }

                if(con.getStartSpan()>1){
                    context.add(ta.getToken(con.getStartSpan()-2));
                }

                if(con.getEndSpan()<ta.size()){
                    context.add(ta.getToken(con.getEndSpan()));
                }
                if(con.getEndSpan()<ta.size()-1){
                    context.add(ta.getToken(con.getEndSpan()+1));
                }

                List<String> preds = ec.topLabels(context, -10);
                if(preds.subList(0,Math.min(2, preds.size())).contains(con.getLabel())) {
                    correct++;
//                    type2correct.put(pred, type2correct.get(pred)+1);
                }
            }
        }
        System.out.println((double)correct/total);

        for(String type: type2total.keySet()){
            System.out.println(type+" "+(double)type2correct.get(type)/type2total.get(type));
        }
    }

    public static Set<String> loadUzPers(){

        try {
            return LineIO.read("/shared/corpora/ner/lorelei/uz/freqnamesUZ")
                    .stream()
                    .map(x -> x.split("\t"))
//                    .filter(x -> Integer.parseInt(x[1])>1)
                    .map(x -> x[0].toLowerCase())
                    .collect(Collectors.toSet());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new HashSet<>();
    }

    public static void extractGazetteers(){
        ColumnFormatReader reader = new ColumnFormatReader();

        String traindir = "/shared/corpora/ner/lorelei/uz/Train";

        Map<String, Set<String>> gaz = new HashMap<>();


        Map<String, List<String>> surf2types = new HashMap<>();

        for(File f: new File(traindir).listFiles()){
            QueryDocument doc = reader.readFile(f);
            Conll2TextAnnotation.populatNERView(doc);

            SpanLabelView ner = (SpanLabelView) doc.getTextAnnotation().getView(ViewNames.NER_CONLL);

            for(Constituent con: ner.getConstituents()){

                String type = con.getLabel();
                String surf = con.getSurfaceForm().toLowerCase().trim();
                if(!surf2types.containsKey(surf))
                    surf2types.put(surf, new ArrayList<>());
                surf2types.get(surf).add(type);

//                if(!gaz.containsKey(type)) gaz.put(type, new HashSet<>());
//
//                boolean over = false;
//                for(String t: gaz.keySet()){
//                    if(t.equals(type)) continue;
//
//                    if(gaz.get(t).contains(con.getSurfaceForm().toLowerCase()))
//                        over = true;
//                }
//
//                if(!over)
//                    gaz.get(type).add(con.getSurfaceForm().toLowerCase().trim());
            }
        }

        for(String surf: surf2types.keySet()){
            String toptype = surf2types.get(surf).stream().collect(groupingBy(x -> x, counting()))
                    .entrySet().stream().sorted((x1, x2) -> Long.compare(x2.getValue(), x1.getValue()))
                    .collect(Collectors.toList()).get(0).getKey();

            if(!gaz.containsKey(toptype))
                gaz.put(toptype, new HashSet<>());
            gaz.get(toptype).add(surf);
        }

        for(String type: gaz.keySet())
            System.out.println(type+" "+gaz.get(type).size());

        Map<String, String> namelist = new HashMap<>();

        for(String type: gaz.keySet()) {
            gaz.get(type).forEach(x -> namelist.put(x, type));
//            String out = gaz.get(type).stream().collect(joining("\n"));
//            try {
//                FileUtils.writeStringToFile(new File("/shared/corpora/ner/lorelei/uz/ctsai/gaz/", type), out, "UTF-8");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        loadUzPers().forEach(x -> namelist.put(x, "PER"));

        String testdir = "/shared/corpora/ner/lorelei/uz/mono";
//        String testdir = "/shared/corpora/ner/lorelei/uz/ctsai/Test";
//        String outdir = "/shared/corpora/ner/lorelei/uz/ctsai/Test-gaznew-per1";
        String outdir = "/shared/corpora/ner/lorelei/uz/ctsai/mono-per";

        RuleBasedAnnotator rule = new RuleBasedAnnotator();

        rule.setSuffixDir("/shared/corpora/ner/lorelei/uz/ctsai/");

        rule.setGazetteers(namelist);
        rule.printGazetteerSize();
        rule.annotate(testdir, outdir);
    }

    public static void removeTestDocs(){

        String dir = "/shared/corpora/ner/lorelei/uz/VOA";
        String test = "/shared/corpora/ner/lorelei/uz/Test";
        String out = "/shared/corpora/ner/lorelei/uz/ctsai/VOA1";

        for(File f: new File(dir).listFiles()){
            if(new File(test, f.getName()).isFile()) {
                System.out.println("skip "+f.getName() );
                continue;
            }

            try {
                FileUtils.writeStringToFile(new File(out, f.getName()), FileUtils.readFileToString(f), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public static void eval(String preddir, String golddir){

        ColumnFormatReader reader = new ColumnFormatReader();


        int correct = 0, ptotal = 0, gtotal = 0;
        int perc = 0, locc = 0, gpec = 0, orgc = 0;
        int perp = 0, locp = 0, gpep = 0, orgp = 0;
        int pert = 0, loct = 0, gpet = 0, orgt = 0;
        for(File f: new File(golddir).listFiles()) {
            QueryDocument doc = reader.readFile(f);
            Conll2TextAnnotation.populatNERView(doc);

            QueryDocument preddoc = reader.readFile(new File(preddir, f.getName()));
            Conll2TextAnnotation.populatNERView(preddoc);

            SpanLabelView ner = (SpanLabelView) doc.getTextAnnotation().getView(ViewNames.NER_CONLL);
            SpanLabelView predner = (SpanLabelView) preddoc.getTextAnnotation().getView(ViewNames.NER_CONLL);

            ptotal += predner.getConstituents().size();
            gtotal += ner.getConstituents().size();

            perp += predner.getConstituents().stream().filter(x -> x.getLabel().equals("PER")).count();
            orgp += predner.getConstituents().stream().filter(x -> x.getLabel().equals("ORG")).count();
            gpep += predner.getConstituents().stream().filter(x -> x.getLabel().equals("GPE")).count();
            locp += predner.getConstituents().stream().filter(x -> x.getLabel().equals("LOC")).count();

            pert += ner.getConstituents().stream().filter(x -> x.getLabel().equals("PER")).count();
            orgt += ner.getConstituents().stream().filter(x -> x.getLabel().equals("ORG")).count();
            gpet += ner.getConstituents().stream().filter(x -> x.getLabel().equals("GPE")).count();
            loct += ner.getConstituents().stream().filter(x -> x.getLabel().equals("LOC")).count();

            for (Constituent pred : predner.getConstituents()) {
                String type = pred.getLabel();
                int start = pred.getStartSpan();
                int end = pred.getEndSpan();
                boolean match = false;
                for (Constituent gold : ner.getConstituents()) {
                    if (gold.getStartSpan() == start && gold.getEndSpan() == end && type.equals(gold.getLabel())) {
                        correct++;
                        match = true;
                        if(type.equals("PER")) perc++;
                        if(type.equals("ORG")) orgc++;
                        if(type.equals("GPE")) gpec++;
                        if(type.equals("LOC")) locc++;
                        break;
                    }

                }

                if(!match)
                    System.out.println(doc.getDocID()+" "+pred.getSurfaceForm()+" "+pred.getLabel());

            }

        }

        double pre = (double)correct/ptotal;
        double rec = (double)correct/gtotal;
        double f1 = 2*pre*rec/(pre+rec);

        System.out.println(pre+" "+rec+" "+f1);

        pre = (double)perc/perp;
        rec = (double)perc/pert;
        f1 = 2*pre*rec/(pre+rec);
        System.out.println("PER "+pre+" "+rec+" "+f1);

        pre = (double)orgc/orgp;
        rec = (double)orgc/orgt;
        f1 = 2*pre*rec/(pre+rec);
        System.out.println("ORG "+pre+" "+rec+" "+f1);

        pre = (double)locc/locp;
        rec = (double)locc/loct;
        f1 = 2*pre*rec/(pre+rec);
        System.out.println("LOC "+pre+" "+rec+" "+f1);
        pre = (double)gpec/gpep;
        rec = (double)gpec/gpet;
        f1 = 2*pre*rec/(pre+rec);
        System.out.println("GPE "+pre+" "+rec+" "+f1);
    }

    public static void main(String[] args) {

//        removeTestDocs();
//        extractGazetteers();
//        try {
//            printWord2VecData();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        w2vExp();
//        countWords();
//        countConsecMentions();

        String pred = "/shared/corpora/ner/lorelei/uz/ctsai/Test-gaznew-per1";
//        pred = "/shared/corpora/ner/wikifier-features/ug/cp3/dev2-gaz4";
        String gold = "/shared/corpora/ner/lorelei/uz/Test";
//        gold = "/shared/corpora/ner/eval/column/dev2";
//        eval(pred, gold);
//
//        String indir = "/shared/corpora/ner/lorelei/uz/Train";
//        String outdir = "/shared/corpora/ner/lorelei/uz/ctsai/Train-nolabel";
//        removeLabels(indir, outdir);
    }
}
