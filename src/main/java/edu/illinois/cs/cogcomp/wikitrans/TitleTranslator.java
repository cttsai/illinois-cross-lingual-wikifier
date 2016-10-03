package edu.illinois.cs.cogcomp.wikitrans;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.transliteration.SPModel;
import edu.illinois.cs.cogcomp.utils.Utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 9/3/16.
 */
public class TitleTranslator {


    private Map<String,Map<String, Double>> s2t2prob = new HashMap<>();

    private Map<String, Map<String, Double>> ilm2j2prob = new HashMap<>();
    private Map<String, Map<String, Double>> e2f2prob = new HashMap<>();

    private Map<String, Map<String, Double>> lm2a2prob = new HashMap<>();
    private Map<String, Map<String, Double>> lm2a2c = new HashMap<>();
    private Map<String, Double> lm2c = new HashMap<>();

    private List<Map<String, List<Pair<String, String>>>> a2pairs = new ArrayList<>();


    private Map<String, Map<String, Double>> en2fo2prob = new HashMap<>();
    private Map<String, Map<String, Double>> ilm2j2c = new HashMap<>();
    private Map<String, Double> ilm2c = new HashMap<>();
    private Map<String, Double> e2c = new HashMap<>();
    private Map<String, Map<String, Double>> e2f2c = new HashMap<>();


    private Map<String, Map<String, Double>> src2align = new HashMap<>();

    private List<Pair<List<String>, List<String>>> segmap = new ArrayList<>();

    private Map<Pair<List<String>, List<String>>, Integer> segmapcnt = new HashMap<>();

    private Map<String, Map<String, Double>> newprob;

    private Map<String, Pair<String, Double>> bestscore = new HashMap<>();

    private Map<String, List<List<String>>> segcache = new HashMap<>();

    private int np_th = 4;
    private int l_th = 15;
    private int npair = 10000;


    public String del = "\\s+";

    public TitleTranslator(){

    }

    public List<Pair<String, String>> readPairs(String infile, int max){
        List<Pair<String, String>> titlepairs = new ArrayList<>();

        double src_nt = 0, tgt_nt = 0;
        try {
            for(String line: LineIO.read(infile)){
                String[] parts = line.split("\t");
                if(parts.length < 2) continue;

                if(parts[0].equals(parts[1])) continue;

                if(parts[0].contains("/") || parts[1].contains("/")) continue;

                parts[0] = parts[0].replaceAll(",", "").replaceAll("\\s+", " ");
                parts[1] = parts[1].replaceAll(",", "").replaceAll("\\s+", " ");

                titlepairs.add(new Pair<>(parts[0], parts[1]));

                src_nt += parts[0].split(del).length;
                tgt_nt += parts[1].split("\\s+").length;

//                String[] p0 = parts[0].split("-");
//                String[] p1 = parts[1].split("-");
//
//                if(p0.length != p1.length) continue;
//
//                for(int i = 0; i < p0.length; i++) {
//
////                    if(p0[i].length() >15 || p1[i].length() > 15) continue;
//
//                    if (p0[i].toLowerCase().equals(p1[i].toLowerCase())) continue;
//
//                    titlepairs.add(new Pair<>(p0[i], p1[i]));
//                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        titlepairs = titlepairs.stream().distinct().collect(toList());
        System.out.println("Read "+titlepairs.size()+" pairs");
        System.out.println("# distinct pairs: "+titlepairs.stream().distinct().count());
        System.out.println(src_nt/titlepairs.size()+" "+tgt_nt/titlepairs.size());

        if(max > 0)
            titlepairs = titlepairs.subList(0,Math.min(max, titlepairs.size()));
        return titlepairs;
    }

    private void normalizeProbTGivenS(){
//        System.out.println("Normalizing Pr(T|S)...");
        for(String s: s2t2prob.keySet()){
            Map<String, Double> t2prob = s2t2prob.get(s);
            double sum = 0;
            for(String t: t2prob.keySet()){
                sum+=t2prob.get(t);
            }
            for(String t: t2prob.keySet()){
                if(sum<0.00000000001)
                    t2prob.put(t, 0.0);
                else
                    t2prob.put(t, t2prob.get(t)/sum);
            }
        }
    }
    private void normalizeProb(Map<String, Map<String, Double>> map){
//        System.out.println("Normalizing probs...");
        for(String s:  map.keySet()){
            Map<String, Double> t2prob = map.get(s);
            double sum = 0;
            for(String t: t2prob.keySet()){
                sum+=t2prob.get(t);
            }
            for(String t: t2prob.keySet()){
                t2prob.put(t, t2prob.get(t)/sum);
            }
        }
    }

//    private String getAlignmentKey(List<String> seg1, List<String> seg2){
//        return seg1.stream().collect(joining("-"))+"-"+seg2.stream().collect(joining("-"));
//    }
//
//    public class SourceWorker implements Runnable {
//
//        private String source;
//        private List<Pair<String, String>> pairs;
//        private Map<String, Double> align2prob = new HashMap<>();
//        private List<List<String>> src_segs;
//
//        public SourceWorker(List<Pair<String, String>> pairs, String src) {
//            this.pairs = pairs;
//            this.source = src.toLowerCase();
//            src_segs = getAllSegment(source);
//        }
//
//        @Override
//        public void run() {
//            for (Pair<String, String> pair : pairs) {
//                String target = pair.getSecond().toLowerCase();
//                List<List<String>> tgt_segs = getAllSegment(target);
//
//                for (List<String> ss : src_segs) {
//                    for (List<String> ts : tgt_segs) {
//                        if (ss.size() == ts.size()) {
//
//                            double ptgivens = 1.0;
//                            for (int i = 0; i < ss.size(); i++) {
//                                String s = ss.get(i);
//                                String t = ts.get(i);
//                                ptgivens *= s2t2prob.get(s).get(t);
//                            }
////                            String akey = ss.stream().collect(joining("-"));
//                            String akey = getAlignmentKey(ss, ts);
//
//                            if (!align2prob.containsKey(akey)) align2prob.put(akey, 0.0);
//                            align2prob.put(akey, align2prob.get(akey) + ptgivens);
//                        }
//                    }
//                }
//            }
//            double sum = 0;
//            for(String align: align2prob.keySet())
//                sum += align2prob.get(align);
//
//            for(String align: align2prob.keySet())
//                align2prob.put(align, align2prob.get(align)/sum);
//
//            synchronized (src2align){
//                src2align.put(source, align2prob);
//            }
//        }
//    }
//
//    private void computeAlignProb(List<Pair<String, String>> pairs){
//        System.out.println("Computing Pr(A|S)...");
//
//        ExecutorService executor = Executors.newFixedThreadPool(30);
//        Map<String, List<Pair<String, String>>> src2pairs = pairs.stream().collect(groupingBy(x -> x.getFirst()));
//        for(String source: src2pairs.keySet()){
//            executor.execute(new SourceWorker(src2pairs.get(source), source));
//        }
//        executor.shutdown();
//        try {
//            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//    }

    public class PairWorker implements Runnable {

        private Pair<String, String> pair;
        private double wordprob;

        public PairWorker(Pair<String, String> pair, double wordprob) {
            this.pair = pair;
            this.wordprob = wordprob;
        }

        @Override
        public void run() {
            String source = pair.getFirst().toLowerCase();
            String target = pair.getSecond().toLowerCase();
            List<List<String>> src_segs = getAllSegment(source);
            List<List<String>> tgt_segs = getAllSegment(target);

            double aprob_sum = 0;

            Map<String, Map<String, Double>> tmp_prob = new HashMap<>();

            for(List<String> ss: src_segs) {
                for (List<String> ts : tgt_segs) {
                    if (ss.size() == ts.size()) {

                        double aprob = 1.0;
                        for (int i = 0; i < ss.size(); i++) {
                            String s = ss.get(i);
                            String t = ts.get(i);

                            aprob *= s2t2prob.get(s).get(t);
                        }
                        aprob_sum += aprob;

                        for (int i = 0; i < ss.size(); i++) {
                            String s = ss.get(i);
                            String t = ts.get(i);
                            addToMap(s, t, aprob, tmp_prob);
                        }
                    }
                }
            }

            if(aprob_sum == 0){
//                System.out.println("aprob_sum = 0");
//                System.out.println(pair);
                return;
            }

            synchronized (newprob) {
                for (String s : tmp_prob.keySet()) {
                    for (String t : tmp_prob.get(s).keySet()) {
                        if(!tmp_prob.get(s).get(t).isNaN() && !Double.isNaN(aprob_sum))
                            addToMap(s, t, tmp_prob.get(s).get(t)/aprob_sum*wordprob, newprob);
                        else {
                            System.out.println(tmp_prob.get(s).get(t) + " " + aprob_sum);
                            System.exit(-1);
                        }
                    }
                }
            }
        }
    }

    private void updateProb(List<Pair<String, String>> pairs, List<Double> wordprobs){

//        System.out.println("Updating Pr(T|S)");
        ExecutorService executor = Executors.newFixedThreadPool(20);
        newprob = new HashMap<>();
        int cnt = 0;
        for(int i = 0; i < pairs.size(); i++){
            if(cnt++%100 == 0)
                System.out.print(cnt+"\r");
            executor.execute(new PairWorker(pairs.get(i), wordprobs.get(i)));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        for(String s: s2t2prob.keySet()){
            for(String t: s2t2prob.get(s).keySet()){
                if(newprob.containsKey(s) && newprob.get(s).containsKey(t))
                    s2t2prob.get(s).put(t, newprob.get(s).get(t));
            }
        }

//        s2t2prob = newprob;
    }

    public void addToMap(String key1, String key2, Double value, Map<String, Map<String, Double>> map){
        if(!map.containsKey(key1)) map.put(key1, new HashMap<>());

        Map<String, Double> submap = map.get(key1);

        if(!submap.containsKey(key2)) submap.put(key2, 0.0);

        submap.put(key2, submap.get(key2)+value);

    }

    public void addToMap(String key, Double value, Map<String, Double> map){

        if(!map.containsKey(key)) map.put(key, 0.0);

        map.put(key, map.get(key)+value);

    }

    private void initJointProb(List<Pair<String[], String[]>> pairs){

        System.out.println("Initialing probabilities...");
        int cnt = 0;
        for(Pair<String[], String[]> pair: pairs){
            if(cnt++%100 == 0)
                System.out.print(cnt+"\r");
//            System.out.println(pair.getFirst());
            for(String source: pair.getFirst()){
                for(String target: pair.getSecond()){
                    List<List<String>> src_segs = getAllSegment(source.toLowerCase());
                    List<List<String>> tgt_segs = getAllSegment(target.toLowerCase());

                    for(List<String> ss: src_segs){
                        for(List<String> ts: tgt_segs){
                            if(ss.size() == ts.size()){

                                for(int i = 0; i < ss.size(); i++){
                                    String s = ss.get(i);
                                    String t = ts.get(i);
                                    addToMap(s, t, 1.0, s2t2prob);
                                }
                            }
                        }
                    }
                }
            }
        }

        normalizeProbTGivenS();

        System.out.println("initializing alignment probibilities");
        for(Pair<String[], String[]> pair: pairs){
            int l = pair.getSecond().length;
            int m = pair.getFirst().length;
            for(int i = 0; i < pair.getSecond().length; i++){
                String key1 = i+"_"+l+"_"+m;
//                addToMap(key1, "null", 1.0, ilm2j2prob);  // no null first
                for(int j = 0; j < pair.getFirst().length; j++) {
                    addToMap(key1, String.valueOf(j), 1.0, ilm2j2prob);
                    addToMap(pair.getSecond()[i], pair.getFirst()[j], 1.0, e2f2prob);
                }
            }
        }

        normalizeProb(ilm2j2prob);
        normalizeProb(e2f2prob);

    }

    private void initProb(List<Pair<String, String>> pairs){
        System.out.println("Initialing probabilities...");
        int cnt = 0;
        for(Pair<String, String> pair: pairs){
            if(cnt++%100 == 0)
                System.out.print(cnt+"\r");
//            System.out.println(pair.getFirst());
            String source = pair.getFirst().toLowerCase();
            String target = pair.getSecond().toLowerCase();
//            System.out.println(source+" "+target);
            List<List<String>> src_segs = getAllSegment(source);
            List<List<String>> tgt_segs = getAllSegment(target);

            for(List<String> ss: src_segs){
                for(List<String> ts: tgt_segs){
                    if(ss.size() == ts.size()){

                        for(int i = 0; i < ss.size(); i++){
                            String s = ss.get(i);
                            String t = ts.get(i);
                            addToMap(s, t, 1.0, s2t2prob);
                        }
                    }
                }
            }
        }

        System.out.println("prob size "+s2t2prob.size());
//        System.out.println("# segment mappings: "+segmapcnt.size());

    }

    private void printProbs(){
        System.out.println("========= Dumping probs =========");
        for(String s: s2t2prob.keySet()){
            for(String t: s2t2prob.get(s).keySet()){
                System.out.println(s+"\t"+t+"\t"+s2t2prob.get(s).get(t));
            }
        }
    }

    public void loadProbs(String file){
        System.out.println("Loading model..."+file);

        s2t2prob = new HashMap<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            while(line != null){

                String[] parts = line.trim().split("\t");

                if(!s2t2prob.containsKey(parts[0])) s2t2prob.put(parts[0], new HashMap<>());

                double prob = Double.parseDouble(parts[2]);
//                if(prob > 0)
                    s2t2prob.get(parts[0]).put(parts[1], prob*0.5);

                line = br.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Map.Entry<String, Double>> generate(String str){

        List<List<String>> segs = getAllSegment(str);
        Map<String, Double> trans2score = new HashMap<>();

        for(List<String> seg: segs){

            double score = 1;
            String trans = "";
            for(String s: seg){
                if(bestscore.containsKey(s)){
//                    System.out.println("get "+s);
                    score *= bestscore.get(s).getSecond();
                    trans += " "+bestscore.get(s).getFirst()+" "+bestscore.get(s).getSecond();
                }
                else if(s2t2prob.containsKey(s)){
                    List<Map.Entry<String, Double>> t2prob = s2t2prob.get(s).entrySet().stream()
                            .sorted((x1, x2) -> Double.compare(x2.getValue(), x1.getValue()))
                            .collect(toList());

                    score *= t2prob.get(0).getValue();
                    trans += " "+t2prob.get(0).getKey()+t2prob.get(0).getValue();

                    bestscore.put(s, new Pair<>(t2prob.get(0).getKey(), t2prob.get(0).getValue()));
                }
                else{
                    trans="";
                    break;
//                    trans+=" "+s;
//                    score *= 0.9;
                }
            }

            if(!trans.isEmpty()) {
                trans2score.put(trans, score);//*Math.pow(0.5, seg.size()));
            }
        }

        return trans2score.entrySet().stream()
//                .sorted((x1, x2) -> Integer.compare(x1.getKey().length(), x2.getKey().length()))
                .sorted((x1, x2) -> Double.compare(x2.getValue(), x1.getValue()))
//                .map(x -> x.getKey())
                .collect(toList());
    }

    public void testGenerate(String testfile, String modelfile){

        loadProbs(modelfile);

        List<Pair<String, String>> test_pairs = readPairs(testfile, -1);

        int cnt = 0;
        double totalf1 = 0;
        for(Pair<String, String> pair: test_pairs){
            if(cnt++%100 == 0) System.out.print(cnt+"\r");

            List<Map.Entry<String, Double>> trans = generate(pair.getFirst());

            System.out.println();
            System.out.println(pair.getFirst()+" "+pair.getSecond());
            trans.forEach(x -> System.out.println("\t"+x));
            System.out.println();

            List<String> refs = new ArrayList<>();
            refs.add(pair.getSecond());
            if(trans.size()>0) {
                double f1 = Utils.GetFuzzyF1(trans.get(0).getKey(), refs);
//            System.out.println(pair.getFirst()+" -> "+trans.get(0)+" "+f1);

                totalf1 += f1;
            }

        }

        System.out.println("F1 "+totalf1/test_pairs.size());

    }

    private void writeAlignProbs(String path){
        System.out.println("Writing align probs...");

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path+".align"));
//            for(String ilm: ilm2j2prob.keySet()){
//                for(String j: ilm2j2prob.get(ilm).keySet()){
//                    bw.write(ilm+"\t"+j+"\t"+ilm2j2prob.get(ilm).get(j)+"\n");
//                }
//            }

            for(String lm: lm2a2prob.keySet()){
                for(String a: lm2a2prob.get(lm).keySet())
                    bw.write(lm+"\t"+a+"\t"+lm2a2prob.get(lm).get(a)+"\n");
            }
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void writeProbs(String path){
        System.out.println("Writing probs...");

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(path));
            for(String s: s2t2prob.keySet()){
                for(String t: s2t2prob.get(s).keySet()){
                    if(!s.trim().isEmpty() && !t.trim().isEmpty()) {
                        Double val = s2t2prob.get(s).get(t);
                        if(val>0.0000000001)
                            bw.write(s + "\t" + t + "\t" + s2t2prob.get(s).get(t) + "\n");
                    }
                }
            }
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Re-implementation of Jeff's transliterator
     * @param pairs
     */
    public void trainTransliterateProb(List<Pair<String, String>> pairs, List<Pair<String, String>> test_pairs, String modelfile){

        int n_iter = 5;
        initProb(pairs);
        normalizeProbTGivenS();
//        computeAlignProb(pairs);
//        printProbs();

        List<Double> wordprobs = new ArrayList<>();
        pairs.forEach(x -> wordprobs.add(1.0));

        for(int iter = 0; iter < n_iter; iter++) {
            System.out.println("========== Iteration "+iter+" ===========");
            updateProb(pairs, wordprobs);
            normalizeProbTGivenS();
//            computeAlignProb(pairs);
//            printProbs();

//            writeProbs(modelfile + "." + iter);
            if (test_pairs != null)
                evalModel(test_pairs);
        }

//        writeProbs(modelfile);
    }


    private List<List<String>> getAllSegment(String str){


        if(segcache.containsKey(str))
            return segcache.get(str);

        List<List<String>> ret = new ArrayList<>();

        // the base case: return the only character
        if(str.length()==1){
            ret.add(Arrays.asList(str));
            return ret;
        }

        // split at one place, and recurse the rest
        for(int i = 1; i < str.length(); i++){
            String head = str.substring(0, i);
            String tail = str.substring(i, str.length());

            for(List<String> segs: getAllSegment(tail)){
                List<String> tmp = new ArrayList<>();
                tmp.add(head);
                tmp.addAll(segs);
                ret.add(tmp);
            }
        }

        // add the full string, no split
        ret.add(Arrays.asList(str));

        segcache.put(str, ret);

        return ret;
    }

    private List<Pair<String, String>> alignWords(List<Pair<String[], String[]>> pairs){

        List<Pair<String, String>> ret = new ArrayList<>();

        for(Pair<String[] ,String[]> pair: pairs){


            if(pair.getFirst().length!=pair.getSecond().length) continue;

            for(int i = 0; i < pair.getFirst().length; i++) {
                ret.add(new Pair<>(pair.getFirst()[i], pair.getSecond()[i]));
            }
        }

        System.out.println("After aligning words, #pairs "+ret.size());
        return ret;
    }

    public void alignAndLearn(String trainfile, String testfile, String modelfile){

        List<Pair<String[], String[]>> part_pairs = selectAndSplitTrain(trainfile);
        List<Pair<String, String>> test_pairs = readPairs(testfile, -1);
        alignAndLearn(part_pairs, test_pairs, modelfile);
    }

    public void alignAndLearn(List<Pair<String[], String[]>> part_pairs, List<Pair<String, String>> test_pairs, String modelfile){
        List<Pair<String, String>> pairs = alignWords(part_pairs);
        trainTransliterateProb(pairs, test_pairs, modelfile);
    }

    public List<List<Integer>> perm(List<Integer> input){

        List<List<Integer>> results = new ArrayList<>();

        if(input.size() == 1){
            results.add(input);
            return results;
        }

        for(int i = 0; i < input.size(); i++){

            int a = input.get(i);

            List<Integer> rest = input.stream().filter(x -> x != a).collect(toList());

            List<List<Integer>> perms = perm(rest);

            for(List<Integer> p: perms)
                p.add(input.get(i));

            results.addAll(perms);
        }

        return results;
    }

    public List<Integer> getBsetPerm(int k){
        List<Integer> idxarray = new ArrayList<>();
        for(int i = 0; i < k; i++) idxarray.add(i);

        List<List<Integer>> perms = perm(idxarray);

        double max_a = -1;
        List<Integer> max_perm = null;
        for(List<Integer> p: perms){
            double score = 1.0;
            for( int i = 0; i < p.size(); i++){
                String ilm = i + "_" + p.size() + "_" + p.size();
                String j = String.valueOf(p.get(i));
                score *= ilm2j2prob.get(ilm).get(j);
            }
            if(score > max_a){
                max_a = score;
                max_perm = p;
            }
        }

        System.out.println(k+" "+max_perm);

        return max_perm;
    }

    public void evalModel(List<Pair<String, String>> pairs, SPModel model){

        double correctmrr = 0;
        double correctacc = 0;
        double totalf1 = 0;

        Map<Integer, List<Integer>> best_align = new HashMap<>();

        model.setMaxCandidates(20);

        for(Pair<String, String> pair: pairs){

            String[] parts = pair.getFirst().split(del);
            List<String> preds = new ArrayList<>();
            List<List<String>> predsl = new ArrayList<>();

//            if(!best_align.containsKey(parts.length))
//                best_align.put(parts.length, getBsetPerm(parts.length));

            for(String part: parts) {
                try {
                    List<Pair<Double, String>> prediction = model.Generate(part).toList();

                    for(int i = 0; i < prediction.size(); i++){
                        if(predsl.size() > i)
                            predsl.get(i).add(prediction.get(i).getSecond());
                        else {
                            List<String> tmp = new ArrayList<>();
                            tmp.add(prediction.get(i).getSecond());
                            predsl.add(tmp);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            predsl = predsl.stream().filter(x -> x.size() == parts.length).collect(toList());


            // no reorder
            preds = predsl.stream().map(x -> x.stream().collect(joining(" "))).collect(toList());
            //reorder
//            for(List<String> p: predsl){
//                String s = "";
//                for(Integer i: best_align.get(p.size())){
//                    s+=p.get(i)+" ";
//                }
//                preds.add(s.trim());
//            }

            int bestindex = -1;

            if(preds.size()>0) {
                List<String> refs = Arrays.asList(pair.getSecond());
                totalf1 += Utils.GetFuzzyF1(preds.get(0), refs);
            }

            int index = preds.indexOf(pair.getSecond());
            if(bestindex == -1 || index < bestindex){
                bestindex = index;
            }

            if (bestindex >= 0) {
                correctmrr += 1.0 / (bestindex + 1);
                if(bestindex == 0){
                    correctacc += 1.0;
                }
            }
        }
        double mrr = correctmrr / (double)pairs.size();
        double acc = correctacc / (double)pairs.size();
        double f1 = totalf1 / (double)pairs.size();

        System.out.println("=============");
        System.out.println("AVGMRR=" + mrr);
        System.out.println("AVGACC=" + acc);
        System.out.println("AVGF1 =" + f1);

    }

    public void evalModel(List<Pair<String, String>> pairs){

        SPModel model = new SPModel(s2t2prob);

        evalModel(pairs, model);
    }

    public void evalModel(List<Pair<String, String>> pairs, String modelfile){

        SPModel model = null;
        try {
            model = new SPModel(modelfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        evalModel(pairs, model);
    }

    public void evalModel(String testfile, String modelfile){
        List<Pair<String, String>> pairs = readPairs(testfile, -1);

        SPModel model = null;
        try {
            model = new SPModel(modelfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        evalModel(pairs, model);

    }


    public void calGenerateProb(List<Pair<String, String>> pairs){

        System.out.println("Calculating word generationg probibiliries...");

        en2fo2prob = new HashMap<>();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        for(Pair<String, String> pair: pairs) {
            executor.execute(new GenProbWorker(pair));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        normalizeProb(en2fo2prob);
    }

    public class GenProbWorker implements Runnable {

        private Pair<String, String> pair;

        public GenProbWorker(Pair<String, String> pair) {
            this.pair = pair;
        }

        @Override
        public void run() {
            List<List<String>> src_segs = getAllSegment(pair.getFirst());
            List<List<String>> tgt_segs = getAllSegment(pair.getSecond());

            double aprob_sum = 0;

            Map<String, Map<String, Double>> tmp_prob = new HashMap<>();

            for (List<String> ss : src_segs) {
                for (List<String> ts : tgt_segs) {
                    if (ss.size() == ts.size()) {

                        double aprob = 1.0;
                        for (int k = 0; k < ss.size(); k++) {
                            String s = ss.get(k);
                            String t = ts.get(k);
                            aprob *= s2t2prob.get(s).get(t);
                        }
                        aprob_sum += aprob;
                    }
                }
            }

            synchronized (en2fo2prob) {
                addToMap(pair.getSecond(), pair.getFirst(), aprob_sum, en2fo2prob);
//                        addToMap(en_word, fo_word, e2f2prob.get(en_word).get(fo_word), en2fo2prob);
            }
        }
    }

    private List<TitlePair> initJointProb1(List<Pair<String[], String[]>> pairs){

        List<TitlePair> ret = new ArrayList<>();
        System.out.println("Initialing probabilities...");
        int cnt = 0;
        for(Pair<String[], String[]> pair: pairs) {
                System.out.print((cnt++) + " "+pair.getFirst().length+" "+pair.getSecond().length+"\r");
            TitlePair tpair = new TitlePair(pair);
            tpair.populateAllAssignments();   // only for this new method
            ret.add(tpair);

            for(String a: tpair.align2pairs.keySet()){
                addToMap(tpair.lm, a, 1.0, lm2a2prob);
            }

            for(String a: tpair.align2pairs.keySet()) {
                for(Pair<String, String> p: tpair.align2pairs.get(a)) {
                    List<List<String>> src_segs = getAllSegment(p.getFirst().toLowerCase());
                    List<List<String>> tgt_segs = getAllSegment(p.getSecond().toLowerCase());

                    for(List<String> ss: src_segs){
                        for(List<String> ts: tgt_segs){
                            if(ss.size() == ts.size()){

                                for(int i = 0; i < ss.size(); i++){
                                    String s = ss.get(i);
                                    String t = ts.get(i);
                                    addToMap(s, t, 1.0, s2t2prob);
                                }
                            }
                        }
                    }
                }
            }
        }
        normalizeProbTGivenS();
        normalizeProb(lm2a2prob);

        return ret;
    }

    public void updateJointProbs1(List<TitlePair> tpairs){

        List<Pair<String, String>> allpairs = tpairs.stream()
                .flatMap(x -> x.align2pairs.values().stream().flatMap(y -> y.stream()))
                .collect(toList());
        calGenerateProb(allpairs);

        List<Pair<String, String>> train_pairs = new ArrayList<>();  // transliteration training data
        List<Double> wordprobs = new ArrayList<>();
//        System.out.println("Updating q...");
        for(TitlePair tpair: tpairs){

            Map<String, Double> a2prob = new HashMap<>();
            double pas_sum = 0;
            for(String a: tpair.align2pairs.keySet()){
                double pairprob = 1;
                for(Pair<String, String> wpair: tpair.align2pairs.get(a)){

                    pairprob *= en2fo2prob.get(wpair.getSecond()).get(wpair.getFirst());

                }
                double pa = lm2a2prob.get(tpair.lm).get(a) * pairprob;
                a2prob.put(a, pa);
                pas_sum += pa;
            }

            for (String a : a2prob.keySet()) {
                double ap = a2prob.get(a) / pas_sum;
                addToMap(tpair.lm, a, ap, lm2a2c);
                addToMap(tpair.lm, ap, lm2c);

                for(Pair<String, String> wpair: tpair.align2pairs.get(a)){
                    train_pairs.add(wpair);
                    wordprobs.add(ap);
                }
            }
        }

        // Update q(a|l,m) = c(a|l,m)/c(l,m)
        for(String lm: lm2a2prob.keySet()){
            for(String a: lm2a2prob.get(lm).keySet()){
                lm2a2prob.get(lm).put(a, lm2a2c.get(lm).get(a)/lm2c.get(lm));
            }
        }

//        // Update t(f|e)
//        for(String e: e2c.keySet()){
//            for(String f: e2f2c.get(e).keySet()){
//                e2f2prob.get(e).put(f, e2f2c.get(e).get(f)/e2c.get(e));
//            }
//        }


        updateProb(train_pairs, wordprobs);
        normalizeProbTGivenS();
    }

    public void updateJointProbs(List<Pair<String[], String[]>> pairs){

        List<Pair<String, String>> allpairs = new ArrayList<>();
        for(Pair<String[], String[]> pair: pairs) {
            for (int i = 0; i < pair.getSecond().length; i++) {
                String en_word = pair.getSecond()[i];
                for (int j = 0; j < pair.getFirst().length; j++) {
                    String fo_word = pair.getFirst()[j];
                    allpairs.add(new Pair<>(fo_word, en_word));
                }
            }
        }
        calGenerateProb(allpairs);

        List<Double> wordprobs = new ArrayList<>();
        System.out.println("Updating q...");
        for(Pair<String[], String[]> pair: pairs){
            String[] e = pair.getSecond();
            String[] f = pair.getFirst();
            int l = e.length;
            int m = f.length;
            for(int i = 0; i < l; i++){
                String ilm = i + "_" + l + "_" + m;

                double sum_pij = 0;
                List<Double> pijs = new ArrayList<>();
                for(int j = 0; j < m; j++){
                    Double q = ilm2j2prob.get(ilm).get(String.valueOf(j));
                    Double wordprob = en2fo2prob.get(e[i]).get(f[j]);
//                    Double wordprob = e2f2prob.get(e[i]).get(f[j]);
                    double pij = q * wordprob;
                    pijs.add(pij);
                    sum_pij += pij;
                }

                // Updating counts for c(j|i,l,m) and c(i,l,m)
                for(int j = 0; j < m; j++){
                    double pij = pijs.get(j) / sum_pij;
                    wordprobs.add(pij);
                    addToMap(ilm, String.valueOf(j), pij, ilm2j2c);
                    addToMap(ilm, pij, ilm2c);

                    addToMap(e[i], f[j], pij, e2f2c);
                    addToMap(e[i], pij, e2c);

                }
            }
        }

        // Update q(j|i,l,m) = c(j|i,l,m)/c(i,l,m)
        for(String ilm: ilm2j2prob.keySet()){
            for(String j: ilm2j2prob.get(ilm).keySet()){
                ilm2j2prob.get(ilm).put(j, ilm2j2c.get(ilm).get(j)/ilm2c.get(ilm));
            }
        }

        // Update t(f|e)
        for(String e: e2c.keySet()){
            for(String f: e2f2c.get(e).keySet()){
                e2f2prob.get(e).put(f, e2f2c.get(e).get(f)/e2c.get(e));
            }
        }

        // pick the most probable alignments to generate training pairs for transliteration

        List<Pair<String, String>> train_pairs = new ArrayList<>();
        for(Pair<String[], String[]> pair: pairs){
            String[] e = pair.getSecond();
            String[] f = pair.getFirst();
            int l = e.length;
            int m = f.length;
            for(int i = 0; i < l; i++) {
                String ilm = i + "_" + l + "_" + m;

                for(int j = 0; j < m; j++){
                    train_pairs.add(new Pair<>(f[j], e[i]));
                }

//                List<Map.Entry<String, Double>> sort_prob = ilm2j2prob.get(ilm).entrySet().stream()
//                        .sorted((x1, x2) -> Double.compare(x2.getValue(), x1.getValue()))
//                        .collect(Collectors.toList());
//
//                int topidx = Integer.parseInt(sort_prob.get(0).getKey());
//
//                train_pairs.add(new Pair<>(f[topidx], e[i]));
//                wordprobs.add(1.0);
            }
        }

        updateProb(train_pairs, wordprobs);
        normalizeProbTGivenS();
    }

    private List<Pair<String[], String[]>> selectAndSplitTrain(String infile){
        List<Pair<String, String>> pairs = readPairs(infile, npair);
        List<Pair<String[], String[]>> part_pairs = new ArrayList<>();

        for(Pair<String, String> pair: pairs){

            String[] parts1 = pair.getFirst().split(del);
            String[] parts2 = pair.getSecond().split("\\s+");

            if(parts1.length > np_th || parts2.length > np_th) continue;

            boolean bad = false;
            for(String part: parts1)
                if(part.length()> l_th || part.trim().isEmpty()) bad = true;
            for(String part: parts2)
                if(part.length()> l_th || part.trim().isEmpty()) bad = true;

            if(bad) continue;

            part_pairs.add(new Pair<>(parts1, parts2));
        }


        System.out.println("# training title pairs "+part_pairs.size());
        return part_pairs;

    }

    public void jointTrainAlignTrans(String infile, String testfile, String modelfile){

        List<Pair<String, String>> test_pairs = readPairs(testfile, -1);

        List<Pair<String[], String[]>> part_pairs = selectAndSplitTrain(infile);


//        alignAndLearn(pairs, test_pairs,"tmp");

        // initialize all probabilities
        initJointProb(part_pairs);
//        List<TitlePair> tpairs = initJointProb1(part_pairs);


        int iter = 5;
        for(int i = 0; i < iter; i++) {
            System.out.println("---------------- Iteration "+i+"---------------");
            updateJointProbs(part_pairs);
//            updateJointProbs1(tpairs);
//            writeProbs(modelfile+".iter"+i);
//            writeAlignProbs(modelfile+".iter"+i);
            evalModel(test_pairs);
        }

        writeProbs(modelfile);
        writeAlignProbs(modelfile+".align");

    }

    public static void main(String[] args) {


        TitleTranslator tt = new TitleTranslator();

        String lang = args[0];
        if(lang.equals("zh"))
            tt.del = "·";

//        String str = "a b c";
//        for(String s: str.split(tt.del))
//            System.out.println(s);
//
//        System.exit(-1);

        List<String> types = Arrays.asList("all", "loc", "org", "per");


//        String type = args[1];

        for(String type: types) {
//        String infile = "/shared/corpora/ner/gazetteers/"+lang+"/all."+lang+".single.train";
            String infile = "/shared/corpora/ner/gazetteers/" + lang + "/"+type+".pair.train";
            String testfile = "/shared/corpora/ner/gazetteers/" + lang + "/" + type + ".pair.test";
//        String modelfile = "/shared/corpora/ner/gazetteers/"+lang+"/model/probsss";
            String modelfile = "/shared/corpora/ner/gazetteers/" + lang + "/model/" + type + ".title.tmp";
//        tt.alignAndLearn(infile, testfile, modelfile);

            tt.jointTrainAlignTrans(infile, testfile, modelfile);
        }



//        tt.evalModel(testfile, modelfile);
    }
}
