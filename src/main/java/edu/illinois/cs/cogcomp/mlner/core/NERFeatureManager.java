package edu.illinois.cs.cogcomp.mlner.core;

import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import edu.illinois.cs.cogcomp.indsup.learning.LexManager;
import edu.illinois.cs.cogcomp.xlwikifier.core.StopWord;
import edu.illinois.cs.cogcomp.xlwikifier.core.WordEmbedding;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.MediaWikiSearch;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class NERFeatureManager implements Serializable {
    private static final long serialVersionUID = -1932878634118945538L;
    private LexManager lex;
    private MediaWikiSearch mws;
    private LangLinker ll;
    public WordEmbedding we;
    private Set<String> en_stops;
    private Set<String> fo_stops;
    private boolean alltype = true;
    private boolean mono = false;

    public NERFeatureManager(String lang){
        lex = new LexManager();
        mws = new MediaWikiSearch();
        ll = new LangLinker();
//        ll.factor = 0.95;
//        if(lang != null) {
//            we = new WordEmbedding();
//            if(!lang.equals("en"))
//                we.loadMultiDBNew(lang);
//            else
//                we.setMonoVecsNew(lang);
//        }
        en_stops = StopWord.getStopWords("en");
//        en_stops.clear();
        fo_stops = StopWord.getStopWords(lang);
    }


    public Map<String, Double> getFeatureMap(ELMention mention, List<ELMention> before_mentions, List<ELMention> after_mentions, boolean train){
        Map<String, Double> featureMap = new HashMap<>();
        if(mention.is_ne){
            addIsNE(mention, featureMap);
        }
        else if(mention.is_stop || (mention.getMid().startsWith("NIL") && mention.getWikiTitle().startsWith("NIL"))){
            addStopWord(mention, featureMap);
//            addNILMID(mention, featureMap);
        }
        else {
            addTopWikiTypes(mention, featureMap);
            addBeforeWikiTypes(before_mentions, featureMap, 0);
            addAfterWikiTypes(after_mentions, featureMap, 0);

            addTopTypes(mention, featureMap, train);
            addBeforeTypes(before_mentions, featureMap, 0);
//            addBeforeTypes(before_mentions, featureMap, 1);
            addAfterTypes(after_mentions, featureMap, 0);
//            addAfterTypes(after_mentions, featureMap, 1);
            addSecondTypes(mention, featureMap);
            addBeforeSecondTypes(before_mentions, featureMap, 0);
            addAfterSecondTypes(after_mentions, featureMap, 0);
//            addTopRankerFeatures(mention, featureMap);
//            addTopScore(mention, featureMap);
//            addTopTitle(mention, featureMap);
        }

//        addBeforeTypes(before_mentions, featureMap, 1);
//        addBeforeWikiTypes(before_mentions, featureMap, 1);
//        addTopStringSim(mention, featureMap);
//        addMentionEmb(mention, featureMap);
//        addBeforeTypes(before_mentions, featureMap, 1);
//        addBeforeWikiTypes(before_mentions, featureMap, 1);
//        addBeforeTypes(before_mentions, featureMap, 1);
//        addBeforeTypes(before_mentions, featureMap, 3);
//        addBeforeTypes1(before_mentions, featureMap);
//        addPrepBefore(before_mentions, featureMap);

//        addPrevTag(before_mentions, train, featureMap);
//        addPrevPrevTag(before_mentions, train, featureMap);
//        addCap(mention, featureMap);
//        addPreCap(before_mentions, featureMap);
//        addAfterCap(after_mentions, featureMap);
//        addWord(mention, featureMap);
//        addPreWord(before_mentions, featureMap);
//        addAfterWord(after_mentions, featureMap);
//        addHyph(mention, after_mentions, before_mentions, featureMap);
//        addPrevNE(before_mentions, featureMap);
//        addWordEmbed(mention, featureMap);

//        addFeatureCombos(featureMap);
//        addFeatureCombos1(before_mentions, train, featureMap);
        return featureMap;
    }

    public FeatureVector getFV(Map<String, Double> map){

        FeatureVector fv = lex.convertRawFeaMap2LRFeatures(map);
        fv.sort();
        return fv;
    }

    private void addIsNE(ELMention m, Map<String, Double> fm){
        addFeature("ISNEFROMEPREVIOUS-"+m.pred_type, 1, fm);
        addFeature("ISNEFROMEPREVIOUS-"+m.pred_type.substring(2), 1, fm);
    }

    private void addStopWord(ELMention m, Map<String, Double> fm){
        if(m.is_stop)
            addFeature("ISSTOPWORD", 1, fm);
    }

    private void addNILMID(ELMention m, Map<String, Double> fm){
        if(m.getMid().startsWith("NIL"))
            addFeature("NILTOPMID", 1, fm);
    }

    private void addPrevNE(List<ELMention> before, Map<String, Double> fm){
        if(before.size()>0){
            if(before.get(0).is_ne)
                addFeature("PREISNE", 1, fm);
        }
    }

    private void addTopRankerFeatures(ELMention m, Map<String, Double> fm){
        if(m.getCandidates().size()>0){
            if(m.getCandidates().get(0).ranker_feats!=null)
                for(String name: m.getCandidates().get(0).ranker_feats.keySet()){
                    addFeature("TOPRANKERF-"+name, m.getCandidates().get(0).ranker_feats.get(name), fm);
                }
        }
    }

    private void addWordEmbed(ELMention m, Map<String, Double> f){

        if(m.ngram == 1) {
            Double[] vec = we.getWordVector(m.getMention(), m.getLanguage());
            if(vec != null)
                for(int i = 0; i < vec.length; i++){
                    addFeature("WORDEMBED-"+i+":", vec[i], f);
                }
        }

    }

    private void addWord(ELMention m, Map<String, Double> f){
        addFeature("WORD-"+m.getMention().toLowerCase(), 1, f);
    }

    private void addPreWord(List<ELMention> before, Map<String, Double> f){
        if(before.size()>0){
            addFeature("PREWORD-"+before.get(0).getMention().toLowerCase(), 1, f);
        }
    }

    private void addAfterWord(List<ELMention> after, Map<String, Double> f){
        if(after.size()>0){
            addFeature("AFTERWORD-"+after.get(0).getMention().toLowerCase(), 1, f);
        }
    }

    private void addHyph(ELMention m, List<ELMention> before, List<ELMention> after, Map<String, Double> f){
        if(m.getMention().contains("-"))
            addFeature("CONTAINHYPHEN", 1, f);
        else if(before.size()>0 && before.get(0).getMention().contains("-")){
                addFeature("CONTAINHYPHEN", 1, f);
        }
        else if(after.size()>0 && after.get(0).getMention().contains("-")){
            addFeature("CONTAINHYPHEN", 1, f);
        }
    }

    private void addCap(ELMention m, Map<String, Double> f){
        String init = m.getMention().substring(0,1);
        if(init.equals(init.toUpperCase()))
            addFeature("INITCAP", 1.0, f);
    }

    private void addPreCap(List<ELMention> before, Map<String, Double> f){
        if(before.size()>0){
            String init = before.get(0).getMention().substring(0,1);
            if(init.equals(init.toUpperCase()))
                addFeature("PREINITCAP", 1.0, f);
        }
    }

    private void addAfterCap(List<ELMention> after, Map<String, Double> f){
        if(after.size()>0){
            String init = after.get(0).getMention().substring(0,1);
            if(init.equals(init.toUpperCase()))
                addFeature("AFTERINITCAP", 1.0, f);
        }
    }
    private void addTopScore(ELMention m, Map<String, Double> f){
        if(m.getCandidates().size()>0)
            addFeature("TOPSCORE", m.getCandidates().get(0).score, f);
    }

    private void addTopStringSim(ELMention m, Map<String, Double> f){
        if(m.getCandidates().size()>0){
            addFeature("TOPSTRINGSIM", m.getCandidates().get(0).score, f);
        }
    }

    private void addMentionEmb(ELMention m, Map<String, Double> f){

        Double[] vec = we.getWordVector(m.getMention(), m.getLanguage());
        if(vec != null)
            for(int i = 0; i < vec.length; i++){
                addFeature("MENTIONEMBED"+i, vec[i], f);
            }
    }

    private void addPrevTag(List<ELMention> before, boolean train, Map<String, Double> f){
        if(before.size()>0){
//            if(train && before.get(0).is_ne_gold)
//                addFeature("BEFORE_IS_NE", 1.0, f);
//            if(!train && before.get(0).is_ne)
//                addFeature("BEFORE_IS_NE", 1.0, f);
            if(train)
                addFeature("BEFORETAG-"+before.get(0).getType(), 1.0, f);
            else
                addFeature("BEFORETAG-"+before.get(0).pred_type, 1.0, f);
        }
    }

    private void addPrevPrevTag(List<ELMention> before, boolean train, Map<String, Double> f){
        if(before.size()>1){
            if(train)
                addFeature("PREVPREVTAG-"+before.get(1).getType(), 1.0, f);
            else
                addFeature("PREVPREVTAG-"+before.get(1).pred_type, 1.0, f);
        }
    }

    private void addPrepBefore(List<ELMention> before, Map<String, Double> f){
        if(before.size()>0 && before.get(0).is_stop){
            addFeature("BEFORE-STOP", 1.0, f);
        }
    }

    private void addAfterTypes(List<ELMention> after, Map<String, Double> f, int k){
        if(after.size()>k){
            if(after.get(k).is_stop) return;
            String mid = after.get(k).getMid();
            if(!mid.startsWith("NIL")){
//                List<String> fbtypes = qm.lookupTypeFromMid(mid);
//                Set<String> tokenset = fbtypes.stream().flatMap(x -> Arrays.asList(x.toLowerCase().substring(1).split("/")).stream())
//                        .collect(toSet());
                Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                for(String t: tokenset)
                    if(alltype || t.equals("location") || t.equals("person") || t.equals("organization"))
                        addFeature("AFTERFBTYPETOKEN-"+t, 1.0, f);
//                for (String t : fbtypes)
//                    addFeature("AFTER:"+t, 1.0, f);
            }
        }
    }

    public void addAfterWikiTypes(List<ELMention> after, Map<String, Double> f, int k) {
        if(after.size()>k){
            if(after.get(k).is_stop) return;
            if(after.get(k).getCandidates().size() == 0) return;
            String lang = after.get(k).getCandidates().get(0).lang;
            String wiki_title = after.get(k).getCandidates().get(0).orig_title;
            if(wiki_title != null && !wiki_title.startsWith("NIL")){
                if(mono) {
                    List<String> cats = mws.getCategories(mws.formatTitle(wiki_title), lang);
                    Set<String> tokenset = cats.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                            .collect(toSet());
                    tokenset.removeAll(fo_stops);
                    for (String t : tokenset) {
                        addFeature("FOREIGNAFTERWIKICATTOKEN-" + t, 1.0, f);
                    }
                }
                else {
                    String en_title = wiki_title;
                    if (!lang.equals("en")) en_title = ll.translateToEn(wiki_title, lang);
                    if (en_title != null) {
                        List<String> cats = mws.getCategories(mws.formatTitle(en_title), "en");
                        Set<String> tokenset = cats.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                                .collect(toSet());
                        tokenset.removeAll(en_stops);
//                        tokenset.retainAll(en_stops);
                        for (String t : tokenset)
                            addFeature("AFTERWIKICATTOKEN-" + t, 1.0, f);
                    }
                }
            }
        }
    }

    private void addBeforeTypes(List<ELMention> ms, Map<String, Double> f, int k){
        if(ms.size()>k){
            if(ms.get(k).is_stop) {
                return;
            }
            String mid = ms.get(k).getMid();
            if(!mid.startsWith("NIL")){
//                List<String> fbtypes = qm.lookupTypeFromMid(mid);
//                Set<String> tokenset = fbtypes.stream().flatMap(x -> Arrays.asList(x.toLowerCase().substring(1).split("/")).stream())
//                        .collect(toSet());
                Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                for(String t: tokenset)
                    if(alltype || t.equals("location") || t.equals("person") || t.equals("organization"))
                        addFeature("BEFOREFBTYPETOKEN-"+t, 1.0, f);
//                for (String t : fbtypes)
//                    addFeature("BEFORE"+k+":"+t, 1.0, f);
            }
        }
    }

    public void addBeforeWikiTypes(List<ELMention> ms, Map<String, Double> f, int k) {
		if(ms.size()>k){
			if(ms.get(k).is_stop) return;
			if(ms.get(k).getCandidates().size() == 0){
                return;
            }
			String wiki_title = ms.get(k).getCandidates().get(0).orig_title;
            String lang = ms.get(k).getCandidates().get(0).lang;
            if(wiki_title != null && !wiki_title.startsWith("NIL")){
                if(mono) {
                    List<String> cats = mws.getCategories(mws.formatTitle(wiki_title), lang);
                    Set<String> tokenset = cats.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                            .collect(toSet());
                    tokenset.removeAll(fo_stops);
                    for (String t : tokenset) {
                        addFeature("FOREIGNBEFOREWIKICATTOKEN-" + t, 1.0, f);
                    }
                }
                else {
                    String en_title = wiki_title;
                    if (!lang.equals("en")) en_title = ll.translateToEn(wiki_title, lang);
                    if (en_title != null) {
                        List<String> cats = mws.getCategories(mws.formatTitle(en_title), "en");
                        Set<String> tokenset = cats.stream()
                                .flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                                .collect(toSet());
                        tokenset.removeAll(en_stops);
//                        tokenset.retainAll(en_stops);
                        for (String t : tokenset)
                            addFeature("BEFOREWIKICATTOKEN-" + t, 1.0, f);

                    }
                }
            }
		}
    }

    public void addTopWikiTypes(ELMention m, Map<String, Double> f) {
        if(m.getCandidates().size()>0){
            String wiki_title = m.getCandidates().get(0).orig_title;
            String lang = m.getCandidates().get(0).lang;
            if(wiki_title != null && !wiki_title.startsWith("NIL")){
                if(mono) {
                    List<String> cats = mws.getCategories(mws.formatTitle(wiki_title), lang);
//                        cats.forEach(x -> x = x.replaceAll("\\s+", "-").replaceAll(":", "-"));
//                        cats = cats.stream().map(x -> x.replaceAll("\\s+", "-").replaceAll(":", "-")).collect(Collectors.toList());
//                        cats.forEach(x -> addFeature("FOREIGNTOPWIKICAT-"+x,1.0,f));
                    Set<String> tokenset = cats.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                            .collect(toSet());
                    tokenset.removeAll(fo_stops);
                    for (String t : tokenset) {
                        addFeature("FOREIGNTOPWIKICATTOKEN-" + t, 1.0, f);
                    }
                }
                else {
                    String en_title = wiki_title;
                    if (!lang.equals("en")) en_title = ll.translateToEn(wiki_title, lang);
                    if (en_title != null) {
                        List<String> cats = mws.getCategories(mws.formatTitle(en_title), "en");
                        Set<String> tokenset = cats.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                                .collect(toSet());
                        tokenset.removeAll(en_stops);
//                        tokenset.retainAll(en_stops);
                        for (String t : tokenset) {
//                        System.out.println("\t\twiki:" + t);
                            addFeature("TOPWIKICATTOKEN-" + t, 1.0, f);
                        }
                    }
                }
            }
        }
    }

    public void addTopTitle(ELMention m, Map<String, Double> fm){
        if(!m.getWikiTitle().startsWith("NIL"))
            addFeature("TOPWIKITITLE-"+m.getWikiTitle(), 1, fm);

//        if(!m.getWikiTitle().startsWith("NIL")){
//            String en_title = ll.translateToEn(m.getWikiTitle(), m.getLanguage());
//            addFeature("TOPWIKITITLEEN-"+en_title, 1, fm);
//        }
        if(!m.getMid().startsWith("NIL"))
            addFeature("TOPMID-"+m.getMid(), 1, fm);
    }

    public void addTopTypes(ELMention m, Map<String, Double> f, boolean train) {
        try {
            String mid;
//            if(train && m.gold_mid!=null && !m.gold_mid.startsWith("NIL"))
//                mid = m.gold_mid;
//            else
            mid = m.getMid();

            if(!mid.startsWith("NIL")) {
//                List<String> fbtypes = qm.lookupTypeFromMid(mid);
//                Set<String> tokenset = fbtypes.stream().flatMap(x -> Arrays.asList(x.toLowerCase().substring(1).split("/")).stream())
//                        .collect(toSet());
                Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                for(String t: tokenset) {
//                    System.out.println("\t\tfb:"+t);
                    addFeature("TOPFBTYPETOKEN-" + t, 1.0, f);
                }
//                for (String t : fbtypes)
//                    addFeature(t, 1.0, f);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addAfterSecondTypes(List<ELMention> ms, Map<String, Double> f, int k) {
        try {
            if(ms.size()>k){
                ELMention m = ms.get(k);
                if(m.is_stop) {
                    return;
                }
                if(m.getCandidates().size()>1) {
                    String mid = m.getCandidates().get(1).title;
                    if(!mid.startsWith("NIL")) {
//                        List<String> fbtypes = qm.lookupTypeFromMid(mid);
//                        Set<String> tokenset = fbtypes.stream().flatMap(x -> Arrays.asList(x.toLowerCase().substring(1).split("/")).stream())
//                                .collect(toSet());
                        Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                        for(String t: tokenset)
                            if(alltype || t.equals("location") || t.equals("person") || t.equals("organization"))
                                addFeature("AFTERFBTYPETOKEN-"+t, 1.0, f);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBeforeSecondTypes(List<ELMention> ms, Map<String, Double> f, int k) {
        try {
            if(ms.size()>k){
                ELMention m = ms.get(k);
                if(m.is_stop) {
                    return;
                }
                if(m.getCandidates().size()>1) {
                    String mid = m.getCandidates().get(1).title;
                    if(!mid.startsWith("NIL")) {
//                        List<String> fbtypes = qm.lookupTypeFromMid(mid);
//                        Set<String> tokenset = fbtypes.stream().flatMap(x -> Arrays.asList(x.toLowerCase().substring(1).split("/")).stream())
//                                .collect(toSet());
                        Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                        for(String t: tokenset)
                            if(alltype || t.equals("location") || t.equals("person") || t.equals("organization"))
                                addFeature("BEFOREFBTYPETOKEN-"+t, 1.0, f);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addSecondTypes(ELMention m, Map<String, Double> f) {
        try {
            if(m.getCandidates().size()>1) {
                String mid = m.getCandidates().get(1).title;
                if(!mid.startsWith("NIL")) {
//                    List<String> fbtypes = qm.lookupTypeFromMid(mid);
//                    Set<String> tokenset = fbtypes.stream().flatMap(x -> Arrays.asList(x.toLowerCase().substring(1).split("/")).stream())
//                            .collect(toSet());
                    Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                    for(String t: tokenset)
                        addFeature("TOPFBTYPETOKEN-"+t, 1.0, f);
//                    for (String t : fbtypes)
//                        addFeature(t, 1.0, f);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addFeatureCombos1(List<ELMention> before, boolean train, Map<String, Double> fm){
        Map<String, Double> newf = new HashMap<>();
        if(before.size()>0){
            String tag;
            if(train)
                tag = "BEFORETAG-"+before.get(0).getType();
            else
                tag = "BEFORETAG-"+before.get(0).pred_type;
            for(String key: fm.keySet()){
                newf.put(tag+"_x_"+key, fm.get(key));
            }
            for(String key: newf.keySet())
                addFeature(key, newf.get(key), fm);
        }
    }

    private void addFeatureCombos(Map<String, Double> fm){
        List<String> keys = fm.keySet().stream().sorted((x1, x2) -> Integer.compare(lex.getFeatureID(x1), lex.getFeatureID(x2)))
//                .filter(x -> x.contains("WIKICAT") || x.contains("FBTYPE") || x.contains("TAG-"))
                .collect(toList());
        for(int i = 0; i < keys.size(); i++){
            for (int j = i + 1; j < keys.size(); j++) {
                String name = "COMBO_" + keys.get(i) + "_" + keys.get(j);
                double val = fm.get(keys.get(i)) * fm.get(keys.get(j));
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
