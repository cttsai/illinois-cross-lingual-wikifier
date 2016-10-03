package edu.illinois.cs.cogcomp.mlner.experiments;

import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.NERDocument;
import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.ParametersForLbjCode;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.mlner.CrossLingualNER;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.WikiDocReader;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by ctsai12 on 5/1/16.
 */
public class AnnotateWikiDocs {

    public AnnotateWikiDocs(){}

    public void annotate(String lang){

        FreeBaseQuery.loadDB(true);
        int start = 0, end = 2000000;
        String outpath = "/shared/corpora/ner/wikipedia/"+lang+"-camera";
        CrossLingualNER.setLang(lang, true);
        WikiDocReader dr = new WikiDocReader();
        List<String> paths = null;
        dr.tokenizer = MultiLingualTokenizer.getTokenizer(lang);
        try {
            String dir = "/shared/dickens/ctsai12/multilingual/wikidump/" + lang + "/" + lang + "_wiki_view/";
            paths = LineIO.read(dir+"file.list.rand");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        LangLinker ll = new LangLinker();
        ExecutorService executor = Executors.newFixedThreadPool(15);
        boolean on = false;
        for(String filename: paths.subList(start, Math.min(paths.size(),end))) {

            if(filename.equals("Kasba Gölü")) on = true;
            if(!on) continue;

            if((new File(outpath, filename).isFile())) continue;
            if(ll.translateToEn(filename, lang) == null) continue;
            if(!isNE(filename, lang)) continue;
            QueryDocument doc = dr.readWikiDocSingle(lang, filename, true);
            if(doc == null) continue;
            executor.execute(new DocAnnotator(doc, outpath));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private boolean isNE(String filename, String lang){
            String mid = FreeBaseQuery.getMidFromTitle(filename, lang);
            if(mid == null) return false;
            List<String> types = FreeBaseQuery.getTypesFromMid(mid);
            types = types.stream().filter(x -> x.contains("location") || x.contains("person") || x.contains("organization")).collect(Collectors.toList());
            return types.size() > 0;
    }

    public class DocAnnotator implements Runnable {


        private QueryDocument doc;
        private String outpath;

        public DocAnnotator(QueryDocument doc, String outpath) {
            this.outpath = outpath;
            this.doc = doc;
        }

        @Override
        public void run() {
            NERDocument nerdoc = CrossLingualNER.annotate(doc);

            String out = "";

            float avgscore = 0;
            int cnt = 0;

            int sen_cnt = 0;
            for(LinkedVector sen: nerdoc.sentences) {
//                if(sen_cnt == 0){
//                    sen_cnt++;
//                    continue;
//                }
                String senout = "";
                boolean has_ent = false;
                boolean skip = false;
                String prev_tag = "O";
                for (int j = 0; j < sen.size(); j++) {
                    NEWord w = (NEWord) sen.get(j);

//                    // remove all parentheses
//                    if(w.form.equals(")")){
//                        skip = false;
//                        continue;
//                    }
//                    if(w.form.equals("(")) skip = true;
//                    if(skip) continue;

                    String init = w.form.substring(0,1);
                    String tag = "O";
//                    if(init.toLowerCase().equals(init)){
//                        avgscore += 1;
//                    }
//                    else {
                        if (ParametersForLbjCode.currentParameters.featuresToUse.containsKey("PredictionsLevel1")) {
                            avgscore += w.predictionConfLevel2;
                            tag = w.neTypeLevel2;
                        } else {
                            avgscore += w.predictionConfLevel1;
                            tag = w.neTypeLevel1;
                        }
//                    }

                    if(tag.startsWith("I") && prev_tag.startsWith("O")){
                        tag = "O";
                    }

                    if(!tag.equals("O")) has_ent = true;
                    senout+=tag;
                    prev_tag = tag;

                    cnt++;
                    senout += "\tx\tx\tx\tx\t";
                    senout += w.form;
                    senout += "\tx\tx\tx\tx";
                    for(String wf: w.wikifierfeats)
                        senout+= "\t"+wf;
                    senout+= "\n";
                }
                senout+= "\n";
                if(avgscore/cnt == 1)
                    if(has_ent)
                        out += senout;
            }
            try {
                if(!out.isEmpty())
                    FileUtils.writeStringToFile(new File(outpath, doc.getDocID()), out, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

//            doc = null;
        }
    }

    public static void main(String[] args) {
        AnnotateWikiDocs awd = new AnnotateWikiDocs();
        awd.annotate(args[0]);
    }
}
