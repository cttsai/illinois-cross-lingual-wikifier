package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 4/12/17.
 */
public class EREReader {


    public static List<ELMention> readAnnotations(String label_dir) throws ParserConfigurationException, IOException, SAXException {
        List<ELMention> ms = new ArrayList<>();

        for(File f: new File(label_dir).listFiles()) {

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(f);
            doc.getDocumentElement().normalize();

            Set<String> seen = new HashSet<>();

            NodeList nodes = doc.getElementsByTagName("entity");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element entity = (Element) nodes.item(i);
                String ent_type = entity.getAttribute("type");
                String specificity = entity.getAttribute("specificity");
                if(!specificity.equals("specific")) continue;
                NodeList mentions = entity.getElementsByTagName("entity_mention");
                for (int j = 0; j < mentions.getLength(); j++) {
                    Element mention = (Element) mentions.item(j);
                    String noun_type = mention.getAttribute("noun_type");

                    if (!noun_type.equals("NAM") && !noun_type.equals("NOM")) continue;

                    String start_off = mention.getAttribute("offset");
                    String length = mention.getAttribute("length");
                    String docid = mention.getAttribute("source");

//                    System.out.println(docid+" "+start_off);
                    Node text = mention.getElementsByTagName("mention_text").item(0);

                    String surface;
                    if (text != null)
                        surface = text.getTextContent();
                    else
                        surface = mention.getTextContent();

                    if (noun_type.equals("NOM")) {
                        Node element = mention.getElementsByTagName("nom_head").item(0);
                        surface = element.getTextContent();
                        start_off = ((Element) element).getAttribute("offset");
                        length = ((Element) element).getAttribute("length");
                    }

                    int start = Integer.parseInt(start_off);
                    int end = start + Integer.parseInt(length);

                    if (seen.contains(start + "_" + end)) continue;
                    seen.add(start + "_" + end);

                    ELMention m = new ELMention(docid, start, end);
                    m.setSurface(surface);
                    m.setType(ent_type);
                    m.noun_type = noun_type;
                    ms.add(m);
                }
            }
        }

        System.out.println("#gold mentions "+ms.size());
        return ms;
    }

    public static List<QueryDocument> read(String source_dir, String label_dir, String lang) {
        List<QueryDocument> ret = new ArrayList<>();

        List<ELMention> golds = null;
        try {
            golds = readAnnotations(label_dir);
        } catch (Exception e) {
            e.printStackTrace();
        }

        TACDataReader reader = new TACDataReader(true);

        List<QueryDocument> docs = null;
        try {
            docs = reader.readDocs(source_dir, lang);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(QueryDocument doc: docs){

            String[] parts = doc.getDocID().split("\\.");
            String docid = Arrays.asList(parts).stream()
                    .filter(x -> !x.equals("txt") && !x.equals("cmp") && !x.equals("mp") && !x.equals("xml") && !x.equals("mpdf"))
                    .collect(joining("."));
            List<ELMention> docms = golds.stream().filter(x -> x.getDocID().equals(docid))
                    .filter(x -> x.noun_type.equals("NOM"))
                    .collect(Collectors.toList());

//            List<ELMention> mentions = new ArrayList<>();
//            for(ELMention m: docms){
//                Pair<Integer, Integer> offs = doc.getXmlhandler().getPlainOffsets(m.getStartOffset(), m.getEndOffset());
//                if(offs == null) continue;
//                m.setStartOffset(offs.getFirst());
//                m.setEndOffset(offs.getSecond());
//                mentions.add(m);
//            }
            doc.mentions = docms;
            ret.add(doc);
        }


        return ret;
    }

    public static void main(String[] args) {
        String ere_dir = "/shared/corpora/corporaWeb/deft/eng/LDC2016E31_DEFT_Rich_ERE_English_Training_Annotation_R3/data/ere";
        String src_dir = "/shared/corpora/corporaWeb/deft/eng/LDC2016E31_DEFT_Rich_ERE_English_Training_Annotation_R3/data/source";
        src_dir = "/shared/corpora/corporaWeb/deft/eng/LDC2015E68_DEFT_Rich_ERE_English_Training_Annotation_R2_V2/data/source";
        ere_dir = "/shared/corpora/corporaWeb/deft/eng/LDC2015E68_DEFT_Rich_ERE_English_Training_Annotation_R2_V2/data/ere";
        src_dir = "/shared/corpora/corporaWeb/deft/eng/LDC2015E29_DEFT_Rich_ERE_English_Training_Annotation_V2/data/source/cmptxt";
        ere_dir = "/shared/corpora/corporaWeb/deft/eng/LDC2015E29_DEFT_Rich_ERE_English_Training_Annotation_V2/data/ere/cmptxt";
        ere_dir = "/shared/corpora/corporaWeb/tac/2016/ere-all/es-test/ere";
        src_dir = "/shared/corpora/corporaWeb/tac/2016/ere-all/es-test/source";
        String lang = "es";

        List<QueryDocument> docs = EREReader.read(src_dir, ere_dir, lang);

        String out_dir = "/shared/corpora/ner/nominal_exp/es.ere.NOM.spe";

        TACDataReader.writeCoNLLFormat(docs, docs.stream().flatMap(x -> x.mentions.stream()).collect(Collectors.toList()), out_dir);

    }
}
