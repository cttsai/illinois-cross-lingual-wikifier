

CP="./target/classes/:./target/dependency/*:./config/:./" # use this 
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.xlwikifier.evaluation.TAC2016Eval $1 $2 $3
