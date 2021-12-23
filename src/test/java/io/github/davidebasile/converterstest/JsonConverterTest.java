package io.github.davidebasile.converterstest;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.github.davidebasile.contractautomata.automaton.MSCA;
import io.github.davidebasile.contractautomata.converters.DataConverter;
import io.github.davidebasile.contractautomata.converters.MSCAConverter;
import io.github.davidebasile.contractautomatatest.MSCATest;
import io.github.davidebasile.converters.JSonConverter;

public class JsonConverterTest {
	private final DataConverter bdc = new DataConverter();
	private final String dir = System.getProperty("user.dir")+"/src/test/java/io/github/davidebasile/resources/";
	
	@Test
	public void importTest() throws Exception {
		MSCAConverter jc = new JSonConverter();
		MSCA aut = jc.importMSCA(dir+"testgraph.json");
	    bdc.exportMSCA(dir+"testgraph", aut);
	    assertEquals(MSCATest.checkTransitions(aut, aut),true);
	}
	
	@Test
	public void exportTest() throws Exception {
		MSCA aut = bdc.importMSCA(dir+"strategy.data");
		MSCAConverter jc = new JSonConverter();
		jc.exportMSCA(dir+"strategy.json",aut);
	}

}
