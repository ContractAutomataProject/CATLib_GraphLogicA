package io.github.davidebasile.converterstest;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Test;

import io.github.davidebasile.contractautomata.automaton.MSCA;
import io.github.davidebasile.contractautomata.converters.DataConverter;
import io.github.davidebasile.contractautomata.converters.MSCAConverter;
import io.github.davidebasile.converters.PngConverter;

public class BufferedImageConverterTest {
	private final MSCAConverter bdc = new DataConverter();
	private final PngConverter pdc = new PngConverter();
	private final String dir = System.getProperty("user.dir")+"/src/test/java/io/github/davidebasile/resources/";

	@Test
	public void exportTest() throws Exception {
		MSCA aut = bdc.importMSCA(dir+"maze.data");
		pdc.exportMSCA(dir+"maze_export.png",aut);

	}
	
	@Test
	public void overlapTest() throws Exception {
		BufferedImage plant = ImageIO.read(new File(dir+"maze_export.png"));
	
	    File output = new File(dir+"overlap_maze_strategy_nodown.png");
	    ImageIO.write(pdc.overlap(plant,pdc.getBufferedImage(bdc.importMSCA(dir+"strategy_nodown.data"))), "png", output);
	}
}
