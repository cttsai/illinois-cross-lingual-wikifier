package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.core.constants.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ctsai12 on 10/26/16.
 */
public class CrossLingualWikifierManager {
    private static Logger logger = LoggerFactory.getLogger(CrossLingualWikifierManager.class);

    private static Map<String, CrossLingualWikifier> annotatorMap;


    public static CrossLingualWikifier buildWikifierAnnotator(Language lang, String configFile) {

        if(annotatorMap == null)
            annotatorMap = new HashMap<>();

        if(!annotatorMap.containsKey(lang.name())){
            CrossLingualWikifier xlwikifier = null;
            try {
                xlwikifier = new CrossLingualWikifier(lang, configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            annotatorMap.put(lang.name(), xlwikifier);
        }

        return annotatorMap.get(lang.name());
    }
}
