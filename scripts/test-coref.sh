#!/bin/sh


LANG=$1
CONFIG=$2

CP="./target/dependency/*:./target/classes/"
nice java -ea -Xmx90g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.evaluation.TestCrossDocCoref 5000

