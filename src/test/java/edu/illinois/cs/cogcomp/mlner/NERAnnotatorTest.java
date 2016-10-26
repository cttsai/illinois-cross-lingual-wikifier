package edu.illinois.cs.cogcomp.mlner;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.Constituent;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.mlner.MultiLingualNERAnnotator;
import edu.illinois.cs.cogcomp.tokenizers.CharacterTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.Language;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * Created by ctsai12 on 10/25/16.
 */
public class NERAnnotatorTest {

    final static String spanish_input = "Barack Hussein Obama II3 es el cuadragésimo cuarto y actual presidente de los Estados Unidos de América. Fue senador por el estado de Illinois desde el 3 de enero de 2005 hasta su renuncia el 16 de noviembre de 2008. Además, es el quinto legislador afroamericano en el Senado de los Estados Unidos, tercero desde la era de reconstrucción. También fue el primer candidato afroamericano nominado a la presidencia por el Partido Demócrata y es el primero en ejercer el cargo presidencial.";
    final static String chinese_input = "巴拉克·歐巴馬是美國民主黨籍政治家，也是第44任美國總統，於2008年初次當選，並於2012年成功連任。歐巴馬是第一位非裔美國總統。他1961年出生於美國夏威夷州檀香山，童年和青少年時期分別在印尼和夏威夷度過。1991年，歐巴馬以優等生榮譽從哈佛法學院畢業。1996年開始參選公職，在補選中，當選伊利諾州參議員。";

    final private static Map<String, String> spanish_answers = new HashMap<>();
    static{
        spanish_answers.put("Barack Hussein Obama", "PER");
        spanish_answers.put("Estados Unidos de América", "GPE");
        spanish_answers.put("Illinois", "GPE");
        spanish_answers.put("afroamericano", "LOC");
        spanish_answers.put("Senado", "ORG");
        spanish_answers.put("Estados Unidos", "GPE");
        spanish_answers.put("Partido Demócrata", "ORG");
    }

    final private static Map<String, String> chinese_answers = new HashMap<>();
    static{
        chinese_answers.put("巴拉克·歐巴馬", "PER");
        chinese_answers.put("美國民主黨", "ORG");
        chinese_answers.put("美國", "GPE");
        chinese_answers.put("歐巴馬", "PER");
        chinese_answers.put("非", "LOC");
        chinese_answers.put("美國夏威夷州檀香山", "GPE");
        chinese_answers.put("印尼", "GPE");
        chinese_answers.put("夏威夷", "GPE");
        chinese_answers.put("哈佛法學院", "ORG");
        chinese_answers.put("伊利諾州參議員", "ORG");
    }

    @Test
    public void testSpanishResults(){

        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer("es");
        TextAnnotation ta = tokenizer.getTextAnnotation(spanish_input);

        String config = "config/xlwikifier.config";

        MultiLingualNERAnnotator annotator = null;
        try {
            annotator = new MultiLingualNERAnnotator("ES-NER", Language.ES, config);
        } catch (IOException e) {
            e.printStackTrace();
        }

        annotator.addView(ta);

        for(Constituent c: ta.getView("ES-NER").getConstituents()){
            assertTrue("No entity mention \""+c.getSurfaceForm()+"\"", spanish_answers.containsKey(c.getSurfaceForm()));
            assertTrue("Entity "+c.getSurfaceForm()+" has type "+c.getLabel()+" instead of "+spanish_answers.get(c.getSurfaceForm())
                    , c.getLabel().equals(spanish_answers.get(c.getSurfaceForm())));
        }
    }

    @Test
    public void testChineseResults(){

        Tokenizer tokenizer = new CharacterTokenizer();
        TextAnnotation ta = tokenizer.getTextAnnotation(chinese_input);

        String config = "config/xlwikifier.config";

        MultiLingualNERAnnotator annotator = null;
        try {
            annotator = new MultiLingualNERAnnotator("ZH-NER", Language.ZH, config);
        } catch (IOException e) {
            e.printStackTrace();
        }

        annotator.addView(ta);

        for(Constituent c: ta.getView("ZH-NER").getConstituents()){
            assertTrue("No entity mention \""+c.getSurfaceForm()+"\"", chinese_answers.containsKey(c.getSurfaceForm()));
            assertTrue("Entity "+c.getSurfaceForm()+" has type "+c.getLabel()+" instead of "+chinese_answers.get(c.getSurfaceForm())
                    , c.getLabel().equals(chinese_answers.get(c.getSurfaceForm())));
        }
    }

}
