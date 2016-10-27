package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.DumpReader;
import org.mapdb.*;
import org.mapdb.serializer.SerializerArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 12/1/15.
 */
public class WikiCandidateGenerator {

    private DB db;
    private Map<String, DB> db_pool = new HashMap<>();

    public BTreeMap<Object[], Float> p2t2prob; // phrase to title to probability
    public BTreeMap<Object[], Float> t2p2prob;
    public BTreeMap<Object[], Float> w2t2prob;
    public BTreeMap<Object[], Float> t2w2prob;

    private String lang;
    private Map<String, String> title2id, id2redirect;
    private Map<String, List<WikiCand>> cand_cache = new HashMap<>();
    private boolean use_cache = false;
    private int top = 10;
    public boolean en_search = false;
    public boolean word_search = false;
    private Tokenizer tokenizer;
    private WikiCandidateGenerator en_generator;
    private static Logger logger = LoggerFactory.getLogger(WikiCandidateGenerator.class);

    public WikiCandidateGenerator(String lang, boolean read_only) {
        loadDB(lang, read_only);
        tokenizer = MultiLingualTokenizer.getTokenizer(lang);
        if (!lang.equals("en"))
            en_generator = new WikiCandidateGenerator("en", true);
    }

    public void setId2Redirect(Map<String, String> map) {
        this.id2redirect = map;
    }

    public void setTitle2Id(Map<String, String> map) {
        this.title2id = map;
    }

    public void loadDB(String lang, boolean read_only) {
        this.lang = lang;
        if (db_pool.containsKey(lang)) db = db_pool.get(lang);
        else {
            String dbfile = ConfigParameters.db_path + "/candidates/" + lang;
//            String dbfile = "/shared/preprocessed/ctsai12/multilingual/mapdb/candidates/"+lang+"_candidates";
            if (read_only) {
                db = DBMaker.fileDB(new File(dbfile))
                        .closeOnJvmShutdown()
                        .readOnly()
                        .make();
                p2t2prob = db.treeMap("p2t", new SerializerArray(Serializer.STRING), Serializer.FLOAT)
                        .open();
                t2p2prob = db.treeMap("t2p", new SerializerArray(Serializer.STRING), Serializer.FLOAT)
                        .open();
                w2t2prob = db.treeMap("w2t", new SerializerArray(Serializer.STRING), Serializer.FLOAT)
                        .open();
                t2w2prob = db.treeMap("t2w", new SerializerArray(Serializer.STRING), Serializer.FLOAT)
                        .open();
            } else {
                db = DBMaker.fileDB(new File(dbfile))
                        .closeOnJvmShutdown()
                        .make();
                p2t2prob = db.treeMap("p2t", new SerializerArray(Serializer.STRING), Serializer.FLOAT)
                        .create();
                t2p2prob = db.treeMap("t2p", new SerializerArray(Serializer.STRING), Serializer.FLOAT)
                        .create();
                w2t2prob = db.treeMap("w2t", new SerializerArray(Serializer.STRING), Serializer.FLOAT)
                        .create();
                t2w2prob = db.treeMap("t2w", new SerializerArray(Serializer.STRING), Serializer.FLOAT)
                        .create();
            }
            db_pool.put(lang, db);
        }


    }

    public void closeDB() {
        if (db != null && !db.isClosed()) {
            db.commit();
            db.close();
        }
        this.lang = null;
    }


    public String getFinalTitle(String title) {
        title = title.toLowerCase().replaceAll(" ", "_");

        if (title2id.containsKey(title)) {
            String id = title2id.get(title);
            if (id2redirect.containsKey(id)) {
                return id2redirect.get(id);
            }
        }
        return title;
    }


    public void genCandidates(List<QueryDocument> docs) {
        logger.info("Generating candidates...");
        for (QueryDocument doc : docs) {
            genCandidates(doc);
        }
    }

    public void genCandidates(QueryDocument doc) {

        for (ELMention m : doc.mentions) {

            if (m.getCandidates().size() == 0) {
                List<WikiCand> cands = genCandidate(m.getSurface());

                cands = cands.subList(0, Math.min(top, cands.size()));
                m.getCandidates().addAll(cands);
            }
        }
    }

    public List<WikiCand> genCandidate(String surface) {
        surface = surface.toLowerCase().trim();

        List<WikiCand> cands = getCandsBySurface(surface);
        if (cands.size() == 0) {
            if (word_search)
                cands = getCandidateByWord(surface, 6);
            if (en_search)
                cands = en_generator.getCandsBySurface(surface);
        }

        return cands;
    }


    /**
     * surface must be lowercased
     *
     * @param surface
     * @return
     */
    public List<WikiCand> getCandsBySurface(String surface) {

        List<WikiCand> cands = new ArrayList<>();
        NavigableMap<Object[], Float> ctitles = p2t2prob.subMap(new Object[]{surface}, new Object[]{surface, null});

        List<Map.Entry<Object[], Float>> sorted_cands = ctitles.entrySet().stream().sorted((x1, x2) -> Float.compare(x2.getValue(), x1.getValue()))
                .collect(Collectors.toList()).subList(0, Math.min(ctitles.size(), top));

        for (Map.Entry<Object[], Float> c : sorted_cands) {
            String title = (String) c.getKey()[1];
            int dist = getEditDistance(title, surface);
            double s = 1;
            if (dist != 0) s = 1.0 / dist;
            WikiCand cand = new WikiCand(title, s);
            cand.psgivent = c.getValue();
            cand.ptgivens = t2p2prob.get(new Object[]{title, surface});
            cand.lang = lang;
            cand.src = "surface";
            cand.query_surface = surface;
            cands.add(cand);

        }
        return cands;
    }

    private int getEditDistance(String title, String surface) {
        title = title.replaceAll("_", " ");
        int idx = title.indexOf("(");
        if (idx > 0)
            title = title.substring(0, idx).trim();
        int st = surface.split("\\s+").length;
        String[] tt = surface.split("\\s+");
        if (tt.length == 3 && st == 2 && tt[1].endsWith("."))
            title = tt[0] + " " + tt[2];
        int dist = LevensteinDistance.getLevensteinDistance(surface, title);
        return dist;
    }


    public List<WikiCand> getCandidateByWord(String surface, int max_cand) {
        List<WikiCand> cands = new ArrayList<>();
        String[] tokens = null;
        if (lang.equals("zh"))
            tokens = surface.split("·");
        else
            tokens = tokenizer.getTextAnnotation(surface).getTokens();
//            tokens = surface.split("\\s+");
        int each_word_top = max_cand / tokens.length;
        for (String t : tokens) {

            NavigableMap<Object[], Float> ctitles = w2t2prob.subMap(new Object[]{t}, new Object[]{t, null});

            List<Map.Entry<Object[], Float>> sorted_cands = ctitles.entrySet().stream().sorted((x1, x2) -> Float.compare(x2.getValue(), x1.getValue()))
                    .collect(Collectors.toList()).subList(0, Math.min(ctitles.size(), each_word_top));

            for (Map.Entry<Object[], Float> c : sorted_cands) {
                String title = (String) c.getKey()[1];
                int dist = getEditDistance(title, surface);
                double s = 1;
                if (dist != 0) s = 1.0 / dist;
                WikiCand cand = new WikiCand(title, s);
                cand.psgivent = c.getValue();
                cand.ptgivens = t2w2prob.get(new Object[]{title, t});
                cand.lang = lang;
                cand.src = "word";
                cand.query_surface = surface;
                cands.add(cand);

            }
        }
//        word_cands = word_cands.stream().sorted((x1, x2) -> Double.compare(x2.getScore(), x1.getScore())).collect(toList());
        cands = cands.stream().sorted((x1, x2) -> Double.compare(x2.ptgivens, x1.ptgivens)).collect(toList());
        return cands.subList(0, Math.min(cands.size(), max_cand));
    }

//    private List<WikiCand> getCandidatesByNgram(String surface){
//
//        int top = 20000000;
//
//        List<String> cand_titles = new ArrayList<>();
//        for(int i = 0; i < surface.length()-4; i++){
//            String ngram = surface.substring(i, i+4);
//            if(fourgramidx.containsKey(ngram)){
//                String[] titles = fourgramidx.get(ngram);
//                cand_titles.addAll(Arrays.asList(titles));
//            }
//        }
//
//        Map<String, Long> t2cnt = cand_titles.stream().collect(groupingBy(x -> x, counting()));
//        List<Map.Entry<String, Long>> sortedt = t2cnt.entrySet().stream().sorted((x1, x2) -> Long.compare(x2.getValue(), x1.getValue()))
//                .collect(toList());
//
//        List<WikiCand> ret = new ArrayList<>();
//        for(int i = 0; i < sortedt.size() && i < top; i++){
//            String title = sortedt.get(i).getKey();
//            Long cnt = sortedt.get(i).getValue();
//
//            int dist = getEditDistance(title, surface);
//            double s = 1;
//            if(dist != 0) s = 1.0/dist;
//            WikiCand c = new WikiCand(title, s);
//            c.lang = lang;
//            c.src = "ngram";
//            c.query_surface = surface;
//            ret.add(c);
//        }
//        return ret;
//    }

    private void populateWord2Title(String file, String lang) {
        logger.info("Populating " + lang + " candidate database from " + file);
        if (db == null)
            loadDB(lang, false);

        w2t2prob.clear();
        t2w2prob.clear();

        Map<String, List<String>> s2t = new HashMap<>();
        Map<String, List<String>> t2s = new HashMap<>();

        try {
            for (String line : LineIO.read(file)) {

                String[] sp = line.split("\t");
                if (sp.length < 2) continue;
                String s = sp[0].toLowerCase().trim();
                String t = getFinalTitle(sp[1]);

                String[] tokens;
                if (lang.equals("zh"))
                    tokens = s.split("·");
                else
                    tokens = tokenizer.getTextAnnotation(s).getTokens();

                for (String ss : tokens) {
                    if (!s2t.containsKey(ss))
                        s2t.put(ss, new ArrayList<>());
                    s2t.get(ss).add(t);
                    if (!t2s.containsKey(t))
                        t2s.put(t, new ArrayList<>());
                    t2s.get(t).add(ss);
                }
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("s2t size " + s2t.size());
        logger.info("t2s size " + t2s.size());

        for (String title : t2s.keySet()) {
            String[] title_tokens = null;
            if (lang.equals("zh"))
                title_tokens = title.toLowerCase().split("·");
            else
                title_tokens = title.toLowerCase().split("_");

            for (String token : title_tokens) {
                if (!s2t.containsKey(token))
                    s2t.put(token, new ArrayList<>());
                s2t.get(token).add(title);
                t2s.get(title).add(token);
            }
        }

        logger.info("Calculating p(title | word)...");
        int cnt = 0;
        for (String surface : s2t.keySet()) {
            cnt++;
            if (cnt % 10000 == 0)
                System.out.print(cnt * 100.0 / s2t.size() + "\r");
            Map<String, Long> t2cnt = s2t.get(surface).stream().collect(groupingBy(x -> x, counting()));
            float sum = 0;
            for (String title : t2cnt.keySet()) {
                sum += t2cnt.get(title);
            }
            for (String title : t2cnt.keySet()) {
                w2t2prob.put(new Object[]{surface, title}, t2cnt.get(title) / sum);
            }
        }

        logger.info("Calculating p(word | title)...");
        for (String title : t2s.keySet()) {
            Map<String, Long> s2cnt = t2s.get(title).stream().collect(groupingBy(x -> x, counting()));
            float sum = 0;
            for (String surface : s2cnt.keySet()) {
                sum += s2cnt.get(surface);
            }
            for (String surface : s2cnt.keySet()) {
                t2w2prob.put(new Object[]{title, surface}, s2cnt.get(surface) / sum);
            }
        }

    }

    /**
     * Import the candidate generation DB
     *
     * @param lang
     * @param redirect_file
     * @param page_file
     * @param cand_file
     */
    public void populateDB(String lang, String redirect_file, String page_file, String cand_file) {
        tokenizer = MultiLingualTokenizer.getTokenizer(lang);
        DumpReader dr = new DumpReader();
        dr.readRedirects(redirect_file);
        dr.readTitle2ID(page_file);
        this.setId2Redirect(dr.id2redirect);
        this.setTitle2Id(dr.title2id);
        populateMentionDB(cand_file, lang);
        populateWord2Title(cand_file, lang);
        db.commit();
        db.close();
//        loadDB(lang, true);
    }

//    public void populate4GramIdx(String lang, String redirect_file, String page_file){
//        loadDB(lang, false);
//        DumpReader dr = new DumpReader();
//        dr.readRedirects(redirect_file);
//        dr.readTitle2ID(page_file);
//        this.setId2Redirect(dr.id2redirect);
//        this.setTitle2Id(dr.title2id);
//
//        Map<String, Map<String, Integer>> gram2titlecnt = new HashMap<>();
//        System.out.println("Counting 4 grams...");
//        int cnt = 0;
//        for(String title: title2id.keySet()){
//            if(cnt++%100000 == 0) System.out.print(cnt+"\r");
//            String ftitle = getFinalTitle(title);
//            String[] tokens = title.split("_");
//            for(String token: tokens){
//                token = token.toLowerCase();
//                if(token.trim().isEmpty()) continue;
//                for(int i = 0; i < token.length()-3; i++){
//                    String gram = token.substring(i, i + 3);
//                    if(!gram2titlecnt.containsKey(gram))
//                        gram2titlecnt.put(gram, new HashMap<>());
//                    Map<String, Integer> titlecnt = gram2titlecnt.get(gram);
//                    if(!titlecnt.containsKey(ftitle))
//                        titlecnt.put(ftitle, 1);
//                    else
//                        titlecnt.put(ftitle, titlecnt.get(ftitle)+1);
//                }
//            }
//        }
//        System.out.println("#4grams "+gram2titlecnt.size());
//
//        System.out.println("Sorting and saving...");
//        for(String gram: gram2titlecnt.keySet()){
//            List<String> sorted = gram2titlecnt.get(gram).entrySet().stream()
//                    .sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue()))
//                    .map(x -> x.getKey()).collect(toList());
//
//            String[] titles = new String[sorted.size()];
//            titles = sorted.toArray(titles);
//            trigramidx.put(gram, titles);
//        }
//    }


    private void populateMentionDB(String file, String lang) {
        logger.info("Populating " + lang + " candidate database from " + file);
        if (db == null)
            loadDB(lang, false);

        p2t2prob.clear();
        t2p2prob.clear();

        Map<String, List<String>> s2t = new HashMap<>();
        Map<String, List<String>> t2s = new HashMap<>();

        try {
            for (String line : LineIO.read(file)) {

                String[] sp = line.split("\t");

                if (sp.length < 2) continue;

                String s = sp[0].toLowerCase().trim();
                String t = getFinalTitle(sp[1]);
                if (!s2t.containsKey(s))
                    s2t.put(s, new ArrayList<>());
                s2t.get(s).add(t);
                if (!t2s.containsKey(t))
                    t2s.put(t, new ArrayList<>());
                t2s.get(t).add(s);
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (String title : t2s.keySet()) {
            String title_surface = title.replaceAll("_", " ").toLowerCase().trim();
            if (!s2t.containsKey(title_surface))
                s2t.put(title_surface, new ArrayList<>());
            s2t.get(title_surface).add(title);
            t2s.get(title).add(title_surface);
        }

        logger.info("Calculating p(title | phrase)...");
        int cnt = 0;
        for (String surface : s2t.keySet()) {
            cnt++;
            if (cnt % 10000 == 0)
                System.out.print(cnt * 100.0 / s2t.size() + "\r");
            Map<String, Long> t2cnt = s2t.get(surface).stream().collect(groupingBy(x -> x, counting()));
            float sum = 0;
            for (String title : t2cnt.keySet()) {
                sum += t2cnt.get(title);
            }
            for (String title : t2cnt.keySet()) {
                p2t2prob.put(new Object[]{surface, title}, t2cnt.get(title) / sum);
            }
        }

        logger.info("Calculating p(phrase | title)...");
        for (String title : t2s.keySet()) {
            Map<String, Long> s2cnt = t2s.get(title).stream().collect(groupingBy(x -> x, counting()));
            float sum = 0;
            for (String surface : s2cnt.keySet()) {
                sum += s2cnt.get(surface);
            }
            for (String surface : s2cnt.keySet()) {
                t2p2prob.put(new Object[]{title, surface}, s2cnt.get(surface) / sum);
            }
        }
    }

    public void selectMentions(List<QueryDocument> docs, double p) {
        System.out.println("#mentions before selection: " + docs.stream().flatMap(x -> x.mentions.stream()).count());
        List<ELMention> easy_all = new ArrayList<>();
        List<ELMention> hard_all = new ArrayList<>();
        for (QueryDocument doc : docs) {
            List<ELMention> hard = doc.mentions.stream().filter(x -> x.getCandidates().size() == 0
                    || !x.getCandidates().get(0).getTitle().toLowerCase().equals(x.gold_wiki_title.toLowerCase()))
                    .collect(toList());
            hard.forEach(x -> x.eazy = false);
            hard_all.addAll(hard);
            List<ELMention> easy = doc.mentions.stream().filter(x -> x.getCandidates().size() > 0
                    && x.getCandidates().get(0).getTitle().toLowerCase().equals(x.gold_wiki_title.toLowerCase()))
                    .collect(toList());
            easy.forEach(x -> x.eazy = true);
            easy_all.addAll(easy);

        }
        System.out.println("#hard " + hard_all.size() + " #easy " + easy_all.size());
        Collections.shuffle(easy_all, new Random(0));
        hard_all.addAll(easy_all.subList(0, (int) Math.min(easy_all.size(), hard_all.size() * p)));
        for (QueryDocument doc : docs) {
            doc.mentions = hard_all.stream().filter(x -> x.getDocID().equals(doc.getDocID()))
                    .sorted((x1, x2) -> Integer.compare(x1.getStartOffset(), x2.getStartOffset()))
                    .collect(toList());
        }
        logger.info("#mentions after selection: " + docs.stream().flatMap(x -> x.mentions.stream()).count());
    }

    public static void main(String[] args) {
        ConfigParameters.setPropValues();
        WikiCandidateGenerator g = new WikiCandidateGenerator(args[0], true);
//        System.out.println(g.getCandsBySurface("Michael Pettis", "en"));
        System.exit(-1);

        g.closeDB();
    }


}
