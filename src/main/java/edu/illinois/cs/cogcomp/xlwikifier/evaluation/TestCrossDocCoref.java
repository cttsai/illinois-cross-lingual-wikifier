package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.xlwikifier.*;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.PostProcessing;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.SurfaceClustering;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Created by ctsai12 on 12/3/16.
 */
public class TestCrossDocCoref {

    public static void main(String[] args) {

        String config = "config/test-coref.config";
        ConfigParameters.setPropValues(config);
        int[] nds = {5000, 2000, 1000, 200};

        for (int ndoc : nds) {
            Language lang = Language.Spanish;
            List<QueryDocument> docs = TACDataReader.readSpanishEvalDocs(ndoc);

            MultiLingualNER mlner = MultiLingualNERManager.buildNerAnnotator(lang, config);

            CrossLingualWikifier xlwikifier = CrossLingualWikifierManager.buildWikifierAnnotator(lang, config);

            double starttime = System.currentTimeMillis();
            int cnt = 0;
            for (QueryDocument doc : docs) {
                System.out.println(cnt++);

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
                doc.mentions = SurfaceClustering.cluster(doc.mentions);

            }
            double totaltime = (System.currentTimeMillis() - starttime) / 1000.0;
            System.out.println("Time " + totaltime + " secs");

            starttime = System.currentTimeMillis();
            SurfaceClustering.crossDocCoref(docs);
            totaltime = (System.currentTimeMillis() - starttime) / 1000.0;
            System.out.println("Time " + totaltime + " secs");

        }
    }
}
