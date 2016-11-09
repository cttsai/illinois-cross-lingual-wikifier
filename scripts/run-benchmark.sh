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
# Using configuration file config/xlwikifier-tac.config with 10 test documents, you will get the following performance:
#
# Spanish (es):
# Mention Span: Precision:0.9215 Recall:0.8333 F1:0.8752
# Mention Span + Entity Type: Precision:0.8840 Recall:0.7994 F1:0.8395
# Mention Span + Entity Type + FreeBase ID: Precision:0.7747 Recall:0.7006 F1:0.7358
#
# Chinese (zh):
# Mention Span: Precision:0.9137 Recall:0.6859 F1:0.7836
# Mention Span + Entity Type: Precision:0.9042 Recall:0.6787 F1:0.7753
# Mention Span + Entity Type + FreeBase ID: Precision:0.7955 Recall:0.5971 F1:0.6822

LANG=$1
CONFIG=$2

CP="./target/dependency/*:./target/classes/"
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.evaluation.TAC2016Eval $LANG $CONFIG

