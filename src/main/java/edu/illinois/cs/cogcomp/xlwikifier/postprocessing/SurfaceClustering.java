package edu.illinois.cs.cogcomp.xlwikifier.postprocessing;


import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 9/29/15.
 */
public class SurfaceClustering {

    private static final Logger logger = LoggerFactory.getLogger(SurfaceClustering.class);
    private static int nil_cnt = 1;

	public static double jaccard_th = 0.5;
	public static int mention_th = 1;

    public static void NILClustering(List<QueryDocument> docs, int th){

		mention_th = th;

		List<ELMention> nils = new ArrayList();
		for(QueryDocument doc: docs){
			List<ELMention> non_nils = new ArrayList();

			for(ELMention m: doc.mentions){

				m.setDocID(doc.getDocID());

        		if(ConfigParameters.target_kb.equals("freebase")){
					if(m.getMid().startsWith("NIL"))
						nils.add(m);
					else
						non_nils.add(m);
				}
				else if(ConfigParameters.target_kb.equals("enwiki")){
					if(m.getEnWikiTitle().startsWith("NIL"))
						nils.add(m);
					else
						non_nils.add(m);
				}
			}

			doc.mentions = non_nils;
		}
		
		System.out.println("#NIL mentions: "+nils.size());

        List<ELMention> results = SurfaceClustering.cluster(nils);

        Map<String, List<ELMention>> doc2mentions = results.stream().collect(groupingBy(x -> x.getDocID()));

        for(QueryDocument doc: docs){
            if(doc2mentions.containsKey(doc.getDocID())){
				doc.mentions.addAll(doc2mentions.get(doc.getDocID()));
                doc.mentions = doc.mentions.stream()
                        .sorted(Comparator.comparingInt(ELMention::getStartOffset))
                        .collect(toList());
            }
        }
    }

    private static String printDocMentions(List<ELMention> mentions) {
        StringBuilder bldr = new StringBuilder();
        for ( ELMention m : mentions ) {
            bldr.append( m.getSurface() ).append( " (" ).append( m.getStartOffset() ).append( "-" ).append( m.getEndOffset() );
            bldr.append( "): " ).append( m.getMid() ).append( "/" ).append( m.getWikiTitle() ).append( "/" );
            bldr.append( m.getEnWikiTitle() ).append( "\n" );
        }

        return bldr.toString();
    }

    public static List<ELMention> clusterAuthors(List<ELMention> authors){

        Map<ELMention, List<ELMention>> clusters = authors.stream().collect(groupingBy(x -> x));

        for(ELMention key: clusters.keySet()){
            for(ELMention m: clusters.get(key))
                m.setMid("NIL" + String.format("%05d", nil_cnt++));
        }

        return clusters.entrySet().stream().flatMap(x -> x.getValue().stream()).collect(toList());
    }

    public static List<ELMention> cluster(List<ELMention> mentions){

        //nil_cnt = 1;

        // use gold_wiki_title to store the target ID
        if(ConfigParameters.target_kb.equals("freebase"))
            mentions.forEach(x -> x.gold_wiki_title = x.getMid());
        else if(ConfigParameters.target_kb.equals("enwiki"))
            mentions.forEach(x -> x.gold_wiki_title = x.getEnWikiTitle());

        // assign NIL singltons
        for(ELMention m: mentions){
            if (m.gold_wiki_title.equals("NIL"))
                m.gold_wiki_title = "NIL" + String.format("%05d", nil_cnt++);
        }

        List<List<ELMention>> clusters = mentions.stream()
                .collect(groupingBy(x -> x.gold_wiki_title))
                .entrySet().stream().map(x -> x.getValue())
                .collect(toList());

        // sort mentions in each cluster by surface length
        // sort clusters by number of mentions and the longest mention in the cluster
        clusters.stream().map(x -> x.stream().sorted((m1, m2) -> Integer.compare(m2.getSurface().length(), m1.getSurface().length())).collect(toList()))
                .sorted((c1, c2) -> Integer.compare(c2.get(0).getSurface().length(), c1.get(0).getSurface().length()))
                .sorted((c1, c2) -> Integer.compare(c2.size(), c1.size()))
                .collect(toList());


        // separate clusters by the entity type of the longest mention
        Map<String, List<List<ELMention>>> type2clusters = clusters.stream().collect(groupingBy(x -> x.get(0).getType()));

        List<ELMention> results = new ArrayList<>();
        for(String type: type2clusters.keySet()){
			List<ELMention> rs = clusterClusters(type2clusters.get(type));
            results.addAll(rs);
        }

        if(ConfigParameters.target_kb.equals("freebase"))
            results.forEach(x -> x.setMid(x.gold_wiki_title));
        else if(ConfigParameters.target_kb.equals("enwiki"))
            results.forEach(x -> x.setEnWikiTitle(x.gold_wiki_title));

        return results;
    }

    private static List<ELMention> clusterClusters(List<List<ELMention>> clusters){
        for(int i = 0; i < clusters.size(); i++){
            if(clusters.get(i).size()==0) continue;
            String id = clusters.get(i).get(0).gold_wiki_title;

            List<ELMention> to_be_merge = new ArrayList<>();

            // add similar enough smaller clusters into to_be_merge
            for(int j = i+1; j < clusters.size(); j++){
                if(clusters.get(j).size()==0) continue;
                if(compareClusters(clusters.get(i), clusters.get(j))){
                    clusters.get(j).forEach(x -> x.gold_wiki_title = id);
                    to_be_merge.addAll(clusters.get(j));
                    clusters.get(j).clear();
                }
            }

            clusters.get(i).addAll(to_be_merge);
        }

        return clusters.stream().flatMap(x -> x.stream()).collect(toList());
    }


    private static boolean compareClusters(List<ELMention> c1, List<ELMention> c2){
        Set<String> c1_surface = c1.stream().map(x -> x.getSurface()).collect(toSet());
        Set<String> c2_surface = c2.stream().map(x -> x.getSurface()).collect(toSet());

		int cnt = 0;
        for(String m2: c2_surface){
            if(compareMentionCluster(m2, c1_surface)){
				cnt++;
				if(cnt >= mention_th)
					return true;
			}
        }
        return false;
    }


    private static boolean compareMentionCluster(String m, Set<String> c){
        OptionalDouble max = c.stream().mapToDouble(x -> jaccard(x, m)).max();
        if(max.isPresent() && max.getAsDouble() > jaccard_th)
            return true;
        return false;
    }

    private static double jaccard(String m1, String m2){
        String[] t1 = m1.toLowerCase().split("\\s+|·");
        String[] t2 = m2.toLowerCase().split("\\s+|·");

        Set<String> s1 = Arrays.asList(t1).stream().collect(toSet());
        Set<String> s2 = Arrays.asList(t2).stream().collect(toSet());

        Set<String> union = new HashSet<>(s1);
        union.addAll(s2);
        s1.retainAll(s2);
        return (double)s1.size()/union.size();
    }
}
