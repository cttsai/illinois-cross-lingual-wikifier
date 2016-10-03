package edu.illinois.cs.cogcomp.xlwikifier.experiments;

import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.TAC2014Reader;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.xlel21.TACKnowledgeBase;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

/**
 * Created by ctsai12 on 7/26/16.
 */
public class TAC2014Exp {

    public static void run(){
        String lang = "en";
        TAC2014Reader reader = new TAC2014Reader();
        List<QueryDocument> docs = reader.read2014TrainQueries();
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                m.setStartOffset(m.plain_start);
                m.setEndOffset(m.plain_end);
            }
        }

        WikiCandidateGenerator wcg = new WikiCandidateGenerator(true);
        wcg.genCandidates(docs, lang);

        Ranker ranker = Ranker.loadPreTrainedRanker(lang);
        ranker.setWikiTitleByModel(docs);

        convertGoldIdToTitle(docs);

        printPairsForNESIM(docs);
    }

    public static void printPairsForNESIM(List<QueryDocument> docs){

        String out = "";
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                Set<String> cands = m.getCandidates().stream().map(x -> x.getTitle().toLowerCase()).collect(toSet());
                if(!cands.contains(m.gold_wiki_title)) continue;

                for(String cand: cands){
                    out += m.getMention()+"\t"+cand.replaceAll("_"," ")+"\t"+cand.equals(m.gold_wiki_title.toLowerCase())+"\t"+m.getType()+"\n";
                }
            }
        }

        try {
            FileUtils.writeStringToFile(new File("NESim.data"), out, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void convertGoldIdToTitle(List<QueryDocument> docs){
        TACKnowledgeBase kb = TACKnowledgeBase.defaultInstance();
        int fail = 0;
        int c = 0;
        for(QueryDocument doc: docs) {
            for (ELMention m : doc.mentions) {
                if (!m.gold_wiki_title.startsWith("NIL")) {
                    TACKnowledgeBase.Entry en = kb.getEntryById(m.gold_wiki_title);
                    if (en == null) {
                        fail ++;
                    } else {
                        m.gold_wiki_title = en.title;
                    }
                }
            }
        }

        System.out.println(fail);
        System.out.println(c);
    }

    public static void main(String[] args) {

        run();

    }
}
