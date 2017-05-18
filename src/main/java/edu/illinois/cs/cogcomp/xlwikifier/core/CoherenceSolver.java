package edu.illinois.cs.cogcomp.xlwikifier.core;

import edu.illinois.cs.cogcomp.infer.ilp.GurobiHook;
import edu.illinois.cs.cogcomp.infer.ilp.ILPSolver;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.ELMention;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.QueryDocument;
import edu.illinois.cs.cogcomp.xlwikifier.datastructures.WikiCand;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class CoherenceSolver{

  public CoherenceSolver(QueryDocument doc,
                         RankerFeatureManager fm){
    this.doc = doc;
    this.fm = fm;
    initializeVariables();
    initializeConstraints();
    solver.setMaximize(true);
        // initialize variables
        // initialize constraints
        // solve
        // set Wiki title

  }

  private void initializeVariables(){
  }

  private void initializeConstraints(){
  }

  public void solve(){
    for (int i = 0; i < doc.mentions.size(); i++) {
      ELMention m = doc.mentions.get(i);
      List<WikiCand> cands = m.getCandidates();
      if (cands.size() > 0) {
        cands = cands.stream().sorted((x1, x2) -> 
            Double.compare(x2.getScore(), x1.getScore())).collect(toList());
        m.setCandidates(cands);
        m.setWikiTitle(cands.get(0).getTitle());
        m.setMidVec(fm.we.getTitleVector(m.getWikiTitle(), cands.get(0).lang));
      } else {
        m.setWikiTitle("NIL");
        m.setMidVec(null);
      }
    }
  }

  private ILPSolver solver;
  private QueryDocument doc;
  private RankerFeatureManager fm; 
}
