package edu.illinois.cs.cogcomp.mlner.experiments;

import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.NERDocument;
import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.ParametersForLbjCode;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.mlner.CrossLingualNER;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ctsai12 on 6/29/16.
 */
public class AnnotatePlainDocs {

    public void annotateDir(String dir, String lang, String outpath) throws IOException {
        FreeBaseQuery.loadDB(true);
        CrossLingualNER.setLang(lang, true);
        ExecutorService executor = Executors.newFixedThreadPool(15);
        for(File f: (new File(dir)).listFiles()){
            String line = FileUtils.readFileToString(f);
            TextAnnotation ta = CrossLingualNER.tokenizer.getTextAnnotation(line);
            QueryDocument doc = new QueryDocument(f.getName());
            doc.plain_text = line;
            doc.setTextAnnotation(ta);
            executor.execute(new DocAnnotator(doc, outpath));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void annotateFile(String file, String lang, String outpath) throws FileNotFoundException {
        FreeBaseQuery.loadDB(true);
        CrossLingualNER.setLang(lang, true);
        ExecutorService executor = Executors.newFixedThreadPool(15);
        int cnt = 0;
        for(String line: LineIO.read(file)){
            TextAnnotation ta = CrossLingualNER.tokenizer.getTextAnnotation(line);
            QueryDocument doc = new QueryDocument(String.valueOf(cnt++));
            doc.plain_text = line;
            doc.setTextAnnotation(ta);
            executor.execute(new DocAnnotator(doc, outpath));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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

                    String tag = "O";
                    if (ParametersForLbjCode.currentParameters.featuresToUse.containsKey("PredictionsLevel1")) {
                        avgscore += w.predictionConfLevel2;
                        tag = w.neTypeLevel2;
                    } else {
                        avgscore += w.predictionConfLevel1;
                        tag = w.neTypeLevel1;
                    }

                    if(tag.startsWith("I") && prev_tag.startsWith("O")){
                        tag = "O";
                    }
                    List<String> wikifierf = Arrays.asList(w.wikifierfeats);
                    if(tag.contains("PER")){
                        if(wikifierf.stream().filter(x -> x.contains("person")).count()==0)
                            tag = "O";
                    }
                    if(tag.contains("ORG")){
                        if(wikifierf.stream().filter(x -> x.contains("organization")).count()==0)
                            tag = "O";
                    }
                    if(tag.contains("LOC")){
                        if(wikifierf.stream().filter(x -> x.contains("location")).count()==0)
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
        }
    }

    public static void main(String[] args) {
        AnnotatePlainDocs apd = new AnnotatePlainDocs();
        String file = "/shared/corpora/ner/parallel/tr/tr.txt";
        String dir = "/shared/corpora/corporaWeb/lorelei/turkish/tools/ltf2txt/full_short";
        String outpath = "/shared/corpora/ner/wikifier-features/tr/parallel-norank-filter";
        try {
            apd.annotateFile(file, "tr", outpath);
//            apd.annotateDir(dir, "tr", outpath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
