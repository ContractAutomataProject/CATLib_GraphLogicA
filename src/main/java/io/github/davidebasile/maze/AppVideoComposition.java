package io.github.davidebasile.maze;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import io.github.davidebasile.contractautomata.automaton.ModalAutomaton;
import io.github.davidebasile.contractautomata.automaton.label.CALabel;
import io.github.davidebasile.contractautomata.automaton.state.BasicState;
import io.github.davidebasile.contractautomata.automaton.state.CAState;
import io.github.davidebasile.contractautomata.automaton.transition.ModalTransition;
import io.github.davidebasile.contractautomata.converters.DataConverter;
import io.github.davidebasile.contractautomata.converters.MSCAConverter;
import io.github.davidebasile.contractautomata.operators.MSCACompositionFunction;
import io.github.davidebasile.contractautomata.requirements.StrongAgreement;
import io.github.davidebasile.converters.PngConverter;

public class AppVideoComposition 
{
	public static void main( String[] args ) throws Exception
	{
		System.out.println( "Maze Demo!" );
		String dir = System.getProperty("user.dir")+"/src/test/java/io/github/davidebasile/resources/video/";
		PngConverter pdc = new PngConverter();
		MSCAConverter dc = new DataConverter();
		
		System.out.println("importing...");
		ModalAutomaton<CALabel> maze = pdc.importMSCA(dir+"maze2.png");
		dc.exportMSCA(dir+"maze2.data",maze);

		//MSCA maze = dc.importMSCA(dir+"maze2.aut.data");
		ModalAutomaton<CALabel> driver = dc.importMSCA(dir+"driver.data");

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
				.map(ca->new ModalTransition<List<BasicState>,List<String>,CAState,CALabel>(ca,new CALabel(1,0,"?forbidden"),ca,
						ModalTransition.Modality.URGENT))
				.collect(Collectors.toSet()));

		System.out.println("composing...");
		
		MSCACompositionFunction cf = new MSCACompositionFunction(Arrays.asList(maze,driver),
				new StrongAgreement().negate());
		int bound=0;
		do {
			bound++;
			ModalAutomaton<CALabel> comp=cf.apply(bound);//555);
	
//			System.out.println("synthesis...");
//			MSCA strategy = new MpcSynthesisOperator(new StrongAgreement()).apply(comp);
	
			System.out.println("saving "+bound);
	
			ImageIO.write(pdc.overlap(ImageIO.read(new File(dir+"maze2.png")),pdc.getBufferedImage(comp)),//strategy)), 
					"png",  
					new File(dir+bound+".png"));//"maze2_strategy_overlay.png"));
		} while (!cf.isFrontierEmpty());
	}
}



//		MSCA strategy = new SynthesisOperator(
//				(x,t,bad)->Stream.of(x.getSource(),x.getTarget())
//				.map(s->s.getState().get(0).getState())
//				.anyMatch(s->s.contains("#000000")),
//				(x,t,bad)->false,
//				new Agreement()).apply(comp);			
