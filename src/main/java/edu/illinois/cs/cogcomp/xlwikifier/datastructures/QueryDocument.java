package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.xlwikifier.core.RankerFeatureManager;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.TACReader;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 9/2/15.
 */
public class QueryDocument {
    private String id;
    public String translated_text;
    public String xml_text;
    private transient TextAnnotation ta;
    public List<String> ne_words_es;
    public List<ELMention> mentions = new ArrayList<>();
    public List<ELMention> golds;
    public List<ELMention> authors = new ArrayList<>();
    public List<String> wikifier_features;
    public String plain_text;
    public boolean inc_end = true;
    private int search_start = 0;
    private Set<String> seen = new HashSet<>();
    public List<ELMention> tokens = new ArrayList<>();

    public QueryDocument(String id){
        this.id = id;
    }

    public String getDocID(){ return id; }
    public void setTextAnnotation(TextAnnotation ta){
        this.ta = ta;
    }
    public TextAnnotation getTextAnnotation(){ return this.ta; }


    public String getTranslatedText(){ return translated_text; }
    public String getXmlText(){ return this.xml_text; }

    public void setXmlText(String text){
        this.xml_text = text;
        this.xml_text = this.xml_text.replaceAll("\n", " ");

//        this.plain_text = xml_text.replaceAll("\\<quote.*?/quote\\>", " ");
        this.plain_text = xml_text.replaceAll("\\<.*?\\>", " ");
        this.plain_text = Arrays.asList(this.plain_text.split("\n")).stream()
                .filter(x -> !x.startsWith("http:"))
                .filter(x -> !x.isEmpty())
                .collect(joining(" "));
		this.plain_text = this.plain_text.replaceAll("\\s+"," ");

    }

    public int getNextNonXmlChar(String xml_text, int search_start){
        int next_start = search_start;
        boolean inxml = false, inquote = false;
        for(; next_start < xml_text.length(); next_start++){
            String c = xml_text.substring(next_start, next_start+1).trim();
            if(c.equals("　")) c = "";
            if(!inxml && c.equals("<"))
                inxml = true;
//            if(!inquote && xml_text.substring(next_start).startsWith("<quote"))
//                inquote = true;
//            if(!inxml && xml_text.substring(next_start).startsWith("http:"))
//                inhttp = true;

            if(!c.isEmpty() && !inxml && !inquote){
                break;
            }

//            if(c.isEmpty() && inhttp)
//                inhttp = false;

//            if(!inquote && xml_text.substring(next_start).startsWith("/quote>"))
//                inquote = false;
            if(inxml && c.equals(">"))
                inxml = false;
        }

        return next_start;
    }

    public void populateXmlMapping(){

        int search_start = getNextNonXmlChar(xml_text, 0);
        int last_idx = -1;
        for(int i = 0; i < ta.getTokens().length; i++){
            IntPair thisoff = ta.getTokenCharacterOffset(i);
            int idx = xml_text.indexOf(ta.getToken(i), search_start);


            if (i > 0) {
                IntPair prevoff = ta.getTokenCharacterOffset(i - 1);
                if(thisoff.getFirst() < prevoff.getSecond())
                    idx = last_idx;
                else if((thisoff.getFirst() - prevoff.getFirst() < idx - last_idx)){ // the form has changed and the gap is large
                    idx = getNextNonXmlChar(xml_text, search_start);
                }
            }


            if(idx == -1 || i == 0){
                if(i < ta.getTokens().length-1){
                    IntPair nextoff = ta.getTokenCharacterOffset(i + 1);


                    if(nextoff.getFirst() < thisoff.getSecond()) {
//                            idx = doc.xml_text.indexOf(ta.getToken(i).substring(0,1), search_start);
                        int next_start = getNextNonXmlChar(xml_text, search_start);
                        idx = next_start;
                    }
                }

                if(idx == -1) {
                    if(ta.getToken(i).contains(" ")){
                        ELMention token = new ELMention();
                        tokens.add(token);
                        continue;
                    }
                    System.out.println(getDocID() + " " + ta.getToken(i));
                    System.out.println(ta.getToken(i)+" "+ta.getTokenCharacterOffset(i)+" "+idx+" "+ta.getToken(i).length()+" "+search_start);
                    System.out.println(ta.getToken(i + 1) + " " + ta.getTokenCharacterOffset(i + 1));
                    System.exit(-1);
                }
            }


//            System.out.println(ta.getToken(i)+" "+ta.getTokenCharacterOffset(i)+" "+idx+" "+search_start+" "+xml_text.substring(idx, idx+10));

            last_idx = idx;
            search_start = idx + thisoff.getSecond() - thisoff.getFirst();
            int xml_start = idx;
            int xml_end = idx + thisoff.getSecond() - thisoff.getFirst();

            ELMention token = new ELMention();
            token.xml_start = xml_start;
            token.xml_end = xml_end;
            token.plain_end = thisoff.getSecond();
            token.plain_start = thisoff.getFirst();
            token.setMention(ta.getToken(i));
            tokens.add(token);
        }
    }

    /**
     * Get Spanish mention offsets in the plain text
     * @param m
     * @return
     */
    public Pair<Integer, Integer> getPlainOffsets(ELMention m){
        String xml_text = this.xml_text.replaceAll("\n", " ").toLowerCase();
        String plain_text = this.plain_text.replaceAll("\n", " ").toLowerCase();

        int end_xml = m.getEndOffset()+1;
        if(!inc_end) end_xml--;
        String surface = xml_text.substring(m.getStartOffset(), end_xml).trim();
        if(!m.getMention().toLowerCase().equals(surface)){
//            System.out.println(getDocID()+" "+m.getStartOffset()+" "+m.getEndOffset());
            System.out.println("!! "+surface+" "+m.getMention());
            return null;
//            System.out.println(xml_text);
        }

        while(true) {
            int context_idx = -1;
            int window = 22;
            while (context_idx == -1 && window >= 0) {
                String context = xml_text.substring(Math.max(m.getStartOffset() - window, 0), Math.min(end_xml + window, xml_text.length()));
                context_idx = plain_text.indexOf(context, search_start);
                if (context_idx == -1) {
                    if(window == 0){
                        window = -1;
                        break;
                    }
                    window = Math.max(window - 3, 0);
                }
            }

            if (window < 0) {
//            if(m.gold_mid!=null && !m.gold_mid.startsWith("NIL")) {
//                System.out.println("window < 0 " + this.id + " " + m.getMention());
//            }
//            System.out.println(plain_text);
//            System.exit(-1);
                return null;
            }

            int start = context_idx + window;
            if (m.getStartOffset() - window < 0)
                start = context_idx + m.getStartOffset();

            int end = start + surface.length();
            if (plain_text.length() > end && plain_text.substring(start + 1, end + 1).equals(surface)) {
                start++;
                end++;
            } else if (plain_text.length() > end + 1 && plain_text.substring(start + 2, end + 2).equals(surface)) {
                start += 2;
                end += 2;
            }

            if (!plain_text.substring(start, end).equals(surface)) {
                System.out.println("'" + surface + "'");
                System.out.println("'" + plain_text.substring(start, end) + "'");
                return null;
            }


            if(!seen.contains(start+"_"+end)){
                seen.add(start+"_"+end);
                return new Pair<>(start, end);
            }
            else
                search_start = end;
        }
    }

    public String replaceSpecialChars(String ps){
        if(ps.contains("'"))
            ps = ps.replaceAll("'", " ' ");
        if(ps.contains("-"))
            ps = ps.replaceAll("-", " - ");
        if(ps.contains("."))
            ps = ps.replaceAll("\\.", " . ");
        ps = ps.replaceAll("\\s+"," ").trim();
        return ps;
    }

    /**
     * Compute edit distance between two list of tokens
     * @param tokens1
     * @param tokens2
     * @return
     */
    public int matchTokens(List<String> tokens1, List<String> tokens2){
        tokens1 = new ArrayList<>(tokens1);
        int score = 0;
        while(tokens1.size()>0 && tokens2.size()>0) {
            int min_idx = -1;
            int match_idx = -1;
            int min_score = 1000;
            for (int i = 0; i < tokens1.size(); i++) {
                int m = 1000, midx = -1;
                for(int j = 0; j < tokens2.size(); j++){
                    int  s = LevensteinDistance.getLevensteinDistance(tokens1.get(i), tokens2.get(j));
                    if(s < m){
                        m = s;
                        midx = j;
                    }
                }
                if(m < min_score){
                    min_score = m;
                    min_idx = i;
                    match_idx = midx;
                }
            }
            score += min_score;
            tokens1.remove(min_idx);
            tokens2.remove(match_idx);
        }
        int t1_remain = tokens1.stream().collect(joining("")).length();
        int t2_remain = tokens2.stream().collect(joining("")).length();
        return score+t1_remain+t2_remain;
    }

    public void prepareFeatures(RankerFeatureManager fm){

        // compute vectors to represent mentions
        if(!fm.ner_mode) {
            if (fm.context_lang.equals("zh"))
                mentions.forEach(x -> x.mention_vec = fm.we.getVectorFromWords(x.getMention().split("·"), fm.context_lang));
            else
                mentions.forEach(x -> x.mention_vec = fm.we.getVectorFromWords(x.getMention().split("\\s+"), fm.context_lang));
        }
    }

    public void free(){

        for(ELMention m: mentions){
            m.mention_vec = null;
            m.context30 = null;
            m.context100 = null;
            m.context200 = null;
            m.other_ne_vecs = null;
            m.pre_title_vecs = null;
            m.pre_title = null;
            m.other_ne = null;
        }

    }


    public static void main(String[] args) {

        String test = "<quote> ancde </quote> sdsd <quote> asdasdasd </quote>";

        System.out.println(test.replaceAll("\\<quote.*?/quote\\>", " "));
    }
}
