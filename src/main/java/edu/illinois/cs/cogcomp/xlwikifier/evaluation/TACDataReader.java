package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.ner.IO.ResourceUtilities;
import edu.illinois.cs.cogcomp.nlp.tokenizer.Tokenizer;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * This class reads TAC 2016 data
 * Created by ctsai12 on 10/27/16.
 */
public class TACDataReader {

    public static List<QueryDocument> readEnglishEvalDocs() {

//        Set<String> docids = readEnglishGoldNAM().stream()
//                .map(x -> x.getDocID()).collect(Collectors.toSet());
//
//        String dir = "/shared/corpora/corporaWeb/tac/LDC2016E63_TAC_KBP_2016_Evaluation_Source_Corpus_V1.1/data/eng/";
//        for(String id: docids){
//            String f = null;
//            if(id.contains("_NW_"))
//                f = dir+"nw/";
//            else
//                f = dir+"df/";
//            try {
//                Files.copy(new File(f, id+".xml"), new File("/shared/preprocessed/ctsai12/multilingual/deft/xlwikifier-data/test/tac-en/", id+".xml"));
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        List<QueryDocument> docs = new ArrayList<>();
        return docs;
    }

    public static List<QueryDocument> readChineseEvalDocs() {

        List<QueryDocument> docs = new ArrayList<>();

        List<String> filenames = null;
        try {
            filenames = LineIO.read(ConfigParameters.tac_zh_samples);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        TextAnnotationBuilder tokenizer = MultiLingualTokenizer.getTokenizer("zh");
        for (String filename: filenames) {
            int idx = filename.lastIndexOf(".");
            int idx1 = filename.lastIndexOf("/");
            String docid = filename.substring(idx1+1, idx);

            String xml_text = null;
            InputStream res = ResourceUtilities.loadResource(filename);
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(res, "UTF-8"));
                xml_text = in.lines().collect(joining("\n"))+"\n";
                String tmp = ChineseHelper.convertToSimplifiedChinese(xml_text);
                if(xml_text.length() != tmp.length()){
                    System.out.println(docid+" "+xml_text.length()+" "+tmp.length());
                    System.exit(-1);
                }
                xml_text = ChineseHelper.convertToSimplifiedChinese(xml_text);
                in.close();
            }catch(Exception e){
                e.printStackTrace();
            }

            QueryDocument doc = new QueryDocument(docid);
            XMLOffsetHandler xmlhandler = new XMLOffsetHandler(xml_text, tokenizer);
            doc.setText(xmlhandler.plain_text);
            doc.setTextAnnotation(xmlhandler.ta);
            doc.setXmlHandler(xmlhandler);
            docs.add(doc);
        }

        return docs;
    }

    public static List<QueryDocument> readSpanishEvalDocs(int ndocs) {

        List<QueryDocument> docs = new ArrayList<>();

        TextAnnotationBuilder tokenizer = MultiLingualTokenizer.getTokenizer("es");
        List<String> filenames = null;
        try {
            filenames = LineIO.read(ConfigParameters.tac_es_samples);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        for (String filename: filenames) {
            if(docs.size() == ndocs) break;
            int idx = filename.lastIndexOf(".");
            int idx1 = filename.lastIndexOf("/");
            String docid = filename.substring(idx1+1, idx);

            String xml_text = null;
            InputStream res = ResourceUtilities.loadResource(filename);
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(res, "UTF-8"));
                xml_text = in.lines().collect(joining("\n"));
                in.close();
            }catch(Exception e){
                e.printStackTrace();
            }


            QueryDocument doc = new QueryDocument(docid);
            XMLOffsetHandler xmlhandler = new XMLOffsetHandler(xml_text, tokenizer);
            doc.setText(xmlhandler.plain_text);
            doc.setTextAnnotation(xmlhandler.ta);
            doc.setXmlHandler(xmlhandler);
            docs.add(doc);
        }

        return docs;
    }

    public static List<ELMention> readEnglishGoldNAM(){
        return readGoldMentions().stream()
                .filter(x -> x.getLanguage().equals("ENG"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }

    public static List<ELMention> readChineseGoldNAM(){
        return readGoldMentions().stream()
                .filter(x -> x.getLanguage().equals("CMN"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }

    public static List<ELMention> readSpanishGoldNAM(){
        return readGoldMentions().stream()
                .filter(x -> x.getLanguage().equals("SPA"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }

    private static List<ELMention> readGoldMentions(){
        return readGoldMentions(ConfigParameters.tac_golds);
    }

    private static List<ELMention> readGoldMentions(String filename){
        List<ELMention> ret = new ArrayList<>();
        List<String> lines = new ArrayList<>();

        InputStream res = ResourceUtilities.loadResource(filename);
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(res, "UTF-8"));
            lines = in.lines().collect(Collectors.toList());
            in.close();
        }catch (Exception e){
            e.printStackTrace();
        }

        for(String line: lines){
            String[] tokens = line.split("\t");
            String id = tokens[1];
            String mention = tokens[2];
            String[] tmp = tokens[3].split(":");
            String docid = tmp[0];
            String[] offsets = tmp[1].split("-");
            int start = Integer.parseInt(offsets[0]);
            int end = Integer.parseInt(offsets[1])+1;
            String answer = tokens[4];
            String type = tokens[5];
            String lang = docid.split("_")[0];
            String noun_type = tokens[6];

            ELMention m = new ELMention(id, mention, docid);
            m.setLanguage(lang);
            m.setType(type);
            m.setEndOffset(end);
            m.setStartOffset(start);
            m.gold_mid = answer;
            m.setNounType(noun_type);
            ret.add(m);
        }
        return ret;
    }

    public static void main(String[] args) {
        readEnglishEvalDocs();
    }
}
