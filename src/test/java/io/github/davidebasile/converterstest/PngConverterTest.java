package io.github.davidebasile.converterstest;

import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import org.junit.Test;

import io.github.davidebasile.contractautomata.automaton.ModalAutomaton;
import io.github.davidebasile.contractautomata.automaton.label.CALabel;
import io.github.davidebasile.contractautomata.converters.DataConverter;
import io.github.davidebasile.contractautomata.converters.MSCAConverter;
import io.github.davidebasile.contractautomatatest.MSCATest;
import io.github.davidebasile.converters.JSonConverter;
import io.github.davidebasile.converters.PngConverter;

public class PngConverterTest {
	private final MSCAConverter bdc = new DataConverter();
	private final PngConverter pdc = new PngConverter();
	private final JSonConverter jc = new JSonConverter();
	private final String dir = System.getProperty("user.dir")+"/src/test/java/io/github/davidebasile/resources/";

	@Test 
	public void importTest() throws Exception {
		ModalAutomaton<CALabel> aut = pdc.importMSCA(dir+"maze.png");
		ModalAutomaton<CALabel> test = jc.importMSCA(dir+"maze.json");
		bdc.exportMSCA(dir+"maze_imported.data", aut);
		assertTrue(MSCATest.checkTransitions(aut, test));
	}
	
	@Test
	public void exportTest() throws Exception {
		ModalAutomaton<CALabel> aut = bdc.importMSCA(dir+"maze.data");
		pdc.exportMSCA(dir+"maze_export.png",aut);

	}
	
	@Test
	public void overlapTest() throws Exception {
		BufferedImage plant = ImageIO.read(new File(dir+"maze_export.png"));
	
	    File output = new File(dir+"overlap_maze_strategy_noturningback.png");
	    ImageIO.write(pdc.overlap(plant,pdc.getBufferedImage(bdc.importMSCA(dir+"strategy_noturningback.data"))), "png", output);
	}
	
}
