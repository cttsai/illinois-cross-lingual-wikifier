

CP="./target/classes/:./target/dependency/*:./config/:./" # use this 
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifier $1 $2 $3
