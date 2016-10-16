package edu.illinois.cs.cogcomp.xlwikifier.evaluation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by lchen112 on 10/16/16.
 */
public class GoldDocument {
    public ArrayList<GoldMention> goldMentions = new ArrayList<GoldMention>();
    public String plain_text;
    public String id;

    public GoldDocument(File documentFile) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(documentFile);
        document.getDocumentElement().normalize();
        this.plain_text = ((Element) document.getElementsByTagName("text").item(0)).getTextContent();
        this.id = ((Element) document.getElementsByTagName("id").item(0)).getTextContent();
        NodeList nList = document.getElementsByTagName("reference");
        for (int temp = 0; temp < nList.getLength(); temp++) {
            Node node = nList.item(temp);
            Element eElement = (Element) node;
            String surface = eElement.getElementsByTagName("surface").item(0).getTextContent();
            int charStart = Integer.parseInt(eElement.getElementsByTagName("charStart").item(0).getTextContent());
            int charLength = Integer.parseInt(eElement.getElementsByTagName("charLength").item(0).getTextContent());
            int charEnd = charStart + charLength;
            String annotation = eElement.getElementsByTagName("annotation").item(0).getTextContent();
            goldMentions.add(new GoldMention(surface, charStart, charEnd, annotation));
        }
    }

}
