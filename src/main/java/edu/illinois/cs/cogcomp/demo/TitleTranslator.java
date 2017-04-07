package edu.illinois.cs.cogcomp.demo;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created by ctsai12 on 4/25/16.
 */
public class TitleTranslator {

    private String output;
    private String output1 = "";
    private static Logger logger = LoggerFactory.getLogger(TitleTranslator.class);

    private List<String> supported_langs = Arrays.asList("zh", "ja", "ko", "es", "de", "fr", "it", "ta", "tr", "ru", "ar", "he", "pt");

    public TitleTranslator(String query, String language) {

        String lang_code = Language.valueOf(language).getCode();

        if(lang_code.equals("zh"))
            query = ChineseHelper.convertToTraditionalChinese(query);

        logger.info("query lang code: "+lang_code);
        String en_query = query;
        if(!lang_code.equals("en"))
            en_query = LangLinker.getLangLinker(lang_code).translateToEn(query, lang_code);

        if(en_query == null) {
            output = "no English title...";
            return;
        }

        output = "#"+en_query.replaceAll("\\s+", "");

        for(String lang: supported_langs){
            String tmp = LangLinker.getLangLinker(lang).translateFromEn(en_query, lang);
            if(tmp != null) {
                output1 += Language.getLanguageByCode(lang).toString()+": "+tmp+"<br />";
                tmp = tmp.replaceAll("_", "");
                output += " #" + tmp;
            }
        }
    }


    public String getOutput() {
        return this.output;
    }

    public String getOutput1(){
        return this.output1;
    }

    public static void main(String[] args) {
        try {
            ConfigParameters.setPropValues("config/xlwikifier-demo.config");
        } catch (IOException e) {
            e.printStackTrace();
        }
        TitleTranslator t = new TitleTranslator("Barack_Obama", "English");
        System.out.println(t.getOutput());
    }
}
