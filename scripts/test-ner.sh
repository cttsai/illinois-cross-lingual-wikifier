#!/bin/sh

TRAINLANG=en
TESTLANG=bn


#TEST="/shared/corpora/ner/wikifier-features/es/tac2015-test12-prop"
#TEST="/shared/corpora/ner/wikifier-features/zh/tac2015-test12-char-prop"
#TEST="/shared/corpora/ccgPapersData/NER/Data/GoldData/Reuters/ColumnFormatDocumentsSplit/Test"
#TEST="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/"$LANG"/tac2015-test"
TEST="/shared/preprocessed/ctsai12/multilingual/xlwikifier-data/ner-data/"$TESTLANG"/lorelei-test"

configFile=$1

cpath="target/classes:target/dependency/*:config"

java -classpath  ${cpath} -Xmx60g edu.illinois.cs.cogcomp.xlwikifier.mlner.ModelTrainer "test" $TEST $TRAINLANG $TESTLANG $configFile



