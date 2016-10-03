package edu.illinois.cs.cogcomp.lorelei.utils;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.transliteration.SPModel;
import edu.illinois.cs.cogcomp.utils.TopList;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 7/17/16.
 */
public class ArabicToULY {


    private SPModel model;
    private Set<String> puncs;

    public ArabicToULY() {
        try {
            model = new SPModel("/shared/corpora/transliteration/lorelei/models/probs-ug-rev.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
        model.setMaxCandidates(1);


        try {
            puncs = new HashSet<>(LineIO.read("/shared/experiments/ctsai12/workspace/xlwikifier/stopwords/puncs"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getULYTabLine(String in) {
        String[] parts = in.split("\t");
        String ret = "";
        for(String t: parts)
            ret+= getULYPhrase(t)+"\t";
        return ret.trim();
    }

    public String getULYPhrase(String in) {
        String[] tokens = in.split("\\s+");
        String ret = "";
        for(String t: tokens)
            ret+= getULY(t)+" ";
        return ret.trim();
    }

    public String getULY(String in){
        if(puncs.contains(in.trim()))
            return in;
        try {
            TopList<Double, String> result = model.Generate(in);
            if(result.size() == 0) return in;
            for(Pair<Double, String> r: result)
                return r.getSecond();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return in;
    }

    public void transConll(String indir, String outdir){

        try {
            int cnt = 0;
            for(File f: new File(indir).listFiles()){

                if(cnt++%100 == 0) System.out.println(cnt);

                String out = "";
                for(String line: LineIO.read(f.getAbsolutePath())){

                    if(line.trim().isEmpty())
                        out+="\n";
                    else {
                        String[] parts = line.split("\t");
                        parts[5] = this.getULY(parts[5]);
                        out += Arrays.asList(parts).stream().collect(joining("\t"))+"\n";
                    }
                }

                FileUtils.writeStringToFile(new File(outdir, f.getName()), out, "UTF-8");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public static void main(String[] args) {
        ArabicToULY au = new ArabicToULY();

        String indir = "/shared/corpora/ner/eval/column/setE_";
//        indir = "/shared/corpora/ner/wikifier-features/ug/eval-setE-new";
        String outdir = "/shared/corpora/ner/eval/column/setE-uly_";
//        outdir = "/shared/corpora/ner/wikifier-features/ug/newE-uly";

        au.transConll(indir, outdir);

//        String f = "/shared/corpora/ner/gazetteers/ug/ni2/per";
//        String fout = "/shared/corpora/ner/gazetteers/ug/ni2/per.uly";

        String dir = "/shared/corpora/ner/gazetteers/ug/tmp1";
//        dir = "/shared/experiments/ctsai12/workspace/xlwikifier/stopwords/tmp";

//        for(File f: new File(dir).listFiles()) {
//
//            String out = "";
//
//            try {
//                for (String line : LineIO.read(f.getAbsolutePath())) {
//                    out += au.getULYTabLine(line.trim()) +"\n";
//                }
//
//                FileUtils.writeStringToFile(new File(dir+"-uly", f.getName()), out, "UTF-8");
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }


    }
}
