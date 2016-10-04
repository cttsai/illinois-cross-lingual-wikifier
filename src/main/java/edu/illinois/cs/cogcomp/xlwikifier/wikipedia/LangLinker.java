package edu.illinois.cs.cogcomp.xlwikifier.wikipedia;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.tokenizers.ChineseTokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import org.apache.commons.io.FileUtils;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 12/4/15.
 */
public class LangLinker {

    private DB db;
    public ConcurrentNavigableMap<String, String> to_en;
    public ConcurrentNavigableMap<String, String> from_en;
    private String lang;
    private Map<String, String> to_cache = new HashMap<>();
    private Map<String, String> from_cache = new HashMap<>();
    private Set<String> skip = new HashSet<>();
    public double factor = 0;
    public LangLinker(){

    }

    public void loadDB(String lang, boolean read_only){
        if(lang.equals("en")){
            System.out.println("no en db for lang link");
            System.exit(-1);
        }

        if(read_only){
            db = DBMaker.newFileDB(new File(ConfigParameters.db_path+"/titlelang", lang))
                    .cacheSize(1000)
                    .transactionDisable()
                    .closeOnJvmShutdown()
                    .readOnly()
                    .make();

        }
        else {
            db = DBMaker.newFileDB(new File(ConfigParameters.db_path + "/titlelang", lang))
                    .cacheSize(1000)
                    .transactionDisable()
                    .closeOnJvmShutdown()
                    .make();
        }
        to_en = db.createTreeMap("toen")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        from_en = db.createTreeMap("fromen")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();

        this.lang = lang;

        if(factor > 0) {
            List<String> titles = to_en.keySet().stream().collect(toList());
            Collections.shuffle(titles, new Random(0));
            int s = (int) (titles.size()*factor);
            this.skip.addAll(titles.subList(0, s));
            System.out.println("------------------------");
            System.out.println("load "+lang+" db factor:"+factor+" skip:"+skip.size());
            System.exit(-1);
        }
    }

    public void closeDB(){
        if(db != null && !db.isClosed()){
            db.commit();
            db.close();
        }
    }


    public void populateDBNew(String lang, String lang_file, String page_file){
        ChineseTokenizer ct = null;
        if(lang.equals("zh"))
            ct = new ChineseTokenizer();
        loadDB(lang, false);
        DumpReader dr = new DumpReader();
        dr.readTitle2ID(page_file);
        dr.readId2En(lang_file, "en");
        List<String> aligns = new ArrayList<>();
        for(String id: dr.id2title.keySet()){
            if(dr.id2en.containsKey(id)){
                String en = formatTitle(dr.id2en.get(id));
                String foreign = formatTitle(dr.id2title.get(id));
                if(lang.equals("zh"))
                    foreign = ct.trad2simp(foreign);
                if(!en.isEmpty() && !foreign.isEmpty()){
                    to_en.put(foreign, en);
                    from_en.put(en, foreign);
                    aligns.add("title_"+en+" ||| title_"+foreign);
                }
            }
        }

        String ali_file = "/shared/preprocessed/ctsai12/multilingual/wikidump/"+lang+"/titles.en"+lang+".align";
        try {
            FileUtils.writeStringToFile(new File(ali_file), aligns.stream().collect(joining("\n")), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String formatTitle(String title){
        String[] tokens = title.toLowerCase().split("\\s+");
        return Arrays.asList(tokens).stream().collect(joining("_"));
    }

    public String translateToEn(String title, String lang){
        if(lang.equals("en")){
            return title;
//            System.out.println("Translate EN to EN");
//            System.exit(-1);
        }
        if(db == null || this.lang == null || !this.lang.equals(lang)){
            loadDB(lang, true);
            this.lang = lang;
            if(to_en.size()==0){
                System.out.println("lang link db is empty");
                System.exit(-1);
            }

        }

        title = title.toLowerCase().replaceAll(" ", "_");

        String cache_key = title+"_"+lang;
        if(to_cache.containsKey(cache_key)) {
            if(to_cache.get(cache_key).equals("NIL"))
                return null;
            return to_cache.get(cache_key);
        }
        if(to_en.containsKey(title) && !skip.contains(title)) {
            String ret = to_en.get(title);
            to_cache.put(cache_key, ret);
            return ret;
        }

        to_cache.put(cache_key, "NIL");
        return null;
    }

    public String translateFromEn(String title, String lang){
        if(lang.equals("en")){
            System.out.println("Translate EN to EN");
            System.exit(-1);
        }
        if(db == null || !this.lang.equals(lang)){
            loadDB(lang, true);
            this.lang = lang;
            if(from_en.size()==0){
                System.out.println("lang link db is empty");
                System.exit(-1);
            }
        }
        title = title.toLowerCase().replaceAll(" ", "_");

        String cache_key = title+"_"+lang;
        if(from_cache.containsKey(cache_key)) {
            if(from_cache.get(cache_key).equals("NIL"))
                return null;
            return from_cache.get(cache_key);
        }
        if(from_en.containsKey(title)) {
            String ret = from_en.get(title);
            from_cache.put(cache_key, ret);
            return ret;
        }

        from_cache.put(cache_key, "NIL");
        return null;
    }

    public static void main(String[] args) {

        LangLinker ll = new LangLinker();
        ll.loadDB("tr", true);
        System.out.println(ll.to_en.size());
//        ll.translateToEn("", "tr");
//        String page = "/shared/bronte/ctsai12/multilingual/wikidump/es/eswiki-20150901-page.sql";
//        String lang = "/shared/bronte/ctsai12/multilingual/wikidump/es/eswiki-20150901-langlinks.sql.gz";
//        String lang = "fr";
//        String page = "/shared/bronte/ctsai12/multilingual/wikidump/"+lang+"/"+lang+"wikipedia-20151123-page.sql";
//        String lang_file = "/shared/bronte/ctsai12/multilingual/wikidump/"+lang+"/"+lang+"wikipedia-20151123-langlinks.sql.gz";
//        ll.populateDBNew(lang, lang_file, page);
        ll.closeDB();
    }
}
