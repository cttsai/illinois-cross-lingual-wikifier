package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ctsai12 on 10/26/16.
 */
public class MultiLingualNERManager {
    private static Logger logger = LoggerFactory.getLogger(MultiLingualNERManager.class);

    private static Map<String, MultiLingualNER> annotatorMap;


    public static MultiLingualNER buildNerAnnotator(Language lang, String configFile) {

        if(annotatorMap == null)
            annotatorMap = new HashMap<>();

        if(!annotatorMap.containsKey(lang.getNERViewName())){
            MultiLingualNER mlner = null;
            try {
                mlner = new MultiLingualNER(lang, configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            annotatorMap.put(lang.getNERViewName(), mlner);
        }

        return annotatorMap.get(lang.getNERViewName());
    }
}
