package edu.illinois.cs.cogcomp.xlwikifier.wikipedia;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import edu.illinois.cs.cogcomp.core.io.IOUtils;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
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

    private String redirect_cache = "/shared/bronte/tac2015/mediawiki_cache/redirects";
    private String title_cache = "/shared/bronte/tac2015/mediawiki_cache/title_category";
    private String search_cache = "/shared/bronte/tac2015/mediawiki_cache/search";
    private String search_cache1 = "/shared/bronte/tac2015/mediawiki_cache/search1";
    public MediaWikiSearch(){}

    public List<String> search1(String query, String lang, String mode){

        String cache_file = search_cache1+"/"+lang+"/"+mode+"/"+query;
        String json = null;
        if(IOUtils.exists(cache_file)){
            try {
                json = FileUtils.readFileToString(new File(cache_file), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {

            HttpTransport httpTransport = new NetHttpTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            GenericUrl url = new GenericUrl("https://"+lang+".wikipedia.org/w/api.php");
            url.put("action", "opensearch");
            url.put("search", query);
            url.put("profile", mode);
            url.put("redirect", "resolve");
            url.put("format", "json");
            url.put("utf8","");
            try {
                HttpRequest request = requestFactory.buildGetRequest(url);
                HttpResponse httpResponse = request.execute();
                json = httpResponse.parseAsString();
                FileUtils.writeStringToFile(new File(cache_file), json, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        System.out.println("JSON:");
        List<String> ret = new ArrayList<>();
        try {
            JSONParser parser = new JSONParser();
            JSONArray response = null;
            response = (JSONArray)parser.parse(json);
            JSONArray urls = (JSONArray) response.get(1);
            for(Object url: urls){
                ret.add(url.toString().replaceAll(" ", "_"));
            }
//            System.out.println(response.get(3));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }
    public List<String> search(String query, String lang){

        String cache_file = search_cache+"/"+lang+"/"+query;
        String json = null;
        if(IOUtils.exists(cache_file)){
            try {
                json = FileUtils.readFileToString(new File(cache_file), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {

            HttpTransport httpTransport = new NetHttpTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            GenericUrl url = new GenericUrl("https://"+lang+".wikipedia.org/w/api.php");
            url.put("action", "query");
            url.put("list", "search");
            url.put("srsearch", query);
            url.put("srlimit", 20);
            url.put("srprop", "size|score");
            url.put("format", "json");
            url.put("utf8","");
            try {
                HttpRequest request = requestFactory.buildGetRequest(url);
                HttpResponse httpResponse = request.execute();
                json = httpResponse.parseAsString();
                FileUtils.writeStringToFile(new File(cache_file), json, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        System.out.println("JSON:");
//        System.out.println(json);
        List<String> ret = new ArrayList<>();
        try {
            JSONParser parser = new JSONParser();
            JSONObject response = null;
            response = (JSONObject)parser.parse(json);
            JSONObject q = (JSONObject) response.get("query");
            JSONArray pages = (JSONArray) q.get("search");
            for(Object page: pages){
                ret.add(((JSONObject) page).get("title").toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public List<String> getCategories(String title, String lang){

        String title_ = title.replaceAll("/", "||");
        String cache_file;
        if(lang.equals("en"))
            cache_file = title_cache+"/"+title_;
        else
            cache_file = title_cache+"/"+lang+"/"+title_;

        List<String> ret = new ArrayList<>();
        if(cache_file.length()>200 || cache_file.endsWith("..")) return ret;
        String json = null;
        if(IOUtils.exists(cache_file)){
            try {
                json = FileUtils.readFileToString(new File(cache_file), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {

            HttpTransport httpTransport = new NetHttpTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            GenericUrl url = new GenericUrl("https://"+lang+".wikipedia.org/w/api.php");
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
            response = (JSONObject)parser.parse(json);
            JSONObject q = (JSONObject) response.get("query");
            JSONObject pages = (JSONObject) q.get("pages");
            if(pages == null) return ret;
            for(Object key: pages.keySet()){
                if(key.toString().equals(-1))
                    return ret;
                JSONObject page = (JSONObject) pages.get(key);
                if(!page.containsKey("categories"))
                    return ret;
                JSONArray cats = (JSONArray) page.get("categories");
                for(Object cat: cats){
                    ret.add(((JSONObject)cat).get("title").toString());
                }
            }
        } catch (Exception e) {
            System.out.println(title);
            e.printStackTrace();
        }
        return ret;
    }

    public String searchTitle(String title){

        String cache_file = title_cache+"/"+title;
        String json = null;
        if(IOUtils.exists(cache_file)){
            try {
                json = FileUtils.readFileToString(new File(cache_file), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {

            HttpTransport httpTransport = new NetHttpTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            GenericUrl url = new GenericUrl("https://en.wikipedia.org/w/api.php");
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
                e.printStackTrace();
            }
        }

//        System.out.println(json);
        try {
            JSONParser parser = new JSONParser();
            JSONObject response = null;
            response = (JSONObject)parser.parse(json);
            JSONObject q = (JSONObject) response.get("query");
            JSONObject pages = (JSONObject) q.get("pages");
            for(Object key: pages.keySet()){
                if(key.toString().equals(-1))
                    return null;
                JSONObject page = (JSONObject) pages.get(key);
                return page.get("title").toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getRedirectsTo(String title, String lang){

        String cache_file = redirect_cache+"/"+lang+"/"+title;
        String json = null;
        List<String> redirects = new ArrayList<>();
        if(IOUtils.exists(cache_file)){
            try {
                json = FileUtils.readFileToString(new File(cache_file), "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {

            HttpTransport httpTransport = new NetHttpTransport();
            HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
            GenericUrl url = new GenericUrl("https://"+lang+".wikipedia.org/w/api.php");
            url.put("action", "query");
            url.put("titles", title);
            url.put("prop", "redirects");
            url.put("format", "json");
            try {
                HttpRequest request = requestFactory.buildGetRequest(url);
                HttpResponse httpResponse = request.execute();
                json = httpResponse.parseAsString();
                FileUtils.writeStringToFile(new File(cache_file), json, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
                return redirects;
//                System.exit(-1);
            }
        }

//        System.out.println(json);
        try {
            JSONParser parser = new JSONParser();
            JSONObject response = null;
            response = (JSONObject)parser.parse(json);
            JSONObject q = (JSONObject) response.get("query");
            JSONObject pages = (JSONObject) q.get("pages");
            for(Object key: pages.keySet()){
                JSONObject page = (JSONObject) pages.get(key);
                if(!page.containsKey("redirects"))
                    break;
                JSONArray res = (JSONArray) page.get("redirects");
                for(Object redirect: res){
                    Object t = ((JSONObject)redirect).get("title");
                    redirects.add(t.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return redirects;
    }

    public String formatTitle(String title){
        String tmp = "";
        for(String token: title.split("_")){
            if(token.length()==0) continue;
            tmp+=token.substring(0,1).toUpperCase();
            if(token.length()>1)
                tmp+=token.substring(1,token.length());
            tmp+="_";
        }
        return tmp.substring(0, tmp.length()-1);
    }

    public static void main(String[] args) {
        MediaWikiSearch s = new MediaWikiSearch();
//        System.out.println(s.getRedirectsTo("United_States", "en"));
//        s.getRedirectsTo("Tsarna", "en");
        FreeBaseQuery.loadDB(true);
        List<String> titles = s.search1("Vladimir Putín", "es", "fuzzy");
//        if(titles.size() > 0){
//            System.out.println(titles.get(0));
//
//            System.out.println(FreeBaseQuery.getMidFromTitle(titles.get(0), "es"));
//
//        }



//        System.out.println(s.getCategories("Erkin_Alptekin", "en"));

//        System.out.println(s.getRedirectsTo("Dzhokhar Tsarnaev","en"));
    }
}
