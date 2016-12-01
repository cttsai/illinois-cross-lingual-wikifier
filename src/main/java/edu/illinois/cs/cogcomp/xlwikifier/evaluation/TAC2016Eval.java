package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.xlwikifier.*;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.PostProcessing;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.SurfaceClustering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class runs MultiLingualNER and CrossLingualWikifier on TAC-KBP 2016 EDL dataset.
 * It only evaluates on Spanish and Chinese named entity annotations
 *
 * The paths to the data are specified in config/xlwikifier-tac.config
 *
 * It can be run by executing "scripts/run-benchmark.sh es" or "scripts/run-benchmark.sh zh"
 *
 * Created by ctsai12 on 10/27/16.
 */
public class TAC2016Eval {

    private static Logger logger = LoggerFactory.getLogger(TAC2016Eval.class);
    private static List<ELMention> golds;

    private static int span_cnt = 0, ner_cnt = 0, link_cnt = 0;
    private static double pred_total = 0;
    private static double gold_total = 0;

    public static void evaluate(QueryDocument doc){

        List<ELMention> doc_golds = golds.stream().filter(x -> x.getDocID().equals(doc.getDocID()))
                .collect(Collectors.toList());

        gold_total += doc_golds.size();

        for(ELMention m: doc.mentions){

            for(ELMention gm: doc_golds){
                if(m.getStartOffset() == gm.getStartOffset() && m.getEndOffset() == gm.getEndOffset()){
                    span_cnt++;
                    if(m.getType().equals(gm.getType())){
                        ner_cnt++;
                        if(m.getMid().startsWith("NIL")){
                            if(gm.gold_mid.startsWith("NIL"))
                                link_cnt++;
                        }
                        else{
                            if(m.getMid().equals(gm.gold_mid))
                                link_cnt++;
                        }
                    }
                    break;
                }
            }
        }
        pred_total += doc.mentions.size();

    }

    public static void main(String[] args) {

        if(args.length < 2){
            logger.error("Require two arguments: language and config file");
        }

        String config = args[1];
        ConfigParameters.setPropValues(config);

        List<QueryDocument> docs = null;
        Language lang = null;
        if(args[0].equals("zh")){
            lang = Language.Chinese;
            docs = TACDataReader.readChineseEvalDocs();
            golds = TACDataReader.readChineseGoldNAM();
        }
        else if(args[0].equals("es")){
            lang = Language.Spanish;
            docs = TACDataReader.readSpanishEvalDocs();
            golds = TACDataReader.readSpanishGoldNAM();
        }
        else
            logger.error("Unknown language: "+args[0]);

        MultiLingualNER mlner = MultiLingualNERManager.buildNerAnnotator(lang, config);

        CrossLingualWikifier xlwikifier = CrossLingualWikifierManager.buildWikifierAnnotator(lang, config);

        for(QueryDocument doc: docs){
//            if(!doc.getDocID().equals("CMN_DF_000191_20160428_G00A0D3AC"))
//                continue;

            logger.info("Working on document: "+doc.getDocID());

            // ner
            mlner.annotate(doc);

            // clean mentions contain xml tags
            PostProcessing.cleanSurface(doc);

            // wikification
            xlwikifier.annotate(doc);

            // map plain text offsets to xml offsets
            TACUtils.setXmlOffsets(doc);

            // add author mentions inside xml tags
            TACUtils.addPostAuthors(doc);

            // remove mentions between <quote> and </quote>
            TACUtils.removeQuoteMentions(doc);

            // simple coref to re-set short mentions' title
            PostProcessing.fixPerAnnotation(doc);

            // NIL clustering
            SurfaceClustering.cluster(doc);

            evaluate(doc);
        }

        double rec = span_cnt/gold_total;
        double pre = span_cnt/pred_total;
        double f1 = 2*rec*pre/(rec+pre);
        System.out.print("Mention Span: ");
        System.out.printf("Precision:%.4f Recall:%.4f F1:%.4f\n", pre, rec, f1);

        rec = ner_cnt/gold_total;
        pre = ner_cnt/pred_total;
        f1 = 2*rec*pre/(rec+pre);
        System.out.print("Mention Span + Entity Type: ");
        System.out.printf("Precision:%.4f Recall:%.4f F1:%.4f\n", pre, rec, f1);

        rec = link_cnt/gold_total;
        pre = link_cnt/pred_total;
        f1 = 2*rec*pre/(rec+pre);
        System.out.print("Mention Span + Entity Type + FreeBase ID: ");
        System.out.printf("Precision:%.4f Recall:%.4f F1:%.4f\n", pre, rec, f1);
    }
}
