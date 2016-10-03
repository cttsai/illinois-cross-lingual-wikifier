package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.core.io.LineIO;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Created by ctsai12 on 5/1/16.
 */
public class StopWord {

    public static Set<String> getStopWords(String lang){
        Set<String> ret = new HashSet<>();
        try {
            if((new File("stopwords/stopwords."+lang)).exists()) {
                ret.addAll(LineIO.read("stopwords/stopwords." + lang).stream()
                        .map(x -> x.trim().toLowerCase()).collect(toSet()));
            }
            else
                System.out.println("No stopwords for "+lang);
            ret.addAll(LineIO.read("stopwords/puncs").stream().collect(toSet()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
}
