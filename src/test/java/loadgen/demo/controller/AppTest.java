package loadgen.demo.controller;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jbehave.core.embedder.*;
import org.jbehave.core.steps.*;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
    	Embedder embedder = new Embedder();
    	List<String> storyPaths = Arrays.asList(
    		"/Users/cliffordberg/Documents/loadgen-java/demo/controller/features/Demo.feature");
 
		embedder.candidateSteps().add(new Demo());
		embedder.runStoriesAsPaths(storyPaths);
    	
        assertTrue( true );
    }
}
