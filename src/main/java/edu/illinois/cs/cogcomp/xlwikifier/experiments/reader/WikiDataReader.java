package edu.illinois.cs.cogcomp.xlwikifier.experiments.reader;

import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ctsai12 on 1/19/16.
 */
public class WikiDataReader {
    private static Logger logger = LoggerFactory.getLogger(WikiDataReader.class);
    private String path = "/shared/bronte/ctsai12/multilingual/data/wikidata2/";
    public WikiDataReader(){}

    private List<QueryDocument> read(String dir, String lang) throws IOException {
        List<QueryDocument> docs = new ArrayList<>();
        File df = new File(dir);
        for(File f: df.listFiles()){
            if(f.toString().endsWith(".txt")) {
                String docid = f.toString();
                QueryDocument doc = new QueryDocument(docid);
                String text = FileUtils.readFileToString(f, "UTF-8");
                doc.plain_text = text;

                String mf = f.toString().substring(0, f.toString().length()-3)+"mentions";
                List<ELMention> mentions = new ArrayList<>();
//                System.out.println(f);
                for(String line: LineIO.read(mf)){
                    String[] tokens = line.split("\t");
                    try{
                        Integer.parseInt(tokens[0]);
                        Integer.parseInt(tokens[1]);
                    }
                    catch (Exception e){
                        continue;
                    }
                    ELMention m = new ELMention(docid, Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]));
                    m.gold_enwiki_title = tokens[2];
                    m.gold_wiki_title = tokens[3];
                    m.setMention(text.substring(m.getStartOffset(), m.getEndOffset()));
                    m.gold_lang = lang;
                    m.is_ne = false;
                    if(tokens.length>4){
                        if(Integer.parseInt(tokens[4])==1)
                            m.eazy = false;
                        else
                            m.eazy = true;
                    }
                    mentions.add(m);
                }
                doc.mentions = mentions;
                docs.add(doc);
            }
        }
        logger.info("read "+docs.size()+" docs");
        logger.info("read "+ docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");
        return docs;
    }

    public List<QueryDocument> readTrainData(String lang){
        logger.info("Reading "+lang+" wikipedia training docs...");
        try {
            return read(path+lang+"/train", lang);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<QueryDocument> readTestData(String lang){
        logger.info("Reading "+lang+" wikipedia test docs...");
        try {
            return read(path+lang+"/test", lang);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {
        WikiDataReader r = new WikiDataReader();
        r.readTrainData("ta");
        r.readTestData("ta");
//        r.readTrainData("de");
//        r.readTestData("de");
//        r.readTrainData("fr");
//        r.readTestData("fr");
//        r.readTrainData("it");
//        r.readTestData("it");
//        r.readTrainData("zh");
//        r.readTestData("zh");
//        List<QueryDocument> docs = r.readTestData("es");
//        for(QueryDocument doc: docs){
//            for(ELMention m: doc.mentions){
//                String ss = doc.plain_text.substring(m.getStartOffset(), m.getEndOffset());
//                if(!m.getMention().equals(ss)) {
//                    System.out.println(doc.getDocID());
//                    System.out.println(m.getMention() + " " + ss);
//                    System.out.println(m.getStartOffset()+" "+m.getEndOffset());
//                    System.exit(-1);
//                }
//            }
//        }
    }
}
