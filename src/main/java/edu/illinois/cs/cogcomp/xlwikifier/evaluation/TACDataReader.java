package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.ner.IO.ResourceUtilities;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * This class reads TAC 2015/2016 Tri-Lingual EDL data,
 * including training and test data of 2015 and the test data of 2016.
 *
 * The main function converts the data into CoNLL column format,
 * which is used for training NER models.
 *
 * Created by ctsai12 on 10/27/16.
 */
public class TACDataReader {

    private static Logger logger = LoggerFactory.getLogger(TACDataReader.class);
    private boolean dieOnReadFailure;

    public TACDataReader( boolean dieOnReadFailure )
    {
        this.dieOnReadFailure = dieOnReadFailure;
    }


    public void writeCoNLLFormat(List<QueryDocument> docs, List<ELMention> golds, String out_dir){

        for(QueryDocument doc: docs)
            doc.mentions = golds.stream().filter(x -> doc.getDocID().startsWith(x.getDocID())).collect(Collectors.toList());

        resolveNested(docs);

        int ent_cnt = 0;
        for(QueryDocument doc: docs){

            List<Pair<Integer, Integer>> badintervals = TACUtils.getBadIntervals(doc.getXMLText());

            TextAnnotation ta = doc.getTextAnnotation();

            int psid = -1;
            String out = "";
            boolean preO = true;
            for(int i = 0; i < ta.getTokens().length; i++){

                String token = ta.getToken(i);
                IntPair plain_offsets = ta.getTokenCharacterOffset(i);
                Pair<Integer, Integer> xml_offsets = doc.getXmlhandler().getXmlOffsets(plain_offsets.getFirst(), plain_offsets.getSecond());

                boolean bad = false;
                for(Pair<Integer, Integer> inte: badintervals){
                    if(xml_offsets.getFirst() >= inte.getFirst() && xml_offsets.getSecond() < inte.getSecond()){
                        bad = true;
                        break;
                    }
                }
                if(bad) continue;

                if(token == null || token.trim().isEmpty()) {
                    out+="\n";
                    continue;
                }

                int sid = doc.getTextAnnotation().getSentenceId(i);
                if(i!=0 && sid!=psid) out += "\n";
                psid = sid;

                String label = "O";
                for(ELMention m: doc.mentions){
                    String type = m.getType();
                    if(m.getStartOffset() >= xml_offsets.getFirst()
                            && m.getStartOffset() < xml_offsets.getSecond()) {
                        label = "B-" + type;
                        ent_cnt++;
                        break;
                    }
                    else if(m.getStartOffset() < xml_offsets.getFirst() && m.getEndOffset() > xml_offsets.getFirst()) {
                        if(preO)
                            label = "B-" + type;
                        else
                            label = "I-" + type;
                        break;
                    }
                }

                preO = label.equals("O");

                if(token.startsWith("http:"))
                    out += "\n";
                else
                    out += label+"\t"+xml_offsets.getFirst()+"\t"+xml_offsets.getSecond()+"\tx\tx\t"+token+"\tx\tx\tx\tx\n";
            }
            try {
                FileUtils.writeStringToFile(new File(out_dir, doc.getDocID()), out, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        System.out.println("# entities "+ent_cnt);
    }

    public static boolean overlap(ELMention m, ELMention m1){
        return (m.getStartOffset() >= m1.getStartOffset() && m.getStartOffset() < m1.getEndOffset())
                || (m1.getStartOffset() >= m.getStartOffset() && m1.getStartOffset() < m.getEndOffset());
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
                        if(m.getSurface().length() >= m1.getSurface().length()) {
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

            doc.mentions = nm;
        }
    }

    public List<QueryDocument> read2016EnglishEvalDocs() throws IOException {
        return readDocs(ConfigParameters.tac2016_en_eval, "en");
    }
    public List<QueryDocument> read2016SpanishEvalDocs() throws IOException {
        return readDocs(ConfigParameters.tac2016_es_eval, "es");
    }
    public List<QueryDocument> read2016ChineseEvalDocs() throws IOException {
        return readDocs(ConfigParameters.tac2016_zh_eval, "zh");
    }

    public List<QueryDocument> read2015EnglishEvalDocs() throws IOException {
        return readDocs(ConfigParameters.tac2015_en_eval, "en");
    }
    public List<QueryDocument> read2015SpanishEvalDocs() throws IOException {
        return readDocs(ConfigParameters.tac2015_es_eval, "es");
    }
    public List<QueryDocument> read2015ChineseEvalDocs() throws IOException {
        return readDocs(ConfigParameters.tac2015_zh_eval, "zh");
    }

    public List<QueryDocument> read2015EnglishTrainDocs() throws IOException {
        return readDocs(ConfigParameters.tac2015_en_train, "en");
    }
    public List<QueryDocument> read2015SpanishTrainDocs() throws IOException {
        return readDocs(ConfigParameters.tac2015_es_train, "es");
    }
    public List<QueryDocument> read2015ChineseTrainDocs() throws IOException {
        return readDocs(ConfigParameters.tac2015_zh_train, "zh");
    }

    public List<QueryDocument> readDocs(String corpusDir, String lang) throws IOException {
        List<QueryDocument> docs = new ArrayList<>();

        TextAnnotationBuilder tokenizer = MultiLingualTokenizer.getTokenizer(lang);
        List<File> files = Arrays.stream(new File(corpusDir).listFiles()).sorted().collect(Collectors.toList());
        for(File file: files){

//            if(file == null) continue;

            String filename = file.getAbsolutePath();
            System.out.println(filename);
            int idx = filename.lastIndexOf(".");
            int idx1 = filename.lastIndexOf("/");
            String docid = filename.substring(idx1+1, idx);

            String xml_text = null;
            InputStream res = ResourceUtilities.loadResource(filename);
            BufferedReader in = new BufferedReader(new InputStreamReader(res, "UTF-8"));
            xml_text = in.lines().collect(joining("\n"));
            in.close();

            if(lang.equals("zh"))
                xml_text = ChineseHelper.convertToSimplifiedChinese(xml_text);

            QueryDocument doc = new QueryDocument(docid);
            XMLOffsetHandler xmlhandler = new XMLOffsetHandler(xml_text, tokenizer);
            doc.setText(xmlhandler.plain_text);
            doc.setTextAnnotation(xmlhandler.ta);
            doc.setXmlHandler(xmlhandler);
            docs.add(doc);
        }

        return docs;
    }

    public List<ELMention> read2016EnglishEvalGoldNAM(){
        List<ELMention> mentions = readGoldMentions(ConfigParameters.tac2016_eval_golds).stream()
                .filter(x -> x.getLanguage().equals("ENG"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());

        System.out.println("#golds: "+mentions.size());
        System.out.println("#NILs: "+mentions.stream().filter(x -> x.gold_mid.startsWith("NIL")).count());

        return mentions;
    }
    public List<ELMention> read2016SpanishEvalGoldNAM(){
        return readGoldMentions(ConfigParameters.tac2016_eval_golds).stream()
                .filter(x -> x.getLanguage().equals("SPA"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }
    public List<ELMention> read2016ChineseEvalGoldNAM(){
        return readGoldMentions(ConfigParameters.tac2016_eval_golds).stream()
                .filter(x -> x.getLanguage().equals("CMN"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }

    public List<ELMention> read2015EnglishEvalGoldNAM(){
        return readGoldMentions(ConfigParameters.tac2015_eval_golds).stream()
                .filter(x -> x.getLanguage().equals("ENG"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }
    public List<ELMention> read2015SpanishEvalGoldNAM(){
        return readGoldMentions(ConfigParameters.tac2015_eval_golds).stream()
                .filter(x -> x.getLanguage().equals("SPA"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }
    public List<ELMention> read2015ChineseEvalGoldNAM(){
        return readGoldMentions(ConfigParameters.tac2015_eval_golds).stream()
                .filter(x -> x.getLanguage().equals("CMN"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }
    public List<ELMention> read2015EnglishTrainGoldNAM(){
        return readGoldMentions(ConfigParameters.tac2015_train_golds).stream()
                .filter(x -> x.getLanguage().equals("ENG"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }
    public List<ELMention> read2015SpanishTrainGoldNAM(){
        return readGoldMentions(ConfigParameters.tac2015_train_golds).stream()
                .filter(x -> x.getLanguage().equals("SPA"))
                .filter(x -> x.noun_type.equals("NAM"))
                .collect(Collectors.toList());
    }
    public List<ELMention> read2015ChineseTrainGoldNAM(){
        return readGoldMentions(ConfigParameters.tac2015_train_golds).stream()
                .filter(x -> x.getLanguage().equals("CMN"))
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
            String lang = null;
            if(docid.contains("ENG"))
                lang = "ENG";
            else if(docid.contains("SPA"))
                lang = "SPA";
            else if(docid.contains("CMN"))
                lang = "CMN";
            else{
                logger.error("unknown language "+docid);
                System.exit(-1);
            }
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
        try {
            ConfigParameters.setPropValues("config/xlwikifier-tac.config");
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean dieOnReadFailure = true;
        TACDataReader reader = new TACDataReader(dieOnReadFailure);

        List<QueryDocument> docs = null;
        try {
            docs = reader.read2016EnglishEvalDocs();
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<ELMention> golds = reader.read2016EnglishEvalGoldNAM();

        String outdir = "/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/en/tac2016.eval/";

        reader.writeCoNLLFormat(docs, golds, outdir);
    }
}
