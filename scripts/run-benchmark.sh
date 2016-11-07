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
# Mention Span: Precision:0.9158 Recall:0.8395 F1:0.8760
# Mention Span + Entity Type: Precision:0.8788 Recall:0.8056 F1:0.8406
# Mention Span + Entity Type + FreeBase ID: Precision:0.7710 Recall:0.7068 F1:0.7375
#
# Chinese (zh):
# Mention Span: Precision:0.9309 Recall:0.6787 F1:0.7850
# Mention Span + Entity Type: Precision:0.9276 Recall:0.6763 F1:0.7822
# Mention Span + Entity Type + FreeBase ID: Precision:0.7993 Recall:0.5827 F1:0.6741

LANG=$1
CONFIG=$2

CP="./target/dependency/*:./target/classes/"
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.evaluation.TAC2016Eval $LANG $CONFIG

