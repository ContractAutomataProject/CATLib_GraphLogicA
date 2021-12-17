package io.github.davidebasile.maze;

import java.util.Arrays;
import java.util.stream.Stream;

import io.github.davidebasile.contractautomata.automaton.MSCA;
import io.github.davidebasile.contractautomata.converters.DataConverter;
import io.github.davidebasile.contractautomata.converters.MSCAConverter;
import io.github.davidebasile.contractautomata.operators.CompositionFunction;
import io.github.davidebasile.contractautomata.operators.SynthesisOperator;
import io.github.davidebasile.contractautomata.requirements.Agreement;
import io.github.davidebasile.contractautomata.requirements.StrongAgreement;
import io.github.davidebasile.converters.JsonConverter;

public class App 
{

//	#FFFFFF white
//	#00FF00 green
//	#000000 black
//	#0000FF blue
//	MSCA aut = new DataConverter().importMSCA(dir+"/src/main/java/io/github/davidebasile/maze/resources/maze.data");

	public static void main( String[] args ) throws Exception
    {
        System.out.println( "Maze Demo!" );
        String dir = System.getProperty("user.dir")+"/src/test/java/io/github/davidebasile/resources/";
    	 
    	System.out.println("importing...");
    	MSCAConverter jc = new JsonConverter();
		MSCA maze = jc.importMSCA(dir+"maze.json");
		

    	MSCAConverter dc = new DataConverter();
		MSCA driver = dc.importMSCA(dir+"driver_nodown.data");
		
		System.out.println("resetting states");
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

//		System.out.println("saving imported automaton...");
//		new DataConverter().exportMSCA(dir+"maze.data", maze);
		
		System.out.println("composing...");
		MSCA comp = new CompositionFunction().apply(Arrays.asList(maze,driver), new StrongAgreement().negate(), 100);
		
		System.out.println("synthesis...");
		MSCA strategy = new SynthesisOperator((x,t,bad)->Stream.of(x.getSource(),x.getTarget())
				.map(s->s.getState().get(0).getState())
				.anyMatch(s->s.contains("#000000")),(x,t,bad)->false,new Agreement()).apply(comp);
		

		System.out.println("saving strategy.");
		jc.exportMSCA(dir+"strategy_nodown.json", strategy);
    }
}
