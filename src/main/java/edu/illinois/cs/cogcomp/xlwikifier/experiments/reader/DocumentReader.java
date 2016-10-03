package edu.illinois.cs.cogcomp.xlwikifier.experiments.reader;

import com.google.gson.Gson;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.Constants;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 8/31/15.
 */
public class DocumentReader {

    public Map<String, String> title_map;
    public Tokenizer tokenizer;
    private static Logger logger = LoggerFactory.getLogger(DocumentReader.class);
    public DocumentReader(){}

    public DocumentReader(String lang){
//        title_map = new HashMap<>();
//
//        try {
//            ArrayList<String> lines = LineIO.read("/shared/bronte/ctsai12/multilingual/wikidump/"+lang+"/" + lang + "_wiki_view/file.list.rand");
//            for(String line: lines){
//                title_map.put(line.toLowerCase().replaceAll(" ","_"), line);
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }

        tokenizer = MultiLingualTokenizer.getTokenizer(lang);
    }

    /**
     * Read all files under the input dir
     * @param dir
     * @return  doc id to line list
     */
    public Map<String, List<String>> readDocs(String dir){
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

    public AnnotatedDocument readAnnotatedDocs(String filename){
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



    public void generateAnnotatedDocJson(String in_dir, String out_dir){
        Map<String, List<String>> id2xml = readDocs(in_dir);
        Gson gson = new Gson();

        for(String id: id2xml.keySet()){
            System.out.println(id);
            List<String> lines = id2xml.get(id);
            lines = lines.stream().map(x -> x.trim())
                    .filter(x -> x.startsWith("<ORIGINAL") || x.startsWith("<TOKEN"))
                    .collect(Collectors.toList());
            List<List<Token>> segments = new ArrayList<>();
            String ptext = null;
            for(String line: lines){
                String text = line.split(">")[1].split("<")[0];
                text = StringEscapeUtils.unescapeXml(text).trim();
                if(line.startsWith("<ORIGINAL")){
                    ptext = text;
                    segments.add(new ArrayList<>());
                }
                else{
                    String[] splits = line.split("\"");
                    int start = Integer.parseInt(splits[7]);
                    int end = Integer.parseInt(splits[9]);
                    Token t = new Token(ptext, start, end, text);
                    segments.get(segments.size()-1).add(t);
                }
            }

            AnnotatedDocument doc = new AnnotatedDocument(id);
            boolean start = false;
            String tmp = "";
            Paragraph para = null;
            for(List<Token> segment: segments){
                if(segment.size() == 0) continue;
                if(!segment.get(0).id.startsWith("<")) {
                    if(start) {
                        if (tmp.isEmpty()) { // start a new paragraph
                            tmp = segment.get(0).id;
                            para = new Paragraph();
                        } else
                            tmp += " " + segment.get(0).id;
                        segment.forEach(x -> x.id = "");
                        para.tokens.addAll(segment);
                    }
                }
                else{
                    // finish up the previous paragraph
                    if(!tmp.isEmpty()){
                        if(!tmp.endsWith("."))
                            tmp += " .";
                        para.surface = tmp;
                        tmp = "";
                        doc.paragraphs.add(para);
                    }

                    if(segment.get(0).id.toLowerCase().startsWith("<quote"))
                        start = false;
                    else
                        start = true;
                }
            }
            if(!tmp.isEmpty()){
                if(!tmp.endsWith("."))
                    tmp += " .";
                para.surface = tmp;
                doc.paragraphs.add(para);
            }
            String json = gson.toJson(doc, AnnotatedDocument.class);
            try {
                FileUtils.writeStringToFile(new File(out_dir + id ), json, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();

            }
        }
    }

    public QueryDocument readWikiDocSingle(String lang, String filename, boolean check){
        String dir = Constants.wikidumpdir+lang+"/"+lang+"_wiki_view/";
//        String dir = Constants.wikidumpdir+lang+"/docs/";
        List<String> lines = null;
        TextAnnotation ta = null;
        try {
            lines = LineIO.read(dir+"annotation/"+filename);
            String plain = FileUtils.readFileToString(new File(dir+"plain/"+filename), "UTF-8");
            ta = tokenizer.getTextAnnotation(plain);
            if(ta == null)
                return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Token> tokens = new ArrayList<>();
        List<ELMention> mentions = new ArrayList<>();
        for (String line : lines) {
            if(!line.startsWith("#")){
                String[] sp = line.split("\t");
                if(sp.length < 3) continue;
                try{
                    Integer.parseInt(sp[1]);
                    Integer.parseInt(sp[2]);
                }
                catch (Exception e){
                    continue;
                }
                Token t = new Token("", Integer.parseInt(sp[1]), Integer.parseInt(sp[2]), sp[0]);
                tokens.add(t);
            }
            else{
                String[] sp = line.substring(1).split("\t");
                if(sp.length<3) continue;
                if(sp[0].trim().isEmpty()) continue;
                ELMention m;
                try {
                    m = new ELMention(filename, Integer.parseInt(sp[1]), Integer.parseInt(sp[2]));
                }catch(Exception e){
                    continue;
                }
                m.gold_wiki_title = sp[0];
                m.gold_lang = lang;
                mentions.add(m);
            }
        }

        AnnotatedDocument adoc = new AnnotatedDocument(filename);
        Paragraph para = new Paragraph();
//        para.tokens.addAll(tokens);
//        adoc.paragraphs.add(para);

        for (ELMention m : mentions) {
            String surface;
            // check if there is an error regarding token/character offsets
            try {
                int start_idx = ta.getTokenIdFromCharacterOffset(m.getStartOffset());
                int end_idx = ta.getTokenIdFromCharacterOffset(m.getEndOffset() - 1);
//                if (!lang.equals("zh"))
//                    surface = adoc.getSurface(m.getStartOffset(), m.getEndOffset());
//                else
//                    surface = adoc.getSurfaceNoSpace(m.getStartOffset(), m.getEndOffset());
                surface = ta.getText().substring(m.getStartOffset(), m.getEndOffset());
                surface = surface.replaceAll("\n", " ");


                if (surface == null || m.getStartOffset() < 0 ||
                        start_idx < 0 || end_idx < 0
                        || !surface.contains(ta.getToken(start_idx))
                        || !surface.contains(ta.getToken(end_idx))) {
                    if(check)
                        return null;
                    else
                        m.setMention(null);
                }
                else {
                    m.setMention(surface);
                }
            } catch (Exception e) {
                if(check)
                    return null;
                else
                    m.setMention(null);
            }
        }
        mentions = mentions.stream().filter(x -> x.getMention()!=null && !x.getMention().trim().isEmpty())
                .collect(Collectors.toList());
        QueryDocument doc = new QueryDocument(adoc, "");
        doc.plain_text = ta.getText();
        doc.mentions = mentions;
        doc.setTextAnnotation(ta);
        return doc;
    }

    public List<QueryDocument> readWikiDocsNew(String lang, int start, int end){
        logger.info("Reading "+lang+" wikipedia docs...");
        List<String> paths = null;
        String dir = null;

        tokenizer = MultiLingualTokenizer.getTokenizer(lang);

        Set<String> badfile = new HashSet<>();

        try {
            dir = "/shared/preprocessed/ctsai12/multilingual/wikidump/"+lang+"/"+lang+"_wiki_view/";
//            dir = "/shared/preprocessed/ctsai12/multilingual/wikidump/"+lang+"/docs/";
            paths = LineIO.read(dir+"file.list.rand");
//            badfile.addAll(LineIO.read(dir+"bad.list"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<QueryDocument> docs = new ArrayList<>();
        int cnt = 0;
        int bad = 0;
        for(String filename: paths.subList(start, Math.min(paths.size(),end))) {
            if((cnt++)%100 == 0)
                System.out.print((cnt++) + "\r");
            if(badfile.contains(filename)) continue;

            QueryDocument doc = readWikiDocSingle(lang, filename, true);
            if(doc == null || doc.mentions.size() == 0){
                bad++;
                badfile.add(filename);
                continue;
            }
            docs.add(doc);
        }
        System.out.println();
        logger.info("#bad "+bad);
        logger.info("#docs "+docs.size());
        logger.info("#mentions "+docs.stream().flatMap(x -> x.mentions.stream()).count());
        String badstr = badfile.stream().collect(joining("\n"));
        try {
            FileUtils.writeStringToFile(new File(dir+"bad.list"), badstr, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return docs;
    }


    public List<QueryDocument> readEnglishDocuments(boolean train){
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
        DirectoryStream<Path> stream = null;
        List<QueryDocument> docs = new ArrayList<>();
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
                docs.add(doc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return docs;
    }

    public void parseSpanishWikiDocs(){
//        String dir = "/shared/experiments/ctsai12/workspace/wikipedia-api/output";
        String dir = "/shared/dickens/ctsai12/multilingual/wikidump/en_wiki_view";
        DirectoryStream<Path> stream = null;
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("en.wikipedia.path"));
            stream = Files.newDirectoryStream(new File(dir).toPath());
        } catch (Exception e) {
            e.printStackTrace();
        }

        int cnt = 0;
        for(Path p: stream) {
            if((cnt++)%1000 == 0) System.out.print(cnt +"\r");
            List<String> lines = null;
            try {
                lines = LineIO.read(p.toString());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            int c = 0;
            for (String line : lines) {
                if(line.startsWith("#")){
                    String[] sp = line.substring(1).split("\t");
                    if(sp.length<3) continue;
                    c++;
                }
            }

            if(c > 20) {
                try {
                    bw.write(p.toString() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<QueryDocument> readSpanishDocuments(){
        List<QueryDocument> docs = new ArrayList<>();
        Map<String, List<String>> id2lines = readDocs(Constants.trainingDocTranslatedSpa);
        MentionReader mr = new MentionReader();
        for(String id: id2lines.keySet()){
            String xmlfile = Constants.trainingDocSpaXml+id;
            if(id.contains("_DF_"))
                xmlfile += ".df.xml";
            else
                xmlfile += ".xml";
            String xml_text = null;
            try {
                xml_text = FileUtils.readFileToString(new File(xmlfile), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            AnnotatedDocument adoc = readAnnotatedDocs(Constants.trainingDocSpaPlainJson + id);
            QueryDocument doc = new QueryDocument(adoc, id2lines.get(id).stream().collect(joining("\n\n")));
            doc.setXmlText(xml_text);
            doc.mentions = mr.readTrainingMentionsSpa().stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
            docs.add(doc);
        }
        return docs;
    }


    public List<QueryDocument> readSpanishTestDocuments(){
        List<QueryDocument> docs = new ArrayList<>();
        Map<String, List<String>> id2lines = readDocs(Constants.testDocTranslatedSpa);
        MentionReader mr = new MentionReader();
        List<ELMention> mentions = mr.readTestMentionsSpa();
        for(ELMention m: mentions){
            m.setStartOffset(m.getStartOffset()+39);
            m.setEndOffset(m.getEndOffset()+39);
        }

        for(String id: id2lines.keySet()){
            String xmlfile = Constants.testDocSpaXml+id;
            if(id.contains("_DF_"))
                xmlfile += ".df.xml";
            else if(id.contains("_NW_"))
                xmlfile += ".nw.xml";
            String xml_text = null;
            try {
                xml_text = FileUtils.readFileToString(new File(xmlfile), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
            AnnotatedDocument adoc = readAnnotatedDocs(Constants.testDocSpaPlainJson + id);
            QueryDocument doc = new QueryDocument(adoc, id2lines.get(id).stream().collect(joining("\n\n")));
            doc.setXmlText(xml_text);
            doc.mentions = mentions.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
            docs.add(doc);
        }
        return docs;
    }

    public static void main(String[] args) throws Exception {
        DocumentReader r = new DocumentReader();
        r.readWikiDocsNew("zh", 0, 5000);

//        r.generateAnnotatedDocJson(Constants.testAnnoDocEngXml, Constants.testDocEngPlainJson);
//        r.translateDocsFromJson(Constants.testDocEngPlainJson, Constants.testDocEngPlain, false);
//        r.generateAnnotatedDocJson(Constants.trainingAnnoDocSpaXml, Constants.trainingDocSpaPlainJson);
//        r.translateDocsFromJson(Constants.trainingDocSpaPlainJson, Constants.trainingDocTranslatedSpa);

//        r.generateAnnotatedDocJson(Constants.testAnnoDocSpaXml, Constants.testDocSpaPlainJson);
//        r.translateDocsFromJson(Constants.testDocSpaPlainJson, Constants.testDocTranslatedSpa);
//        r.readSpanishWikiDocs();
//        r.parseSpanishWikiDocs();
//        r.readWikiDocs("en");
//        r.readWikiDocsNew("en", 0, 1000);

//        List<QueryDocument> docs = r.readChineseDocuments();
//        List<QueryDocument> docs = r.readChineseTestDocuments();
//		r.translateDocs(docs);
        //Translator translator = new Translator();
		//System.out.println(translator.translate("在我看来，美国的种族歧视，非不能治愈，实在是不敢治也","zh-CN","en"));

		/*
        int nn = 0, nns = 0;
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                Pair<Integer, Integer> offsets = doc.getPlainOffsets(m);
                if(offsets==null){
                    nn++;
                    if(!m.gold_mid.startsWith("NIL")) {
                        nns++;
                        System.out.println(doc.getDocID() + " " + m.getStartOffset() + " " + m.getEndOffset());
                    }
                }
//                System.out.println(m.getStartOffset()+" "+m.getEndOffset()+" "+m.getMention());
                String surface = doc.getXmlText().substring(m.getStartOffset(), m.getEndOffset() + 1);
                if(!surface.equals(m.getMention())){
                    System.out.println(surface+" | "+m.getMention());
                }
            }
        }

        System.out.println(nn+" "+nns);
        System.out.println(docs.stream().flatMap(x -> x.mentions.stream()).count());
		*/

    }
}
