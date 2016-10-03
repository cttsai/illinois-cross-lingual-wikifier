package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import edu.illinois.cs.cogcomp.indsup.learning.LexManager;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 */
public class LinkerFeatureManager implements Serializable {
    private LexManager lex;
    private Set<String> ranker_fnames;

    public LinkerFeatureManager(){
        lex = new LexManager();
        String[] names = {"TITLEPROB", "PRESCORE", "INITMATCH", "INITPARTIALMATCH"};
        ranker_fnames = new HashSet<>(Arrays.asList(names));
    }



    public FeatureVector getFV(ELMention m, QueryDocument doc){

        Map<String, Double> featureMap = new HashMap<>();

        addTopRankerFeatures(m, featureMap);
        addTopRankerScore(m, featureMap);
        addMinSim(m, featureMap);
        addAvgSim(m, featureMap);
        addNoTopVec(m, featureMap);
        addTopSource(m, featureMap);
//        addSmallCands(m, featureMap);
//        addTopRankerScoreDiff(m, featureMap);

        addFeatureCombos(featureMap);
        FeatureVector fv = lex.convertRawFeaMap2LRFeatures(featureMap);
        fv.sort();
        return fv;
    }

    private void addMinSim(ELMention m, Map<String, Double> fm){
        List<WikiCand> cands = m.getCandidates();
        if(cands.size()>0){
            Double score = cands.get(cands.size() - 1).ranker_feats.get("PRESCORE");
            addFeature("MINSCORE", score, fm);
        }
    }

    private void addAvgSim(ELMention m, Map<String, Double> fm){
        List<WikiCand> cands = m.getCandidates();
        double sum = 0;
        for(WikiCand c: cands){
            double score = c.ranker_feats.get("PRESCORE");
            sum += score;
        }
        if(cands.size()>0)
            sum /= cands.size();
        addFeature("AVGSCORE", sum, fm);
    }

    private void addTopRankerFeatures(ELMention m, Map<String, Double> fm){
        if(m.getCandidates().size()>0){
            for(String name: m.getCandidates().get(0).ranker_feats.keySet()){
//                if(ranker_fnames.contains(name))
                    addFeature("TOP:"+name, m.getCandidates().get(0).ranker_feats.get(name), fm);
            }
        }
    }

    private void addNoTopVec(ELMention m, Map<String, Double> fm){
        if(m.getMidVec()==null)
            addFeature("NULLTOPVEC", 1, fm);
    }

    private void addSmallCands(ELMention m, Map<String, Double> fm){
        if(m.getCandidates().size()<5)
            addFeature("SMALLCANDS", 1, fm);
    }

    private void addTopSource(ELMention m, Map<String, Double> fm){
        if(m.getCandidates().size()>0){
            addFeature("TOPSRC"+m.getCandidates().get(0).src, 1, fm);
        }
    }

    private void addTopRankerScore(ELMention m, Map<String, Double> fm){
        if(m.getCandidates().size()>0){
            addFeature("TOPCANDSCORE", m.getCandidates().get(0).score, fm);
        }
    }

    private void addTopRankerScoreDiff(ELMention m, Map<String, Double> fm){
        if(m.getCandidates().size()>1){
            addFeature("TOPCANDSCOREDIFF", m.getCandidates().get(0).score - m.getCandidates().get(1).score, fm);
        }
//        else if(m.getCandidates().size()>0){
//            addFeature("TOPCANDSCOREDIFF", m.getCandidates().get(0).score, fm);
//        }
    }


    private void addFeatureCombos(Map<String, Double> fm){

        List<String> keys = fm.keySet().stream().sorted((x1, x2) -> Integer.compare(lex.getFeatureID(x1), lex.getFeatureID(x2))).collect(toList());
        for(int i = 0; i < keys.size(); i++){
            for(int j = i + 1; j < keys.size(); j++){
                String name = keys.get(i)+"_x_"+keys.get(j);
                double val = fm.get(keys.get(i))*fm.get(keys.get(j));
                addFeature(name, val, fm);
            }
        }
    }


    private void addSrc(WikiCand cand, Map<String, Double> f){
        addFeature("SRC:"+cand.src, 1.0, f);
    }


    private void addTitleProb(WikiCand cand, Map<String, Double> f){
        addFeature("TITLEPROB", cand.ptgivens, f);
    }

    private void addPreScore(WikiCand cand, Map<String, Double> f){
        addFeature("PRESCORE", cand.score, f);
    }


    public void addFeature(String featureName, double val,
                           Map<String, Double> featureMap) {
        featureMap.put(featureName, val);
        if (!lex.containFeature(featureName) && lex.isAllowNewFeatures()) {
            lex.previewFeature(featureName);
        }
    }



    public static void main(String[] args) throws IOException {


    }
}
