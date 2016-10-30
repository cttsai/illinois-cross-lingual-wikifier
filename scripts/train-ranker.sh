

LANG=$1
CONFIG=$2

CP="./target/classes/:./target/dependency/*:./config/" 
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.core.Ranker $LANG $CONFIG
