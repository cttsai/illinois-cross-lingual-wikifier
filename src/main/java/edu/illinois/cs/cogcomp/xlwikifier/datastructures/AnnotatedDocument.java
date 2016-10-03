package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 9/8/15.
 * This class contains the tokenization by TAC
 */

public class AnnotatedDocument {

    public AnnotatedDocument(String id){ this.id = id; }
    public String id;
    public List<Paragraph> paragraphs = new ArrayList<>();


    public static void main(String[] args) {

    }

    public String getPlainText(){
        String text = "";
        for(Paragraph para: paragraphs){
            text+=para.tokens.stream().map(x -> x.surface).collect(joining(" "))+" ";
        }
        return text.trim();
    }

    public String getSurface(int start, int end){
        String ret = "";
        for(Paragraph para: paragraphs){
            for(Token t: para.tokens){
                if(t.start_char >= end)
                    return ret.trim();
                if(t.start_char >= start){
                    ret += t.surface+" ";
                }
            }
        }
        if(!ret.trim().isEmpty())
            return ret.trim();
        return null;
    }
    public String getSurfaceNoSpace(int start, int end){
        String ret = "";
        for(Paragraph para: paragraphs){
            for(Token t: para.tokens){
                if(t.start_char >= end)
                    return ret.trim();
                if(t.start_char >= start){
                    ret += t.surface;
                }
            }
        }
        if(!ret.trim().isEmpty())
            return ret.trim();
        return null;
    }

}
