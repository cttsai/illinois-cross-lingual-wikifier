package edu.illinois.cs.cogcomp.xlwikifier;

/**
 * Created by ctsai12 on 8/31/15.
 */
public class Constants {

    public static final String trainingDir2015 = "/shared/corpora/corporaWeb/tac/LDC2015E75_TAC_KBP_2015_Tri-Lingual_Entity_Discovery_and_Linking_Training_Data/";
//    public static final String trainingQueries2015 = trainingDir2015 + "data/tac_kbp_2015_tedl_training_gold_standard_entity_mentions.tab";
    public static final String trainingQueries2015 = "/shared/corpora/corporaWeb/tac/LDC2015E75_TAC_KBP_2015_Tri-Lingual_Entity_Discovery_and_Linking_Training_Data_V2.0/data/tac_kbp_2015_tedl_training_gold_standard_entity_mentions.tab";
    public static final String testingQueries = "/shared/corpora/corporaWeb/tac/LDC2015E103_TAC_KBP_2015_Tri-Lingual_Entity_Discovery_and_Linking_Evaluation_Gold_Standard_Entity_Mentions_and_Knowledge_Base_Links/data/tac_kbp_2015_tedl_evaluation_gold_standard_entity_mentions.tab";

    public static final String trainingSpaRoot = trainingDir2015 + "data/source_docs/spa/";
    public static final String trainingDocSpaXml = trainingSpaRoot+ "xml/";
    public static final String trainingAnnoDocSpaXml = trainingSpaRoot+ "annotate_xml/";
    public static final String trainingDocSpaPlain = trainingSpaRoot + "plain/";
    public static final String trainingDocSpaPlainJson = trainingSpaRoot + "plain_json/";
    public static final String trainingDocTranslatedSpa = trainingSpaRoot + "translated_plain/";

    public static final String trainingCmnRoot = trainingDir2015 + "data/source_docs/cmn/";
    public static final String trainingDocCmnXml = trainingCmnRoot+ "xml/";



    public static final String testDir2015 = "/shared/corpora/corporaWeb/tac/LDC2015E93_TAC_KBP_2015_Tri-Lingual_Entity_Discovery_and_Linking_Evaluation_Source_Corpus/";
    public static final String testSpaRoot = testDir2015 + "data/spa/";
    public static final String testAnnoDocSpaXml = testSpaRoot+ "annotate_xml/";
    public static final String testDocSpaPlainJson = testSpaRoot + "plain_json/";
    public static final String testDocTranslatedSpa = testSpaRoot + "translated_plain/";
    public static final String testDocSpaXml = testSpaRoot+ "xml/";

    public static final String testCmnRoot = testDir2015+"data/cmn/";
    public static final String testDocCmnXml = testCmnRoot + "xml/";
    public static final String testDocCmnTranslated = testCmnRoot + "translated_plain/";



    public static final String trainingEngRoot = trainingDir2015 + "data/source_docs/eng/";
    public static final String trainingAnnoDocEngXml = trainingEngRoot+ "annotate_xml/";
    public static final String trainingDocEngPlainJson = trainingEngRoot + "plain_json/";
    public static final String trainingDocEngPlain = trainingEngRoot + "plain/";
    public static final String trainingDocEngXml = trainingEngRoot+ "xml/";

    public static final String testEngRoot = testDir2015 + "data/eng/";
    public static final String testAnnoDocEngXml = testEngRoot+ "annotate_xml/";
    public static final String testDocEngPlainJson = testEngRoot + "plain_json/";
    public static final String testDocEngPlain = testEngRoot + "plain/";
    public static final String testDocEngXml = testEngRoot+ "xml/";

    // curator
    public static String curatorMachine = "trollope.cs.illinois.edu";
    public static int curatorPort = 9010;
    public static String pathToAnnotationCache = "/shared/bronte/tac2015/CuratorAnnotationCache";


    public static final String dbpath = "/shared/bronte/ctsai12/multilingual/mapdb";
    public static String dbpath1 = "/shared/preprocessed/ctsai12/multilingual/mapdb-new";
    public static String rootdir ="/shared/preprocessed/ctsai12/multilingual/";
    public static String wikidumpdir = rootdir+"wikidump/";

//    public static final String dbpath = "/scratch/ctsai12/mapdb";

    // for TAC 2014
    public static final String TAC2014_eval_path = "/shared/corpora/corporaWeb/tac/2014/LDC2014E81_TAC_2014_KBP_English_Entity_Discovery_and_Linking_Evaluation_Queries_and_Knowledge_Base_Links_V2.0/";
    public static final String TAC2014_train_path = "/shared/corpora/corporaWeb/tac/2014/LDC2014E54_TAC_2014_KBP_English_Entity_Discovery_and_Linking_Training_Data_V1.2/";

    public static final String TAC2014_train_mention_xml = TAC2014_train_path+"/data/tac_2014_kbp_english_EDL_training_queries.xml";
    public static final String TAC2014_train_mention_tab = TAC2014_train_path+"/data/tac_2014_kbp_english_EDL_training_KB_links.tab";

    public static final String TAC2014_eval_mention_xml = TAC2014_eval_path+"/data/tac_2014_kbp_english_EDL_evaluation_queries.xml";
    public static final String TAC2014_eval_mention_tab = TAC2014_eval_path+"/data/tac_2014_kbp_english_EDL_evaluation_KB_links.tab";

    public static final String src_doc_dir = "/shared/bronte/tac2014/xmlTextCache2014_test/";
}
