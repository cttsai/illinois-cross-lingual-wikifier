package edu.illinois.cs.cogcomp.xlwikifier.experiments;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.transliteration.SPModel;
import edu.illinois.cs.cogcomp.utils.TopList;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.xlel21.TransLookUp;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 1/19/16.
 */
public class Transliterator {

    private SPModel model;
    private Map<String, List<String>> cand_cache = new HashMap<>();
    private Map<String, List<String>> seq_map;
    private Set<String> seq_queries;
    private String seq_path = "/shared/experiments/ctsai12/workspace/sequitur/cache/map-";
    private boolean use_seq = false;
    private String lang;
    private Map<String, String> phrasemap;
    private Map<String, String> wordmap;
    private int ncand = 1;
    public int ntrans = 0;
    public Transliterator(String lang){
        this.lang = lang;
        try {
            if(use_seq){
                seq_map = new HashMap<>();
                for(String line: LineIO.read(seq_path+lang)){
                    String[] tokens = line.toLowerCase().split("\t");
                    List<String> tmp = Arrays.asList(tokens[1].split("\\s+"));
                    seq_map.put(tokens[0], tmp);
                }
                System.out.println(seq_map.get("табо"));
//                System.exit(-1);
                seq_queries = new HashSet<>();
            }
            else {
                model = new SPModel("output/probs-" + lang + ".txt");
                model.setMaxCandidates(ncand);
//            if(lang.equals("es"))
//                model = new SPModel("output/probs-Spanish.txt");
//            else if(lang.equals("ur"))
//                model = new SPModel("output/probs-Urdu.txt");
//			else if(lang.equals("ta"))
//                model = new SPModel("output/probs-Tamil.txt");
//			else if(lang.equals("he"))
//                model = new SPModel("output/probs-Hebrew.txt");
//            else if(lang.equals("tr"))
//                model = new SPModel("output/probs-Turkish.txt");
//            else if(lang.equals("el"))
//                model = new SPModel("output/probs-Greek.txt");
//            else{
//                System.out.println("Not supported language "+lang);
//                System.exit(-1);
//            }

            }
            loadPhraseAndWordMaps(lang);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadPhraseAndWordMaps(String lang){

        System.out.println("Loading phrase and word maps...");
        phrasemap = new HashMap<>();  // the title mapping which is used to generate wordmap
        wordmap = new HashMap<>();  // original training data of transliteration

        String file = "/shared/corpora/ner/gazetteers/"+lang+"/all."+lang;
        List<String> lines = null;
        try {
            lines = LineIO.read(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for(String line: lines){
            String[] parts = line.split("\t");
            phrasemap.put(parts[0], parts[1]);
        }

        file = "/shared/corpora/ner/gazetteers/"+lang+"/wordmap."+lang+"-en.2";
        try {
            lines = LineIO.read(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for(String line: lines){
            String[] parts = line.split("\t");
            if(!wordmap.containsKey(parts[0]))
                wordmap.put(parts[0], parts[1]);
        }
    }

    public String LookupPhraseMap(String str){
        str = str.toLowerCase();
        if(phrasemap.containsKey(str))
            return phrasemap.get(str);

        return null;
    }

    public String getEngTransToken(String text){
        String ret = null;
        try {
            TopList<Double,String> prediction = model.Generate(text.toLowerCase());
            for(Pair<Double, String> pred: prediction){
                ret = pred.getSecond();
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public List<String> getEngTransTokenCandsSeq(String text){
        if(seq_map.containsKey(text)) {
            List<String> ret = seq_map.get(text);
            return ret.subList(0, Math.min(ret.size(), 3));
        }
        else{
            System.out.println("missing: "+text);
            seq_queries.add(text);
            return new ArrayList<>();
        }
    }

    /**
     * The main method, given a token, return possible transliterations
     * @param text
     * @return
     */
    public List<String> getEngTransTokenCands(String text){
        List<String> ret = new ArrayList<>();

        if(wordmap.containsKey(text.toLowerCase()))
            ret.add(wordmap.get(text.toLowerCase()));

        if(ret.size() >= ncand) return ret;

        ntrans++;

        try {
            TopList<Double,String> prediction = model.Generate(text.toLowerCase());
            for(Pair<Double, String> pred: prediction){
                if(!ret.contains(pred.getSecond())) {
                    ret.add(pred.getSecond());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
    public double getProbability(String src, String trg){
//        System.out.println(src+"||"+trg);
        String[] st = src.split("\\s+");
        String[] tt = trg.split("\\s+");

        double score_s = 0;
        for(int i = 0; i < st.length; i++){
            double max_score = -1;
            for(int j = 0; j < tt.length; j++){
                double s = model.Probability(st[i], tt[j]);
                if(s > max_score) max_score = s;
            }
            score_s += max_score;
        }

        return score_s/st.length;
    }

    public String getEngTrans(String text){
        String[] tokens = text.split("\\s+");

        String ret = Arrays.asList(tokens).stream()
                .map(x -> getEngTransToken(x))
                .filter(x -> x!=null)
                .collect(joining(" "));

        if(ret.trim().isEmpty())
            return null;
        else
            return ret;
    }

    /**
     * Given a string (could have multiple tokens), generate transliteration candidates
     * @param text
     * @return
     */
    public List<String> getEngTransCands(String text){
        if(cand_cache.containsKey(text)) return cand_cache.get(text);
        String[] tokens = text.split("\\s+");
        List<List<String>> poss_token_tran = new ArrayList<>();
        for(String token: tokens){
            List<String> trancands;
            if(use_seq)
                trancands = getEngTransTokenCandsSeq(token);
            else
                trancands = getEngTransTokenCands(token);

            // only take top 1 for long mentions
            if(trancands.size() > 0 && tokens.length > 4)
                trancands = trancands.subList(0, 1);
            else
                trancands = trancands.subList(0, Math.min(trancands.size(), ncand));
            poss_token_tran.add(trancands);
        }

//        List<String> ret = new ArrayList<>();
//        for(int i = 0; i < poss_token_tran.get(0).size(); i++){
//            String tmp = "";
//            int j = 0;
//            for(j = 0; j < poss_token_tran.size(); j++){
//                if(poss_token_tran.get(j).size() <= i)
//                    break;
//                tmp += poss_token_tran.get(j).get(i)+" ";
//            }
//            if(j!=poss_token_tran.size())
//                continue;
//            ret.add(tmp.trim());
//        }

        int n_combo = 1;
        for(List<String> pos: poss_token_tran) n_combo *= pos.size();
        String[] combos = new String[n_combo];
        int[] counter = new int[poss_token_tran.size()];
        Arrays.fill(counter, 0);
        for(int i = 0; i < n_combo; i++){
            String tmp = "";
            for(int j = 0; j < counter.length; j++)
                tmp += poss_token_tran.get(j).get(counter[j])+" ";
            combos[i] = tmp.trim();

            for(int j = 0; j <counter.length; j++) {
                counter[j]++;
                if (counter[j] == poss_token_tran.get(j).size()){
                    counter[j] = 0;
                }
                else break;
            }
        }
        List<String> ret = Arrays.asList(combos);
//        System.out.println(text);
//        System.out.println(ret);
        cand_cache.put(text, ret);
        return ret;
    }

    public void printSeqQueries(){
        if(use_seq) {
            String out = seq_queries.stream().collect(joining("\n"));
            try {
                FileUtils.writeStringToFile(new File(seq_path + lang + ".query"), out, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }


    public static void main(String[] args) {

//        edu.illinois.cs.cogcomp.transliteration.Transliterator t = new edu.illinois.cs.cogcomp.transliteration.Transliterator("tr");

        Transliterator trans = new Transliterator("tr");
        System.out.println(trans.getEngTransCands(args[0].toLowerCase()));

        System.out.println("look up:");
        TransLookUp trl = new TransLookUp("tr");
        System.out.println(trl.LookupTrans(args[0].toLowerCase()));

//        System.out.println(t.getEngTrans("Grigoriy"));
//        System.out.println(t.getEngTrans("Şelihov"));

    }
}
