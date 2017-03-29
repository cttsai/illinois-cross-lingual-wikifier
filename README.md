# Illinois Cross-Lingual Wikifier
Given a piece of text in any language, a cross-lingual wikifier identifies mentions of named entities and grounds them to the corresponding entries in the English Wikipedia. This project implements the approaches proposed in the following two papers:
* <a href="http://cogcomp.cs.illinois.edu/papers/TsaiRo16b.pdf" target="_blank">Cross-Lingual Wikification Using Multilingual Embeddings (Tsai and Roth, NAACL 2016)</a> 
* <a href="http://cogcomp.cs.illinois.edu/papers/TsaiMaRo16.pdf" target="_blank">Cross-Lingual Named Entity Recognition via Wikification (Tsai et al., CoNLL 2016)</a> 

This <a href="http://cogcomp.cs.illinois.edu/page/demo_view/xl_wikifier" target="_blank">demo</a> will give you some intuition about this project. The demo is presented in COLING 2016 (the [paper](http://cogcomp.cs.illinois.edu/page/publication_view/809) and [poster](http://cogcomp.cs.illinois.edu/files/posters/poster.pdf))

### Setup

Download [this file](http://cogcomp.cs.illinois.edu/Data/ccgPapersData/ctsai12/xlwikifier-mapdb.tar.gz) which contains MapDB indices of FreeBase dump and English, Spanish, and Chinese Wikipedia. Follow the README inside to extract the files and set the corresponding paths in the config file.

Note that we currently only release the resources for these three languages.

For CogComp members, if you want to know where are the resources for other languages, please contact me.

### Run Benchmark
```
mvn dependency:copy-dependencies
mvn compile
./scripts/run-benchmark.sh es config/xlwikifier-tac.config
```
This script evaluates Spanish and Chinese performnace on TAC-KBP 2016 EDL shared task. You need to specify the paths to the test documents and the gold annotations in the config file. Check [config/xlwikifier-tac.config](https://github.com/cttsai/illinois-cross-lingual-wikifier/blob/master/config/xlwikifier-tac.config) for example. These documents are in the original format provided by LDC. You will get the following performance on named entities:

```
English
strong mention match:       Precision:0.935 Recall:0.801 F1:0.863
strong typed mention match: Precision:0.900 Recall:0.771 F1:0.831
string typed all match:     Precision:0.764 Recall:0.655 F1:0.705
mention ceaf:               Precision:0.854 Recall:0.732 F1:0.788

Spanish 
strong mention match:       Precision:0.885 Recall:0.806 F1:0.844
strong typed mention match: Precision:0.859 Recall:0.782 F1:0.819
string typed all match:     Precision:0.755 Recall:0.688 F1:0.720
mention ceaf:               Precision:0.822 Recall:0.749 F1:0.784

Chinese
strong mention match:       Precision:0.868 Recall:0.724 F1:0.789
strong typed mention match: Precision:0.835 Recall:0.696 F1:0.759
string typed all match:     Precision:0.746 Recall:0.622 F1:0.679
mention ceaf:               Precision:0.807 Recall:0.673 F1:0.734
```
### Train NER Model

Use ./scripts/train-ner.sh to train Illinois NER models with wikifier features. Note that the training and test files should be in the column format. 

### Train Wikifier Ranking Model
Requirements:
* Download and build the <a href="https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/#large_scale_ranksvm" target="_blank">ranking version of liblinear</a>. In the config file of cross-lingual wikifier, set the "liblinear_path" to the liblinear folder which contains the binary file "train".
* The path to the processed Wikipedia dumps needs to be set in the config. This is only available on CogComp machines now.
```
./scripts/train-ranker.sh es config/xlwikifier-tac.config
```
This script trains ranking models using Wikipedia articles. The resulting model is saved at the location specified in the config file.  

### Contact
Chen-Tse Tsai (ctsai12@illinois.edu)
