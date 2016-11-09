package edu.illinois.cs.cogcomp.xlwikifier.wikipedia;

import com.github.stuxuhai.jpinyin.ChineseHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 1/25/16.
 */
public class DumpReader {

    private static Logger logger = LoggerFactory.getLogger(DumpReader.class);
    public Map<String, String> id2redirect;
    public Map<String, String> title2id;
    public Map<String, String> id2title;
    public Map<String, String> id2en;

    public DumpReader() {

    }

    public void readId2En(String file, String target_lang) {
        logger.info("Reading lang links " + file);
        id2en = new HashMap<>();
        try {
            InputStream in = new GZIPInputStream(new FileInputStream(file));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();
            while (line != null) {
                if (!line.contains("INSERT INTO")) {
                    line = br.readLine();
                    continue;
                }
                int start = line.indexOf("(");
                line = line.substring(start + 1, line.length());
                String[] tokens = line.split("\\),\\(");
                for (String t : tokens) {
                    String[] ts = t.split("'");
                    if (ts.length < 4) continue;
                    String id = ts[0].substring(0, ts[0].length() - 1);
                    String lang = ts[1];
                    String en = ts[3];
                    en = en.replaceAll("\\\\", "");
                    if (lang.equals(target_lang))
                        id2en.put(id, en);
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("id2en size " + id2en.size());
    }

    public void readRedirects(String file, String lang) {
        logger.info("Reading redirects " + file);
        id2redirect = new HashMap<>();
        try {
            InputStream in = new GZIPInputStream(new FileInputStream(file));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();
            while (line != null) {
                if (line.contains("INSERT INTO")) {
                    int start = line.indexOf("(");
                    line = line.substring(start + 1, line.length());
                    String[] tokens = line.split("\\),\\(");
                    for (String t : tokens) {
                        String[] ts = t.split("',");
                        String[] cs = ts[0].split(",");
                        if (cs.length < 3) continue;
                        if (cs[2].length() < 2) continue;
                        String id = cs[0];
                        List<String> cst = Arrays.asList(cs);
                        String tmp = cst.subList(2, cst.size()).stream().collect(joining(","));

                        String title = tmp.substring(1).toLowerCase();
                        title = title.replaceAll("\\\\", "");
                        if(lang.equals("zh"))
                            title = ChineseHelper.convertToSimplifiedChinese(title);

                        id2redirect.put(id, title);
                    }
                }
                line = br.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void readTitle2ID(String file, String lang) {
        logger.info("Reading titles " + file);
        title2id = new HashMap<>();
        id2title = new HashMap<>();

        try {
            InputStream in = new GZIPInputStream(new FileInputStream(file));
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();
            while (line != null) {
                if (line.contains("INSERT INTO")) {
                    int start = line.indexOf("(");
                    line = line.substring(start + 1, line.length());
                    String[] tokens = line.split("\\),\\(");
                    for (String t : tokens) {
                        String[] ts = t.split("',");
                        String[] cs = ts[0].split(",");
                        if (cs[1].equals("0")) {
                            if (cs.length < 3) continue;
                            if (cs[2].length() < 2) continue;

                            List<String> cst = Arrays.asList(cs);
                            String tmp = cst.subList(2, cst.size()).stream().collect(joining(","));

                            String id = cs[0];
                            String title = tmp.substring(1).toLowerCase();
                            title = title.replaceAll("\\\\", "");
                            if(lang.equals("zh"))
                                title = ChineseHelper.convertToSimplifiedChinese(title);
                            title2id.put(title, id);
                            id2title.put(id, title);
                        }
                    }
                }
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        logger.info("#titles:" + title2id.size());
    }

    public static void main(String[] args) {
//        DumpReader dr = new DumpReader();
//        dr.readId2En("/shared/bronte/ctsai12/multilingual/wikidump/es/eswiki-20151002-langlinks.sql.gz");


    }
}
