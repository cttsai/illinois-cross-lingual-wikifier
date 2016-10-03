package edu.illinois.cs.cogcomp.xlwikifier.experiments.xlel21;

import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

/**
 * Created by ctsai12 on 1/23/16.
 */
public class XLELReader {

//    private String query_dir = "/shared/dickens/ctsai12/multilingual/data/21lang/machine-aligned-queries/";
    private String doc_dir = "/shared/dickens/ctsai12/multilingual/data/21lang/docs/";
    private String query_dir = "/shared/dickens/ctsai12/multilingual/data/21lang/hand-curated-queries/";
    private Map<String, String> id2en;

    public XLELReader(){
        loadEnMentions();

    }

    private void loadEnMentions(){

        List<String> lines = null;
        try {
            lines = LineIO.read("/shared/dickens/ctsai12/multilingual/data/21lang/monolingual-queries/PER-all-queries");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        id2en = new HashMap<>();
        for(String line: lines){
            String[] tokens = line.split("\t");
            id2en.put(tokens[0], tokens[1]);
        }
    }

    public List<QueryDocument> readDocs(String lang, String part){

        List<ELMention> mentions;
        if(part.equals("train")){
            mentions = readMentions(lang, "train");
            mentions.addAll(readMentions(lang, "development"));
        }
        else
            mentions = readMentions(lang, "eval");

//        mentions = mentions.stream().filter(x -> !x.gold_wiki_title.startsWith("NIL"))
//                .collect(Collectors.toList());
        Set<String> idset = mentions.stream().map(x -> x.getID()).collect(toSet());
        Set<String> dnames = mentions.stream().map(x -> x.getDocID()).collect(toSet());
        List<QueryDocument> docs = new ArrayList<>();
        TACKnowledgeBase kb = TACKnowledgeBase.defaultInstance();

//        FeatureManager fm = new FeatureManager(true, "en", "en", false);
        String dir = doc_dir+lang+"/"+lang+"/";

        int null_vec = 0;
        for(String dname: dnames){
            try {
                String text = FileUtils.readFileToString(new File(dir + dname), "UTF-8");
                QueryDocument doc = new QueryDocument(dname);
                doc.plain_text = text;
                docs.add(doc);
                Map<String, List<ELMention>> m2elm = mentions.stream().filter(x -> x.getDocID().equals(dname)).collect(groupingBy(x -> x.getMention()));
                doc.mentions = new ArrayList<>();
                for(String surface: m2elm.keySet()){
                    int start = 0;
                    for(ELMention m: m2elm.get(surface)) {
//                    surface = surface.replaceAll(" ,",",").replaceAll(" '","'");
                        int idx = text.indexOf(surface, start);
                        if (idx == -1) {
                            System.out.println("Counldn't match " + m.getID() + " " + m.getMention() + " " + dname);
                            break;
                        }
                        m.setStartOffset(idx);
                        m.setEndOffset(idx + surface.length());
                        if (!m.gold_wiki_title.startsWith("NIL"))
                            m.gold_wiki_title = kb.getEntryById(m.gold_wiki_title).title;
                        m.gold_lang = "en";
                        doc.mentions.add(m);
                        start = idx + surface.length();
//                    Double[] vec = fm.we.getTitleVector(m.gold_wiki_title, "en");
//                    if(vec == null)
//                        null_vec++;

                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Read "+docs.size()+" docs");
        System.out.println(docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");
        System.out.println("#mentions don't have gold vec "+null_vec);
        return docs;
    }

    public List<ELMention> readMentions(String lang, String part){
        System.out.println("Reading "+lang+" "+part+" mentions");
        List<String> lines = null;
        try {
            lines = LineIO.read(query_dir + lang + "/PER-"+part+"-queries");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<ELMention> mentions = new ArrayList<>();
        for(String line: lines){
            String[] tokens = line.trim().split("\t");
            if(tokens[4].equals("UNKNOWN"))
                continue;
            String surface = tokens[1].replaceAll("-LRB-", "(").replaceAll("-RRB-",")");
            String docid = tokens[2].substring(0, tokens[2].length()-4);
            ELMention m = new ELMention(tokens[0], surface, docid);
            m.setType(tokens[3]);
            m.gold_wiki_title = tokens[4];
            if(!id2en.containsKey(tokens[0]))
                System.out.println("No English Translation: "+tokens[0]);
            m.en_gold_trans = id2en.get(tokens[0]);
            mentions.add(m);
        }
        System.out.println(mentions.size()+" mentions");
        return mentions;
    }

    public static void main(String[] args) {
        XLELReader r = new XLELReader();
//        List<ELMention> ms = r.readTrainMentions("it", "train");
//        System.out.println(ms.size());
        r.readDocs("fr", "train");
        r.readDocs("fr", "eval");
    }

}
