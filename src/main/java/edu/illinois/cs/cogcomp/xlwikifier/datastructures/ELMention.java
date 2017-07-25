package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

import edu.illinois.cs.cogcomp.xlwikifier.core.RankerFeatureManager;
import org.apache.commons.lang.builder.EqualsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by ctsai12 on 8/27/15.
 */
public class ELMention {
    public String id;
    private String surface;
    public String docid;
    private String type;
    private int start_offset;
    private int end_offset;
    private String language;
    private String wiki_title = "NIL";
    public String gold_wiki_title;
    private String en_wiki_title = "NIL";
    private List<WikiCand> cands = new ArrayList<>();
    public boolean eazy;
    public String pred_type;
    public String gold_lang;
    //    public boolean is_ne;
    public boolean is_ne_gold;
    public int ngram;
    public boolean is_stop;
    public Map<String, Double> ner_features = new HashMap<>();
    public double confidence = 1.0;

    // for ranker features
    private transient float[] mid_vec;
    public transient float[] context30;
    public transient float[] context100;
    public transient float[] context200;
    public transient float[] other_ne;
    public transient float[] pre_title;
    public transient List<float[]> pre_title_vecs;
    public transient List<float[]> other_ne_vecs;
    public transient float[] mention_vec;

    // for tac exp
    private String mid = "NIL";
    public String gold_mid;
    public String noun_type;

    public ELMention() {
    }

    ;

    public ELMention(String docid, int start, int end) {
        this.docid = docid;
        this.start_offset = start;
        this.end_offset = end;
    }

    public ELMention(String id, String mention, String docid) {
        this.id = id;
        this.surface = mention;
        this.docid = docid;
    }

    public ELMention(String surface, int start, int end, String type, String wiki, String enwiki) {
        this.surface = surface;
        this.start_offset = start;
        this.end_offset = end;
        this.type = type;
        this.wiki_title = wiki;
        this.en_wiki_title = enwiki;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLanguage(String lang) {
        this.language = lang;
    }

    public void setStartOffset(int start) {
        this.start_offset = start;
    }

    public void setEndOffset(int end) {
        this.end_offset = end;
    }

    public void setMid(String ans) {
        this.mid = ans;
    }

    public void setSurface(String surface) {
        this.surface = surface;
    }

    public void setWikiTitle(String t) {
        this.wiki_title = t;
    }

    public void setEnWikiTitle(String t){
        if(t == null)
            en_wiki_title = "NIL";
        else
            en_wiki_title = t;
    }

    public String getEnWikiTitle(){
        return en_wiki_title;
    }

    public void setNounType(String t) {
        this.noun_type = t;
    }

    public String getNounType() {
        return this.noun_type;
    }

    public void setMidVec(float[] vec) {
        this.mid_vec = vec;
    }

    public float[] getMidVec() {
        return this.mid_vec;
    }

    public String getSurface() {
        return this.surface;
    }

    public String getDocID() {
        return this.docid;
    }

	public void setDocID(String id) {
		this.docid = id;
	}

    public String getType() {
        return this.type;
    }

    public String getLanguage() {
        return this.language;
    }

    public int getStartOffset() {
        return this.start_offset;
    }

    public int getEndOffset() {
        return this.end_offset;
    }

    public String getMid() {
        return this.mid;
    }

    public String getWikiTitle() {
        return this.wiki_title;
    }

    public String getGoldMid() {
        return this.gold_mid;
    }

    @Override
    public String toString() {
        return "\"" + surface + "\", " + start_offset + ", " + end_offset + ", \"" + type + "\", \"" + wiki_title + "\", \"" + en_wiki_title + "\"";
    }

    public void setCandidates(List<WikiCand> wikiCandidates) {
        this.cands = wikiCandidates;
    }

    public List<WikiCand> getCandidates() {
        return this.cands;
    }


    public void prepareFeatures(QueryDocument doc, RankerFeatureManager fm, List<ELMention> pms) {
        context30 = fm.getWeightedContextVector(this, doc, 5);
        context100 = fm.getWeightedContextVector(this, doc, 10);
        context200 = fm.getWeightedContextVector(this, doc, 25);

        if (!fm.ner_mode) {
            other_ne_vecs = new ArrayList<>();
            for (ELMention me : doc.mentions) {
                if (getStartOffset() != me.getStartOffset() || getEndOffset() != me.getEndOffset())
                    other_ne_vecs.add(me.mention_vec);
            }
            other_ne = fm.we.averageVectors(other_ne_vecs);

            pre_title_vecs = pms.stream().map(x -> x.getMidVec()).filter(x -> x != null).collect(Collectors.toList());
            pre_title = fm.getTitleAvg(pms);
        }
    }

    public void cleanFeatureCache(){
        mid_vec = null;
        context30 = null;
        context100 = null;
        context200 = null;
        other_ne = null;
        pre_title = null;
        pre_title_vecs = null;
        other_ne_vecs = null;
        mention_vec = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ELMention))
            return false;
        if (obj == this)
            return true;

        ELMention rhs = (ELMention) obj;
        return new EqualsBuilder().append(docid, rhs.getDocID())
                .append(start_offset, rhs.getStartOffset())
                .append(end_offset, rhs.getEndOffset())
                .isEquals();
    }
}
