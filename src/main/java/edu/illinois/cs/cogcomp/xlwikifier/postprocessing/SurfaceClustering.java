package edu.illinois.cs.cogcomp.xlwikifier.postprocessing;


import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 9/29/15.
 */
public class SurfaceClustering {

    private static int nil_cnt = 1;

    public static void cluster(QueryDocument doc){

        // use gold_wiki_title to store the target ID
        if(ConfigParameters.target_kb.equals("freebase"))
            doc.mentions.forEach(x -> x.gold_wiki_title = x.getMid());
        else if(ConfigParameters.target_kb.equals("enwiki"))
            doc.mentions.forEach(x -> x.gold_wiki_title = x.getEnWikiTitle());

        // assign NIL singltons
        for(ELMention m: doc.mentions){
            if (m.gold_wiki_title.startsWith("NIL"))
                m.gold_wiki_title = "NIL" + String.format("%04d", nil_cnt++);
        }

        List<List<ELMention>> clusters = doc.mentions.stream()
                .collect(groupingBy(x -> x.gold_wiki_title))
                .entrySet().stream().map(x -> x.getValue())
                .sorted((c1, c2) -> Integer.compare(longestMention(c2), longestMention(c1)))
                .sorted((c1, c2) -> Integer.compare(c2.size(), c1.size()))
                .collect(toList());

        for(int i = 0; i < clusters.size(); i++){
            if(clusters.get(i).size()==0) continue;
            String id = clusters.get(i).get(0).gold_wiki_title;
            for(int j = i+1; j < clusters.size(); j++){
                if(clusters.get(j).size()==0) continue;
                if(compareClusters(clusters.get(i), clusters.get(j))){
                    clusters.get(j).forEach(x -> x.gold_wiki_title = id);
                    clusters.get(i).addAll(clusters.get(j));
                    clusters.get(j).clear();
                }
            }
        }

        doc.mentions = clusters.stream().flatMap(x -> x.stream()).collect(toList());

        if(ConfigParameters.target_kb.equals("freebase"))
            doc.mentions.forEach(x -> x.setMid(x.gold_wiki_title));
        else if(ConfigParameters.target_kb.equals("enwiki"))
            doc.mentions.forEach(x -> x.setEnWikiTitle(x.gold_wiki_title));

        doc.mentions = doc.mentions.stream().sorted((x1, x2) -> Integer.compare(x1.getStartOffset(), x2.getStartOffset()))
                .collect(Collectors.toList());
    }

    private static int longestMention(List<ELMention> mentions){
        OptionalInt m = mentions.stream().mapToInt(x -> x.getSurface().length()).max();
        if(m.isPresent())
            return m.getAsInt();
        return 0;
    }

    private static boolean compareClusters(List<ELMention> c1, List<ELMention> c2){
        for(ELMention m2: c2){
            if(compareMentionCluster(m2, c1))
                return true;
        }
        return false;
    }


    private static boolean compareMentionCluster(ELMention m, List<ELMention> c){
        OptionalDouble max = c.stream().mapToDouble(x -> jaccard(x, m)).max();
        if(max.isPresent() && max.getAsDouble() >= 0.5)
            return true;
        return false;
    }

    private static double jaccard(ELMention m1, ELMention m2){
        String[] t1 = m1.getSurface().toLowerCase().split("\\s+");
        String[] t2 = m2.getSurface().toLowerCase().split("\\s+");

        Set<String> s1 = Arrays.asList(t1).stream().collect(toSet());
        Set<String> s2 = Arrays.asList(t2).stream().collect(toSet());

        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        s1.retainAll(s2);
        return (double)s1.size()/union.size();
    }
}