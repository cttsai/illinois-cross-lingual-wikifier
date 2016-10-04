#!/bin/sh

TRAIN="/shared/corpora/ner/wikifier-features/en/train-camera3"

TEST="/shared/corpora/ner/wikifier-features/en/test-camera3"

configFile="config/ner-example.config"

cpath="target/classes:target/dependency/*:config"

java -classpath  ${cpath} -Xmx60g edu.illinois.cs.cogcomp.LbjNer.LbjTagger.NerTagger -train $TRAIN -test $TEST -c $configFile



