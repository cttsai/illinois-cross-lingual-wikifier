package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.CoreferenceView;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * This class runs MultiLigualNER and CrossLingualWikifier on the Spansih and Chinese sample text,
 * and checks prediction results.
 * <p>
 * Created by ctsai12 on 10/25/16.
 */
public class TestAnnotators {

    final static  String english_input = "Barack Obama is an American politician serving as the 44th President of the United States. " +
            "Born in Honolulu, Hawaii, Obama is a graduate of Columbia University " +
            "and Harvard Law School. He worked as a civil rights attorney and taught constitutional law at University of Chicago Law School " +
            "between 1992 and 2004. He ran unsuccessfully in the Democratic primary for the United States House of Representatives " +
            "in 2000 against incumbent Bobby Rush.";

    final static String spanish_input = "Barack Obama es el cuadragésimo cuarto y actual presidente de " +
            "los Estados Unidos de América. Fue senador por el estado de Illinois desde el 3 de enero de 2005 hasta su " +
            "renuncia el 16 de noviembre de 2008. Además, es el quinto legislador afroamericano en el Senado de los Estados Unidos, " +
            "tercero desde la era de reconstrucción. También fue el primer candidato afroamericano nominado a la presidencia " +
            "por el Partido Demócrata y es el primero en ejercer el cargo presidencial.";

    final static String chinese_input = "巴拉克·歐巴馬是美國民主黨籍政治家，也是第44任美國總統。" +
            "歐巴馬是第一位非裔美國總統。他1961年出生於美國夏威夷州檀香山。1991年，以優等生榮譽從哈佛法學院畢業。" +
            "1996年開始參選公職，在補選中，當選伊利諾州參議員。";

    final private static Map<Pair<Integer, Integer>, ELMention> spanish_answers = new HashMap<>();

    static {
        spanish_answers.put(new Pair<>(0, 12), new ELMention("Barack Obama", 0, 12, "PER", "barack_obama", "barack_obama"));
        spanish_answers.put(new Pair<>(66, 91), new ELMention("Estados Unidos de América", 66, 91, "GPE", "estados_unidos", "united_states"));
        spanish_answers.put(new Pair<>(122, 130), new ELMention("Illinois", 122, 130, "GPE", "illinois", "illinois"));
        spanish_answers.put(new Pair<>(257, 263), new ELMention("Senado", 257, 263, "ORG", "senado", "senate"));
        spanish_answers.put(new Pair<>(271, 285), new ELMention("Estados Unidos", 271, 285, "GPE", "estados_unidos", "united_states"));
        spanish_answers.put(new Pair<>(406, 423), new ELMention("Partido Demócrata", 406, 423, "ORG", "partido_demócrata_(estados_unidos)", "democratic_party_(united_states)"));
    }

    final private static Map<Pair<Integer, Integer>, ELMention> chinese_answers = new HashMap<>();

    static {
        chinese_answers.put(new Pair<>(0, 7), new ELMention("巴拉克·欧巴马", 0, 7, "PER", "贝拉克·奥巴马", "barack_obama"));
        chinese_answers.put(new Pair<>(8, 10), new ELMention("美国", 8, 10, "GPE", "美国", "united_states"));
        chinese_answers.put(new Pair<>(10, 13), new ELMention("民主党", 10, 13, "ORG", "民主党_(美国)", "democratic_party_(united_states)"));
        chinese_answers.put(new Pair<>(24, 26), new ELMention("美国", 24, 26, "GPE", "美国", "united_states"));
        chinese_answers.put(new Pair<>(29, 32), new ELMention("欧巴马", 29, 32, "PER", "贝拉克·奥巴马", "barack_obama"));
        chinese_answers.put(new Pair<>(38, 40), new ELMention("美国", 38, 40, "GPE", "美国", "united_states"));
        chinese_answers.put(new Pair<>(52, 61), new ELMention("美国夏威夷州檀香山", 52, 61, "GPE", "NIL", "NIL0001"));
        chinese_answers.put(new Pair<>(75, 80), new ELMention("哈佛法学院", 75, 80, "ORG", "哈佛法学院", "harvard_law_school"));
        chinese_answers.put(new Pair<>(102, 106), new ELMention("伊利诺州", 102, 106, "GPE", "伊利诺伊州", "illinois"));
    }

    @Test
    public void testSpanishResults() {

        Language lang = Language.Spanish;

        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer(lang.getCode());
        TextAnnotation ta = tokenizer.getTextAnnotation(spanish_input);

        String config = "config/xlwikifier-demo.config";

        MultiLingualNER ner_annotator = null;
        try {
            ner_annotator = new MultiLingualNER(lang, config);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ner_annotator.addView(ta);

        for (Constituent c : ta.getView(ner_annotator.getViewName()).getConstituents()) {
            Pair<Integer, Integer> key = new Pair<>(c.getStartCharOffset(), c.getEndCharOffset());
            assertTrue("No entity mention \"" + c.getSurfaceForm() + "\"", spanish_answers.containsKey(key));
            String gold_type = spanish_answers.get(key).getType();
            assertTrue("Entity " + c.getSurfaceForm() + " has type " + c.getLabel() + " instead of " + gold_type
                    , c.getLabel().equals(gold_type));
        }

        CrossLingualWikifier xlwikifier = null;
        try {
            xlwikifier = new CrossLingualWikifier(lang, config);
        } catch (IOException e) {
            e.printStackTrace();
        }

        xlwikifier.addView(ta);

        CoreferenceView corefview = (CoreferenceView) ta.getView(xlwikifier.getViewName());
        for (Constituent c : corefview.getConstituents()) {
            Pair<Integer, Integer> key = new Pair<>(c.getStartCharOffset(), c.getEndCharOffset());
            String gold_entitle = spanish_answers.get(key).getEnWikiTitle();
            assertTrue("Entity " + c.getSurfaceForm() + " has English title " + c.getLabel() + " instead of " + gold_entitle,
                    c.getLabel().equals(gold_entitle));
        }

    }

    @Test
    public void testChineseResults() {

        Language lang = Language.Chinese;
        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer(lang.getCode());
        TextAnnotation ta = tokenizer.getTextAnnotation(chinese_input);

        String config = "config/xlwikifier-demo.config";

        MultiLingualNER annotator = null;
        try {
            annotator = new MultiLingualNER(lang, config);
        } catch (IOException e) {
            e.printStackTrace();
        }

        annotator.addView(ta);

        for (Constituent c : ta.getView(annotator.getViewName()).getConstituents()) {
            Pair<Integer, Integer> key = new Pair<>(c.getStartCharOffset(), c.getEndCharOffset());
            assertTrue("No entity mention \"" + c.getSurfaceForm() + "\"", chinese_answers.containsKey(key));

            String gold_type = chinese_answers.get(key).getType();
            assertTrue("Entity " + c.getSurfaceForm() + " has type " + c.getLabel() + " instead of " + gold_type
                    , c.getLabel().equals(gold_type));
        }

        CrossLingualWikifier xlwikifier = null;
        try {
            xlwikifier = new CrossLingualWikifier(lang, config);
        } catch (IOException e) {
            e.printStackTrace();
        }

        xlwikifier.addView(ta);

        CoreferenceView corefview = (CoreferenceView) ta.getView(xlwikifier.getViewName());
        for (Constituent c : corefview.getConstituents()) {
            Pair<Integer, Integer> key = new Pair<>(c.getStartCharOffset(), c.getEndCharOffset());
            String gold_entitle = chinese_answers.get(key).getEnWikiTitle();
            assertTrue("Entity " + c.getSurfaceForm() + " has English title " + c.getLabel() + " instead of " + gold_entitle,
                    c.getLabel().equals(gold_entitle));
        }
    }

//    @Test
//    public void testEnglishResults() {
//
//        Language lang = Language.English;
//        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer(lang.getShortName());
//        TextAnnotation ta = tokenizer.getTextAnnotation(english_input);
//
//        String config = "config/xlwikifier-demo.config";
//
//        MultiLingualNER annotator = null;
//        try {
//            annotator = new MultiLingualNER(lang, config);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        annotator.addView(ta);
//
//        CrossLingualWikifier xlwikifier = null;
//        try {
//            xlwikifier = new CrossLingualWikifier(lang, config);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        xlwikifier.addView(ta);
//    }

}
