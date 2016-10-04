

CP="./target/classes/:./target/dependency/*:./config/:./" # use this 
java -ea -Xmx30g -cp $CP edu.illinois.cs.cogcomp.demo.XLWikifierDemo $1 $2 $3
