package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.indsup.learning.LexManager;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiDocReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;

import java.io.*;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * Created by ctsai12 on 10/10/15.
 */
public class Ranker {

    public RankerFeatureManager fm;
    private double[] model;
    private static Logger logger = LoggerFactory.getLogger(Ranker.class);

    public Ranker(){

    }
    public Ranker(String lang){
        fm = new RankerFeatureManager(lang);
    }

    public void closeDBs(){
        fm.we.closeDB();
    }

    public static Ranker loadPreTrainedRanker(String lang, String filepath){
        logger.info("Loading ranker model: "+filepath);
        if(!new File(filepath).exists()){
            logger.error("Model doesn't exist: "+filepath);
            System.exit(-1);
        }
        if(!new File(filepath+".lm").exists()){
            logger.error("Lexical manager doesn't exist: "+filepath+".lm");
            System.exit(-1);
        }
        Ranker ranker = new Ranker(lang);
        ranker.readModel(filepath);
        ranker.loadLexicalManager(filepath+".lm");
        return ranker;
    }

    public void readModel(String filepath){
        List<String> lines = null;
        try {
            lines = LineIO.read(filepath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        int start = lines.indexOf("w")+1;
        if(start == 0) return;
        this.model = lines.subList(start, lines.size()).stream().mapToDouble(x -> Double.parseDouble(x)).toArray();
    }

    public void saveLexicalManager(String filepath){
        logger.info("saving lecical manager...");
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(new File(filepath)));
            oos.writeObject(fm.lex);
            oos.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void loadLexicalManager(String filepath){

        logger.info("loading lexical manager...");
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filepath));
            LexManager lm = (LexManager) ois.readObject();
            fm.lex = lm;
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public void train(List<QueryDocument> docs, String modelname){
        try {
            writeSVMData(docs, modelname);
        } catch (IOException e) {
            e.printStackTrace();
        }
        trainSVM(modelname);
        this.readModel(modelname);
    }

    public void writeSVMData(List<QueryDocument> docs, String name)throws IOException {
        System.out.println("Generating svm data...");
        BufferedWriter bw = new BufferedWriter(new FileWriter("svmdata/tmp"));
        int mcnt = 0, dcnt = 0, qid = 1;
        for(QueryDocument doc: docs){
            dcnt++;
            if(doc.mentions == null || doc.mentions.size()==0) continue;
            doc.prepareFeatures(fm);
            for(int i = 0; i < doc.mentions.size(); i++){
                if ((mcnt++) % 100 == 0) System.out.print("===========> processed #docs: "+dcnt +" #mentions: "+mcnt+" <========== \r");
                ELMention m = doc.mentions.get(i);
                m.setMidVec(fm.we.getTitleVector(m.gold_wiki_title, m.gold_lang));
                m.prepareFeatures(doc, fm, doc.mentions.subList(0, i));
                for (WikiCand cand : m.getCandidates()) {
                    String fea = getFeatureString(cand, m, doc, qid);
                    if (fea == null) continue; //fea = "0 qid:100000 1:0";
                    if(!m.gold_wiki_title.startsWith("NIL"))
                        bw.write(fea + "\n");
                }
                qid++;
            }
        }
        System.out.println();
        System.out.println("#training mentions: "+mcnt);
        bw.close();
    }

    public void trainSVM(String name){
        System.out.println("Training...");
        if(name.contains("/")) {
            File dir = new File(name.substring(0, name.lastIndexOf("/")));
            if(!dir.isDirectory() && !dir.exists())
                dir.mkdirs();
        }

        executeCmd("liblinear-ranksvm-1.95/train -c 0.01 svmdata/tmp " + name );
    }

    public void setWikiTitleByTopCand(List<QueryDocument> docs){
        for(QueryDocument doc: docs){
            setWikiTitleByTopCand(doc);
        }
    }

    public void setWikiTitleByTopCand(QueryDocument doc){
        for(ELMention m: doc.mentions){
            if(m.getCandidates().size()>0)
                m.setWikiTitle(m.getCandidates().get(0).getTitle());
            else
                m.setWikiTitle("NIL");
        }
    }

    public void setWikiTitleByModel(List<QueryDocument> docs){
        logger.info("Ranking...");
        logger.info("#mentions:"+docs.stream().flatMap(x -> x.mentions.stream()).count());
        int dcnt = 0;
        for(QueryDocument doc: docs) {
            dcnt++;
            System.out.print("Proccessed #docs:" + dcnt + "\r");
            setWikiTitleByModel(doc);
        }
    }


    /**
     * Use wikipedia titles stored in mentions as candidates
     * @param doc
     */
    public void setWikiTitleByModel(QueryDocument doc) {
        doc.prepareFeatures(fm);
        for(int i = 0; i < doc.mentions.size(); i++){
            ELMention m = doc.mentions.get(i);
            if(m.is_ne) continue;
            m.prepareFeatures(doc, fm, doc.mentions.subList(0, i));
            List<WikiCand> cands = m.getCandidates();
            for (WikiCand cand : cands) {
                double score = getScoreByModel(m, cand, doc);
                cand.setScore(score);
            }

            if (cands.size() > 0) {
                cands = cands.stream().sorted((x1, x2) -> Double.compare(x2.getScore(), x1.getScore())).collect(toList());
                m.setCandidates(cands);

                m.setWikiTitle(cands.get(0).getTitle());
                m.setMidVec(fm.we.getTitleVector(m.getWikiTitle(), cands.get(0).lang));
            } else {
                m.setWikiTitle("NIL");
                m.setMidVec(null);
            }
        }
    }

    public String getFeatureString(WikiCand cand, ELMention m, QueryDocument doc, int qid){
        String ret = "";
        FeatureVector fv = fm.getTitleFV(m, cand, doc);
        int[] idx = fv.getIdx();
        double[] value = fv.getValue();
        if(idx.length == 0 ) return null;
        if(m.gold_wiki_title.toLowerCase().equals(cand.getTitle().toLowerCase()))
            ret += "2 qid:"+qid;
        else
            ret += "1 qid:"+qid;
        for (int i = 0; i < idx.length; i++) {
            double v = value[i];
            ret += " " + idx[i] + ":" + String.format("%.7f", v);
        }
        return ret;
    }


    public double getScoreByModel(ELMention m, WikiCand cand, QueryDocument doc){

        FeatureVector fv = fm.getTitleFV(m, cand, doc);
        int[] idx = fv.getIdx();
        double[] value = fv.getValue();

        double score = 0.0;
        for(int i = 0; i < idx.length; i++) {
            if(idx[i] > model.length) continue;
            score += value[i] * model[idx[i] - 1];
        }
        return score;
    }

    public void executeCmd(String cmd){
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmd);
            p.waitFor();
            p.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static Ranker trainRanker(String lang, int n_docs, double ratio, String modelfile){
        Ranker ranker = new Ranker(lang);
        ranker.fm.ner_mode = false;

        WikiDocReader reader = new WikiDocReader();
        List<QueryDocument> docs = reader.readWikiDocsNew(lang, 0, n_docs);

        FreeBaseQuery.loadDB(true);
        WikiCandidateGenerator wcg = new WikiCandidateGenerator(true);
        wcg.genCandidates(docs, lang);
        wcg.selectMentions(docs, ratio);
        ranker.train(docs, modelfile);
        ranker.saveLexicalManager(modelfile+".lm");
        return ranker;
    }

    public static void main(String[] args) {

        ConfigParameters param = new ConfigParameters();
        param.setPropValues();

        String lang = args[0];

        trainRanker(lang, 20000, 0.5, "models/ranker/default/"+lang+"/ranker.model");
    }

}
