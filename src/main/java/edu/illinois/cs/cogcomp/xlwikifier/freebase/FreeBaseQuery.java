package edu.illinois.cs.cogcomp.xlwikifier.freebase;

import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import org.mapdb.*;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerArray;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by ctsai12 on 5/13/16.
 */
public class FreeBaseQuery {
    private static DB db;
    public static HTreeMap<String, String[]> mid2types;
    public static HTreeMap<String, String> titlelang2mid;
    public static HTreeMap<String, String[]> midlang2title;

    public static boolean isloaded(){
        return db != null;
    }

    public static void loadDB(boolean read_only){

        String db_file = ConfigParameters.db_path+"/freebase/mapdb";
//        String db_file = "/shared/bronte/ctsai12/freebase/mapdb-tmp";

        if(read_only) {
            db = DBMaker.fileDB(db_file)
                    .closeOnJvmShutdown()
                    .readOnly()
                    .make();
            mid2types = db.hashMap("mid2types")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(new SerializerArray(Serializer.STRING))
                    .open();
            titlelang2mid = db.hashMap("titlelang2mid")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .open();
        }
        else {
            db = DBMaker.fileDB(db_file)
                    .closeOnJvmShutdown()
                    .make();
            mid2types = db.hashMap("mid2types")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(new SerializerArray(Serializer.STRING))
                    .createOrOpen();
            midlang2title = db.hashMap("midlang2title")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(new SerializerArray(Serializer.STRING))
                    .createOrOpen();
            titlelang2mid = db.hashMap("titlelang2mid")
                    .keySerializer(Serializer.STRING)
                    .valueSerializer(Serializer.STRING)
                    .createOrOpen();
        }

    }

    public static void importDump() throws IOException {

        String file = "/shared/preprocessed/ctsai12/freebase/fb.plain.new";
        FreeBaseQuery.loadDB(false);

//        DB.TreeMapSink<String,String[]> m2tsink = db
//                .treeMap("mid2types", Serializer.STRING, new SerializerArray<>(Serializer.STRING))
//                .createFromSink();
//        DB.TreeMapSink<String,String> tl2msink = db
//                .treeMap("mid2types", Serializer.STRING, Serializer.STRING)
//                .createFromSink();
//        DB.TreeMapSink<String,String[]> ml2tsink = db
//                .treeMap("mid2types", Serializer.STRING, new SerializerArray<>(Serializer.STRING))
//                .createFromSink();

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        String mid = "";
        List<String> types = new ArrayList<>();
        Map<String, List<String>> lang2titles = new HashMap<>();

        int cnt = 0;
        while(line != null){
            if(++cnt%100000 == 0){
                System.out.print(cnt+"\r");
            }
//            if(cnt == 100000) break;
            String[] parts = line.trim().split("\t");

            // done with the previous mid, put into DB
//            if(!parts[0].equals(mid) && !mid.isEmpty()){
//
//                populateMid2Types(mid, types);
//
//                types = new ArrayList<>();
//                lang2titles = new HashMap<>();
//            }

            // process the current line
            mid = parts[0];
//            if(parts.length == 2) types.add(parts[1]);
            if(parts.length == 3 && !parts[1].contains("_id")){
                String lang = parts[1];
                String title = parts[2];
//                if(!lang2titles.containsKey(lang)) lang2titles.put(lang, new ArrayList<>());
//                lang2titles.get(lang).add(title);
//                tl2msink.put(title+"|"+lang, mid);
                populateTitleLang2Mid(title+"|"+lang, mid);
            }

            line = br.readLine();
        }
//        titlelang2mid = tl2msink.create();
//        midlang2title = ml2tsink.create();
//        mid2types = m2tsink.create();

        FreeBaseQuery.closeDB();
    }

    public static void populateTitleLang2Mid(String key, String mid){
        titlelang2mid.put(key, mid);
    }

    public static void populateMid2Types(String mid, List<String> types){
        types = types.stream().distinct().collect(toList());
        String[] tmp = new String[types.size()];
        tmp = types.toArray(tmp);
        mid2types.put(mid, tmp);
    }

    public static void populateMidLang2Titles(String mid, Map<String, List<String>> lang2titles){
        for(String lang: lang2titles.keySet()){
            List<String> titles = lang2titles.get(lang);
            String[] tmp = new String[titles.size()];
            tmp = titles.toArray(tmp);
            midlang2title.put(mid+"|"+lang, tmp);
        }
    }

//    public static void pumpMid2Types(Map<String, List<String>> collect){
//        System.out.println("Transforming...");
//        List<Fun.Tuple2<String, String[]>> data = new ArrayList<>();
//        for(String mid: collect.keySet()){
//            List<String> types = collect.get(mid);
//            types = types.stream().distinct().collect(toList());
//            String[] tmp = new String[types.size()];
//            tmp = types.toArray(tmp);
//            data.add(new Fun.Tuple2<>(mid, tmp));
//        }
//        Comparator<Fun.Tuple2<String, String[]>> comparator = new Comparator<Fun.Tuple2<String,String[]>>(){
//
//            @Override
//            public int compare(Fun.Tuple2<String, String[]> o1,
//                               Fun.Tuple2<String, String[]> o2) {
//                return o1.a.compareTo(o2.a);
//            }
//        };
//
//        System.out.println("Sorting...");
//        // need to reverse sort list
//        Iterator<Fun.Tuple2<String, String[]>> iter = Pump.sort(data.iterator(),
//                true, 100000,
//                Collections.reverseOrder(comparator), //reverse  order comparator
//                db.getDefaultSerializer()
//        );
//
//
//        BTreeKeySerializer<String> keySerializer = BTreeKeySerializer.STRING;
//
//        System.out.println("Pumping...");
//        BTreeMap<Object, Object> map = db.createTreeMap("mid2types")
//                .pumpSource(iter)
//                .pumpPresort(100000)
//                .keySerializer(keySerializer)
//                .make();
//    }

    public static void closeDB(){
        if(db != null && !db.isClosed())
            db.close();
    }

    public static List<String> getTitlesFromMid(String mid, String lang){
        String key = mid+"|"+lang;
        if(midlang2title.containsKey(key))
            return Arrays.asList(midlang2title.get(key));
        else
            return null;
    }

    public static String getMidFromTitle(String title, String lang){
        title = formatTitle(title);
        String key = title+"|"+lang;
        if(titlelang2mid.containsKey(key))
            return titlelang2mid.get(key);
        else
            return null;
    }

    public static List<String> getTypesFromTitle(String title, String lang){
        String mid = getMidFromTitle(title, lang);
        if(mid == null) return new ArrayList<>();
        return getTypesFromMid(mid);
    }

    public static Set<String> getCoarseTypeSet(String mid){

        List<String> types = getTypesFromMid(mid);
        Set<String> tokenset = types.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\.")).stream())
                .collect(toSet());
        return tokenset;
    }

    public static List<String> getTypesFromMid(String mid){
        if(mid!=null && mid2types.containsKey(mid))
            return Arrays.asList(mid2types.get(mid));
        else
            return new ArrayList<>();
    }

    public static String formatTitle(String title){
        if(title.contains(" "))
            title = title.replaceAll(" ", "_");
        if(title.toLowerCase().equals(title)){
            String tmp = "";
            for(String token: title.split("_")){
                if(token.length()==0) continue;
                tmp+=token.substring(0,1).toUpperCase();
                if(token.length()>1)
                    tmp+=token.substring(1,token.length());
                tmp+="_";
            }
            title = tmp.substring(0, tmp.length()-1);
        }
        return title;
    }

    public static void main(String[] args) {

//        DBMaker.fileDB("/shared/preprocessed/ctsai12/multilingual/mapdb-new1/freebase/testdb")
//                .closeOnJvmShutdown()
//                .make();
//        System.exit(-1);

//        FreeBaseQuery.loadDB(true);
        ConfigParameters params = new ConfigParameters();
        params.setPropValues();
        try {
            FreeBaseQuery.importDump();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        String mid = FreeBaseQuery.getMidFromTitle("巴拉克·歐巴馬", "zh-cn");
//        System.out.println(mid);
//        System.out.println(FreeBaseQuery.getTypesFromMid(mid));
//        for(String key: titlelang2mid.keySet()){
//            if(key.contains("|ug"))
//                System.out.println(key);
//        }
//        String mid = FreeBaseQuery.getMidFromTitle("巴拉克·歐巴馬", "zh-cn");
//        System.out.println(mid);
//        System.out.println(FreeBaseQuery.getTypesFromMid(mid).size());
//        List<String> fbtypes = FreeBaseQuery.getTypesFromMid(mid);
//        Set<String> tokenset = fbtypes.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\.")).stream())
//                .collect(toSet());
//        System.out.println(tokenset);

    }


}
