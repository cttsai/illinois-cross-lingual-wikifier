package edu.illinois.cs.cogcomp.wikitrans;


import edu.illinois.cs.cogcomp.core.datastructures.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 9/28/16.
 */
public class TitlePair {

    public String[] src_title;
    public String[] tgt_title;

    public String lm;

    public Map<String, List<Pair<String, String>>> align2pairs;

    public TitlePair(Pair<String[], String[]> pair){
        src_title = pair.getFirst();
        tgt_title = pair.getSecond();
        lm = tgt_title.length+"_"+src_title.length;
    }

    public Pair<String[], String[]> getPartsPair(){
       return new Pair<>(src_title, tgt_title);
    }

    public void populateAllAssignments(){
        int l = tgt_title.length;
        int m = src_title.length;
        List<List<Integer>> assignments = TransUtils.getAllAssignments(l, m+1);  // m+1: adding null alignment
//        List<List<Integer>> assignments = TransUtils.getAllAssignments(m, l);
        // each word in src can be linked to multiple tgt words

        align2pairs = new HashMap<>();
        for (List<Integer> assign : assignments) {

            String akey = assign.stream().map(x -> String.valueOf(x)).collect(joining("_"));

            List<Pair<String, String>> wordpairs = new ArrayList<>();

//            for(int i = 0; i < l; i++){
//                String srcwords = "";
//                int n_t = 0;
//                for(int j = 0; j < m; j++){
//                    if(assign.get(j) == i){
//                        srcwords+=" "+src_title[j];
//                        n_t++;
//                    }
//                }
//
//                srcwords = srcwords.trim();
//                if(!srcwords.isEmpty() && n_t == 1){
//                    wordpairs.add(new Pair<>(srcwords, tgt_title[i]));
//                }
//            }

            for (int j = 0; j < m; j++) {
                String targetwords = "";
                int n_t = 0;
                for (int i = 0; i < l; i++) {
                    if (assign.get(i) == j) {
                        targetwords += " " + tgt_title[i];
                        n_t++;
                    }
                }

                targetwords = targetwords.trim();
                if(!targetwords.isEmpty() && (n_t == 1)){ // || (n_t == 2 && targetwords.length() < 10))) {
                    wordpairs.add(new Pair<>(src_title[j], targetwords));
                }
            }


            if(wordpairs.size() == m)
                align2pairs.put(akey, wordpairs);
        }

    }
}
