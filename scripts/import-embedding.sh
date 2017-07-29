#!/bin/sh

# Index cross-lingual embeddings using MapDB
# Note that the paths to the embeddings are hard-coded in WordEmbedding.java

# The target language code
LANG=$1

# The config file which specifies MapDB location
CONFIG=$2

CP="./target/classes/:./target/dependency/*"
java -ea -Xmx70g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.core.WordEmbedding $LANG $CONFIG
