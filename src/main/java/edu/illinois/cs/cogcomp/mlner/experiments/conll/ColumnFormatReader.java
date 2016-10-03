package edu.illinois.cs.cogcomp.mlner.experiments.conll;

import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 3/2/16.
 */
public class ColumnFormatReader {

    public List<TextAnnotation> tas;
    public List<QueryDocument> docs;
    public List<List<String>> wikifier_features;
    private static Logger logger = LoggerFactory.getLogger(ColumnFormatReader.class);
    private String mention = null, type = null;
    private int mention_start = -1, mention_end = -1;
    private int mention_xml_start = -1, mention_xml_end = -1;

    public ColumnFormatReader(){

    }

    public void loadDryRun(String set){

        if(set.equals("0")){
            readDir("/shared/corpora/ner/dryrun/set0/mono", false);
        }
        else if(set.equals("E")){
            readDir("/shared/corpora/ner/dryrun/setE-new", false);
        }
        else{
            System.out.println("Wrong set name in dry run: "+set);
            System.exit(-1);
        }
    }


    public void loadTacTrain(String lang){
        if(lang.equals("zh"))
            readDir("/shared/corpora/ner/tac/zh/train2-4types", true);
        if(lang.equals("es"))
            readDir("/shared/corpora/ner/tac/es/train-4types", true);
        if(lang.equals("en"))
            readDir("/shared/corpora/ner/tac/en/train-4types", true);
    }

    public void loadTacTest(String lang){
        if(lang.equals("zh"))
            readDir("/shared/corpora/ner/tac/zh/test2-4types", true);
        if(lang.equals("es"))
            readDir("/shared/corpora/ner/tac/es/test-4types", true);
        if(lang.equals("en"))
            readDir("/shared/corpora/ner/tac/en/test-4types", true);

    }



    public void loadTrain(String lang){
        if(lang.equals("es"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/es/esp.train.proc","iso-8859-1");
        else if(lang.equals("nl"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/nl/ned.train.proc","iso-8859-1");
        else if(lang.equals("de"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/de/deu.train.proc","iso-8859-1");
        else if(lang.equals("en"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/en/eng.train.proc","iso-8859-1");
//            else if(lang.equals("tr"))
//                readDir("/shared/corpora/ner/lorelei/tr/Train", true);
//            else if(lang.equals("uz"))
//                readDir("/shared/corpora/ner/lorelei/uz/Train", true);
//            else if(lang.equals("bn"))
//                readDir("/shared/corpora/ner/lorelei/bn/Train", true);
//            else if(lang.equals("ha"))
//                readDir("/shared/corpora/ner/lorelei/ha/Train", true);
//            else if(lang.equals("ar"))
//                readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/ar/ANERCorp.train.proc", "utf-8");

    }

    public void loadCoNLLSubmission(String lang, String part){
        if(part.equals("train")){
            if(lang.equals("en") || lang.equals("es") || lang.equals("nl") || lang.equals("de"))
                loadTrain(lang);
            else
                loadHengJiTrain(lang);
        }
        else{
            if(lang.equals("en") || lang.equals("es") || lang.equals("nl") || lang.equals("de"))
                loadTest(lang);
            else
                loadHengJiTest(lang);
        }
    }

    public void loadOntoNotes(String part){
        if(part.equals("traindev")){
            readDir("/shared/corpora/ner/ontonotes/ColumnFormat/TrainAndDev", false);
        }
        else if(part.equals("test")){
            readDir("/shared/corpora/ner/ontonotes/ColumnFormat/Test", false);
        }
    }

    public void loadHengJiTrain(String lang){
        String dir = "/shared/corpora/ner/hengji/";
        readDir(dir+lang+"/Train", true);
    }

    public void loadHengJiTest(String lang){
        String dir = "/shared/corpora/ner/hengji/";
        readDir(dir+lang+"/Test", true);
    }

    public void loadTest(String lang){
        if(lang.equals("es"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/es/esp.testb.proc","iso-8859-1");
        else if(lang.equals("nl"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/nl/ned.testb.proc","iso-8859-1");
        else if(lang.equals("de"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/de/deu.testb.proc","iso-8859-1");
        else if(lang.equals("en"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/en/eng.testb.proc","iso-8859-1");
//            else if(lang.equals("tr"))
//                readDir("/shared/corpora/ner/lorelei/tr/Test", true);
//            else if(lang.equals("uz"))
//                readDir("/shared/corpora/ner/lorelei/uz/Test", true);
//            else if(lang.equals("bn"))
//                readDir("/shared/corpora/ner/lorelei/bn/Test", true);
//            else if(lang.equals("ha"))
//                readDir("/shared/corpora/ner/lorelei/ha/Test", true);
//            else if(lang.equals("ar"))
//                readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/ar/ANERCorp.test.proc", "utf-8");

    }

    public void loadDev(String lang){
        if(lang.equals("es"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/es/esp.testa.proc","iso-8859-1");
        else if(lang.equals("nl"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/nl/ned.testa.proc","iso-8859-1");
        else if(lang.equals("de"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/de/deu.testa.proc","iso-8859-1");
        else if(lang.equals("en"))
            readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/en/eng.testa.proc","iso-8859-1");

    }
//    public void loadSpaTrain(){
//        System.out.println("loading Spanish training data");
//        readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/es/esp.train.proc");
//        System.out.println("Done");
//    }
//
//    public void loadDeTrain(){
//        readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/de/deu.train.proc");
//    }
//    public void loadDeDev(){
//        readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/de/deu.testa.proc");
//    }
//    public void loadDeTest(){
//        readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/de/deu.testb.proc");
//    }
//
//    public void loadSpaDev(){
//        readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/es/esp.testa.proc");
//    }
//
//    public void loadSpaTest(){
//        readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/es/esp.testb.proc");
//    }
//
//    public void loadDutTrain(){
//        readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/nl/ned.train.proc");
//    }
//    public void loadDutDev(){
//        readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/nl/ned.testa.proc");
//    }
//    public void loadDutTest(){
//        readFile("/shared/dickens/ctsai12/multilingual/data/conll2001/nl/ned.testb.proc");
//    }

    private TextAnnotation createTextAnnotation(String text, List<IntPair> offsets, List<String> surfaces, List<Integer> sen_ends, String id){

        IntPair[] offs = new IntPair[offsets.size()];
        offs = offsets.toArray(offs);
        String[] surfs = new String[surfaces.size()];
        surfs = surfaces.toArray(surfs);
        int[] ends = new int[sen_ends.size()];
        for(int i = 0; i < sen_ends.size(); i++)
            ends[i] = sen_ends.get(i);

        if(ends[ends.length-1]!=surfaces.size()) {
            System.out.println(ends[ends.length - 1]);
            System.out.println(surfaces.size());
            System.exit(-1);
        }

        TextAnnotation ta = new TextAnnotation("", id, text, offs,
                surfs, ends);
        return ta;

    }

    private void resetVars(){
        mention = null;
        type = null;
        mention_start = -1;
        mention_end = -1;
        mention_xml_start = -1;
        mention_xml_end = -1;
    }

    private ELMention createMention(String docid){
        ELMention m = new ELMention(docid, mention_start, mention_end);
        m.setMention(mention);
        m.setType(type);
        m.xml_start = mention_xml_start; // just use plain start to record xml start...
        m.xml_end = mention_xml_end;
        resetVars();
        return m;
    }

    public QueryDocument readFile(File f){
        String docid = f.getName();
        List<String> features = new ArrayList<>();
        List<IntPair> offsets = new ArrayList<>();
        List<String> surfaces = new ArrayList<>();
        List<Integer> sen_ends = new ArrayList<>();
        String text = "";
        List<ELMention> mentions = new ArrayList<>();
        List<String> lines = null;
        resetVars();
        try {
            lines = LineIO.read(f.getAbsolutePath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        for(String line: lines){
            if(line.contains("-DOCSTART-")) continue;
            if(line.trim().isEmpty()){
                if(surfaces.size()>0 && (sen_ends.size() == 0 || surfaces.size()!= sen_ends.get(sen_ends.size()-1)))
                    sen_ends.add(surfaces.size());
                if(mention != null && mention_start!=-1 && mention_end!=-1){
                    mentions.add(createMention(docid));
                }
            }
            else {
                String[] tokens = line.split("\t");
                if(tokens.length < 6) continue;
                String surface = tokens[5];
                String tag = tokens[0];
                surfaces.add(surface);
                IntPair off = new IntPair(text.length(), text.length() + surface.length());
                offsets.add(off);
                text += surface+" ";
                String fs = "";
                for(int i = 10; i < tokens.length; i++)
                    fs += tokens[i]+"\t";
                fs = fs.trim();
                features.add(fs);

                int xml_start = -1, xml_end = -1;
                if(!tokens[1].equals("x"))
                    xml_start = Integer.parseInt(tokens[1]);
                if(!tokens[2].equals("x"))
                    xml_end = Integer.parseInt(tokens[2]);

                if(tag.contains("-")){
                    String[] tags = tag.split("-");
                    if(tags[0].equals("B")){
                        if(mention != null && mention_start!=-1 && mention_end!=-1){
                            mentions.add(createMention(docid));
                        }
                        mention = surfaces.get(surfaces.size()-1);
                        type = tags[1];
                        mention_start = off.getFirst();
                        mention_end = off.getSecond();
                        mention_xml_start = xml_start;
                        mention_xml_end = xml_end;
                    }
                    else if(tags[0].equals("I") && mention != null){
                        mention += " "+surfaces.get(surfaces.size()-1);
                        mention_end = off.getSecond();
                        mention_xml_end = xml_end;
                    }
                }
                else{
                    if(mention != null && mention_start!=-1 && mention_end!=-1){
                        mentions.add(createMention(docid));
                    }
                }
            }
        }
        if(mention != null && mention_start!=-1 && mention_end!=-1){
            mentions.add(createMention(docid));
        }
        if(sen_ends.size() == 0 || sen_ends.get(sen_ends.size()-1) != surfaces.size())
            sen_ends.add(surfaces.size());
        QueryDocument doc = new QueryDocument(f.getName());
        TextAnnotation ta;
        try {
            ta = createTextAnnotation(text, offsets, surfaces, sen_ends, docid);
        } catch (Exception e) {
            logger.info("bad");
            logger.info(doc.getDocID());
            return null;
        }
        doc.plain_text = text.trim();
        doc.mentions = mentions;
        doc.golds = mentions;
        doc.setTextAnnotation(ta);
        if (ta.getTokens().length != features.size()) {
            logger.info("# wikifier features doesn't match # tokens!");
            System.exit(-1);
        }
//        wikifier_features.add(features);
        doc.wikifier_features = features;
        return doc;
    }


    public void readDir(String dir, boolean skip_nomen) {
//        List<TextAnnotation> tas = new ArrayList<>();
        List<QueryDocument> docs = new ArrayList<>();
        List<List<String>> wikifier_features = new ArrayList<>();
        File folder = new File(dir);
        int cnt = 0;
        for(File f: folder.listFiles()){
//            logger.info(cnt+" Reading "+f.getAbsolutePath());
            cnt++;
            QueryDocument doc = readFile(f);
            if(!skip_nomen || doc.mentions.size()>0) {
                docs.add(doc);
            }
        }
        this.docs = docs;
        logger.info("#docs:"+docs.size());
        logger.info("#nes:"+docs.stream().flatMap(x -> x.mentions.stream()).count());
    }

    /**
     * This is for Conll 2002 data, which is different from the widely used column format
     * @param file
     * @param encode
     */
    private void readFile(String file, String encode){
        String[] ts = file.split("/");
        String filename = ts[ts.length-1];
        List<IntPair> offsets = new ArrayList<>();
        List<String> surfaces = new ArrayList<>();
        List<Integer> sen_ends = new ArrayList<>();
        String text = "";
        List<ELMention> mentions = new ArrayList<>();
        List<TextAnnotation> tas = new ArrayList<>();
        List<QueryDocument> docs = new ArrayList<>();
        try {

            String mention = null, type = null;
            int mention_start = -1, mention_end = -1;
            for(String line: LineIO.read(file, encode)){
                if(line.contains("-DOCSTART-")){
                    if(mentions.size()>0) {
                        String id = filename + "-" + docs.size();
                        QueryDocument doc = new QueryDocument(id);
                        TextAnnotation ta = createTextAnnotation(text, offsets, surfaces, sen_ends, id);
                        doc.plain_text = text.trim();
                        doc.mentions = mentions;
                        doc.golds = mentions;
                        doc.setTextAnnotation(ta);

                        text = "";
                        offsets = new ArrayList<>();
                        surfaces = new ArrayList<>();
                        sen_ends = new ArrayList<>();
                        mentions = new ArrayList<>();

                        docs.add(doc);
                        tas.add(ta);
                    }

                }
                else if(line.trim().isEmpty()){
                    if(surfaces.size()>0)
                        sen_ends.add(surfaces.size());
                    if(mention != null && mention_start!=-1 && mention_end!=-1){
                        ELMention m = new ELMention(filename+"-"+docs.size(), mention_start, mention_end);
                        m.setMention(mention);
                        m.setType(type);
                        mentions.add(m);
                        mention = null;
                        mention_start = -1;
                        mention_end = -1;
                    }
                }
                else {
                    String[] tokens = line.split(" ");
                    surfaces.add(tokens[0]);
                    IntPair off = new IntPair(text.length(), text.length() + tokens[0].length());
                    offsets.add(off);
                    text += tokens[0]+" ";

//                    System.out.println(line);
                    if(tokens[1].contains("-")){
                        String[] tags = tokens[1].split("-");
                        if(tags[0].equals("B")){
                            if(mention != null && mention_start!=-1 && mention_end!=-1){
                                ELMention m = new ELMention(filename+"-"+docs.size(), mention_start, mention_end);
                                m.setMention(mention);
                                m.setType(type);
                                mentions.add(m);
                                mention = null;
                                mention_start = -1;
                                mention_end = -1;
                            }
                            mention = surfaces.get(surfaces.size()-1);
                            type = tags[1];
                            mention_start = off.getFirst();
                            mention_end = off.getSecond();
                        }
                        else if(tags[0].equals("I")){
                            mention += " "+surfaces.get(surfaces.size()-1);
                            mention_end = off.getSecond();
                        }
                    }
                    else{
                        if(mention != null && mention_start!=-1 && mention_end!=-1){
                            ELMention m = new ELMention(filename+"-"+docs.size(), mention_start, mention_end);
                            m.setMention(mention);
                            m.setType(type);
                            mentions.add(m);
                            mention = null;
                            mention_start = -1;
                            mention_end = -1;
                        }
                    }
                }
            }
            if(mention != null && mention_start!=-1 && mention_end!=-1){
                ELMention m = new ELMention(filename+"-"+docs.size(), mention_start, mention_end);
                m.setMention(mention);
                m.setType(type);
                mentions.add(m);
            }
//            sen_ends.add(surfaces.size());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String id = filename+"-"+docs.size();
        QueryDocument doc = new QueryDocument(id);
        TextAnnotation ta = createTextAnnotation(text, offsets, surfaces, sen_ends, id);
        doc.plain_text = text.trim();
        doc.mentions = mentions;
        doc.golds = mentions;
        doc.setTextAnnotation(ta);
        tas.add(ta);
        docs.add(doc);

        this.tas = tas;
        this.docs = docs;

        System.out.println("#docs:"+docs.size());
        System.out.println("#nes:"+docs.stream().flatMap(x -> x.mentions.stream()).count());
    }

    public void checkGolds(){

        docs = docs.subList(0,1);
        Map<Integer, Long> m = docs.stream().flatMap(x -> x.mentions.stream())
                .map(x -> x.getMention().split("\\s+").length)
                .collect(groupingBy(x -> x, counting()));
        System.out.println(m);
        System.out.println(docs.stream().flatMap(x -> x.mentions.stream())
                .filter(x -> x.getMention().split("\\s+").length > 2)
                .map(x -> x.getMention()+" "+x.getStartOffset()+" "+x.getType()+" "+x.getDocID())
                .collect(toList()));
    }



    public static void main(String[] args) {
        ColumnFormatReader r = new ColumnFormatReader();
        r.loadTacTest("zh");
        r.loadTacTrain("zh");
//        r.loadMono("ha");
//        r.loadHengJiTrain("tr");
//        r.loadHengJiTrain("tl");
//        r.loadHengJiTrain("bn");
//        r.loadHengJiTrain("ta");
//        r.loadHengJiTrain("yo");
//        r.loadHengJiTest("tr");
//        r.loadHengJiTest("tl");
//        r.loadHengJiTest("bn");
//        r.loadHengJiTest("ta");
//        r.loadHengJiTest("yo");
    }
}
