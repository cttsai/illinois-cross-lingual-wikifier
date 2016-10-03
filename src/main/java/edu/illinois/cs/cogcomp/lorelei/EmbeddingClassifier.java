package edu.illinois.cs.cogcomp.lorelei;

import edu.illinois.cs.cogcomp.lorelei.core.RuleBasedAnnotator;
import edu.illinois.cs.cogcomp.xlwikifier.core.WordEmbedding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ctsai12 on 7/29/16.
 */
public class EmbeddingClassifier {

    private WordEmbedding we;
    private String lang;

    public Map<String, Double[]> typevec = new HashMap<>();

    public EmbeddingClassifier(String lang){
        this.lang = lang;
        we = new WordEmbedding();
        we.loadMonoDBNew(lang);
        for(String type: RuleBasedAnnotator.entitytypes)
            typevec.put(type, we.getWordVector("type:"+type.toLowerCase(), lang));
    }

    public String classify(List<String> context, double th){

        List<String> labels = topLabels(context, th);

        return labels.size() == 0? "O" : labels.get(0);

    }

    public List<String> topLabels(List<String> context, double th){

        List<Double[]> vecs = context.stream().map(x -> we.getWordVector(x, lang))
                .filter(x -> x != null).collect(Collectors.toList());

        Double[] avg = we.averageVectors(vecs);

        String max_type = null;
        double max_score = -10000;


        Map<String, Double> type2score = new HashMap<>();

        for(String type: typevec.keySet()){
            double score = we.cosine(typevec.get(type), avg);
            type2score.put(type, score);
        }

        return type2score.entrySet().stream()
                .sorted((x1, x2) -> Double.compare(x2.getValue(), x1.getValue()))
                .filter(x -> x.getValue() > th)
                .map(x -> x.getKey())
                .collect(Collectors.toList());

//        return max_score>th ? max_type : "O";
    }
}
