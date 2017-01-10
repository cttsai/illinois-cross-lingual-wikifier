package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.postprocessing.SurfaceClustering;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Test nil cluster behavior.
 * @author mssammon
 */
public class TestNilCluster {

    private static final String fileA = "src/test/resources/edu/illinois/cs/cogcomp/xlwikifier/synth-nil.txt";
    private static final String fileB = "src/test/resources/edu/illinois/cs/cogcomp/xlwikifier/synth-nil-two.txt";

    @Test
    public void testNilCluster() {

        String[] files = new String[]{fileA, fileB};
        List<QueryDocument> qdocs = new ArrayList<>(2);
        String config = "config/xlwikifier-demo.config";

        Language lang = Language.English;
        TextAnnotationBuilder tokenizer = MultiLingualTokenizer.getTokenizer(lang.getCode());

        MultiLingualNER annotator = null;
        try {
            annotator = new MultiLingualNER(lang, config);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        CrossLingualWikifier xlwikifier = null;
        try {
            xlwikifier = new CrossLingualWikifier(lang, config);
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        for ( String file : files ) {
            String fileText = null;
            try {
                fileText = LineIO.slurp(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                fail(e.getMessage());
            }

            TextAnnotation ta = tokenizer.createTextAnnotation("test", file, fileText);

            annotator.addView(ta);

            xlwikifier.addView(ta);
            qdocs.add(CrossLingualWikifier.ta2QueryDoc(ta, annotator.getViewName()));
        }

        SurfaceClustering.NILClustering(qdocs, 3);
    }
}
