package edu.illinois.cs.cogcomp.lorelei.utils;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 7/17/16.
 */
public class CopyLabels {

    public static void run(String indir, String origdir, String outdir){

        try {
            int cnt = 0;
            for(File infile: new File(indir).listFiles()){
                if(cnt++%100 == 0) System.out.println(cnt);
                List<String> inlines = LineIO.read(infile.getAbsolutePath());
                List<String> origlines = LineIO.read(origdir+infile.getName());

                if(origlines.size() == inlines.size()+1){
                    if(origlines.get(origlines.size()-1).trim().isEmpty())
                        origlines = origlines.subList(0, origlines.size()-1);
                }

                if(origlines.size()+1 == inlines.size()){
                    if(inlines.get(inlines.size()-1).trim().isEmpty())
                        inlines = inlines.subList(0, inlines.size()-1);
                }

                if(inlines.size()!=origlines.size()){
                    System.out.println(infile.getName());
                    System.exit(-1);
                }

                String out = "";
                for(int i = 0; i < inlines.size(); i++){
                    if(inlines.get(i).trim().isEmpty()) {
                        if(!origlines.get(i).trim().isEmpty())
                            System.out.println("What!");
                        out += "\n";
                        continue;
                    }
                    String label = inlines.get(i).split("\t")[0];
                    List<String> parts = Arrays.asList(origlines.get(i).split("\t"));
                    out += label+"\t"+parts.subList(1, parts.size()).stream().collect(joining("\t"))+"\n";
                }

                FileUtils.writeStringToFile(new File(outdir, infile.getName()), out, "UTF-8");
            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String indir = "/shared/corpora/ner/wikifier-features/ug/newNW-SWM2-uly/";
//        String origdir = "/shared/corpora/ner/wikifier-features/ug/eval-setE-new-ann-tw/";
        String origdir = "/shared/corpora/ner/wikifier-features/ug/eval-set0-NW-new/";
        String outdir = "/shared/corpora/ner/wikifier-features/ug/newNW-SWM2/";

        run(indir, origdir, outdir);
    }
}
