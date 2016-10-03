package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.xlwikifier.core.RankerFeatureManager;

import java.util.*;

/**
 * Created by ctsai12 on 9/2/15.
 */
public class QueryDocument {
    private String id;
    private transient TextAnnotation ta;
    public List<ELMention> mentions = new ArrayList<>();
    public String plain_text;
    public List<ELMention> tokens = new ArrayList<>();

    public QueryDocument(String id){
        this.id = id;
    }

    public String getDocID(){ return id; }
    public void setTextAnnotation(TextAnnotation ta){
        this.ta = ta;
    }
    public TextAnnotation getTextAnnotation(){ return this.ta; }


    public void prepareFeatures(RankerFeatureManager fm){

        // compute vectors to represent mentions
        if(!fm.ner_mode) {
            if (fm.context_lang.equals("zh"))
                mentions.forEach(x -> x.mention_vec = fm.we.getVectorFromWords(x.getMention().split("·"), fm.context_lang));
            else
                mentions.forEach(x -> x.mention_vec = fm.we.getVectorFromWords(x.getMention().split("\\s+"), fm.context_lang));
        }
    }


    public static void main(String[] args) {
    }
}
