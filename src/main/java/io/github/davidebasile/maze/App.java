package io.github.davidebasile.maze;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import io.github.davidebasile.contractautomata.automaton.MSCA;
import io.github.davidebasile.contractautomata.automaton.label.CALabel;
import io.github.davidebasile.contractautomata.automaton.transition.MSCATransition;
import io.github.davidebasile.contractautomata.converters.DataConverter;
import io.github.davidebasile.contractautomata.converters.MSCAConverter;
import io.github.davidebasile.contractautomata.operators.CompositionFunction;
import io.github.davidebasile.contractautomata.operators.MpcSynthesisOperator;
import io.github.davidebasile.contractautomata.requirements.StrongAgreement;
import io.github.davidebasile.converters.PngConverter;

public class App 
{
	public static void main( String[] args ) throws Exception
	{
		System.out.println( "Maze Demo!" );
		String dir = System.getProperty("user.dir")+"/src/test/java/io/github/davidebasile/resources/";

		System.out.println("importing...");
		PngConverter pdc = new PngConverter();
		//		MSCA maze = pdc.importMSCA(dir+"maze2.png");


		MSCAConverter dc = new DataConverter();
		MSCA maze = dc.importMSCA(dir+"maze2.aut.data");
		MSCA driver = dc.importMSCA(dir+"driver.data");

		System.out.println("resetting states...");
		//reset initial state
		maze.getInitial().getState().stream()
		.forEach(s->{s.setInitial(false); s.setFinalstate(false);});

		//set initial state to a blue node
		maze.getStates().parallelStream()
		.filter(s->s.getState().get(0).getState().contains("#0000FF"))
		.findAny().orElseThrow(IllegalArgumentException::new)
		.getState().forEach(s->{s.setInitial(true);});

		//set all green states to final
		maze.getStates().parallelStream()
		.filter(s->s.getState().get(0).getState().contains("#00FF00"))
		.flatMap(s->s.getState().stream())
		.forEach(s->s.setFinalstate(true));

		//set forbidden states
		maze.getTransition().addAll(maze.getStates().parallelStream()
				.filter(ca->ca.getState().get(0).getState().contains("#000000"))
				.map(ca->new MSCATransition(ca,new CALabel(1,0,"?forbidden"),ca,
						MSCATransition.Modality.URGENT))
				.collect(Collectors.toSet()));

		System.out.println("composing...");
		MSCA comp=new CompositionFunction().apply(Arrays.asList(maze,driver), 
				new StrongAgreement().negate(),
				555);//79);

		System.out.println("synthesis...");		
		//		MSCA strategy = new SynthesisOperator(
		//				(x,t,bad)->Stream.of(x.getSource(),x.getTarget())
		//				.map(s->s.getState().get(0).getState())
		//				.anyMatch(s->s.contains("#000000")),
		//				(x,t,bad)->false,
		//				new Agreement()).apply(comp);			
		MSCA strategy = new MpcSynthesisOperator(new StrongAgreement()).apply(comp);

		System.out.println("saving ");

		ImageIO.write(pdc.overlap(ImageIO.read(new File(dir+"maze2.png")),pdc.getBufferedImage(strategy)), 
				"png",  
				new File(dir+"maze2_strategy_overlay.png"));
	}
}
