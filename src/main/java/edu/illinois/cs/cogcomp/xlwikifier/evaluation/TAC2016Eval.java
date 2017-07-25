package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.xlwikifier.*;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.PostProcessing;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.SurfaceClustering;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.MediaWikiSearch;
import net.didion.jwnl.dictionary.database.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FileUtils;

import java.io.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

/**
 * This class runs MultiLingualNER and CrossLingualWikifier on TAC-KBP 2016 EDL dataset.
 *
 * The paths to the data are specified in config/xlwikifier-tac.config
 *
 * It can be run by "scripts/run-benchmark.sh"
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

    private static int match_nil = 0, has_cand = 0, has_cand1 = 0, incand = 0;
    private static int mention_cnt = 0;

    public static void evaluate(QueryDocument doc){

        List<ELMention> doc_golds = golds.stream().filter(x -> doc.getDocID().startsWith(x.getDocID()))
                .collect(Collectors.toList());

        gold_total += doc_golds.size();

        for(ELMention m: doc.mentions){

            for(ELMention gm: doc_golds){
                if(m.getStartOffset() == gm.getStartOffset() && m.getEndOffset() == gm.getEndOffset()){
                    span_cnt++;
                    if(m.getType().equals(gm.getType())){
                        ner_cnt++;

                        // correct KB ID prediction
                        if((m.getMid().startsWith("NIL") && gm.gold_mid.startsWith("NIL")) ||
                               m.getMid().equals(gm.gold_mid) ){
                                link_cnt++;
                        }

                        // gold is not NIL, wrong prediction, gold is in candidate set
                        if(!gm.gold_mid.startsWith("NIL") && !gm.gold_mid.equals(m.getMid())){
                            if(m.getCandidates()!=null) {
                                Set<String> cands = m.getCandidates().stream().filter(x -> x != null).map(x -> x.title).collect(Collectors.toSet());
                                if (cands.contains(gm.gold_mid)) {
                                    System.out.println(doc.getDocID()+" "+m.getSurface()+" "+gm.gold_mid+" "+m.getMid()+" "+m.getType()+" "+gm.getType());
                                    for(WikiCand cand: m.getCandidates())
                                        if(cand!=null)
                                            System.out.println("\t"+cand.title+" "+cand.orig_title);

                                    incand++;
                                }
                            }
                        }

                        // NIL gold, but has candidate
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

    public static void printSubmissionFormat(QueryDocument doc, String outfile){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outfile, true));
            for(ELMention m: doc.mentions) {
                bw.write("UI_CCG\t" + mention_cnt + "\t" + m.getSurface() + "\t" + doc.getDocID() + ":" + m.getStartOffset() + "-" + (m.getEndOffset() - 1) + "\t" + m.getMid() + "\t" + m.getType() + "\tNAM\t" + m.confidence + "\t" + m.getEnWikiTitle() + "\n");
                mention_cnt++;
            }
            bw.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    public static void printSubmissionFormat(List<ELMention> mentions, String outfile){
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outfile, true));
            for(ELMention m: mentions) {
                bw.write("UI_CCG\t" + mention_cnt + "\t" + m.getSurface() + "\t" + m.getDocID() + ":" + m.getStartOffset() + "-" + (m.getEndOffset() - 1) + "\t" + m.getMid() + "\t" + m.getType() + "\tNAM\t" + m.confidence + "\t" + m.getEnWikiTitle() + "\n");
                mention_cnt++;
            }
            bw.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static void printEREFormat(QueryDocument doc, String outdir){
        File dir = new File(outdir);
        if(!dir.exists())
            dir.mkdir();
        else
            dir.delete();

        try {

            BufferedWriter bw = new BufferedWriter(new FileWriter(outdir+"/"+doc.getDocID()+".rich_ere.xml"));

            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            bw.write("<deft_ere kit_id=\"kit_id\" doc_id=\""+doc.getDocID()+"\" source_type=\"multi_post\">\n");
            bw.write("\t<entities>\n");

            Map<String, List<ELMention>> mid2ms = doc.mentions.stream().collect(groupingBy(x -> x.getMid()));
            int entity_cnt = 0, mention_cnt = 0;
            for (String mid : mid2ms.keySet()) {
                List<ELMention> ms = mid2ms.get(mid).stream()
                        .sorted((x1, x2) -> Integer.compare(x2.getSurface().length(), x1.getSurface().length()))
                        .collect(Collectors.toList());
                ELMention cano = ms.get(0);
                bw.write("\t\t<entity id=\"ent-"+entity_cnt+"\" type=\""+cano.getType()+"\" specificity=\"specific\" kb_id=\""+mid+"\">\n");

                for(ELMention m: ms){
                    bw.write("\t\t\t<entity_mention id=\"m-"+mention_cnt+"\" noun_type=\""+m.noun_type+"\" source=\""+doc.getDocID()+"\" offset=\""+m.getStartOffset()+"\" length=\""+(m.getEndOffset()-m.getStartOffset()+1)+"\">\n");
                    bw.write("\t\t\t\t<mention_text>"+m.getSurface()+"</mention_text>\n");
                    bw.write("\t\t\t</entity_mention>\n");
                    mention_cnt++;
                }

                bw.write("\t\t</entity>\n");
                entity_cnt++;
            }

            bw.write("\t</entities>\n");
            bw.write("</deft_ere>");
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

	public static void printEvalFormat(List<QueryDocument> docs, String outfile){

		String out = "";
//		String out1 = "";
		int cnt = 0;
		for(QueryDocument doc: docs){
			for(ELMention m: doc.mentions){
				out += doc.getDocID()+"\t"+m.getStartOffset()+"\t"+(m.getEndOffset()-1)+"\t"+m.getMid()+"\t0\t"+m.getType()+"\n";
//                out1 += doc.getDocID()+"\t"+m.getStartOffset()+"\t"+(m.getEndOffset()-1)+"\t"+m.getSurface()+"\t"+m.getWikiTitle()+"\t"+m.getMid()+"\t0\t"+m.getType()+"\n";
			}
		}

		try {
			FileUtils.writeStringToFile(new File(outfile), out, "UTF-8");
//            FileUtils.writeStringToFile(new File(outfile+".more"), out1, "UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void checkGoldMentionTypes(List<ELMention> mentions){

	    for(ELMention m: mentions){

	        if(!m.gold_mid.startsWith("NIL")){
                List<String> types = FreeBaseQuery.getTypesFromMid(m.gold_mid);
//                if(m.getType().equals("LOC") || m.getType().equals("GPE")){
                if(m.getType().equals("GPE")){
                    if(!types.contains("location.location")){
                        System.out.println(m.getSurface()+" "+m.gold_mid+" "+m.getType()+" "+types);
                    }
                }
                else if(m.getType().equals("PER")){
                    if(!types.contains("people.person")){
                        System.out.println(m.getSurface()+" "+m.gold_mid+" "+m.getType()+" "+types);
                    }
                }
                else if(m.getType().equals("ORG")){
                    if(!types.contains("organization.organization")){
//                        System.out.println(m.getSurface()+" "+m.gold_mid+" "+m.getType()+" "+types);
                    }

                }

            }

        }
    }

    public static void main(String[] args) {

        if(args.length < 3){
            logger.error("Usage: " + NAME + " <language code=es|zh> configFile outputFile [<failOnReadError=true|false]");
        }

        String config = args[1];
        try {
            ConfigParameters.setPropValues(config);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        boolean failOnReadError = false;

        String submission_file = "/shared/bronte/Tinkerbell/EDL/cold_start_outputs/es/TAC2016.es.test";
        if (args.length >= 3)
            submission_file = args[2];

        if (args.length == 4)
            failOnReadError = Boolean.parseBoolean(args[3]);

        TACDataReader reader = new TACDataReader(failOnReadError);
        Language lang = null;
        List<QueryDocument> docs = null;


        try {
            if (args[0].equals("zh")) {
                lang = Language.Chinese;
                docs = reader.read2016ChineseEvalDocs();
                golds = reader.read2016ChineseEvalGoldNAM();
//                docs = reader.readDocs("/shared/corpora/corporaWeb/tac/2017/LDC2017E25_TAC_KBP_2017_Evaluation_Source_Corpus/data/cmn/df/", "zh");
            } else if (args[0].equals("es")) {
                lang = Language.Spanish;
                docs = reader.read2016SpanishEvalDocs();
                golds = reader.read2016SpanishEvalGoldNAM();

                // the entire 30k evaluation docs for 2016 and 2017
//                docs = reader.readDocs("/shared/corpora/corporaWeb/tac/LDC2016E63_TAC_KBP_2016_Evaluation_Source_Corpus_V1.1/data/spa/nw/", "es");
//                docs.addAll(reader.readDocs("/shared/corpora/corporaWeb/tac/LDC2016E63_TAC_KBP_2016_Evaluation_Source_Corpus_V1.1/data/spa/df/", "es"));
//                docs = reader.readDocs("/shared/corpora/corporaWeb/tac/2017/LDC2017E25_TAC_KBP_2017_Evaluation_Source_Corpus/data/spa/nw/", "es");
//                docs.addAll(reader.readDocs("/shared/corpora/corporaWeb/tac/2017/LDC2017E25_TAC_KBP_2017_Evaluation_Source_Corpus/data/spa/df/", "es"));
            } else if (args[0].equals("en")) {
                lang = Language.English;
                docs = reader.read2016EnglishEvalDocs();
                golds = reader.read2016EnglishEvalGoldNAM();

                // the entire 30k evaluation docs
//                docs = reader.readDocs("/shared/corpora/corporaWeb/tac/LDC2016E63_TAC_KBP_2016_Evaluation_Source_Corpus_V1.1/data/eng/nw/", "en");
//                docs = reader.readDocs("/shared/corpora/corporaWeb/tac/2017/LDC2017E25_TAC_KBP_2017_Evaluation_Source_Corpus/data/eng/nw/", "en");
            } else
                logger.error("Unknown language: " + args[0]);
        }
        catch ( IOException e ) {
            e.printStackTrace();
            System.exit(-1);
        }

        MultiLingualNER mlner = MultiLingualNERManager.buildNerAnnotator(lang, config);

        CrossLingualWikifier xlwikifier = CrossLingualWikifierManager.buildWikifierAnnotator(lang, config);

        // only author mentions are clustered across documents
        List<ELMention> authors = new ArrayList<>();

        for(int i = 0; i < docs.size(); i++){

            QueryDocument doc = docs.get(i);

            logger.info(i+" Working on document: "+doc.getDocID());

            // ner
            mlner.annotate(doc);

            // clean mentions contain xml tags
            PostProcessing.cleanSurface(doc);

            // wikification
            xlwikifier.annotate(doc);

            // map plain text offsets to xml offsets
            TACUtils.setXmlOffsets(doc);

            // remove mentions between <quote> and </quote>
            TACUtils.removeQuoteMentions(doc);

            // simple coref to re-set short mentions' title
            PostProcessing.fixPerAnnotation(doc);

            // cluster mentions based on surface forms
            doc.mentions = SurfaceClustering.cluster(doc.mentions);

            // this will be used to combine with nominals
            printSubmissionFormat(doc, submission_file);

            // add author mentions inside xml tags
            authors.addAll(TACUtils.getDFAuthors(doc));
            authors.addAll(TACUtils.getNWAuthors(doc));

            if(golds == null)
                docs.set(i, null);
        }

        authors = SurfaceClustering.clusterAuthors(authors);
        printSubmissionFormat(authors, submission_file);

//		printEvalFormat(docs, "tac."+args[0]+".results");

        // Evaluate the results if golds is not null.
        // Note that one can always run the official evaluation script on the output file.
        // This is just for convenience.
        if(golds != null) {
            for (QueryDocument doc : docs) {
                doc.mentions.addAll(authors.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(Collectors.toList()));
                evaluate(doc);
            }
        }
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

        System.out.println("#NER matched NIL "+match_nil+", has cand "+has_cand+", non-NIL mid "+has_cand1);
        System.out.println("#Gold MIDs in cands, but not top: "+incand);
    }
}
