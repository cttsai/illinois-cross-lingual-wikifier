package edu.illinois.cs.cogcomp.xlwikifier.datastructures;


import java.util.Map;

/**
 * Created by ctsai12 on 11/21/15.
 */
public class WikiCand {
    public String title;
    public double score;
    public double psgivent;
    public double ptgivens;
    public String lang;
    public boolean top = false;
    public String src;
    public Map<String, Double> ranker_feats;
    public String query_surface;
    public String orig_title;
    private String wikiTitle;

    public WikiCand(String title, double s) {
        this.title = title;
        this.score = s;
    }

    public String getTitle() {
        return title;
    }

    public String getOrigTitle() {
        return orig_title;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double s) {
        this.score = s;
    }

    public String toString() {
        return title + "\t" + orig_title + "\t" + score+" "+ptgivens;
    }

    public String getWikiTitle() {
        return wikiTitle;
    }
}
