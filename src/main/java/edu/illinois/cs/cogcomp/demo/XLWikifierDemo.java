package edu.illinois.cs.cogcomp.demo;

import edu.illinois.cs.cogcomp.mlner.CrossLingualNER;
import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifier;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ctsai12 on 4/25/16.
 */
public class XLWikifierDemo {

    private String output;
    private String runtime;
    private static Logger logger = LoggerFactory.getLogger(XLWikifierDemo.class);

    public XLWikifierDemo(String text, String lang, boolean transfer){
        FreeBaseQuery.loadDB(true);
        long startTime = System.currentTimeMillis();
        CrossLingualNER.setLang(lang, transfer);
        QueryDocument doc = CrossLingualNER.annotate(text);
        double totaltime = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.info("Time "+totaltime+" secs");
        runtime = "";
        runtime += "NER took: "+totaltime+" secs. ";
        startTime = System.currentTimeMillis();
        CrossLingualWikifier.setLang(lang);
        CrossLingualWikifier.wikify(doc);
        totaltime = (System.currentTimeMillis() - startTime) / 1000.0;
        logger.info("Time "+totaltime+" secs");
        runtime += "Wikification took: "+totaltime+" secs.";
        output = formatOutput(doc, lang);
    }

    /**
     * The output to be shown on the web demo
     * @param doc
     * @param lang
     * @return
     */
    private String formatOutput(QueryDocument doc, String lang){
        logger.info("Formatting demo outputs...");
        String out = "";

        int pend = 0;
        for(ELMention m: doc.mentions){
            int start = m.getStartOffset();
            out += doc.plain_text.substring(pend, start);
            String ref = "#";
			String en_title = formatTitle(m.en_wiki_title);
            if(!m.en_wiki_title.startsWith("NIL"))
                ref = "http://en.wikipedia.org/wiki/"+en_title;
            //else if(!m.getWikiTitle().startsWith("NIL"))
            //    ref = "http://"+lang+".wikipedia.org/wiki/"+m.getWikiTitle();
            //String tip = "English Wiki: "+m.en_wiki_title+" <br> "+lang+": "+m.getWikiTitle()+" <br> "+m.getType();
            String tip = "English Wiki: "+en_title+" <br> Entity Type: "+m.getType();
            out += "<a class=\"top\" title=\"\" data-html=true data-placement=\"top\" data-toggle=\"tooltip\" href=\""+ref+"\" data-original-title=\""+tip+"\">"+m.getMention()+"</a>";
            pend = m.getEndOffset();
        }
        out += doc.plain_text.substring(pend, doc.plain_text.length());
        return out;
    }

    public String formatTitle(String title){
        String tmp = "";
        for(String token: title.split("_")){
            if(token.length()==0) continue;
			if(token.startsWith("(")){
				tmp+=token.substring(0,2).toUpperCase();
				if(token.length()>2)
					tmp+=token.substring(2,token.length());
			}
			else{
				tmp+=token.substring(0,1).toUpperCase();
				if(token.length()>1)
					tmp+=token.substring(1,token.length());
			}
			tmp+="_";
        }
        return tmp.substring(0, tmp.length()-1);
    }

    public String getOutput(){
        return this.output;
    }

    public String getRuntime(){
        return this.runtime;
    }

    public static void main(String[] args) {
        String text = "Louis van Gaal , Endonezya maçı sonrasında oldukça ses getirecek açıklamalarda bulundu .";
        text = "Paul Kantor teaches information science at Rutgers University";
        XLWikifierDemo result = new XLWikifierDemo(text, "en", true);
        System.out.println(result.getOutput());
    }

}
