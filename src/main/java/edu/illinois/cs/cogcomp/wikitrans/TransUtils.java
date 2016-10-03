package edu.illinois.cs.cogcomp.wikitrans;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ctsai12 on 9/28/16.
 */
public class TransUtils {
    public static List<List<Integer>> getAllAssignments(int l, int m){
        List<List<Integer>> ret = new ArrayList<>();

        if(l == 0){
            ret.add(new ArrayList<>());
            return ret;
        }


        List<List<Integer>> ass = getAllAssignments(l - 1, m);
        for(int j = 0; j < m; j++){
            for(List<Integer> as: ass){
                List<Integer> tmp = new ArrayList<>(as);
                tmp.add(j);
                ret.add(tmp);
            }
        }

        return ret;
    }

}
