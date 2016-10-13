package edu.illinois.cs.cogcomp.xlwikifier.freebase;

import org.mapdb.*;

import java.io.*;
import java.util.*;

/**
 * Created by ctsai12 on 5/13/16.
 */
public class FreeBaseImporter {

    private static Map<String, List<String>> typecollect = new HashMap<>();
    private static Map<String, Map<String, List<String>>> titlecollect = new HashMap<>();
    private static Map<String, String> titlelangcollect = new HashMap<>();
    public static void importDump() throws IOException {

        String file = "/shared/preprocessed/ctsai12/freebase/fb.plain.new";
        FreeBaseQuery.loadDB(false);

        BufferedReader br = new BufferedReader(new FileReader(file));
        String line = br.readLine();
        String mid = "";
        List<String> types = new ArrayList<>();
        Map<String, List<String>> lang2titles = new HashMap<>();

        int cnt = 0;
        while(line != null){
            if(++cnt%100000 == 0) System.out.println(cnt);
            if((cnt+1) % 100000000 == 0) writeToDB();
//            if(cnt == 100000) break;
            String[] parts = line.trim().split("\t");

            // done with the previous mid, put into DB
            if(!parts[0].equals(mid) && !mid.isEmpty()){
                typecollect.put(mid, types);
                titlecollect.put(mid, lang2titles);
                types = new ArrayList<>();
                lang2titles = new HashMap<>();
            }

            // process the current line
            mid = parts[0];
            if(parts.length == 2) types.add(parts[1]);
            if(parts.length == 3 && !parts[1].contains("_id")){
                String lang = parts[1];
                String title = parts[2];
                if(!lang2titles.containsKey(lang)) lang2titles.put(lang, new ArrayList<>());
                lang2titles.get(lang).add(title);
                titlelangcollect.put(title+"|"+lang, mid);
            }

            line = br.readLine();
        }
        writeToDB();
        FreeBaseQuery.closeDB();
    }

    private static void writeToDB(){
        System.out.println("Writing to DB...");
        System.out.println("\tProcessing types..."+typecollect.size());
//        FreeBaseQuery.pumpMid2Types(typecollect);
        int cnt = 0;
        for(String key: typecollect.keySet()) {
            if(cnt++%100000 == 0) System.out.print(cnt+"\r");
            FreeBaseQuery.populateMid2Types(key, typecollect.get(key));
        }
        System.out.println("\tProcessing mid to titles..."+titlecollect.size());
        cnt = 0;
        for(String key: titlecollect.keySet()) {
            if (cnt++ % 100000 == 0) System.out.print(cnt + "\r");
            FreeBaseQuery.populateMidLang2Titles(key, titlecollect.get(key));
        }
        System.out.println("\tProcessing title to mid..."+titlelangcollect.size());
        cnt = 0;
        for(String key: titlelangcollect.keySet()) {
            if (cnt++ % 100000 == 0) System.out.print(cnt + "\r");
            FreeBaseQuery.populateTitleLang2Mid(key, titlelangcollect.get(key));
        }
        typecollect = new HashMap<>();
        titlecollect = new HashMap<>();
        titlelangcollect = new HashMap<>();

    }

//    public static void TestPump(String dataFile, List<Fun.Tuple2<String,String>>inputData) {
//
//
//
//        DB db = DBMaker.newFileDB(new File("test.db"))
//                .transactionDisable()
//                .closeOnJvmShutdown()
//                .make();
//
//        Comparator<Fun.Tuple2<String, String>> comparator = new Comparator<Fun.Tuple2<String,String>>(){
//
//            @Override
//            public int compare(Fun.Tuple2<String, String> o1,
//                               Fun.Tuple2<String, String> o2) {
//                return o1.a.compareTo(o2.a);
//            }
//        };
//
//        // need to reverse sort list
//        Iterator<Fun.Tuple2<String, String>> iter = Pump.sort(inputData.iterator(),
//                true, 100000,
//                Collections.reverseOrder(comparator), //reverse  order comparator
//                db.getDefaultSerializer()
//        );
//
//
//        BTreeKeySerializer<String> keySerializer = BTreeKeySerializer.STRING;
//
//        BTreeMap<Object, Object> map = db.createTreeMap(dataFile)
//                .pumpSource(iter)
//                .pumpPresort(100000)
//                .keySerializer(keySerializer)
//                .make();
//
//
//        // close/flush db
//        db.close();
//
//        // re-connect with transactions enabled
//        db = DBMaker.newFileDB(new File("test.db"))
//                .closeOnJvmShutdown()
//                .make();
//
//        map = db.getTreeMap(dataFile);
//
//        System.out.println(map.get("a"));
//        System.out.println(map.get("c"));
//    }

    public static void main(String[] args) {

//        List<Fun.Tuple2<String, String>> data = new ArrayList<>();
//        data.add(new Fun.Tuple2<String, String>("a", "b"));
//        data.add(new Fun.Tuple2<String, String>("c", "d"));
//        TestPump("test", data);
        try {
            FreeBaseImporter.importDump();
        } catch (IOException e) {
            e.printStackTrace();
        }

//        FreeBaseQuery.loadDB(true);
//        System.out.println(FreeBaseQuery.getMidFromTitle("Shane_Green", "en"));
//        System.out.println(FreeBaseQuery.getMidFromTitle("Shane_Greene", "fr"));
//        System.out.println(FreeBaseQuery.getTypesFromMid("m.0101hm2z"));
//        System.out.println(FreeBaseQuery.getTitlesFromMid("m.0101hm2z", "en"));
//        System.out.println(FreeBaseQuery.getTitlesFromMid("m.0101hm2z", "ja"));
    }
}
