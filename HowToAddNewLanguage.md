**This only works for CogComp members**

Following are the steps to add a new language: 
(The example language is Amharic, Wikipedia language code: am)

1. Process the Wikipedia dump

    Run [scripts/import-wiki.sh](https://github.com/cttsai/illinois-cross-lingual-wikifier/blob/master/scripts/import-wiki.sh). This script will download the required dumps. If it fails, it could be the https certification issue. See the comments in this script for more details. 
    
    Example:
    ```
    ./scripts/import-wiki.sh am 20170701 config/xlwikifier-demo.config
    ```


2. Train cross-lingual embeddings

    - Train monolingual embeddings for the new language using Gensim's skip-gram implementation. Run /shared/bronte/ctsai12/multilingual/sg/train.py with the target language code. The input and output files are hard-coded, so you may want to change that.
        
        Example:
        ```
        cd /shared/bronte/ctsai12/multilingual/sg
        ./train.py am
        ```
    - Project the embeddings of the new language and English into the same space. Run /shared/experiments/ctsai12/workspace/eacl14-cca/run.sh with the target language code. Note that this step could be slow since the size of English embeddings is large.
        
        Example:
        ```
        cd /shared/experiments/ctsai12/workspace/eacl14-cca
        ./run.sh am
        ```

3. Index the cross-lingual embeddings using MapDB

    Run [scripts/import-embedding.sh](https://github.com/cttsai/illinois-cross-lingual-wikifier/blob/master/scripts/import-embedding.sh). 
    
    Example:
    ```    
    ./scripts/import-embedding.sh am
    ```

4. Train the ranking model

    - Download and build the <a href="https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/#large_scale_ranksvm" target="_blank">ranking version of liblinear</a>. In the config file of cross-lingual wikifier, set the "liblinear_path" to the liblinear folder which contains the binary file "train". You can also copy it from /shared/experiments/ctsai12/workspace/xlwikifier/liblinear-ranksvm-1.95

    - Set the path of ranking model in the configuration file. For example, add the following line in config/xlwikifier-demo.config:
    ```
    am_ranking_model = xlwikifier-data/models/ranker/default/am/ranker.model
    ```
    
    - Run [scripts/train-ranker.sh](https://github.com/cttsai/illinois-cross-lingual-wikifier/blob/master/scripts/train-ranker.sh).
   
    Example:
    ```
    ./scripts/train-ranker.sh am 3000 config/xlwikifier-demo.config
    ```
    
    
