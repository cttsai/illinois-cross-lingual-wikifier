package edu.illinois.cs.cogcomp.xlwikifier.experiments.xlel21;

import edu.illinois.cs.cogcomp.core.io.LineIO;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 1/26/16.
 */
public class TransLookUp {

    private Map<String, String> foreign2en;
    private Map<String, String> wordmap;

    public TransLookUp(){

    }

    public TransLookUp(String lang){
//        load(lang);
        loadnew(lang);
    }

    public void loadnew(String lang){
        foreign2en = new HashMap<>();
        wordmap = new HashMap<>();

//        String file = "/shared/corpora/transliteration/wikidata/wikidata."+lang;
//        String file = "/shared/corpora/transliteration/wikipedia-ct/"+lang+"/all."+lang;
        String file = "/shared/corpora/ner/gazetteers/"+lang+"/all."+lang;
        List<String> lines = null;
        try {
            lines = LineIO.read(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for(String line: lines){
            String[] parts = line.split("\t");
            foreign2en.put(parts[0], parts[1]);
        }

        file = "/shared/corpora/ner/gazetteers/"+lang+"/wordmap."+lang+"-en.2";
        try {
            lines = LineIO.read(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for(String line: lines){
            String[] parts = line.split("\t");
            if(!wordmap.containsKey(parts[0]))
                wordmap.put(parts[0], parts[1]);
        }
    }

    public void load(String lang){
//        if(lang.equals("tr"))
//            lang = "Turkish";
//        else if(lang.equals("el"))
//            lang = "Greek";

        foreign2en = new HashMap<>();
        wordmap = new HashMap<>();

//        String file = "/shared/corpora/transliteration/wikidata/wikidata."+lang;
        String file = "/shared/corpora/transliteration/from_anne_irvine/irvine-data."+lang;
        List<String> lines = null;
        try {
            lines = LineIO.read(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        Map<String, Map<String, Integer>> word2wordcnt = new HashMap<>();

        for(String line: lines){
            if(line.startsWith("#")) continue;
            String[] parts = line.split("\t");
            if(parts.length < 2) continue;

            int idx = parts[0].indexOf("(");
            if(idx > 0)
                parts[0] = parts[0].substring(0, idx).trim();
            idx = parts[1].indexOf("(");
            if(idx > 0)
                parts[1] = parts[1].substring(0, idx).trim();
            foreign2en.put(parts[0].toLowerCase(), parts[1].toLowerCase());

            String[] tokens1 = parts[0].toLowerCase().split("\\s+");
            String[] tokens2 = parts[1].toLowerCase().split("\\s+");
            if(tokens1.length != tokens2.length) continue;
            for(int i = 0; i < tokens1.length; i++){
                if(!word2wordcnt.containsKey(tokens1[i]))
                    word2wordcnt.put(tokens1[i], new HashMap<>());
                Map<String, Integer> wordcnt = word2wordcnt.get(tokens1[i]);
                if(!wordcnt.containsKey(tokens2[i]))
                    wordcnt.put(tokens2[i], 1);
                else
                    wordcnt.put(tokens2[i], wordcnt.get(tokens2[i])+1);
            }
        }

        for(String fword: word2wordcnt.keySet()){
            List<String> sorted = word2wordcnt.get(fword).entrySet().stream()
                    .sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue()))
                    .map(x -> x.getKey()).collect(toList());
            wordmap.put(fword, sorted.get(0));
        }

        System.out.println("Loaded "+foreign2en.size()+" transliterations");
        System.out.println("Loaded "+wordmap.size()+" word transliterations");
    }

    public String LookupTrans(String str){
        str = str.toLowerCase();
        if(foreign2en.containsKey(str))
            return foreign2en.get(str);

        String[] tokens = str.split("\\s+");
        String ret = "";
        for(String token: tokens){
            if(wordmap.containsKey(token)) {
                ret += wordmap.get(token) + " ";
            }
            else return null;
        }

        if(!ret.trim().isEmpty())
            return ret.trim();

        return null;
    }

}
