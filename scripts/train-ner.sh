#!/bin/sh

TRAIN="/shared/corpora/ner/wikifier-features/es/tac-traintest+ere"
#TRAIN="/shared/corpora/ner/wikifier-features/zh/tac-traintest+ere"

TEST="/shared/corpora/ner/wikifier-features/es/tac2015-test12-prop"
#TEST="/shared/corpora/ner/wikifier-features/zh/tac2015-test12-char-prop"

LANG=es
configFile=$1

cpath="target/classes:target/dependency/*:config"

java -classpath  ${cpath} -Xmx60g edu.illinois.cs.cogcomp.xlwikifier.mlner.ModelTrainer $TRAIN $TEST $LANG $configFile



