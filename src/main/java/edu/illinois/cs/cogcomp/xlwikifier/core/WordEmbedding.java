package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import edu.illinois.cs.cogcomp.xlwikifier.Constants;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.WikiDocReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;

import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 10/10/15.
 */
public class WordEmbedding {

    private static Logger logger = LoggerFactory.getLogger(WordEmbedding.class);

    public ConcurrentNavigableMap<String, Double[]> mono_vec;
    public Map<String, ConcurrentNavigableMap<String, Double[]>> mono_vecs;
    public ConcurrentNavigableMap<String, Double[]> multi_vec_en;
    public ConcurrentNavigableMap<String, Double[]> multi_vec_lang;
    public ConcurrentNavigableMap<String, Double[]> doc_mention_vec;
    public ConcurrentNavigableMap<String, Double[]> doc_title_vec;

    public Map<String, ConcurrentNavigableMap<String, Double[]>> multi_vecs = new HashMap<>();

    public Map<String, ConcurrentNavigableMap<String, Double[]>> multi_vecs_cache = new HashMap<>();

    public Map<String, Set<String>> stopwords = new HashMap<>();
    private WikiDocReader dr;

    public DB sgtitle_db;
    public DB mono_db;
    private DB multi_db;
    private DB multi_cache_db;
    private DB doc_db;
    public int dim;
    private String lang;
    private String dr_lang;
    private Map<String, Double[]> vec_cache = new HashMap<>();
    private Map<String, Double[]> title_vec_cache = new HashMap<>();
    private Map<String, Double[]> doc_mention_cache = new HashMap<>();
    private Map<String, Double[]> doc_title_cache = new HashMap<>();
    private boolean use_mcache = true;

    public WordEmbedding() {
        loadStopWords("en");

    }
    public void loadStopWords(String lang){
        stopwords.put(lang, StopWord.getStopWords(lang));
    }

    public void closeDB() {
        if (multi_cache_db != null && !multi_cache_db.isClosed()) {
            multi_cache_db.commit();
            multi_cache_db.close();
        }
        if (multi_db != null && !multi_db.isClosed()) {
            multi_db.commit();
            multi_db.close();
        }
        if (mono_db != null && !mono_db.isClosed()) {
            mono_db.commit();
            mono_db.close();
        }
        if (doc_db != null && !doc_db.isClosed()) {
            doc_db.commit();
            doc_db.close();
        }
    }


    public void loadMonoDBNew(String lang) {
        logger.info("Loading mono vectores "+lang);
//        if(mono_db != null && !mono_db.isClosed())
//            mono_db.close();
        mono_db = DBMaker.newFileDB(new File(Constants.dbpath1+"/mono-embeddings", lang))
                .cacheSize(100000)
                .transactionDisable()
                .closeOnJvmShutdown()
                .make();
        mono_vec = mono_db.createTreeMap(lang)
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        loadStopWords(lang);

        multi_vecs.put(lang, mono_vec);  // because it gets vector from multi_vecs always
        if(multi_vecs.get(lang).containsKey("obama"))
            dim = multi_vecs.get(lang).get("obama").length;
        else
            logger.info("no vec for obama");
    }

    /**
     * This is for mono embed experiments of NAACL paper
     * @param lang
     */
    public void setMonoVecsNew(String lang) {
        if(!lang.equals("en")) {
            loadMonoDBNew(lang);
            multi_vecs.put(lang, mono_vec);
        }
        loadMonoDBNew("en");
        multi_vecs.put("en", mono_vec);
        dim = multi_vecs.get("en").get("obama").length;
    }

    public void createMultiVec(String lang) {
        File dir = new File(Constants.dbpath1+"multi-embeddings/"+lang);
        dir.mkdir();
        loadMultiDBNew(lang);
        if(multi_vec_lang.size()!=0 || multi_vec_en.size()!=0){
            System.out.println("map is not empty");
            System.exit(-1);
        }
    }

    public void loadMultiWordDB(String lang) {
        logger.info("Loading "+lang+" multi word vectors...");
        File f = new File(Constants.dbpath, "multi-embeddings/"+lang+".word");
        if(!f.exists()){
            System.out.println("DB doesn't exist "+f.toString());
            System.exit(-1);
        }

        multi_db = DBMaker.newFileDB(f)
                .cacheSize(100000)
                .transactionDisable()
                .closeOnJvmShutdown()
                .make();
        multi_vec_en = multi_db.createTreeMap("en")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        multi_vec_lang = multi_db.createTreeMap("lang")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        multi_vecs.put("en", multi_vec_en);
        multi_vecs.put(lang, multi_vec_lang);
        if(multi_vec_en.containsKey("obama"))
            dim = multi_vec_en.get("obama").length;
        this.lang = lang;
        loadStopWords(lang);
    }

    public void loadMultiDBNew(String lang) {
        logger.info("Loading "+lang+" multi vectors...");
        File f = new File(Constants.dbpath1, "multi-embeddings/"+lang+"/"+lang);
        multi_db = DBMaker.newFileDB(f)
                .cacheSize(100000)
                .transactionDisable()
                .closeOnJvmShutdown()
                .make();
        multi_vec_en = multi_db.createTreeMap("en")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        multi_vec_lang = multi_db.createTreeMap("lang")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        multi_vecs.put("en", multi_vec_en);
        multi_vecs.put(lang, multi_vec_lang);
        if(multi_vec_en.containsKey("obama"))
            dim = multi_vec_en.get("obama").length;
        this.lang = lang;
        loadStopWords(lang);
    }

    public void loadDocDB(String lang) {
        doc_db = DBMaker.newFileDB(new File(Constants.dbpath, "doc-embeddings/"+lang))
                .cacheSize(100000)
                .transactionDisable()
                .closeOnJvmShutdown()
                .make();
        doc_mention_vec = doc_db.createTreeMap("mention")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        doc_title_vec = doc_db.createTreeMap("title")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
    }

//    public void loadMultiDB() {
//        sgtitle_db = DBMaker.newFileDB(new File(dbpath, "SGTITLE"))
////        sgtitle_db = DBMaker.newFileDB(new File(dbpath, "embeddings"))
//                .cacheSize(100000)
//                .transactionDisable()
//                .closeOnJvmShutdown()
//                .make();
//        Map<String, String> tmp = new HashMap<>();
////        tmp.put("en", "en_cca");
////        tmp.put("es", "es_cca");
//        tmp.put("en", "en_enes_title_only");
//        tmp.put("es", "es_enes_title_only");
////        tmp.put("en", "en_enes_new");
////        tmp.put("es", "es_enes_new");
//        lang2dbname.put("es", tmp);
//
//        tmp = new HashMap<>();
//        tmp.put("en", "en_entr_cca");
//        tmp.put("tr", "tr_entr_cca");
//        lang2dbname.put("tr", tmp);
//
//        tmp = new HashMap<>();
////        tmp.put("en", "en_enfr_cca");
////        tmp.put("fr", "fr_enfr_cca");
//        tmp.put("en", "en_enfr_title_only");
//        tmp.put("fr", "fr_enfr_title_only");
//        lang2dbname.put("fr", tmp);
//
//        tmp = new HashMap<>();
//        tmp.put("en", "en_enha1_cca");
//        tmp.put("ha", "ha_enha1_cca");
//        lang2dbname.put("ha", tmp);
//    }

//    public Map<String, ConcurrentNavigableMap<String, Double[]>> getMultiVecsNew(String lang) {
//        if (multi_db == null || !this.lang.equals(lang)) loadMultiDBNew(lang);
//        Map<String, ConcurrentNavigableMap<String, Double[]>> ret = null;
////        ret.put("en", multi_vec_en);
////        ret.put(lang, multi_vec_lang);
////        this.dim = multi_vec_en.get("obama").length;
////        this.multi_vecs = ret;
//        return ret;
//    }


    public Double[] getWikiDocMentionVec(String title, String lang){
        if(title == null) return zeroVec();
        String cache_key = title+"_"+lang;
        if(doc_mention_cache.containsKey(cache_key))
            return doc_mention_cache.get(cache_key);

        if(doc_db == null) loadDocDB(lang);

        title = title.toLowerCase().replaceAll(" ","_");

        if(!doc_mention_vec.containsKey(title))
            populateWikiDocRep(title, lang);

        Double[] ret = null;
        if(doc_mention_vec.containsKey(title))
            ret = doc_mention_vec.get(title);
        doc_mention_cache.put(cache_key, ret);
        return ret;
    }

    public Double[] getWikiDocTitleVec(String title, String lang){
        if(title == null) return zeroVec();
        String cache_key = title+"_"+lang;
        if(doc_title_cache.containsKey(cache_key))
            return doc_title_cache.get(cache_key);

        if(doc_db == null) loadDocDB(lang);

        title = title.toLowerCase().replaceAll(" ","_");

        if(!doc_title_vec.containsKey(title))
            populateWikiDocRep(title, lang);

        Double[] ret = null;
        if(doc_title_vec.containsKey(title))
            ret = doc_title_vec.get(title);
        doc_title_cache.put(cache_key, ret);
        return ret;
    }

    public void populateWikiDocRep(String title, String lang){
//        System.out.println("Calculating title representation: "+title+" "+lang);
        if(dr == null || !dr_lang.equals(lang)) {
            dr = new WikiDocReader(lang);
            dr_lang = lang;
        }

        String path;
        if(dr.title_map.containsKey(title))
            path = dr.title_map.get(title);
        else {
//            System.out.println("no doc: "+title);
            return;
        }

        QueryDocument doc = dr.readWikiDocSingle(lang, path, false);
        List<Double[]> vecs = doc.mentions.stream().map(x -> getVectorFromWords(x.getMention().split("\\s+"), lang)).collect(toList());
        Double[] mention_avg = averageVectors(vecs);
        vecs = doc.mentions.stream().map(x -> getTitleVector(x.gold_wiki_title, lang))
                .filter(x -> x!=null).collect(toList());
        Double[] title_avg = averageVectors(vecs);
        doc_mention_vec.put(title, mention_avg);
        doc_title_vec.put(title, title_avg);
    }

    public Double[] getVectorFromWords(List<String> words, String lang){
        List<Double[]> vecs = new ArrayList<>();
        for(String word: words){
            Double[] vec = getWordVector(word, lang);
            if(vec != null)
                vecs.add(vec);
        }
        return averageVectors(vecs);
    }

    public Double[] getVectorFromWords(String[] words, String lang){
        return getVectorFromWords(Arrays.asList(words), lang);
    }

    public Double[] zeroVec(){
        Double[] ret = new Double[dim];
        Arrays.fill(ret, 0.0);
        return ret;
    }

    public Double[] averageVectors(List<Double[]> vecs){
        Double[] ret = new Double[dim];
        Arrays.fill(ret, 0.0);

        int n = 0;
        for(Double[] v: vecs){
            if(v == null) continue;
            for(int i = 0; i < v.length; i++)
                ret[i] += v[i];
            n++;
        }
        if(vecs.size()>0)
            for(int i = 0; i < dim; i++)
                ret[i] /= n;
        return ret;
    }

    public Double[] averageVectors(List<Double[]> vecs, List<Double> weights){
        Double[] ret = new Double[dim];
        Arrays.fill(ret, 0.0);

        double sum = 0;
        for(int i = 0; i < vecs.size(); i++){
            double w = weights.get(i);
            sum += w;
            for(int j = 0; j < vecs.get(i).length; j++)
                ret[j] += w*vecs.get(i)[j];
        }

        if(vecs.size()>0 && sum > 0)
            for(int i = 0; i < dim; i++)
                ret[i] /= sum;
        return ret;
    }
    public void loadEmbeddingToDB(String file, ConcurrentNavigableMap<String, Double[]> map) {

        System.out.println("Loading word embeddings from " + file);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            int j = 0;
            while (line != null) {
                if (j++ % 10000 == 0) System.out.print(j + "\r");
                String[] tokens = line.trim().split("\\s+");
                Double[] vec = new Double[tokens.length - 1];
                dim = tokens.length - 1;
                for (int i = 1; i < tokens.length; i++)
                    vec[i - 1] = Double.parseDouble(tokens[i]);
//                System.out.println(tokens[0]);
//                System.out.println(vec[dim-1]);
                map.put(tokens[0], vec);
                line = br.readLine();
            }
            System.out.println();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public Double[] getVector(String query, String lang){
        if(use_mcache && vec_cache.containsKey(query))
            return vec_cache.get(query);
        if((stopwords.containsKey(lang) && stopwords.get(lang).contains(query))
                ||(!multi_vecs.get(lang).containsKey(query))) {
            if(use_mcache) vec_cache.put(query, null);
            return null;
        }

        Double[] vec = multi_vecs.get(lang).get(query);
        if(use_mcache) vec_cache.put(query, vec);
        return vec;
    }

    public Double[] getWordVector(String word, String lang){
        if(!multi_vecs.containsKey(lang)){
            System.err.println("Couldn't find word embeddings for "+lang);
            System.exit(-1);
        }
        word = word.toLowerCase();
        Double[] vec = getVector(word, lang);
        return vec;
    }

    public Double[] getTitleVector(String title, String lang){
        if(title == null || title.startsWith("NIL")) {
            return null;
        }

        title = "title_"+title.replaceAll(" ", "_").toLowerCase();
        Double[] vec = getVector(title, lang);
//        if(vec == null) return zeroVec();
        return vec;
    }

    public double cosine(Double[] v1, Double[] v2) {
        if (v1.length != v2.length) {
            System.out.println("Array size don't match");
            System.exit(-1);
        }

        double n1 = 0, n2 = 0, in = 0;
        for (int i = 0; i < v1.length; i++) {
            in += v1[i] * v2[i];
            n1 += v1[i] * v1[i];
            n2 += v2[i] * v2[i];
        }

        if (n1 == 0 || n2 == 0)
            return 0;

        return in / (Math.sqrt(n1) * Math.sqrt(n2));
    }

    public static void main(String[] args) {
        WordEmbedding we = new WordEmbedding();
//        we.loadCCADB();
//        we.loadSGDB();
//        we.loadEmbeddingToDB("/shared/bronte/ctsai12/multilingual/sg/vectors/vectors.en.notitle", we.sg_en_notitle);
//        we.loadEmbeddingToDB("/shared/bronte/ctsai12/multilingual/sg/vectors/vectors.es.notitle", we.sg_es_notitle);                      Boom Boom Pow
//        we.loadEmbeddingToDB("/shared/experiments/ctsai12/workspace/eacl14-cca/sg.notitle.ratio0.5_orig1_projected.txt", we.sg_en_notitle_cca);
//        we.loadEmbeddingToDB("/shared/eGxperiments/ctsai12/workspace/eacl14-cca/sg.notitle.ratio0.5_orig2_projected.txt", we.sg_es_notitle_cca);
//        we.loadEmbeddingToDB("/shared/experiments/ctsai12/workspace/eacl14-cca/sg.withtitle.ratio0.5_orig1_projected.txt", we.sg_en_wordalign_cca);
//        we.loadEmbeddingToDB("/shared/experiments/ctsai12/workspace/eacl14-cca/sg.withtitle.ratio0.5_orig2_projected.txt", we.sg_es_wordalign_cca);
//        we.loadEmbeddingToDB("/shared/experiments/ctsai12/workspace/eacl14-cca/sg.titlealign.enha1.r0.5_orig1_projected.txt", we.sg_en_enha1_cca);
//        we.loadEmbeddingToDB("/shared/experiments/ctsai12/workspace/eacl14-cca/sg.titlealign.enha1.r0.5_orig2_projected.txt", we.sg_ha_enha1_cca);
//        we.loadEmbeddingToDB("/shared/experiments/ctsai12/workspace/eacl14-cca/sg.titlealign.enfr.r0.5_orig1_projected.txt", we.sg_en_enfr_cca);
//        we.loadEmbeddingToDB("/shared/experiments/ctsai12/workspace/eacl14-cca/sg.titlealign.enfr.r0.5_orig2_projected.txt", we.sg_fr_enfr_cca);

//        we.loadMonoVec("");
//        we.loadMonoDBNew("ug-ner");
//        we.loadEmbeddingToDB("/shared/preprocessed/ctsai12/multilingual/vectors/vectors.mono.ner1", we.mono_vec);
//        String[] langs = {"es", "de", "fr", "it", "zh", "tr", "he", "ar", "ta", "th", "tl", "ur"};
//        for(String lang: langs)
//        we.setMonoVecsNew(lang);

//        String dir = "/shared/shelley/ctsai12/eacl14-cca/";
        String dir = "/shared/experiments/ctsai12/workspace/eacl14-cca/";
        dir = "/home/mayhew2/software/crosslingual-cca/";
        dir = "/shared/preprocessed/ctsai12/multilingual/cca/";
		String name = args[0];
//        String name = "ug";
        we.createMultiVec(name);
        we.loadEmbeddingToDB(dir + "/"+name+"/en"+name+"_orig1_projected.txt", we.multi_vec_en);
        we.loadEmbeddingToDB(dir + "/"+name+"/en"+name+"_orig2_projected.txt", we.multi_vec_lang);
//        we.loadEmbeddingToDB(dir + "myresult2_uz_projected.txt", we.multi_vec_en);
//        we.loadEmbeddingToDB(dir + "myresult2_ug_projected.txt", we.multi_vec_lang);
//        we.loadMultiDBNew("ug");
//        we.loadMultiDBNew("uz");
//        System.out.println(Arrays.asList(we.getWordVector("sheheri", "ug")));

        we.closeDB();
    }
}
