package edu.illinois.cs.cogcomp.xlwikifier.experiments.tac2016;

import edu.illinois.cs.cogcomp.core.algorithms.LevensteinDistance;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.mlner.classifier.BinaryTypeClassifier;
import edu.illinois.cs.cogcomp.mlner.classifier.FiveTypeClassifier;
import edu.illinois.cs.cogcomp.mlner.experiments.conll.ColumnFormatReader;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.core.StopWord;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.TAC2015Exp;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.TACReader;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.MediaWikiSearch;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.illinois.cs.cogcomp.mlner.core.Utils;

import static java.util.stream.Collectors.*;

/**
 * Created by ctsai12 on 9/9/16.
 *
 * Given an NER output dir, first use copyXmlOffsets to populate xml offsets for each token.
 * They wikifyMentions uses Cross-Lingual Wikifier to assign Wikipedai titles and FreeBase IDs.
 * The function printResults extract author mentions and print the wikified results in tab format.
 * Since there are a lot of eval docs, I divide docs into chunks. The above steps are applied to each chunk.
 * The function combine combines the outputs of each chunk
 * Clustering conducts a simple surface-based clustering. This is mainly for NILs, but will fix
 * non-NIL clusters as well.
 * In the end, mergeNOM combines the results with NOM annotations.
 */
public class Solver {



    public void copyXmlOffsets(String indir, String origdir, String outdir){


        int cnt = 0;
        for(File f: new File(indir).listFiles()){
//            if(!f.getName().equals("SPA_DF_000389_20151119_G00A09NZ5")) continue;
            if(cnt++%100 == 0) System.out.print(cnt+"\r");
            if(new File(outdir, f.getName()).exists()) continue;

            List<String> inlines=null, origlines = null;
            try {
                inlines = LineIO.read(f.getAbsolutePath());
                origlines = LineIO.read(origdir+"/"+f.getName());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            inlines = inlines.stream().filter(x -> !x.contains("-DOCSTART-")).collect(Collectors.toList());

            origlines = origlines.stream().filter(x -> x.split("\t").length > 5).collect(Collectors.toList());
            inlines = inlines.stream().filter(x -> x.split("\t").length > 5).collect(Collectors.toList());

            long n = inlines.stream().filter(x -> !x.trim().isEmpty()).count();
            long n1 = origlines.stream().filter(x -> !x.trim().isEmpty()).count();
            if(n!=n1){
                System.out.println("line # differs "+n+" "+n1);
                System.out.println(f.getName());
                System.exit(-1);
//                continue;
            }


//            if(inlines.size() == origlines.size()+1 && inlines.get(inlines.size()-1).trim().isEmpty())
//                inlines = inlines.subList(0, inlines.size()-1);
//            if(inlines.size()!=origlines.size()){
//
//            }

            String out = "";
            int j = 0;
            for(int i = 0; i < inlines.size(); i++){
                if(inlines.get(i).trim().isEmpty()){
                    out+="\n";
                    continue;
                }

                while(origlines.get(j).trim().isEmpty())j++;


                String[] inparts = inlines.get(i).split("\t");
                String[] origparts = origlines.get(j).split("\t");

//                System.out.println(origlines.get(j));
//                System.out.println(origparts[5]);

//                System.out.println(inparts[5]+" "+origparts[5]);
//                if(!inparts[5].equals(origparts[5]) && !origparts[5].startsWith(inparts[5])){
//                    System.out.println("surface doesn't match! "+inparts[5]+" "+origparts[5]);
//                    System.out.println(f.getName());
//                    System.exit(-1);
//                }

                inparts[1] = origparts[1];
                inparts[2] = origparts[2];
//                System.out.println(inlines.get(i));
//                System.out.println(origlines.get(i));
                out += Arrays.asList(inparts).stream().collect(joining("\t"))+"\n";
                j++;
            }
            try {
                FileUtils.writeStringToFile(new File(outdir, f.getName()), out, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Find text regions that are inside <quote> and </quote> (in xml offsets)
     * @param doc
     * @return
     */
    public static List<Pair<Integer, Integer>> getBadIntervals(QueryDocument doc){

        List<Pair<Integer, Integer>> ret = new ArrayList<>();
        int start = -1;
        for(int i = 0; i < doc.xml_text.length(); i++){
            if(start == -1 && doc.xml_text.substring(i).startsWith("<quote"))
                start = i;

            if(start > -1 && doc.xml_text.substring(0, i).endsWith("/quote>")) {
                ret.add(new Pair(start, i));
                start = -1;
            }

        }

        return ret;
    }

    /**
     * Text between <quote> and </quote> shouldn't be annotated
     * @param docs
     * @param xmldocs
     */
    public void removeQuoteMentions(List<QueryDocument> docs, List<QueryDocument> xmldocs){

        Map<String, List<QueryDocument>> did2doc = xmldocs.stream().collect(groupingBy(x -> x.getDocID()));
        int n_add = 0;
        for(QueryDocument doc: docs) {
            List<Pair<Integer, Integer>> interval = getBadIntervals(did2doc.get(doc.getDocID()).get(0));

            List<ELMention> nm = new ArrayList<>();
            for(ELMention m: doc.mentions){
                boolean bad = false;
                for(Pair<Integer, Integer> inte: interval){

                    if((m.xml_start >= inte.getFirst() && m.xml_start < inte.getSecond())
                            ||(inte.getFirst() >= m.xml_start && inte.getFirst() < m.xml_end)) {
                        bad = true;
                        n_add++;
                        break;
                    }
                }

                if(!bad)
                    nm.add(m);
            }
            doc.mentions = nm;
        }
        System.out.println("Removed "+n_add+" mentions");
    }

    /**
     * This is diffeerent from the one in TAC2015Exp/
     * Extract author names from <AUTHOR> </AUTHOR>
     * Not sure if these names should be annotated
     * @return
     */
    public void extractAuthors(){

        List<QueryDocument> docs = TACReader.readSpanish2016EvalDocs();
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter("/shared/bronte/tac2015/neleval/2016/es-eval-authors"));
            List<ELMention> ret = new ArrayList<>();
            for(QueryDocument doc: docs){
                Pattern pattern = Pattern.compile(" <AUTHOR>(.*?)</AUTHOR>");

                Matcher matcher = pattern.matcher(doc.getXmlText());
                while(matcher.find()){
                    String mention = matcher.group(1);
                    int start = matcher.start(1);
                    int end = matcher.end(1);
                    while(mention.startsWith(" ")) {
                        start++;
                        mention = mention.substring(1);
                    }
                    while(mention.endsWith(" ")) {
                        end--;
                        mention = mention.substring(0, mention.length()-1);
                    }

                    bw.write(doc.getDocID()+"\t"+start+"\t"+(end-1)+"\tNIL\t"+0+"\tPER\t"+mention+"\n");
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Print wikified results. Note that this is not the final result for submission.
     * @param docs
     * @param xmldocs
     * @param outfile
     */
    public void printResults(List<QueryDocument> docs, List<QueryDocument> xmldocs, String outfile){


        TAC2015Exp te = new TAC2015Exp();
        List<ELMention> authors = te.extractAuthors(xmldocs);
//        for(ELMention m: authors){
//            if(m.getEndOffset() <= m.getStartOffset()){
//                continue;
//            }
//            out += m.getDocID()+"\t"+m.getStartOffset()+"\t"+m.getEndOffset()+"\t"+"NIL"+"\t"+0+"\t"+m.getType()+"\n";
//        }

        for(QueryDocument doc: docs){
            List<ELMention> as = authors.stream().filter(x -> x.getDocID().equals(doc.getDocID())).collect(Collectors.toList());
            as.forEach(x -> x.setMid("NIL"));
            for(ELMention m: as){
                boolean get = false;
//                for(ELMention m1: as){
//                    if(!m.getMention().equals(m1.getMention()) && m1.getMention().contains(m.getMention())){
//                        m.setType("GPE");
//                        get = true;
//                        break;
//                    }
//                }
                if(!get) m.setType("PER");
                m.xml_start = m.getStartOffset();
                m.xml_end = m.getEndOffset();
            }
            doc.mentions.addAll(as);
        }

//        propogateNERLabel(docs, xmldocs, 2);
        removeQuoteMentions(docs, xmldocs);
        fixPerMid(docs);

//        SurfaceClustering cluster = new SurfaceClustering();
//        List<ELMention> mentions = cluster.cluster(docs);

        String out = "";
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
            for(QueryDocument doc: docs){
                for(ELMention m: doc.mentions){
                    if(m.xml_end <= m.xml_start)
                        continue;
                    bw.write(m.getDocID()+"\t"+m.xml_start+"\t"+(m.xml_end-1)+"\t"+m.getMid()+"\t"+0+"\t"+m.getType()+"\t"+m.getMention()+"\n");
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Results are at "+outfile);
//        try {
//            FileUtils.writeStringToFile(new File(outfile), out, "UTF-8");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    /**
     * Use longer names' MID
     * @param docs
     */
    public void fixPerMid(List<QueryDocument> docs){
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                if(!m.getType().equals("PER")) continue;
                String mid = null;
                int length = m.getMention().length();
//                String type = m.getType();

                for(ELMention m1: doc.mentions){
                    if(!m1.getType().equals("PER")) continue;
                    if(m1.getMention().toLowerCase().contains(m.getMention().toLowerCase())
                            && m1.getMention().length() > length
                            && !m1.getMid().startsWith("NIL")){
                        mid = m1.getMid();
                        length = m1.getMention().length();
                    }
                }

                if(mid!=null)
                    m.setMid(mid);
            }
        }
    }

    /**
     * This is for analysis purpose
     * @param docs
     * @param xmldocs
     */
    public void setGoldMid(List<QueryDocument> docs, List<QueryDocument> xmldocs){
        int nogold = 0;
        Map<String, List<QueryDocument>> did2docs = xmldocs.stream().collect(groupingBy(x -> x.getDocID()));
        for(QueryDocument doc: docs) {
            QueryDocument xdoc = did2docs.get(doc.getDocID()).get(0);
            for (ELMention m : doc.mentions) {

                String gold = null;
                for(ELMention xm: xdoc.mentions){
                    if((m.xml_start >= xm.getStartOffset() && m.xml_start <= xm.getEndOffset())
                            || (xm.getStartOffset() >= m.xml_start && xm.getStartOffset() < m.xml_end)){
                        gold = xm.gold_mid;
                        break;
                    }
                }

                if(gold == null) {
//                    System.out.println(doc.getDocID()+" "+m.getMention()+" "+m.xml_start+" "+m.xml_end);
                    nogold++;
                }

                m.gold_mid = gold;

            }
        }

        System.out.println("no match "+nogold);

    }

    /**
     * Use wikipedia search API to query titles given mentions
     * @param docs
     * @param lang
     */
    public void wikiSearchSolver(List<QueryDocument> docs, String lang){
        System.out.println("Querying wikipedia...");
        MediaWikiSearch ws = new MediaWikiSearch();
        String fb_lang = lang;
        if(lang.equals("zh")) fb_lang = "zh-cn";

        int cnt = 0;
        for(QueryDocument doc: docs){
            if(cnt++%100 == 0) System.out.print(cnt+"\r");
            for(ELMention m: doc.mentions){
                if(m.getCandidates().size() > 0) continue;

                List<String> titles = ws.search1(m.getMention(), lang, "fuzzy");

                String title = null;
                String mid = null;
                if(titles.size()>0) {
                    title = titles.get(0);
                    mid = FreeBaseQuery.getMidFromTitle(title, fb_lang);
                }
                else {
                    titles = ws.search1(m.getMention(), "en", "fuzzy");
                    if (titles.size() > 0) {
                        String en_mid = FreeBaseQuery.getMidFromTitle(titles.get(0), "en");
                        if (title == null || mid == null) {
                            title = titles.get(0);
                            mid = en_mid;
                        } else {
                            int d = LevensteinDistance.getLevensteinDistance(m.getMention(), title);
                            int d1 = LevensteinDistance.getLevensteinDistance(m.getMention(), titles.get(0));
                            if (d1 < d) {
                                title = titles.get(0);
                                mid = en_mid;
                            }
                        }
                    }
                }


                if(mid != null) {
                    int d = LevensteinDistance.getLevensteinDistance(m.getMention(), title);
//                    if(m.getCandidates().size() == 0){ // || (!lang.equals("zh") && d <=1) ) {
//                        System.out.println(m.getMention()+" "+title);
                        m.setMid(mid);
                        m.setWikiTitle(title);
//                    }
                }
            }
        }
    }

    /**
     * Wikify mentions in docs (NER is done)
     * @param docs
     */
    public void wikifyMentions(List<QueryDocument> docs, String lang){


        if(lang.equals("zh")){
            for(QueryDocument doc: docs)
                doc.mentions.forEach(x -> x.setMention(x.getMention().replaceAll("\\s+", "")));
        }


        if(!FreeBaseQuery.isloaded())
            FreeBaseQuery.loadDB(true);

        TAC2015Exp te = new TAC2015Exp();
        WikiCandidateGenerator wcg = new WikiCandidateGenerator(true);
        wcg.genCandidates(docs, lang);
        WikiCandidateGenerator wcg_en = new WikiCandidateGenerator(true);
        wcg_en.genCandidates(docs, "en");
        Ranker ranker = Ranker.loadPreTrainedRanker(lang, "ranker/tac-"+lang);
//        Ranker ranker = te.trainRanker(lang, 1000, 3);
//        ranker.saveLexicalManager("models/ranker/tac-es/lm");
        ranker.setWikiTitleByModel(docs);
//        Ranker ranker = new Ranker();
//        ranker.setWikiTitleByTopCand(docs);

        System.out.println("no cand "+docs.stream().flatMap(x -> x.mentions.stream()).filter(x -> x.getCandidates().size()==0).count());

        Utils utils = new Utils();
        for(QueryDocument doc: docs) {
            utils.setMidByWikiTitle(doc);
        }

        BinaryTypeClassifier bc = new BinaryTypeClassifier();
        bc.train(true);
        FiveTypeClassifier fc = new FiveTypeClassifier();
        fc.train(false);
        for(QueryDocument doc: docs) {
            List<ELMention> newm = new ArrayList<>();
            for(ELMention m: doc.mentions) {
                if(!m.getMid().startsWith("NIL")) {
                    if(!bc.isTheTypes(m.getMid())) continue;
                    String type = fc.getCoarseType(m.getMid());
                    m.setType(type);
                }
                newm.add(m);
            }

            doc.mentions = newm;
        }
        wikiSearchSolver(docs, lang);
    }

    /**
     * This doesn't work well
     * @param docs
     * @param xmldocs
     * @param th
     */
    public void propogateNERLabel(List<QueryDocument> docs, List<QueryDocument> xmldocs, int th){

        Map<String, List<QueryDocument>> did2doc = xmldocs.stream().collect(groupingBy(x -> x.getDocID()));
        int n_add = 0;
        for(QueryDocument doc: docs){
            String xml_text = did2doc.get(doc.getDocID()).get(0).xml_text;
            String text = xml_text.toLowerCase();

            List<ELMention> newm = new ArrayList<>();
            Map<String, List<ELMention>> surface2m = doc.mentions.stream().collect(groupingBy(x -> x.getMention().toLowerCase()));
            Map<String, String> surf2type = new HashMap<>();
            Map<String, String> mid2type = new HashMap<>();
            for(String surf: surface2m.keySet()){
                if(surface2m.get(surf).size()<2) continue;
                if(surf.length() < th) continue;
                List<Map.Entry<String, Long>> type2cnt = surface2m.get(surf).stream().map(x -> x.getType()).collect(groupingBy(x -> x, counting()))
                        .entrySet().stream().sorted((x1, x2) -> Long.compare(x2.getValue(), x1.getValue()))
                        .collect(Collectors.toList());
                surf2type.put(surf, type2cnt.get(0).getKey());
                List<Map.Entry<String, Long>> mid2cnt = surface2m.get(surf).stream().map(x -> x.getMid()).collect(groupingBy(x -> x, counting()))
                        .entrySet().stream().sorted((x1, x2) -> Long.compare(x2.getValue(), x1.getValue()))
                        .collect(Collectors.toList());
                mid2type.put(surf, mid2cnt.get(0).getKey());
            }
            for(String surf: surf2type.keySet()){
                int search_start = 0;
                while(true){
                    int idx = text.indexOf(surf, search_start);
                    if(idx == -1) break;
                    search_start = idx + surf.length();

                    boolean overlap = false;
                    for(ELMention mm: doc.mentions) {
                        if ((mm.xml_start >= idx && mm.xml_start <= idx+surf.length())
                                || (idx >= mm.xml_start && idx <mm.xml_end)){
                            overlap = true;
                            break;
                        }
                    }

                    if(!overlap){
                        ELMention nm = new ELMention(doc.getDocID(), idx, idx+surf.length());
                        nm.setMention(xml_text.substring(idx, idx+surf.length()));
                        nm.setType(surf2type.get(surf));
                        nm.xml_start = idx;
                        nm.xml_end = idx+surf.length();
                        nm.setMid(mid2type.get(surf));
                        System.out.println(surf+" "+nm.getType()+" "+ nm.getMid()+" "+nm.xml_end+" "+nm.xml_end+" "+doc.getDocID());
                        newm.add(nm);
                        n_add++;
                    }

                }

            }

            doc.mentions.addAll(newm);
            doc.mentions = doc.mentions.stream().sorted((x1, x2) -> Integer.compare(x1.getStartOffset(), x2.getStartOffset())).collect(Collectors.toList());
        }
        System.out.println("Added "+n_add+" mentions");
    }


    public List<QueryDocument> readTabFile(String file){

        Map<String, QueryDocument> did2doc = new HashMap<>();

        //out += m.getDocID()+"\t"+m.xml_start+"\t"+(m.xml_end-1)+"\t"+m.getMid()+"\t"+0+"\t"+m.getType()+"\t"+m.getMention()+"\n";
        int bad = 0;
        try {
            for(String line: LineIO.read(file)){
                String[] parts = line.trim().split("\t");
                if(parts.length<6){
                    bad++;
                    continue;
                }
                String docid = parts[0];
                if(!did2doc.containsKey(docid))
                    did2doc.put(docid, new QueryDocument(docid));
                QueryDocument doc = did2doc.get(docid);

                ELMention m = new ELMention(docid, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                m.setMid(parts[3]);
                m.setType(parts[5]);
                if(parts.length>6)
                    m.setMention(parts[6]);
                doc.mentions.add(m);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("#bad lines "+bad);

        return did2doc.values().stream().collect(Collectors.toList());
    }

    /**
     * Since there are a lot of eval docs, they are divided into chunks.
     * This is used to combine the wikified results of each chunk.
     * @param outfile
     */
    public void combine(String outfile){
        String path = "/shared/bronte/tac2015/neleval/2016/zh-eval-";

        List<QueryDocument> docs = new ArrayList<>();
        for(int i = 0; i < 6; i++){
            docs.addAll(readTabFile(path+(i+1)+"-new"));
        }

        System.out.println("Loaded "+docs.size()+" docs "+docs.stream().flatMap(x -> x.mentions.stream()).count()+" mentions from tab files");

        // get the surface string, cause I forgot to save them
//        List<QueryDocument> xmldocs = TACReader.readSpanish2016EvalDocs();
        List<QueryDocument> xmldocs = TACReader.readChinese2016EvalDocs();
        Map<String, List<QueryDocument>> did2doc = xmldocs.stream().collect(groupingBy(x -> x.getDocID()));

        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(outfile));
            for(QueryDocument doc: docs){
                for(ELMention m: doc.mentions){
                    String surf = did2doc.get(m.getDocID()).get(0).xml_text.substring(m.getStartOffset(), m.getEndOffset() + 1);
                    m.setMention(surf);
                    bw.write(m.getDocID()+"\t"+m.getStartOffset()+"\t"+m.getEndOffset()+"\t"+m.getMid()+"\t"+0+"\t"+m.getType()+"\t"+m.getMention()+"\n");
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove xml tags if exist in mentnions
     * @param docs
     */
    public void cleanSurface(List<QueryDocument> docs){
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                String surf = m.getMention();
                int idx = surf.indexOf("<");
                if(idx > -1){
                    while(idx > 0 && surf.substring(idx-1, idx).trim().isEmpty()) idx--;
                    if(idx>0) {
                        m.setEndOffset(m.getEndOffset()-(surf.length()-idx));
                        m.setMention(surf.substring(0, idx));
//                        System.out.println(surf+" ||| "+m.getMention()+" "+m.getStartOffset()+" "+m.getEndOffset());
                    }
                }
            }
        }
    }

    /**
     * This works better for Chinese
     * @param infile
     * @param outfile
     */
    public void Clustering(String infile, String outfile){

        List<QueryDocument> docs = readTabFile(infile);

//        docs.addAll(readTabFile("/shared/bronte/tac2015/neleval/2016/zh-eval-authors"));

        cleanSurface(docs);

        SurfaceClustering cluster = new SurfaceClustering();
        cluster.cluster(docs);

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
            for(QueryDocument doc: docs){
                for(ELMention m: doc.mentions){
//                    bw.write(m.getDocID()+"\t"+m.getStartOffset()+"\t"+m.getEndOffset()+"\t"+m.getMid()+"\t"+0+"\t"+m.getType()+"\t"+m.getMention()+"\n");
                    bw.write(m.getDocID()+"\t"+m.getStartOffset()+"\t"+m.getEndOffset()+"\t"+m.getMid()+"\t"+0+"\t"+m.getType()+"\n");
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Results are at "+outfile);
    }

    public void mergeNOM(String tabfile, String nomfile){

        String outfile = "/shared/bronte/tac2015/neleval/2016/zh-submission-5";
        List<QueryDocument> docs = readTabFile(tabfile);

        ColumnFormatReader cr = new ColumnFormatReader();
        cr.readDir(nomfile,false);
        Map<String, List<QueryDocument>> did2docs = cr.docs.stream().collect(groupingBy(x -> x.getDocID()));

        int n = 0;
        int nil = 9999999;
        for(QueryDocument doc: docs){
            QueryDocument nomdoc = did2docs.get(doc.getDocID()).get(0);
            List<ELMention> nm = new ArrayList<>();
            for(ELMention nom: nomdoc.mentions){

                nom.setStartOffset(nom.xml_start);  // important! use output offsets
                nom.setEndOffset(nom.xml_end-1);

                boolean over = false;
                for(ELMention nam: doc.mentions){
                    if((nam.getStartOffset() >= nom.getStartOffset() && nam.getStartOffset() <= nom.getEndOffset())
                            || (nom.getStartOffset() >= nam.getStartOffset() && nom.getStartOffset() <= nam.getEndOffset())){
                        over = true;
                        n++;
                        break;
                    }
                }
                nom.noun_type = "NOM";
                nom.setMention(nom.getMention().replaceAll("\\s+", ""));
                if(!over) nm.add(nom);
            }
            doc.mentions.forEach(x -> x.noun_type = "NAM");
            doc.mentions.addAll(nm);
            doc.mentions = doc.mentions.stream().sorted((x1, x2) -> Integer.compare(x1.getStartOffset(), x2.getStartOffset())).collect(Collectors.toList());

            int window = 20;
            boolean use_window = false;
            boolean remove = true;

            Set<String> stops = StopWord.getStopWords("zh");
            doc.mentions = doc.mentions.stream().filter(x -> !x.getMention().trim().isEmpty() && !stops.contains(x.getMention().trim())).collect(Collectors.toList());

            for(int i = 0; i < doc.mentions.size(); i++){
                ELMention m = doc.mentions.get(i);
                if(!m.noun_type.equals("NOM")) continue;

                ELMention prev_nam = null, prev_nom = null;
                for(int j = i-1; j >=0; j--){
                    ELMention pm = doc.mentions.get(j);
                    if(pm == null) continue;
                    if(prev_nam == null && pm.noun_type.equals("NAM") && pm.getType().equals(m.getType())) {
                        if(!use_window || m.getStartOffset() - pm.getEndOffset() < window)
                            prev_nam = pm;
                    }
                    if(prev_nom == null && pm.noun_type.equals("NOM") && pm.getType().equals(m.getType())
                            && pm.getMention().toLowerCase().equals(m.getMention().toLowerCase())){
                        if(!use_window || m.getStartOffset() - pm.getEndOffset() < window) {
                            prev_nom = pm;
                        }
                    }
                }

                if(prev_nom != null){
                    m.setMid(prev_nom.getMid());
                }
                else if(prev_nam != null){
                    m.setMid(prev_nam.getMid());
                }
                else{
                    if(remove) {
                        doc.mentions.set(i, null);
                    }
                    else {
                        m.setMid("NIL" + nil);
                        nil--;
                    }
                }


            }
            doc.mentions = doc.mentions.stream().filter(x -> x!=null).collect(Collectors.toList());
        }

        outputSubmission(docs, outfile);
    }

    public void outputSubmission(List<QueryDocument> docs, String outfile){

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
            int menid = 0;
            for(QueryDocument doc: docs){
                for(ELMention m: doc.mentions){
                    bw.write("UI_CCG_5\tZH-"+menid+"\t"+m.getMention()+"\t"+doc.getDocID()+":"+m.getStartOffset()+"-"+m.getEndOffset()+"\t"+m.getMid()+"\t"+m.getType()+"\t"+m.noun_type+"\t1.0\n");
                    menid++;
                }
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        String nerdir = "/shared/corpora/ner/wikifier-features/es/eval2016-tagged";
        nerdir = "/shared/corpora/ner/wikifier-features/zh/tac2015-test12-char-ann1";
        String origdir = "/shared/corpora/ner/tac/es/eval2016";
        origdir = "/shared/corpora/ner/tac/zh/test-new12-char";
        String outdir = "/shared/corpora/ner/wikifier-features/es/tac2015-test12-ann-xml1";
        outdir = "/shared/corpora/ner/wikifier-features/zh/tac2015-test12-char-ann-xml1";


        Solver solver = new Solver();
//        solver.copyXmlOffsets(nerdir, origdir, outdir);

        String lang = "zh";

//        List<QueryDocument> docs = solver.mergeNEs(outdir, outdir1);

//        List<QueryDocument> docs = new ArrayList<>();
//        String path = "/shared/corpora/ner/wikifier-features/es/eval2016-";
//        for(int i = 0; i < 4; i++) {
//            ColumnFormatReader r = new ColumnFormatReader();
//            r.readDir(path+(i+1)+"-ann-xml", false);
//            docs.addAll(r.docs);
//        }


//        solver.extractAuthors();
//        System.exit(-1);

        ColumnFormatReader r = new ColumnFormatReader();
        r.readDir(outdir, false);
        List<QueryDocument> docs = r.docs;
//        Set<String> dids = docs.stream().map(x -> x.getDocID()).collect(Collectors.toSet());
//
        List<QueryDocument> xmldocs = null;
//        xmldocs = TACReader.readChinese2016EvalDocs(dids);
//        if(xmldocs.size()!=docs.size()){
//            System.out.println("Doc size doesn't match");
//            System.exit(-1);
//        }
        if(lang.equals("es"))
            xmldocs = TACReader.readSpanishDocuments(false);
        else if(lang.equals("zh"))
            xmldocs = TACReader.readChineseDocuments(false);
//
////        solver.setGoldMid(r.docs, xmldocs);

//        solver.removeQuoteMentions(docs, xmldocs);
//        solver.wikifyMentions(docs, lang);
        String outfile = "/shared/bronte/tac2015/neleval/2016/zh-2015-1";
//        solver.printResults(docs, xmldocs, outfile);

//        String outfile = "/shared/bronte/tac2015/neleval/2016/zh-eval-all";
//        solver.combine(outfile);
//
//
        solver.Clustering(outfile, outfile+"-cluster-authors");


        String tabfile = "/shared/bronte/tac2015/neleval/2016/zh-eval-all-cluster-authors";
        String nomfile = "/shared/corpora/ner/tac/zh/eval2016-nom-xml-new";
//        solver.mergeNOM(tabfile, nomfile);

    }
}
