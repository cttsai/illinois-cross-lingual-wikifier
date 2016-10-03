package edu.illinois.cs.cogcomp.lorelei.core;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.mlner.experiments.conll.ColumnFormatReader;
import edu.illinois.cs.cogcomp.lorelei.utils.Conll2TextAnnotation;
import edu.illinois.cs.cogcomp.lorelei.utils.TextAnnotation2Conll;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 7/15/16.
 */
public class DesignatorAnnotator {


    private Map<Integer, Map<String, Integer>> wordcnt = new HashMap<>();
    private Map<String, Integer> unicnt = new HashMap<>();
    private Map<String, Integer> bicnt = new HashMap<>();
    private Map<String, Integer> tricnt = new HashMap<>();

    private String root = "/shared/corpora/ner/gazetteers/ug/designators/new1/";
    private String docdir = "/shared/corpora/ner/eval/column/mono-all-uly";

    public RuleBasedAnnotator rule_annotator;


    public Map<String, Set<String>> badnames = new HashMap<>();
    public Set<String> bads = new HashSet<>();
    public Map<String, String> gaz = new HashMap<>();

    private double uni_good_th = 5;
    private double uni_bad_th = 5;

    private double bi_good_th = 5; // for the pmi of bigrams
    private double bi_good_th1 = 8; // for the pmi of bigrams and keyword

    private double bi_bad_th = 5;

    private int min_cnt = 2;

    public Map<String, Integer> loadMap(String file){
        Map<String, Integer> ret = new HashMap<>();
        try {
            for(String line: LineIO.read(file)){
                String[] tokens = line.split("\t");
                ret.put(tokens[0], Integer.parseInt(tokens[1]));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public List<String> loadDesignators(String type, boolean after){
        String dir = "/shared/corpora/ner/gazetteers/ug/designators/new/";
        String filename = dir + type.toLowerCase() + "/" + type.toLowerCase();

        List<String> ret = new ArrayList<>();
        try {
            for(String line: LineIO.read(filename)){

                String[] parts = line.split("\t");
                if(after) {
                    if (parts.length == 3 && parts[2].equals("1"))
                        ret.add(parts[0].trim().toLowerCase());
                }
                else
                    if (parts.length < 3 || !parts[2].equals("1"))
                        ret.add(parts[0].trim().toLowerCase());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

//        System.out.println(type+" loaded "+ret.size()+" designators");
        return ret;
    }

    public void printFrequentNgrams(){

        boolean after = true;

//        String type = "ORG";
        for(String type: RuleBasedAnnotator.entitytypes){
            List<String> keywords = loadDesignators(type, after);
//        keywords = keywords.subList(0,10);
            for(String keyword: keywords){
                findPhrases(keyword, docdir, type, after);
            }
        }
    }

    public void countNgrams(){

        ColumnFormatReader reader = new ColumnFormatReader();
        int total = 0;
        for(File f: (new File(docdir)).listFiles()) {
            if(total++%1000 == 0){
                System.out.println(total);
            }
            QueryDocument doc = reader.readFile(f);
            Conll2TextAnnotation.populatNERView(doc);
            TextAnnotation ta = doc.getTextAnnotation();
            for(int i = 0; i < ta.getNumberOfSentences(); i++){
                int start = ta.getSentence(i).getStartSpan();
                int end = ta.getSentence(i).getEndSpan();
                for(int j = start; j < end; j++){

                    String surface = ta.getToken(j).toLowerCase();
//                    if(unicnt.containsKey(surface))
//                        unicnt.put(surface,unicnt.get(surface)+1);
//                    else
//                        unicnt.put(surface, 1);
//
//                    unitotal++;

                    if(j<end-1){
                        surface += " "+ta.getToken(j+1).toLowerCase();
//                        if(bicnt.containsKey(surface))
//                            bicnt.put(surface,bicnt.get(surface)+1);
//                        else
//                            bicnt.put(surface, 1);
//                        bitotal++;
                    }
                    if(j< end -2) {
                        surface += " " + ta.getToken(j + 2).toLowerCase();
                        if (tricnt.containsKey(surface))
                            tricnt.put(surface, tricnt.get(surface) + 1);
                        else
                            tricnt.put(surface, 1);
                        tritotal++;
                    }
                }
            }
        }

//        String out = unicnt.entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue()))
//                .map(x -> x.getKey()+"\t"+x.getValue()+"\t"+Math.log((double)x.getValue()/unitotal))
//                .collect(joining("\n"));
//        String out1 = bicnt.entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue()))
//                .map(x -> x.getKey()+"\t"+x.getValue()+"\t"+Math.log((double)x.getValue()/bitotal))
//                .collect(joining("\n"));
        String out2 = tricnt.entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue()))
                .map(x -> x.getKey()+"\t"+x.getValue())
                .collect(joining("\n"));

        try {
//            FileUtils.writeStringToFile(new File(root, "uni.prob"), out, "UTF-8");
//            FileUtils.writeStringToFile(new File(root, "bi.prob"), out1, "UTF-8");
            FileUtils.writeStringToFile(new File(root, "tri.prob"), out2, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void findPhrases(String keyword, String indir, String type, boolean after){

        ColumnFormatReader reader = new ColumnFormatReader();

//        wordcnt.clear();
//        aftercnt.clear();

        wordcnt.put(1, new HashMap<>());
        wordcnt.put(2, new HashMap<>());
        wordcnt.put(3, new HashMap<>());

        System.out.println(keyword+" "+type);

        int total = 0;
        for(File f: (new File(indir)).listFiles()) {
            if(total++%1000 == 0){
                System.out.println(total);
//                break;
            }
            QueryDocument doc = reader.readFile(f);
            findPhrases(doc, keyword, after);
        }

        List<Map.Entry<String, Integer>> sorted1 = wordcnt.get(1).entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue())).collect(Collectors.toList());
        List<Map.Entry<String, Integer>> sorted2 = wordcnt.get(2).entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue())).collect(Collectors.toList());
//        List<Map.Entry<String, Integer>> sorted3 = wordcnt.get(3).entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue())).collect(Collectors.toList());

        sorted1 = sorted1.stream().filter(x -> x.getValue()>0)
                    .filter(x -> !RuleBasedAnnotator.stopwords.contains(x.getKey()))
                    .collect(Collectors.toList());
        System.out.println(sorted1.size());
        String out1 = sorted1.stream().map(x -> x.getKey()+"\t"+x.getValue()).collect(joining("\n"));

        sorted2 = sorted2.stream().filter(x -> x.getValue()>0)
                    .filter(x -> !RuleBasedAnnotator.stopwords.contains(x.getKey().split("\\s+")[0]))
                    .filter(x -> !RuleBasedAnnotator.stopwords.contains(x.getKey().split("\\s+")[1]))
                    .collect(Collectors.toList());
        System.out.println(sorted2.size());
        String out2 = sorted2.stream().map(x -> x.getKey()+"\t"+x.getValue()).collect(joining("\n"));

//        sorted3 = sorted3.stream().filter(x -> x.getValue()>1)
//                .filter(x -> !RuleBasedAnnotator.stopwords.contains(x.getKey().split("\\s+")[0]))
//                .filter(x -> !RuleBasedAnnotator.stopwords.contains(x.getKey().split("\\s+")[1]))
//                .filter(x -> !RuleBasedAnnotator.stopwords.contains(x.getKey().split("\\s+")[2]))
//                .collect(Collectors.toList());
//        System.out.println(sorted3.size());
//        String out3 = sorted3.stream().map(x -> x.getKey()+"\t"+x.getValue()).collect(joining("\n"));

        String dir = root+type.toLowerCase();
        try {
            String suff = "before";
            if(after) suff = "after";
            FileUtils.writeStringToFile(new File(dir, keyword+".1."+suff), out1, "UTF-8");
            FileUtils.writeStringToFile(new File(dir, keyword+".2."+suff), out2, "UTF-8");
//            FileUtils.writeStringToFile(new File(dir, keyword+".3.before"), out3, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void findPhrases(QueryDocument doc, String keyword, boolean after){

        TextAnnotation ta = doc.getTextAnnotation();

        String text = ta.getText().toLowerCase();

//        if(text.contains(keyword))
//            System.out.println(doc.getDocID());

        int idx = -keyword.length();
        while (true) {
            idx = text.indexOf(keyword, idx + keyword.length());
            if (idx < 0) break;


            if(after){
                int tid = ta.getTokenIdFromCharacterOffset(idx+keyword.length()-1);
                if(tid < ta.size()-1){
                    Map<String, Integer> map = wordcnt.get(1);
                    String prev = ta.getToken(tid + 1).toLowerCase();
                    if (map.containsKey(prev))
                        map.put(prev, map.get(prev) + 1);
                    else
                        map.put(prev, 1);

                }

                if(tid < ta.size()-2){
                    Map<String, Integer> map = wordcnt.get(2);
                    String prev = ta.getToken(tid + 1).toLowerCase()+" "+ta.getToken(tid+2).toLowerCase();
                    if (map.containsKey(prev))
                        map.put(prev, map.get(prev) + 1);
                    else
                        map.put(prev, 1);

                }

            }
            else {
                int tid = ta.getTokenIdFromCharacterOffset(idx);
                if (tid > 2) {
                    Map<String, Integer> map = wordcnt.get(3);
                    String prev = ta.getToken(tid - 3).toLowerCase() + " " + ta.getToken(tid - 2).toLowerCase() + " " + ta.getToken(tid - 1).toLowerCase();
                    if (map.containsKey(prev))
                        map.put(prev, map.get(prev) + 1);
                    else
                        map.put(prev, 1);
                }

                if (tid > 1) {
                    Map<String, Integer> map = wordcnt.get(2);
                    String prev = ta.getToken(tid - 2).toLowerCase() + " " + ta.getToken(tid - 1).toLowerCase();
                    if (map.containsKey(prev))
                        map.put(prev, map.get(prev) + 1);
                    else
                        map.put(prev, 1);
                }

                if (tid > 0) {
                    Map<String, Integer> map = wordcnt.get(1);
                    String prev = ta.getToken(tid - 1).toLowerCase();
                    if (map.containsKey(prev))
                        map.put(prev, map.get(prev) + 1);
                    else
                        map.put(prev, 1);
                }
            }
        }
    }

    public void annotateRegion(TextAnnotation ta){

        List<String> regions = Arrays.asList("jenubiy", "shimaliy", "sherqiy", "gherbiy");

        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);

        String text = ta.getText().toLowerCase();

        for(String name: regions) {
            int idx = -name.length();
            while (true) {
                idx = text.indexOf(name, idx + name.length());
                if (idx < 0) break;

                // no space before
                if (idx > 0 && !text.substring(idx - 1, idx).trim().isEmpty()) continue;

                // if no space after
                if (idx + name.length() < text.length() - 1
                        && !text.substring(idx + name.length(), idx + name.length() + 1).trim().isEmpty())
                    continue;


                int start = ta.getTokenIdFromCharacterOffset(idx);

                List<Constituent> overlaps = nerview.getConstituentsCoveringToken(start);
                if(overlaps.size()>0) continue;

                if(start < ta.size()-1){
                    List<Constituent> next = nerview.getConstituentsCoveringToken(start + 1);
                    if(next.size() == 0) continue;

                    int end = next.get(0).getEndSpan();

                    String nexttoken = ta.getToken(start+1).toLowerCase();
                    String type = next.get(0).getLabel();

                    if(type.equals("GPE")) {
                        if (name.equals("jenubiy") && (nexttoken.startsWith("afriq") || nexttoken.startsWith("koriy") || nexttoken.startsWith("sudan")))
                            type = "GPE";
                        else if (name.equals("shimaliy") && nexttoken.startsWith("koriy"))
                            type = "GPE";
                        else if (name.equals("sherqiy") && nexttoken.startsWith("türk"))
                            type = "GPE";
                        else
                            type = "LOC";
                    }

                    nerview.getConstituentsCoveringSpan(start, end).forEach(x -> nerview.removeConstituent(x));
                    Constituent c = new Constituent(type, ViewNames.NER_CONLL, ta, start, end);
//                    System.out.println(c.getSurfaceForm()+" "+c.getLabel());
                    nerview.addConstituent(c);
                }
            }
        }


    }


    public boolean containDigit(String str){

        for(char c: str.toCharArray()){
            if(Character.isDigit(c))
                return true;
        }

        return false;
    }

    public int annotateORGBigram(TextAnnotation ta){
        String text = ta.getText();
        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
        List<String> keywords = loadDesignators("ORG", false);
        int total = 0;
        for (String key : keywords) {
            int idx = -key.length();
            while (true) {
                idx = org.apache.commons.lang3.StringUtils.indexOfIgnoreCase(text, key, idx+key.length());
                if (idx < 0) break;

                // no space before
                if (idx > 0 && !text.substring(idx - 1, idx).trim().isEmpty()) continue;

                int tid = ta.getTokenIdFromCharacterOffset(idx);


                if (tid > 1) {
                    String token1 = ta.getToken(tid - 2);
                    String token2 = ta.getToken(tid - 1);


                    if(ta.getSentenceId(tid)!=ta.getSentenceId(tid-2)) continue;

                    if(rule_annotator.inStopWords(token1)) continue;
                    if(rule_annotator.inStopWords(token2)) continue;


                    if(bads.contains(token1+" "+token2)) continue;

                    String name = token1+" "+token2+" "+key;
                    if(rule_annotator.blacklist.contains(name)) continue;


                    int start = tid - 2;


                    if(name.startsWith("cheklik mesuli")){
                        if(start > 0 && ta.getToken(start -1 ).equals("soda")){
                            start --;
                            name = "soda "+name;
                        }
                    }


                    if(start > 0){
                        List<Constituent> over = nerview.getConstituentsCoveringToken(start - 1);
                        if(over.size() > 0 &&
                                (over.get(0).getLabel().equals("GPE") || over.get(0).getLabel().equals("LOC"))) {
                            start = over.get(0).getStartSpan();
                        }
                        else if(start > 1 && nerview.getConstituentsCoveringToken(start - 2).size() > 0){
                            over = nerview.getConstituentsCoveringToken(start - 2);
                            if (over.get(0).getLabel().equals("GPE") || over.get(0).getLabel().equals("LOC")) {
                                start = over.get(0).getStartSpan();
                            }
                        }
                        else if(name.startsWith("j x") || name.startsWith("soda chek")) { // extend no matter what
                            start--;
                        }
                    }


                    List<Constituent> overlap = nerview.getConstituentsCoveringSpan(start, tid + 1);
                    overlap.forEach(x -> nerview.removeConstituent(x));

                    if(key.startsWith("uniwérs") && ta.getToken(start).startsWith("amérik")){
                        Constituent c = new Constituent("GPE", ViewNames.NER_CONLL, ta, start, start+1);
                        nerview.addConstituent(c);
                        start ++;
                    }

                    Constituent c = new Constituent("ORG", ViewNames.NER_CONLL, ta, start, tid+1);
                    nerview.addConstituent(c);
//                    System.out.println("adding " + c.getSurfaceForm()+" ORG");
                    total++;
                }
            }
        }

        return total;
    }

    public void annotateUnigram(TextAnnotation ta){

        String text = ta.getText();
        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
        for(String type: RuleBasedAnnotator.entitytypes) {
            List<String> keywords = loadDesignators(type, false);
            for (String key : keywords) {
                int idx = -key.length();
                while (true) {
                    idx = org.apache.commons.lang3.StringUtils.indexOfIgnoreCase(text, key, idx+key.length());
                    if (idx < 0) break;

                    // no space before
                    if (idx > 0 && !text.substring(idx - 1, idx).trim().isEmpty()) continue;

                    int tid = ta.getTokenIdFromCharacterOffset(idx);


                    if (tid > 0) {

                        String ptoken = ta.getToken(tid - 1);


                        if(ta.getSentenceId(tid)!=ta.getSentenceId(tid-1)) continue;

                        if(rule_annotator.inStopWords(ptoken)) continue;

                        if(bads.contains(ptoken)) continue;

                        String name = ptoken;
                        int end = tid;
                        if(!type.equals("PER")){
                            name = ptoken +" "+key;
                            end++;
                        }

                        if(rule_annotator.blacklist.contains(name)) continue;

                        int start = tid - 1;

                        if(start > 0 && name.startsWith("taqim aralli")){
                            String prev = ta.getToken(start - 1);
                            if(rule_annotator.inStopWords(prev)) continue;
                            start--;
                        }

                        List<Constituent> overlap = nerview.getConstituentsCoveringSpan(start, end);
                        overlap.forEach(x -> nerview.removeConstituent(x));

                        Constituent c = new Constituent(type, ViewNames.NER_CONLL, ta, start, end);
                        nerview.addConstituent(c);
//                        System.out.println("adding " + c.getSurfaceForm()+" "+type);
                    }
                }
            }
        }
    }

    public void annotateByDesignators(TextAnnotation ta){
        annotateUnigram(ta);

        uyghurRule(ta);

        annotateORGBigram(ta);

        JXrule(ta);

        JemiyitRule(ta);

        annotateRegion(ta);

        ArmyRule(ta);

        TeamRule(ta);
    }

    public void annotateByDesignators(String indir, String outdir){

        if(gaz.size() == 0)
            addConfidentNamesToGaz();

        Set<String> bads = badnames.entrySet().stream().flatMap(x -> x.getValue().stream()).collect(Collectors.toSet());


        ColumnFormatReader reader = new ColumnFormatReader();
        int total = 0, total1 = 0;
        int doccnt = 0;
        for(File f: (new File(indir)).listFiles()){
            if(doccnt++ % 100 == 0) System.out.println(doccnt);
            QueryDocument doc = reader.readFile(f);

            TextAnnotation ta = doc.getTextAnnotation();
            if(!ta.hasView(ViewNames.NER_CONLL))
                Conll2TextAnnotation.populatNERView(doc);

            annotateByDesignators(ta);

            TextAnnotation2Conll.writeTaToConll(doc.getTextAnnotation(), doc, outdir);
        }
        System.out.println(""+total1);

    }


    public static Map<String, Double> uniprob = new HashMap<>();
    public static Map<String, Double> biprob = new HashMap<>();
    public static Map<String, Double> triprob = new HashMap<>();
    public static int unitotal, bitotal, tritotal;

    public void calPMI(){

        unicnt = loadMap(root+"uni.prob");
        bicnt = loadMap(root+"bi.prob");
        tricnt = loadMap(root+"tri.prob");


        unitotal = unicnt.values().stream().mapToInt(x -> x).sum();
        bitotal = bicnt.values().stream().mapToInt(x -> x).sum();
        tritotal = tricnt.values().stream().mapToInt(x -> x).sum();

        for(String word: unicnt.keySet())
            uniprob.put(word, (double)unicnt.get(word)/unitotal);
        for(String word: bicnt.keySet())
            biprob.put(word, (double)bicnt.get(word)/bitotal);
        for(String word: tricnt.keySet())
            triprob.put(word, (double)tricnt.get(word)/tritotal);

        System.out.println("Loaded Ngram probabilities");

        boolean after = true;

        for(String type: RuleBasedAnnotator.entitytypes){
            List<String> keywords = loadDesignators(type, after);
            for(String keyword: keywords){
                calUniPMI(type, keyword, after);
            }
        }

        for(String type: RuleBasedAnnotator.entitytypes){
            List<String> keywords = loadDesignators(type, after);
            for(String keyword: keywords){
                calBiPMI(type, keyword, after);
            }
        }

//        String type = "ORG";
//        List<String> keywords = loadDesignators(type, after);
//        for(String keyword: keywords){
//            calTriPMI(type, keyword);
//        }
    }

    public void calTriPMI(String type, String key) {
        String file = root+type.toLowerCase()+"/"+key+".3.before";

        Map<String, Double> cnt = loadDoubleMap(file, 1);

        double total = cnt.entrySet().stream().mapToDouble(x -> x.getValue()).sum();


        Map<String, Double> filtered = new HashMap<>();
        for(String word: cnt.keySet()){
            if(!triprob.containsKey(word)){
                System.out.println("no "+word);
                continue;
            }
            if(containDigit(word)) continue;
            filtered.put(word, Math.log(cnt.get(word)/total) - Math.log(triprob.get(word)));
        }

        Map<String, Double> tripmi = new HashMap<>();
        for(String word: filtered.keySet()){
            String[] tokens = word.split("\\s+");
            double tprob = Math.log(1/tritotal);
            if(triprob.containsKey(word))
                tprob = Math.log(triprob.get(word));

            double uprob = Math.log(uniprob.get(tokens[0])*uniprob.get(tokens[1])*uniprob.get(tokens[2]));
            tripmi.put(word, tprob - uprob);
        }

        String out = tripmi.entrySet().stream().sorted((x1, x2) -> Double.compare(x2.getValue(), x1.getValue()))
                .map(x -> x.getKey() + "\t" + x.getValue() +"\t"+filtered.get(x.getKey()))
                .collect(joining("\n"));

        try {
            FileUtils.writeStringToFile(new File(root, type.toLowerCase()+"/"+key+".3.pmi"), out, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void calBiPMI(String type, String key, boolean after) {
        String suf = "before";
        if(after) suf = "after";
        String file = root+type.toLowerCase()+"/"+key+".2."+suf;

        Map<String, Double> cnt = loadDoubleMap(file, 1);

        double total = cnt.entrySet().stream().mapToDouble(x -> x.getValue()).sum();


        Map<String, Double> filtered = new HashMap<>();
        for(String word: cnt.keySet()){
            if(!biprob.containsKey(word)){
                System.out.println("no "+word);
                continue;
            }
            if(containDigit(word)) continue;
            filtered.put(word, Math.log(cnt.get(word)/total) - Math.log(biprob.get(word)));
        }

        Map<String, Double> bipmi = new HashMap<>();
        for(String word: filtered.keySet()){
            String[] tokens = word.split("\\s+");
            double bprob = Math.log(1/bitotal);
            if(biprob.containsKey(word))
                bprob = Math.log(biprob.get(word));

            double uprob = Math.log(uniprob.get(tokens[0])*uniprob.get(tokens[1]));
            bipmi.put(word, bprob - uprob);
        }

        String out = bipmi.entrySet().stream().sorted((x1, x2) -> Double.compare(x2.getValue(), x1.getValue()))
                .map(x -> x.getKey() + "\t" + x.getValue() +"\t"+filtered.get(x.getKey()))
                .collect(joining("\n"));

        try {
            FileUtils.writeStringToFile(new File(root, type.toLowerCase()+"/"+key+".2.pmi"), out, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void calUniPMI(String type, String key, boolean after){

        String suf = "before";
        if(after) suf = "after";
        String file = root+type.toLowerCase()+"/"+key+".1."+suf;

        Map<String, Double> cnt = loadDoubleMap(file, 1);

        double total = cnt.entrySet().stream().mapToDouble(x -> x.getValue()).sum();

        Map<String, Double> filtered = new HashMap<>();
        for(String word: cnt.keySet()){
            if(!uniprob.containsKey(word)){
                System.out.println("no "+word);
                continue;
            }
            if(containDigit(word)) continue;
            filtered.put(word, Math.log(cnt.get(word)/total) - Math.log(uniprob.get(word)));
        }

        String out = filtered.entrySet().stream().sorted((x1, x2) -> Double.compare(x2.getValue(), x1.getValue()))
                .map(x -> x.getKey() + "\t" + x.getValue())
                .collect(joining("\n"));

        try {
            FileUtils.writeStringToFile(new File(root, type.toLowerCase()+"/"+key+".1.pmi"), out, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Map<String, Double> loadDoubleMap(String file, int col){
        Map<String, Double> cnt = new HashMap<>();

        try {
            for(String line: LineIO.read(file)){
                String[] parts = line.split("\t");
                double c = Double.parseDouble(parts[col]);
                cnt.put(parts[0], c);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return cnt;
    }


    public void addConfidentUnigram(String cntfile, String pmifile, String key, String type, boolean after){

        Map<String, Integer> cntmap = loadMap(cntfile);
        try {
            for(String line: LineIO.read(pmifile)){
                String[] parts = line.split("\t");
                String word = parts[0];

                if(rule_annotator.inStopWords(word)) continue;

                double pmi = Double.parseDouble(parts[1]);

                if(cntmap.get(word) < min_cnt) continue;

                if(pmi > uni_good_th){
                    String name = word;
                    if(name.startsWith("bolm"))
                        System.out.println("!!!! "+name+" "+key);
                    if(!type.equals("PER")){
                        if (after)
                            name = key+" "+word;
                        else
                            name = word +" "+key;
                    }
                    if(!rule_annotator.blacklist.contains(name))
                        gaz.put(name, type);
                }

                if(pmi < uni_bad_th)
                    badnames.get(key).add(word);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void addConfidentBigram(String cntfile, String pmifile, String key, String type, boolean after){

        Set<String> gpes = rule_annotator.getGazetteers("GPE");
        Map<String, Integer> cntmap = loadMap(cntfile);
        try {
            for(String line: LineIO.read(pmifile)){
                String[] parts = line.split("\t");
                String word = parts[0];

                if(rule_annotator.inStopWords(word.split("\\s+")[0])) continue;

                if(after && rule_annotator.inStopWords(word.split("\\s+")[1])) continue;

                double pmi = Double.parseDouble(parts[1]);
                double pmi1 = Double.parseDouble(parts[2]);

                if(cntmap.get(word) < min_cnt) continue;

                if(pmi > bi_good_th && pmi1 > bi_good_th1){
                    String name = word;
                    if(!type.equals("PER")){  // person doesn't include designators
                        if (after)
                            name = key +" "+word;
                        else
                            name = word +" "+key;
                    }
                    if(!rule_annotator.blacklist.contains(name))
                        gaz.put(name, type);
                }

                if(pmi < bi_bad_th){
                    String[] tokens = word.split("\\s+");
                    if(tokens[0].startsWith("uyghur")) continue;
                    for(String gpe: gpes){
                        if(tokens[0].startsWith(gpe)) continue;
                    }

                    badnames.get(key).add(word);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void addConfidentNamesToGaz(){

        for(String type: RuleBasedAnnotator.entitytypes) {
            List<String> keywords = loadDesignators(type, false);
            for(String key: keywords){

                badnames.put(key, new HashSet<>());
                badnames.get(key).add("apet");
                String cntfile = root+type.toLowerCase()+"/"+key+".1.before";
                String pmifile = root + type.toLowerCase() + "/" + key + ".1.pmi";
                addConfidentUnigram(cntfile, pmifile, key, type, false);

                cntfile = root + type.toLowerCase()+"/"+key+".2.before";
                pmifile = root+type.toLowerCase()+"/"+key+".2.pmi";
                addConfidentBigram(cntfile, pmifile, key, type, false);
            }
        }

//        for(String type: RuleBasedAnnotator.entitytypes) {
//            List<String> keywords = loadDesignators(type, true);
//            for(String key: keywords){
//
//                badnames.put(key, new HashSet<>());
//
//                String cntfile = root + type.toLowerCase() + "/" + key + ".1.after";
//                String pmifile = root + type.toLowerCase() + "/" + key + ".1.pmi";
//                addConfidentUnigram(cntfile, pmifile, key, type, true);
//
//                cntfile = root + type.toLowerCase()+"/"+key+".2.after";
//                pmifile = root+type.toLowerCase()+"/"+key+".2.pmi";
//                addConfidentBigram(cntfile, pmifile, key, type, true);
//            }
//        }


        rule_annotator.addGazetteers(gaz);
        rule_annotator.printGazetteerSize();

        System.out.println("Names from designators: "+gaz.size());
        bads = badnames.entrySet().stream().flatMap(x -> x.getValue().stream()).collect(Collectors.toSet());

//        for(String key: badnames.keySet()){
//            badnames.get(key).forEach(x -> System.out.println(x+" "+key));
//        }
    }

    public void JXrule(TextAnnotation ta) {


        String text = ta.getText();
        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
        List<String> keywords = Arrays.asList("j x", "jamaet xewpsiz");
        for (String key : keywords) {
            int idx = -key.length();
            while (true) {
                idx = org.apache.commons.lang3.StringUtils.indexOfIgnoreCase(text, key, idx+key.length());
                if (idx < 0) break;

                // no space before
                if (idx > 0 && !text.substring(idx - 1, idx).trim().isEmpty()) continue;

                // if no space after "j x"
                if (key.equals("j x") && idx + key.length() < text.length() - 1
                        && !text.substring(idx + key.length(), idx + key.length() + 1).trim().isEmpty())
                    continue;

                int tid = ta.getTokenIdFromCharacterOffset(idx);

                int start = tid;
                int end = tid + 2;
                List<Constituent> over = nerview.getConstituentsCoveringSpan(start, end);
                if(over.size()>0 && over.get(0).getStartSpan() < start && over.get(0).getEndSpan()>end)
                    continue;

                if(over.size()>0 && over.get(0).getEndSpan() > end)
                    end = over.get(0).getEndSpan();
                else
                    end++;


                List<Constituent> prev = nerview.getConstituentsCoveringToken(start - 1);
                if(prev.size() > 0) {
                    if(prev.get(0).getLabel().equals("GPE") || prev.get(0).getLabel().equals("LOC"))
                        start = prev.get(0).getStartSpan();
                    else
                        continue;
                }
                else if(start > 0){
                    String pt = ta.getToken(start - 1);
                    if(pt.startsWith("sheherl") || pt.startsWith("rayon") || pt.startsWith("yézil")){
                        if(start - 2 >= 0 && !rule_annotator.inStopWords(ta.getToken(start - 2)))
                            start -= 2;
                        else
                            continue;
                    }
                    else
                        continue;
                }
                else
                    continue;


                nerview.getConstituentsCoveringSpan(start, end).forEach(x -> nerview.removeConstituent(x));
                Constituent c = new Constituent("ORG", ViewNames.NER_CONLL, ta, start, end);
                nerview.addConstituent(c);

            }
        }
    }
    public void uyghurRule(TextAnnotation ta) {

        String text = ta.getText();
        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
        List<String> keywords = Arrays.asList("qoshun", "rehberli", "siyasi rehberl", "diyar", "pasport");
        for(String key: keywords) {
            int idx = -key.length();
            while (true) {
                idx = org.apache.commons.lang3.StringUtils.indexOfIgnoreCase(text, key, idx + key.length());
                if (idx < 0) break;

                // no space before
                if (idx > 0 && !text.substring(idx - 1, idx).trim().isEmpty()) continue;


                int tid = ta.getTokenIdFromCharacterOffset(idx);


                if(tid > 0){
                    if(ta.getToken(tid - 1).toLowerCase().startsWith("uyghu")){
                        int start = tid - 1;
                        int end = tid;

                        if(key.equals("diyar"))
                            end++;

                        nerview.getConstituentsCoveringSpan(start, end).forEach(x -> nerview.removeConstituent(x));
                        Constituent c = new Constituent("GPE", ViewNames.NER_CONLL, ta, start, end);
                        nerview.addConstituent(c);
                    }
                }
            }
        }
    }

    public void TeamRule(TextAnnotation ta) {

        String text = ta.getText();
        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
        List<String> keywords = Arrays.asList("komandi");
        for(String key: keywords) {
            int idx = -key.length();
            while (true) {
                idx = org.apache.commons.lang3.StringUtils.indexOfIgnoreCase(text, key, idx + key.length());
                if (idx < 0) break;

                // no space before
                if (idx > 0 && !text.substring(idx - 1, idx).trim().isEmpty()) continue;


                int tid = ta.getTokenIdFromCharacterOffset(idx);

                if(nerview.getConstituentsCoveringToken(tid).size()>0) continue;

                int start = -1;
                int end = tid+1;

                boolean get = false;

                for(int i = tid -1; i >= 0 && i > tid-3; i--){

                    List<Constituent> overlap = nerview.getConstituentsCoveringToken(i);
                    if(overlap.size()>0 && (overlap.get(0).getLabel().equals("GPE"))){
                        start = overlap.get(0).getStartSpan();
                        get = true;
                        break;
                    }
                    else if(ta.getToken(i).toLowerCase().startsWith("uyghur")){
                        start = i;
                        get = true;
                        break;
                    }
                }

                if(get) {
                    nerview.getConstituentsCoveringSpan(start, end).forEach(x -> nerview.removeConstituent(x));
                    Constituent c = new Constituent("ORG", ViewNames.NER_CONLL, ta, start, end);
                    nerview.addConstituent(c);
                }
            }
        }
    }

    public void ArmyRule(TextAnnotation ta) {

        String text = ta.getText();
        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
        List<String> keywords = Arrays.asList("armyis", "armiy", "qoshu");
        for(String key: keywords) {
            int idx = -key.length();
            while (true) {
                idx = org.apache.commons.lang3.StringUtils.indexOfIgnoreCase(text, key, idx + key.length());
                if (idx < 0) break;

                // no space before
                if (idx > 0 && !text.substring(idx - 1, idx).trim().isEmpty()) continue;


                int tid = ta.getTokenIdFromCharacterOffset(idx);

                if(nerview.getConstituentsCoveringToken(tid).size()>0) continue;

                int start = -1;
                int end = tid+1;

                boolean get = false;

                for(int i = tid -1; i >= 0 && i > tid-3; i--){

                    List<Constituent> overlap = nerview.getConstituentsCoveringToken(i);
                    if(overlap.size()>0 && (overlap.get(0).getLabel().equals("GPE") || overlap.get(0).getLabel().equals("ORG"))){
                        start = overlap.get(0).getStartSpan();
                        get = true;
                        break;
                    }
                    else if(ta.getToken(i).toLowerCase().startsWith("uyghur")){
                        start = i;
                        get = true;
                        break;
                    }
                }

                if(get) {
                    nerview.getConstituentsCoveringSpan(start, end).forEach(x -> nerview.removeConstituent(x));
                    Constituent c = new Constituent("ORG", ViewNames.NER_CONLL, ta, start, end);
                    nerview.addConstituent(c);
                }
            }
        }
    }


    public void JemiyitRule(TextAnnotation ta) {

        String text = ta.getText();
        SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);
        String key = "jemiyit";
        int idx = -key.length();
        while (true) {
            idx = org.apache.commons.lang3.StringUtils.indexOfIgnoreCase(text, key, idx+key.length());
            if (idx < 0) break;

            // no space before
            if (idx > 0 && !text.substring(idx - 1, idx).trim().isEmpty()) continue;


            int tid = ta.getTokenIdFromCharacterOffset(idx);

            boolean get = false;

            int start = -1;
            int end = tid+1;

            for(int i = tid -1; i >= 0 && i > tid-5; i--){

                List<Constituent> overlap = nerview.getConstituentsCoveringToken(i);
                if(overlap.size()>0 && (overlap.get(0).getLabel().equals("GPE") || overlap.get(0).getLabel().equals("LOC"))){
                    start = overlap.get(0).getStartSpan();
                    get = true;
                }
                else if(ta.getToken(i).toLowerCase().startsWith("uyghur")){
                    start = i;
                    get = true;
                }
                else if(ta.getToken(i).toLowerCase().startsWith("qizil")){
                    start = i;
                    get = true;
                }
                else if(get)
                    break;
            }

            if(get) {
                nerview.getConstituentsCoveringSpan(start, end).forEach(x -> nerview.removeConstituent(x));
                Constituent c = new Constituent("ORG", ViewNames.NER_CONLL, ta, start, end);
                nerview.addConstituent(c);
            }
        }
    }

    public static void main(String[] args) {

        DesignatorAnnotator annotator = new DesignatorAnnotator();
        annotator.rule_annotator = new RuleBasedAnnotator();

//        consolidateNgrams();

//        for(int i = 0; i < per_keywords.size(); i++)
//            findPhrases(per_keywords.get(1), indir);
//        printFrequentNgrams();
//        calPMI();
//        addConfidentNamesToGaz();
//
//        List<Map.Entry<String, Integer>> sort = suf_cnt.entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue())).collect(Collectors.toList());
//        sort.forEach(x -> System.out.println(x.getKey()+"\t"+x.getValue()));

//        cleanLists();


        String indir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/newE-gpe-uly";
        indir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/mono-gpe-uly";
//        indir = "/shared/corpora/ner/eval/column/set0-mono-NW-uly-gaz";
        String outdir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/newE-army-uly";

        annotator.annotateByDesignators(indir, outdir);

        RuleBasedAnnotator.compairConll(indir, outdir);
//        System.out.println(text.contains("[a-zA-Z]+"));
    }
}
