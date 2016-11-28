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
# Mention Span: Precision:0.8787 Recall:0.8004 F1:0.8377
# Mention Span + Entity Type: Precision:0.8528 Recall:0.7768 F1:0.8130
# Mention Span + Entity Type + FreeBase ID: Precision:0.7688 Recall:0.7003 F1:0.7329
#
# Chinese (zh):
# Mention Span: Precision:0.8682 Recall:0.7235 F1:0.7893
# Mention Span + Entity Type: Precision:0.8346 Recall:0.6955 F1:0.7587
# Mention Span + Entity Type + FreeBase ID: Precision:0.7501 Recall:0.6250 F1:0.6819

LANG=$1
CONFIG=$2

CP="./target/dependency/*:./target/classes/"
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.evaluation.TAC2016Eval $LANG $CONFIG

