package edu.illinois.cs.cogcomp.mlner;

import edu.illinois.cs.cogcomp.LbjNer.ExpressiveFeatures.ExpressiveFeaturesAnnotator;
import edu.illinois.cs.cogcomp.LbjNer.InferenceMethods.Decoder;
import edu.illinois.cs.cogcomp.LbjNer.LbjFeatures.NETaggerLevel1;
import edu.illinois.cs.cogcomp.LbjNer.LbjFeatures.NETaggerLevel2;
import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.*;
import edu.illinois.cs.cogcomp.LbjNer.LbjTagger.Parameters;
import edu.illinois.cs.cogcomp.LbjNer.ParsingProcessingData.TaggedDataReader;
import edu.illinois.cs.cogcomp.core.datastructures.textannotation.TextAnnotation;
import edu.illinois.cs.cogcomp.lbjava.nlp.Word;
import edu.illinois.cs.cogcomp.lbjava.parse.LinkedVector;
import edu.illinois.cs.cogcomp.mlner.core.NERUtils;
import edu.illinois.cs.cogcomp.tokenizers.CharacterTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.MultiLingualTokenizer;
import edu.illinois.cs.cogcomp.tokenizers.Tokenizer;
import edu.illinois.cs.cogcomp.xlwikifier.ConfigParameters;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.freebase.FreeBaseQuery;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by ctsai12 on 4/24/16.
 */
public class CrossLingualNER {

    private static String lang;
    public static Tokenizer tokenizer;
    private static NERUtils utils;
    public static boolean transfer = true;

    private static NETaggerLevel1 taggerLevel1;
    private static NETaggerLevel2 taggerLevel2;

    private static Logger logger = LoggerFactory.getLogger(CrossLingualNER.class);

    public static void init(String l, boolean trans){
        if(!l.equals(lang) || trans != transfer) {
            ConfigParameters.setPropValues();

            if(!FreeBaseQuery.isloaded())
                FreeBaseQuery.loadDB(true);

            transfer = trans;
            lang = l;
            logger.info("Setting lang in xlner: " + lang);

            if(lang.equals("zh"))
                tokenizer = new CharacterTokenizer();
            else
                tokenizer = MultiLingualTokenizer.getTokenizer(lang);

            utils = new NERUtils(lang);
            try {
                if(ConfigParameters.ner_models.containsKey(lang))
                    Parameters.readConfigAndLoadExternalData(ConfigParameters.ner_models.get(lang), false);
                else
                    logger.error("No NER model set for "+lang);
            } catch (IOException e) {
                e.printStackTrace();
            }

            taggerLevel1 = new NETaggerLevel1(ParametersForLbjCode.currentParameters.pathToModelFile + ".level1", ParametersForLbjCode.currentParameters.pathToModelFile + ".level1.lex");
            taggerLevel2 = new NETaggerLevel2(ParametersForLbjCode.currentParameters.pathToModelFile + ".level2", ParametersForLbjCode.currentParameters.pathToModelFile + ".level2.lex");
        }
    }

    public static Data doc2NERData(QueryDocument doc){
        logger.info("Converting document datastructure...");

        Vector<Vector<String>> tokens = new Vector<>();
        TextAnnotation ta = doc.getTextAnnotation();
        tokens.add(new Vector<>());
        Vector<LinkedVector> sentences =new Vector<>();
        int psen = -1;
        sentences.add(new LinkedVector());
        for(ELMention m: doc.mentions){
            int tokenid = ta.getTokenIdFromCharacterOffset(m.getStartOffset());
            int senid = ta.getSentenceId(tokenid);
            if(senid != psen){
                sentences.add(new LinkedVector());
                psen = senid;
            }
            NEWord word=new NEWord(new Word(m.getMention()),null,"unlabeled");
            word.char_start = m.getStartOffset();
            word.char_end = m.getEndOffset();
            String[] wikif = new String[m.ner_features.size()];
            int i = 0;
            for(String key: m.ner_features.keySet()){
                wikif[i++] = key+":"+m.ner_features.get(key);
            }
            word.wikifierfeats = wikif;
            NEWord.addTokenToSentence(sentences.lastElement(), word);
        }
        TaggedDataReader.connectSentenceBoundaries(sentences);
        NERDocument nerdoc = new NERDocument(sentences, "consoleInput");
        return new Data(nerdoc);
    }

    public static List<ELMention> extractPredictions(NERDocument nerdoc, String text){
        List<ELMention> ret = new ArrayList<>();

        for(LinkedVector sen: nerdoc.sentences){
            int start = -1, end = -1;
            String pretype = "O";
            for(int j = 0; j < sen.size(); j++){
                NEWord w = (NEWord) sen.get(j);
                if(w.neTypeLevel2.contains("-")){   // a NE token
                    String type = w.neTypeLevel2.substring(2);
                    if(!type.equals(pretype)){  // type is different from the previous token
                        if(start != -1){
                            ELMention m = new ELMention("", start, end);
                            m.setMention(text.substring(start, end));
                            m.setType(pretype);
                            ret.add(m);
                        }
                        start = w.char_start;
                        end = w.char_end;
                        pretype = type;
                    }
                    else{   // extend the mention
                        end=w.char_end;
                    }
                }
                else{
                    if(start != -1){
                        ELMention m = new ELMention("", start, end);
                        m.setMention(text.substring(start, end));
                        m.setType(pretype);
                        ret.add(m);
                        start = -1;
                        end = -1;
                        pretype = "O";
                    }
                }
            }
        }
        return ret;
    }

    public static NERDocument annotate(QueryDocument doc){

        // doc.mentions stores tokens
        if(ParametersForLbjCode.currentParameters.featuresToUse.containsKey("Wikifier"))
            utils.wikifyNgrams(doc);
        else{
            doc.mentions = utils.getNgramMentions(doc, 1);
        }

        Data nerdata = doc2NERData(doc);
        ExpressiveFeaturesAnnotator.train = false;
        try {
            ExpressiveFeaturesAnnotator.annotate(nerdata);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Vector<Data> data = new Vector<>();
        data.addElement(nerdata);

        if (ParametersForLbjCode.currentParameters.labelsToIgnoreInEvaluation != null)
            data.elementAt(0).setLabelsToIgnore(ParametersForLbjCode.currentParameters.labelsToIgnoreInEvaluation);
        if (ParametersForLbjCode.currentParameters.labelsToAnonymizeInEvaluation != null)
            data.elementAt(0).setLabelsToAnonymize(ParametersForLbjCode.currentParameters.labelsToAnonymizeInEvaluation);

        try {
            Decoder.annotateDataBIO(data.elementAt(0), taggerLevel1, taggerLevel2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data.elementAt(0).documents.get(0);
    }

    public static QueryDocument annotate(String text){
        logger.info("Running multilingual NER...");

        if(lang == null){
            logger.error("lang not set!");
            return null;
        }

        QueryDocument doc = new QueryDocument("");
        doc.plain_text = text;
        TextAnnotation ta = tokenizer.getTextAnnotation(text);
        doc.setTextAnnotation(ta);
        NERDocument nerdoc = annotate(doc);
        List<ELMention> mentions = extractPredictions(nerdoc, doc.plain_text);
        doc.mentions = mentions;
        return doc;
    }

    public static void main(String[] args) {
        CrossLingualNER.init("zh", false);
//        String text = "Barack Hussein Obama II3 es el cuadragésimo cuarto y actual presidente de los Estados Unidos de América. Fue senador por el estado de Illinois desde el 3 de enero de 2005 hasta su renuncia el 16 de noviembre de 2008. Además, es el quinto legislador afroamericano en el Senado de los Estados Unidos, tercero desde la era de reconstrucción. También fue el primer candidato afroamericano nominado a la presidencia por el Partido Demócrata y es el primero en ejercer el cargo presidencial.";
        String text = "巴拉克·歐巴馬是美國民主黨籍政治家，也是第44任美國總統，於2008年初次當選，並於2012年成功連任。歐巴馬是第一位非裔美國總統。他1961年出生於美國夏威夷州檀香山，童年和青少年時期分別在印尼和夏威夷度過。1991年，歐巴馬以優等生榮譽從哈佛法學院畢業。1996年開始參選公職，在補選中，當選伊利諾州參議員。";
        QueryDocument doc = CrossLingualNER.annotate(text); // from DF_FTR_TUR_0514802_20140900
        doc.mentions.forEach(x -> System.out.println(x.getMention()+" "+x.getType()));
    }
}
