package edu.illinois.cs.cogcomp.xlwikifier.mlner;

import edu.illinois.cs.cogcomp.indsup.learning.LexManager;
import edu.illinois.cs.cogcomp.xlwikifier.core.StopWord;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.MediaWikiSearch;

import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.toSet;

public class NERFeatureManager implements Serializable {
    private static final long serialVersionUID = -1932878634118945538L;
    private LexManager lex;
    private LangLinker ll;
    private Set<String> en_stops;
    private Set<String> fo_stops;
    private boolean alltype = true;
    private boolean mono = false;

    public NERFeatureManager(String lang) {
        lex = new LexManager();
        ll = LangLinker.getLangLinker(lang);
        en_stops = StopWord.getStopWords("en");
        fo_stops = StopWord.getStopWords(lang);
    }


    public Map<String, Double> getFeatureMap(ELMention mention, List<ELMention> before_mentions, List<ELMention> after_mentions, boolean train) {
        Map<String, Double> featureMap = new HashMap<>();
        if (mention.is_stop || (mention.getMid().startsWith("NIL") && mention.getWikiTitle().startsWith("NIL"))) {
            addStopWord(mention, featureMap);
        } else {
//            addTopWikiTypes(mention, featureMap);
//            addBeforeWikiTypes(before_mentions, featureMap, 0);
//            addAfterWikiTypes(after_mentions, featureMap, 0);

            addTopTypes(mention, featureMap, train);
            addBeforeTypes(before_mentions, featureMap, 0);
            addAfterTypes(after_mentions, featureMap, 0);
            addSecondTypes(mention, featureMap);
            addBeforeSecondTypes(before_mentions, featureMap, 0);
            addAfterSecondTypes(after_mentions, featureMap, 0);
        }

        return featureMap;
    }

    private void addIsNE(ELMention m, Map<String, Double> fm) {
        addFeature("ISNEFROMEPREVIOUS-" + m.pred_type, 1, fm);
        addFeature("ISNEFROMEPREVIOUS-" + m.pred_type.substring(2), 1, fm);
    }

    private void addStopWord(ELMention m, Map<String, Double> fm) {
        if (m.is_stop)
            addFeature("ISSTOPWORD", 1, fm);
    }

    private void addAfterTypes(List<ELMention> after, Map<String, Double> f, int k) {
        if (after.size() > k) {
            if (after.get(k).is_stop) return;
            String mid = after.get(k).getMid();
            if (!mid.startsWith("NIL")) {
                Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                for (String t : tokenset)
                    if (alltype || t.equals("location") || t.equals("person") || t.equals("organization"))
                        addFeature("AFTERFBTYPETOKEN-" + t, 1.0, f);
            }
        }
    }

    public void addAfterWikiTypes(List<ELMention> after, Map<String, Double> f, int k) {
        if (after.size() > k) {
            if (after.get(k).is_stop) return;
            if (after.get(k).getCandidates().size() == 0) return;
            String lang = after.get(k).getCandidates().get(0).lang;
            String wiki_title = after.get(k).getCandidates().get(0).orig_title;
            if (wiki_title != null && !wiki_title.startsWith("NIL")) {
                if (mono) {
                    List<String> cats = MediaWikiSearch.getCategories(wiki_title, lang);
                    Set<String> tokenset = cats.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                            .collect(toSet());
                    tokenset.removeAll(fo_stops);
                    for (String t : tokenset) {
                        addFeature("FOREIGNAFTERWIKICATTOKEN-" + t, 1.0, f);
                    }
                } else {
                    String en_title = wiki_title;
                    if (!lang.equals("en")) en_title = ll.translateToEn(wiki_title, lang);
                    if (en_title != null) {
                        List<String> cats = MediaWikiSearch.getCategories(en_title, "en");
                        Set<String> tokenset = cats.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                                .collect(toSet());
                        tokenset.removeAll(en_stops);
                        for (String t : tokenset)
                            addFeature("AFTERWIKICATTOKEN-" + t, 1.0, f);
                    }
                }
            }
        }
    }

    private void addBeforeTypes(List<ELMention> ms, Map<String, Double> f, int k) {
        if (ms.size() > k) {
            if (ms.get(k).is_stop) {
                return;
            }
            String mid = ms.get(k).getMid();
            if (!mid.startsWith("NIL")) {
                Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                for (String t : tokenset)
                    if (alltype || t.equals("location") || t.equals("person") || t.equals("organization"))
                        addFeature("BEFOREFBTYPETOKEN-" + t, 1.0, f);
            }
        }
    }

    public void addBeforeWikiTypes(List<ELMention> ms, Map<String, Double> f, int k) {
        if (ms.size() > k) {
            if (ms.get(k).is_stop) return;
            if (ms.get(k).getCandidates().size() == 0) {
                return;
            }
            String wiki_title = ms.get(k).getCandidates().get(0).orig_title;
            String lang = ms.get(k).getCandidates().get(0).lang;
            if (wiki_title != null && !wiki_title.startsWith("NIL")) {
                if (mono) {
                    List<String> cats = MediaWikiSearch.getCategories(wiki_title, lang);
                    Set<String> tokenset = cats.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                            .collect(toSet());
                    tokenset.removeAll(fo_stops);
                    for (String t : tokenset) {
                        addFeature("FOREIGNBEFOREWIKICATTOKEN-" + t, 1.0, f);
                    }
                } else {
                    String en_title = wiki_title;
                    if (!lang.equals("en")) en_title = ll.translateToEn(wiki_title, lang);
                    if (en_title != null) {
                        List<String> cats = MediaWikiSearch.getCategories(en_title, "en");
                        Set<String> tokenset = cats.stream()
                                .flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                                .collect(toSet());
                        tokenset.removeAll(en_stops);
                        for (String t : tokenset)
                            addFeature("BEFOREWIKICATTOKEN-" + t, 1.0, f);

                    }
                }
            }
        }
    }

    public void addTopWikiTypes(ELMention m, Map<String, Double> f) {
        if (m.getCandidates().size() > 0) {
            String wiki_title = m.getCandidates().get(0).orig_title;
            String lang = m.getCandidates().get(0).lang;
            if (wiki_title != null && !wiki_title.startsWith("NIL")) {
                if (mono) {
                    List<String> cats = MediaWikiSearch.getCategories(wiki_title, lang);
                    Set<String> tokenset = cats.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                            .collect(toSet());
                    tokenset.removeAll(fo_stops);
                    for (String t : tokenset) {
                        addFeature("FOREIGNTOPWIKICATTOKEN-" + t, 1.0, f);
                    }
                } else {
                    String en_title = wiki_title;
                    if (!lang.equals("en")) en_title = ll.translateToEn(wiki_title, lang);
                    if (en_title != null) {
                        List<String> cats = MediaWikiSearch.getCategories(en_title, "en");
                        Set<String> tokenset = cats.stream().flatMap(x -> Arrays.asList(x.toLowerCase().split("\\s+")).stream())
                                .collect(toSet());
                        tokenset.removeAll(en_stops);
                        for (String t : tokenset) {
                            addFeature("TOPWIKICATTOKEN-" + t, 1.0, f);
                        }
                    }
                }
            }
        }
    }

    public void addTopTypes(ELMention m, Map<String, Double> f, boolean train) {
        try {
            String mid;
            mid = m.getMid();

            if (!mid.startsWith("NIL")) {
                Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                for (String t : tokenset) {
                    addFeature("TOPFBTYPETOKEN-" + t, 1.0, f);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addAfterSecondTypes(List<ELMention> ms, Map<String, Double> f, int k) {
        try {
            if (ms.size() > k) {
                ELMention m = ms.get(k);
                if (m.is_stop) {
                    return;
                }
                if (m.getCandidates().size() > 1) {
                    String mid = m.getCandidates().get(1).title;
                    if (!mid.startsWith("NIL")) {
                        Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                        for (String t : tokenset)
                            if (alltype || t.equals("location") || t.equals("person") || t.equals("organization"))
                                addFeature("AFTERFBTYPETOKEN-" + t, 1.0, f);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBeforeSecondTypes(List<ELMention> ms, Map<String, Double> f, int k) {
        try {
            if (ms.size() > k) {
                ELMention m = ms.get(k);
                if (m.is_stop) {
                    return;
                }
                if (m.getCandidates().size() > 1) {
                    String mid = m.getCandidates().get(1).title;
                    if (!mid.startsWith("NIL")) {
                        Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                        for (String t : tokenset)
                            if (alltype || t.equals("location") || t.equals("person") || t.equals("organization"))
                                addFeature("BEFOREFBTYPETOKEN-" + t, 1.0, f);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addSecondTypes(ELMention m, Map<String, Double> f) {
        try {
            if (m.getCandidates().size() > 1) {
                String mid = m.getCandidates().get(1).title;
                if (!mid.startsWith("NIL")) {
                    Set<String> tokenset = FreeBaseQuery.getCoarseTypeSet(mid);
                    for (String t : tokenset)
                        addFeature("TOPFBTYPETOKEN-" + t, 1.0, f);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addFeature(String featureName, double val,
                           Map<String, Double> featureMap) {
        featureMap.put(featureName, val);
        if (!lex.containFeature(featureName) && lex.isAllowNewFeatures()) {
            lex.previewFeature(featureName);
        }
    }
}
