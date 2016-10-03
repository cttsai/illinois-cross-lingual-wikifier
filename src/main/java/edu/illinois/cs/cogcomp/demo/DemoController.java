package edu.illinois.cs.cogcomp.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by ctsai12 on 4/25/16.
 */
@RestController
public class DemoController {
    private static Logger logger = LoggerFactory.getLogger(DemoController.class);

    @CrossOrigin
    @RequestMapping(value = "/xlwikifier", method = RequestMethod.POST)
    public XLWikifierDemo annotate(@RequestParam(value="text", defaultValue="") String text,
                                   @RequestParam(value="lang", defaultValue = "") String lang,
//                                   @RequestParam(value="transfer", defaultValue = "") boolean transfer,
                                   HttpServletRequest request) {

        final StaticLoggerBinder binder = StaticLoggerBinder.getSingleton();
        System.out.println(binder.getLoggerFactory());
        System.out.println(binder.getLoggerFactoryClassStr());


        logger.info("Request from: "+request.getRemoteAddr()+" "+request.getRemoteUser());
        logger.info("Text: "+text);
        logger.info("Lang: "+lang);
//        logger.info("Transfer: "+transfer);

        return new XLWikifierDemo(text, lang, false);
    }
}
