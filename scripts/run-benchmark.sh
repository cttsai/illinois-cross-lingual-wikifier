#!/bin/sh

###
# Run a benchmark test class that evaluates cross-lingual wikifier models on 
# a small subset of the TAC 2016 EDL data
#
# Running example:
# scripts/run-benchmark.sh es config/xlwikifier-tac.config
#
# Two options for LANG: "es" and "zh"
#
# Using configuration file config/xlwikifier-tac.config with all TAC2016 test documents, you will get the following performance:
#
# Spanish (es):
# Mention Span: Precision:0.8793 Recall:0.8007 F1:0.8382
# Mention Span + Entity Type: Precision:0.8538 Recall:0.7775 F1:0.8139
#
# Chinese (zh):
# Mention Span: Precision:0.8683 Recall:0.7236 F1:0.7894
# Mention Span + Entity Type: Precision:0.8350 Recall:0.6959 F1:0.7591

LANG=$1
CONFIG=$2

CP="./target/dependency/*:./target/classes/"
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.evaluation.TAC2016Eval $LANG $CONFIG

