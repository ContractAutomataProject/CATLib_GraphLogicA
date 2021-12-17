package io.github.davidebasile.converterstest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.github.davidebasile.contractautomata.automaton.MSCA;
import io.github.davidebasile.contractautomata.converters.DataConverter;
import io.github.davidebasile.contractautomata.converters.MSCAConverter;
import io.github.davidebasile.contractautomatatest.MSCATest;
import io.github.davidebasile.converters.JsonConverter;

public class JsonConverterTest {
	private final DataConverter bdc = new DataConverter();
	private final String dir = System.getProperty("user.dir")+"/src/test/java/io/github/davidebasile/resources/";
	
	@Test
	public void importTest() throws Exception {
		MSCAConverter jc = new JsonConverter();
		MSCA aut = jc.importMSCA(dir+"testgraph.json");
	    bdc.exportMSCA(dir+"testgraph", aut);
	//    MSCA test= bdc.importMSCA(dir+"/CAtest/voxlogicajsonimporttest.data");
	    assertEquals(MSCATest.checkTransitions(aut, aut),true);
	}
	
	@Test
	public void exportTest() throws Exception {
		MSCA aut = bdc.importMSCA(dir+"strategy.data");
		MSCAConverter jc = new JsonConverter();
		jc.exportMSCA(dir+"strategy.json",aut);//only for coverage
	}

}
