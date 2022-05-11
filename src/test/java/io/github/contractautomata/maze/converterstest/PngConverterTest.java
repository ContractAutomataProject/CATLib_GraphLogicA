package io.github.contractautomata.maze.converterstest;

import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import org.junit.Test;

import io.github.contractautomata.maze.converters.JSonConverter;
import io.github.contractautomata.maze.converters.PngConverter;

public class PngConverterTest {
	private final AutDataConverter<CALabel> bdc = new AutDataConverter<>(CALabel::new);
	private final PngConverter pdc = new PngConverter();
	private final JSonConverter jc = new JSonConverter();
	private final String dir = Paths.get(System.getProperty("user.dir")).getParent()
			+"/CATLib_PngConverter/src/test/java/io/github/contractautomata/resources/";

	@Test 
	public void importTest() throws Exception {
		Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> aut =
				pdc.importMSCA(dir+"maze.png");
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> test = jc.importMSCA(dir+"maze.json");
		bdc.exportMSCA(dir+"maze_imported.data", aut);
		assertTrue(JsonConverterTest.autEquals(aut, test));
	}
	
	@Test
	public void exportTest() throws Exception {
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>, CALabel>> aut = bdc.importMSCA(dir+"maze.data");
		pdc.exportMSCA(dir+"maze_export.png",aut);

	}
	
	@Test
	public void overlapTest() throws Exception {
		BufferedImage plant = ImageIO.read(new File(dir+"maze_export.png"));
	
	    File output = new File(dir+"overlap_maze_strategy_noturningback.png");
	    ImageIO.write(pdc.overlap(plant,pdc.getBufferedImage(bdc.importMSCA(dir+"strategy_noturningback.data"))), "png", output);
	}

	@Test
	public importJsonExportPng() throws Exception {
	}
	
}
