package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.tokenizers.CharacterTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class reads TAC 2016 data
 * Created by ctsai12 on 10/27/16.
 */
public class TACDataReader {


    public static List<QueryDocument> readChineseEvalDocs() {

        Set<String> docids = readChineseGoldNAM().stream()
                .map(x -> x.getDocID()).collect(Collectors.toSet());

        List<QueryDocument> docs = new ArrayList<>();

        Tokenizer tokenizer = new CharacterTokenizer();
        for (File f : new File(ConfigParameters.tac_zh_dir).listFiles()) {
            int idx = f.getName().lastIndexOf(".");
            String docid = f.getName().substring(0, idx);

            if(!docids.contains(docid)) continue;

            String xml_text = null;
            try {
                xml_text = FileUtils.readFileToString(f, "UTF-8");
            } catch (IOException e) {
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

    public static List<QueryDocument> readSpanishEvalDocs() {

        Set<String> docids = readSpanishGoldNAM().stream()
                .map(x -> x.getDocID()).collect(Collectors.toSet());

        List<QueryDocument> docs = new ArrayList<>();

        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer("es");
        for (File f : new File(ConfigParameters.tac_es_dir).listFiles()) {
            int idx = f.getName().lastIndexOf(".");
            String docid = f.getName().substring(0, idx);

            if(!docids.contains(docid)) continue;

            String xml_text = null;
            try {
                xml_text = FileUtils.readFileToString(f, "UTF-8");
            } catch (IOException e) {
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
        List<String> lines = null;
        try {
            lines = LineIO.read(filename);
        } catch (FileNotFoundException e) {
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
}
