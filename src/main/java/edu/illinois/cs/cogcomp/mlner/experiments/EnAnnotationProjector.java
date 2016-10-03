package edu.illinois.cs.cogcomp.mlner.experiments;

import com.google.gson.Gson;
import edu.illinois.cs.cogcomp.annotation.AnnotatorService;
import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.curator.CuratorFactory;
import edu.illinois.cs.cogcomp.mlner.core.NERUtils;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifier;
import edu.illinois.cs.cogcomp.xlwikifier.core.StopWord;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.WikiDocReader;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.MediaWikiSearch;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikidataHelper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 5/1/16.
 */
public class EnAnnotationProjector {

    private static Set<String> stopwords;

    public static void projectEuroparl() throws IOException, InterruptedException {
        CrossLingualWikifier.setLang("en");
        String lang = "es";
        stopwords = StopWord.getStopWords("es");
        NERUtils nerutils = new NERUtils();
        nerutils.setLang(lang);
        Gson gson = new Gson();
        LangLinker ll = new LangLinker();
        String en_dir = "/shared/corpora/ner/europarl/es/en";
        String out_dir = "/shared/corpora/ner/europarl/es/es1";
        String in_dir = "/shared/corpora/ner/europarl/es/es_text";

        ExecutorService executor = Executors.newFixedThreadPool(5);
        File en_dirf = new File(en_dir);

        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer(lang);

        for(File enf: en_dirf.listFiles()){
//            if(!enf.getName().equals("0")) continue;
            System.out.println(enf.getName());
            File infile = new File(in_dir, enf.getName());
            if(!infile.isFile()) continue;

            String text = FileUtils.readFileToString(infile);

            QueryDocument doc = new QueryDocument(infile.getName());
            TextAnnotation ta;
            try {
                ta = tokenizer.getTextAnnotation(text);
            }
            catch(Exception e){
                continue;
            }
            doc.setTextAnnotation(ta);
            doc.plain_text = text;
            doc.mentions = nerutils.getNgramMentions(doc, 1);

            String json = FileUtils.readFileToString(enf, "UTF-8");
            QueryDocument en_doc = gson.fromJson(json, QueryDocument.class);
            executor.execute(new DocAnnotator(en_doc, doc, out_dir, lang, ll));

        }
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
    }

    public static void annotate() throws Exception {

        CrossLingualWikifier.setLang("en");
        AnnotatorService curator = CuratorFactory.buildCuratorClient();

        String lang = "es";
        stopwords = StopWord.getStopWords("es");

        LangLinker ll = new LangLinker();
        WikiDocReader dr = new WikiDocReader();
        String dir = "/shared/dickens/ctsai12/multilingual/wikidump/" + lang + "/" + lang + "_wiki_view/";
        ArrayList<String> paths = LineIO.read(dir + "file.list.rand");
        List<String> paths_lower = paths.stream().map(x -> x.toLowerCase()).collect(toList());
        dr.tokenizer = MultiLingualTokenizer.getTokenizer(lang);

        Gson gson = new Gson();
        String outpath = "/shared/corpora/ner/wikipedia/projection/"+lang+"2";
        ExecutorService executor = Executors.newFixedThreadPool(10);

        NERUtils nerutils = new NERUtils();
        nerutils.setLang(lang);

        String en_dir = "/shared/corpora/ner/wikipedia/en/";
        File fdir = new File(en_dir);
        int exist = 0;
        for(File f: fdir.listFiles()){
            String name = f.getName();
//            name = name.replaceAll(" ", "_").toLowerCase();
            String es_title = ll.translateFromEn(name, lang);
            if(es_title==null) continue;
            es_title = es_title.replaceAll("_", " ");
            int idx = paths_lower.indexOf(es_title);
            if(idx < 0) continue;

//            if(!paths.get(idx).equals("Sarah Trimmer")) continue;

            // check if the output file is already there
            if((new File(outpath, paths.get(idx))).isFile()){
                exist++;
                continue;
            }

            QueryDocument doc = dr.readWikiDocSingle(lang, paths.get(idx), true);
            if(doc == null) continue;
            doc.golds = doc.mentions;
            doc.mentions = nerutils.getNgramMentions(doc, 1);

            String json = FileUtils.readFileToString(f, "UTF-8");
            QueryDocument en_doc = gson.fromJson(json, QueryDocument.class);
            executor.execute(new DocAnnotator(en_doc, doc, outpath, lang, ll));

        }
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

    }

    public static class DocAnnotator implements Runnable {


        private QueryDocument en_doc;
        private QueryDocument fo_doc;
        private String outpath;
        private String lang;
        private LangLinker ll;

        public DocAnnotator(QueryDocument en_doc, QueryDocument fo_doc, String outpath, String lang, LangLinker ll) {
            this.outpath = outpath;
            this.en_doc = en_doc;
            this.fo_doc = fo_doc;
            this.lang = lang;
            this.ll = ll;
        }

        @Override
        public void run() {
            WikidataHelper wh = new WikidataHelper();
            MediaWikiSearch mws = new MediaWikiSearch();
            System.out.println("Doc: "+en_doc.getDocID());
            System.out.println("Targer doc: "+fo_doc.getDocID());
            System.out.println("#en mentions "+en_doc.mentions.size());

            Set<String> failed = new HashSet<>();
            List<ELMention> matches = new ArrayList<>();
            List<ELMention> ms = en_doc.mentions.stream().sorted((x1, x2) -> Integer.compare(x2.getMention().length(), x1.getMention().length())).collect(toList());
            for(ELMention m: ms){
                if(failed.contains(m.getMention())) continue;
                for(int j = 0; j <2; j++) {
                    ELMention match = projectMention(m, fo_doc, wh, mws, lang, ll);
                    if (match != null)
                        matches.add(match);
                    else
                        failed.add(m.getMention());
                }
            }
            System.out.println("#matched: "+matches.size());
            fo_doc.mentions = matches;
            if(matches.size()> 0)
                printConll(fo_doc, outpath);
        }
    }

    public static void printConll(QueryDocument doc, String outpath) {
        String out = "";

        TextAnnotation ta = doc.getTextAnnotation();

        for(int i = 0; i < ta.getTokens().length; i++){
            IntPair offsets = ta.getTokenCharacterOffset(i);
            if(i>0 && ta.getSentenceId(i) != ta.getSentenceId(i-1))
                out += "\n";

            String tag = "O";
            for(int j = 0; j < doc.mentions.size(); j++) {
                ELMention mention = doc.mentions.get(j);
                if (offsets.getFirst() == mention.getStartOffset()) {
                    tag = "B-"+mention.getType();
                    break;
                }
                else if (offsets.getFirst() > mention.getStartOffset() && offsets.getSecond() <= mention.getEndOffset()){
                    tag = "I-"+mention.getType();
                    break;
                }
            }

            out += tag+"\tx\tx\tx\tx";
            out += "\t"+ta.getToken(i)+"\tx\tx\tx\tx";
            out += "\n";
        }

        try {
            System.out.println(outpath+"/"+doc.getDocID());
            FileUtils.writeStringToFile(new File(outpath, doc.getDocID()), out, "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ELMention projectMention(ELMention m, QueryDocument doc, WikidataHelper wh, MediaWikiSearch wikisearch, String lang, LangLinker ll){

        List<ELMention> unigram = doc.mentions;

        List<String> possible_strings = new ArrayList<>();
        // the original mention
        possible_strings.add(m.getMention().toLowerCase());

        // get the translated mention

        // get English wiki title, target language title, and redirects...
        String top = m.getWikiTitle();
        if(top!=null && !top.startsWith("NIL")) {
            possible_strings.add(top.replaceAll("_", " ").toLowerCase());

            String es = ll.translateFromEn(top, "es");
            if(es != null){
//            if (es.size() > 0) {
                possible_strings.add(es.replaceAll("_", " ").toLowerCase());
                List<String> redirects = wikisearch.getRedirectsTo(wikisearch.formatTitle(es), lang).stream().map(x -> x.toLowerCase()).collect(Collectors.toList());
                possible_strings.addAll(redirects);
            }
        }

        // de-duplicate
        possible_strings = possible_strings.stream().distinct().collect(toList());

        // remove parenthesis
        for(int i = 0; i < possible_strings.size(); i++){
            String tmp = possible_strings.get(i);
            int idx = tmp.indexOf("(");
            if(idx > 0){
                tmp = tmp.substring(0, idx).trim();
                possible_strings.set(i, tmp);
            }
        }
        possible_strings = possible_strings.stream().distinct().collect(toList());

        System.out.println(m.getMention());
        System.out.println(possible_strings);
        System.out.println(unigram.size());

        int min_score = 1000;
        String min_mention = null;
        int window = 0, edit_th = 2;
        int min_start = -1, min_end = -1, min_len = 0;
        for(String ps: possible_strings) {

            ps = replaceSpecialChars(ps);
            ArrayList<String> mtokens = new ArrayList<>(Arrays.asList(ps.split("\\s+")));
            int n = mtokens.size();
            for(int l = Math.max(1,n-window); l <= n+window; l++) {
                for (int i = 0; i < unigram.size() - l + 1; i++) {
                    List<ELMention> stokens = unigram.subList(i, i + l).stream()
                            .filter(x -> !x.getMention().equals(","))
                            .collect(toList());

                    List<String> surfaces = stokens.stream().map(x -> x.getMention()).collect(toList());
                    String ngram = surfaces.stream().collect(joining(" ")).trim();

                    if (ngram.isEmpty()) continue;
                    if(stopwords.contains(ngram.toLowerCase())) continue;
//                    if(ngram.substring(0,1).toLowerCase().equals(ngram.substring(0,1))) continue;
                    surfaces = surfaces.stream().map(x -> x.toLowerCase()).collect(Collectors.toList());
                    int score = matchTokens(mtokens, surfaces);
                    int start = stokens.get(0).getStartOffset();
                    int end = stokens.get(stokens.size()-1).getEndOffset();
                    if (score < min_score || (score == min_score && (start < min_start || end-start > min_len))) {
                        min_score = score;
                        min_mention = ngram;
                        min_start = start;
                        min_end = end;
                        min_len = end - start;
                    }
                }
            }
        }

        // if the best match is good, return it
        if (min_score < edit_th) {
            if(m.getMention().length() < 4 && min_score>0) return null;
            final int finalMin_end = min_end;
            final int finalMin_start = min_start;
            unigram = unigram.stream().filter(x -> x.getEndOffset() <= finalMin_start || x.getStartOffset() >= finalMin_end).collect(Collectors.toList());
            System.out.println("\tmatched: "+min_mention);
            doc.mentions = unigram;

            ELMention match = new ELMention(doc.getDocID(), min_start, min_end);
            match.setMention(doc.plain_text.substring(min_start, min_end));
            match.setType(m.getType());
            match.setWikiTitle(m.getWikiTitle());
            return match;
        }
        return null;
    }

    public static String replaceSpecialChars(String ps){
        if(ps.contains("'"))
            ps = ps.replaceAll("'", " ' ");
        if(ps.contains("-"))
            ps = ps.replaceAll("-", " - ");
        if(ps.contains("."))
            ps = ps.replaceAll("\\.", " . ");
        ps = ps.replaceAll("\\s+"," ").trim();
        return ps;
    }

    /**
     * Compute edit distance between two list of tokens
     * @param tokens1
     * @param tokens2
     * @return
     */
    public static int matchTokens(List<String> tokens1, List<String> tokens2){
        tokens1 = new ArrayList<>(tokens1);
        int score = 0;
        while(tokens1.size()>0 && tokens2.size()>0) {
            int source_idx = -1;
            int target_idx = -1;
            int min_score = 1000;
            for (int i = 0; i < tokens1.size(); i++) {  // for each source token
                int m = 1000, midx = -1;
                for(int j = 0; j < tokens2.size(); j++){ // find the best target token
                    int  s = LevensteinDistance.getLevensteinDistance(tokens1.get(i), tokens2.get(j));
                    if(s < m){
                        m = s;
                        midx = j;
                    }
                }
                if(m < min_score){ // record the best pair of tokens
                    min_score = m;
                    source_idx = i;
                    target_idx = midx;
                }
            }
            score += min_score;
            if(source_idx == -1 || target_idx == -1) return 1000;
            tokens1.remove(source_idx);
            tokens2.remove(target_idx);
        }
        int t1_remain = tokens1.stream().collect(joining("")).length();
        int t2_remain = tokens2.stream().collect(joining("")).length();
        return score+t1_remain+t2_remain;
    }

    public static void main(String[] args) {
        try {
//            EnAnnotationProjector.projectEuroparl();
            EnAnnotationProjector.annotate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
