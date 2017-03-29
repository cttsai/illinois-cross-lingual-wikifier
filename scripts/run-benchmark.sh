#!/bin/sh

###
# Run a benchmark test class that evaluates cross-lingual wikifier models on 
# a small subset of the TAC 2016 EDL data
#
# Running example:
# scripts/run-benchmark.sh es config/xlwikifier-tac.config
#
# Three options for LANG: "en", "es", and "zh"
#
# Using configuration file config/xlwikifier-tac.config with all TAC2016 test documents, you will get the following performance:
#

LANG=$1
CONFIG=$2

CP="./target/dependency/*:./target/classes/"
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.evaluation.TAC2016Eval $LANG $CONFIG

