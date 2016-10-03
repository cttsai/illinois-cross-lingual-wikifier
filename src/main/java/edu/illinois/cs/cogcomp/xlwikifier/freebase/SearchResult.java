package edu.illinois.cs.cogcomp.xlwikifier.freebase;

/**
 * Created by ctsai12 on 9/16/15.
 */
public class SearchResult {
    private String name;
    private String mid;
    private String id;
    private String lang;
    private Double score;
    private Notable notable;

    public SearchResult(){}

    public String getMid(){ return mid; }
    public String getName(){ return name; }
    public double getScore(){ return score; }
    public String getLang(){ return lang; }
    public void setScore(double s){ this.score = s; }


    private class Notable{
        private String name;
        private String id;

    }
}
