
#mvn clean
#mvn dependency:copy-dependencies
#mvn compile



CP="./target/classes/:./target/dependency/*:./config/:/home/kchang10/workspace/112-latentcoref/src/main/resources:/shared/bronte/upadhya3/gurobi603/linux64/lib/gurobi.jar" # use this 
#java -ea -Xmx30g -cp $CP spanish.EDLSolver $1 $2
java -ea -Xmx30g -cp $CP spanish.Main $1 $2 $3
#java -ea -Xmx30g -cp $CP spanish.WikiCandidateGenerator $1 $2
#java -ea -Xmx30g -cp $CP spanish.ner.FreeLingNER $1 $2
#java -cp $CP tac.entitylinking.SubmissionGenerator $1
#java -cp $CP tac.entitylinking.Test $1
#java -cp $CP tac.entitylinking.main.EntityLinking2013 $1
#java -cp $CP tac.entitylinking.SubmissionGenerator $1
#java -cp $CP tac.entitylinking.main.AcronymLinking $1
#java -cp $CP tac.entitylinking.main.AcronymTesting $1

#RESULT="/shared/bronte/tac2014/2013rerun/finalResults_$1"
#GOLD="/shared/bronte/tac2014/LDC2013E90_TAC_2013_KBP_English_Entity_Linking_Evaluation_Queries_and_Knowledge_Base_Links_V1.1/data/tac_2013_kbp_english_entity_linking_evaluation_KB_links.tab"
#echo "./el_scorer.py $GOLD $RESULT"
#./el_scorer.py $GOLD $RESULT
