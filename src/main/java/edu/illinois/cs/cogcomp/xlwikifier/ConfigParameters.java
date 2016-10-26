package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ctsai12 on 10/3/16.
 */
public class ConfigParameters {

    public static String db_path;
    public static String dump_path;
    public static String stopword_path;
    public static String model_path;
    public static Map<String, String> ner_models = new HashMap<>();

    public static void setPropValues(){
        setPropValues("config/xlwikifier.config");
    }

    public static void setPropValues(String file){
        ResourceManager rm = null;
        try {
            rm = new ResourceManager(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        db_path = rm.getString("db_path").trim();
        dump_path = rm.getString("dump_path").trim();
        stopword_path = rm.getString("stopword_path").trim();
        model_path = rm.getString("model_path").trim();

        // load NER model configs
        for(Language lang: Language.values()){
            String l = lang.toString().toLowerCase();
            String key = l+"_ner_config";
            if(rm.containsKey(key))
                ner_models.put(l, rm.getString(key).trim());
        }
    }

    public static void main(String[] args) {

        ConfigParameters param = new ConfigParameters();
        param.setPropValues();
    }
}
