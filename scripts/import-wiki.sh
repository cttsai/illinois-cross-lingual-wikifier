#!/bin/sh

# Importing Wikipedia dump.
# Dumps can be found here: https://dumps.wikimedia.org/backup-index.html
# 
# Note that we use very simple tokenization (white space based). 
# If the new langauge needs special tokenization, you can add the tokenizer in the WikipediaAPI project.
#
# For cogcomp members:
# Due to the https certification issue, using the JAVA from module will fail to download the dumps.
# The problem is that we do not permission to update the cetification.
# You can download the four required dumps manually and comment out downloadDump() in Importer.java.
# Another solution is to use a local JAVA (e.g., /shared/experiments/ctsai12/Downloads/jre1.8.0_60).

# Language code. E.g., 'en': English
LANG=$1

# The date of the dump. E.g., 20170701
DATE=$2

# The config file which specifies output paths
CONFIG=$3

CP="./target/classes/:./target/dependency/*:./" 
java -ea -Xmx60g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.wikipedia.Importer $LANG $DATE $CONFIG
