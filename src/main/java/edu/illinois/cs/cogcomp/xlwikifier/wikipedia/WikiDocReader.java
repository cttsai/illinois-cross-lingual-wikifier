package edu.illinois.cs.cogcomp.xlwikifier.wikipedia;

import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.io.LineIO;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 8/31/15.
 */
public class WikiDocReader {

    public Map<String, String> title_map;
    public Tokenizer tokenizer;
    private static Logger logger = LoggerFactory.getLogger(WikiDocReader.class);

    public WikiDocReader() {
    }

    public WikiDocReader(String lang) {
        tokenizer = MultiLingualTokenizer.getTokenizer(lang);
    }

    /**
     * Read all files under the input dir
     *
     * @param dir
     * @return doc id to line list
     */
    public Map<String, List<String>> readDocs(String dir) {
        Map<String, List<String>> ret = new HashMap<>();
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(new File(dir).toPath());
            for (Path p : stream) {
                String[] tokens = p.toString().split("/");
                String id = tokens[tokens.length - 1].split("\\.")[0];
                if (id.trim().isEmpty()) continue;
                List<String> lines = LineIO.read(p.toString());
                ret.put(id, lines);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public QueryDocument readWikiDocSingle(String lang, String filename, boolean check) {
        String dir = ConfigParameters.dump_path + lang + "/" + lang + "_wiki_view/";
        List<String> lines = null;
        TextAnnotation ta = null;
        try {
            lines = LineIO.read(dir + "annotation/" + filename);
            String plain = FileUtils.readFileToString(new File(dir + "plain/" + filename), "UTF-8");
            ta = tokenizer.getTextAnnotation(plain);
            if (ta == null)
                return null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<ELMention> mentions = new ArrayList<>();
        for (String line : lines) {
            if (!line.startsWith("#")) {
                String[] sp = line.split("\t");
                if (sp.length < 3) continue;
                try {
                    Integer.parseInt(sp[1]);
                    Integer.parseInt(sp[2]);
                } catch (Exception e) {
                    continue;
                }
            } else {
                String[] sp = line.substring(1).split("\t");
                if (sp.length < 3) continue;
                if (sp[0].trim().isEmpty()) continue;
                ELMention m;
                try {
                    m = new ELMention(filename, Integer.parseInt(sp[1]), Integer.parseInt(sp[2]));
                } catch (Exception e) {
                    continue;
                }
                m.gold_wiki_title = sp[0];
                m.gold_lang = lang;
                mentions.add(m);
            }
        }

        for (ELMention m : mentions) {
            String surface;
            // check if there is an error regarding token/character offsets
            try {
                int start_idx = ta.getTokenIdFromCharacterOffset(m.getStartOffset());
                int end_idx = ta.getTokenIdFromCharacterOffset(m.getEndOffset() - 1);
//                if (!lang.equals("zh"))
//                    surface = adoc.getSurface(m.getStartOffset(), m.getEndOffset());
//                else
//                    surface = adoc.getSurfaceNoSpace(m.getStartOffset(), m.getEndOffset());
                surface = ta.getText().substring(m.getStartOffset(), m.getEndOffset());
                surface = surface.replaceAll("\n", " ");


                if (surface == null || m.getStartOffset() < 0 ||
                        start_idx < 0 || end_idx < 0
                        || !surface.contains(ta.getToken(start_idx))
                        || !surface.contains(ta.getToken(end_idx))) {
                    if (check)
                        return null;
                    else
                        m.setSurface(null);
                } else {
                    m.setSurface(surface);
                }
            } catch (Exception e) {
                if (check)
                    return null;
                else
                    m.setSurface(null);
            }
        }
        mentions = mentions.stream().filter(x -> x.getSurface() != null && !x.getSurface().trim().isEmpty())
                .collect(Collectors.toList());
        QueryDocument doc = new QueryDocument(filename);
        doc.text = ta.getText();
        doc.mentions = mentions;
        doc.setTextAnnotation(ta);
        return doc;
    }

    public List<QueryDocument> readWikiDocsNew(String lang, int start, int end) {
        logger.info("Reading " + lang + " wikipedia docs...");
        List<String> paths = null;
        String dir = null;

        tokenizer = MultiLingualTokenizer.getTokenizer(lang);

        Set<String> badfile = new HashSet<>();

        try {
            dir = "/shared/preprocessed/ctsai12/multilingual/wikidump/" + lang + "/" + lang + "_wiki_view/";
//            dir = "/shared/preprocessed/ctsai12/multilingual/wikidump/"+lang+"/docs/";
            paths = LineIO.read(dir + "file.list.rand");
//            badfile.addAll(LineIO.read(dir+"bad.list"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        List<QueryDocument> docs = new ArrayList<>();
        int cnt = 0;
        int bad = 0;
        for (String filename : paths.subList(start, Math.min(paths.size(), end))) {
            if ((cnt++) % 100 == 0)
                System.out.print((cnt++) + "\r");
            if (badfile.contains(filename)) continue;

            QueryDocument doc = readWikiDocSingle(lang, filename, true);
            if (doc == null || doc.mentions.size() == 0) {
                bad++;
                badfile.add(filename);
                continue;
            }
            docs.add(doc);
        }
        System.out.println();
        logger.info("#bad " + bad);
        logger.info("#docs " + docs.size());
        logger.info("#mentions " + docs.stream().flatMap(x -> x.mentions.stream()).count());
        String badstr = badfile.stream().collect(joining("\n"));
        try {
            FileUtils.writeStringToFile(new File(dir + "bad.list"), badstr, "UTF-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return docs;
    }

    public static void main(String[] args) throws Exception {
        WikiDocReader r = new WikiDocReader();
        r.readWikiDocsNew("zh", 0, 5000);

    }
}
