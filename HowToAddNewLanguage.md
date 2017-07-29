**This only works for CogComp members**

Following are the steps to add a new language:

1. Process the Wikipedia dump

    Run [scripts/import-wiki.sh](https://github.com/cttsai/illinois-cross-lingual-wikifier/blob/master/scripts/import-wiki.sh). See the file for more details. 
    
    Example:
    ```
    ./scripts/import-wiki.sh om 20170701 config/xlwikifier-demo.config
    ```


2. Train cross-lingual embeddings

    - Train monolingual embeddings for the new language
        Run /shared/bronte/ctsai12/multilingual/sg/train.py with the target language code. The input and output files are hard-coded, so you may want to change that.
        
        Example:
        ```
        ./train.py am
        ```
    - Project the embeddings of the new language and English into the same space
        Run /shared/experiments/ctsai12/workspace/eacl14-cca/run.sh with the target language code. Note that this step could be slow since the size of English embeddings is large.
        
        Example:
        ```
        ./run.sh am
        ```

3. Index the cross-lingual embeddings using MapDB

    Run [scripts/import-embedding.sh](https://github.com/cttsai/illinois-cross-lingual-wikifier/blob/master/scripts/import-embedding.sh). 
    
    Example:
    ```
    ./scripts/import-embedding.sh om
    ```

4. Train the ranking model
