package edu.illinois.cs.cogcomp.mlner.experiments;

import com.google.gson.Gson;
import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifier;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.WikiDocReader;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ctsai12 on 5/1/16.
 */
public class AnnotateEnWikiDocs {

    public static void annotateEuroparl() throws Exception {
        String lang = "en";
        String target_lang = "es";
        String outpath = "/shared/corpora/ner/europarl/"+target_lang+"/"+lang+"/";
//        String inpath = "/shared/corpora/ner/parallel/es/news-commentary-v8.es-en.en";
        CrossLingualWikifier.setLang(lang);
        AnnotatorService curator = CuratorFactory.buildCuratorClient();
        String inpath = "/shared/corpora/ner/europarl/es/en_text";
        File indir = new File(inpath);
        System.out.println(indir.listFiles().length);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for(File f: indir.listFiles()){
            String text = FileUtils.readFileToString(f);
            QueryDocument doc = new QueryDocument(f.getName());
            doc.plain_text = text;
            executor.execute(new DocAnnotator(doc, outpath, curator));

        }
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }


    public static void annotateWikiDocs() throws Exception {
        String lang = "en";
        String outpath = "/shared/corpora/ner/wikipedia/"+lang+"/";

        CrossLingualWikifier.setLang(lang);
        AnnotatorService curator = CuratorFactory.buildCuratorClient();

        LangLinker ll = new LangLinker();
        String target_lang = "es";

        int start = 100000, end = 1000000;
        WikiDocReader dr = new WikiDocReader();
        List<String> paths = null;
        String dir = "/shared/dickens/ctsai12/multilingual/wikidump/" + lang + "/" + lang + "_wiki_view/";
        paths = LineIO.read(dir+"file.list.rand");
        dr.tokenizer = MultiLingualTokenizer.getTokenizer(lang);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        for(String filename: paths.subList(start, Math.min(paths.size(),end))) {
            if((new File(outpath, filename).isFile())) continue;
            String es_title = ll.translateFromEn(filename, target_lang);
            if(es_title == null) continue;
            QueryDocument doc = dr.readWikiDocSingle(lang, filename, true);
            if(doc == null || doc.mentions.size() < 3){
                continue;
            }
            executor.execute(new DocAnnotator(doc, outpath, curator));
        }
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

    }

    public static class DocAnnotator implements Runnable {


        private QueryDocument doc;
        private String outpath;
        private AnnotatorService curator;

        public DocAnnotator(QueryDocument doc, String outpath, AnnotatorService curator) {
            this.outpath = outpath;
            this.doc = doc;
            this.curator = curator;
        }

        @Override
        public void run() {
            TextAnnotation ta = null;
            try {
                ta = curator.createBasicTextAnnotation("", "", doc.plain_text);
                curator.addView(ta, ViewNames.NER_CONLL);
            } catch (Exception e) {
                e.printStackTrace();
            }

            SpanLabelView view = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);

            doc.plain_text = ta.getText();

            List<ELMention> mentions = new ArrayList<>();
            for(Constituent c: view.getConstituents()){
                ELMention m = new ELMention(doc.getDocID(), c.getStartCharOffset(), c.getEndCharOffset());
                m.setMention(c.getSurfaceForm());
                m.setType(c.getLabel());
                m.setLanguage("en");
                mentions.add(m);
            }
            doc.mentions = mentions;
            CrossLingualWikifier.wikify(doc);

            Gson gson = new Gson();
            String json = gson.toJson(doc);
            try {
                FileUtils.writeStringToFile(new File(outpath, doc.getDocID()), json, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        try {
            AnnotateEnWikiDocs.annotateEuroparl();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
