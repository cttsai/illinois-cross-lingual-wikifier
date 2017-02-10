package edu.illinois.cs.cogcomp.xlwikifier.wikipedia;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 12/4/15.
 */
public class LangLinker {

    private DB db;
    public HTreeMap<String, String> to_en;
    public HTreeMap<String, String> from_en;
    private String lang;
    private Map<String, String> to_cache = new HashMap<>();
    private Map<String, String> from_cache = new HashMap<>();
    private static Logger logger = LoggerFactory.getLogger(LangLinker.class);

    public LangLinker() {

    }

    public void loadDB(String lang, boolean read_only) {
        if (lang.equals("en")) {
            logger.error("no English DB for lang links");
            System.exit(-1);
        }

        String dbfile = ConfigParameters.db_path + "/titlemap/" + lang;

        to_cache.clear();
        from_cache.clear();

        if (read_only) {
            db = DBMaker.fileDB(dbfile)
                    .fileChannelEnable()
                    .closeOnJvmShutdown()
                    .readOnly()
                    .make();
            to_en = db.hashMap("toen")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .open();
            from_en = db.hashMap("fromen")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .open();

        } else {
            db = DBMaker.fileDB(dbfile)
                    .closeOnJvmShutdown()
                    .make();
            to_en = db.hashMap("toen")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .create();
            from_en = db.hashMap("fromen")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .create();
        }


        this.lang = lang;
    }

    public void closeDB() {
        if (db != null && !db.isClosed()) {
            db.commit();
            db.close();
        }
    }


    public void populateDBNew(String lang, String lang_file, String page_file) {
//        ChineseTokenizer ct = null;
//        if (lang.equals("zh"))
//            ct = new ChineseTokenizer();
        loadDB(lang, false);
        DumpReader dr = new DumpReader();
        dr.readTitle2ID(page_file, lang);
        dr.readId2En(lang_file, "en");
        List<String> aligns = new ArrayList<>();
        for (String id : dr.id2title.keySet()) {
            if (dr.id2en.containsKey(id)) {
                String en = formatTitle(dr.id2en.get(id));
                String foreign = formatTitle(dr.id2title.get(id));
//                if (lang.equals("zh"))
//                    foreign = ChineseHelper.convertToSimplifiedChinese(foreign);
//                    foreign = ct.trad2simp(foreign);
                if (!en.isEmpty() && !foreign.isEmpty()) {
                    to_en.put(foreign, en);
                    from_en.put(en, foreign);
                    aligns.add("title_" + en + " ||| title_" + foreign);
                }
            }
        }

        closeDB();

        String ali_file = ConfigParameters.dump_path+"/"+ lang + "/titles.en" + lang + ".align";
        try {
            FileUtils.writeStringToFile(new File(ali_file), aligns.stream().collect(joining("\n")), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String formatTitle(String title) {
        String[] tokens = title.toLowerCase().split("\\s+");
        return Arrays.asList(tokens).stream().collect(joining("_"));
    }

    public String translateToEn(String title, String lang) {
        if (lang.equals("en")) {
            return title;
        }

        if (db == null || this.lang == null || !this.lang.equals(lang)) {
            loadDB(lang, true);
            this.lang = lang;
        }

        title = title.toLowerCase().replaceAll(" ", "_");

        String cache_key = title + "_" + lang;
        if (to_cache.containsKey(cache_key)) {
            if (to_cache.get(cache_key).equals("NIL"))
                return null;
            return to_cache.get(cache_key);
        }
        if (to_en.containsKey(title)) {
            String ret = to_en.get(title);
            to_cache.put(cache_key, ret);
            return ret;
        }

        to_cache.put(cache_key, "NIL");
        return null;
    }

    public String translateFromEn(String title, String lang) {
        if (lang.equals("en")) {
            logger.error("Translate English Title to English");
            System.exit(-1);
        }
        if (db == null || !this.lang.equals(lang)) {
            loadDB(lang, true);
            this.lang = lang;
        }
        title = title.toLowerCase().replaceAll(" ", "_");

        String cache_key = title + "_" + lang;
        if (from_cache.containsKey(cache_key)) {
            if (from_cache.get(cache_key).equals("NIL"))
                return null;
            return from_cache.get(cache_key);
        }
        if (from_en.containsKey(title)) {
            String ret = from_en.get(title);
            from_cache.put(cache_key, ret);
            return ret;
        }

        from_cache.put(cache_key, "NIL");
        return null;
    }

    public static void main(String[] args) {

        try {
            ConfigParameters.setPropValues();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        LangLinker ll = new LangLinker();
//        ll.loadDB("zh", true);

        System.out.println(ll.translateToEn(ChineseHelper.convertToSimplifiedChinese("希拉蕊·柯林頓"), "zh"));
    }
}
