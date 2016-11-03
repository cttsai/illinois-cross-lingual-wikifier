# Illinois Cross-Lingual Wikifier
This project implements the following two papers:
* [Cross-Lingual Wikification Using Multilingual Embeddings (Tsai and Roth, NAACL 2016)](http://cogcomp.cs.illinois.edu/page/publication_view/785)
* [Cross-Lingual Named Entity Recognition via Wikification (Tsai et al., CoNLL 2016)](http://cogcomp.cs.illinois.edu/page/publication_view/796)

The [live demo](http://bilbo.cs.illinois.edu/~ctsai12/xlwikifier/) will give you some intuition about this project.

### Setup (for CogComp members)
If you are not using illinois-xlwikifier-resources in pom.xml, you need to do:

ln -s /shared/preprocessed/ctsai12/multilingual/deft/xlwikifier-data/

This data folder only contains English, Spanish, and Chinese models for TAC EDL. Models for other languages are at /shared/preprocessed/ctsai12/multilingual/xlwikifier-data/

### Run Benchmark
* mvn dependency:copy-dependencies
* mvn compile
* ./scripts/run-benchmark.sh es config/xlwikifier-tac.config

Check run-benchmark.sh for more details

### Train Ranking Model
Two requirements:
* Download and build [ranking version of liblinear](https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/#large_scale_ranksvm). In the config file, set the "liblinear_path" to the liblinear folder which contains "train".
* The path to the processed Wikipedia dumps needs to be set. This is only available on CogComp machines now.

Use ./script/train-ranker.sh and check the [corresponding class](https://github.com/cttsai/cross-lingual-wikifier/blob/demo/src/main/java/edu/illinois/cs/cogcomp/xlwikifier/core/Ranker.java). This script trains a ranking model using Wikipedia articles.

### Train NER Model

Use ./script/train-ner.sh to train Illinois NER models with wikifier features. Note that the training and test files should be in the colum format. 

### Contact
Chen-Tse Tsai (ctsai12@illinois.edu)
