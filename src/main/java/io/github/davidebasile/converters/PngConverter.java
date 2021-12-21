package io.github.davidebasile.converters;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.imageio.ImageIO;

import io.github.davidebasile.contractautomata.automaton.MSCA;
import io.github.davidebasile.contractautomata.automaton.state.CAState;
import io.github.davidebasile.contractautomata.converters.MSCAConverter;

public class PngConverter implements MSCAConverter {

	@Override
	public MSCA importMSCA(String filename) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void exportMSCA(String filename, MSCA aut) throws Exception {
		String suffix=(filename.endsWith(".png"))?"":"png";
	    File output = new File(filename+suffix);
	    ImageIO.write(getBufferedImage(aut), "jpg", output);
	}

	public BufferedImage getBufferedImage(MSCA aut) {
		final Function<CAState,String> getstate = ca -> ca.getState().get(0).getState().split("_")[0];
		final Function<CAState,String> getattr = ca -> ca.getState().get(0).getState().split("_")[1];
		
		BiFunction<Set<CAState>,Integer,Integer> fwh = (set,i)->set.parallelStream()
				.map(ca-> getstate.apply(ca).replaceAll("[\\(\\)\\s]", "").split(";"))
				.mapToInt(arr->Integer.parseInt(arr[i]))
				.max().orElse(-1);

		int width=fwh.apply(aut.getStates(), 0);
	
		int height=fwh.apply(aut.getStates(), 1);
		
	    BufferedImage image=new BufferedImage(width+1, height+1, BufferedImage.TYPE_INT_RGB);
	    
	    System.out.println(width+" "+height);
	    
	    aut.getStates().stream()
	    .forEach(ca->{
	    	String[] coord = getstate.apply(ca).replaceAll("[\\(\\)\\s]", "").split(";");
	    	System.out.println(ca);
	    	image.setRGB(Integer.parseInt(coord[0]), Integer.parseInt(coord[1]), Color.decode(getattr.apply(ca)).getRGB());
	    });
	    
	    return image;
	}
	
	public BufferedImage overlap(BufferedImage plant, BufferedImage strategy) {	
		BufferedImage overlap = new BufferedImage(plant.getWidth(),plant.getHeight(),BufferedImage.TYPE_INT_RGB);
		for (int i=0;i<plant.getWidth();i++)
			for (int j=0;j<plant.getHeight();j++)
			{
				if (i<strategy.getWidth()&&j<strategy.getHeight()&&strategy.getRGB(i, j)!=Color.black.getRGB())
					overlap.setRGB(i, j, Color.RED.getRGB());
				else
					overlap.setRGB(i, j, plant.getRGB(i, j));
			}
		return overlap;
	}
}
