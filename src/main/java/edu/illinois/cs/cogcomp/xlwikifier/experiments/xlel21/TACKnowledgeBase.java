package edu.illinois.cs.cogcomp.xlwikifier.experiments.xlel21;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlRootElement;

import edu.illinois.cs.cogcomp.core.utilities.XmlModel;
import org.apache.commons.lang.StringEscapeUtils;

/**
 * Created by ctsai12 on 1/22/16.
 */
@XmlRootElement
public class TACKnowledgeBase implements Iterable<TACKnowledgeBase.Entry>{

    public static enum EntryType{
        PER,
        ORG,
        GPE{
            @Override
            public String NERType() {
                return "LOC";
            }
        },
        UKN {
            @Override
            public String NERType() {
                return "MISC";
            }
        };

        public String NERType(){
            return name();
        }
    }

    public static class Entry{
        @XmlAttribute
        public String title;
        @XmlAttribute
        public EntryType type;
        @XmlAttribute
        public String id;
        @Override
        public String toString() {
            return "Entry [title=" + title + ", type=" + type + ", id=" + id + "]";
        }

    }

    @XmlElement(name="entry")
    public List<Entry> entries = new ArrayList<>();

    @XmlTransient
    private Map<String,Entry> idIndex = null;
    @XmlTransient
    private Map<String,Entry> titleIndex = null;

    private void ensureIndex() {
        if (idIndex == null) {
            idIndex = new HashMap<>();
            titleIndex = new HashMap<>();
            for (Entry e : entries) {
//                e.title = TitleNormalizer.normalize(e.title);
                idIndex.put(e.id, e);
                titleIndex.put(e.title.toLowerCase(), e);
            }
            System.out.println("#id index:"+idIndex.size());
            System.out.println("#title index:"+titleIndex.size());
        }
    }

    public Entry getEntryById(String id){
        synchronized (this){
            ensureIndex();
        }
        return idIndex.get(id);
    }

    public String getTitleById(String id){
        Entry e = getEntryById(id);
        return e == null ? null : e.title;
    }

    public Entry getEntryByTitle(String title){
        ensureIndex();
//        String normalizedTitle = TitleNormalizer.normalize(title);
        return titleIndex.get(title);
    }

    public String getIdByTitle(String title){
        Entry e = getEntryByTitle(title);
        return e == null ? null : e.id;
    }

    public String getTypeByTitle(String title){
        ensureIndex();
        Entry e = getEntryByTitle(title);
        if (e != null)
            return e.type.NERType();
        return null;
    }

    @Override
    public String toString() {
        return "TACKnowledgeBase of size " + entries.size();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entries == null) ? 0 : entries.hashCode());
        result = prime * result + ((idIndex == null) ? 0 : idIndex.hashCode());
        result = prime * result + ((titleIndex == null) ? 0 : titleIndex.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TACKnowledgeBase other = (TACKnowledgeBase) obj;
        if (entries == null) {
            if (other.entries != null)
                return false;
        } else if (!entries.equals(other.entries))
            return false;
        if (idIndex == null) {
            if (other.idIndex != null)
                return false;
        } else if (!idIndex.equals(other.idIndex))
            return false;
        if (titleIndex == null) {
            if (other.titleIndex != null)
                return false;
        } else if (!titleIndex.equals(other.titleIndex))
            return false;
        return true;
    }


    public static TACKnowledgeBase defaultInstance(){
//        return XmlModel.load(TACKnowledgeBase.class, "data/TAC_ProblemsAndTextData/TACKB.xml");
        return XmlModel.load(TACKnowledgeBase.class, "/shared/preprocessed/ctsai12/multilingual/TAC.KB.new");
    }

    public void saveKB(){

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("data/TAC.KB.new"));
            bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
            bw.write("<tacKnowledgeBase>\n");
            for(Entry en: this){
                bw.write("<entry title=\""+ StringEscapeUtils.escapeXml(en.title)+"\" type=\""+en.type+"\" id=\""+en.id+"\"/>\n");
            }
            bw.write("</tacKnowledgeBase>\n");
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public Iterator<Entry> iterator() {
        return entries.iterator();
    }



}

