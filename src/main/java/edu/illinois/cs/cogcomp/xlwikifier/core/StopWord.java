package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.LbjNer.IO.ResourceUtilities;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by ctsai12 on 5/1/16.
 */
public class StopWord {

    private static Map<String, Set<String>> stopwordMap = new HashMap<>();

    public static Set<String> getStopWords(String lang) {

        if(stopwordMap.containsKey(lang))
            return stopwordMap.get(lang);
        else {
            Set<String> ret = new HashSet<>();

            try {
                InputStream res = ResourceUtilities.loadResource(ConfigParameters.stopword_path+"stopwords." + lang);
                BufferedReader in = new BufferedReader(new InputStreamReader(res, "UTF-8"));
                ret.addAll(in.lines().map(x -> x.trim().toLowerCase()).collect(Collectors.toSet()));
                in.close();

                res = ResourceUtilities.loadResource(ConfigParameters.stopword_path+"puncs");
                in = new BufferedReader(new InputStreamReader(res, "UTF-8"));
                ret.addAll(in.lines().map(x -> x.trim().toLowerCase()).collect(Collectors.toSet()));
                in.close();
            }catch (Exception e){
                e.printStackTrace();
            }

            stopwordMap.put(lang, ret);
            return ret;
        }
    }
}
