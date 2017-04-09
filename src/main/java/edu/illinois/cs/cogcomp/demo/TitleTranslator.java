package edu.illinois.cs.cogcomp.demo;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinkerNew;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by ctsai12 on 4/25/16.
 */
public class TitleTranslator {

    private String output;
    private String output1 = "";
    private String output2 = "";
    private String output3 = "";
    private static Logger logger = LoggerFactory.getLogger(TitleTranslator.class);

    private List<String> supported_langs = Arrays.asList("en","es","zh","ja","ko","de","pt","nl","tr","tl","yo","bn","ta","sv","fr","ru","it","pl","vi","uk","ca","fa","no","ar","sh","fi","hu","id","ro","cs","sr","ms","eu","eo","bg","da","kk","sk","hy","he","lt","hr","ce","sl","et","gl","uz","la","el","be","vo","hi","th","az","ka","ur","mk","oc","mg","cy","lv","bs","tt","tg","te");

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
        Set<String> seen = new HashSet<>();

        output = "#"+en_query.replaceAll("\\s+", "").replaceAll("-", "").replaceAll("_", "");
        seen.add(output.substring(1));

        LangLinkerNew ll = LangLinkerNew.getLangLinker(lang_code);

        Map<String, String> lang2titles = ll.translate(en_query);

        if(lang2titles.size() == 0){
            String red = ll.getRedirect(en_query);
            if(red != null) {
                lang2titles = ll.translate(red);
                int idx = red.indexOf("(");
                if(idx > 0)
                    red = red.substring(0, idx);

                red = red.replaceAll("\\s+", "").replaceAll("-", "").replaceAll("_", "");
                if(!output.substring(1).equals(red)) {
                    output += " #" + red;
                    seen.add(red);
                }
            }
        }

        output += " #hedgetag";
        int j = 0;
        for(String lang: supported_langs){
            if(!lang2titles.containsKey(lang)) continue;
            Language l = Language.getLanguageByCode(lang);
            String title = lang2titles.get(lang);
            int idx = title.indexOf("(");
            if(idx > 0)
                title = title.substring(0, idx);
            title = title.replaceAll("_", "").trim();
            if(j==0)
                output1 += "<span class=\"text-info\">"+l.toString()+":</span> "+title+"<br />";
            else if(j==1)
                output2 += "<span class=\"text-info\">"+l.toString()+":</span> "+title+"<br />";
            else if(j==2)
                output3 += "<span class=\"text-info\">"+l.toString()+":</span> "+title+"<br />";

            j = (j+1)%3;

            title = title.replaceAll("-", "").trim();
            if(!seen.contains(title)) {
                output += " #" + title;
                seen.add(title);
            }

            if(l.toString().equals("Chinese")) {
                String trad = ChineseHelper.convertToTraditionalChinese(title);
                if(!seen.contains(trad)) {
                    output += " #" + trad;
                    seen.add(trad);
                }
            }
        }

    }


    public String getOutput() {
        return this.output;
    }

    public String getOutput1(){
        return this.output1;
    }

    public String getOutput2(){
        return this.output2;
    }

    public String getOutput3(){
        return this.output3;
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
