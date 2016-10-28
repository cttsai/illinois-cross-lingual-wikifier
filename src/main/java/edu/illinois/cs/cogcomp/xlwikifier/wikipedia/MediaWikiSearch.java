package edu.illinois.cs.cogcomp.xlwikifier.wikipedia;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.stanford.nlp.parser.nndep.Config;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ctsai12 on 9/18/15.
 */
public class MediaWikiSearch {

    private static String category_cache = ConfigParameters.search_cache+"/title_category";
    private static String search_cache = ConfigParameters.search_cache+"/search";

    public static List<String> search(String query, String lang, String mode) {

        String cache_file = search_cache + "/" + lang + "/" + mode + "/" + query;
        String json = null;
        if (IOUtils.exists(cache_file)) {
            try {
                json = FileUtils.readFileToString(new File(cache_file), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            HttpTransport httpTransport = new NetHttpTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            GenericUrl url = new GenericUrl("https://" + lang + ".wikipedia.org/w/api.php");
            url.put("action", "opensearch");
            url.put("search", query);
            url.put("profile", mode);
            url.put("redirect", "resolve");
            url.put("format", "json");
            url.put("utf8", "");
            try {
                HttpRequest request = requestFactory.buildGetRequest(url);
                HttpResponse httpResponse = request.execute();
                json = httpResponse.parseAsString();
                FileUtils.writeStringToFile(new File(cache_file), json, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        List<String> ret = new ArrayList<>();
        try {
            JSONParser parser = new JSONParser();
            JSONArray response = null;
            response = (JSONArray) parser.parse(json);
            JSONArray urls = (JSONArray) response.get(1);
            for (Object url : urls) {
                ret.add(url.toString().replaceAll(" ", "_"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }


    public static List<String> getCategories(String title, String lang) {

        title = formatTitle(title);
        String title_ = title.replaceAll("/", "||");
        String cache_file;
        if (lang.equals("en"))
            cache_file = category_cache + "/" + title_;
        else
            cache_file = category_cache + "/" + lang + "/" + title_;

        List<String> ret = new ArrayList<>();
        if (cache_file.length() > 200 || cache_file.endsWith("..")) return ret;
        String json = null;
        if (IOUtils.exists(cache_file)) {
            try {
                json = FileUtils.readFileToString(new File(cache_file), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {

            HttpTransport httpTransport = new NetHttpTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            GenericUrl url = new GenericUrl("https://" + lang + ".wikipedia.org/w/api.php");
            url.put("action", "query");
            url.put("titles", title);
            url.put("prop", "categories");
            url.put("format", "json");
            url.put("redirects", null);
            try {
                HttpRequest request = requestFactory.buildGetRequest(url);
                HttpResponse httpResponse = request.execute();
                json = httpResponse.parseAsString();
                FileUtils.writeStringToFile(new File(cache_file), json, "UTF-8");
            } catch (Exception e) {
                System.out.println(title);
                e.printStackTrace();
            }
        }

//        System.out.println(json);
        try {
            JSONParser parser = new JSONParser();
            JSONObject response = null;
            response = (JSONObject) parser.parse(json);
            JSONObject q = (JSONObject) response.get("query");
            JSONObject pages = (JSONObject) q.get("pages");
            if (pages == null) return ret;
            for (Object key : pages.keySet()) {
                if (key.toString().equals(-1))
                    return ret;
                JSONObject page = (JSONObject) pages.get(key);
                if (!page.containsKey("categories"))
                    return ret;
                JSONArray cats = (JSONArray) page.get("categories");
                for (Object cat : cats) {
                    ret.add(((JSONObject) cat).get("title").toString());
                }
            }
        } catch (Exception e) {
            System.out.println(title);
            e.printStackTrace();
        }
        return ret;
    }

    public static String formatTitle(String title) {
        String tmp = "";
        for (String token : title.split("_")) {
            if (token.length() == 0) continue;
            tmp += token.substring(0, 1).toUpperCase();
            if (token.length() > 1)
                tmp += token.substring(1, token.length());
            tmp += "_";
        }
        return tmp.substring(0, tmp.length() - 1);
    }

    public static void main(String[] args) {

    }
}
