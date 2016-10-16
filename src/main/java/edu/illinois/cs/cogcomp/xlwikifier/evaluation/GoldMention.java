package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import org.w3c.dom.Node;

/**
 * Created by lchen112 on 10/15/16.
 */
public class GoldMention {
    private int start_offset;
    private int end_offset;
    private String wiki_title;
    private String mention;
    public GoldMention(String surface, int charStart, int charEnd, String annotation){
        this.mention = surface;
        this.start_offset = charStart;
        this.end_offset = charEnd;
        this.wiki_title = annotation.toLowerCase();
    }
    public  String getMention(){ return mention; }
    public String getWikititle(){ return wiki_title; }
    public String toString() {
        return mention+" "+start_offset+","+end_offset;
    }
}
