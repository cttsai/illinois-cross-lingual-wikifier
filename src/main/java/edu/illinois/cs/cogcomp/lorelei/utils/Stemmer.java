package edu.illinois.cs.cogcomp.lorelei.utils;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 7/14/16.
 */
public class Stemmer {

    private String[] suffixes = {"نىڭ","دىن", "دىكى", "نى", "ىنى", "تىكى"}; //{"ning", "diki", "din", "da", "ni", "mu", "gha", "tiki"};

    public String stem(String str){
        for(String suf: suffixes){
            if(str.endsWith(suf)){
                return str.substring(0, str.length() - suf.length());
            }
        }

        return str;
    }


    public void stemConllDocuments(String indir, String outdir){

        for(File file: (new File(indir)).listFiles()){

            String out = "";
            try {
                for(String line: LineIO.read(file.getAbsolutePath())){
                    if(line.trim().isEmpty())
                        out += "\n";
                    else{
                        String[] parts = line.split("\t");
                        parts[5] = stem(parts[5]);
                        out += Arrays.asList(parts).stream().collect(joining("\t"))+"\n";
                    }
                }

                FileUtils.writeStringToFile(new File(outdir, file.getName()), out, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static void main(String[] args) {
        Stemmer stemmer = new Stemmer();
        String indir = "/shared/corpora/ner/wikifier-features/ug/eval-set0-NW1-norank-ann6-filter-gaz-autocorrect1-nodev";
        String outdir = "/shared/corpora/ner/wikifier-features/ug/iter10-nodev-stem";

//        String indir = "/shared/corpora/ner/eval/column/dev-iter13";
//        String outdir = "/shared/corpora/ner/eval/column/dev-iter13-stem";

        stemmer.stemConllDocuments(indir, outdir);
    }
}
