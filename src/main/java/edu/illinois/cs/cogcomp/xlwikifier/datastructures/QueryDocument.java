package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.xlwikifier.core.RankerFeatureManager;
import edu.illinois.cs.cogcomp.xlwikifier.evaluation.XMLOffsetHandler;

import java.util.*;

/**
 * Created by ctsai12 on 9/2/15.
 */
public class QueryDocument {
    private String id;
    private transient TextAnnotation ta;
    public List<ELMention> mentions = new ArrayList<>();
    public String text;
    private XMLOffsetHandler xmlhandler; // for tac documents

    public QueryDocument(String id) {
        this.id = id;
    }

    public String getDocID() {
        return id;
    }

    public void setDocID(String id){
        this.id = id;
    }

    public void setText(String t){
        this.text = t;
    }

    public String getText(){
        return text;
    }

    public void setTextAnnotation(TextAnnotation ta) {
        this.ta = ta;
    }

    public TextAnnotation getTextAnnotation() {
        return this.ta;
    }

    public void setXmlHandler(XMLOffsetHandler xh){
        xmlhandler = xh;
    }

    public XMLOffsetHandler getXmlhandler(){
        return xmlhandler;
    }

    public String getXMLText(){
        return xmlhandler.xml_text;
    }

    public void prepareFeatures(RankerFeatureManager fm) {

        // compute vectors to represent mentions
        if (!fm.ner_mode) {
            if (fm.context_lang.equals("zh"))
                mentions.forEach(x -> x.mention_vec = fm.we.getVectorFromWords(x.getSurface().split("·"), fm.context_lang));
            else
                mentions.forEach(x -> x.mention_vec = fm.we.getVectorFromWords(x.getSurface().split("\\s+"), fm.context_lang));
        }
    }


    public static void main(String[] args) {
    }
}
