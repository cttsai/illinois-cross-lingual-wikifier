package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;

import java.io.IOException;

/**
 * Created by ctsai12 on 10/3/16.
 */
public class ConfigParameters {

    public static String db_path;
    public static String dump_path;
    public static String stopword_path;
    public static String model_path;

    public void getPropValues(){
        String propfile = "config/xlwikifier.config";
        ResourceManager rm = null;
        try {
            rm = new ResourceManager(propfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        db_path = rm.getString("db_path");
        dump_path = rm.getString("dump_path");
        stopword_path = rm.getString("stopword_path");
        model_path = rm.getString("model_path");

    }

    public static void main(String[] args) {

        ConfigParameters param = new ConfigParameters();
        param.getPropValues();
    }
}
