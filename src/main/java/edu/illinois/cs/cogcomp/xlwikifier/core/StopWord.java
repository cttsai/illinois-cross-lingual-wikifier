package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

/**
 * Created by ctsai12 on 5/1/16.
 */
public class StopWord {

    public static Set<String> getStopWords(String lang) {
        Set<String> ret = new HashSet<>();
        try {
            File f = new File(ConfigParameters.stopword_path, "stopwords." + lang);
            if (f.exists()) {
                ret.addAll(LineIO.read(f.getAbsolutePath()).stream()
                        .map(x -> x.trim().toLowerCase()).collect(toSet()));
            } else
                System.out.println("No stopwords for " + lang);

            ret.addAll(LineIO.read(ConfigParameters.stopword_path + "/puncs").stream().collect(toSet()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
}
