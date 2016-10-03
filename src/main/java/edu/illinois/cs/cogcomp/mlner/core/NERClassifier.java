package edu.illinois.cs.cogcomp.mlner.core;

import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.lang.math.NumberUtils;

import java.util.*;

/**
 * It's only used for generating wikifier features.
 * We use illinois-ner as the classifier instead.
 * Created by ctsai12 on 9/29/15.
 */
public class NERClassifier {
    private NERFeatureManager fm;

    public NERClassifier(String lang){
        fm = new NERFeatureManager(lang);
    }

    public void extractNERFeatures(QueryDocument doc){
        for(int j = 0; j < doc.mentions.size(); j++){
            ELMention m = doc.mentions.get(j);
            if(m.ner_features.size() > 0)
                continue;
            if(NumberUtils.isNumber(m.getMention().trim()))
                continue;
            Map<String, Double> map = getFeatureMap(doc.mentions, j, true);
            for(String key: map.keySet()){
                m.ner_features.put(key, map.get(key));

            }
        }
    }

    private Map<String, Double> getFeatureMap(List<ELMention> mentions, int idx, boolean train){
        int window = 3;
        List<ELMention> before_mentions = new ArrayList<>();
        List<ELMention> after_mentions = new ArrayList<>();
        ELMention m = mentions.get(idx);
        for(int i = idx - 1; i >=0 && before_mentions.size() < window; i --) {
            before_mentions.add(mentions.get(i));
        }
        for(int i = idx+1; i < mentions.size() && after_mentions.size() < window; i++) {
            after_mentions.add(mentions.get(i));
        }
        return fm.getFeatureMap(m, before_mentions, after_mentions, train);
    }

    public static void main(String[] args) {

    }


}
