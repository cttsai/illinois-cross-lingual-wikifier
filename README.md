# Illinois Cross-Lingual Wikifier
This project implements the following two papers:
* [Cross-Lingual Wikification Using Multilingual Embeddings (Tsai and Roth, NAACL 2016)](http://cogcomp.cs.illinois.edu/page/publication_view/785)
* [Cross-Lingual Named Entity Recognition via Wikification (Tsai et al., CoNLL 2016)](http://cogcomp.cs.illinois.edu/page/publication_view/796)

The [live demo](http://bilbo.cs.illinois.edu/~ctsai12/xlwikifier/) will give you some intuition about this project.

### Setup (for CogComp members)
If you are not using illinois-xlwikifier-resources in pom.xml, you need to do:

ln -s /shared/preprocessed/ctsai12/multilingual/deft/xlwikifier-data/

This data folder only contains English, Spanish, and Chinese models for TAC EDL. Models and resources for other languages are at /shared/preprocessed/ctsai12/multilingual/xlwikifier-data/

### Run Benchmark
```
mvn dependency:copy-dependencies
mvn compile
./scripts/run-benchmark.sh es config/xlwikifier-tac.config
```
The test documents are specified in the config file. These documents are from TAC 2016 EDL shared task. Check run-benchmark.sh for the desired performance and other details. 

### Train Ranking Model
Requirements:
* Download and build the [ranking version of liblinear](https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/#large_scale_ranksvm). In the config file of cross-lingual wikifier, set the "liblinear_path" to the liblinear folder which contains the binary file "train".
* The path to the processed Wikipedia dumps needs to be set in the config. This is only available on CogComp machines now.
```
./scripts/train-ranker.sh es config/xlwikifier-tac.config
```
This script trains ranking models using Wikipedia articles. The resulting model is saved at the location specified in the config file.  

### Train NER Model

Use ./scripts/train-ner.sh to train Illinois NER models with wikifier features. Note that the training and test files should be in the column format. 

### Contact
Chen-Tse Tsai (ctsai12@illinois.edu)
