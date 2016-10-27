package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 12/14/15.
 */
public class TFIDFManager {

    private DB db;
    public HTreeMap<String, Integer> word2df;
    private String lang;

    public TFIDFManager() {

    }

    public void loadDB(String lang, boolean read_only) {
        if (read_only) {
            db = DBMaker.fileDB(new File(ConfigParameters.db_path + "/tfidf", lang))
                    .closeOnJvmShutdown()
                    .readOnly()
                    .make();
            word2df = db.hashMap("df")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.INTEGER)
                    .open();
        } else {
            db = DBMaker.fileDB(new File(ConfigParameters.db_path + "/tfidf", lang))
                    .closeOnJvmShutdown()
                    .make();
            word2df = db.hashMap("df")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.INTEGER)
                    .create();
        }
        this.lang = lang;
    }


    public Map<String, Float> getWordWeights(List<String> words, String lang) {
        if (db == null || !this.lang.equals(lang)) {
            loadDB(lang, true);
            if (word2df.size() == 0) {
                System.out.println("TFIDF db of " + lang + " is empty");
                System.exit(-1);
            }
        }

        Map<String, Float> ret = new HashMap<>();
        Map<String, Long> w2cnt = words.stream().map(x -> x.toLowerCase()).collect(groupingBy(x -> x, counting()));

        float tf_sum = 0;
        float df_sum = 0;
        Map<String, Integer> w2df = new HashMap<>();
        for (String w : w2cnt.keySet()) {
            tf_sum += w2cnt.get(w);
            if (word2df.containsKey(w)) {
                int d = word2df.get(w);
                w2df.put(w, d);
                df_sum += d;
            } else
                w2df.put(w, 0);
        }

        for (String w : w2cnt.keySet()) {
            float tf = w2cnt.get(w) / tf_sum;
            float idf = 0;
            if (w2df.get(w) != 0)
                idf = (float) Math.log(df_sum / w2df.get(w));
            ret.put(w, tf * idf);
        }
        return ret;
    }

    public void populateDB(String lang, String file) throws IOException {
        loadDB(lang, false);
        word2df.clear();


        Map<String, Integer> w2d = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        int cnt = 0;
        while (line != null) {
            if (cnt++ % 10000 == 0) System.out.print(cnt + "\r");

            String[] tokens = line.toLowerCase().split("\\s+");
            Set<String> ts = Arrays.asList(tokens).stream().collect(toSet());
            for (String t : ts) {
                if (!w2d.containsKey(t))
                    w2d.put(t, 1);
                else
                    w2d.put(t, w2d.get(t) + 1);
            }
            line = br.readLine();
        }
        br.close();

        for (String w : w2d.keySet()) {
            word2df.put(w, w2d.get(w));
        }

        closeDB();
    }

    public void closeDB() {
        if (db != null && !db.isClosed()) {
            db.commit();
            db.close();
        }
    }

    public static void main(String[] args) {
        TFIDFManager tm = new TFIDFManager();
        String lang = "ar";
        try {
            tm.populateDB(lang, "/shared/bronte/ctsai12/multilingual/text/" + lang + ".withtitle");
        } catch (IOException e) {
            e.printStackTrace();
        }
        tm.closeDB();
    }
}
