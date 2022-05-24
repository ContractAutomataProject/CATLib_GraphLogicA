package io.github.contractautomata.maze;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.List;
import java.util.stream.Collectors;

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
    @org.junit.Test
    public void testApp() throws Exception {
        List<String> test= List.of("(1; 1; 0)_#FFFFFF", "(1; 1; 0)_#FFFFFF", "Driver", "Open");
        String test2="(1, 1, 0),(1, 1, 0),Driver,Open.png";

        String temp = test.stream()
                .map(x->x.split("_")[0].replaceAll(";", ","))
                .collect(Collectors.joining(","));

        assertEquals(temp,test2.split(".png")[0]);
    }
}
