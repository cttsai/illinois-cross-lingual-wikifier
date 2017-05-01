package edu.illinois.cs.cogcomp.xlwikifier.mlner;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.core.StopWord;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.core.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.stanford.nlp.util.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 3/2/16.
 */
public class NERUtils {

    private final Logger logger = LoggerFactory.getLogger(NERUtils.class);
    public Set<String> stops;
    public String lang;
    private LangLinker ll;
    private WikiCandidateGenerator wcg;
    private NERFeatureManager fm;
    private Ranker ranker;
    private Map<String, Map<String, Double>> fcache = new HashMap<>();

    public NERUtils(String lang) {
        setLang(lang);
        ll = LangLinker.getLangLinker(lang);
        wcg = new WikiCandidateGenerator(lang, true);
        ranker = Ranker.loadPreTrainedRanker(lang, ConfigParameters.ranker_ner.get(lang));
        ranker.setNERMode(true);
    }

    public void setLang(String lang) {
        this.lang = lang;
        stops = StopWord.getStopWords(lang);
        fm = new NERFeatureManager(lang);
    }

    /**
     * Wikify n-grams and generate NER features
     * The main logic of Tsai et al., CoNLL 2016
     *
     * @param doc
     */
    public void wikifyNgrams(QueryDocument doc) {
        List<ELMention> prevm = new ArrayList<>();

        // up to 4-grams.
        for (int n = 4; n > 0; n--) {
            doc.mentions = getNgramMentions(doc, n);
            propFeatures(doc, prevm);
            extractNERFeatures(doc, n);
            prevm = doc.mentions;
        }
    }

    public void extractNERFeatures(QueryDocument doc, int n) {
        for (int j = 0; j < doc.mentions.size(); j++) {
            ELMention m = doc.mentions.get(j);

            String surface = m.getSurface().toLowerCase();

            if (fcache.containsKey(surface)) {
                m.ner_features = fcache.get(surface);
                continue;
            }

            if (m.ner_features.size() > 0) continue;
            if (NumberUtils.isNumber(surface.trim())) continue;

            generateTitleCandidates(m, n);
        }

        if(ConfigParameters.no_ranking_in_ner)
            ranker.setWikiTitleByTopCand(doc);
        else
            ranker.setWikiTitleByModel(doc);

        for(int j = 0; j < doc.mentions.size(); j++){
            ELMention m = doc.mentions.get(j);
            setMidByWikiTitle(m);
            Map<String, Double> map = getFeatureMap(doc.mentions, j, true);
            for (String key : map.keySet()) {
                m.ner_features.put(key, map.get(key));
            }
//            fcache.put(m.getSurface().toLowerCase(), m.ner_features);
        }
    }

    private Map<String, Double> getFeatureMap(List<ELMention> mentions, int idx, boolean train) {
        int window = 3;
        List<ELMention> before_mentions = new ArrayList<>();
        List<ELMention> after_mentions = new ArrayList<>();
        ELMention m = mentions.get(idx);
        for (int i = idx - 1; i >= 0 && before_mentions.size() < window; i--) {
            before_mentions.add(mentions.get(i));
        }
        for (int i = idx + 1; i < mentions.size() && after_mentions.size() < window; i++) {
            after_mentions.add(mentions.get(i));
        }
        return fm.getFeatureMap(m, before_mentions, after_mentions, train);
    }

    public List<ELMention> getNgramMentions(QueryDocument doc, int n) {
        List<ELMention> mentions = new ArrayList<>();
        TextAnnotation ta = doc.getTextAnnotation();
        if (ta == null) return mentions;
        for (int i = 0; i < ta.getTokens().length - n + 1; i++) {
            if (ta.getSentenceId(i) != ta.getSentenceId(i + n - 1))
                continue;
            IntPair offset = ta.getTokenCharacterOffset(i);
            IntPair offset1 = ta.getTokenCharacterOffset(i + n - 1);
            ELMention m = new ELMention(doc.getDocID(), offset.getFirst(), offset1.getSecond());
            String token = ta.getToken(i);
            int idx = token.indexOf("'");
            if (idx >= 0) token = token.substring(0, idx);
            String surface = token;
            for (int j = i + 1; j < i + n; j++) {
                token = ta.getToken(j);
                idx = token.indexOf("'");
                if (idx >= 0) token = token.substring(0, idx);
                if (lang.equals("zh"))
                    surface += token;
                else
                    surface += " " + token;
            }
//            if(surface.trim().isEmpty()) continue;
            m.setSurface(surface);
            m.ngram = n;
            m.is_stop = false;
            List<String> tokens = Arrays.asList(surface.toLowerCase().split("\\s+"));
            if (tokens.stream().filter(x -> stops.contains(x)).count() == tokens.size())
                m.is_stop = true;
            m.is_ne_gold = false;
            m.setType("O");

            m.pred_type = "O";
            mentions.add(m);
        }

        mentions = mentions.stream().sorted(Comparator.comparingInt(ELMention::getStartOffset))
                .collect(toList());
        mentions.forEach(x -> x.setLanguage(lang));
        return mentions;
    }

    /**
     * Generate title candidates for n-grams
     * This is different from the algorithm in WikiCandGenerator, since not all n-grams are entities
     * @param m
     * @param n
     */
    public void generateTitleCandidates(ELMention m, int n) {
        if (!m.getWikiTitle().startsWith("NIL")) return;
        if (m.ner_features.size() > 0) return;

        String surface = m.getSurface().toLowerCase();
        List<WikiCand> cands = wcg.getCandsBySurface(surface);

        if (cands.size() == 0 && n == 1 && surface.length() > 0) {
            if (StringUtils.isCapitalized(m.getSurface())) {
                cands.addAll(wcg.getCandidateByWord(surface, 6));
            }
        }

        if (cands.size() == 0 && !lang.equals("en")) {
            if (n > 1) {
                cands.addAll(wcg.en_generator.getCandsBySurface(surface));
            } else if (surface.length() > 0) {
                if (StringUtils.isCapitalized(m.getSurface())) {
                    cands.addAll(wcg.en_generator.getCandsBySurface(surface));
                    if (cands.size() == 0)
                        cands.addAll(wcg.en_generator.getCandidateByWord(surface, 6));
                }
            }
        }

        m.setCandidates(cands);

    }

    public void setEnWikiTitle(QueryDocument doc){
        for (ELMention m : doc.mentions) {
            if (!m.getWikiTitle().equals("NIL") && m.getEnWikiTitle().startsWith("NIL")) {
                if (!lang.equals("en"))
                    m.setEnWikiTitle(ll.translateToEn(m.getWikiTitle(), lang));
                else
                    m.setEnWikiTitle(m.getWikiTitle());
            }
        }

    }

    public String translateToEn(String str) {
        return ll.translateToEn(str, lang);
    }

    public void setMidByWikiTitle(ELMention m) {
        if (m.getWikiTitle().startsWith("NIL") && m.getEnWikiTitle().startsWith("NIL")) {
            m.setMid("NIL");
        }

        for (WikiCand c : m.getCandidates()) {
            String mid = getMidByWikiTitle(c.getTitle(), c.lang);
            c.orig_title = c.title;
            if (mid != null) {
//                List<String> types = FreeBaseQuery.getTypesFromMid(mid);
                c.title = mid;
                if(m.getMid().startsWith("NIL")) {
                    m.setMid(mid);
                }
            }
            else
                c.title = "NIL";
        }
    }

    public void setMidByWikiTitle(QueryDocument doc) {
        doc.mentions.forEach(x -> setMidByWikiTitle(x));
    }

    public String getMidByWikiTitle(String title, String lang) {
        if (title.trim().isEmpty())
            return null;
        if (title.startsWith("NIL"))
            return null;
        String fblang = lang;
        if (lang.equals("zh")) fblang = "zh-cn";

        String mid = FreeBaseQuery.getMidFromTitle(formatTitle(title), fblang);
        if (mid != null) return mid; // in the m.12345 format

        mid = FreeBaseQuery.getMidFromTitle(formatTitle(title), "en");
        if (mid != null) return mid;

        String ent = ll.translateToEn(title, lang);
        if (ent != null) {
            ent = formatTitle(ent);
            mid = FreeBaseQuery.getMidFromTitle(ent, "en");
            if (mid != null) return mid;
        }
        return null;
    }

    public String formatTitle(String title) {
        String tmp = "";
        for (String token : title.split("_")) {
            if (token.length() == 0) continue;
            tmp += token.substring(0, 1).toUpperCase();
            if (token.length() > 1)
                tmp += token.substring(1, token.length());
            tmp += "_";
        }
        return tmp.substring(0, tmp.length() - 1);
    }

    public void propFeatures(QueryDocument doc, List<ELMention> prevms) {
        if (prevms == null) return;
        int pidx;
        for (ELMention m : doc.mentions) {
            for (pidx = 0; pidx < prevms.size(); pidx++) {
                ELMention pm = prevms.get(pidx);
                if (pm.getStartOffset() >= m.getEndOffset()) break;

                if (m.getStartOffset() >= pm.getStartOffset() &&
                        m.getEndOffset() <= pm.getEndOffset()) {

                    String BI = "";
                    if (m.getStartOffset() > pm.getStartOffset()) BI = "I-";
                    else BI = "B-";

                    for (String fname : pm.ner_features.keySet()) {
                        if (fname.contains("TOPFBTYPETOKEN-person"))
                            m.ner_features.put(BI + "TOPFBTYPETOKEN-person", 1.0);
                        if (fname.contains("TOPFBTYPETOKEN-location"))
                            m.ner_features.put(BI + "TOPFBTYPETOKEN-location", 1.0);
                        if (fname.contains("TOPFBTYPETOKEN-organization"))
                            m.ner_features.put(BI + "TOPFBTYPETOKEN-organization", 1.0);
                    }
                }
            }
        }
    }
}
