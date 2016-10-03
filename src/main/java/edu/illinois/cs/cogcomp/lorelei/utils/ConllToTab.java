package edu.illinois.cs.cogcomp.lorelei.utils;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by ctsai12 on 6/2/16.
 */
public class ConllToTab {

    private static Logger logger = LoggerFactory.getLogger(ConllToTab.class);
    public static void run(String anno_dir, String orig_dir, String outfile) throws IOException {

        String system = "UI_CCG";


        String out = "";
        for(File af: (new File(anno_dir)).listFiles()){
            String docid = af.getName();
            List<String> alines = LineIO.read(af.getAbsolutePath());
            List<String> olines = LineIO.read(orig_dir+"/"+docid);
            if(alines.size()!=olines.size()){
                logger.info(af.getName());
                logger.info("Different # lines "+alines.size()+" "+olines.size());
                System.exit(-1);
            }

            int cnt = 0;
            int start = -1, end = -1;
            String surface = "";
            String type = null;
            for(int i = 0; i <alines.size(); i++){
                if(alines.get(i).trim().isEmpty()) continue;
                String[] tokens = alines.get(i).split("\t");
                String label = tokens[0];

                if(label.startsWith("O")){
                    if(start != -1) {
                        if((end-start+1) != surface.length()){
                            System.out.println(docid+" "+surface+" "+start+" "+end+" "+surface.length());
                        }
                        out += system + "\t" + docid + "NE-" + (cnt++) + "\t" + surface + "\t" + docid + ":" + start + "-" + end + "\tNIL\t" + type + "\tNAM\t1.0\n";
                    }
                    start = -1;
                    end = -1;
                    type = null;
                    surface = "";
                }
                else{
                    String[] otokens = olines.get(i).split("\t");
                    int start_off = Integer.parseInt(otokens[1]);
                    int end_off = Integer.parseInt(otokens[2]);
                    String s = otokens[5];
                    if(label.startsWith("B")){
                        if(start != -1){
                            if((end-start+1) != surface.length()){
                                System.out.println(docid+" "+surface+" "+start+" "+end+" "+surface.length());
                            }
                            out += system+"\t"+docid+"NE-"+(cnt++)+"\t"+surface+"\t"+docid+":"+start+"-"+end+"\tNIL\t"+type+"\tNAM\t1.0\n";
                        }
                        start = start_off;
                        end = end_off;
                        type = label.substring(2);
                        surface = s;
                    }
                    else if(label.startsWith("I")){
                        end = end_off;
                        if(end-start+1 == surface.length()+s.length())
                            surface+=s;
                        else
                            surface+=" "+s;
                    }
                }
            }
            if(start != -1) {
                if ((end -start + 1) != surface.length()) {
                    System.out.println(docid+" "+surface + " " + start + " " + end+" "+surface.length());
                }
                out += system + "\t" + docid + "NE-" + (cnt++) + "\t" + surface + "\t" + docid + ":" + start + "-" + end + "\tNIL\t" + type + "\tNAM\t1.0\n";
            }
        }
        FileUtils.writeStringToFile(new File(outfile), out, "UTF-8");

    }

    public static void main(String[] args) {
//        String anno_dir = "/shared/corpora/ner/wikifier-features/zh/Test-dryrunE-nifix-wikionly";
        String anno_dir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/test1";
//        anno_dir = "/shared/corpora/ner/eval/column/trans-model-anno2";
        String orig_dir = "/shared/corpora/ner/eval/column/setE_";
        String outfile = "/shared/corpora/ner/eval/submission/ner/cp3/test1.tab";
        try {
            run(anno_dir, orig_dir, outfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
