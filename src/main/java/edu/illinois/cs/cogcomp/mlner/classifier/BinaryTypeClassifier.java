package edu.illinois.cs.cogcomp.mlner.classifier;

import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.indsup.learning.FeatureVector;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import org.apache.commons.io.FileUtils;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.*;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.QueryMQL;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.DocumentReader;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.MentionReader;
import structure.MulticlassClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import LBJ3.learn.SparseAveragedPerceptron;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 9/29/15.
 */
public class BinaryTypeClassifier {
    private FeatureManager fm;
    private List<String> label2type = new ArrayList<>();
    private MulticlassClassifier mc;
    Set<String> goodtypes = null;
    QueryMQL qm = new QueryMQL();

    public BinaryTypeClassifier(){
        fm = new FeatureManager();
        SparseAveragedPerceptron.Parameters p = new SparseAveragedPerceptron.Parameters();
        p.learningRate = 0.1;
        p.thickness = 1.0;
        SparseAveragedPerceptron baseLTU = new SparseAveragedPerceptron(p);
        mc = new MulticlassClassifier(baseLTU);
        MentionReader mr = new MentionReader();
        try {
            goodtypes = mr.getTopTrainTypes(5).entrySet().stream().flatMap(x -> x.getValue().stream()).collect(toSet());
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(!FreeBaseQuery.isloaded())
            FreeBaseQuery.loadDB(true);
    }

//    private void getExampleCache(){
//        MentionReader mr = new MentionReader();
//        List<ELMention> golds = mr.readTrainingMentionsEng();
//        Set<String> spagolds = mr.readTrainingMentionsSpa().stream().map(x -> x.getGoldMidOrNIL())
//                .filter(x -> !x.equals("NIL")).collect(toSet());
//        golds = golds.stream().filter(x -> !spagolds.contains(x.getGoldMid())).collect(toList());
//        Map<String, List<ELMention>> doc2golds = golds.stream().collect(groupingBy(x -> x.getDocID()));
//        DocumentReader dr = new DocumentReader();
//        List<QueryDocument> docs = dr.readEnglishDocuments(true);
//        QueryMQL qm = new QueryMQL();
//
//        try {
//            GlobalParameters.loadConfig("config/TAC2015.xml");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//        Set<String> negative_ids = new HashSet<>();
//        Set<String> positive_ids = new HashSet<>();
//        for(QueryDocument doc: docs) {
//            if(!doc2golds.containsKey(doc.getDocID()))
//                continue;
//
//            System.out.println("doc id:" + doc.getDocID());
//
//            TextAnnotation ta = null;
//            LinkingProblem problem = null;
//            try {
//                ta = GlobalParameters.curator.getTextAnnotation(doc.getTranslatedText());
//                problem = new LinkingProblem(doc.getDocID(), ta, new ArrayList<>());
//                InferenceEngine engine = new InferenceEngine(false);
//                engine.annotate(problem, null, false, false, -1);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            List<Mention> nes = problem.components.stream()
//                    .filter(x -> x.isNamedEntity())
//                    .collect(toList());
//            for(Mention m: nes){
//                if(m.getTopTitle() == null) continue;
//                String mid = qm.lookupMidFromTitle(m.getTopTitle(), "en");
//                if(mid == null) continue;
//                Pair<Integer, Integer> offsets = doc.getXmlOffsets(m);
//                if(offsets!=null){
//                    if(isOverlap(offsets.getFirst(), offsets.getSecond()-1, doc2golds.get(doc.getDocID())))
//                        positive_ids.add(mid);
//                    else
//                        negative_ids.add(mid);
//                }
//            }
//        } // doc
//
//
//        try {
//            FileUtils.writeStringToFile(new File("2015data/binary_type_classifier/negative.ne"), negative_ids.stream().collect(joining("\n")),"UTF-8");
//            FileUtils.writeStringToFile(new File("2015data/binary_type_classifier/positive.ne"), positive_ids.stream().collect(joining("\n")),"UTF-8");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

//    private void cacheNegMidsSpa(){
//        DocumentReader dr = new DocumentReader();
//        List<QueryDocument> docs = dr.readSpanishDocuments();
//        EDLSolver solver = new EDLSolver();
//        List<ELMention> mentions = null;
//        try {
//            mentions = solver.answerQueries(docs, true);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        MentionReader mr = new MentionReader();
//        List<ELMention> golds = mr.readTrainingMentionsSpa();
//        Map<String, List<ELMention>> doc2golds = golds.stream().filter(x -> !x.getGoldMid().startsWith("NIL"))
//                .collect(groupingBy(x -> x.getDocID()));
//        List<String> negs = new ArrayList<>();
//        for(ELMention m: mentions){
//            if(m.getGoldMid().startsWith("NIL")) continue;
//
//            if(!isOverlap(m.getStartOffset(), m.getEndOffset(), doc2golds.get(m.getDocID()))){
//                negs.add(m.getGoldMid());
//            }
//        }
//
//        try {
//            FileUtils.writeStringToFile(new File("negative.spa"), negs.stream().collect(joining("\n")),"UTF-8");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private Set<String> getPositiveMidsEng(boolean dev){
        MentionReader mr = new MentionReader();
        List<ELMention> golds = mr.readTrainingMentionsEng();
        if(dev) {
            Set<String> spagolds = mr.readTrainingMentionsSpa().stream().map(x -> x.getGoldMidOrNIL())
                    .filter(x -> !x.equals("NIL")).collect(toSet());
            golds = golds.stream().filter(x -> !spagolds.contains(x.getGoldMid())).collect(toList());
        }
        return golds.stream().filter(x -> !x.getGoldMid().startsWith("NIL"))
                .map(x -> x.getGoldMid()).collect(toSet());
    }

    private Set<String> getPositiveMidsSpa(){
        MentionReader mr = new MentionReader();
        List<ELMention> golds = mr.readTrainingMentionsSpa();
        return golds.stream().filter(x -> !x.getGoldMid().startsWith("NIL"))
                .map(x -> x.getGoldMid()).collect(toSet());
    }

    private Set<String> loadMID(String file){
        try {
//            return LineIO.read("binary.negative.mid")
            return LineIO.read(file)
                    .stream().collect(toSet());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void train(boolean dev) {

        List<Pair<FeatureVector, Integer>> train_data = new ArrayList<>();

//        Set<String> pos_ids = getPositiveMidsEng(dev);
//        Set<String> neg_ids = loadMID("/shared/bronte/ctsai12/multilingual/2015data/binary_type_classifier/train_data/binary.negative.mid");
//
//        if(!dev){
//            pos_ids.addAll(getPositiveMidsSpa());
//            neg_ids.addAll(loadMID("/shared/bronte/ctsai12/multilingual/2015data/binary_type_classifier/train_data/negative.spa"));
//        }

        Set<String> pos_ids = loadMID("/shared/bronte/ctsai12/multilingual/2015data/token.bi.pos");
        Set<String> neg_ids = loadMID("/shared/bronte/ctsai12/multilingual/2015data/token.bi.neg");

//        System.out.println("#neg before removing pos: " + neg_ids.size());
        neg_ids.removeAll(pos_ids);
//        System.out.println("#neg after removing pos: " + neg_ids.size());

        // adding positive examples
        int pos = 0, neg = 0;
        for (String mid : pos_ids) {
            FeatureVector fv = fm.getFV(mid);
            if(fv.getValue().length == 0)
                continue;
            train_data.add(new Pair<>(fv, 1));
            pos++;
        }

        // adding negative examples
        for (String mid : neg_ids) {
//            Set<String> types = qm.lookupTypeFromMid(mid).stream().collect(toSet());
            Set<String> types = FreeBaseQuery.getTypesFromMid(mid).stream().collect(Collectors.toSet());
            types.retainAll(goodtypes);
            if(types.size()>0) continue;

            FeatureVector fv = fm.getFV(mid);
            if(fv.getValue().length == 0)
                continue;
            train_data.add(new Pair<>(fv, 2));
            neg++;
        }


        System.out.println("#POS: " + pos + " #NEG: " + neg);
        Collections.shuffle(train_data);
        mc.learn(train_data, 100);
    }


    public boolean isTheTypes(String mid){
//        List<String> types = qm.lookupTypeFromMid(mid);
//        types.retainAll(goodtypes);
//        if(types.size()>0) return true;
        return mc.getLabel(fm.getFV(mid)) == 1;
    }

    public boolean isOverlap(int start, int end, List<ELMention> ms){

        if(ms == null)
            return false;

        for(ELMention m: ms){
            if((start >= m.getStartOffset() && start <= m.getEndOffset())
                    || (end >= m.getStartOffset() && end <= m.getEndOffset()))
                return true;
        }
        return false;
    }

    public static void main(String[] args) {

        BinaryTypeClassifier bc = new BinaryTypeClassifier();
//        bc.getExampleCache();
//        bc.cacheNegMidsSpa();
//        bc.getNegativeExamples();
        bc.train(true);
//        System.out.println(bc.isTheTypes("m.0hf35"));
        System.exit(-1);
    }


}
