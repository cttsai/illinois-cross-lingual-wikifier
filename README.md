# Illinois Cross-Lingual Wikifier
Given a piece of text in any language, a cross-lingual wikifier identifies mentions of named entities and grounds them to the corresponding entries in the English Wikipedia. This project implements the approaches proposed in the following two papers:
* <a href="http://cogcomp.cs.illinois.edu/papers/TsaiRo16b.pdf" target="_blank">Cross-Lingual Wikification Using Multilingual Embeddings (Tsai and Roth, NAACL 2016)</a> 
* <a href="http://cogcomp.cs.illinois.edu/papers/TsaiMaRo16.pdf" target="_blank">Cross-Lingual Named Entity Recognition via Wikification (Tsai et al., CoNLL 2016)</a> 

This <a href="http://bilbo.cs.illinois.edu/~ctsai12/xlwikifier/" target="_blank">demo</a> will give you some intuition about this project.

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
* Download and build the <a href="https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/#large_scale_ranksvm" target="_blank">ranking version of liblinear</a>. In the config file of cross-lingual wikifier, set the "liblinear_path" to the liblinear folder which contains the binary file "train".
* The path to the processed Wikipedia dumps needs to be set in the config. This is only available on CogComp machines now.
```
./scripts/train-ranker.sh es config/xlwikifier-tac.config
```
This script trains ranking models using Wikipedia articles. The resulting model is saved at the location specified in the config file.  

### Contact
Chen-Tse Tsai (ctsai12@illinois.edu)
