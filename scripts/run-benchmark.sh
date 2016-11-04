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
# Using configuration file config/xlwikifier-tac.config, you will get the following performance:
#
# Spanish (es):
# Mention Span: Precision:0.9037 Recall:0.8395 F1:0.8704
# Mention Span + Entity Type: Precision:0.8671 Recall:0.8056 F1:0.8352
# Mention Span + Entity Type + FreeBase ID: Precision:0.7575 Recall:0.7037 F1:0.7296
#
# Chinese (zh):
# Mention Span: Precision:0.9042 Recall:0.6787 F1:0.7753
# Mention Span + Entity Type: Precision:0.8946 Recall:0.6715 F1:0.7671
# Mention Span + Entity Type + FreeBase ID: Precision:0.7764 Recall:0.5827 F1:0.6658

LANG=$1
CONFIG=$2

CP="./target/dependency/*:./target/classes/"
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.evaluation.TAC2016Eval $LANG $CONFIG

