# Illinois Cross-Lingual Wikifier
This project implements the following two papers:
* [Cross-Lingual Wikification Using Multilingual Embeddings (Tsai and Roth, NAACL 2016)](http://cogcomp.cs.illinois.edu/page/publication_view/785)
* [Cross-Lingual Named Entity Recognition via Wikification (Tsai et al., CoNLL 2016)](http://cogcomp.cs.illinois.edu/page/publication_view/796)

The [live demo](http://bilbo.cs.illinois.edu/~ctsai12/xlwikifier/) will give you some intuition about this project.

### Setup (for CogComp members)
To run an example with defualt models:
* Fork the project, checkout the demo branch, and go to the root directory of the project 
* ln -s /shared/preprocessed/ctsai12/multilingual/xlwikifier-data/brown-clusters/ brown-clusters
* ln -s /shared/preprocessed/ctsai12/multilingual/xlwikifier-data/models models
* mvn dependency:copy-dependencies
* mvn compile
* ./scripts/run-example.sh

### Train Ranking Model
Use ./script/train-ranker.sh and check the [corresponding class](https://github.com/cttsai/cross-lingual-wikifier/blob/demo/src/main/java/edu/illinois/cs/cogcomp/xlwikifier/core/Ranker.java). This script trains a ranking model using Wikipedia articles.

### Train NER Model

We use Illinois NER to train and do inference. Check ./script/train-ner.sh as an example. Note that the input files are in the colum format. 
