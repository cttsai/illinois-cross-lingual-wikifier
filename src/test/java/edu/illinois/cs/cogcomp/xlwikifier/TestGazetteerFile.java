package edu.illinois.cs.cogcomp.xlwikifier;

import edu.illinois.cs.cogcomp.core.io.LineIO;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import static org.junit.Assert.fail;

/**
 * Test to investigate behavior of gazetteer file -- contains 10K+ lines, but can read only ~1k
 */
public class TestGazetteerFile {

    @Test
    public void testLoadGazetteer()
    {
//        InputStream res = ResourceUtilities.loadResource("xlwikifier-data/gazetteers/zh-per");

        try {
            ArrayList<String> lines = LineIO.readFromClasspath("xlwikifier-data/gazetteers/zh-per" );
            System.out.println( "read " + lines.size() + " lines using LineIO." );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            fail( e.getMessage() );
        }

//        InputStreamReader is = new InputStreamReader(res, StandardCharsets.UTF_8 ); //.UTF_8);
//
//        BufferedReader in = new BufferedReader(is);
//
//        int numLines = 0;
//        int numChars = 0;
//
//        String str = null;
//
//        try {
//            while( (str = in.readLine()) != null )//&& numLines < 100 )
//            {
//                numLines++;
//                numChars += str.length();
//                System.err.println( str );
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            fail(e.getMessage());
//        }
//        System.out.println( "read " + numLines + " lines, " + numChars + " characters." );
    }
}
