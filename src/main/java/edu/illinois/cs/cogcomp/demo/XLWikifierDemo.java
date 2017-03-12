package edu.illinois.cs.cogcomp.demo;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifier;
import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifierManager;
import edu.illinois.cs.cogcomp.xlwikifier.MultiLingualNER;
import edu.illinois.cs.cogcomp.xlwikifier.MultiLingualNERManager;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ctsai12 on 4/25/16.
 */
public class XLWikifierDemo {

    private String output;

    private String default_config = "config/xlwikifier-demo.config";
    private static Logger logger = LoggerFactory.getLogger(XLWikifierDemo.class);

    public XLWikifierDemo(String text, String language) {

        logger.info("Language: "+language);
//        Language lang = Language.getLanguageByCode(language);
        Language lang = Language.valueOf(language);

        TextAnnotationBuilder tokenizer = MultiLingualTokenizer.getTokenizer(lang.getCode());
        if(language.equals("Chinese"))
            text = ChineseHelper.convertToSimplifiedChinese(text);
        TextAnnotation ta = tokenizer.createTextAnnotation(text);

        long startTime = System.currentTimeMillis();
        MultiLingualNER mlner = MultiLingualNERManager.buildNerAnnotator(lang, default_config);
        mlner.addView(ta);
        double totaltime = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.info("Time " + totaltime + " secs");

        startTime = System.currentTimeMillis();
        CrossLingualWikifier xlwikifier = CrossLingualWikifierManager.buildWikifierAnnotator(lang, default_config);
        xlwikifier.addView(ta);
        totaltime = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.info("Time " + totaltime + " secs");

        output = formatOutput(xlwikifier.result, language);
        logger.info("Done");
    }

    /**
     * The output to be shown on the web demo
     *
     * @param doc
     * @param lang
     * @return
     */
    private String formatOutput(QueryDocument doc, String lang) {
        String out = "<span> ";

        int pend = 0;
        for (ELMention m : doc.mentions) {
            int start = m.getStartOffset();
            out += doc.text.substring(pend, start);
            String ref = "#";
            String en_title = formatTitle(m.getEnWikiTitle());
            String tip = "English Wiki: " + en_title + " <br> Entity Type: " + m.getType();
            if (!m.getEnWikiTitle().startsWith("NIL")) {
                ref = "http://en.wikipedia.org/wiki/" + m.getEnWikiTitle();
                out += "<a class=\"top\" target=\"_blank\" title=\"\" data-html=true data-placement=\"top\" data-toggle=\"tooltip\" href=\"" + ref + "\" data-original-title=\"" + tip + "\">" + m.getSurface() + "</a>";
            }
            else{
                out += "<a class=\"top\" target=\"_blank\" title=\"\" data-html=true data-placement=\"top\" data-toggle=\"tooltip\" href=\"" + ref + "\" data-original-title=\"" + tip + "\" onclick=\"return false;\">" + m.getSurface() + "</a>";
            }
            pend = m.getEndOffset();
        }
        out += doc.text.substring(pend, doc.text.length());
        out += " </span>";
        return out;
    }

    public String formatTitle(String title) {
        String tmp = "";
        for (String token : title.split("_")) {
            if (token.length() == 0) continue;
            if (token.startsWith("(")) {
                tmp += token.substring(0, 2).toUpperCase();
                if (token.length() > 2)
                    tmp += token.substring(2, token.length());
            } else {
                tmp += token.substring(0, 1).toUpperCase();
                if (token.length() > 1)
                    tmp += token.substring(1, token.length());
            }
            tmp += "_";
        }
        return tmp.substring(0, tmp.length() - 1);
    }

    public String getOutput() {
        return this.output;
    }

    public static void main(String[] args) {
        String text = "Barack Obama Zum Anhören bitte klicken! ist ein US-amerikanischer Politiker und seit dem 20. Januar 2009 der 44. Präsident der Vereinigten Staaten. Obama ist ein auf US-Verfassungsrecht spezialisierter Rechtsanwalt. Im Jahr 1992 schloss er sich der Demokratischen Partei an, für die er 1997 Mitglied im Senat von Illinois wurde. Im Anschluss gehörte er von 2005 bis 2008 als Junior Senator für diesen US-Bundesstaat dem Senat der Vereinigten Staaten an. Bei der Präsidentschaftswahl des Jahres 2008 errang er die Kandidatur seiner Partei und setzte sich dann gegen den Republikaner John McCain durch." ;
        text = "ברק חוסיין אובמה השני (באנגלית: Barack Hussein Obama II (מידע · עזרה); נולד ב-4 באוגוסט 1961) הוא נשיאה הארבעים וארבעה של ארצות הברית (מאז שנת 2009), וצפוי לסיים את כהונתו, בתום שתי קדנציות, ב-20 בינואר 2017. אובמה הוא האפרו-אמריקאי הראשון המכהן בתפקיד זה. לפני בחירתו לנשיא כיהן בסנאט של ארצות הברית מטעם מדינת אילינוי, מינואר 2005";
        XLWikifierDemo result = new XLWikifierDemo(text, "Hebrew");
        System.out.println(result.output);
    }
}
