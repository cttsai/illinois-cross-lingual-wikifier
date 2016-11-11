package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

/**
 * Created by ctsai12 on 10/25/16.
 */
public enum Language {
    English("EN"),
    Spanish("ES"),
    Chinese("ZH"),
    German("DE"),
    Dutch("NL"),
    Turkish("TR"),
    Tagalog("TL"),
    Yoruba("YO"),
    Bengali("BN"),
    Tamil("TA");


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
        else if(lang.equals("nl"))
            return Language.Dutch;
        else if(lang.equals("de"))
            return Language.German;
        else if(lang.equals("tr"))
            return Language.Turkish;
        else if(lang.equals("tl"))
            return Language.Tagalog;
        else if(lang.equals("yo"))
            return Language.Yoruba;
        else if(lang.equals("bn"))
            return Language.Bengali;
        else if(lang.equals("ta"))
            return Language.Tamil;
        else{
            System.out.println("Unknown language: "+lang);
            System.exit(-1);
        }



        return null;
    }
}
