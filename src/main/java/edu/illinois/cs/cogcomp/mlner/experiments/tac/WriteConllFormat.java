package edu.illinois.cs.cogcomp.mlner.experiments.tac;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.TACReader;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.tac2016.Solver;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * Created by ctsai12 on 5/31/16.
 */
public class WriteConllFormat {

    public void writeZHDocs(){
        List<QueryDocument> docs = TACReader.loadZHDocsWithPlainMentions(true);
        String outdir = "/shared/corpora/ner/tac/zh/";
//        writeDocs(docs, outdir+"train-new12-char-prop/");

        docs = TACReader.loadZHDocsWithPlainMentions(false);
        writeDocs(docs, outdir+"test-new12-char-prop/");

    }

    public void writeESDocs(){
        List<QueryDocument> docs = TACReader.loadESDocsWithPlainMentions(true);
        String outdir = "/shared/corpora/ner/tac/es/";
//        writeDocs(docs, outdir+"train-new12-prop/");

        docs = TACReader.loadESDocsWithPlainMentions(false);
        writeDocs(docs, outdir+"test-new12/");

    }

    public void writeENDocs(){
        List<QueryDocument> docs = TACReader.loadENDocsWithPlainMentions(true);
        String outdir = "/shared/corpora/ner/tac/en/";
        writeDocs(docs, outdir+"train/");

        docs = TACReader.loadENDocsWithPlainMentions(false);
        writeDocs(docs, outdir+"test/");

    }


    public static void writeDocs(List<QueryDocument> docs, String outdir){


        int ent_cnt = 0;
        for(QueryDocument doc: docs){
            if(doc.getTextAnnotation().getTokens().length != doc.tokens.size()){
                System.out.println("Token # don't match: "+doc.getDocID());
                System.out.println(doc.getTextAnnotation().getTokens().length+" "+doc.tokens.size());
                System.exit(-1);
            }

            List<Pair<Integer, Integer>> badintervals = Solver.getBadIntervals(doc);

            int psid = -1;
            String out = "";
            boolean preB = false;
            for(int i = 0; i < doc.tokens.size(); i++){

                ELMention token = doc.tokens.get(i);

//                boolean bad = false;
//
//                for(Pair<Integer, Integer> inte: badintervals){
//
//                    if((token.xml_start >= inte.getFirst() && token.xml_start < inte.getSecond())
//                            ||(inte.getFirst() >= token.xml_start && inte.getFirst() < token.xml_end)) {
//                        bad = true;
//                        break;
//                    }
//                }
//                if(bad) continue;

                if(token.getMention() == null) {
                    out+="\n";
                    continue;
                }

                int sid = doc.getTextAnnotation().getSentenceId(i);
                if(i!=0 && sid!=psid) out += "\n";
                psid = sid;



                String label = "O";
                for(ELMention m: doc.mentions){
                    String type = m.getType();
                    if(m.getStartOffset() >= token.xml_start && m.getStartOffset() < token.xml_end) {
                        label = "B-" + type;
                        ent_cnt++;
                        break;
                    }
                    else if(m.getStartOffset() < token.xml_start && m.getEndOffset() > token.xml_start) {
                        if(preB)
                            label = "I-" + type;
                        else
                            label = "B-" + type;

                        break;
                    }
                }

                if(label.equals("O")) preB = false;
                else preB = true;

                if(token.getMention().startsWith("http:"))
                    out += "\n";
                else
                    out += label+"\t"+token.xml_start+"\t"+token.xml_end+"\tx\tx\t"+token.getMention()+"\tx\tx\tx\tx\n";
            }
            try {
                FileUtils.writeStringToFile(new File(outdir+doc.getDocID()), out, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        System.out.println("# entities "+ent_cnt);
    }

    public static void main(String[] args) {

        WriteConllFormat writer = new WriteConllFormat();
//        writer.writeZHDocs();

        TACReader tr = new TACReader();
//        List<QueryDocument> docs = tr.readSpanish2016EvalDocs();
        List<QueryDocument> docs = tr.readChinese2016EvalDocs();
        String outdir = "/shared/corpora/ner/tac/zh/eval2016/";
        writeDocs(docs, outdir);

//        writer.writeESDocs();
//        writeENDocs();
    }
}
