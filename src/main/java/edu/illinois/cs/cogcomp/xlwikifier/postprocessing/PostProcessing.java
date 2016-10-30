package edu.illinois.cs.cogcomp.xlwikifier.postprocessing;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.MediaWikiSearch;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ctsai12 on 10/27/16.
 */
public class PostProcessing {
    public static void cleanSurface(QueryDocument doc){
        for(ELMention m: doc.mentions){
            String surf = m.getSurface();
            int idx = surf.indexOf("<");
            if(idx > -1){
                while(idx > 0 && surf.substring(idx-1, idx).trim().isEmpty()) idx--;
                if(idx>0) {
                    m.setEndOffset(m.getEndOffset()-(surf.length()-idx));
                    m.setSurface(surf.substring(0, idx));
                }
            }
        }
    }

    public static void fixPerAnnotation(QueryDocument doc){

        List<ELMention> length_sorted = doc.mentions.stream()
                .filter(x -> x.getType().equals("PER"))
                .sorted((x1, x2) -> Integer.compare(x2.getSurface().length(), x1.getSurface().length()))
                .collect(Collectors.toList());

        for(int i = 1; i < length_sorted.size(); i++){
            ELMention current = length_sorted.get(i);
            for(int j = 0; j < i; j++){
                ELMention longer = length_sorted.get(j);

                if(longer.getSurface().toLowerCase().contains(current.getSurface().toLowerCase())){
                    if(!longer.getMid().startsWith("NIL"))
                        current.setMid(longer.getMid());
                    if(!longer.getEnWikiTitle().startsWith("NIL"))
                        current.setEnWikiTitle(longer.getEnWikiTitle());
                    if(!longer.getWikiTitle().startsWith("NIL"))
                        current.setWikiTitle(longer.getWikiTitle());
                    break;
                }

            }
        }
//        for(ELMention m: doc.mentions){
//            if(!m.getType().equals("PER")) continue;
//            String mid = null;
//            int length = m.getSurface().length();
//
//            for(ELMention m1: doc.mentions){
//                if(!m1.getType().equals("PER")) continue;
//                if(m1.getSurface().toLowerCase().contains(m.getSurface().toLowerCase())
//                        && m1.getSurface().length() > length){
//                    mid = m1.getMid();
//                    length = m1.getSurface().length();
//                }
//            }
//            if(mid!=null)
//                m.setMid(mid);
//        }
    }

    public static void wikiSearchSolver(QueryDocument doc, String lang){
        System.out.println("Querying wikipedia...");
        String fb_lang = lang;
        if(lang.equals("zh")) fb_lang = "zh-cn";

        for(ELMention m: doc.mentions){
            if(m.getCandidates().size() > 0) continue;

            List<String> titles = MediaWikiSearch.search(m.getSurface(), lang, "fuzzy");

            if(titles.size()>0) {
                m.setWikiTitle(titles.get(0));
                String mid = FreeBaseQuery.getMidFromTitle(titles.get(0), fb_lang);
                if(mid != null)
                    m.setMid(mid);
            }
            else {
                titles = MediaWikiSearch.search(m.getSurface(), "en", "fuzzy");
                if (titles.size() > 0) {
                    m.setEnWikiTitle(titles.get(0));
                    String mid = FreeBaseQuery.getMidFromTitle(titles.get(0), "en");
                    if (mid != null)
                        m.setMid(mid);
                }
            }
        }
    }
}
