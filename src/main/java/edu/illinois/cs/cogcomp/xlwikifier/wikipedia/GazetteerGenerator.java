package edu.illinois.cs.cogcomp.xlwikifier.wikipedia;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 * This class uses title mapping between English and the target language to
 * generate gazetters for the target language.
 *
 * Created by ctsai12 on 3/9/16.
 */
public class GazetteerGenerator {

    private Set<String> per_types = new HashSet<>();
    private Set<String> org_types = new HashSet<>();
    private Set<String> loc_types = new HashSet<>();
    private Set<String> song_types = new HashSet<>();
    private Set<String> film_types = new HashSet<>();

    private String dir = "/shared/corpora/ner/gazetteers/";
    public GazetteerGenerator(){

        per_types.add("/people/person");
        org_types.add("/organization/organization");
        loc_types.add("/location/location");
        song_types.add("/music/album");
        film_types.add("/film/film");
    }

    public static Set<String> loadFile(String file){

        try {
            return LineIO.read(file).stream().map(x -> x.toLowerCase()).collect(toSet());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void run(String lang){

        Set<String> en_per = null, en_loc = null, en_org= null, en_song= null, en_film= null;
        en_per = loadFile("/shared/corpora/ner/gazetteers/en/per");
        en_loc = loadFile("/shared/corpora/ner/gazetteers/en/loc");
        en_org = loadFile("/shared/corpora/ner/gazetteers/en/org");
        en_song = loadFile("/shared/corpora/ner/gazetteers/en/song");
        en_film = loadFile("/shared/corpora/ner/gazetteers/en/film");
        LangLinker ll = LangLinker.getLangLinker(lang);

        Set<String> per_titles = new HashSet<>();
        Set<String> org_titles = new HashSet<>();
        Set<String> loc_titles = new HashSet<>();
        Set<String> song_titles = new HashSet<>();
        Set<String> film_titles = new HashSet<>();

        System.out.println(ll.to_en.size());
        int cnt = 0;
        for(String ft: ll.getForeignTitles()){
            if(cnt++%1000 == 0){
                System.out.println(cnt);
                System.out.println(per_titles.size());
                System.out.println(loc_titles.size());
                System.out.println(org_titles.size());
                System.out.println(song_titles.size());
            }
            String title = ll.translateToEn(ft, lang).toLowerCase();
            String tt = title.replaceAll("_", " ");
            int idx = tt.indexOf("(");
            if(idx > 0) tt = tt.substring(0, idx);
            tt = tt.trim();
            ft = ft.replaceAll("_", " ");
            idx = ft.indexOf("(");
            if(idx > 0) ft = ft.substring(0, idx);
            ft = formatTitle(ft.trim());

            if(en_per.contains(tt))
                per_titles.add(ft);
            if(en_loc.contains(tt))
                loc_titles.add(ft);
            if(en_org.contains(tt))
                org_titles.add(ft);
            if(en_song.contains(tt))
                song_titles.add(ft);
            if(en_film.contains(tt))
                film_titles.add(ft);
        }

        String path = dir +lang;
        try {
            FileUtils.writeStringToFile(new File(path+"/per"), per_titles.stream().collect(joining("\n")), "utf-8");
            FileUtils.writeStringToFile(new File(path+"/loc"), loc_titles.stream().collect(joining("\n")), "utf-8");
            FileUtils.writeStringToFile(new File(path+"/org"), org_titles.stream().collect(joining("\n")), "utf-8");
            FileUtils.writeStringToFile(new File(path+"/song"), song_titles.stream().collect(joining("\n")), "utf-8");
            FileUtils.writeStringToFile(new File(path+"/film"), film_titles.stream().collect(joining("\n")), "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public String formatTitle(String title){
        String tmp = "";
        for(String token: title.split("\\s+")){
            if(token.length()==0) continue;
            tmp+=token.substring(0,1).toUpperCase();
            if(token.length()>1)
                tmp+=token.substring(1,token.length());
            tmp+=" ";
        }
        tmp = tmp.trim();
        String tmp1 = "";
        for(String token: tmp.split("-")){
            if(token.length()==0) continue;
            tmp1+=token.substring(0,1).toUpperCase();
            if(token.length()>1)
                tmp1+=token.substring(1,token.length());
            tmp1+="-";
        }

        return tmp1.substring(0, tmp1.length()-1);
    }

    public static void main(String[] args) {
        try {
            ConfigParameters.setPropValues("config/xlwikifier-demo.config");
        } catch (IOException e) {
            e.printStackTrace();
        }
        GazetteerGenerator gg = new GazetteerGenerator();
        gg.run("so");
    }
}
