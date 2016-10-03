package edu.illinois.cs.cogcomp.xlwikifier.experiments.reader;

import com.google.gson.Gson;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.tokenizers.ChineseTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.CharacterTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.Constants;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.AnnotatedDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 5/31/16.
 */
public class TACReader {

    public static Map<String, List<String>> readDocs(String dir){
        Map<String, List<String>> ret = new HashMap<>();
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(new File(dir).toPath());
            for(Path p: stream){
                String[] tokens = p.toString().split("/");
                String id = tokens[tokens.length-1].split("\\.")[0];
                if(id.trim().isEmpty()) continue;
                List<String> lines = LineIO.read(p.toString());
                ret.put(id, lines);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static List<QueryDocument> readEnglishDocuments(boolean train){
        String plain_path;
        String json_path;
        String xml_path;
        if(train){
            plain_path = Constants.trainingDocEngPlain;
            json_path = Constants.trainingDocEngPlainJson;
            xml_path = Constants.trainingDocEngXml;
        }
        else{
            plain_path = Constants.testDocEngPlain;
            json_path = Constants.testDocEngPlainJson;
            xml_path = Constants.testDocEngXml;

        }
        MentionReader mr = new MentionReader();
        List<ELMention> mentions = mr.readEnMentionsNE(train);
        for(ELMention m: mentions){
            if(!train) {
                m.setStartOffset(m.getStartOffset() + 39);
                m.setEndOffset(m.getEndOffset() + 39);
            }
        }
        DirectoryStream<Path> stream = null;
        List<QueryDocument> docs = new ArrayList<>();
        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer("en");
        int cnt = 0, cnt1 = 0;
        try {
            stream = Files.newDirectoryStream(new File(plain_path).toPath());
            for(Path p: stream) {
                String[] tokens = p.toString().split("/");
                String id = tokens[tokens.length - 1].split("\\.")[0];
//                System.out.println("doc id:" + id);

                String text = FileUtils.readFileToString(p.toFile(), "UTF-8");
                AnnotatedDocument adoc = readAnnotatedDocs(json_path + id);
                QueryDocument doc = new QueryDocument(adoc, text);

                String xmlfile = xml_path+id;
                if(id.contains("_DF_"))
                    xmlfile += ".df.xml";
                else if(!train)
                    xmlfile += ".nw.xml";
                else
                    xmlfile += ".xml";
                String xml_text = FileUtils.readFileToString(new File(xmlfile), "UTF-8");
                doc.setXmlText(xml_text);
                doc.setTextAnnotation(tokenizer.getTextAnnotation(doc.plain_text));
                doc.mentions = mentions.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
                for(ELMention m: doc.mentions){
                    String surface = doc.xml_text.substring(m.getStartOffset(), m.getEndOffset()+1);
                    if(!m.getMention().toLowerCase().equals(surface.toLowerCase())){
//                        System.out.println(doc.getDocID());
//                        System.out.println(surface);
//                        System.out.println(m.getMention());
                        cnt1++;
                    }
                    cnt++;
                }
                docs.add(doc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(cnt1+" "+cnt);
        return docs;
    }
    public static List<QueryDocument> readChinese2016EvalDocs(Set<String> dids) {
        List<QueryDocument> docs = new ArrayList<>();
        String dir = "/shared/corpora/corporaWeb/tac/LDC2016E63_TAC_KBP_2016_Evaluation_Source_Corpus_V1.1/data/cmn/";

        List<File> files = new ArrayList<>();

        files.addAll(Arrays.asList(new File(dir + "nw").listFiles()));
        files.addAll(Arrays.asList(new File(dir + "df").listFiles()));

//        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer("zh");
        Tokenizer tokenizer = new CharacterTokenizer();
        for (File f : files) {
            int idx = f.getName().lastIndexOf(".");
            String docid = f.getName().substring(0, idx);
            if(!dids.contains(docid)) continue;

            String xml_text = null;
            try {
                xml_text = FileUtils.readFileToString(f, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

            QueryDocument doc = new QueryDocument(docid);
            doc.setXmlText(xml_text);
            doc.setTextAnnotation(tokenizer.getTextAnnotation(doc.plain_text));
            doc.populateXmlMapping();
            docs.add(doc);
        }

        System.out.println("#docs " + docs.size() + " #chars " + docs.stream().mapToInt(x -> x.plain_text.length()).sum());
        return docs;
    }

    public static List<QueryDocument> readChinese2016EvalDocs() {
        List<QueryDocument> docs = new ArrayList<>();
        String dir = "/shared/corpora/corporaWeb/tac/LDC2016E63_TAC_KBP_2016_Evaluation_Source_Corpus_V1.1/data/cmn/";

        List<File> files = new ArrayList<>();

        files.addAll(Arrays.asList(new File(dir + "nw").listFiles()));
        files.addAll(Arrays.asList(new File(dir + "df").listFiles()));

//        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer("zh");
        Tokenizer tokenizer = new CharacterTokenizer();
        for (File f : files) {
            String xml_text = null;
            try {
                xml_text = FileUtils.readFileToString(f, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

            int idx = f.getName().lastIndexOf(".");
            String docid = f.getName().substring(0, idx);
//            if(!docid.equals("CMN_NW_001278_20130326_F000113NB")) continue;
            QueryDocument doc = new QueryDocument(docid);
            doc.setXmlText(xml_text);
            doc.setTextAnnotation(tokenizer.getTextAnnotation(doc.plain_text));
            doc.populateXmlMapping();
            docs.add(doc);
        }

        System.out.println("#docs " + docs.size() + " #chars " + docs.stream().mapToInt(x -> x.plain_text.length()).sum());
        return docs;
    }

    public static List<QueryDocument> readSpanish2016EvalDocs(Set<String> ids) {
        List<QueryDocument> docs = new ArrayList<>();
        String dir = "/shared/corpora/corporaWeb/tac/LDC2016E63_TAC_KBP_2016_Evaluation_Source_Corpus_V1.1/data/spa/";

        List<File> files = new ArrayList<>();

        files.addAll(Arrays.asList(new File(dir + "nw").listFiles()));
        files.addAll(Arrays.asList(new File(dir + "df").listFiles()));


        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer("es");
        for (File f : files) {
            int idx = f.getName().lastIndexOf(".");
            String docid = f.getName().substring(0, idx);

            if(!ids.contains(docid)) continue;

            String xml_text = null;
            try {
                xml_text = FileUtils.readFileToString(f, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

            QueryDocument doc = new QueryDocument(docid);
            doc.setXmlText(xml_text);
            doc.setTextAnnotation(tokenizer.getTextAnnotation(doc.plain_text));
            doc.populateXmlMapping();
            docs.add(doc);
        }

        System.out.println("#docs " + docs.size() + " #chars " + docs.stream().mapToInt(x -> x.plain_text.length()).sum());
        return docs;
    }

    public static List<QueryDocument> readSpanish2016EvalDocs() {
        List<QueryDocument> docs = new ArrayList<>();
        String dir = "/shared/corpora/corporaWeb/tac/LDC2016E63_TAC_KBP_2016_Evaluation_Source_Corpus_V1.1/data/spa/";

        List<File> files = new ArrayList<>();

        files.addAll(Arrays.asList(new File(dir + "nw").listFiles()));
        files.addAll(Arrays.asList(new File(dir + "df").listFiles()));


        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer("es");
        for (File f : files) {
            String xml_text = null;
            try {
                xml_text = FileUtils.readFileToString(f, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

            int idx = f.getName().lastIndexOf(".");
            String docid = f.getName().substring(0, idx);
            QueryDocument doc = new QueryDocument(docid);
            doc.setXmlText(xml_text);
            doc.setTextAnnotation(tokenizer.getTextAnnotation(doc.plain_text));
            doc.populateXmlMapping();
            docs.add(doc);
        }

        System.out.println("#docs " + docs.size() + " #chars " + docs.stream().mapToInt(x -> x.plain_text.length()).sum());
        return docs;
    }


    public static List<QueryDocument> readSpanishDocuments(boolean train){
        String trans_path, xml_path, anno_path;
        if(train){
            trans_path = Constants.trainingDocTranslatedSpa;
            xml_path = Constants.trainingDocSpaXml;
            anno_path = Constants.trainingDocSpaPlainJson;
        }
        else{
            trans_path = Constants.testDocTranslatedSpa;
            xml_path = Constants.testDocSpaXml;
            anno_path = Constants.testDocSpaPlainJson;
        }
        List<QueryDocument> docs = new ArrayList<>();
        MentionReader mr = new MentionReader();
        List<ELMention> mentions = mr.readESMentions(train);
        for(ELMention m: mentions){
            if(!train) {
                m.setStartOffset(m.getStartOffset() + 39);
                m.setEndOffset(m.getEndOffset() + 39);
            }
        }

        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer("es");
//        WhiteSpaceTokenizer tokenizer = new WhiteSpaceTokenizer();
//        EnglishTokenizer tokenizer = new EnglishTokenizer();
        Map<String, List<String>> id2lines = readDocs(trans_path);
        for(String id: id2lines.keySet()){
            String xmlfile = xml_path+id;
            if(id.contains("_DF_"))
                xmlfile += ".df.xml";
            else if(!train && id.contains("_NW_"))
                xmlfile += ".nw.xml";
            else
                xmlfile += ".xml";
            String xml_text = null;
            try {
                xml_text = FileUtils.readFileToString(new File(xmlfile), "UTF-8");
//                if(id.equals("SPA_DF_000404_20150427_F001000C0")){
//                    System.out.println(xml_text.substring(2616, 2621));
//                    System.exit(-1);
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
//            AnnotatedDocument adoc = readAnnotatedDocs(anno_path+ id);
//            QueryDocument doc = new QueryDocument(adoc, id2lines.get(id).stream().collect(joining("\n\n")));
            QueryDocument doc = new QueryDocument(id);
            doc.setXmlText(xml_text);
            doc.setTextAnnotation(tokenizer.getTextAnnotation(doc.plain_text));
            doc.populateXmlMapping();
            doc.mentions = mentions.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
            docs.add(doc);
        }
        return docs;
    }

    public static String replaceSpecialChars(String ps){
        if(ps.contains("'"))
            ps = ps.replaceAll("'", " ' ");
        if(ps.contains("\""))
            ps = ps.replaceAll("\"", " \" ");
        if(ps.contains("-"))
            ps = ps.replaceAll("-", " - ");
        if(ps.contains("."))
            ps = ps.replaceAll("\\.", " . ");
        if(ps.contains("?"))
            ps = ps.replaceAll("\\?", " ? ");
        if(ps.contains(":"))
            ps = ps.replaceAll(":", " : ");
        if(ps.contains(";"))
            ps = ps.replaceAll(";", " ; ");
        if(ps.contains(","))
            ps = ps.replaceAll(",", " , ");
        if(ps.contains("@"))
            ps = ps.replaceAll("@", " , ");
        if(ps.contains("!"))
            ps = ps.replaceAll("!", " , ");
        if(ps.contains("("))
            ps = ps.replaceAll("\\(", " ( ");
        if(ps.contains(")"))
            ps = ps.replaceAll("\\)", " ) ");
        ps = ps.replaceAll("\\s+"," ").trim();
        return ps;
    }

    private static AnnotatedDocument readAnnotatedDocs(String filename){
        Gson gson = new Gson();
        String json = null;
        try {
            json = FileUtils.readFileToString(new File(filename), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        AnnotatedDocument doc = gson.fromJson(json, AnnotatedDocument.class);
        return doc;
    }

    public static List<QueryDocument> readChineseDocuments(boolean train){
        String xml_path;
        if(train) xml_path = Constants.trainingDocCmnXml;
        else xml_path = Constants.testDocCmnXml;

        ChineseTokenizer ctokenizer = (ChineseTokenizer) MultiLingualTokenizer.getTokenizer("zh");
        CharacterTokenizer tokenizer = new CharacterTokenizer();
        MentionReader mr = new MentionReader();
        List<ELMention> mentions = mr.readZHMentions(train);
        for (ELMention m : mentions) {
            m.setMention(ctokenizer.trad2simp(m.getMention()));
            if(!train) {
                m.setStartOffset(m.getStartOffset() + 39);
                m.setEndOffset(m.getEndOffset() + 39);
            }
        }

        List<QueryDocument> docs = new ArrayList<>();
        Map<String, List<String>> id2lines = readDocs(xml_path);
        for(String id: id2lines.keySet()){
//            if(!id.equals("CMN_NW_001172_20150407_F0000006B")) continue;
            String xmlfile = xml_path+id;
            if(id.contains("_DF_"))
                xmlfile += ".df.xml";
            else if(!train && id.contains("_NW_"))
                xmlfile += ".nw.xml";
            else
                xmlfile += ".xml";

            String xml_text = null;
            try {
                xml_text = FileUtils.readFileToString(new File(xmlfile), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
//            AnnotatedDocument adoc = readAnnotatedDocs(Constants.trainingDocSpaPlainJson + id);
//            AnnotatedDocument adoc = new AnnotatedDocument(id);
            QueryDocument doc = new QueryDocument(id);
            xml_text = ctokenizer.trad2simp(xml_text);
            doc.setXmlText(xml_text);
            doc.setTextAnnotation(tokenizer.getTextAnnotation(doc.plain_text));
            doc.populateXmlMapping();
            docs.add(doc);
            doc.mentions = mentions.stream().filter(x -> x.getDocID().equals(id)).collect(Collectors.toList());
        }
        return docs;
    }

    public static List<QueryDocument> loadENDocsWithPlainMentions(boolean train){
        List<QueryDocument> docs = readEnglishDocuments(train);
        setMentionPlainOffsets(docs);
        System.out.println("Loaded "+docs.size()+" docs, "+docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");
        return docs;
    }

    public static List<QueryDocument> loadESDocsWithPlainMentions(boolean train){
        List<QueryDocument> docs = readSpanishDocuments(train);

        if(train)
            propogateLabel(docs, 4);

        System.out.println(docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");
        resolveNested(docs);
        System.out.println(docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");
//        setMentionPlainOffsets(docs);
//        System.out.println("Loaded "+docs.size()+" docs, "+docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");
        return docs;
    }

    public static boolean overlap(ELMention m, ELMention m1){
        return (m.getStartOffset() >= m1.getStartOffset() && m.getStartOffset() < m1.getEndOffset())
                || (m1.getStartOffset() >= m.getStartOffset() && m1.getStartOffset() < m.getEndOffset());

    }

    public static void propogateLabel(List<QueryDocument> docs, int th){
        int n_add = 0;
        for(QueryDocument doc: docs){
            List<ELMention> nmm = new ArrayList<>();
            for(ELMention m: doc.mentions){
                String s = m.getMention().toLowerCase();
                String s1 = doc.xml_text.substring(m.getStartOffset(), m.getEndOffset()).toLowerCase();
                if(!s.replaceAll("\\s+", "").equals(s1.replaceAll("\\s+", ""))){
                    System.out.println("Removed "+doc.getDocID()+" "+s+" || "+s1);
//                    System.exit(-1);
                }
                else
                    nmm.add(m);
            }
            doc.mentions = nmm;

            List<ELMention> newm = new ArrayList<>();
            Map<String, List<ELMention>> surface2m = doc.mentions.stream().collect(groupingBy(x -> x.getMention().toLowerCase()));
            for(String surf: surface2m.keySet()){
                if(surf.length() < th) continue;
                String text = doc.xml_text.toLowerCase();
                int search_start = 0;
                while(true){
                    int idx = text.indexOf(surf, search_start);
                    if(idx == -1) break;
                    search_start = idx + surf.length();

                    boolean overlap = false;
                    for(ELMention mm: doc.mentions) {
                        if ((mm.getStartOffset() >= idx && mm.getStartOffset() < idx+surf.length())
                                || (idx >= mm.getStartOffset() && idx < mm.getEndOffset())){
                            overlap = true;
                            break;
                        }
                    }

                    if(!overlap){
                        ELMention nm = new ELMention(doc.getDocID(), idx, idx+surf.length());
                        nm.setMention(doc.xml_text.substring(idx, idx+surf.length()));
                        nm.setType(surface2m.get(surf).get(0).getType());
                        newm.add(nm);

//                        System.out.println("get "+nm.getMention()+" "+ nm.getType());
                        n_add++;
                    }

                }

            }

            doc.mentions.addAll(newm);
            doc.mentions = doc.mentions.stream().sorted((x1, x2) -> Integer.compare(x1.getStartOffset(), x2.getStartOffset())).collect(Collectors.toList());
        }
        System.out.println("Added "+n_add+" mentions");
    }


    public static void resolveNested(List<QueryDocument> docs){

        for(QueryDocument doc: docs){
            Set<Integer> bad = new HashSet<>();
            for(int i = 0; i < doc.mentions.size(); i++){
                if(bad.contains(i)) continue;
                ELMention m = doc.mentions.get(i);
                for(int j = i+1; j < doc.mentions.size(); j++){
                    if(bad.contains(j)) continue;
                    ELMention m1 = doc.mentions.get(j);
                    if(overlap(m, m1)){
                        if(m.getMention().length() >= m1.getMention().length()) {
//                        if(m.getMention().length() <= m1.getMention().length()) {
                            bad.add(j);
                        }
                        else {
                            bad.add(i);
                            break;
                        }
                    }
                }
            }

            List<ELMention> nm = new ArrayList<>();
            for(int i = 0; i < doc.mentions.size(); i++)
                if(!bad.contains(i))
                    nm.add(doc.mentions.get(i));

            doc.mentions = nm.stream().sorted((x1, x2) -> Integer.compare(x1.getStartOffset(), x2.getStartOffset())).collect(Collectors.toList());
        }

    }

    public static void setMentionPlainOffsets(List<QueryDocument> docs){
        int cnt = 0;
        for(QueryDocument doc: docs) {
            List<ELMention> nm = new ArrayList<>();
            List<ELMention> authors = new ArrayList<>();
            Set<String> seen_off = new HashSet<>();
            for(ELMention m: doc.mentions){
                Pair<Integer, Integer> offsets = doc.getPlainOffsets(m);
                if(offsets!=null) {
                    m.setStartOffset(offsets.getFirst());
                    m.setEndOffset(offsets.getSecond());
                    String tmp = doc.getTextAnnotation().getText().substring(m.getStartOffset(), m.getEndOffset());
                    tmp = tmp.replaceAll("\\s+","");
                    if(!tmp.toLowerCase().equals(m.getMention().replaceAll("\\s+","").toLowerCase())){
                        System.out.println(doc.getDocID());
                        System.out.println(m.getMention()+" "+tmp);
                        System.exit(-1);
                    }
//                    seen_off.add(offsets.getFirst()+"_"+offsets.getSecond());
//                    System.out.println(doc.getDocID()+" "+m.getStartOffset()+" "+m.getEndOffset()+" "+m.getMention());
                    try {
                        int tid = doc.getTextAnnotation().getTokenIdFromCharacterOffset(m.getStartOffset());
                        int tid1 = doc.getTextAnnotation().getTokenIdFromCharacterOffset(m.getEndOffset() - 1);
                        String token = doc.getTextAnnotation().getToken(tid).toLowerCase();
                        String token1 = doc.getTextAnnotation().getToken(tid1).toLowerCase();
                        if (!m.getMention().toLowerCase().contains(token) || !m.getMention().toLowerCase().contains(token1)) {

                            if(m.getMention().toLowerCase().startsWith(token) || token.startsWith(m.getMention().toLowerCase())){
                                nm.add(m);
                            }
                            else if(m.getMention().toLowerCase().equals("@"+token)){
                                nm.add(m);
                            }
                            else {
//                                System.out.println(doc.getDocID() + " " + m.getMention() + " " + token + " " + token1);
//                                System.out.println(doc.getTextAnnotation().getText().substring(m.getStartOffset(), m.getEndOffset()));
                                cnt++;
                            }
                        }
                        else
                            nm.add(m);

                    }catch (Exception e){
                        continue;
                    }

                }
                else{
//                    System.out.println("Failed to map mention: "+m.getMention()+" "+m.getType());
                    authors.add(m);
                }
            }
            doc.mentions = nm;
            doc.authors = authors;
        }

        System.out.println("Skipped "+cnt+" mentions");
    }

    public static List<QueryDocument> loadZHDocsWithPlainMentions(boolean train){
        List<QueryDocument> docs = readChineseDocuments(train);
//        if(train)
            propogateLabel(docs, 2);
        System.out.println(docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");
        resolveNested(docs);
        System.out.println(docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");
//        setMentionPlainOffsets(docs);
//        System.out.println("Loaded "+docs.size()+" docs, "+docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");
        return docs;
    }

    public static void main(String[] args) {

//        readSpanish2016EvalDocs();
        readChinese2016EvalDocs();
//        loadENDocsWithPlainMentions(false);
    }
}
