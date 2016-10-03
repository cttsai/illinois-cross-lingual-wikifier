package edu.illinois.cs.cogcomp.mlner.experiments;

import com.google.gson.Gson;
import edu.illinois.cs.cogcomp.mlner.core.Utils;
import edu.illinois.cs.cogcomp.mlner.experiments.conll.ColumnFormatReader;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.TAC2015Exp;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.mlner.core.NERClassifier;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * This class reads documents in the column format and produces wikifier features
 * for each token. The wikifier features are appended in the end of each line.
 * This is used to produce the data in the cross-lingual NER paper.
 * Created by ctsai12 on 3/2/16.
 */
public class GenWikifierFeatures {

    private Utils utils;
    private NERClassifier unic;
    private static Logger logger = LoggerFactory.getLogger(GenWikifierFeatures.class);

    public GenWikifierFeatures(){
        utils = new Utils();
    }


    /**
     * Wikify the n-grams and cache them
     */
    public void genWikifiedCache(List<QueryDocument> docs, String lang, String name){
        logger.info("Generating wikifier cache...");
        Ranker ranker = Ranker.loadPreTrainedRanker(lang, "ranker/ner/"+lang);
        WikiCandidateGenerator wcg = new WikiCandidateGenerator(true);
        utils.setLang(lang);
        String dir = "/shared/bronte/ctsai12/multilingual/2015data/"+lang+"/"+name+"/";
        for(int i = 1; i <= 4; i ++) {
            utils.genNERTrainingCache(docs, dir + i, i, wcg, ranker);
        }
        wcg.closeDB();
    }


    public List<QueryDocument> loadDocCache(String dir){
        logger.info("loading document cache from "+dir);
        Gson gson = new Gson();
        File df = new File(dir);
        List<QueryDocument> docs = new ArrayList<>();
        for(File f: df.listFiles()) {
            String json = null;
            try {
                json = FileUtils.readFileToString(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
            QueryDocument doc = gson.fromJson(json, QueryDocument.class);
            for(ELMention m: doc.mentions)
                m.ner_features = new HashMap<>();
            docs.add(doc);
        }
        return docs;
    }

    /**
     * Read wikified documents from the cache and generate wikifier features
     */
    private void genNERFeatures(String dir, String outpath, String lang, List<TextAnnotation> tas){

        Map<String, List<ELMention>> did2ms = new HashMap<>();
        List<QueryDocument> docs = null;
        for(int n = 4; n >0; n--) {
            docs = loadDocCache(dir+n);
            docs.forEach(x -> x.mentions.forEach(y -> y.setLanguage(lang)));
            utils.propFeatures(docs, did2ms);
            unic.extractFeatures(docs, n);
            did2ms = new HashMap<>();
            for(QueryDocument doc: docs){
                did2ms.put(doc.getDocID(), doc.mentions);
            }
        }
        utils.setBIOGolds(docs);
        unic.writeFeatures(docs, tas, outpath, true);
    }


    public void genFeaturesFromWikifiedDocs(List<QueryDocument> docs, String lang, String cachename, String name){
        utils.setLang(lang);
        unic = new NERClassifier(lang);
        String train_in_dir = "/shared/bronte/ctsai12/multilingual/2015data/"+lang+"/"+cachename+"/";
        String train_out_dir = "/shared/corpora/ner/wikifier-features/"+lang+"/"+name+"/";
        List<TextAnnotation> tas = docs.stream().map(x -> x.getTextAnnotation()).collect(Collectors.toList());

        genNERFeatures(train_in_dir, train_out_dir, lang, tas);
    }


    public static void main(String[] args) {
        FreeBaseQuery.loadDB(true);
        GenWikifierFeatures ce = new GenWikifierFeatures();

        String lang = "zh";

        ColumnFormatReader reader = new ColumnFormatReader();
        String cachename = "tac2015-train12-char-prop";
//        String cachename = "ere-NOM-head";
        String in_dir = "/shared/corpora/ner/tac/zh/train-new12-char-prop";
//        String in_dir = "/shared/corpora/ner/ere/es/NOM-head";
        reader.readDir(in_dir, false);
        ce.genWikifiedCache(reader.docs, lang, cachename);

        reader.readDir(in_dir, false);
        ce.genFeaturesFromWikifiedDocs(reader.docs, lang, cachename, cachename);

//        else {
//            reader.loadCoNLLSubmission(lang, part);
//            ce.genFeaturesFromWikifiedDocs(reader.docs, args[0], cachename, conllname);
//        }
    }
}
