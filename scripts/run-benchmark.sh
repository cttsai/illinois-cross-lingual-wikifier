#!/bin/sh

###
# Run a benchmark test class that evaluates cross-lingual wikifier models on the TAC 2016 EDL data
#
# Two options for LANG: "es" and "zh"
#
# Using configuration file config/xlwikifier-tac.config, you will get the following performance:
#
# Spanish (es):
# Mention Span: Precision:0.8764 Recall:0.7937 F1:0.8330
# Mention Span + Entity Type: Precision:0.8511 Recall:0.7708 F1:0.8090
# Mention Span + Entity Type + FreeBase ID: Precision:0.7634 Recall:0.6914 F1:0.7256
#
# Chinese (zh):
# Mention Span: Precision:0.8597 Recall:0.7358 F1:0.7929
# Mention Span + Entity Type: Precision:0.8240 Recall:0.7052 F1:0.7600
# Mention Span + Entity Type + FreeBase ID: Precision:0.7409 Recall:0.6340 F1:0.6833

LANG=$1
CONFIG=$2

CP="./target/classes/:./target/dependency/*:./"
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.evaluation.TAC2016Eval $LANG $CONFIG

