package edu.illinois.cs.cogcomp.mlner.experiments.ere;

import edu.illinois.cs.cogcomp.mlner.experiments.tac.WriteConllFormat;
import edu.illinois.cs.cogcomp.tokenizers.CharacterTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.TACReader;
import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 9/8/16.
 */
public class EREReader {

    private String es_root = "/shared/corpora/corporaWeb/lorelei/LDC2015E107_DEFT_Rich_ERE_Spanish_Annotation_V2/data/";

    public EREReader(){

    }
    public List<QueryDocument> readChineseDocs(){
        String ere_dir = "/shared/corpora/corporaWeb/tac/2016/ere-all/zh/ere";
        String src_dir = "/shared/corpora/corporaWeb/tac/2016/ere-all/zh/source";
        String lang = "zh";

        try {
            return readDocs(ere_dir, src_dir, lang);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<QueryDocument> readSpanishDocs(){
        String ere_dir = "/shared/corpora/corporaWeb/tac/2016/ere-all/es-test/ere";
        String src_dir = "/shared/corpora/corporaWeb/tac/2016/ere-all/es-test/source";
        String lang = "es";

        try {
            return readDocs(ere_dir, src_dir, lang);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<QueryDocument> readDocs(String ere_dir, String src_dir, String lang) throws Exception {

        Tokenizer tokenizer = MultiLingualTokenizer.getTokenizer(lang);
        if(lang.equals("zh"))
            tokenizer = new CharacterTokenizer();

        List<QueryDocument> ret = new ArrayList<>();

        List<ELMention> ms = new ArrayList<>();

        for(File f: new File(ere_dir).listFiles()){

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(f);
            doc.getDocumentElement().normalize();

            Set<String> seen = new HashSet<>();

            NodeList nodes = doc.getElementsByTagName("entity");
            for(int i = 0; i < nodes.getLength(); i++){
                Element entity = (Element) nodes.item(i);
                String ent_type = entity.getAttribute("type");
                NodeList mentions = entity.getElementsByTagName("entity_mention");
                for(int j = 0; j < mentions.getLength(); j++){
                    Element mention = (Element) mentions.item(j);
                    String noun_type = mention.getAttribute("noun_type");

                    if(!noun_type.equals("NAM") && !noun_type.equals("NOM")) continue;

                    String start_off = mention.getAttribute("offset");
                    String length = mention.getAttribute("length");
                    String docid = mention.getAttribute("source");


//                    System.out.println(docid+" "+start_off);
                    Node text = mention.getElementsByTagName("mention_text").item(0);

                    String surface;
                    if(text!=null)
                        surface = text.getTextContent();
                    else
                        surface = mention.getTextContent();

                    if(noun_type.equals("NOM")){
                        Node element = mention.getElementsByTagName("nom_head").item(0);
                        surface = element.getTextContent();
                        start_off = ((Element)element).getAttribute("offset");
                        length = ((Element)element).getAttribute("length");
                    }

                    int start = Integer.parseInt(start_off);
                    int end = start+Integer.parseInt(length);

                    if(seen.contains(start+"_"+end)) continue;
                    seen.add(start+"_"+end);

                    ELMention m = new ELMention(docid, start, end);
                    m.setMention(surface);
                    m.setType(ent_type);
                    m.noun_type = noun_type;
                    ms.add(m);
                }
            }
        }

        for(File f: new File(src_dir).listFiles()) {

            String[] parts = f.getName().split("\\.");
            String docid = Arrays.asList(parts).stream()
                    .filter(x -> !x.equals("txt") && !x.equals("cmp") && !x.equals("mp") && !x.equals("xml"))
                    .collect(joining("."));

            String xml_text = FileUtils.readFileToString(f, "UTF-8");
            QueryDocument qdoc = new QueryDocument(docid);
            qdoc.setXmlText(xml_text);
            qdoc.setTextAnnotation(tokenizer.getTextAnnotation(qdoc.plain_text));
            qdoc.populateXmlMapping();
            qdoc.inc_end = false;

            qdoc.mentions = ms.stream().filter(x -> x.getDocID().equals(docid)).collect(Collectors.toList());
            if(qdoc.mentions.size()==0){
                System.out.println("no mention for doc: "+docid);
//                System.exit(-1);
            }

            for(ELMention m: qdoc.mentions){
                int start = m.getStartOffset();
                int end = m.getEndOffset();
                String surface = m.getMention();
                if(!qdoc.xml_text.substring(start, end).replaceAll("\\s+","").equals(surface.replaceAll("\\s+","")))
                    System.out.println(surface+" || "+qdoc.xml_text.substring(start, end));

                m.setMention(qdoc.xml_text.substring(start, end));

            }

            ret.add(qdoc);
        }

        System.out.println("Read "+ret.size()+" docs "+ms.size()+" mentions");

//        TACReader.propogateLabel(ret,4);

        return ret;
    }

    private void printNumMentions(List<QueryDocument> docs){
//        int n = (int) docs.stream().flatMap(x -> x.mentions.stream()).filter(x -> x.noun_type.equals("NAM")).count();
//        int n1 = (int) docs.stream().flatMap(x -> x.mentions.stream()).filter(x -> x.noun_type.equals("NOM")).count();
//        System.out.println("#NAM: "+n+" #NOM: "+n1);
    }



    public void writeConll(List<QueryDocument> docs, String outdir, String noun_type){

        try {
            if(noun_type.equals("NAM")) {
                for(QueryDocument doc: docs)
                    doc.mentions = doc.mentions.stream().filter(x -> x.noun_type.equals("NAM")).collect(Collectors.toList());
            }
            else if(noun_type.equals("NOM")){
                for(QueryDocument doc: docs)
                    doc.mentions = doc.mentions.stream().filter(x -> x.noun_type.equals("NOM")).collect(Collectors.toList());
            }
//            TACReader.propogateLabel(docs,2); // 2 for zh
            printNumMentions(docs);


            TACReader.resolveNested(docs);
            printNumMentions(docs);

            WriteConllFormat.writeDocs(docs, outdir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkOverlap(){
        List<QueryDocument> docs = readSpanishDocs();
        int over = 0;
        int total = 0;
        for(QueryDocument doc: docs){
            for(ELMention m: doc.mentions){
                if(!m.noun_type.equals("NOM")) continue;
                total++;
                for(ELMention m1: doc.mentions){
                    if(!m1.noun_type.equals("NAM")) continue;

                    if((m.getStartOffset() >= m1.getStartOffset() && m.getStartOffset() < m1.getEndOffset())
                            || (m1.getStartOffset() >= m.getStartOffset() && m1.getStartOffset() <m.getEndOffset())){
                        System.out.println(m.getMention()+" ||| "+m1.getMention());
                        over++;
                        break;
                    }
                }

            }
        }
        System.out.println(over);
        System.out.println(total);
    }


    public static void main(String[] args) {
        EREReader reader = new EREReader();
//        List<QueryDocument> docs = reader.readChineseDocs();
        reader.checkOverlap();


//        reader.writeConll(docs, "/shared/corpora/ner/ere/es/NAM-prop/", "NAM");
//        reader.writeConll(reader.readChineseDocs(), "/shared/corpora/ner/ere/zh/NOM-char-head1/", "NOM");
    }
}
