package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.xlwikifier.*;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.PostProcessing;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.SurfaceClustering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;

import java.io.FileNotFoundException;
import java.io.IOException;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.io.File;

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

    private static final String NAME = TAC2016Eval.class.getCanonicalName();

    private static Logger logger = LoggerFactory.getLogger(TAC2016Eval.class);
    private static List<ELMention> golds;

    private static int span_cnt = 0, ner_cnt = 0, link_cnt = 0;
    private static double pred_total = 0;
    private static double gold_total = 0;

    private static int match_nil = 0, has_cand = 0, has_cand1 = 0;

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

                        if(gm.gold_mid.startsWith("NIL")){
                            match_nil++;
                            if(m.getCandidates().size()>0)
                                has_cand++;

                            if(!m.getMid().startsWith("NIL"))
                                has_cand1++;

                        }
                    }
                    break;
                }
            }
        }
        pred_total += doc.mentions.size();
    }

	public static void printEvalFormat(List<QueryDocument> docs, String outfile){

		String out = "";
		String out1 = "";
		int cnt = 0;
		for(QueryDocument doc: docs){
			for(ELMention m: doc.mentions){
				out += doc.getDocID()+"\t"+m.getStartOffset()+"\t"+(m.getEndOffset()-1)+"\t"+m.getMid()+"\t0\t"+m.getType()+"\n";
                out1 += doc.getDocID()+"\t"+m.getStartOffset()+"\t"+(m.getEndOffset()-1)+"\t"+m.getSurface()+"\t"+m.getWikiTitle()+"\t"+m.getMid()+"\t0\t"+m.getType()+"\n";
			}
		}

		try {
			FileUtils.writeStringToFile(new File(outfile), out, "UTF-8");
            FileUtils.writeStringToFile(new File(outfile+".more"), out1, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

    public static void main(String[] args) {

        if(args.length < 2){
            logger.error("Usage: " + NAME + " <language code=es|zh> configFile [<failOnReadError=true|false");
        }

        String config = args[1];
        try {
            ConfigParameters.setPropValues(config);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        boolean failOnReadError = false;

        if (args.length == 3)
            failOnReadError = Boolean.parseBoolean(args[2]);

        TACDataReader reader = new TACDataReader(failOnReadError);
        Language lang = null;
        List<QueryDocument> docs = null;

        try {
            if (args[0].equals("zh")) {
                lang = Language.Chinese;
                docs = reader.read2016ChineseEvalDocs();
                golds = reader.read2016ChineseEvalGoldNAM();
            } else if (args[0].equals("es")) {
                lang = Language.Spanish;
                docs = reader.read2016SpanishEvalDocs();
                golds = reader.read2016SpanishEvalGoldNAM();
            } else if (args[0].equals("en")) {
                lang = Language.English;
                docs = reader.read2016EnglishEvalDocs();
                golds = reader.read2016EnglishEvalGoldNAM();
            } else
                logger.error("Unknown language: " + args[0]);
        }
        catch ( IOException e ) {
            e.printStackTrace();
            System.exit(-1);
        }

        MultiLingualNER mlner = MultiLingualNERManager.buildNerAnnotator(lang, config);

        CrossLingualWikifier xlwikifier = CrossLingualWikifierManager.buildWikifierAnnotator(lang, config);

        for(QueryDocument doc: docs){

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

            // cluster mentions based on surface forms
            doc.mentions = SurfaceClustering.cluster(doc.mentions);

        }

		SurfaceClustering.NILClustering(docs, 3);
		printEvalFormat(docs, "tac."+args[0]+".results");

		for(QueryDocument doc: docs)
            evaluate(doc);
        System.out.println("#golds: "+gold_total);
        System.out.println("#preds: "+pred_total);
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

        System.out.println("#matched nil "+match_nil+" has cand "+has_cand+" non-NIL mid "+has_cand1);
    }
}
