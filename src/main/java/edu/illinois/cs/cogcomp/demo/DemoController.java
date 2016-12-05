package edu.illinois.cs.cogcomp.demo;

import edu.illinois.cs.cogcomp.core.constants.Language;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifier;
import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifierManager;
import edu.illinois.cs.cogcomp.xlwikifier.MultiLingualNER;
import edu.illinois.cs.cogcomp.xlwikifier.MultiLingualNERManager;
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

    private String default_config = "config/xlwikifier-demo.config";

    @PostConstruct
    public void initAnnotators(){

        logger.info("Initializing demo");
        for(Language lang: Language.values()) {
            logger.info("Initializing " + lang.toString() + " NER and wikification");
            MultiLingualNER ner = MultiLingualNERManager.buildNerAnnotator(lang, default_config);
            CrossLingualWikifier wikifier = CrossLingualWikifierManager.buildWikifierAnnotator(lang, default_config);

            Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer(lang.getCode());
            String sample = readExample(lang);

            TextAnnotation ta = tokenizer.getTextAnnotation(sample);
            ner.addView(ta);
            wikifier.addView(ta);
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
}
