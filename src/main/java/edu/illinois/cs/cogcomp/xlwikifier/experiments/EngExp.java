package edu.illinois.cs.cogcomp.xlwikifier.experiments;

import edu.illinois.cs.cogcomp.xlwikifier.CrossLingualWikifier;
import edu.illinois.cs.cogcomp.xlwikifier.core.Ranker;
import edu.illinois.cs.cogcomp.xlwikifier.wikipedia.WikiCandidateGenerator;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.experiments.reader.WikiDataReader;
import edu.illinois.cs.cogcomp.xlwikifier.Utils;

import java.util.List;

/**
 * Created by ctsai12 on 1/20/16.
 */
public class EngExp {

    private WikiDataReader wiki_reader = new WikiDataReader();
    private Evaluator eval = new Evaluator();
    public EngExp(){

    }

    public void wikifierOnWikiDataExp(){

    }


    private Ranker trainEnRanker(){
        String lang = "en";
        String test_lang = "es";
        Ranker ranker = new Ranker(lang);
        String model = Utils.getTime()+"."+lang;

        List<QueryDocument> docs = wiki_reader.readTrainData(lang);
        WikiCandidateGenerator wcg = new WikiCandidateGenerator();
        wcg.genCandidates(docs, lang);
        ranker.train(docs, model);
        return ranker;
    }

//    public void clwikifierOnStandardDataExp(){
//        String[] datasets = {"ACE", "MSNBC", "AQUAINT", "Wikipedia"};
//        Ranker ranker = trainEnRanker();
//        CrossLingualWikifier wikifier = new CrossLingualWikifier();
//
//        for(String dataset: datasets){
//            System.out.println("Running clwikifer on "+dataset);
//            File problem_path = new File("/shared/bronte/mssammon/WikifierResources/eval/ACL2010DataNewFormat/", dataset);
//
//            List<QueryDocument> docs = new ArrayList<>();
//            for(File f: problem_path.listFiles(FileFilters.viewableFiles)){
//                ReferenceProblem ref = ReferenceProblem.load(f);
//                QueryDocument doc = new QueryDocument(ref.id);
//                doc.plain_text = ref.text;
//                doc.mentions = new ArrayList<>();
//                for(ReferenceInstance ri: ref.reference) {
//                    ELMention m = new ELMention(ref.id, ri.charStart, ri.getCharEnd());
//                    m.setMention(ri.surface);
//                    m.gold_wiki_title = ri.annotation;
//                    doc.mentions.add(m);
//                }
//                docs.add(doc);
//            }
//            System.out.println("#docs "+docs.size());
//            wikifier.wikify(docs, "en", ranker);
//            eval.evaluateWikiTitle(docs);
//            break;
//        }
//    }

    public static void main(String[] args) {
        EngExp ee = new EngExp();
//        ee.clwikifierOnStandardDataExp();
    }
}
