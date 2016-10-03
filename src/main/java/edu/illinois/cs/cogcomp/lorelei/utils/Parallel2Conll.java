package edu.illinois.cs.cogcomp.lorelei.utils;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ctsai12 on 6/10/16.
 */
public class Parallel2Conll {

    /**
     * Assuming files are tokenized
     * @param file1
     * @param file2
     */
    public static void run(String file1, String file2, String out1, String out2) throws FileNotFoundException {

        List<List<String>> lines = parseFile(file1);
        List<List<String>> lines1 = parseFile(file2);

        if(lines.size()!=lines1.size()){
            System.out.println("Size doesn't match!!");
            System.exit(-1);
        }

        output(lines, out1);
        output(lines1, out2);


    }

    private static void output(List<List<String>> lines, String outdir){
        String out = "";
        int doc_cnt = 0;
        for(int i = 0; i < lines.size(); i++){
            for(String token: lines.get(i))
                out+=token;
            out+="\n";

            if((i+1)%100 == 0) {
                try {
                    FileUtils.writeStringToFile(new File(outdir, String.valueOf(doc_cnt)), out, "UTF-8");
                    doc_cnt++;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                out = "";
            }
        }
        try {
            FileUtils.writeStringToFile(new File(outdir, String.valueOf(doc_cnt)), out, "UTF-8");
            doc_cnt++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<List<String>> parseFile(String file) throws FileNotFoundException {

        System.out.println("Parsing file "+file);
        int sen_cnt = 0 ;
        List<List<String>> ret = new ArrayList<>();
        for(String line: LineIO.read(file)){
            int token_cnt = 0;
            List<String> sen = new ArrayList<>();
            String[] tokens = line.split("\\s+");
            for(String t: tokens){
                String tmp = "O\tx\tx\tx\tx\t"+t+"\tx\tx\tx\tx\t"+sen_cnt+"\t"+token_cnt+"\n";
                token_cnt++;
                sen.add(tmp);
            }
            ret.add(sen);
            sen_cnt++;
        }
        System.out.println("#Sentences "+ ret.size());
        return ret;
    }

    public static void main(String[] args) {
        String en_file = "/shared/corpora/ner/parallel/bn/eng.txt";
        String fo_file = "/shared/corpora/ner/parallel/bn/bn.txt";
        String en_out = "/shared/corpora/ner/parallel/bn/en-conll1";
        String fo_out = "/shared/corpora/ner/parallel/bn/bn-conll1";
        try {
            run(en_file, fo_file, en_out, fo_out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
