package edu.illinois.cs.cogcomp.xlwikifier.experiments.reader;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.xlwikifier.Constants;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ctsai12 on 2/4/16.
 */
public class TAC2014Reader {
    private static Logger logger = LoggerFactory.getLogger(TACReader.class);

    public TAC2014Reader(){

    }

    private List<ELMention> readGoldMentions(String tab_file, String xml_file){
        try {
            List<ELMention> mentions = new ArrayList<>();
            Map<String, ELMention> id2m = new HashMap<>();
            for(String line: LineIO.read(xml_file)){
                if(line.contains("<query")){
                    String[] tokens = line.trim().split("\"");
                    ELMention m = new ELMention();
                    m.id = tokens[1];
                    mentions.add(m);
                    id2m.put(tokens[1], m);
                }
                else if(line.contains("<name")){
                    String plain_line = line.trim().replaceAll("\\<.*?\\>", "");
                    plain_line = StringEscapeUtils.unescapeXml(plain_line);
                    mentions.get(mentions.size()-1).setMention(plain_line);
                }
                else if(line.contains("<docid")){
                    String plain_line = line.trim().replaceAll("\\<.*?\\>", "");
                    mentions.get(mentions.size()-1).docid = plain_line;
                }
                else if(line.contains("<beg")){
                    String plain_line = line.trim().replaceAll("\\<.*?\\>", "");
                    mentions.get(mentions.size()-1).setStartOffset(Integer.parseInt(plain_line));
                }
                else if(line.contains("<end")){
                    String plain_line = line.trim().replaceAll("\\<.*?\\>", "");
                    mentions.get(mentions.size()-1).setEndOffset(Integer.parseInt(plain_line));
                }
            }
            System.out.println("#mentions: "+mentions.size());

            for(String line: LineIO.read(tab_file)){
                String[] tokens = line.split("\t");
                String id = tokens[0];
                if(id2m.containsKey(id)){
                    id2m.get(id).gold_wiki_title = tokens[1];
                    id2m.get(id).setType(tokens[2]);
                }
            }

            List<ELMention> filtered = new ArrayList<>();
            for(ELMention m: mentions)
                if(m.gold_wiki_title != null)
                    filtered.add(m);

            System.out.println("#mentions: "+filtered.size());

            return filtered;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private List<QueryDocument> getQueryDocsFromMentions(List<ELMention> mentions){

        Map<String, List<ELMention>> docid2m = new HashMap<>();
        for(ELMention m: mentions){
            if(!docid2m.containsKey(m.getDocID()))
                docid2m.put(m.getDocID(), new ArrayList<ELMention>());
            docid2m.get(m.getDocID()).add(m);
        }
        List<QueryDocument> docs = new ArrayList<>();
        for(String docid: docid2m.keySet()) {
            File f = new File(Constants.src_doc_dir + docid);
            try {
                String xml = FileUtils.readFileToString(f, "UTF-8");
                QueryDocument doc = new QueryDocument(docid);
                doc.setXmlText(xml);
                doc.mentions = docid2m.get(docid);
                docs.add(doc);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return docs;
    }

    public List<QueryDocument> read2014TrainQueries(){
        logger.info("Reading TAC 2014 training documents");
        List<ELMention> mentions = readGoldMentions(Constants.TAC2014_train_mention_tab, Constants.TAC2014_train_mention_xml);
        List<QueryDocument> docs = getQueryDocsFromMentions(mentions);
        filterMentionsInXmlTag(docs);
        setPlainOffsets(docs);
        return docs;
    }

    public List<QueryDocument> read2014EvalQueries(){
        logger.info("Reading TAC 2014 evaluation documents");
        List<ELMention> mentions = readGoldMentions(Constants.TAC2014_eval_mention_tab, Constants.TAC2014_eval_mention_xml);
        List<QueryDocument> docs = getQueryDocsFromMentions(mentions);
        filterMentionsInXmlTag(docs);
        setPlainOffsets(docs);
        return docs;
    }

    private void filterMentionsInXmlTag(List<QueryDocument> docs){
        for(QueryDocument doc: docs){
            List<ELMention> mentions = new ArrayList<>();
            for(ELMention m: doc.mentions){
                if(doc.plain_text.contains(m.getMention()))
                    mentions.add(m);
            }
            doc.mentions = mentions;
        }
    }

    private void setPlainOffsets(List<QueryDocument> docs){
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                Pair<Integer, Integer> off = doc.getPlainOffsets(m);
                m.plain_start = off.getFirst();
                m.plain_end = off.getSecond();
            }
        }
    }


    public static void main(String[] args) {
        TAC2014Reader r = new TAC2014Reader();
        List<QueryDocument> docs = r.read2014TrainQueries();
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                if(!m.getMention().toLowerCase().equals(doc.plain_text.substring(m.plain_start, m.plain_end).toLowerCase())) {
                    System.out.println(m.getMention());
                    System.out.println(doc.plain_text.substring(m.plain_start, m.plain_end));
                    System.out.println("!!!");
                }
            }
        }
    }

}
