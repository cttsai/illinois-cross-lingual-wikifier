package edu.illinois.cs.cogcomp.xlwikifier.experiments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.QueryMQL;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import java.io.IOException;
import java.io.File;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.*;


import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by ctsai12 on 11/24/15.
 */
public class Evaluator {

    private static Logger logger = LoggerFactory.getLogger(Evaluator.class);
    public void evaluateMid(List<QueryDocument> docs) {
        int correct = 0, typed_correct = 0, contain = 0;
        int total = 0;
        for(QueryDocument doc: docs) {
            for (ELMention m : doc.mentions) {
                total++;
                if (((m.getMid().equals(m.gold_mid))
                        || (m.getMid().startsWith("NIL") && m.gold_mid.startsWith("NIL")))) {
                    correct++;
                    if(m.getType()==null || m.getType().equals(m.pred_type))
                        typed_correct++;
                }

                Set<String> cands;
                cands = m.getCandidates().stream().map(x -> x.getTitle()).collect(Collectors.toSet());
//                cands = m.top_results.stream().map(x -> x.getMid().substring(1).replaceAll("/",".")).collect(Collectors.toSet());
//                cands = m.top_results.stream().map(x -> x.getMid().substring(1).replaceAll("/",".")).collect(toSet());
                if((cands.size() == 0 && m.gold_mid.startsWith("NIL")) || cands.contains(m.gold_mid)) {
                    contain++;
                }
                else{
//                    System.out.println(cands.stream().collect(joining(" ")));
//                    System.out.println("! "+m.gold_mid);
                }
            }
        }
        System.out.println("Mid Accuracy: " + (double) correct / total + ", " + correct + "/" + total);
        System.out.println("Mid Accuracy (typed): " + (double) typed_correct / total + ", " + typed_correct + "/" + total);
        System.out.println("Mid Coverage: " + (double) contain / total + ", " + contain + "/" + total);
    }

    public void evaluateNonNILMid(List<QueryDocument> docs) {
        int correct = 0, contain = 0;
        int total = 0;
        for(QueryDocument doc: docs) {
            for (ELMention m : doc.mentions) {
                total++;
                if(!m.gold_mid.startsWith("NIL")) {
                    if (m.getMid().equals(m.gold_mid))
                        correct++;
                }
            }
        }
        System.out.println("Non NIL Mid Accuracy: " + (double) correct / total + ", " + correct + "/" + total);
    }

    public void evaluateWikiTitle(List<QueryDocument> docs) {
        int correct = 0, contain = 0, total = 0, contain_hard = 0;
        int hard_cor = 0, hard_tot = 0;
        int top5 = 0, top3 = 0;
        int top5_hard = 0, top3_hard = 0;
		String out = "";
        for(QueryDocument doc: docs) {
            for (ELMention m : doc.mentions) {
                total++;
                if(!m.eazy) hard_tot++;
                if (m.getWikiTitle().toLowerCase().equals(m.gold_wiki_title.toLowerCase())
                        || (m.getWikiTitle().startsWith("NIL") && m.gold_wiki_title.startsWith("NIL"))) {
                    correct++;
                    if(!m.eazy) hard_cor ++;
					out += "1\n";
                }
				else {
//                    System.out.println(m.getMention());
//                    System.out.println("\t"+m.trans);
//                    System.out.println("\t"+m.getWikiTitle()+"\t"+m.gold_wiki_title);
                    out += "0\n";
                }

                List<String> cands = m.getCandidates().stream().map(x -> x.getTitle().toLowerCase()).collect(toList());
                if((cands.size() == 0 && m.gold_wiki_title.startsWith("NIL"))
                    || (cands.subList(0, Math.min(3,cands.size())).contains(m.gold_wiki_title.toLowerCase()))){
                    top3++;
                    if(!m.eazy) top3_hard++;
                }
                if((cands.size() == 0 && m.gold_wiki_title.startsWith("NIL"))
                        || (cands.subList(0, Math.min(5,cands.size())).contains(m.gold_wiki_title.toLowerCase()))){
                    top5++;
                    if(!m.eazy) top5_hard++;
                }


                if(m.getCandidates().stream().map(x -> x.getTitle().toLowerCase()).collect(toSet())
                        .contains(m.gold_wiki_title.toLowerCase())) {
                    contain++;
                    if(!m.eazy) contain_hard++;
                }

            }
        }
        logger.info("Wiki Title P@1: " + (double) 100*correct / total + ", " + correct + "/" + total);
        //logger.info("Wiki Title P@3: " + (double) top3 / total + ", " + top3 + "/" + total);
        //logger.info("Wiki Title P@5: " + (double) top5 / total + ", " + top5 + "/" + total);
        logger.info("Hard Title P@1: " + (double) 100*hard_cor / hard_tot + ", " + hard_cor + "/" + hard_tot);
        //logger.info("Hard Title P@3: " + (double) top3_hard / hard_tot + ", " + top3_hard + "/" + hard_tot);
        //logger.info("Hard Title P@5: " + (double) top5_hard / hard_tot + ", " + top5_hard + "/" + hard_tot);
        logger.info("Easy Title P@1: " + (double) 100*(correct-hard_cor) / (total-hard_tot));
        //logger.info("Easy Title P@3: " + (double) (top3-top3_hard) / (total-hard_tot));
        //logger.info("Easy Title P@5: " + (double) (top5-top5_hard) / (total-hard_tot));
        logger.info("Wiki Title Coverage: " + (double) contain / total + ", " + contain + "/" + total);
        logger.info("Hard Title Coverage: " + (double) contain_hard / hard_tot + ", " + contain_hard + "/" + hard_tot);
        logger.info("Easy Title Coverage: " + (double) (contain-contain_hard) / (total - hard_tot) + ", " + (contain-contain_hard) + "/" + (total-hard_tot));
		try{
		FileUtils.writeStringToFile(new File("wikipedia.output"), out);
		}catch(IOException e){
		}
    }

    public void evaluateWikiTitles(List<QueryDocument> docs) {
        int correct = 0, contain = 0;
        int total = 0;
        QueryMQL qm = new QueryMQL();
        for(QueryDocument doc: docs) {
            for (ELMention m : doc.mentions) {
                total++;
                Set<String> preds = m.wiki_titles.stream().map(x -> x.toLowerCase()).collect(toSet());
                if (preds.contains(m.gold_wiki_title.toLowerCase())
                        || (preds.size()==0 && m.gold_wiki_title.startsWith("NIL")))
                    correct++;

                Set<String> allcands = m.top_results.stream()
                        .flatMap(x -> qm.lookupWikiTitleFromMid(x.getMid(), "es").stream())
                        .map(x -> x.toLowerCase()).collect(toSet());
                if(allcands.contains(m.gold_wiki_title.toLowerCase())
                        || (allcands.size()==0 && m.gold_wiki_title.startsWith("NIL")))
                    contain++;
            }
        }
        System.out.println("Wiki Titles Accuracy: " + (double) correct / total + ", " + correct + "/" + total);
        System.out.println("Wiki Titles Coverage: " + (double) contain / total + ", " + contain + "/" + total);
    }
}
