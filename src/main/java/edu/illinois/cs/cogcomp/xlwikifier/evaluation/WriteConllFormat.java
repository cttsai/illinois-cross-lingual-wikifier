package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ctsai12 on 5/31/16.
 */
public class WriteConllFormat {


    public void writeENDocs(){

        TACDataReader reader = new TACDataReader(true);
        try {
            List<QueryDocument> docs = reader.readEnglishEvalDocs();
            List<ELMention> mentions = reader.readEnglishGoldNAM();

            System.out.println("Gold mention count: "+mentions.size());

            for(QueryDocument doc: docs){
                doc.mentions = mentions.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
            }
            String outdir = "/shared/corpora/ner/tac/en/2016eval/";

            writeDocs(docs.stream().filter(x -> x.mentions.size()>0).collect(Collectors.toList()), outdir);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }


    public static void writeDocs(List<QueryDocument> docs, String outdir){


        int ent_cnt = 0;
        for(QueryDocument doc: docs){

            List<Pair<Integer, Integer>> badintervals = TACUtils.getBadIntervals(doc.getXMLText());

            TextAnnotation ta = doc.getTextAnnotation();
            int psid = -1;
            String out = "";
            boolean preB = false;
            for(int i = 0; i < ta.getTokens().length; i++){

                String token = ta.getToken(i);
                IntPair offsets = ta.getTokenCharacterOffset(i);
                Pair<Integer, Integer> xmloffsets = doc.getXmlhandler().getXmlOffsets(offsets.getFirst(), offsets.getSecond());

                boolean bad = false;
                for(Pair<Integer, Integer> inte: badintervals){

                    if((xmloffsets.getFirst() >= inte.getFirst() && xmloffsets.getFirst() < inte.getSecond())
                            ||(inte.getFirst() >= xmloffsets.getFirst() && inte.getFirst() < xmloffsets.getSecond())) {
                        bad = true;
                        break;
                    }
                }
                if(bad) continue;


                int sid = doc.getTextAnnotation().getSentenceId(i);
                if(i!=0 && sid!=psid) out += "\n";
                psid = sid;


                String label = "O";
                for(ELMention m: doc.mentions){
                    String type = m.getType();
                    if(m.getStartOffset() >= xmloffsets.getFirst() && m.getStartOffset() < xmloffsets.getSecond()) {
                        label = "B-" + type;
                        ent_cnt++;
                        break;
                    }
                    else if(m.getStartOffset() < xmloffsets.getFirst() && m.getEndOffset() > xmloffsets.getFirst()) {
                        if(preB)
                            label = "I-" + type;
                        else {
                            label = "B-" + type;
                            ent_cnt++;
                        }
                        break;
                    }
                }

                if(label.equals("O")) preB = false;
                else preB = true;

                if(token.startsWith("http:"))
                    out += "\n";
                else
                    out += label+"\t"+xmloffsets.getFirst()+"\t"+xmloffsets.getSecond()+"\tx\tx\t"+token+"\tx\tx\tx\tx\n";
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

        try {
            ConfigParameters.setPropValues("config/xlwikifier-tac.config");
        } catch (IOException e) {
            e.printStackTrace();
        }

        WriteConllFormat writer = new WriteConllFormat();
        writer.writeENDocs();

    }
}
