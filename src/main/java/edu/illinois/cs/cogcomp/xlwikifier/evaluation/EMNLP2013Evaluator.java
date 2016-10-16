package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.mlner.CrossLingualNER;
import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifier;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;

import java.io.File;
import java.util.*;

/**
 * Created by lchen112 on 10/10/16.
 */

public class EMNLP2013Evaluator {
    public static final String[] datasets = { "ACE", "MSNBC", "AQUAINT",
            "Wikipedia" };
    public static void evaluateMention(ArrayList<GoldDocument> goldDocuments, ArrayList<QueryDocument> queryDocuments){
        int hit = 0;
        int totalGold = 0;
        int totalPred = 0;
        for(int i = 0; i < goldDocuments.size(); i++){
            ArrayList<String> goldmentions = new ArrayList<String>();
            ArrayList<String> predmentions = new ArrayList<String>();
            for(GoldMention m : goldDocuments.get(i).goldMentions)
                goldmentions.add(m.toString());
            for(ELMention m : queryDocuments.get(i).mentions)
                predmentions.add(m.toString());
            for(String m : goldmentions){
                if(predmentions.contains(m))
                    hit += 1;
            }
            totalGold += goldmentions.size();
            totalPred += predmentions.size();
        }

        System.out.printf("(Mention) Hit = %d, Total Gold = %d, Total Pred = %d\n", hit, totalGold, totalPred);
    }
    public static void evaluateEndSystem(ArrayList<GoldDocument> goldDocuments, ArrayList<QueryDocument> queryDocuments){
        double hit = 0.0;
        double goldTotal = 0.0;
        double predTotal = 0.0;
        for(int i = 0; i < goldDocuments.size(); i++){
            Set<String> mentionSet = new HashSet<String>();
            Set<String> goldBOT = new HashSet<String>();
            Set<String> predBOT = new HashSet<String>();
            for(GoldMention m : goldDocuments.get(i).goldMentions){
                if(!m.getWikititle().equals("*null*")) {
                    mentionSet.add(m.getMention());
                    goldBOT.add(m.getWikititle());
                }
            }
            for(ELMention m : queryDocuments.get(i).mentions){
                if(mentionSet.contains(m.getMention()) && !m.getWikiTitle().equals("*null*"))
                    predBOT.add(m.getWikiTitle());
            }
            Set<String> intersection = new HashSet<String>(goldBOT);
            intersection.retainAll(predBOT);
            hit += (double)intersection.size();
            goldTotal += (double)goldBOT.size();
            predTotal += (double)predBOT.size();
        }
        double precision = hit/predTotal;
        double recall = hit/goldTotal;
        double f1 = 2.0*precision*recall/ (precision+ recall);
        System.out.printf("(EndSystem) F1 = %f, Precision = %f, Recall = %f\n", f1, precision, recall);
    }
    public static void main(String[] args) throws Exception {

        String dataPrefix = "/shared/bronte/mssammon/WikifierResources/eval/ACL2010DataNewFormat/";
        CrossLingualNER.init("en", false);
        CrossLingualWikifier.init("en");

        for (String dataset : datasets) {
            System.out.printf("===========Evaluate Dataset %s ==========\n", dataset);
            String dataFolder = dataPrefix + dataset + "/";
            ArrayList<QueryDocument> queryDocuments = new ArrayList<QueryDocument>();
            ArrayList<GoldDocument> goldDocuments = new ArrayList<GoldDocument>();
            File folder = new File(dataFolder);
            for (File file : folder.listFiles()) {
                GoldDocument gdoc = new GoldDocument(file);
                goldDocuments.add(gdoc);
            }
            for (GoldDocument gd : goldDocuments) {
                QueryDocument qdoc = CrossLingualNER.annotate(gd.plain_text);
                CrossLingualWikifier.wikify(qdoc);
                queryDocuments.add(qdoc);
            }
            evaluateMention(goldDocuments, queryDocuments);
            evaluateEndSystem(goldDocuments, queryDocuments);
        }
    }
}
