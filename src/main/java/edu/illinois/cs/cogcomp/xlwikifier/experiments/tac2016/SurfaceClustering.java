package edu.illinois.cs.cogcomp.xlwikifier.experiments.tac2016;




import edu.illinois.cs.cogcomp.lbjava.IR.QuantifiedConstraintExpression;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;

import java.util.*;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by ctsai12 on 9/29/15.
 */
public class SurfaceClustering {

    public SurfaceClustering(){

    }

    private int longestMention(List<ELMention> mentions){
        OptionalInt m = mentions.stream().mapToInt(x -> x.getMention().length()).max();
        if(m.isPresent())
            return m.getAsInt();
        return 0;
    }

    public void correctPersonAnnotations(List<ELMention> mentions){
        System.out.println("Correcting Person annotations...");
        Map<String, List<ELMention>> doc2mens = mentions.stream()
                .filter(x -> x.getType().equals("PER") || x.isOrigPer())
//                .filter(x -> x.isOrigPer())
                .collect(groupingBy(x -> x.getDocID()));

        int nil_cnt = 1;
        for(String docid: doc2mens.keySet()){
            System.out.println(docid);
            List<ELMention> mens = doc2mens.get(docid);
            for(ELMention m: mens){
                if(m.getMid().startsWith("NIL"))
                    m.setMid("NIL"+(nil_cnt++));
            }
            List<List<ELMention>> clusters = mens.stream()
                    .collect(groupingBy(x -> x.getMid()))
                    .entrySet().stream().map(x -> x.getValue())
                    .sorted((c1, c2) -> Integer.compare(longestMention(c2), longestMention(c1)))
                    .collect(toList());
            for(int i = 0; i < clusters.size(); i++){
                if(clusters.get(i).size()==0) continue;
                String mid = clusters.get(i).get(0).getMid();
                for(int j = i+1; j < clusters.size(); j++){
                    if(clusters.get(j).size()==0) continue;
                    System.out.println("Comparing");
                    System.out.println(clusters.get(i));
                    System.out.println(clusters.get(j));
                    if(compareClusters(clusters.get(i), clusters.get(j))){
                        System.out.println("merge:");
                        clusters.get(i).forEach(x -> System.out.println("\t"+x.getMention()));
                        System.out.println("\t----");
                        clusters.get(j).forEach(x -> System.out.println("\t"+x.getMention()));
                        clusters.get(j).forEach(x -> x.setMid(mid));
                        clusters.get(i).addAll(clusters.get(j));
                        clusters.get(j).clear();
                    }
                }
            }
        }
    }

    public void cluster(List<QueryDocument> docs){


        System.out.println("Performing clustering...");
        List<ELMention> results = new ArrayList<>();


        int nil_cnt = 1;
        for(QueryDocument doc: docs){
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
    }

    private boolean compareClusters(List<ELMention> c1, List<ELMention> c2){
        for(ELMention m2: c2){
            if(compareMentionCluster(m2, c1))
                return true;
        }
        return false;
    }


    private boolean compareMentionCluster(ELMention m, List<ELMention> c){
        OptionalDouble max = c.stream().mapToDouble(x -> jaccard(x, m)).max();
        if(max.isPresent() && max.getAsDouble() >= 0.5)
            return true;
//        long cnt = c.stream().mapToDouble(x -> jaccard(x, m))
//                .filter(x -> x >= 0.5).count();
//        if((double)cnt/c.size()>=0.5)
//            return true;
        return false;
    }

    private double jaccard(ELMention m1, ELMention m2){
        String[] t1 = m1.getMention().toLowerCase().split("\\s+");
        String[] t2 = m2.getMention().toLowerCase().split("\\s+");

        Set<String> s1 = Arrays.asList(t1).stream().collect(toSet());
        Set<String> s2 = Arrays.asList(t2).stream().collect(toSet());

        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        s1.retainAll(s2);
        return (double)s1.size()/union.size();
    }
}