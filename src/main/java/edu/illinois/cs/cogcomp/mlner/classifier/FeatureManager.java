package edu.illinois.cs.cogcomp.mlner.classifier;

import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import edu.illinois.cs.cogcomp.indsup.learning.LexManager;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.MediaWikiSearch;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class FeatureManager implements Serializable {
    private static final long serialVersionUID = -1932878634118945538L;
    private LexManager lex;
    private MediaWikiSearch mws;
    private Map<String, List<String>> typecache = new HashMap<>();

    public FeatureManager(){
        lex = new LexManager();
        mws = new MediaWikiSearch();

    }

    public FeatureVector getFV(String mid){

        Map<String, Double> featureMap = new HashMap<>();
        addTypes(mid, featureMap);
//        addWikiTypes(mid, featureMap);
        addFeatureCombos(featureMap);

//        System.out.println(mid+" "+featureMap);

        FeatureVector fv = lex.convertRawFeaMap2LRFeatures(featureMap);
        fv.sort();
        return fv;
    }


    public void addTypes(String mid, Map<String, Double> f) {
        try {
            List<String> fbtypes;
            if(typecache.containsKey(mid))
                fbtypes = typecache.get(mid);
            else {
                fbtypes = FreeBaseQuery.getTypesFromMid(mid);
                typecache.put(mid, fbtypes);
            }
            fbtypes = fbtypes.stream().filter(x -> !x.startsWith("base.")).collect(Collectors.toList());
            for(String t: fbtypes) {

                String[] tokens = t.split("\\.");
                for(String token: tokens) {
                    addFeature(token, 1.0, f);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addFeatureCombos(Map<String, Double> fm){

        List<String> keys = fm.keySet().stream().sorted((x1, x2) -> Integer.compare(lex.getFeatureID(x1), lex.getFeatureID(x2))).collect(toList());
        for(int i = 0; i < keys.size(); i++){
            for(int j = i + 1; j < keys.size(); j++){
                String name = "COMBO_"+keys.get(i)+"_"+keys.get(j);
                double val = fm.get(keys.get(i))*fm.get(keys.get(j));
                addFeature(name, val, fm);
            }
        }
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
