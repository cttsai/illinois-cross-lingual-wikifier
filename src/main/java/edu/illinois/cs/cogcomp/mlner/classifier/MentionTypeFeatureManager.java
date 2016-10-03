package edu.illinois.cs.cogcomp.mlner.classifier;

import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import edu.illinois.cs.cogcomp.indsup.learning.LexManager;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.QueryMQL;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.MediaWikiSearch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class MentionTypeFeatureManager implements Serializable {
    private static final long serialVersionUID = -1932878634118945538L;
    private LexManager lex;
    private QueryMQL qm;
    private LangLinker ll;
    private MediaWikiSearch mws;
    private Tokenizer tokenizer;
    private Map<String, Set<String>> gaze = new HashMap<>();
    private Map<String, Set<String>> gazec = new HashMap<>();
    private Map<String, Set<String>> gaze_token = new HashMap<>();
    private Map<String, Set<String>> gaze_tokenc = new HashMap<>();
    private String lang;

    public MentionTypeFeatureManager(String lang){
        lex = new LexManager();
        tokenizer = MultiLingualTokenizer.getTokenizer(lang);
        this.lang = lang;

        loadGazetteers(lang);

    }

    public FeatureVector getFV(ELMention mention, String text){

        Map<String, Double> featureMap = new HashMap<>();
        addWordForms(mention, text, featureMap);
        addPreSuffix(mention, featureMap);
        if(!lang.equals("zh"))
            addGaze(mention, featureMap);
//        if(lang.equals("zh"))
//            addCharacter(mention, featureMap);
        addFeatureCombos(featureMap);

        FeatureVector fv = lex.convertRawFeaMap2LRFeatures(featureMap);
        fv.sort();
        return fv;
    }

    private void loadGazetteers(String lang){
        String dir = "/shared/corpora/ner/gazetteers/"+lang+"/";
        File df = new File(dir);
        for(File f: df.listFiles()){


            System.out.println(f.getAbsolutePath());
            try {
                Set<String> names = LineIO.read(f.getAbsolutePath()).stream().collect(toSet());
                System.out.println(names.size());
                Set<String> cnames = names.stream().map(x -> x.toLowerCase()).collect(toSet());
                System.out.println(cnames.size());
                gaze.put(f.getAbsolutePath(), names);
                gazec.put(f.getAbsolutePath(), cnames);
                Set<String> name_tokens;
                if(!lang.equals("zh"))
                    name_tokens = names.stream().flatMap(x -> Arrays.asList(x.split("\\s+")).stream()).collect(toSet());
                else
                    name_tokens = names.stream().flatMap(x -> Arrays.asList(x.split("·")).stream()).collect(toSet());
                gaze_token.put(f.getAbsolutePath(), name_tokens);
                Set<String> name_tokensc = name_tokens.stream().map(x -> x.toLowerCase()).collect(toSet());
                gaze_tokenc.put(f.getAbsolutePath(), name_tokensc);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void addGaze(ELMention m, Map<String, Double> f){
        String surface = m.getMention();

        for(String gaz: gaze.keySet()){
            if(gaze.get(gaz).contains(surface))
                addFeature("Gaze-Exact-"+gaz, 1, f);
        }

        if(!lang.equals("zh"))
            for(String gaz: gazec.keySet()){
                if(gazec.get(gaz).contains(surface.toLowerCase()))
                    addFeature("Gaze-Exact-C-"+gaz, 1, f);
            }

        String[] tokens;
        if(lang.equals("zh"))
            tokens = surface.split("·");
        else
            tokens = surface.split("\\s+");
        for(String t: tokens) {
            for (String gaz : gaze_token.keySet()) {
                if (gaze_token.get(gaz).contains(t))
                    addFeature("Gaze-Contain-" + gaz, 1, f);
            }
        }
    }

    public void addCharacter(ELMention m, Map<String, Double> f){
        for(int i = 0; i < m.getMention().length(); i++){
            addFeature("ZH-Char-"+m.getMention().substring(i, i+1), 1, f);
        }

//        for(int i = 0; i < m.getMention().length()-1; i++){
//            addFeature("ZH-Bigram-"+m.getMention().substring(i, i+2), 1, f);
//        }
//        for(int i = 0; i < m.getMention().length()-2; i++){
//            addFeature("ZH-Trigram-"+m.getMention().substring(i, i+3), 1, f);
//        }
    }

    public void addPreSuffix(ELMention m, Map<String, Double> f){
        String surface = m.getMention();
        if(surface.length()>4){
            addFeature("Prefix-4-"+surface.substring(0,5), 1, f);
            addFeature("Sufix-4-"+surface.substring(surface.length()-4, surface.length()), 1, f);
        }

        if(surface.length()>3){
            addFeature("Prefix-3-"+surface.substring(0,4), 1, f);
            addFeature("Sufix-3-"+surface.substring(surface.length()-3, surface.length()), 1, f);
        }
    }

    public void addWordForms(ELMention m, String text, Map<String, Double> f){

        int window = 30;
        int start = Math.max(0, m.getStartOffset()-window);
        int end = Math.min(text.length(), m.getEndOffset()+window);

        String context_before = text.substring(start, m.getStartOffset()).trim();
        String context_after = text.substring(m.getEndOffset(), end).trim();

        String[] btokens = new String[0];
        if(context_before.length()>0) {
            TextAnnotation ta = tokenizer.getTextAnnotation(context_before);
            if(ta != null)
                btokens = ta.getTokens();
        }
        String[] atokens = new String[0];
        if(context_after.length()>0) {
            TextAnnotation ta = tokenizer.getTextAnnotation(context_after);
            if(ta != null)
                atokens = ta.getTokens();
        }
        String[] ctokens = tokenizer.getTextAnnotation(m.getMention()).getTokens();

        for(int i = 0; i < ctokens.length; i++) {
            String word = ctokens[i].toLowerCase();
            addFeature("Current-Word-"+word, 1, f);

            if(i == 0)
                addFeature("Start-Word-"+word, 1, f);

            if(i == ctokens.length-1)
                addFeature("End-Word-"+word,1,f);
        }

        if(btokens.length>0){
            String prew = btokens[btokens.length-1].toLowerCase();
            addFeature("Prev-Word-"+prew, 1, f);
//            if(btokens.length>1){
//                String pp = btokens[btokens.length-2].toLowerCase();
//                addFeature("PP-Word-"+pp+"-"+prew, 1, f);
//            }
        }

        if(atokens.length>0){
            String next = atokens[0].toLowerCase();
            addFeature("Next-Word-"+next, 1, f);
//            if(atokens.length>1){
//                String nn = atokens[1].toLowerCase();
//                addFeature("NN-Word-"+nn+"-"+next, 1, f);
//            }
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
