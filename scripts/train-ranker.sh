

LANG=$1
NDOCS=$2
CONFIG=$3

CP="./target/classes/:./target/dependency/*:./" 
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.core.Ranker $LANG $NDOCS $CONFIG
