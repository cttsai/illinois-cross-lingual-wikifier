package edu.illinois.cs.cogcomp.xlwikifier.postprocessing;


import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;

import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 9/29/15.
 */
public class SurfaceClustering {

    private static int nil_cnt = 1;

    public static void cluster(QueryDocument doc){
        for(ELMention m: doc.mentions){
            if(m.getMid().startsWith("NIL"))
                m.setMid("NIL"+String.format("%04d", nil_cnt++));
        }
        List<List<ELMention>> clusters = doc.mentions.stream()
                .collect(groupingBy(x -> x.getMid()))
                .entrySet().stream().map(x -> x.getValue())
                .sorted((c1, c2) -> Integer.compare(longestMention(c2), longestMention(c1)))
                .sorted((c1, c2) -> Integer.compare(c2.size(), c1.size()))
                .collect(toList());
        for(int i = 0; i < clusters.size(); i++){
            if(clusters.get(i).size()==0) continue;
            String mid = clusters.get(i).get(0).getMid();
            for(int j = i+1; j < clusters.size(); j++){
                if(clusters.get(j).size()==0) continue;
                if(compareClusters(clusters.get(i), clusters.get(j))){
//                        clusters.get(i).forEach(x -> System.out.println(x.getMention()));
//                        System.out.println("----");
//                        clusters.get(j).forEach(x -> System.out.println(x.getMention()));
                    clusters.get(j).forEach(x -> x.setMid(mid));
                    clusters.get(i).addAll(clusters.get(j));
                    clusters.get(j).clear();
                }
            }
        }


        doc.mentions = clusters.stream().flatMap(x -> x.stream()).collect(toList());

    }

    public static void cluster(List<QueryDocument> docs){

        System.out.println("Performing clustering...");

        for(QueryDocument doc: docs){
            cluster(doc);
        }
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