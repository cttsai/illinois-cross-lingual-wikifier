package edu.illinois.cs.cogcomp.xlwikifier.freebase;

import org.mapdb.*;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by ctsai12 on 5/13/16.
 */
public class FreeBaseQuery {
    private static DB db;
//    private static NavigableSet<Fun.Tuple3<String, String, String[]>> midlang2title;
//    private static NavigableSet<Fun.Tuple3<String, String, String>> titlelang2mid;
    private static ConcurrentNavigableMap<String, String[]> mid2types;
    public static ConcurrentNavigableMap<String, String> titlelang2mid;
    private static ConcurrentNavigableMap<String, String[]> midlang2title;

    public static boolean isloaded(){
        return db != null;
    }

    public static void loadDB(boolean read_only){

        String db_file = "/shared/experiments/ctsai12/freebase/mapdb/db";
//        "/shared/preprocessed/ctsai12/freebase/mapdb/db"

        if(read_only) {
            db = DBMaker.newFileDB(new File(db_file))
                    .cacheSize(30000)
                    .transactionDisable()
                    .closeOnJvmShutdown()
                    .readOnly()
                    .make();
        }
        else {
            db = DBMaker.newFileDB(new File(db_file))
                    .cacheSize(30000)
                    .transactionDisable()
                    .closeOnJvmShutdown()
                    .make();
        }

        mid2types = db.createTreeMap("mid2types")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        midlang2title = db.createTreeMap("midlang2title")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
        titlelang2mid = db.createTreeMap("titlelang2mid")
                .keySerializer(BTreeKeySerializer.STRING)
                .makeOrGet();
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

    public static void pumpMid2Types(Map<String, List<String>> collect){
        System.out.println("Transforming...");
        List<Fun.Tuple2<String, String[]>> data = new ArrayList<>();
        for(String mid: collect.keySet()){
            List<String> types = collect.get(mid);
            types = types.stream().distinct().collect(toList());
            String[] tmp = new String[types.size()];
            tmp = types.toArray(tmp);
            data.add(new Fun.Tuple2<>(mid, tmp));
        }
        Comparator<Fun.Tuple2<String, String[]>> comparator = new Comparator<Fun.Tuple2<String,String[]>>(){

            @Override
            public int compare(Fun.Tuple2<String, String[]> o1,
                               Fun.Tuple2<String, String[]> o2) {
                return o1.a.compareTo(o2.a);
            }
        };

        System.out.println("Sorting...");
        // need to reverse sort list
        Iterator<Fun.Tuple2<String, String[]>> iter = Pump.sort(data.iterator(),
                true, 100000,
                Collections.reverseOrder(comparator), //reverse  order comparator
                db.getDefaultSerializer()
        );


        BTreeKeySerializer<String> keySerializer = BTreeKeySerializer.STRING;

        System.out.println("Pumping...");
        BTreeMap<Object, Object> map = db.createTreeMap("mid2types")
                .pumpSource(iter)
                .pumpPresort(100000)
                .keySerializer(keySerializer)
                .make();
    }

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

        FreeBaseQuery.loadDB(true);
        String mid = FreeBaseQuery.getMidFromTitle("巴拉克·歐巴馬", "zh-cn");
        System.out.println(mid);
        System.out.println(FreeBaseQuery.getTypesFromMid(mid));
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
