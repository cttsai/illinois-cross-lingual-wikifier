package edu.illinois.cs.cogcomp.lorelei.core;

import com.github.stuxuhai.jpinyin.PinyinFormat;
import com.github.stuxuhai.jpinyin.PinyinHelper;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.mlner.experiments.conll.ColumnFormatReader;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 7/14/16.
 */
public class ChineseNameMatcher {

    public RuleBasedAnnotator rule_annotator;

    public void genPinyinNames(){
//        String infile = "/shared/corpora/ner/gazetteers/ug/zh-pers";
//        String outfile = "/shared/corpora/ner/gazetteers/ug/zh-pers.pinyin";

        String infile = "/shared/corpora/ner/gazetteers/ug/loc.clean2";
        String outfile = "/shared/corpora/ner/gazetteers/ug/loc.pinyin";
        String out = "";

        try {
            for(String line: LineIO.read(infile)){
//                if(line.length()>3) continue;
                String py = PinyinHelper.convertToPinyinString(line, "", PinyinFormat.WITHOUT_TONE);
//                String[] tokens = py.split(",");
//                String last = "";
//                for(int i = 1; i < tokens.length; i++) last+=tokens[i];
//                out+=tokens[0]+" "+last+"\n";
                out += py+"\n";
            }
            FileUtils.writeStringToFile(new File(outfile), out, "UTF-8");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public void convertPinyinListToULY(){
        String mapfile = "/shared/corpora/ner/gazetteers/ug/pinyin2uly";
//        String infile = "/shared/corpora/ner/gazetteers/ug/zh.per.pinyin";
//        String outfile = "/shared/corpora/ner/gazetteers/ug/zh.per.uly1";
        String infile = "/shared/corpora/ner/gazetteers/ug/loc.pinyin";
        String outfile = "/shared/corpora/ner/gazetteers/ug/loc.uly";
        Map<String, String> mapping = new HashMap<>();
        try {
            for(String line: LineIO.read(mapfile)){
                String[] parts = line.split("\t");
                mapping.put(parts[0], parts[1].trim());
            }

            String out = "";
            for(String line: LineIO.read(infile)){
                out += convertPinyinToULY(line, mapping)+"\n";
            }
            FileUtils.writeStringToFile(new File(outfile), out, "UTF-8");
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public String convertPinyinToULY(String str, Map<String, String> mapping){
        String ret = "";
        int i = 0;
        while(i < str.length()){
            String sub = str.substring(i);
            if(sub.length()>2){
                String tri = sub.substring(0,3);
                if(mapping.containsKey(tri)){
                    ret += mapping.get(tri);
                    i+=3;
                    continue;
                }
            }

            if(sub.length()>1){
                String bi = sub.substring(0,2);
                if(mapping.containsKey(bi)){
                    ret += mapping.get(bi);
                    i+=2;
                    continue;
                }
            }

            String ch = sub.substring(0,1);
            if(mapping.containsKey(ch)){
                ret += mapping.get(ch);
                i++;
            }
            else{
                ret += ch;
                i++;
            }
        }

        return ret;
    }

//    public Map<String, Set<String>> readNames(String infile, String type){
//
//        Map<String, Set<String>> ret = new HashMap<>();
//
//        try {
//            ret.put(type, new HashSet<>(LineIO.read(infile)));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//        System.out.println("Loaded "+ret.get(type).size()+" names");
//
//        return ret;
//    }

//    public void filterLoc(){
//        String infile = "/shared/corpora/ner/gazetteers/ug/loc.uly+pinyin.long";
//        Map<String, Set<String>> gaz = readNames(infile, "GPE");
//        rule_annotator.filterListByStopwords(gaz);
//        Set<String> locs = gaz.get("GPE");
//        ColumnFormatReader reader = new ColumnFormatReader();
//        String dir = "/shared/corpora/ner/wikifier-features/ug/iter10-NW-correct-uly-zhper1";
//        Map<String, Integer> namecnt = new HashMap<>();
//        int cnt = 0;
//        for(File f: (new File(dir)).listFiles()) {
//            if(cnt++%100 == 0) System.out.println(cnt);
//            if(cnt == 2000) break;
//            QueryDocument doc = reader.readFile(f);
//            String text = doc.plain_text.toLowerCase();
//            for(String name: locs){
//                if(text.contains(" "+name+" ")){
//                    if(namecnt.containsKey(name))
//                        namecnt.put(name, namecnt.get(name)+1);
//                    else
//                        namecnt.put(name, 1);
//                }
//            }
//        }
//
//        List<Map.Entry<String, Integer>> sort = namecnt.entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue())).collect(Collectors.toList());
//
//        sort.subList(0, 200).forEach(x -> System.out.println(x.getKey()+" "+x.getValue()));
//        Set<String> remove = sort.stream().filter(x -> x.getValue()>8).map(x -> x.getKey()).collect(Collectors.toSet());
//        remove.remove("shinjang");
//        remove.remove("béyjing");
//        remove.remove("shinxua");
//        remove.remove("jungxua");
//        remove.remove("shinjangni");
//        remove.remove("xongkong");
//        remove.remove("nenjing");
//
//        locs.removeAll(remove);
//
//        locs = locs.stream().filter(x -> x.length() > 5).collect(Collectors.toSet());
//
//        try {
//            FileUtils.writeStringToFile(new File("/shared/corpora/ner/gazetteers/ug/zh.loc"), locs.stream().collect(joining("\n")), "UTF-8");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }


    public void matchNames(String dir, String outdir){
        rule_annotator.setGazetteers(rule_annotator.getChineseNames());

        rule_annotator.annotate(dir, outdir);

    }

    public static void main(String[] args) {
//        ChineseNameMatcher.genPinyinNames();
//        ChineseNameMatcher.convertPinyinListToULY();

        ChineseNameMatcher zhm = new ChineseNameMatcher();
        zhm.rule_annotator = new RuleBasedAnnotator();

//        filterLoc();

//        ChineseNameMatcher.matchNames("/shared/corpora/ner/wikifier-features/ug/iter16-uly");
//        ChineseNameMatcher.matchNames("/shared/corpora/ner/wikifier-features/ug/eval-setE-norank-swmgaz-tw-uly");

//        String indir = "/shared/corpora/ner/wikifier-features/ug/iter10-NW-correct-uly";
//        String outdir = "/shared/corpora/ner/wikifier-features/ug/iter10-NW-correct-uly-zh";
        String indir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/newE-key-uly";
//        indir = "/shared/corpora/ner/eval/column/set0-mono-NW-uly-gaz1";
        String outdir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/newE-zh-uly";
//        outdir = "/shared/corpora/ner/eval/column/set0-mono-NW-uly-zh";
        zhm.matchNames(indir, outdir);

        RuleBasedAnnotator.compairConll(indir, outdir);
    }
}
