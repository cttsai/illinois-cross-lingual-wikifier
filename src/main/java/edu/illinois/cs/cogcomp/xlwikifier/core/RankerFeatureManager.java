package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
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
    public WordEmbedding we;
    private String lang;
    private LangLinker ll;
    public TFIDFManager tfidf = new TFIDFManager();
    public String context_lang;
    public TextAnnotationBuilder tokenizer;
    public boolean use_foreign_title = true;
    public boolean ner_mode = false;

    public RankerFeatureManager(String lang) {
        this.lang = lang;
        this.context_lang = lang;
        ll = LangLinker.getLangLinker(lang);
        we = new WordEmbedding();
        lex = new LexManager();

        tokenizer = MultiLingualTokenizer.getTokenizer(context_lang);
        loadVectors();
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public void loadVectors() {
        if (lang.equals("en")) {
//            we.setMonoVecsNew("en");
            we.loadDB("es", true);
        } else {
            we.loadDB(lang, true);
        }
    }

    public String getLang() {
        return this.lang;
    }

    public FeatureVector getTitleFV(ELMention m, WikiCand cand, QueryDocument doc) {

        Map<String, float[]> title_vec = new HashMap<>();

        String flang = null;

        // set the embeddings of wikipedia titles (en titles and/or foreign titles)
        if (cand.lang.equals("en")) {
            title_vec.put("en", we.getTitleVector(cand.getTitle(), "en"));
            if (!context_lang.equals("en")) {
                flang = context_lang;
                String t = ll.translateFromEn(cand.getTitle(), context_lang);
                title_vec.put(context_lang, we.getTitleVector(t, context_lang));
            } else
                use_foreign_title = false;
        } else {
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
        if (!ner_mode) {
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
        if (use_foreign_title)
            addCosin(m.context30, title_vec.get(flang), featureMap, "W_CON30_L");
        addCosin(m.context100, title_vec.get("en"), featureMap, "W_CON100");
        if (use_foreign_title)
            addCosin(m.context100, title_vec.get(flang), featureMap, "W_CON100_L");
        addCosin(m.context200, title_vec.get("en"), featureMap, "W_CON200");
        if (use_foreign_title)
            addCosin(m.context200, title_vec.get(flang), featureMap, "W_CON200_L");


        addFeatureCombos(featureMap);
        cand.ranker_feats = featureMap;
        FeatureVector fv = lex.convertRawFeaMap2LRFeatures(featureMap);
        fv.sort();
        return fv;
    }

    private void addFeatureCombos(Map<String, Double> fm) {

        List<String> keys = fm.keySet().stream().sorted((x1, x2) -> Integer.compare(lex.getFeatureID(x1), lex.getFeatureID(x2))).collect(toList());
        for (int i = 0; i < keys.size(); i++) {
            for (int j = i + 1; j < keys.size(); j++) {
                String name = "COMBO_" + keys.get(i) + "_" + keys.get(j);
                double val = fm.get(keys.get(i)) * fm.get(keys.get(j));
                addFeature(name, val, fm);
            }
        }
    }


    public float[] getTitleAvg(List<ELMention> pms) {
        List<float[]> vecs = new ArrayList<>();
        for (ELMention pm : pms) {
            if (pm.getMidVec() != null) {
                vecs.add(pm.getMidVec());
            }
        }
        return we.averageVectors(vecs);
    }

    private void addMaxMinCosine(float[] title_vec, List<float[]> vecs, Map<String, Double> f, String name) {
        double min_score = 0, max_score = 0;
        if (title_vec != null) {
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


    private void addCosin(float[] ne_vec, float[] title_vec, Map<String, Double> f, String name) {
        double score = 0.0;
        if (title_vec != null)
            score = we.cosine(ne_vec, title_vec);
        addFeature(name, score, f);
    }

    private void addTitleProb(WikiCand cand, Map<String, Double> f) {
        addFeature("TITLEPROB", cand.ptgivens, f);
    }


    private void addSurfaceProb(WikiCand cand, Map<String, Double> f) {
        addFeature("SURFACEPROB", cand.psgivent, f);
    }

    public double getNorm(Double[] vec) {
        double s = 0;
        for (double v : vec)
            s += v * v;
        return Math.sqrt(s);
    }


    public void addFeature(String featureName, double val,
                           Map<String, Double> featureMap) {
        featureMap.put(featureName, val);
        if (!lex.containFeature(featureName) && lex.isAllowNewFeatures()) {
            lex.previewFeature(featureName);
        }
    }

    public float[] getWeightedContextVector(ELMention m, QueryDocument doc, int window) {

        TextAnnotation ta = doc.getTextAnnotation();

        int tid_start = ta.getTokenIdFromCharacterOffset(m.getStartOffset());
        int tid_end = ta.getTokenIdFromCharacterOffset(m.getEndOffset()-1)+1;

        List<String> words = new ArrayList<>();
        for(int i = tid_start -1; i >= 0 && i >= tid_start-window; i--){
            words.add(ta.getToken(i));
        }

        for(int i = tid_end; i < ta.getTokens().length && i < tid_end + window; i++){
            words.add(ta.getToken(i));
        }

        return getWeightedVectorFromWords(words, context_lang);
    }

    public List<String> getTokens(String text) {
        if (text.trim().isEmpty()) return new ArrayList<>();
        TextAnnotation ta = null;
        try {
            ta = tokenizer.createTextAnnotation(text);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
        if (ta == null)
            return new ArrayList<>();

        return Arrays.asList(ta.getTokens());
    }


    public float[] getWeightedVectorFromWords(List<String> words, String lang) {
        Map<String, Float> w2tfidf = tfidf.getWordWeights(words, lang);

        List<float[]> vecs = new ArrayList<>();
        List<Float> weights = new ArrayList<>();
        for (String w : w2tfidf.keySet()) {
            float[] vec = we.getWordVector(w, lang);
            if (vec != null) {
                vecs.add(vec);
                weights.add(w2tfidf.get(w));
            }
        }
        float[] ret = we.averageVectors(vecs, weights);
        return ret;

    }


    /**
     * This block of features are used when the candidates are generated by the transliterated mentions
     */

    private void addIsTop(WikiCand cand, Map<String, Double> f) {
        if (cand.top)
            addFeature("ISTOP", 1.0, f);
    }

    private void addSrc(WikiCand cand, Map<String, Double> f) {
        addFeature("SRC:" + cand.src, 1.0, f);
    }

    private void addPreScore(WikiCand cand, Map<String, Double> f) {
        addFeature("PRESCORE", cand.score, f);
    }

    private void addCharBigramDice(WikiCand cand, ELMention m, Map<String, Double> f) {

        String query = m.getSurface().toLowerCase();
        String title = cand.title.replaceAll("_", " ");

        Set<String> querybi = getCharBigram(query);
        Set<String> titlebi = getCharBigram(title);
        double score = jaccard(querybi, titlebi);
        addFeature("BIDICE", score, f);
    }

    private void addCharDice(WikiCand cand, ELMention m, Map<String, Double> f) {

        String query = m.getSurface().toLowerCase();
        String title = cand.title.replaceAll("_", " ");

        Set<String> queryset = getCharSet(query);
        Set<String> titleset = getCharSet(title);
        double score = jaccard(queryset, titleset);
        addFeature("CHARDICE", score, f);
    }

    private double jaccard(Set<String> s1, Set<String> s2) {
        Set<String> inter = new HashSet<>(s1);
        inter.retainAll(s2);
        s2.addAll(s1);
        return (double) inter.size() / s2.size();
    }

    private Set<String> getCharSet(String str) {
        Set<String> ret = new HashSet<>();
        str = str.replaceAll("\\s+", "");
        for (int i = 0; i < str.length(); i++)
            ret.add(str.substring(i, i + 1));
        return ret;
    }

    private Set<String> getCharBigram(String str) {
        Set<String> ret = new HashSet<>();
        for (String token : str.split("\\s+")) {
            for (int i = 0; i < token.length() - 1; i++) {
                ret.add(token.substring(i, i + 2));
            }
        }
        return ret;
    }

    private void addInitialMatch(WikiCand cand, ELMention m, Map<String, Double> f) {

        String query, title;
//        if(cand.orig_title!=null){
//            query = m.getSurface();
//            title = cand.orig_title.replaceAll("_", " ");
//        }
//        else{
        query = m.getSurface();
        title = cand.getTitle().replaceAll("_", " ");
//        }

        String mi = getInitial(query.toLowerCase());
        String ci = getInitial(title);
        if (mi.equals(ci))
            addFeature("INITMATCH", 1, f);
        if (mi.startsWith(ci) || ci.startsWith(mi))
            addFeature("INITPARTIALMATCH", 1, f);
    }

    private void addNumTokenMatch(WikiCand cand, ELMention m, Map<String, Double> f) {

        int mn = m.getSurface().split("\\s+").length;
        int cn = cand.getTitle().replaceAll("_", " ").split("\\s+").length;

        if (mn == cn)
            addFeature("NTOKENMATCH", 1, f);

    }

    private String getInitial(String s) {
        String[] tokens = s.split("\\s+");
        String ret = "";
        for (String token : tokens) {
            ret += token.charAt(0);
        }
        return ret.toLowerCase();
    }
}

