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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * This class reads TAC 2016 data
 * Created by ctsai12 on 10/27/16.
 */
public class TACDataReader {

    private static Logger logger = LoggerFactory.getLogger(TACDataReader.class);
    private boolean dieOnReadFailure;

    public TACDataReader( boolean dieOnReadFailure )
    {
        this.dieOnReadFailure = dieOnReadFailure;
    }

    public List<QueryDocument> readEnglishEvalDocs() throws FileNotFoundException {
        return readEnglishEvalDocs(ConfigParameters.tac_en_samples);
    }
    public List<QueryDocument> readEnglishEvalDocs(String corpusDir) throws FileNotFoundException {
        List<QueryDocument> docs = new ArrayList<>();

        TextAnnotationBuilder tokenizer = MultiLingualTokenizer.getTokenizer("en");
        List<String> filenames = null;
        try {
            filenames = LineIO.read(corpusDir);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            if ( dieOnReadFailure )
                throw e;
            else {
                logger.error("Failed to read file from {}: {}", corpusDir, e.getMessage() );
                logger.error("configured to continue in spite of failure.");
            }
        }

        for (String filename: filenames) {
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

    public List<QueryDocument> readChineseEvalDocs() throws FileNotFoundException {
        return readChineseEvalDocs(ConfigParameters.tac_zh_samples);
    }

    public List<QueryDocument> readChineseEvalDocs(String corpusDir) throws FileNotFoundException {

        List<QueryDocument> docs = new ArrayList<>();

        List<String> filenames = null;
        try {
            filenames = LineIO.read(corpusDir);
        } catch (FileNotFoundException e) {
            if (dieOnReadFailure)
                throw e;
            else {
                e.printStackTrace();
                logger.error("failed to read document from directory {}: {}", corpusDir, e.getMessage());
                logger.error("configured to keep on anyway.");
            }
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

    public List<QueryDocument> readSpanishEvalDocs(int ndocs) throws FileNotFoundException {
        return readSpanishEvalDocs(ndocs, ConfigParameters.tac_es_samples);
    }

    public List<QueryDocument> readSpanishEvalDocs(int ndocs, String corpusDir) throws FileNotFoundException {
        List<QueryDocument> docs = new ArrayList<>();

        TextAnnotationBuilder tokenizer = MultiLingualTokenizer.getTokenizer("es");
        List<String> filenames = null;
        try {
            filenames = LineIO.read(corpusDir);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            if ( dieOnReadFailure )
                throw e;
            else {
                logger.error("Failed to read file from {}: {}", corpusDir, e.getMessage() );
                logger.error("configured to continue in spite of failure.");
            }
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

    public List<ELMention> readEnglishGoldNAM(){
        return readGoldMentions().stream()
                .filter(x -> x.getLanguage().equals("ENG"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }

    public List<ELMention> readChineseGoldNAM(){
        return readGoldMentions().stream()
                .filter(x -> x.getLanguage().equals("CMN"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }

    public List<ELMention> readSpanishGoldNAM(){
        return readGoldMentions().stream()
                .filter(x -> x.getLanguage().equals("SPA"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }

    public List<ELMention> readGoldMentions(){
        return readGoldMentions(ConfigParameters.tac_golds);
    }

    public List<ELMention> readGoldMentions(String filename){
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
        boolean dieOnReadFailure = true;
        TACDataReader reader = new TACDataReader(dieOnReadFailure);
    }
}
