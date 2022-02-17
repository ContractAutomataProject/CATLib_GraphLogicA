package io.github.davidebasile.converters;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import io.github.davidebasile.contractautomata.automaton.MSCA;
import io.github.davidebasile.contractautomata.automaton.label.CALabel;
import io.github.davidebasile.contractautomata.automaton.state.BasicState;
import io.github.davidebasile.contractautomata.automaton.state.CAState;
import io.github.davidebasile.contractautomata.automaton.transition.MSCATransition;
import io.github.davidebasile.contractautomata.converters.MSCAConverter;

public class PngConverter implements MSCAConverter {

	@Override
	public MSCA importMSCA(String filename) throws Exception {
		if (!filename.endsWith(".png"))
			throw new IllegalArgumentException("Not a .png format");
		BufferedImage img = ImageIO.read(new File(filename));

		Set<CAState> sc = IntStream.range(0,img.getWidth())
		.mapToObj(i->IntStream.range(0, img.getHeight())
			.mapToObj(j->new CAState(Arrays.asList(new BasicState("("+i+"; "+j+"; 0)_"+ 
						"#"+Integer.toHexString(img.getRGB(i, j)).substring(2).toUpperCase(), //String.format("#%06x", img.getRGB(i, j) & 0xFFFFFF)  //img.getRGB(i, j)
			 (i==0)&&(j==0),(i==0)&&(j==0))))))
		.flatMap(Function.identity())
		.collect(Collectors.toSet());
		
		Set<MSCATransition> tr = sc.stream()
				.flatMap(s->getTransitions(s,sc,img.getWidth(),img.getHeight()).stream())
				.collect(Collectors.toSet());
		return new MSCA(tr);
	}
	
	private Set<MSCATransition> getTransitions(CAState source, Set<CAState> states, int max_width, int max_height) {
		String regex = "\\(([0-9]*);\\s([0-9]*);\\s([0-9]*)\\)(.)*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(source.getState().get(0).getState());
		matcher.find();

		int x = Integer.parseInt(matcher.group(1));		
		int y = Integer.parseInt(matcher.group(2));
		//int z = Integer.parseInt(matcher.group(3));
		
		Map<String,String> targets_labels = new HashMap<>();
		if (x!=0)
			targets_labels.put("("+(x-1)+"; "+y+"; 0)","?goleft");
		if (x!=max_width-1)
			targets_labels.put("("+(x+1)+"; "+y+"; 0)","?goright");
		if (y!=0)
			targets_labels.put("("+x+"; "+(y-1)+"; 0)","?goup");
		if (y!=max_height-1)
			targets_labels.put("("+x+"; "+(y+1)+"; 0)","?godown");

		return targets_labels.entrySet().stream()
		.map(e->states.parallelStream()
				//.peek(ca->System.out.println(ca.getState().get(0).getState().split("_")[0]+"   "+e.getKey()))
				.filter(ca->ca.getState().get(0).getState().split("_")[0].equals(e.getKey()))
				.map(ca->new MSCATransition(source,new CALabel(1,0,e.getValue()),ca,MSCATransition.Modality.PERMITTED))
				.findAny().orElseThrow(RuntimeException::new))
		.collect(Collectors.toSet());
	}

	@Override
	public void exportMSCA(String filename, MSCA aut) throws Exception {
		String suffix=(filename.endsWith(".png"))?"":".png";
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
	    
	    aut.getStates().stream()
	    .forEach(ca->{
	    	String[] coord = getstate.apply(ca).replaceAll("[\\(\\)\\s]", "").split(";");
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
					overlap.setRGB(i, j, blend(new Color(plant.getRGB(i, j)),Color.red,0.5).getRGB());
				else
					overlap.setRGB(i, j, plant.getRGB(i, j));
			}
		return overlap;
	}
	
	private Color blend( Color c1, Color c2, double ratio ) {
	    if ( ratio > 1f ) ratio = 1f;
	    else if ( ratio < 0f ) ratio = 0f;
	    double iRatio = 1.0f - ratio;

	    int i1 = c1.getRGB();
	    int i2 = c2.getRGB();

	    int a1 = (i1 >> 24 & 0xff);
	    int r1 = ((i1 & 0xff0000) >> 16);
	    int g1 = ((i1 & 0xff00) >> 8);
	    int b1 = (i1 & 0xff);

	    int a2 = (i2 >> 24 & 0xff);
	    int r2 = ((i2 & 0xff0000) >> 16);
	    int g2 = ((i2 & 0xff00) >> 8);
	    int b2 = (i2 & 0xff);

	    int a = (int)((a1 * iRatio) + (a2 * ratio));
	    int r = (int)((r1 * iRatio) + (r2 * ratio));
	    int g = (int)((g1 * iRatio) + (g2 * ratio));
	    int b = (int)((b1 * iRatio) + (b2 * ratio));

	    return new Color( a << 24 | r << 16 | g << 8 | b );
	}
}
