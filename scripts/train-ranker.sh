

CP="./target/classes/:./target/dependency/*:./config/" 
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.core.Ranker $1 $2 $3
