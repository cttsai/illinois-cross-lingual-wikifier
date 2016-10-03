package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.xlwikifier.datastructures.*;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Created by ctsai12 on 11/20/15.
 */
public class Utils {

    public static String getTime(){
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MM.dd.hh.mm.ss");
        return sdf.format(cal.getTime());
    }

    public static void cacheMentions(List<ELMention> mentions, String out){
        Gson gson = new Gson();
        Type t = new TypeToken<List<ELMention>>(){}.getType();
        String json = gson.toJson(mentions, t);
        try {
            FileUtils.writeStringToFile(new File("/shared/bronte/ctsai12/multilingual/2015data/"+out), json, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<ELMention> readMentionCache(String filename) {

        Gson gson = new Gson();
        Type t = new TypeToken<List<ELMention>>() {
        }.getType();
        String json = null;
        try {
            json = FileUtils.readFileToString(new File(filename), "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        List<ELMention> mentions = gson.fromJson(json, t);
        return mentions;
    }


    public static void main(String[] args) {

    }
}
