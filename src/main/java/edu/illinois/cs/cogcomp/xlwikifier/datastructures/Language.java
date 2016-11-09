package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

/**
 * Created by ctsai12 on 10/25/16.
 */
public enum Language {
    English("EN"),
    Spanish("ES"),
    Chinese("ZH");

    private String NER_VIEW;
    private String WIKI_VIEW;
    private String short_name;

    Language(String lang) {
        NER_VIEW = lang + "_NER";
        WIKI_VIEW = lang + "_WIKIFIER";
        short_name = lang.toLowerCase();
    }

    public String getShortName(){
        return short_name;
    }

    public String getNERViewName() {
        return NER_VIEW;
    }

    public String getWikifierViewName() {
        return WIKI_VIEW;
    }

    public static Language getLanguage(String lang){

        if(lang.equals("en"))
            return Language.English;
        else if(lang.equals("es"))
            return Language.Spanish;
        else if(lang.equals("zh"))
            return Language.Chinese;

        return null;
    }
}
