package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;

/**
 * Created by ctsai12 on 9/8/15.
 */
public class Paragraph {
    public String id;
    public List<Token> tokens = new ArrayList<>();
    public String surface;

    public Paragraph(){}

    public Paragraph(Paragraph p){
        this.tokens.addAll(p.tokens);
        this.surface = p.surface;
        this.id = p.id;
    }

    public String getPlainText(){
        return tokens.stream().map(x -> x.surface).collect(joining(" "));
    }



}
