

LANG=$1
NDOCS=1000
CONFIG=$2

CP="./target/classes/:./target/dependency/*:./" 
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.core.Ranker $LANG $NDOCS $CONFIG
