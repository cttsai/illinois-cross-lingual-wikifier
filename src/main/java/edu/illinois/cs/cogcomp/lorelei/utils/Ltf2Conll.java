package edu.illinois.cs.cogcomp.lorelei.utils;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by ctsai12 on 6/2/16.
 */
public class Ltf2Conll {
    private static Logger logger = LoggerFactory.getLogger(Ltf2Conll.class);


    public static Map<String, String> getAnnotations(String anndir, String filename){

        Map<String, String> offsets2type = new HashMap<>();
        try {
            String start=null, end=null, type;
            for(String line: LineIO.read(anndir+"/"+filename)){
                line = line.trim();

                if(line.startsWith("<EXTENT ")){
                    String[] tmp = line.split("\"");
                    start = tmp[1];
                    end = tmp[3];
                }

                if(line.startsWith("<TAG>")){
                    type = line.substring(5).split("<")[0];
                    offsets2type.put(start+"_"+end, type);
                }
            }
        } catch (FileNotFoundException e) {
            return offsets2type;
        }

        return offsets2type;
    }

    public static void run(String indir, String outdir, String anndir){

//        ChineseTokenizer tokenizer = new ChineseTokenizer();

        int cnt = 0;
        for(File f: new File(indir).listFiles()){
            String doc_id = f.getName().split("\\.")[0];
//            if(doc_id.contains("_SN_")) continue;
            cnt++;
            logger.info("Processing file: "+f.getAbsolutePath());

            Map<String, String> off2type = new HashMap<>();
            // if there is gold annotations
            if(anndir!=null) {
                String id = f.getName().split("\\.")[0];
                off2type = getAnnotations(anndir, id + ".laf.xml");
            }
//            System.out.println(off2type);

            List<String> lines = null;
            try {
                lines = LineIO.read(f.getAbsolutePath());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            String out = "";
            for(String line: lines){
                if(line.startsWith("<TOKEN")){
//                    String[] tokens = line.split("\\s+");
//                    String[] tmp = tokens[4].split("\"");
//                    int start = Integer.parseInt(tmp[1]);
//                    tmp = tokens[5].split("\"");
//                    int end = Integer.parseInt(tmp[1]);
////                    String surface = tmp[2].split("<")[0].substring(1);
//                    String surface = tokens[5].split(">")[1].split("<")[0];

                    String[] tokens = line.split(">");
                    String[] tmp = tokens[0].split("\"");
                    int start = Integer.parseInt(tmp[tmp.length-3]);
                    int end = Integer.parseInt(tmp[tmp.length-1]);
                    String surface = tokens[1].split("<")[0];



                    String type = "O";
                    for(String off: off2type.keySet()){
                        int goldstart = Integer.parseInt(off.split("_")[0]);
                        int goldend = Integer.parseInt(off.split("_")[1]);

                        if(start >= goldstart && end <= goldend){
                            String goldtype = off2type.get(off);
                            if(start == goldstart)
                                type = "B-"+goldtype;
                            else
                                type = "I-"+goldtype;
                        }
                    }

                    out+=type+"\t"+start+"\t"+end+"\tx\tx\t"+surface+"\tx\tx\tx\tx\n";
                }
                if(line.startsWith("</SEG")) out+="\n"; // end of a sentence
            }

            try {
                FileUtils.writeStringToFile(new File(outdir, doc_id), out, "utf-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        logger.info("Processed "+cnt+" files");
    }

    public static void main(String[] args) {
//        String indir = "/shared/corpora/corporaWeb/lorelei/dryrun/LDC2016E56_LORELEI_Year1_Dry_Run_Evaluation_IL2_V1.1/set0/data/monolingual_text/ltf";
//        String indir = "/shared/corpora/corporaWeb/lorelei/dryrun/LDC2016E56_LORELEI_Year1_Dry_Run_Evaluation_IL2_V1.1/setE/data/monolingual_text/ltf";
        String indir = "/shared/corpora/corporaWeb/lorelei/evaluation-20160705/LDC2016E57_LORELEI_IL3_Incident_Language_Pack_for_Year_1_Eval/set2/data/monolingual_text/ltf";
        indir = "/shared/corpora/corporaWeb/lorelei/uzbek/data/monolingual_text/ltf";
//        String indir = "/shared/corpora/corporaWeb/lorelei/evaluation-20160705/LDC2016E57_LORELEI_IL3_Incident_Language_Pack_for_Year_1_Eval/set0/data/translation/comparable/eng/ltf";
//        String anndir = "/shared/corpora/corporaWeb/lorelei/LDC2016E30_LORELEI_Mandarin_Incident_Language_Pack_V2.0/setE/data/annotation/entity";
//        String indir = "/shared/corpora/corporaWeb/lorelei/dryrun/LDC2016E56_LORELEI_Year1_Dry_Run_Evaluation_IL2_V1.1/set1/data/monolingual_text/ltf";
        String outdir = "/shared/corpora/ner/eval/column/set2-mono-new";
        outdir = "/shared/corpora/ner/lorelei/uz/mono";
//        String outdir = "/shared/corpora/ner/eval/column/set0-comparable/eng1";
        run(indir, outdir, null);
    }
}
