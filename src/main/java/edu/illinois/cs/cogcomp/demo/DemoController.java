package edu.illinois.cs.cogcomp.demo;

import edu.illinois.cs.cogcomp.annotation.TextAnnotationBuilder;
import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

/**
 * Created by ctsai12 on 4/25/16.
 */
@RestController
public class DemoController {
    private static Logger logger = LoggerFactory.getLogger(DemoController.class);

    public static String config = "";

    @PostConstruct
    public void initAnnotators(){

        try {
            ConfigParameters.setPropValues(config);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        logger.info("Initializing demo");
        for(String lang: ConfigParameters.ranker_models.keySet()) {
            if(lang.equals("en") || lang.equals("es") || lang.equals("zh")){
//            if(new File(ConfigParameters.ranker_models.get(lang)).exists()) {

                Language language = Language.getLanguageByCode(lang);
                String sample = readExample(language);
				if(sample == null) continue;

                logger.info("Initializing " + lang + " NER and Wikifier");
                MultiLingualNER ner = MultiLingualNERManager.buildNerAnnotator(language, config);
                CrossLingualWikifier wikifier = CrossLingualWikifierManager.buildWikifierAnnotator(language, config);

                TextAnnotationBuilder tokenizer = MultiLingualTokenizer.getTokenizer(lang);

                TextAnnotation ta = tokenizer.createTextAnnotation(sample);
                ner.addView(ta);
                wikifier.addView(ta);
            }
        }
    }

    private String readExample(Language lang) {
        String dir = "/home/ctsai12/public_html/xlwikifier/examples/";
        String exp = null;
       try {
            exp = FileUtils.readFileToString(new File(dir, lang.toString()), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exp;
    }

    @CrossOrigin
    @RequestMapping(value = "/xlwikifier", method = RequestMethod.POST)
    public XLWikifierDemo annotate(@RequestParam(value = "text", defaultValue = "") String text,
                                   @RequestParam(value = "lang", defaultValue = "") String lang,
//                                   @RequestParam(value="transfer", defaultValue = "") boolean transfer,
                                   HttpServletRequest request) {

        final StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();

        logger.info("Request from: " + request.getRemoteAddr() + " " + request.getRemoteUser());
        logger.info("Text: " + text);
        logger.info("Lang: " + lang);
//        logger.info("Transfer: "+transfer);

        return new XLWikifierDemo(text, lang);
    }

    @CrossOrigin
    @RequestMapping(value = "/transtitle", method = RequestMethod.POST)
    public TitleTranslator translateTitle(@RequestParam(value = "query", defaultValue = "") String query,
                                          @RequestParam(value = "lang", defaultValue = "") String lang,
                                          HttpServletRequest request) {

        logger.info("Request from: " + request.getRemoteAddr() + " " + request.getRemoteUser());
        logger.info("Query: " + query);
        logger.info("Lang: " + lang);

        return new TitleTranslator(query, lang);
    }
}
