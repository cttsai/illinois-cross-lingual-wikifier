package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ctsai12 on 10/27/16.
 */
public class TACUtils {

    private static final Logger logger = LoggerFactory.getLogger(TACUtils.class);

    /**
     * Find text regions that are inside <quote> and </quote> (in xml offsets)
     * @return
     */
    public static List<Pair<Integer, Integer>> getBadIntervals(String xml_text){

        List<Pair<Integer, Integer>> ret = new ArrayList<>();
        int start = -1;
        for(int i = 0; i < xml_text.length(); i++){
            if(start == -1 && xml_text.substring(i).startsWith("<quote"))
                start = i;

            if(start > -1 && xml_text.substring(0, i).endsWith("/quote>")) {
                ret.add(new Pair(start, i));
                start = -1;
            }
        }
        return ret;
    }

    public static void removeQuoteMentions(QueryDocument doc){

        int n_add = 0;
        List<Pair<Integer, Integer>> interval = getBadIntervals(doc.getXmlhandler().xml_text);

        List<ELMention> nm = new ArrayList<>();
        for(ELMention m: doc.mentions){
            boolean bad = false;
            for(Pair<Integer, Integer> inte: interval){
                if((m.getStartOffset() >= inte.getFirst() && m.getStartOffset() < inte.getSecond())
                        || (inte.getFirst() >= m.getStartOffset() && inte.getFirst() < m.getEndOffset())){
                    bad = true;
                    n_add++;
                    break;
                }
            }
            if(!bad)
                nm.add(m);
        }
        doc.mentions = nm;
//        logger.info("Removed "+n_add+" mentions in the <quote>");
    }

    public static void setXmlOffsets(QueryDocument doc){
        for (ELMention m : doc.mentions) {
            Pair<Integer, Integer> offsets = doc.getXmlhandler().getXmlOffsets(m.getStartOffset(), m.getEndOffset());
            m.setStartOffset(offsets.getFirst());
            m.setEndOffset(offsets.getSecond());
        }
    }

    public static void addPostAuthors(QueryDocument doc){
        List<ELMention> ret = new ArrayList<>();
        Pattern pattern = Pattern.compile(" author=\"(.*?)\"");

        Matcher matcher = pattern.matcher(doc.getXmlhandler().xml_text);
        while(matcher.find()){
            String mention = matcher.group(1);
            int start = matcher.start(1);
            int end = matcher.end(1);
            while(mention.startsWith(" ")) {
                start++;
                mention = mention.substring(1);
            }
            while(mention.endsWith(" ")) {
                end--;
                mention = mention.substring(0, mention.length()-1);
            }

            ELMention m = new ELMention(doc.getDocID(), start, end);
            m.setSurface(mention);
            m.setType("PER");
            m.setMid("NIL");
            ret.add(m);
        }
//        logger.info("Extracted "+ret.size()+" authors in xml tags");

        doc.mentions.addAll(ret);
    }
}
