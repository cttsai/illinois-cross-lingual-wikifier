package edu.illinois.cs.cogcomp.xlwikifier.experiments;

import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.WikiDocReader;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.WikiDataReader;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.LangLinker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Created by ctsai12 on 1/17/16.
 * This class re-produces the reuslts on Wikipedia dataset in our NAACL paper (Table 4)
 */
public class WikiDataWithTransExp {

    public boolean mono = false;
    public boolean word_align = false;
    private Set<String> train_pairs;
    private WikiDataReader wiki_reader;
    private Evaluator eval;
    private static Logger logger = LoggerFactory.getLogger(WikiDataWithTransExp.class);
//    TransliterationExp te = new TransliterationExp();

    public WikiDataWithTransExp(){
        wiki_reader = new WikiDataReader();
        eval = new Evaluator();
    }


    /**
     * Run the experiment of the input language
     */
    public void run(String lang){
        logger.info("Running "+lang+" wikidata experiments...");
        Ranker ranker = trainRanker(lang);
//        ranker.saveLexicalManager("models/ranker/default/"+lang+"/lm");
//        List<QueryDocument> docs = wiki_reader.readTestData(lang);
//        CrossLingualWikifier wikifier = new CrossLingualWikifier(lang, new WikiCandidateGenerator());
//        wikifier.setRanker(ranker);
//        wikifier.wikify(docs);
//        eval.evaluateWikiTitle(docs);
//        ranker.closeDBs();
    }

    public Ranker trainRanker(String lang){
        Ranker ranker = new Ranker(lang);
//        String model = NERUtils.getTime()+"."+lang+"."+mono;
        String model = lang;
        List<QueryDocument> docs = wiki_reader.readTrainData(lang).subList(1, 2000);
        filterNonNE(docs);
        WikiCandidateGenerator wcg = new WikiCandidateGenerator();
//        wcg.genCandidates(docs, "en");
        setCands(docs, lang, wcg);
        evalCands(docs, "en");


//        double rate = 0.3;
//        if(lang.equals("es")) rate = 0.4;
//        wcg.selectMentions(docs, rate);
//        ranker.trainRankerByWikiDocPre(docs, model, lang);
        return ranker;
    }

    private void evalCands(List<QueryDocument> docs, String lang){
        int get = 0, total = 0;
        int pget = 0, oget = 0, lget = 0;
        LangLinker ll = new LangLinker();
        int ncands = 0, nm = 0;
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                List<String> cands;
                ncands+=m.getCandidates().size();
                nm++;
                if(!lang.equals("en"))
                    cands = m.getCandidates().stream().map(x -> ll.translateToEn(x.getTitle(), lang))
                        .filter(x -> x != null).collect(toList());
                else
                    cands = m.getCandidates().stream().map(x -> x.getTitle()).collect(Collectors.toList());

                if(cands.contains(m.gold_enwiki_title.toLowerCase())) {
                    if(m.getType().equals("PER")) pget++;
                    if(m.getType().equals("ORG")) oget++;
                    if(m.getType().equals("LOC")) lget++;
                    get++;
                }
                else{
                    System.out.println(m.getMention()+" --> "+m.gold_enwiki_title+" "+m.getType());
                }
                total++;
            }
        }

        System.out.println("PER "+(double)pget/docs.stream().flatMap(x -> x.mentions.stream()).filter(x -> x.getType().equals("PER")).count());
        System.out.println("ORG "+(double)oget/docs.stream().flatMap(x -> x.mentions.stream()).filter(x -> x.getType().equals("ORG")).count());
        System.out.println("LOC "+(double)lget/docs.stream().flatMap(x -> x.mentions.stream()).filter(x -> x.getType().equals("LOC")).count());
        System.out.println(get+" "+total+" "+(double)get/total);
        System.out.println("avg # cands: "+(double)ncands/nm);
    }

    private void filterNonNE(List<QueryDocument> docs){
        logger.info("Before filtering "+docs.size()+" docs "+docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");

        int cnt = 0, per = 0, loc = 0, org = 0;
        for(QueryDocument doc: docs){
            List<ELMention> filtered = new ArrayList<>();
            if(cnt++%500 == 0) System.out.println(cnt);
            for(ELMention m: doc.mentions){
                String mid = FreeBaseQuery.getMidFromTitle(m.gold_enwiki_title, "en");
                if(mid != null){
                    List<String> types = FreeBaseQuery.getTypesFromMid(mid);
                    if(types.contains("people.person") || types.contains("location.location") || types.contains("organization.organization")) {
//                        m.types = types.stream().collect(Collectors.toList());
                        m.types=FreeBaseQuery.getTypesFromMid(mid);
                        if(types.contains("people.person")) {
                            m.setType("PER");
                            per++;
                        }
                        else if(types.contains("location.location")) {
                            m.setType("LOC");
                            loc++;
                        }
                        else if(types.contains("organization.organization")) {
                            m.setType("ORG");
                            org++;
                        }
                        filtered.add(m);
                    }
                }
            }
            doc.mentions = filtered;
        }
        docs = docs.stream().filter(x -> x.mentions.size()>0).collect(Collectors.toList());
        logger.info("After filtering "+docs.size()+" docs "+docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions");
        logger.info("PER "+per+" ORG "+org+" LOC "+loc);

    }

    /**
     * This method generates trianing and test data from wikipedia articles
     * @param lang
     * @param n_docs
     * @param test_start
     * @param test_end
     */
    public void generateWikiData(String lang, int n_docs, int test_start, int test_end){
        logger.info("Generating WikiData for "+lang);
        WikiCandidateGenerator wcg = new WikiCandidateGenerator();
        WikiDocReader dreader = new WikiDocReader();
        LangLinker ll = new LangLinker();

        List<QueryDocument> docs = dreader.readWikiDocsNew(lang, 0, n_docs);
        train_pairs = new HashSet<>();
        train_pairs.addAll(docs.stream().flatMap(x -> x.mentions.stream())
                .map(x -> x.getMention().toLowerCase()+"_"+x.gold_wiki_title.toLowerCase())
                .collect(toSet()));
        if(!lang.equals("en")) {
            for (QueryDocument doc : docs) {
                doc.mentions.forEach(x -> x.gold_enwiki_title = ll.translateToEn(x.gold_wiki_title, lang));
                doc.mentions = doc.mentions.stream().filter(x -> x.gold_enwiki_title != null).collect(Collectors.toList());
            }
        }
        logger.info("After selecting mentions have en title: "+docs.stream().flatMap(x -> x.mentions.stream()).count());

        wcg.genCandidates(docs, lang);
        wcg.selectMentions(docs, 2);
        writeFile(docs, "/shared/bronte/ctsai12/multilingual/data/wikidata2/"+lang+"/train/");

        docs = dreader.readWikiDocsNew(lang, test_start, test_end);

        logger.info("Before filtering training mentions: "+docs.stream().flatMap(x -> x.mentions.stream()).count());
        for(QueryDocument doc: docs){
            doc.mentions = doc.mentions.stream().filter(x -> !train_pairs.contains(x.getMention().toLowerCase()+"_"+x.gold_wiki_title.toLowerCase())).collect(toList());
        }
        logger.info("After filtering training mentions: "+docs.stream().flatMap(x -> x.mentions.stream()).count());
        if(!lang.equals("en")) {
            for (QueryDocument doc : docs) {
                doc.mentions.forEach(x -> x.gold_enwiki_title = ll.translateToEn(x.gold_wiki_title, lang));
                doc.mentions = doc.mentions.stream().filter(x -> x.gold_enwiki_title != null).collect(Collectors.toList());
            }
        }
        logger.info("After selecting mentions have en title: "+docs.stream().flatMap(x -> x.mentions.stream()).count());
        wcg.genCandidates(docs, lang);
        wcg.selectMentions(docs, 2);
        writeFile(docs, "/shared/bronte/ctsai12/multilingual/data/wikidata2/"+lang+"/test/");
        wiki_reader.readTrainData(lang);
        wiki_reader.readTestData(lang);
    }

    public void writeFile(List<QueryDocument> docs, String dir){
        for(QueryDocument doc: docs) {

            if(doc.mentions.size() == 0) continue;
            String out = "";
            for(ELMention m: doc.mentions){
                out += m.getStartOffset()+"\t"+m.getEndOffset()+"\t"+m.gold_enwiki_title+"\t"+m.gold_wiki_title;
                if(m.eazy)
                    out+= "\t0";
                else
                    out+="\t1";
                out+="\n";
            }
            try {
                String fname = doc.getDocID();
                if(fname.length() > 40)
                    fname = fname.substring(0, 40);
                FileUtils.writeStringToFile(new File(dir, fname+".txt"), doc.plain_text, "UTF-8");
                FileUtils.writeStringToFile(new File(dir, fname+".mentions"), out, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private void setCands(List<QueryDocument> docs, String lang, WikiCandidateGenerator wcg){
        List<ELMention> mentions = docs.stream().flatMap(x -> x.mentions.stream()).collect(toList());
        System.out.println("Querying en cands by foreign mentions...");
        for(ELMention m: mentions){
            List<WikiCand> cands = wcg.getCandsBySurface(m.getMention().toLowerCase(), "en", false);
            cands.addAll(wcg.getCandidateByWord(m.getMention().toLowerCase(), "en", 10));
            m.getCandidates().addAll(cands);
        }
        System.out.println("Querying en cands by transliteration...");
        int c = 0;
        for(ELMention m: mentions){
            if(c++%10 == 0) System.out.print(c+"\r");

//            if(m.getCandidates().size() == 0) {
                List<WikiCand> cands = wcg.getCandsByTransliteration(m.getMention(), lang);
                m.getCandidates().addAll(cands);
//            }
        }

        System.out.println("#tokens need transliterate "+wcg.trans.ntrans);
    }

    public static void main(String[] args) {
        WikiDataWithTransExp wde = new WikiDataWithTransExp();

        FreeBaseQuery.loadDB(true);

        wde.run(args[0]);

//        wde.generateWikiData("es", 5000, 5000, 11000); // note that the final data is from es wikifier
//        wde.generateWikiData("fr", 5000, 5000, 8000);
//        wde.generateWikiData("de", 5000, 5000, 8000);
//        wde.generateWikiData("it", 8000, 10000, 15000);
//        wde.generateWikiData("zh", 20000, 50000, 80000);
//        wde.generateWikiData("tr", 10000, 50000, 70000);
//        wde.generateWikiData("he", 5000, 5000, 12000);
//        wde.generateWikiData("ar", 20000, 50000, 70000);
//        wde.generateWikiData("ta", 30000, 30000, 150000);
//        wde.generateWikiData("th", 30000, 50000, 100000);
//        wde.generateWikiData("tl", 20000, 50000, 400000);
//        wde.generateWikiData("ur", 10000, 10000, 30000);

//        wde.generateWikiData("en", 1000, 5000, 6000);
    }
}
