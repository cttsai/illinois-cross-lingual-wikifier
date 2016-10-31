#!/bin/sh

TRAIN="/shared/corpora/ner/wikifier-features/es/tac-traintest+ere"
#TRAIN="/shared/corpora/ner/wikifier-features/zh/tactrain+ere"

TEST="/shared/corpora/ner/wikifier-features/es/tac2015-test12-prop"
#TEST="/shared/corpora/ner/wikifier-features/zh/tac2015-test12-char-prop"

configFile=$1

cpath="target/classes:target/dependency/*:config"

java -classpath  ${cpath} -Xmx60g edu.illinois.cs.cogcomp.LbjNer.LbjTagger.NerTagger -train $TRAIN -test $TEST -c $configFile



