package io.github.contractautomata.maze;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.state.BasicState;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.catlib.operations.RelabelingOperator;
import io.github.contractautomata.maze.converters.JSonConverter;

public class App2 
{

	private final static String dir = System.getProperty("user.dir")+"/src/test/java/io/github/davidebasile/resources/";

	//private final static PngConverter pdc = new PngConverter();
	private final static AutDataConverter<CALabel> dc = new AutDataConverter<>(CALabel::new);
	private final static JSonConverter jdc = new JSonConverter();

	public static void main( String[] args ) throws Exception
	{

		System.out.println( "Maze Demo!" );
		
		// the commented code compute the composition of two agents and a driver and export it
		//		
		//		System.out.println("importing...");
		//		MSCA maze = pdc.importMSCA(dir+"maze3.png");
		//		MSCA maze2 = new RelabelingOperator().apply(maze);
		//		
		////		dc.exportMSCA(dir+"maze2.data",maze);
		////		//MSCA maze = dc.importMSCA(dir+"maze2.aut.data");
		//		MSCA driver = dc.importMSCA(dir+"driver.data");
		//
		//		System.out.println("resetting states...");
		//		maze.getInitial().getState().stream()
		//		.forEach(s->{s.setInitial(false); s.setFinalstate(false);});
		//		maze2.getInitial().getState().stream()
		//		.forEach(s->{s.setInitial(false); s.setFinalstate(false);});
		//
		//		//set initial and final state to states that are not forbidden, non-deterministic
		//		maze.getStates().parallelStream()
		//		.filter(s->!s.getState().get(0).getState().contains("#000000"))
		//		.findAny().orElseThrow(IllegalArgumentException::new)
		//		.getState().forEach(s->{s.setInitial(true);s.setFinalstate(true);});
		//
		//		maze2.getStates().parallelStream()
		//		.filter(s->!s.getState().get(0).getState().contains("#000000"))
		//		.findAny().orElseThrow(IllegalArgumentException::new)
		//		.getState().forEach(s->{s.setInitial(true);s.setFinalstate(true);});
		//		
		//		//set forbidden states
		//		maze.getTransition().addAll(maze.getStates().parallelStream()
		//				.filter(ca->ca.getState().get(0).getState().contains("#000000"))
		//				.map(ca->new MSCATransition(ca,new CALabel(1,0,"?forbidden"),ca,
		//						MSCATransition.Modality.URGENT))
		//				.collect(Collectors.toSet()));
		//		
		//		maze2.getTransition().addAll(maze.getStates().parallelStream()
		//				.filter(ca->ca.getState().get(0).getState().contains("#000000"))
		//				.map(ca->new MSCATransition(ca,new CALabel(1,0,"?forbidden"),ca,
		//						MSCATransition.Modality.URGENT))
		//				.collect(Collectors.toSet()));
		//
		//		System.out.println("composing...");
		//		
		//		CompositionFunction cf = new CompositionFunction(Arrays.asList(maze,maze2,driver));
		//		MSCA comp = cf.apply(new StrongAgreement().negate(), Integer.MAX_VALUE);
		//		
		//		
		//		System.out.println("exporting...");
		//		
		//		dc.exportMSCA(dir+"twoagents_maze3", comp);
		//		new JSonConverter().exportMSCA(dir+"twoagents_maze3", comp);

		generateImagesForEachState(dc.importMSCA(dir+"twoagents_maze3.data"));
		//	new JSonConverter().exportMSCA(dir+"twoagents_maze3", dc.importMSCA(dir+"twoagents_maze3.data"));
	}

	
	/**
	 * this private method takes the composition automaton of two agents, where each state of the composition 
	 * is a tuple of the position of each agent, and generates for each state of the composition a json encoding image 
	 * of GraphLogica where in the original starting image the positions of the two agents are emphasised with two 
	 * different colors (red and green)
	 */
	private static void generateImagesForEachState(Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut) throws Exception {
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> maze = dc.importMSCA(dir+"maze3.data");				
		
		aut.getStates().parallelStream()
		.forEach(aut_s->{
			final String s1 = aut_s.getState().get(0).getState().split("_")[0];
			final String s2 = aut_s.getState().get(1).getState().split("_")[0];

			Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>
					mazewithagents = new Automaton<>(new RelabelingOperator<String,CALabel>(CALabel::new, s->
			s.split("_")[0].equals(s1)?s.split("_")[0]+"_#FF0000"
					:s.split("_")[0].equals(s2)?s.split("_")[0]+"_#00FF00"
							:s, BasicState::isInitial, BasicState::isFinalState) //FIXME initial and forbidden states must be sent here
					.apply(maze));
			try {
				jdc.exportMSCA(dir+"twoagentsimages/"+JSonConverter.getstate.apply(aut_s), mazewithagents);
			} catch (Exception e) {
				RuntimeException re = new RuntimeException();
				re.addSuppressed(e);
				throw re;
			}		
		});
	}
}



//		MSCA strategy = new SynthesisOperator(
//				(x,t,bad)->Stream.of(x.getSource(),x.getTarget())
//				.map(s->s.getState().get(0).getState())
//				.anyMatch(s->s.contains("#000000")),
//				(x,t,bad)->false,
//				new Agreement()).apply(comp);			
