package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import edu.illinois.cs.cogcomp.indsup.learning.LexManager;

import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.*;

/**
 */
public class RankerFeatureManager implements Serializable {
    private static final long serialVersionUID = -1932878634118945538L;
    public LexManager lex;
    public WordEmbeddingFloat we;
    private String lang;
    private LangLinker ll = new LangLinker();
    public boolean mono_mode = false;
    public boolean word_align_mode = false;
    public TFIDFManager tfidf = new TFIDFManager();
    public String context_lang;
    public Tokenizer tokenizer;
    public boolean use_foreign_title = true;
    public boolean ner_mode = false;

    public RankerFeatureManager(String lang){
        this.lang = lang;
        this.context_lang = lang;
        we = new WordEmbeddingFloat();
        lex = new LexManager();

        tokenizer = MultiLingualTokenizer.getTokenizer(context_lang);
        loadVectors();
    }

    public void setLang(String lang){
        this.lang = lang;
    }

    public void loadVectors(){
        if(lang.equals("en")){
            we.setMonoVecsNew("en");
        }
        else if(mono_mode) {
            we.setMonoVecsNew(lang);
        }
        else if(word_align_mode){
            we.loadMultiWordDB(lang);
        }
        else {
            we.loadMultiDBNew(lang);
        }
    }

    public String getLang(){
        return this.lang;
    }

    public FeatureVector getTitleFV(ELMention m, WikiCand cand, QueryDocument doc){

        Map<String, Float[]> title_vec = new HashMap<>();

        String flang = null;

        // set the embeddings of wikipedia titles (en titles and/or foreign titles)
        if(cand.lang.equals("en")){
            title_vec.put("en", we.getTitleVector(cand.getTitle(), "en"));
            if(!context_lang.equals("en")) {
                flang = context_lang;
                String t = ll.translateFromEn(cand.getTitle(), context_lang);
                title_vec.put(context_lang, we.getTitleVector(t, context_lang));
            }
            else
                use_foreign_title = false;
        }
        else{
            flang = cand.lang;
            title_vec.put(cand.lang, we.getTitleVector(cand.getTitle(), cand.lang));
            String t = ll.translateToEn(cand.getTitle(), cand.lang);
            title_vec.put("en", we.getTitleVector(t, "en"));
        }

        Map<String, Double> featureMap = new HashMap<>();
        addTitleProb(cand, featureMap); // naacl paper
        addSurfaceProb(cand, featureMap); // naacl paper


        // for transliteration
//        addPreScore(cand, featureMap);

//        addInitialMatch(cand, m, featureMap);
//        addNumTokenMatch(cand, m, featureMap);
//        addCharBigramDice(cand, m, featureMap);
//        addCharDice(cand, m, featureMap);

//        addFBTypes(cand, featureMap);
//        addSrc(cand, featureMap);
//        addIsTop(cand, featureMap);

        // naacl paper
        if(!ner_mode) {
            addCosin(m.other_ne, title_vec.get("en"), featureMap, "NE_TITLE_COS");
            if (use_foreign_title)
                addCosin(m.other_ne, title_vec.get(flang), featureMap, "NE_TITLE_COS_L");
            addMaxMinCosine(title_vec.get("en"), m.other_ne_vecs, featureMap, "NE_MINMAX");
            if (use_foreign_title)
                addMaxMinCosine(title_vec.get(flang), m.other_ne_vecs, featureMap, "NE_MINMAX_L");
            addCosin(m.pre_title, title_vec.get("en"), featureMap, "PRETITLE_COS");
            if (use_foreign_title)
                addCosin(m.pre_title, title_vec.get(flang), featureMap, "PRETITLE_COS_L");
            addMaxMinCosine(title_vec.get("en"), m.pre_title_vecs, featureMap, "PRE_MINMAX");
            if (use_foreign_title)
                addMaxMinCosine(title_vec.get(flang), m.pre_title_vecs, featureMap, "PRE_MINMAX_L");
        }
        addCosin(m.context30, title_vec.get("en"), featureMap, "W_CON30");
        if(use_foreign_title)
            addCosin(m.context30, title_vec.get(flang), featureMap, "W_CON30_L");
        addCosin(m.context100, title_vec.get("en"), featureMap, "W_CON100");
        if(use_foreign_title)
            addCosin(m.context100, title_vec.get(flang), featureMap, "W_CON100_L");
        addCosin(m.context200, title_vec.get("en"), featureMap, "W_CON200");
        if(use_foreign_title)
            addCosin(m.context200, title_vec.get(flang), featureMap, "W_CON200_L");


        addFeatureCombos(featureMap);
        cand.ranker_feats = featureMap;
        FeatureVector fv = lex.convertRawFeaMap2LRFeatures(featureMap);
        fv.sort();
        return fv;
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



    public Float[] getTitleAvg(List<ELMention> pms) {
        List<Float[]> vecs = new ArrayList<>();
        for (ELMention pm : pms) {
            if (pm.getMidVec() != null) {
                vecs.add(pm.getMidVec());
            }
        }
        return we.averageVectors(vecs);
    }

//    public Double[] getTitleVector(String title, String lang){
//        title = "title_"+title.replaceAll(" ", "_").toLowerCase();
//        String key = title+"_"+lang;
//        if(title_vec_cache.containsKey(key))
//            return title_vec_cache.get(key);
//
//        Double[] ret = new Double[dim];
//        Arrays.fill(ret, 0.0);
//        if(word2vec.get(lang).containsKey(title)){
//            ret = word2vec.get(lang).get(title);
//        }
//        title_vec_cache.put(key, ret);
//        return ret;
//    }

//    public Double[] getTitleWeightedAvg(List<ELMention> pms){
//        List<String> titles = new ArrayList<>();
//        for(ELMention pm: pms){
//            if(pm.getMidVec()!=null) {
//                titles.add(pm.getWikiTitle());
//            }
//        }
//
////        Map<String, Double> t2w = tfidf.getWordWeights(titles, lang);
//        Map<String, Long> t2w = titles.stream().collect(groupingBy(x -> x, counting()));
//        List<Double[]> vecs = new ArrayList<>();
//        List<Double> weights = new ArrayList<>();
//        for(String t: t2w.keySet()){
//            for(ELMention pm: pms){
//                if(pm.getWikiTitle().equals(t)){
//                    vecs.add(pm.getMidVec());
//                    weights.add((double)t2w.get(t));
//                    break;
//                }
//            }
//        }
//        return we.averageVectors(vecs, weights);
//    }


//    private Double[] getTitleAverage(List<ELMention> ms){
//
//        List<Double[]> vecs = new ArrayList<>();
//        for(ELMention m: ms){
//            if(!m.getMid().startsWith("NIL")) {
//                Double[] v = getMidVectorByWikiTitle(m.getMid(), "en");
//                vecs.add(v);
//            }
//        }
//        return we.averageVectors(vecs);
//    }

    private void addMaxMinCosine(Float[] title_vec, List<Float[]> vecs, Map<String, Double> f, String name){
        double min_score = 0, max_score = 0;
        if(title_vec != null) {
            List<Float> cos = vecs.stream().map(x -> we.cosine(title_vec, x)).collect(toList());
            OptionalDouble max = cos.stream().mapToDouble(x -> x).max();
            if (max.isPresent())
                max_score = max.getAsDouble();
            OptionalDouble min = cos.stream().mapToDouble(x -> x).min();
            if (min.isPresent())
                min_score = min.getAsDouble();
        }
        addFeature(name + "_MAX", max_score, f);
        addFeature(name + "_MIN", min_score, f);
    }


    private void addCosin(Float[] ne_vec, Float[] title_vec, Map<String, Double> f, String name){
        double score = 0.0;
        if(title_vec != null)
            score = we.cosine(ne_vec, title_vec);
        addFeature(name, score, f);
    }

    private void addTitleProb(WikiCand cand, Map<String, Double> f){
        addFeature("TITLEPROB", cand.ptgivens, f);
    }


    private void addSurfaceProb(WikiCand cand, Map<String, Double> f){
        addFeature("SURFACEPROB", cand.psgivent, f);
    }

    private void addProduct(Double[] v1, Double[] v2, Map<String, Double> f, String name){
        double n1 = getNorm(v1);
        double n2 = getNorm(v2);
        for(int i = 0; i < v1.length; i++){
//            if(v1[i]*v2[i]>0)
//                addFeature(name+i, 1.0 ,f);
//            else
//                addFeature(name+i, -1.0, f);
            if(n1!=0 && n2!=0) {
                addFeature(name + i, v1[i]/n1  * (v2[i]/n2 ), f);
            }
        }

    }

    public double getNorm(Double[] vec){
        double s = 0;
        for(double v: vec)
            s += v*v;
        return Math.sqrt(s);
    }


    public void addFeature(String featureName, double val,
                           Map<String, Double> featureMap) {
        featureMap.put(featureName, val);
        if (!lex.containFeature(featureName) && lex.isAllowNewFeatures()) {
            lex.previewFeature(featureName);
        }
    }

    public Float[] getWeightedContextVector(ELMention m, QueryDocument doc, int window){

        int start_off = m.getStartOffset();
        int end_off = m.getEndOffset();
        String text = doc.plain_text;

        int start = Math.max(0, start_off-window);
        int end = Math.min(text.length(), end_off+window);

        String context = text.substring(start, start_off).trim()+" "
                +text.substring(end_off, end).trim();
        context = context.toLowerCase().replaceAll(",", "")
                .replaceAll("\\.", "").replaceAll("'s", "").replaceAll("\n", " ");
        List<String> words = getTokens(context);
        return getWeightedVectorFromWords(words, context_lang);
//        return getWeightedVectorFromWords(words, lang, doc.word_weights);
    }

    public List<String> getTokens(String text){
        if(text.trim().isEmpty()) return new ArrayList<>();
        TextAnnotation ta = null;
        try {
            ta = tokenizer.getTextAnnotation(text);
        }
        catch (Exception e){
            e.printStackTrace();
            return new ArrayList<>();
        }
        if(ta == null)
            return new ArrayList<>();

        return Arrays.asList(ta.getTokens());
    }



    public Float[] getWeightedVectorFromWords(List<String> words, String lang){
        Map<String, Float> w2tfidf = tfidf.getWordWeights(words, lang);

        List<Float[]> vecs = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        for(String w: w2tfidf.keySet()){
            Float[] vec = we.getWordVector(w, lang);
            if(vec != null) {
                vecs.add(vec);
                weights.add(w2tfidf.get(w));
            }
        }
        Float[] ret = we.averageVectors(vecs, weights);
        return ret;

    }


    /**
     * This block of features are used when the candidates are generated by the transliterated mentions
     */

    private void addIsTop(WikiCand cand, Map<String, Double> f){
        if(cand.top)
            addFeature("ISTOP", 1.0, f);
    }

    private void addSrc(WikiCand cand, Map<String, Double> f){
        addFeature("SRC:"+cand.src, 1.0, f);
    }

    private void addPreScore(WikiCand cand, Map<String, Double> f){
        addFeature("PRESCORE", cand.score, f);
    }

    private void addCharBigramDice(WikiCand cand, ELMention m, Map<String, Double> f){

        String query = m.getMention().toLowerCase();
        String title = cand.title.replaceAll("_", " ");

        Set<String> querybi = getCharBigram(query);
        Set<String> titlebi = getCharBigram(title);
        double score = jaccard(querybi, titlebi);
        addFeature("BIDICE", score, f);
    }

    private void addCharDice(WikiCand cand, ELMention m, Map<String, Double> f){

        String query = m.getMention().toLowerCase();
        String title = cand.title.replaceAll("_", " ");

        Set<String> queryset = getCharSet(query);
        Set<String> titleset = getCharSet(title);
        double score = jaccard(queryset, titleset);
        addFeature("CHARDICE", score, f);
    }

    private double jaccard(Set<String> s1, Set<String> s2){
        Set<String> inter = new HashSet<>(s1);
        inter.retainAll(s2);
        s2.addAll(s1);
        return (double) inter.size()/s2.size();
    }

    private Set<String> getCharSet(String str){
        Set<String> ret = new HashSet<>();
        str = str.replaceAll("\\s+","");
        for(int i = 0; i < str.length(); i++)
            ret.add(str.substring(i, i+1));
        return ret;
    }

    private Set<String> getCharBigram(String str){
        Set<String> ret = new HashSet<>();
        for(String token: str.split("\\s+")){
            for(int i = 0; i < token.length()-1; i++){
                ret.add(token.substring(i, i+2));
            }
        }
        return ret;
    }

    private void addInitialMatch(WikiCand cand, ELMention m, Map<String, Double> f){

        String query, title;
//        if(cand.orig_title!=null){
//            query = m.getMention();
//            title = cand.orig_title.replaceAll("_", " ");
//        }
//        else{
        query = m.getMention();
        title = cand.getTitle().replaceAll("_", " ");
//        }

        String mi = getInitial(query.toLowerCase());
        String ci = getInitial(title);
        if(mi.equals(ci))
            addFeature("INITMATCH", 1, f);
        if(mi.startsWith(ci) || ci.startsWith(mi))
            addFeature("INITPARTIALMATCH", 1, f);
    }

    private void addNumTokenMatch(WikiCand cand, ELMention m, Map<String, Double> f){

        int mn = m.getMention().split("\\s+").length;
        int cn = cand.getTitle().replaceAll("_", " ").split("\\s+").length;

        if(mn == cn)
            addFeature("NTOKENMATCH", 1, f);

    }

    private String getInitial(String s){
        String[] tokens = s.split("\\s+");
        String ret = "";
        for(String token: tokens){
            ret += token.charAt(0);
        }
        return ret.toLowerCase();
    }

    /**
     * This block experiments different title representations
     */
//    public Double[] getDocVector(QueryDocument doc, String lang){
//        if(doc_vec_cache.containsKey(doc.getDocID()))
//            return doc_vec_cache.get(doc.getDocID());
//        List<String> words = getDocWords(doc, lang);
////        List<String> words = getDocWords(doc);
//        Double[] vec = we.getVectorFromWords(words, lang);
//        doc_vec_cache.put(doc.getDocID(), vec);
//        return vec;
//    }

    public List<String> getDocWords(QueryDocument doc){
        return Arrays.asList(doc.getTextAnnotation().getTokens());
    }

    public List<String> getDocWords(QueryDocument doc, String lang){
        String text = null;

        text = doc.plain_text.toLowerCase();

        text = text.toLowerCase().replaceAll(",","").replaceAll("\\.", "")
                .replaceAll("'s", "").replaceAll("\n", " ");
        List<String> words = Arrays.asList(text.split("\\s+")).stream().collect(toList());
        return words;
    }
}
