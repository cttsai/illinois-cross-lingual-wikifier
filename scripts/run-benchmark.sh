
LANG=$1

CONFIG=$2

CP="./target/classes/:./target/dependency/*:./"
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.evaluation.TAC2016Eval $LANG $CONFIG
