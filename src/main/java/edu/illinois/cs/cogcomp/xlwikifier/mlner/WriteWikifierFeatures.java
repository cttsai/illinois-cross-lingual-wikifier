package edu.illinois.cs.cogcomp.xlwikifier.mlner;

import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.core.utilities.configuration.ResourceManager;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.ner.LbjTagger.Data;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NERDocument;
import edu.illinois.cs.cogcomp.ner.LbjTagger.NEWord;
import edu.illinois.cs.cogcomp.ner.LbjTagger.ParametersForLbjCode;
import edu.illinois.cs.cogcomp.ner.config.NerBaseConfigurator;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static edu.illinois.cs.cogcomp.ner.LbjTagger.Parameters.readAndLoadConfig;

/**
 * Created by ctsai12 on 3/29/17.
 */
public class WriteWikifierFeatures {

    private static final Logger logger = LoggerFactory.getLogger(ModelTrainer.class);

    public static void run(String in_dir, String lang, String out_dir){
        try {
            NerBaseConfigurator baseConfigurator = new NerBaseConfigurator();
            ResourceManager ner_rm = new ResourceManager(ConfigParameters.ner_models.get(lang));
            ParametersForLbjCode.currentParameters = readAndLoadConfig(baseConfigurator.getConfig(ner_rm), true);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        Data data = null;
        try {
            data = new Data(in_dir, in_dir, "-c", new String[]{}, new String[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }
        NERUtils nerutils = new NERUtils(lang);
        List<QueryDocument> docs = ModelTrainer.data2QueryDocs(data);
        logger.info("Wikifying training documents");
        int cnt = 0;
        for (QueryDocument doc : docs) {
            System.out.print((cnt++)+"\r");
            nerutils.wikifyNgrams(doc);
        }
        ModelTrainer.copyWikifierFeatures(data, docs);

        writeCoNLLFormat(data, out_dir);
    }
    public static void writeCoNLLFormat(Data data, String out_dir){


        int ent_cnt = 0;
        for(NERDocument doc: data.documents){

            String out = "";
            for(LinkedVector sent: doc.sentences){

                for(int i = 0; i < sent.size(); i++){
                    NEWord word = (NEWord) sent.get(i);
                    out += word.neLabel+"\tx\tx\tx\tx\t"+word.form+"\tx\tx\tx\tx";
                    for(String feat: word.wikifierFeatures) {
//                        System.out.println(feat);
                        out += "\t" + feat;
                    }
                    out += "\n";
                }
                out += "\n";
            }
            try {
                FileUtils.writeStringToFile(new File(out_dir, doc.docname), out, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("# entities "+ent_cnt);
    }

    public static void main(String[] args) {
        try {
            ConfigParameters.setPropValues("config/xlwikifier-demo.config");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        String indir = "/shared/corpora/ner/human/ta/conll";
//        indir = "/shared/corpora/ner/conll2003/eng/eng.all.nomisc";
//        indir = "/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/zh/nw.xinhua-char";
        String lang = "ta";
        String outdir = "/shared/corpora/ner/human/ta/conll.wiki2";
//        outdir = "/shared/corpora/ner/conll2003/eng/eng.all.nomisc.wiki";
//        outdir = "/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/zh/nw.xinhua-char.wiki";

        run(indir, lang, outdir);
    }
}
