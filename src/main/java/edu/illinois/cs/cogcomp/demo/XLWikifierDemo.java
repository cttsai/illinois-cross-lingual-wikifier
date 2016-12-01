package edu.illinois.cs.cogcomp.demo;

import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
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
        Language lang = Language.getLanguageByCode(language);

        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer(language);
        TextAnnotation ta = tokenizer.getTextAnnotation(text);

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
        String out = "";

        int pend = 0;
        for (ELMention m : doc.mentions) {
            int start = m.getStartOffset();
            out += doc.text.substring(pend, start);
            String ref = "#";
            String en_title = formatTitle(m.getEnWikiTitle());
            if (!m.getEnWikiTitle().startsWith("NIL"))
                ref = "http://en.wikipedia.org/wiki/" + en_title;
            //else if(!m.getWikiTitle().startsWith("NIL"))
            //    ref = "http://"+lang+".wikipedia.org/wiki/"+m.getWikiTitle();
            //String tip = "English Wiki: "+m.en_wiki_title+" <br> "+lang+": "+m.getWikiTitle()+" <br> "+m.getType();
            String tip = "English Wiki: " + en_title + " <br> Entity Type: " + m.getType();
            out += "<a class=\"top\" target=\"_blank\" title=\"\" data-html=true data-placement=\"top\" data-toggle=\"tooltip\" href=\"" + ref + "\" data-original-title=\"" + tip + "\">" + m.getSurface() + "</a>";
            pend = m.getEndOffset();
        }
        out += doc.text.substring(pend, doc.text.length());
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

}
