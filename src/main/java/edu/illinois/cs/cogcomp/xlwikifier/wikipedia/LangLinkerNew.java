package edu.illinois.cs.cogcomp.xlwikifier.wikipedia;

import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 12/4/15.
 */
public class LangLinkerNew {

    private DB db;
    public BTreeMap<Object[], String> src2lang2tgt;
    public BTreeMap<String, String> redirects;
    private String lang;
    private static Logger logger = LoggerFactory.getLogger(LangLinkerNew.class);
    private static Map<String, LangLinkerNew> lang_linker_map;

    public LangLinkerNew(String lang, boolean read_only) {

        String dbfile = ConfigParameters.db_path + "/titlemapnew/" + lang;

        if (read_only) {
            db = DBMaker.fileDB(dbfile)
                    .fileChannelEnable()
                    .closeOnJvmShutdown()
                    .readOnly()
                    .make();

            src2lang2tgt = db.treeMap("s2l2t", new SerializerArray(Serializer.STRING), Serializer.STRING)
                                .open();
            redirects = db.treeMap("redirect", Serializer.STRING, Serializer.STRING).open();

        } else {
            System.out.println(dbfile);
            db = DBMaker.fileDB(dbfile)
                    .closeOnJvmShutdown()
                    .make();
            src2lang2tgt = db.treeMap("s2l2t", new SerializerArray(Serializer.STRING), Serializer.STRING)
                                .create();
            redirects = db.treeMap("redirect", Serializer.STRING, Serializer.STRING).create();
        }


        this.lang = lang;

    }

    public static LangLinkerNew getLangLinker(String lang){
        if(lang_linker_map == null)
            lang_linker_map = new HashMap<>();

        if(!lang_linker_map.containsKey(lang)){
            logger.info("Initializing LangLinker "+lang);
            LangLinkerNew ll = new LangLinkerNew(lang, true);
            lang_linker_map.put(lang, ll);
        }

        return lang_linker_map.get(lang);
    }

    public void closeDB() {
        if (db != null && !db.isClosed()) {
            db.commit();
            db.close();
        }
    }


    public void populateDBNew(String lang, String lang_file, String page_file, String redirect_file) {

        DumpReader dr = new DumpReader();
        dr.readTitle2ID(page_file, lang);
        dr.readId2Lang2Title(lang_file);

        for (String id : dr.id2title.keySet()) {
            if (dr.id2lang2title.containsKey(id)) {
                String src = formatTitle(dr.id2title.get(id));
                for(String la: dr.id2lang2title.get(id).keySet()) {
                    String tgt = formatTitle(dr.id2lang2title.get(id).get(la));
                    if (!src.isEmpty() && !tgt.isEmpty()) {
                        src2lang2tgt.put(new Object[]{src, la}, tgt);
                    }
                }
            }
        }

        dr.readRedirectTitle2ID(page_file, lang);
        dr.readRedirects(redirect_file, lang);
        for(String id: dr.id2redirect.keySet()){
            if(dr.id2title.containsKey(id)){
                redirects.put(dr.id2title.get(id), dr.id2redirect.get(id));
            }
        }
        closeDB();
    }

    public String formatTitle(String title) {
        String[] tokens = title.toLowerCase().split("\\s+");
        return Arrays.asList(tokens).stream().collect(joining("_"));
    }

    public String getRedirect(String title){
        title = formatTitle(title);
        if(redirects.containsKey(title))
            return redirects.get(title);
        return null;
    }

    public Map<String, String> translate(String title) {
        title = formatTitle(title);
        NavigableMap<Object[], String> entries = src2lang2tgt.subMap(new Object[]{title}, new Object[]{title, null});
        Map<String, String> ret = new HashMap<>();
        for(Object[] obj: entries.keySet()){
            ret.put((String)obj[1], entries.get(obj));
        }
        return ret;
    }

    public String translateTo(String lang, String title) {
        Map<String, String> lang2title = translate(title);
        if(lang2title.containsKey(lang))
            return lang2title.get(lang);
        return null;
    }

    public static void main(String[] args) {

        try {
            ConfigParameters.setPropValues("config/xlwikifier-demo.config");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        String lang = "es";
        String date = "20160801";
        String dumpdir = "/shared/preprocessed/ctsai12/multilingual/wikidump/"+lang+"/";
        String pagefile = dumpdir+lang+"wiki-"+date+"-page.sql.gz";
        String langfile = dumpdir+lang+"wiki-"+date+"-langlinks.sql.gz";
        String redirectfile = dumpdir+lang+"wiki-"+date+"-redirect.sql.gz";

//        LangLinkerNew ll = new LangLinkerNew(lang, false);
//        ll.populateDBNew(lang, langfile, pagefile, redirectfile);

        LangLinkerNew ll = LangLinkerNew.getLangLinker("es");
        int cnt = 0;
        for(Object src: ll.src2lang2tgt.keySet()){
            if(((Object[])src)[1].equals("en"))
                cnt++;
        }
        System.out.println(cnt);
//        System.out.println(ll.translate("barack obama"));
//        System.out.println(ll.getRedirect("birding"));
    }
}
