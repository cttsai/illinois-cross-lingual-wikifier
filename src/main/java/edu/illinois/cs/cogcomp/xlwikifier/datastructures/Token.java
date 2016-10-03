package edu.illinois.cs.cogcomp.xlwikifier.datastructures;

/**
 * Created by ctsai12 on 9/8/15.
 */
public class Token {

    public String id;
    public int start_char;
    public int end_char;
    public String surface;
    public Token(String id, int start, int end, String surface){
        this.id = id;
        this.start_char = start;
        this.end_char = end;
        this.surface = surface;
    }
    @Override
    public String toString() {
    	return surface;
    }
}
