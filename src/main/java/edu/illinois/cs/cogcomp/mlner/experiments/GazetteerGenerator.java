package edu.illinois.cs.cogcomp.mlner.experiments;

import com.google.gson.Gson;
import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.xlwikifier.core.StopWord;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.DumpReader;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;
import org.h2.util.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * This class uses title mapping between English and the target language to
 * generate gazetters for the target language.
 *
 * Created by ctsai12 on 3/9/16.
 */
public class GazetteerGenerator {

    private Set<String> per_types = new HashSet<>();
    private Set<String> org_types = new HashSet<>();
    private Set<String> loc_types = new HashSet<>();
    private Set<String> song_types = new HashSet<>();
    private Set<String> film_types = new HashSet<>();

    private String dir = "/shared/corpora/ner/gazetteers/";
    public GazetteerGenerator(){

        per_types.add("/people/person");
        org_types.add("/organization/organization");
        loc_types.add("/location/location");
        song_types.add("/music/album");
        film_types.add("/film/film");
    }

    public static Set<String> loadFile(String file){

        try {
            return LineIO.read(file).stream().map(x -> x.toLowerCase()).collect(toSet());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Map<String, String> loadEngToFor(String file, boolean reverse){
        Map<String, String> ret = new HashMap<>();
        try {
            for(String line: LineIO.read(file)){
                String[] parts = line.split("\t");
                if(reverse)
                    ret.put(parts[0], parts[1]);
                else
                    ret.put(parts[1], parts[0]);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<Map.Entry<String, Integer>> getMostRepWords(Set<String> titles, String lang){
        Map<String, Integer> word2cnt = new HashMap<>();

        for(String title: titles){
            title = title.replaceAll(",","");
            String[] tokens = title.toLowerCase().split("\\s+");
            if(lang.equals("zh")){
                tokens = new String[title.length()];
                for(int i = 0; i < title.length(); i++)
                    tokens[i] = title.substring(i, i+1).toLowerCase();
            }

//            if(tokens.length==1) continue;
            for(String token: tokens){
                if(!word2cnt.containsKey(token)) word2cnt.put(token, 0);
                word2cnt.put(token, word2cnt.get(token)+1);
            }
        }

        List<Map.Entry<String, Integer>> ret = word2cnt.entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue()))
                .collect(toList());
        return ret;
    }

    private void genTitlePairs(String lang){

        String dir = "/shared/corpora/ner/gazetteers/"+lang;

        FreeBaseQuery.loadDB(true);
        LangLinker ll = new LangLinker();
        ll.loadDB(lang);
        String per_out = "", org_out = "", loc_out = "", all_out = "";

        String query_lang = lang;
        if(lang.equals("zh")) query_lang = "zh-cn";
        for(String ft: ll.to_en.keySet()){
            String mid = FreeBaseQuery.getMidFromTitle(ll.to_en.get(ft), "en");
//            System.out.println(ft+" "+mid);
            List<String> ts = new ArrayList<>();
            if(mid != null) ts.addAll(FreeBaseQuery.getTypesFromMid(mid));
            mid = FreeBaseQuery.getMidFromTitle(ft, query_lang);
            if(mid!=null) ts.addAll(FreeBaseQuery.getTypesFromMid(mid));


            String netype = "";
            if(ts.contains("people.person")) netype = "PER";
            if(ts.contains("organization.organization")) netype = "ORG";
            if(ts.contains("location.location")) netype = "LOC";
            if(netype.isEmpty()) continue;

            String title = ll.to_en.get(ft).toLowerCase();

            String tt = title.replaceAll("_", " ");
            int idx = tt.indexOf("(");
            if(idx > 0) tt = tt.substring(0, idx);
            tt = tt.trim();
            ft = ft.replaceAll("_", " ");
            idx = ft.indexOf("(");
            if(idx > 0) ft = ft.substring(0, idx);
            ft = ft.trim();

//            if(tt.equals(ft)) continue;
            if(StringUtils.isNumber(tt.substring(0, 1))) continue;
            if(StringUtils.isNumber(ft.substring(0, 1))) continue;

            if(netype.equals("PER"))
                per_out+=ft+"\t"+tt+"\n";
            else if(netype.equals("ORG"))
                org_out+=ft+"\t"+tt+"\n";
            else if(netype.equals("LOC"))
                loc_out+=ft+"\t"+tt+"\n";
            all_out+=ft+"\t"+tt+"\n";
        }

        try {
            FileUtils.writeStringToFile(new File(dir, "per."+lang), per_out, "UTF-8");
            FileUtils.writeStringToFile(new File(dir, "org."+lang), org_out, "UTF-8");
            FileUtils.writeStringToFile(new File(dir, "loc."+lang), loc_out, "UTF-8");
            FileUtils.writeStringToFile(new File(dir, "all."+lang), all_out, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static String removeParen(String text){
        text = text.replaceAll("_", " ");
        int idx = text.indexOf("(");
        if(idx > 0) text = text.substring(0, idx);
        return text.trim();
    }

    public static void getOneHopPairs(String target_lang, String bridge_lang){
        String dir = "/shared/corpora/ner/gazetteers/ug/";

        FreeBaseQuery.loadDB(true);

        Map<String, String> ug2mid = getTitleMapping("ug", bridge_lang);

        LangLinker ll = new LangLinker();
        ll.loadDB(bridge_lang);

        String query_lang = bridge_lang;
        if(bridge_lang.equals("zh")) query_lang = "zh-cn";

        String per = "", org = "", loc = "";
        for(String ug: ug2mid.keySet()){
            String bridge = ug2mid.get(ug);
            if(ll.to_en.containsKey(bridge)){
                String en = ll.to_en.get(bridge);
                String mid = FreeBaseQuery.getMidFromTitle(en, "en");
                List<String> ts = new ArrayList<>();
                if (mid != null) ts.addAll(FreeBaseQuery.getTypesFromMid(mid));
                mid = FreeBaseQuery.getMidFromTitle(bridge, query_lang);
                if (mid != null) ts.addAll(FreeBaseQuery.getTypesFromMid(mid));

                String netype = "";
                if (ts.contains("people.person")) netype = "PER";
                if (ts.contains("organization.organization")) netype = "ORG";
                if (ts.contains("location.location")) netype = "LOC";
                if (netype.isEmpty()) continue;

                ug = removeParen(ug);
                en = removeParen(en);
                if(netype.equals("PER"))
                    per += ug+"\t"+en+"\n";
                else if(netype.equals("ORG"))
                    org += ug+"\t"+en+"\n";
                else if(netype.equals("LOC"))
                    loc += ug+"\t"+en+"\n";
            }
        }

        try {
            FileUtils.writeStringToFile(new File(dir, "per.bridge."+bridge_lang), per, "UTF-8");
            FileUtils.writeStringToFile(new File(dir, "org.bridge."+bridge_lang), org, "UTF-8");
            FileUtils.writeStringToFile(new File(dir, "loc.bridge."+bridge_lang), loc, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static Map<String, String> getTitleMapping(String source_lang, String target_lang){
        String root_dir = "/shared/preprocessed/ctsai12/multilingual/";
        String lang = source_lang;
        String date = "20160701";
        String langfile = root_dir+"wikidump/"+lang+"/"+lang+"wiki-"+date+"-langlinks.sql.gz";
        String pagefile = root_dir+"wikidump/"+lang+"/"+lang+"wiki-"+date+"-page.sql.gz";
        DumpReader dr = new DumpReader();
        dr.readId2En(langfile, target_lang);
        dr.readTitle2ID(pagefile);
        LangLinker ll = new LangLinker();
        Map<String, String> ret = new HashMap<>();
        for(String id: dr.id2title.keySet()){
            if(dr.id2en.containsKey(id)){
                String target = ll.formatTitle(dr.id2en.get(id));
                String source = ll.formatTitle(dr.id2title.get(id));
                if(!target.isEmpty() && !source.isEmpty()){
                    ret.put(source, target);
                }
            }
        }
        return ret;
    }


    public static void getExternalGazetteers(){
        String ext = "/shared/corpora/corporaWeb/lorelei/evaluation-20160705/LDC2016E57_LORELEI_IL3_Incident_Language_Pack_for_Year_1_Eval/set0/docs/ChineseUyghurStarDict.txt";
        String dir = "/shared/corpora/ner/gazetteers/ug/";

        Map<String, String> zh2ug = new HashMap<>();
        try {
            for (String line : LineIO.read(ext)) {
                String[] parts = line.split("\t");
//                System.out.println(parts[0]);
				String[] tmp = parts[2].split("；");
                String ugword = tmp[tmp.length-1].trim();
//                System.out.println(ugword);
                zh2ug.put(parts[0],ugword);
            }
        } catch (Exception e){
            e.printStackTrace();
        }

        System.out.println("map size "+zh2ug.size());

        FreeBaseQuery.loadDB(true);
        LangLinker ll = new LangLinker();
        ll.loadDB("zh");

        String per = "", org = "", loc = "";
        for(String ft: ll.to_en.keySet()) {
            String mid = FreeBaseQuery.getMidFromTitle(ll.to_en.get(ft), "en");
//            System.out.println(ft+" "+mid);
            List<String> ts = new ArrayList<>();
            if (mid != null) ts.addAll(FreeBaseQuery.getTypesFromMid(mid));
            mid = FreeBaseQuery.getMidFromTitle(ft, "zh-cn");
            if (mid != null) ts.addAll(FreeBaseQuery.getTypesFromMid(mid));

            String netype = "";
            if (ts.contains("people.person")) netype = "PER";
            if (ts.contains("organization.organization")) netype = "ORG";
            if (ts.contains("location.location")) netype = "LOC";
            if (netype.isEmpty()) continue;

            String title = ll.to_en.get(ft);

            String tt = title.replaceAll("_", " ");
            int idx = tt.indexOf("(");
            if(idx > 0) tt = tt.substring(0, idx);
            tt = tt.trim();
            ft = ft.replaceAll("_", " ");
            idx = ft.indexOf("(");
            if(idx > 0) ft = ft.substring(0, idx);
            ft = ft.trim();

            if(zh2ug.containsKey(ft)){
                String ug = zh2ug.get(ft);
                idx = ug.indexOf("(");
                if(idx > 0 ) ug = ug.substring(0, idx);
                System.out.println(ug+"\t"+tt);
                if(netype.equals("PER"))
                    per += ug+"\t"+tt+"\n";
                else if(netype.equals("ORG"))
                    org += ug+"\t"+tt+"\n";
                else if(netype.equals("LOC"))
                    loc += ug+"\t"+tt+"\n";
            }
        }

        try {
            FileUtils.writeStringToFile(new File(dir, "per.dic"), per, "UTF-8");
            FileUtils.writeStringToFile(new File(dir, "org.dic"), org, "UTF-8");
            FileUtils.writeStringToFile(new File(dir, "loc.dic"), loc, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void run(String lang){

        Set<String> en_per = null, en_loc = null, en_org= null, en_song= null, en_film= null;
        en_per = loadFile("/shared/corpora/ner/gazetteers/en/per");
        en_loc = loadFile("/shared/corpora/ner/gazetteers/en/loc");
        en_org = loadFile("/shared/corpora/ner/gazetteers/en/org");
        en_song = loadFile("/shared/corpora/ner/gazetteers/en/song");
        en_film = loadFile("/shared/corpora/ner/gazetteers/en/film");
        LangLinker ll = new LangLinker();

        Set<String> per_titles = new HashSet<>();
        Set<String> org_titles = new HashSet<>();
        Set<String> loc_titles = new HashSet<>();
        Set<String> song_titles = new HashSet<>();
        Set<String> film_titles = new HashSet<>();

        ll.loadDB(lang);
        int cnt = 0;
        for(String ft: ll.to_en.keySet()){
            if(cnt++%1000 == 0){
                System.out.println(cnt);
                System.out.println(per_titles.size());
                System.out.println(loc_titles.size());
                System.out.println(org_titles.size());
                System.out.println(song_titles.size());
            }
            String title = ll.to_en.get(ft).toLowerCase();
            String tt = title.replaceAll("_", " ");
            int idx = tt.indexOf("(");
            if(idx > 0) tt = tt.substring(0, idx);
            tt = tt.trim();
            ft = ft.replaceAll("_", " ");
            idx = ft.indexOf("(");
            if(idx > 0) ft = ft.substring(0, idx);
            ft = formatTitle(ft.trim());

            if(en_per.contains(tt))
                per_titles.add(ft);
            if(en_loc.contains(tt))
                loc_titles.add(ft);
            if(en_org.contains(tt))
                org_titles.add(ft);
            if(en_song.contains(tt))
                song_titles.add(ft);
            if(en_film.contains(tt))
                film_titles.add(ft);
        }

        String path = dir +lang;
        try {
            FileUtils.writeStringToFile(new File(path+"/per"), per_titles.stream().collect(joining("\n")), "utf-8");
            FileUtils.writeStringToFile(new File(path+"/loc"), loc_titles.stream().collect(joining("\n")), "utf-8");
            FileUtils.writeStringToFile(new File(path+"/org"), org_titles.stream().collect(joining("\n")), "utf-8");
            FileUtils.writeStringToFile(new File(path+"/song"), song_titles.stream().collect(joining("\n")), "utf-8");
            FileUtils.writeStringToFile(new File(path+"/film"), film_titles.stream().collect(joining("\n")), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public String formatTitle(String title){
        String tmp = "";
        for(String token: title.split("\\s+")){
            if(token.length()==0) continue;
            tmp+=token.substring(0,1).toUpperCase();
            if(token.length()>1)
                tmp+=token.substring(1,token.length());
            tmp+=" ";
        }
        tmp = tmp.trim();
        String tmp1 = "";
        for(String token: tmp.split("-")){
            if(token.length()==0) continue;
            tmp1+=token.substring(0,1).toUpperCase();
            if(token.length()>1)
                tmp1+=token.substring(1,token.length());
            tmp1+="-";
        }

        return tmp1.substring(0, tmp1.length()-1);
    }

    public static List<Map.Entry<String, Integer>> getForeignWord(Map<String, String> wordmap, String word, String lang){
        Set<String> fo_words = new HashSet<>(); // the mapped target titles
        for(String w: wordmap.keySet()){
            if(lang.equals("zh")){
                if(w.contains(word))
                    fo_words.add(wordmap.get(w));
            }
            else {
                List<String> parts = Arrays.asList(w.replaceAll(",", "").split("\\s+"));
                if (parts.contains(word))
                    fo_words.add(wordmap.get(w));
            }
        }

        List<Map.Entry<String, Integer>> ranked = getMostRepWords(fo_words, "en");
//        for(int i = 0; i < 3 && i < ranked.size(); i++){
//            Map.Entry<String, Integer> m = ranked.get(i);
//            System.out.println("\t"+m);
//        }
        return ranked;
    }

    public static void getWordMapping(String lang, String type){
        boolean reverse = true;
        boolean dup = false;

        String stop_lang = "en";
        if(reverse) stop_lang = lang;

        String posfix = type;
//        if(reverse) posfix = lang+"-e;

//        String dir = "/shared/corpora/transliteration/wikipedia-ct/"+lang;
        String dir = "/shared/corpora/ner/gazetteers/"+lang;
        String outfile = "/shared/corpora/ner/gazetteers/"+lang+"/wordmap."+posfix;
//        Set<String> entitles = loadFile("/shared/corpora/ner/gazetteers/en/"+type);
        Map<String, String> wordmap = loadEngToFor(dir + "/"+type, reverse);
        List<Map.Entry<String, Integer>> rankword = getMostRepWords(wordmap.keySet(), lang);
        Set<String> stops = StopWord.getStopWords(stop_lang);
        String out = "";

        System.out.println("# words: "+rankword.size());

        int cnt = 0;
        for(Map.Entry<String, Integer> m: rankword) {
            if (cnt++ % 1000 == 0) System.out.println(cnt);
//            if(stops.contains(m.getKey())){
//                System.out.println("removing "+m.getKey());
//            }
//            else {
//                System.out.println(m);
            if (!lang.equals("zh") && stops.contains(m.getKey())) continue;
            if (!lang.equals("zh") && m.getKey().length() < 3) continue;
            List<Map.Entry<String, Integer>> topword = getForeignWord(wordmap, m.getKey(), lang);
            if (topword.size() == 0) continue;

            // if there are multiple winners, use edit distance to break tie
            Integer topscore = topword.get(0).getValue();
            int min = 100000;
            String match = "";
            for (Map.Entry<String, Integer> word : topword) {
                if (word.getValue().equals(topscore)) {
                    int edit = LevensteinDistance.getLevensteinDistance(m.getKey(), word.getKey());
                    if (edit < min) {
                        min = edit;
                        match = word.getKey();
                    } else if (edit == min) {  // if tie, check the initial character
                        String minit = m.getKey().substring(0, 1);
                        if (word.getKey().substring(0, 1).equals(minit) && !match.substring(0, 1).equals(minit))
                            match = word.getKey();
                    }
                }
            }

//            if (min > Math.min(m.getKey().length(), match.length())) continue; // this will filter out translation word pairs!

            if (dup) {
                for (int i = 0; i < topscore; i++)
                    out += m.getKey() + "\t" + match + "\n";
            }
            else
                out += m.getKey() + "\t" + match + "\n";
        }
        try {
            FileUtils.writeStringToFile(new File(outfile), out, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void filterMap(){
        String infile = "/shared/corpora/ner/gazetteers/tr/wordmap.tr-en.2";
        String outfile = "/shared/corpora/ner/gazetteers/tr/wordmap.tr-en.2.edit";

        String out = "";
        try {
            for(String line: LineIO.read(infile)){
                String[] parts = line.split("\t");
                int edit = LevensteinDistance.getLevensteinDistance(parts[0], parts[1]);
                if(edit > Math.min(parts[0].length(), parts[1].length())-1) continue;
                out += line +"\n";
            }
            FileUtils.writeStringToFile(new File(outfile), out, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void genGazFromWikidata(){
        String cache = "/shared/experiments/ctsai12/workspace/wikidata/ug.items";

        Gson gson = new Gson();
        FreeBaseQuery.loadDB(true);
        Map<String, Set<String>> gaz = new HashMap<>();
        gaz.put("per", new HashSet<>());
        gaz.put("org", new HashSet<>());
        gaz.put("loc", new HashSet<>());
        Map<String, Set<String>> pair = new HashMap<>();
        pair.put("per", new HashSet<>());
        pair.put("org", new HashSet<>());
        pair.put("loc", new HashSet<>());
        try {
            for(String line: LineIO.read(cache)){
                Output item = gson.fromJson(line, Output.class);
                if(item.mid!=null){

                    String mid = item.mid.substring(1).replaceAll("/", ".");
                    List<String> types = FreeBaseQuery.getTypesFromMid(mid);
                    String netype = "";
                    if (types.contains("people.person")) netype = "per";
                    else if (types.contains("location.location")) netype = "loc";
                    else if (types.contains("organization.organization")) netype = "org";
                    if(netype.isEmpty()) continue;

                    if(item.ug_name!=null){
                        gaz.get(netype).add(item.ug_name);
                        if(item.en_name!=null)
                            pair.get(netype).add(item.ug_name+"\t"+item.en_name);
                    }

                    if(item.ug_wiki!=null){
                        gaz.get(netype).add(item.ug_wiki);
                        if(item.en_wiki!=null)
                            pair.get(netype).add(item.ug_wiki+"\t"+item.en_wiki);
                    }

                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String dir = "/shared/corpora/ner/gazetteers/ug/";
        try {
            for(String type: gaz.keySet())
                FileUtils.writeStringToFile(new File(dir, type+".wikidata.gaz"), gaz.get(type).stream().collect(joining("\n")), "UTF-8");
            for(String type: pair.keySet())
                FileUtils.writeStringToFile(new File(dir, type+".wikidata.pair"), pair.get(type).stream().collect(joining("\n")), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {
        GazetteerGenerator gg = new GazetteerGenerator();
//        GazetteerGenerator.getOneHopPairs("en","es");
//        gg.run("yo");
        gg.genTitlePairs("ta");
//        GazetteerGenerator.getWordMapping(args[0], args[1]);
//        GazetteerGenerator.genGazFromWikidata();
//        GazetteerGenerator.getExternalGazetteers();
//        GazetteerGenerator.filterMap();
    }
}
