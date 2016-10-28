package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.joining;

/**
 * This class handles character offsets mapping between xml text and the cleaned plain text
 * This is mainly designed for TAC documents
 *
 * Created by ctsai12 on 10/27/16.
 */
public class XMLOffsetHandler {

    public String xml_text;
    public String plain_text;
    public TextAnnotation ta;
    private Map<Integer, Integer> plain2xml_start = new HashMap<>();
    private Map<Integer, Integer> plain2xml_end = new HashMap<>();

    public XMLOffsetHandler(String xml, Tokenizer tokenizer){
        xml_text = xml;

        xml_text = xml_text.replaceAll("\n", " ");

        plain_text = xml_text.replaceAll("\\<.*?\\>", " ");
        plain_text = Arrays.asList(plain_text.split("\n")).stream()
                .filter(x -> !x.startsWith("http:"))
                .filter(x -> !x.isEmpty())
                .collect(joining(" "));
        plain_text = plain_text.replaceAll("\\s+"," ");

        ta = tokenizer.getTextAnnotation(plain_text);

        populateOffsetMapping();
    }

    public int getNextNonXmlChar(String xml_text, int search_start){
        int next_start = search_start;
        boolean inxml = false, inquote = false;
        for(; next_start < xml_text.length(); next_start++){
            String c = xml_text.substring(next_start, next_start+1).trim();
            if(c.equals("　")) c = "";
            if(!inxml && c.equals("<"))
                inxml = true;

            if(!c.isEmpty() && !inxml && !inquote){
                break;
            }
            if(inxml && c.equals(">"))
                inxml = false;
        }

        return next_start;
    }

    public Pair<Integer, Integer> getXmlOffsets(int start, int end){

        return new Pair<>(plain2xml_start.get(start), plain2xml_end.get(end));
    }

    /**
     * Find the character offsets mapping between xml and plain text
     * This is complicated because Stanford Spanish tokenizer handles morphology thus changes the text
     */
    private void populateOffsetMapping(){

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
                        int next_start = getNextNonXmlChar(xml_text, search_start);
                        idx = next_start;
                    }
                }

                if(idx == -1) {
                    if(ta.getToken(i).contains(" ")){
                        continue;
                    }
                    System.out.println(ta.getToken(i)+" "+ta.getTokenCharacterOffset(i)+" "+idx+" "+ta.getToken(i).length()+" "+search_start);
                    System.out.println(ta.getToken(i + 1) + " " + ta.getTokenCharacterOffset(i + 1));
                    System.exit(-1);
                }
            }

            int xml_start = idx;
            int xml_end = idx + thisoff.getSecond() - thisoff.getFirst();

            plain2xml_start.put(thisoff.getFirst(), xml_start);
            plain2xml_end.put(thisoff.getSecond(), xml_end);

            last_idx = xml_start;
            search_start = xml_end;
        }
    }
}
