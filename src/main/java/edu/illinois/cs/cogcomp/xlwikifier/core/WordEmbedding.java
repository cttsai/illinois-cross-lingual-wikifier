package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by ctsai12 on 10/10/15.
 */
public class WordEmbedding {

    private static Logger logger = LoggerFactory.getLogger(WordEmbedding.class);

    public HTreeMap<String, float[]> multi_vec_en;
    public HTreeMap<String, float[]> multi_vec_lang;

    public Map<String, HTreeMap<String, float[]>> multi_vecs = new HashMap<>();

    public Map<String, Set<String>> stopwords = new HashMap<>();

    private DB db;
    public int dim;
    private Map<String, float[]> vec_cache = new HashMap<>();
    private boolean use_mcache = true;

    public WordEmbedding() {
        stopwords.put("en", StopWord.getStopWords("en"));
    }

    public void closeDB() {
        if (db != null && !db.isClosed()) {
            db.commit();
            db.close();
        }
    }

    public void createMultiVec(String lang) {

        String path = ConfigParameters.db_path + "/multi-embeddings/" + lang;
        File dir = new File(path);

        if(!dir.isDirectory()) {
            logger.info("Creating dictionary: " + path);
            dir.mkdir();
        }
        loadDB(lang, false);
    }


    public void loadDB(String lang, boolean read_only) {
        logger.info("Loading " + lang + " multi vectors...");
        File f = new File(ConfigParameters.db_path, "multi-embeddings/" + lang + "/" + lang);

        if (read_only) {
            db = DBMaker.fileDB(f)
                    .fileChannelEnable()
                    .allocateStartSize(1024*1024*1024) // 1G
                    .allocateIncrement(1024*1024*1024)
                    .closeOnJvmShutdown()
                    .readOnly()
                    .make();
            multi_vec_en = db.hashMap("en")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.FLOAT_ARRAY)
                    .open();

            multi_vec_lang = db.hashMap("lang")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.FLOAT_ARRAY)
                    .open();
        } else {
            db = DBMaker.fileDB(f)
                    .closeOnJvmShutdown()
                    .make();

            multi_vec_en = db.hashMap("en")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.FLOAT_ARRAY)
                    .create();

            multi_vec_lang = db.hashMap("lang")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.FLOAT_ARRAY)
                    .create();
        }

        multi_vecs.put("en", multi_vec_en);
        multi_vecs.put(lang, multi_vec_lang);
        if (multi_vec_en.containsKey("obama"))
            dim = multi_vec_en.get("obama").length;

        stopwords.put(lang, StopWord.getStopWords(lang));
    }

    public float[] getVectorFromWords(List<String> words, String lang) {
        List<float[]> vecs = new ArrayList<>();
        for (String word : words) {
            float[] vec = getWordVector(word, lang);
            if (vec != null)
                vecs.add(vec);
        }
        return averageVectors(vecs);
    }

    public float[] getVectorFromWords(String[] words, String lang) {
        return getVectorFromWords(Arrays.asList(words), lang);
    }

    public float[] zeroVec() {
        float[] ret = new float[dim];
        Arrays.fill(ret, (float) 0.0);
        return ret;
    }

    public float[] averageVectors(List<float[]> vecs) {
        float[] ret = zeroVec();

        int n = 0;
        for (float[] v : vecs) {
            if (v == null) continue;
            for (int i = 0; i < v.length; i++)
                ret[i] += v[i];
            n++;
        }
        if (vecs.size() > 0)
            for (int i = 0; i < dim; i++)
                ret[i] /= n;
        return ret;
    }

    public float[] averageVectors(List<float[]> vecs, List<Float> weights) {
        float[] ret = zeroVec();

        float sum = 0;
        for (int i = 0; i < vecs.size(); i++) {
            float w = weights.get(i);
            sum += w;
            for (int j = 0; j < vecs.get(i).length; j++)
                ret[j] += w * vecs.get(i)[j];
        }

        if (vecs.size() > 0 && sum > 0)
            for (int i = 0; i < dim; i++)
                ret[i] /= sum;
        return ret;
    }


    public void loadEmbeddingToDB(String file, HTreeMap<String, float[]> map) {

        System.out.println("Loading word embeddings from " + file);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            int j = 0;
            while (line != null) {
                if (j++ % 10000 == 0) System.out.print(j + "\r");
                String[] tokens = line.trim().split("\\s+");
                float[] vec = new float[tokens.length - 1];
                dim = tokens.length - 1;
                for (int i = 1; i < tokens.length; i++)
                    vec[i - 1] = Float.parseFloat(tokens[i]);
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


    // TODO: fix the cache
    public float[] getVector(String query, String lang) {
        if (use_mcache && vec_cache.containsKey(query))
            return vec_cache.get(query);

        if ((stopwords.containsKey(lang) && stopwords.get(lang).contains(query))
                || (!multi_vecs.get(lang).containsKey(query))) {
            if (use_mcache) vec_cache.put(query, null);
            return null;
        }

        float[] vec = multi_vecs.get(lang).get(query);
        if (use_mcache) vec_cache.put(query, vec);
        return vec;
    }

    /**
     * The main function to get word vectors
     *
     * @param word
     * @param lang
     * @return
     */
    public float[] getWordVector(String word, String lang) {
        if (!multi_vecs.containsKey(lang)) {
            System.err.println("Couldn't find word embeddings for " + lang);
            System.exit(-1);
        }
        word = word.toLowerCase();
        return getVector(word, lang);
    }

    /**
     * The main function to get title vectors
     *
     * @param title
     * @param lang
     * @return
     */
    public float[] getTitleVector(String title, String lang) {
        if (title == null || title.startsWith("NIL")) {
            return null;
        }

        title = "title_" + title.replaceAll(" ", "_").toLowerCase();
        return getVector(title, lang);
    }

    public float cosine(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            System.out.println("Array size don't match");
            System.exit(-1);
        }

        float n1 = 0, n2 = 0, in = 0;
        for (int i = 0; i < v1.length; i++) {
            in += v1[i] * v2[i];
            n1 += v1[i] * v1[i];
            n2 += v2[i] * v2[i];
        }

        if (n1 == 0 || n2 == 0)
            return 0;

        return (float) (in / (Math.sqrt(n1) * Math.sqrt(n2)));
    }

    public static void main(String[] args) {
        try {
            ConfigParameters.setPropValues(args[1]);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        WordEmbedding we = new WordEmbedding();
        String dir = "/shared/preprocessed/ctsai12/multilingual/cca/";
//        String lang = args[0];

        ArrayList<String> langs = null;
        try {
            langs = LineIO.read("import-langs");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        boolean start = false;
        for (String lang : langs) {
            lang = lang.trim();
            if(lang.equals("da") && !start) start = true;
            if(start) {
//        String name = "es";
                we.createMultiVec(lang);
                we.loadEmbeddingToDB(dir + "en" + lang + "_orig1_projected.txt", we.multi_vec_en);
                we.loadEmbeddingToDB(dir + "en" + lang + "_orig2_projected.txt", we.multi_vec_lang);
//        we.loadEmbeddingToDB("/shared/preprocessed/ctsai12/multilingual/vectors/vectors.olden", we.multi_vec_en);
//            we.loadEmbeddingToDB(dir + lang + "/en.txt", we.multi_vec_en);
//            we.loadEmbeddingToDB(dir + lang + "/" + lang + ".txt", we.multi_vec_lang);

                we.closeDB();
            }
        }
    }
}
