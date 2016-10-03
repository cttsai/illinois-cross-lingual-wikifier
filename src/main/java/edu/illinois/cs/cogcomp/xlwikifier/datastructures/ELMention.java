package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

import edu.illinois.cs.cogcomp.xlwikifier.core.RankerFeatureManager;
import org.apache.commons.lang.builder.EqualsBuilder;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.SearchResult;

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
    private String mention;
    public String docid;
    private String type;
    private int start_offset;
    private int end_offset;
    private String language;
    private String mid = "NIL";
    private String freebase_title = "NIL";
    private String wiki_title = "NIL";
    public String en_wiki_title = "NIL";
    private String eng_para;
    private String spa_para;
    private String eng_mention;
    private List<WikiCand> cands = new ArrayList<>();
    public List<String> gold_titles = new ArrayList<>();
    private int eng_start;
    private int eng_end;
    private boolean orig_per = false;
    public List<SearchResult> top_results = null;
    public List<String> wiki_titles = new ArrayList<>();
//    private transient Double[] mid_vec;
    private transient Float[] mid_vec;
    private transient Double[] mid_vec1;
    public String gold_enwiki_title;
    public String gold_mid;
    public int plain_start;
    public int plain_end;
    public int xml_start;
    public int xml_end;
    public boolean eazy;
//    public transient Double[] context30;
//    public transient Double[] context100;
//    public transient Double[] context200;
//    public transient Double[] other_ne;
//    public transient Double[] pre_title;
//public transient List<Double[]> pre_title_vecs;
//    public transient List<Double[]> other_ne_vecs;
//public transient Double[] mention_vec;
    public String gold_wiki_title;
    public transient Float[] context30;
    public transient Float[] context100;
    public transient Float[] context200;
    public transient Float[] other_ne;
    public transient Float[] pre_title;
    public transient List<Float[]> pre_title_vecs;
    public transient List<Float[]> other_ne_vecs;
    public transient Float[] mention_vec;
	public String pred_type;
    public List<String> trans;
    public String gold_lang;
    public transient Map<Integer, Double> idx2valsum = new HashMap<>();
    public String en_gold_trans;
    public boolean is_ne;
    public boolean is_ne_gold;
    public int ngram;
    public boolean is_stop;
    public List<String> types = new ArrayList<>();
    public Map<String, Double> ner_features = new HashMap<>();
    public String noun_type;

    public ELMention(){};

    public ELMention(String docid, int start, int end){
        this.docid = docid;
        this.start_offset = start;
        this.end_offset = end;
    }

    public ELMention(String id, String mention, String docid){
        this.id = id;
        this.mention = mention;
        this.docid = docid;
    }

    public void setType(String type){ this.type = type; }
    public void setLanguage(String lang){ this.language = lang; }
    public void setStartOffset(int start){ this.start_offset = start; }
    public void setEndOffset(int end){ this.end_offset = end; }
    public void setMid(String ans){ this.mid = ans; }
    public void setMention(String mention){
        this.mention = mention;
    }
    public void setFBTitle(String t){ this.freebase_title = t; }
    public void setWikiTitle(String t){ this.wiki_title = t; }
    public void setEngPara(String t){ this.eng_para = t; }
    public void setSpaPara(String t){ this.spa_para = t; }
    public void setEngMention(String t){ this.eng_mention = t;}
    public void setEngStart(int s){ this.eng_start = s; }
    public void setEngEnd(int s){ this.eng_end = s; }
    public void setOrigPer(){ this.orig_per = true; }
    public boolean isOrigPer(){ return this.orig_per; }
    public void setNounType(String t){ this.noun_type = t; }
    public String getNounType(){ return this.noun_type; }
    public void setMidVec(Float[] vec){ this.mid_vec = vec; }
    public void setMidVec1(Double[] vec){ this.mid_vec1 = vec; }
    public Float[] getMidVec(){ return this.mid_vec; }
    public Double[] getMidVec1(){ return this.mid_vec1; }

    public String getID(){ return this.id; }
    public String getMention(){ return this.mention; }
    public String getDocID(){ return this.docid; }
    public int getEngStart(){ return this.eng_start; }
    public int getEngEnd(){ return this.eng_end; }
    public String getType(){ return this.type; }
    public String getLanguage(){ return this.language; }
    public int getStartOffset(){ return this.start_offset; }
    public int getEndOffset(){ return this.end_offset; }
    public String getMid(){ return this.mid; }
    public String getGoldMidOrNIL(){
        if(gold_mid.startsWith("NIL"))
            return "NIL";
        return this.gold_mid; }
    public String getEngPara(){ return this.eng_para; }
    public String getSpaPara(){ return this.spa_para; }
    public String getEngMention(){ return this.eng_mention;}
    public String getWikiTitle(){ return this.wiki_title; }
    public String getFBTitle(){ return this.freebase_title; }
    public String getGoldMid(){ return this.gold_mid; }

    @Override
    public String toString() {
    	return mention+" "+start_offset+","+end_offset;
    }

    public void setCandidates(List<WikiCand> wikiCandidates) {
        this.cands = wikiCandidates;
    }

    public List<WikiCand> getCandidates(){
        return this.cands;
    }

    public void prepareFeatures(QueryDocument doc, RankerFeatureManager fm, List<ELMention> pms){
        context30 = fm.getWeightedContextVector(this, doc, 30);
        context100 = fm.getWeightedContextVector(this, doc, 100);
        context200 = fm.getWeightedContextVector(this, doc, 200);

        if(!fm.ner_mode) {
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

    @Override
    public boolean equals(Object obj){
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
