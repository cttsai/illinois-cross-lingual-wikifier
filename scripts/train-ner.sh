#!/bin/sh

TRAINLANG=zh
TESTLANG=zh

#TRAIN="/shared/corpora/ner/wikifier-features/es/tac-traintest+ere"
#TRAIN="/shared/corpora/ner/wikifier-features/en/train-camera3-misc"
#TRAIN="/shared/experiments/ctsai12/workspace/xlwikifier-demo/xlwikifier-data/ner-data/he/conll"
#TRAIN="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/en/tac2015+NAM.all.old" #<- yeilds best en model
#TRAIN="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/en/tac2015.train+NAM.all.old"
#TRAIN="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/zh/tac2015.train+ere"
TRAIN="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/zh/tac15train16test"
#TRAIN="/shared/corpora/ner/tac/en/2016eval-df"
#TRAIN="/shared/corpora/ccgPapersData/NER/Data/GoldData/Reuters/ColumnFormatDocumentsSplit/TrainPlusDev/"
#TRAIN="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/"$TRAINLANG"/tac+ere"
#TRAIN="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/"$LANG"/lorelei-train"
#TRAIN="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/zh/tac+ere+nwbntcwb.ex"
#TRAIN="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/zh/tac+ere"
#TRAIN="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/zh/tac2015-all"

#TEST="/shared/corpora/ner/wikifier-features/es/tac2015-test12-prop"
#TEST="/shared/corpora/ner/wikifier-features/zh/tac2015-test12-char-prop"
#TEST="/shared/corpora/ccgPapersData/NER/Data/GoldData/Reuters/ColumnFormatDocumentsSplit/Test"
#TEST="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/"$LANG"/tac2015-test"
#TEST="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/"$TESTLANG"/lorelei-test"
#TEST="/shared/experiments/ctsai12/workspace/xlwikifier-demo/xlwikifier-data/ner-data/he/test"
#TEST="/shared/corpora/ner/lorelei1/ar/column-notweets"
#TEST="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/en/tac2016.eval"
#TEST="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/es/tac2015-test1"
TEST="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/zh/tac2015-test"
#TEST="/shared/corpora/corporaWeb/deft/eng/LDC2016E31_DEFT_Rich_ERE_English_Training_Annotation_R3/conll-format-ner-bio"

configFile=$1

cpath="target/classes:target/dependency/*:config"

java -classpath  ${cpath} -Xmx60g edu.illinois.cs.cogcomp.xlwikifier.mlner.ModelTrainer train $TRAIN $TEST $TRAINLANG $configFile



