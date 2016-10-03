package edu.illinois.cs.cogcomp.lorelei.utils;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by ctsai12 on 6/7/16.
 */
public class Laf2Tab {

    public static void run(String indir, String outfile) throws IOException {

        String out = "";
        for(File f: (new File(indir)).listFiles()){

            String docid = null, start = null, end =null, type=null, surface=null;
            int cnt = 0;
            for(String line: LineIO.read(f.getAbsolutePath())){
                line = line.trim();
                if(line.startsWith("<DOC ")){
                    String[] tmp = line.split("\"");
                    docid = tmp[1];
                }

                if(line.startsWith("<EXTENT ")){
                    String[] tmp = line.split("\"");
                    start = tmp[1];
                    end = tmp[3];
                    surface = tmp[4].substring(1).split("<")[0];
                }

                if(line.startsWith("<TAG>")){
                    type = line.substring(5).split("<")[0];
                    out += "Gold\t"+docid+"-"+cnt+"\t"+surface+"\t"+docid+":"+start+"-"+end+"\tNIL\t"+type+"\tNAM\t1.0\n";
                    cnt++;
                }

            }
        }
        FileUtils.writeStringToFile(new File(outfile), out, "UTF-8");
    }

    public static void main(String[] args) {
        String indir = "/shared/corpora/corporaWeb/lorelei/LDC2016E30_LORELEI_Mandarin_Incident_Language_Pack_V2.0/setE/data/annotation/entity";
        String outfile = "/shared/corpora/ner/dryrun/gold.tab";
        try {
            run(indir, outfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
