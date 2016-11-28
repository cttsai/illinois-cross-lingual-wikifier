# Illinois Cross-Lingual Wikifier
This project implements the following two papers:
* [Cross-Lingual Wikification Using Multilingual Embeddings (Tsai and Roth, NAACL 2016)](http://cogcomp.cs.illinois.edu/page/publication_view/785)
* [Cross-Lingual Named Entity Recognition via Wikification (Tsai et al., CoNLL 2016)](http://cogcomp.cs.illinois.edu/page/publication_view/796)

The [live demo](http://bilbo.cs.illinois.edu/~ctsai12/xlwikifier/) will give you some intuition about this project.

### Setup (for CogComp members)
If you are not using illinois-xlwikifier-resources in pom.xml, you need to do:

ln -s /shared/preprocessed/ctsai12/multilingual/deft/xlwikifier-data/

This data folder (and illinois-xlwikifier-resources) only contains English, Spanish, and Chinese models for TAC EDL. Models and resources for other languages are at /shared/preprocessed/ctsai12/multilingual/xlwikifier-data/

### Run Benchmark
```
mvn dependency:copy-dependencies
mvn compile
./scripts/run-benchmark.sh es config/xlwikifier-tac.config
```
This script evaluates Spanish and Chinese performnace on TAC-KBP 2016 EDL shared task. You need to specify the paths to the test documents and the gold annotations in the config file. Check [config/xlwikifier-tac.config](https://github.com/cttsai/illinois-cross-lingual-wikifier/blob/master/config/xlwikifier-tac.config) for example. These documents are in the original format provided by LDC. You will get the following performance on named entities:

```
Spanish 
strong mention match:       Precision:0.879 Recall:0.800 F1:0.838
strong typed mention match: Precision:0.853 Recall:0.777 F1:0.813
strong typed all match:     Precision:0.769 Recall:0.700 F1:0.733

Chinese
strong mention match:       Precision:0.868 Recall:0.724 F1:0.789
strong typed mention match: Precision:0.835 Recall:0.696 F1:0.759
strong typed all match:     Precision:0.750 Recall:0.625 F1:0.682
```


### Train NER Model

Use ./scripts/train-ner.sh to train Illinois NER models with wikifier features. Note that the training and test files should be in the column format. 

### Train Wikifier Ranking Model
Requirements:
* Download and build the [ranking version of liblinear](https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/#large_scale_ranksvm). In the config file of cross-lingual wikifier, set the "liblinear_path" to the liblinear folder which contains the binary file "train".
* The path to the processed Wikipedia dumps needs to be set in the config. This is only available on CogComp machines now.
```
./scripts/train-ranker.sh es config/xlwikifier-tac.config
```
This script trains ranking models using Wikipedia articles. The resulting model is saved at the location specified in the config file.  

### Contact
Chen-Tse Tsai (ctsai12@illinois.edu)
