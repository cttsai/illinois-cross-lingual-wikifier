package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

/**
 * Created by ctsai12 on 10/25/16.
 */
public enum Language {
    EN("EN"),
    ES("ES"),
    ZH("ZH");

    private String NER_VIEW;
    private String WIKI_VIEW;

    Language(String lang) {
        NER_VIEW = lang + "_NER";
        WIKI_VIEW = lang + "_WIKIFIER";
    }

    public String getNERViewName() {
        return NER_VIEW;
    }

    public String getWikifierViewName() {
        return WIKI_VIEW;
    }

    public static Language getLanguage(String lang){

        if(lang.equals("en"))
            return Language.EN;
        else if(lang.equals("es"))
            return Language.ES;
        else if(lang.equals("zh"))
            return Language.ZH;

        return null;
    }
}
