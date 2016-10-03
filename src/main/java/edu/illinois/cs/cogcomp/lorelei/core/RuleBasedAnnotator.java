package edu.illinois.cs.cogcomp.lorelei.core;

import edu.illinois.cs.cogcomp.core.datastructures.ViewNames;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.SpanLabelView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.core.utilities.StringUtils;
import edu.illinois.cs.cogcomp.lorelei.utils.TextAnnotation2Conll;
import edu.illinois.cs.cogcomp.mlner.experiments.conll.ColumnFormatReader;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.lorelei.utils.Conll2TextAnnotation;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 7/10/16.
 */
public class RuleBasedAnnotator {
    private static Logger logger = LoggerFactory.getLogger(RuleBasedAnnotator.class);

    private Map<String, String> namelist = new HashMap<>();
    private Map<String, Set<String>> chinesenames;
    public Set<String> blacklist = new HashSet<>();

    public Map<String, Map<String, Integer>> suf_cnt = new HashMap<>();

    public Map<String, Set<String>> suffix;

    public String sufdir ="/shared/corpora/ner/gazetteers/ug/suffix/" ;

    public static Set<String> stopwords = new HashSet<>();
    public static Set<String> puncs = new HashSet<>();

    public boolean usesuffix = true;

    public static List<String> entitytypes = Arrays.asList("GPE", "LOC", "PER", "ORG");

    private DesignatorAnnotator de_annotator;

    public RuleBasedAnnotator(){
        loadGazetteers();
        loadStopWords();
        loadSuffix();

        de_annotator = new DesignatorAnnotator();
        de_annotator.rule_annotator = this;
    }

    public static List<String> loadList(String file){
        try {
            return LineIO.read(file).stream().map(x-> x.trim().toLowerCase()).collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public static List<String> loadTabList(String file, int col){
        try {
            return LineIO.read(file).stream().map(x-> x.trim().toLowerCase().split("\t")[col])
                    .filter(x -> !x.trim().isEmpty()).collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void loadStopWords(){
        logger.info("Loading stopwords...");
        stopwords.addAll(loadList("/shared/corpora/ner/gazetteers/ug/designators/stopwords"));
        stopwords.addAll(loadList("/shared/experiments/ctsai12/workspace/xlwikifier/stopwords/puncs"));
        puncs.addAll(loadList("/shared/experiments/ctsai12/workspace/xlwikifier/stopwords/puncs"));
        stopwords = stopwords.stream().filter(x -> !x.trim().isEmpty()).collect(Collectors.toSet());
        puncs = puncs.stream().filter(x -> !x.trim().isEmpty()).collect(Collectors.toSet());
    }

    public void setGazetteers(Map<String, String> gaz){
        logger.info("Setting gazetteers...");
        namelist = gaz;
    }

    public  void addGazetteers(Map<String, String> gaz){
        for(String word: gaz.keySet()){
            if(namelist.containsKey(word) && !namelist.get(word).equals(gaz.get(word))){
                System.out.println(word+" "+namelist.get(word)+" "+gaz.get(word));
            }
            namelist.put(word, gaz.get(word));
        }
    }

    public void loadGazetteers(){
        logger.info("Loading new gazetteers");

        String dir = "/shared/corpora/ner/gazetteers/ug/cleaned_gazetteers/";
        for(String type: entitytypes){
            List<String> names = loadList(dir + type).stream()
                    .filter(x -> !x.trim().isEmpty())
                    .collect(Collectors.toList());
            names.forEach(x -> namelist.put(x, type));
        }

        blacklist.addAll(loadList(dir+"blacklist"));


        Map<String, String> newnames = new HashMap<>();
        for(String name: namelist.keySet()){
            if(namelist.get(name).equals("GPE")){
                String[] tokens = name.split("\\s+");
                if(tokens.length>1 && tokens[tokens.length-1].startsWith("shehir")){
                    String nn = "";
                    for(int i = 0; i < tokens.length-1; i++)
                        nn += tokens[i]+" ";
                    nn += "sheher";
                    newnames.put(nn.trim(), "GPE");
                }
            }
        }

        printGazetteerSize();
    }

    public void setSuffixDir(String dir){
        sufdir = dir;
        loadSuffix();
    }

    private void loadSuffix(){
        suffix = new HashMap<>();

        for(String type: entitytypes) {
            String f = sufdir + type + ".suffix";
            if (!new File(f).exists()) {
                logger.info("no suffix file for " + type);
                suffix.put(type, new HashSet<>());
            }
            else {

                Set<String> suffs = null;
                try {
                    suffs = LineIO.read(f).stream()
                            .map(x -> x.split("\t"))
                            .filter(x -> Integer.parseInt(x[1]) > 2)
                            .map(x -> x[0])
                            .collect(Collectors.toSet());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

//                suffix.put(type, new HashSet<>(loadTabList(f, 0).subList(0, top)));
                suffix.put(type, suffs);
                logger.info("Loaded " + type + " suffix " + suffix.get(type).size());
            }
        }
    }

    public boolean inStopWords(String str){
        if(str.isEmpty()) return  true;

        if(StringUtils.isNumeric(str)) return true;

        if(RuleBasedAnnotator.puncs.contains(str.substring(0,1)))
            return true;

        str = str.toLowerCase();
        if(RuleBasedAnnotator.stopwords.contains(str))
            return true;

        for(String stop: RuleBasedAnnotator.stopwords){
            if(str.startsWith(stop) && str.length() - stop.length() < 5) {
                return true;
            }
        }
        return false;
    }

    public Map<String, String> getChineseNames(){
        String infile = "/shared/corpora/ner/gazetteers/ug/zh.per";
        Map<String, Set<String>> gaz = new HashMap<>();
        gaz.put("PER", new HashSet<>(loadList(infile)));
        filterListByStopwords(gaz);

        Map<String, String> namelist = new HashMap<>();
        Set<String> badlast = new HashSet<>();
        badlast.add("say");
        badlast.add("mo");
        badlast.add("men");
        badlast.add("gep");
        for(String name: gaz.get("PER")){
            if(badlast.contains(name.split("\\s+")[0]))
                continue;
            namelist.put(name,"PER");
        }

        return namelist;
    }

    /**
     * Combine various gazetteers
     */
    public void cleanAndCombine(){
        logger.info("loading gazetteers");

        String pair_dir = "/shared/corpora/ner/gazetteers/ug/cleaned_pair";
        for(File f: new File(pair_dir).listFiles()) {
            String type = f.getName();
            List<String> names = loadTabList(f.getAbsolutePath(), 0);
            addNames(names, type);
        }
        printGazetteerSize();

        addNames(loadList("/shared/corpora/ner/gazetteers/ug/designators/country"), "GPE");
        addNames(loadList("/shared/corpora/ner/gazetteers/ug/designators/organization"), "ORG");
        addNames(loadList("/shared/corpora/ner/gazetteers/ug/designators/location"), "LOC");
        addNames(loadList("/shared/corpora/ner/gazetteers/ug/designators/person"), "PER");
//        printGazetteerSize();



        List<String> lines = loadList("/shared/corpora/ner/gazetteers/ug/cleaned_correction/new");
        for(String line: lines){
            String[] parts = line.trim().split("\\|\\|\\|");
            if(parts.length != 2) {
                System.out.println(line);
                System.out.println(parts.length);
            }
        }

        Map<String, List<String[]>> type2lines = lines.stream().map(x -> x.trim().split("\\|\\|\\|")).collect(groupingBy(x -> x[1]));
        for(String type: type2lines.keySet()){
            List<String> names = type2lines.get(type).stream().map(x -> x[0]).collect(Collectors.toList());
            addNames(names, type.toUpperCase());
        }
//        printGazetteerSize();

        for(String type: entitytypes){
            lines = loadList("/shared/corpora/ner/gazetteers/ug/cleaned_correction/"+type+".correct");
            for(String line: lines) {
                String[] parts = line.trim().split("\\|\\|\\|");
                if (parts.length != 2) {
                    System.out.println(line);
                    System.out.println(parts.length);
                }
                String surface = parts[0];

                // check if there is inconsistency
                if(namelist.containsKey(surface) && !namelist.get(surface).equals(type))
                    System.out.println(surface+" "+type+" "+namelist.get(surface));

                // add corrections into namelist
                if(!namelist.containsKey(surface)) {
                    namelist.put(surface, type);
                }
            }
        }

        lines = loadList("/shared/corpora/ner/gazetteers/ug/cleaned_correction/O.correct");
        for(String line: lines) {
            String[] parts = line.trim().split("\\|\\|\\|");
            if (parts.length != 2) {
                System.out.println(line);
                System.out.println(parts.length);
            }
            String surface = parts[0];

            // check if there is inconsistency
            if(namelist.containsKey(surface))
                System.out.println(surface+" "+namelist.get(surface));

            blacklist.add(surface);
        }

        // check if there is name with multiple types
        Set<Map.Entry<String, Long>> conflicts = namelist.entrySet().stream().collect(groupingBy(x -> x.getKey(), counting()))
                .entrySet().stream().filter(x -> x.getValue() > 1).collect(Collectors.toSet());
        if(conflicts.size()>0)
            System.out.println(conflicts);

        printGazetteerSize();

        Map<String, List<Map.Entry<String, String>>> type2list = namelist.entrySet().stream().collect(groupingBy(x -> x.getValue()));

        try {
            for(String type: type2list.keySet()){
                Set<String> names = type2list.get(type).stream().map(x -> x.getKey()).collect(Collectors.toSet());
                FileUtils.writeStringToFile(new File("/shared/corpora/ner/gazetteers/ug/cleaned_gazetteers", type),
                        names.stream().collect(joining("\n")), "UTF-8");
            }

            FileUtils.writeStringToFile(new File("/shared/corpora/ner/gazetteers/ug/cleaned_gazetteers", "blacklist"),
                    blacklist.stream().collect(joining("\n")), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

//        System.out.println(namelist.entrySet().stream().filter(x -> x.getKey().endsWith("liq") || x.getKey().endsWith("lik"))
//                .collect(Collectors.toSet()));
//        System.out.println(namelist.get("shinjang"));
//        System.out.println(namelist.get("uyghur"));
    }

    public void printSuffix(String dir){
        for(String type: suf_cnt.keySet()){
            String out = suf_cnt.get(type).entrySet().stream()
                    .filter(x -> !x.getKey().trim().isEmpty())
                    .sorted((x1, x2) -> Integer.compare(x2.getValue(), x1.getValue()))
                    .map(x -> x.getKey()+"\t"+x.getValue()).collect(joining("\n"));
            try {
                FileUtils.writeStringToFile(new File(dir, type+".suffix"), out, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Set<String> getGazetteers(String type){
        return namelist.entrySet().stream().filter(x -> x.getValue().equals(type))
                .map(x -> x.getKey()).collect(Collectors.toSet());
    }

    private void addNames(List<String> names, String type){
        for(String name: names){
            if(namelist.containsKey(name) && !namelist.get(name).equals(type))
                logger.warn(name+" "+namelist.get(name)+" "+type);
            else
                namelist.put(name, type);
        }
    }

    public void printGazetteerSize(){
        Map<String, Long> type2cnt = namelist.entrySet().stream().collect(groupingBy(x -> x.getValue(), counting()));
        logger.info("Gazetteer size: ");
        for(String type: type2cnt.keySet())
            logger.info(type+" "+type2cnt.get(type));
        logger.info("Blacklist size: "+blacklist.size());
    }

    public void filterListByStopwords(Map<String, Set<String>> gaz){

        for(String type: gaz.keySet()) {
            Set<String> list = gaz.get(type);
            Set<String> ret = new HashSet<>();

            for (String item : list) {
                item = item.toLowerCase();

                if(blacklist.contains(item)) continue;

                String[] parts = item.split("\\s+");
                boolean bad = false;
                for (String part : parts) {
                    if (stopwords.contains(part) || blacklist.contains(item)){
                        bad = true;
                        break;
                    }
                }
                if(bad) continue;

                ret.add(item);
            }
            gaz.put(type, ret);
        }
    }

    /**
     * Add a new constituent into the NER view
     * @param ner_view
     * @param start
     * @param end
     * @param type
     * @return
     */
    public boolean addConstituent(SpanLabelView ner_view, int start, int end, String type, int mode){
        TextAnnotation ta = ner_view.getTextAnnotation();
        int length = ta.getTokenCharacterOffset(end).getSecond() - ta.getTokenCharacterOffset(start).getFirst();
        List<Constituent> overlaps = ner_view.getConstituentsCoveringSpan(start, end+1);
        if(mode == 0) {
            if (overlaps.size() == 0) { // Doesn't overlap with any existing annotations
                Constituent c = new Constituent(type, ViewNames.NER_CONLL, ta, start, end + 1);
                ner_view.addConstituent(c);
                return true;
            }
        } else if (mode == 1){ // remove overlap constituents
            boolean exist = false;
            for(Constituent con: overlaps){
                if((con.getStartSpan() == start && con.getEndSpan() == end+1) ||
                        (con.getSurfaceForm().length() > length && type.equals(con.getLabel()))) {
                    exist = true;
                }
                else {
                    logger.info("remove: "+con.getSurfaceForm());
                    ner_view.removeConstituent(con);
                }
            }

            if(!exist) {  // not already tagged
                Constituent c = new Constituent(type, ViewNames.NER_CONLL, ner_view.getTextAnnotation(), start, end + 1);
                ner_view.addConstituent(c);
                return true;
            }
        } else if (mode == 2){ // merge with the overlapped mention before
            for(Constituent con: overlaps){
                int s = con.getStartSpan();
                int e = con.getEndSpan();
                if(s < start && e <= end + 1){
                    start = s;
                }
                logger.info("remove: "+con.getSurfaceForm()+" "+con.getLabel());
                ner_view.removeConstituent(con);
            }

            Constituent c = new Constituent(type, ViewNames.NER_CONLL, ner_view.getTextAnnotation(), start, end + 1);
            ner_view.addConstituent(c);
            return true;
        } else if (mode == 3){ // overwrite if the existing one is shorter

            boolean over = true;
            for(Constituent con: overlaps){
                if(con.getSurfaceForm().length() >= length)
                    over = false;
            }

            if(over){
                for(Constituent cons: overlaps){
                    logger.info("remove "+cons.getSurfaceForm());
                    ner_view.removeConstituent(cons);
                }
                Constituent c = new Constituent(type, ViewNames.NER_CONLL, ner_view.getTextAnnotation(), start, end + 1);
                ner_view.addConstituent(c);
                return true;
            }
        }

        return false;
    }

    /**
     * @param doc
     */
    public int annotate(QueryDocument doc, Map<String, Set<String>> gazetteers, int mode, boolean usesuffix){


        TextAnnotation ta = doc.getTextAnnotation();
        if(!ta.hasView(ViewNames.NER_CONLL))
            Conll2TextAnnotation.populatNERView(doc);

        String text = ta.getText();

        SpanLabelView ner_view = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);

        int newann = 0;
        for(String type: gazetteers.keySet()){
            for(String name: gazetteers.get(type)){
                String suff = "";
                int idx = -name.length();
                while(true) {
//                    idx = text.indexOf(name, idx+name.length()+suff.length());
                    idx = org.apache.commons.lang3.StringUtils.indexOfIgnoreCase(text, name, idx+name.length()+suff.length());
                    if(idx < 0) break;

                    // no space before
                    if (idx > 0 && !text.substring(idx - 1, idx).trim().isEmpty()) continue;

                    if(!usesuffix) {
                        // if no space after
                        if (idx + name.length() < text.length() - 1
                                && !text.substring(idx + name.length(), idx + name.length() + 1).trim().isEmpty())
                            continue;
                    }

                    int start = ta.getTokenIdFromCharacterOffset(idx);

                    int end = ta.getTokenIdFromCharacterOffset(idx + name.length()-1);

                    String surface = text.substring(ta.getTokenCharacterOffset(start).getFirst(), ta.getTokenCharacterOffset(end).getSecond());

                    if(stopwords.contains(surface)) continue;


                    suff = surface.substring(name.length(), surface.length());

                    if(usesuffix && name.length() < 5 && suff.length()>0) continue;


                    // suffix is not in the suffix list
                    if(usesuffix && type.equals("PER") && !suff.isEmpty() && !suffix.get(type).contains(suff)) continue;
//                    if(usesuffix && !suff.isEmpty() && !suffix.get(type).contains(suff)) continue;

                    if(blacklist.contains(surface)){
                        logger.info("matched blacklist "+surface+" "+type+" "+name);
                        continue;
                    }

                    // counting suffixes
//                    if(!suf_cnt.containsKey(type))
//                        suf_cnt.put(type, new HashMap<>());
//
//                    Map<String, Integer> cntmap = suf_cnt.get(type);
//                    if(!cntmap.containsKey(suff))
//                        cntmap.put(suff, 1);
//                    else
//                        cntmap.put(suff, cntmap.get(suff)+1);

//                    String first = ta.getToken(start);
//                    lastnames.add(first);

                    boolean added = addConstituent(ner_view, start, end, type, mode);
                    if(added){
//                        logger.info("match: "+name+" ||| "+ta.getToken(end));
                        newann++;
                    }
                }
            }
        }

        return newann;
    }

    public void annotate(String dir, String outdir){

        chinesenames = getChineseNames().entrySet().stream()
                .collect(groupingBy(x -> x.getValue(), mapping(x -> x.getKey(), toSet())));

        List<List<Map.Entry<String, String>>> lists = namelist.entrySet().stream()
                .collect(groupingBy(x -> x.getKey().split("\\s+").length))
                .entrySet().stream().sorted((x1, x2) -> Integer.compare(x2.getKey(), x1.getKey()))
                .map(x -> x.getValue())
                .collect(Collectors.toList());


        ExecutorService executor = Executors.newFixedThreadPool(20);
        ColumnFormatReader reader = new ColumnFormatReader();
        int nann = 0, cnt = 0;
        for(File f: (new File(dir)).listFiles()){
            if(cnt++%100 == 0) System.out.println(cnt);
            QueryDocument doc = reader.readFile(f);
            if(doc == null) continue;

            executor.execute(new DocumentAnnotator(doc, lists, outdir));
        }
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(nann);
    }

    public class DocumentAnnotator implements Runnable {

        private QueryDocument doc;
        private List<List<Map.Entry<String, String>>> sorted_lists;
        private String outdir;


        public DocumentAnnotator(QueryDocument doc, List<List<Map.Entry<String, String>>> lists, String outdir) {
            this.doc = doc;
            this.sorted_lists = lists;
            this.outdir = outdir;
        }

        @Override
        public void run() {
            for(List<Map.Entry<String, String>> list: sorted_lists) {
                Map<String, Set<String>> gaz = list.stream().collect(groupingBy(x -> x.getValue(), mapping(x -> x.getKey(), toSet())));
                annotate(doc, gaz, 0, usesuffix);
            }
            de_annotator.annotateByDesignators(doc.getTextAnnotation());
//            annotate(doc, chinesenames, 0, usesuffix);
            TextAnnotation2Conll.writeTaToConll(doc.getTextAnnotation(), doc, outdir);
        }
    }

    public static void getAcronym(String indir, String outdir){

        ColumnFormatReader reader = new ColumnFormatReader();
        Pattern p = Pattern.compile("((\\s|^)\\w){2,}(\\s|$)");
        for(File f: (new File(indir)).listFiles()) {
            QueryDocument doc = reader.readFile(f);
            Conll2TextAnnotation.populatNERView(doc);
            TextAnnotation ta = doc.getTextAnnotation();

            SpanLabelView nerview = (SpanLabelView) ta.getView(ViewNames.NER_CONLL);

            Matcher m = p.matcher(ta.getText());
            while(m.find()){
                int start = m.start();
                int end = m.end();
                String surface = m.group();
                if(surface.startsWith(" ")) {
                    start++;
                    surface = surface.substring(1);
                }
                if(surface.endsWith(" ")) {
                    end--;
                    surface = surface.substring(0, surface.length()-1);
                }

                int startt = ta.getTokenIdFromCharacterOffset(start);
                int endt = ta.getTokenIdFromCharacterOffset(end-1);
                if(nerview.getConstituentsCoveringSpan(startt, endt+1).size()>0) continue;

                boolean bad = false;
                for(String ch: surface.split("\\s+")){
                    if(StringUtils.isNumeric(ch)) bad = true;
                }
                if(bad) continue;

                System.out.println(f.getName()+" match "+surface);
            }
        }
    }

    public static void compairConll(String dir1, String dir2){

        ColumnFormatReader reader = new ColumnFormatReader();
        Map<String, Integer> newcnt = new HashMap<>();
        for(File f: (new File(dir1)).listFiles()) {

            QueryDocument doc1 = reader.readFile(f);
            QueryDocument doc2 = reader.readFile(new File(dir2, f.getName()));

            Conll2TextAnnotation.populatNERView(doc1);
            Conll2TextAnnotation.populatNERView(doc2);

            TextAnnotation ta1 = doc1.getTextAnnotation();
            TextAnnotation ta2 = doc2.getTextAnnotation();

            SpanLabelView nerview1 = (SpanLabelView) ta1.getView(ViewNames.NER_CONLL);
            SpanLabelView nerview2 = (SpanLabelView) ta2.getView(ViewNames.NER_CONLL);

            for(Constituent c: nerview2.getConstituents()){
                List<Constituent> overlap = nerview1.getConstituentsCoveringSpan(c.getStartSpan(), c.getEndSpan());
                if(overlap.size() > 0 && overlap.get(0).getStartSpan() == c.getStartSpan() &&
                        overlap.get(0).getEndSpan() == c.getEndSpan() && overlap.get(0).getLabel().equals(c.getLabel()))
                    continue;

                System.out.print("old: ");
                overlap.forEach(x -> System.out.print(x.getSurfaceForm()+":"+x.getLabel()+" "));

                String key = c.getSurfaceForm()+":"+c.getLabel();
                if(newcnt.containsKey(key))
                    newcnt.put(key, newcnt.get(key)+1);
                else
                    newcnt.put(key,1);

                System.out.println("new: "+c.getSurfaceForm()+":"+c.getLabel());
            }
        }
        List<Map.Entry<String, Integer>> sort = newcnt.entrySet().stream().sorted((x1, x2) -> Integer.compare(x1.getValue(), x2.getValue())).collect(Collectors.toList());
        sort.forEach(x -> System.out.println(x.getKey()+" "+x.getValue()));
        System.out.println(sort.stream().mapToInt(x -> x.getValue()).sum());
    }

    public void cleanNames() {

        Set<String> suffix = new HashSet<>();
        suffix.addAll(loadList(sufdir + "noun-adj.suffix"));
        suffix = suffix.stream().filter(x -> x.length() > 1).collect(Collectors.toSet());


        Map<String, String> newnames = new HashMap<>();
        for (String type : entitytypes) {

            for (String name : getGazetteers(type)) {

                String[] tokens = name.split("\\s+");

                String last = tokens[tokens.length - 1];

                for (String suf : suffix) {
                    if (name.endsWith(suf)) {
                        if (last.length() - suf.length() < 6) continue;
                        newnames.put(name.substring(0, name.length() - suf.length()), type);
                        break;
                    }
                }
            }
        }

        for (String name : newnames.keySet())
            namelist.put(name, newnames.get(name));
    }


    public void gpeNames() {

        Set<String> gpes = getGazetteers("GPE");

        int cnt = 0;
        Set<String> newgpes = new HashSet<>();
        for(String name: gpes){
            String[] tokens = name.split("\\s+");
            String last = tokens[tokens.length-1];
            if(tokens.length > 1){
                if(last.startsWith("shehiri") || last.startsWith("wilayiti") || last.startsWith("nahiy") || last.startsWith("ölkisi")){
                    String pre = Arrays.asList(tokens).subList(0, tokens.length - 1).stream().collect(joining(" "));
                    if(pre.length()>3)
                        newgpes.add(pre);
                }
            }
        }

        System.out.println(newgpes.size());
        System.out.println(newgpes);

        namelist.clear();

        newgpes.forEach(x -> namelist.put(x, "GPE"));

    }

    /**
     * Extract name lists from annotated documents
     */
    public void extractGazetteers() {
        ColumnFormatReader reader = new ColumnFormatReader();

        String dir = "/shared/corpora/ner/eval/column/dev2";

        Map<String, Set<String>> gaz = new HashMap<>();


        Map<String, List<String>> surf2types = new HashMap<>();

        for (File f : new File(dir).listFiles()) {
            QueryDocument doc = reader.readFile(f);
            Conll2TextAnnotation.populatNERView(doc);

            SpanLabelView ner = (SpanLabelView) doc.getTextAnnotation().getView(ViewNames.NER_CONLL);

            for (Constituent con : ner.getConstituents()) {

                String type = con.getLabel();
                String surf = con.getSurfaceForm().toLowerCase().trim();
                if (!surf2types.containsKey(surf))
                    surf2types.put(surf, new ArrayList<>());
                surf2types.get(surf).add(type);

            }
        }


        // get the most frequent type for each surface
        for (String surf : surf2types.keySet()) {
            String toptype = surf2types.get(surf).stream().collect(groupingBy(x -> x, counting()))
                    .entrySet().stream().sorted((x1, x2) -> Long.compare(x2.getValue(), x1.getValue()))
                    .collect(Collectors.toList()).get(0).getKey();

            if (!gaz.containsKey(toptype))
                gaz.put(toptype, new HashSet<>());
            gaz.get(toptype).add(surf);
        }

        String outdir = "/shared/corpora/ner/gazetteers/ug/dev2/";

        for(String type: gaz.keySet()){
            try {
                FileUtils.writeStringToFile(new File(outdir, type), gaz.get(type).stream().collect(joining("\n")), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addDev2(){

        for(String type: entitytypes){
            Set<String> names = new HashSet<>(loadList("/shared/corpora/ner/gazetteers/ug/dev2/" + type));

            System.out.println(names.size());
            names = names.stream().filter(x -> !inStopWords(x)).collect(Collectors.toSet());
            System.out.println(names.size());

            names.forEach(x -> namelist.put(x, type));
        }


    }

    public void mergePredictions(String indir1, String indir2, String outdir){

        ColumnFormatReader reader = new ColumnFormatReader();
        int doccnt = 0;
        for(File f: (new File(indir1)).listFiles()) {
            if (doccnt++ % 100 == 0) System.out.println(doccnt);
            QueryDocument doc1 = reader.readFile(f);
            TextAnnotation ta1 = doc1.getTextAnnotation();
            Conll2TextAnnotation.populatNERView(doc1);

            QueryDocument doc2 = reader.readFile(new File(indir2, f.getName()));
            TextAnnotation ta2 = doc2.getTextAnnotation();
            Conll2TextAnnotation.populatNERView(doc2);

            if(ta1.size()!=ta2.size()){
                System.out.println("Size don't match "+ta1.getId());
                System.exit(-1);
            }

            SpanLabelView nerview1 = (SpanLabelView) ta1.getView(ViewNames.NER_CONLL);
            SpanLabelView nerview2 = (SpanLabelView) ta2.getView(ViewNames.NER_CONLL);

            for(Constituent c2: nerview2.getConstituents()){

                boolean bad = false;
                for(int i = c2.getStartSpan(); i < c2.getEndSpan(); i++){
                    if(inStopWords(ta2.getToken(i)))
                        bad = true;
                }
                if(bad) continue;


                if(blacklist.contains(c2.getSurfaceForm()) || stopwords.contains(c2.getSurfaceForm()))
                    continue;
                if(c2.getLabel().equals("PER")) {
                    List<Constituent> over = nerview1.getConstituentsCoveringSpan(c2.getStartSpan(), c2.getEndSpan());
//                    if(over.size()>0){  // resolve conflicts
//                        Constituent o = over.get(0);
//                        if(!o.getLabel().equals("PER")) continue;
//                        if(c2.getSurfaceForm().length() > o.getSurfaceForm().length()) {
////                            System.out.println(o.getSurfaceForm() + "->" + c2.getSurfaceForm());
//                            over.forEach(x -> nerview1.removeConstituent(x));
//                            Constituent c = new Constituent(c2.getLabel(), ViewNames.NER_CONLL, ta1, c2.getStartSpan(), c2.getEndSpan());
//                            nerview1.addConstituent(c);
//                        }
//                    }
                    if (over.size() ==0){
//                        else{
                        Constituent c = new Constituent(c2.getLabel(), ViewNames.NER_CONLL, ta1, c2.getStartSpan(), c2.getEndSpan());
                        nerview1.addConstituent(c);
                    }

                }
                else{
                    List<Constituent> over = nerview1.getConstituentsCoveringSpan(c2.getStartSpan(), c2.getEndSpan());
                    if(over.size() == 0){
                        Constituent c = new Constituent(c2.getLabel(), ViewNames.NER_CONLL, ta1, c2.getStartSpan(), c2.getEndSpan());
                        nerview1.addConstituent(c);
                    }
//                    else{
//                        Constituent o = over.get(0);
//                        if(c2.getSurfaceForm().length() >= o.getSurfaceForm().length())
//                            o = c2;
//
//                        nerview1.getConstituentsCoveringSpan(o.getStartSpan(), o.getEndSpan())
//                                .forEach(x -> nerview1.removeConstituent(x));
//                        Constituent c = new Constituent(o.getLabel(), ViewNames.NER_CONLL, ta1, o.getStartSpan(), o.getEndSpan());
//                        nerview1.addConstituent(c);
//
//                    }
                }
            }

            TextAnnotation2Conll.writeTaToConll(ta1, doc1, outdir);
        }

    }

    public static void main(String[] args) {


//        String indir = "/shared/corpora/ner/wikifier-features/ug/eval-setE-new";
        String indir = "/shared/corpora/ner/eval/column/setE-uly_";
//        indir = "/shared/corpora/ner/eval/column/mono-all-uly";
//        indir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/mono-uly1";
//        indir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/e-zh";
        String outdir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/test1";
//        outdir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/test";
        RuleBasedAnnotator annotator = new RuleBasedAnnotator();
        DesignatorAnnotator de_annotator = new DesignatorAnnotator();
        de_annotator.rule_annotator = annotator;

//        usesuffix = false;
        annotator.addDev2();
        annotator.cleanNames();
        de_annotator.addConfidentNamesToGaz();
//        gpeNames();
//        annotator.annotate(indir, outdir);


//        String indir1 = "/shared/corpora/ner/wikifier-features/ug/cp3/final/e-gpe-tw";
        String indir1 = "/shared/corpora/ner/wikifier-features/ug/cp3/final/ensemble3-1";
        String indir2 = "/shared/corpora/ner/eval/column/mono-all-uly-anno2";
        indir2 = "/shared/corpora/ner/wikifier-features/ug/cp3/final/e-anno1";
        String out = "/shared/corpora/ner/wikifier-features/ug/cp3/final/e-anno1-ens3";
//        mergePredictions(indir1, indir2, out);


//        indir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/e-gpe";
//        outdir = "/shared/corpora/ner/wikifier-features/ug/cp3/final/e-anno";
//        compairConll(indir, outdir);

    }
}
