package io.github.contractautomata.maze;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.label.action.OfferAction;
import io.github.contractautomata.catlib.automaton.label.action.RequestAction;
import io.github.contractautomata.catlib.automaton.state.BasicState;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition.Modality;
import io.github.contractautomata.catlib.converters.AutConverter;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.catlib.operations.MSCACompositionFunction;
import io.github.contractautomata.catlib.operations.RelabelingOperator;
import io.github.contractautomata.maze.converters.JSonConverter;
import io.github.contractautomata.maze.converters.PngConverter;

public class AppMazeTwoAgents
{

	private final static String gateCoordinates = "(2; 7; 0)";
	private final static String dir = System.getProperty("user.dir")+"/src/test/java/io/github/contractautomata/maze/resources/";

	private final static PngConverter pdc = new PngConverter();
	private final static AutDataConverter<CALabel> dc = new AutDataConverter<>(CALabel::new);
	private final static JSonConverter jdc = new JSonConverter();

	public static void main( String[] args ) throws Exception
	{

		System.out.println( "Maze Demo!" );

		computesCompositionAndSaveIt();

		generateImagesForEachState(dc.importMSCA(dir+"twoagents_maze3.data"),false);
		//	new JSonConverter().exportMSCA(dir+"twoagents_maze3", dc.importMSCA(dir+"twoagents_maze3.data"));
	}

	private static void computesCompositionAndSaveIt() throws IOException {

		//compute the composition of two agents and a driver and export it
		System.out.println("importing...");
		
		final State<String> stateDriver = new State<>(List.of(new BasicState<>("Driver",true,true)));
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> driver = 
				new Automaton<>(Stream.of("goup","godown","goleft","goright")
				.map(s->new CALabel(1,0,new OfferAction(s)))
				.map(act->new ModalTransition<>(stateDriver,act,stateDriver,Modality.PERMITTED))
				.collect(Collectors.toSet()));
				
				//	dc.importMSCA(dir+"driver.data");


		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> door = 
				new Automaton<>(Map.of(new State<>(List.of(new BasicState<>("Close",true,false))), 
						new State<>(List.of(new BasicState<>("Open",false,true))))
						.entrySet().stream()
						.flatMap(e->Stream.of(new ModalTransition<>(e.getKey(), new CALabel(1,0,new OfferAction("open")), e.getValue(), Modality.PERMITTED),
								new ModalTransition<>(e.getValue(), new CALabel(1,0,new OfferAction("close")), e.getKey(), Modality.PERMITTED)))
						.collect(Collectors.toSet()));


		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>  maze = pdc.importMSCA(dir+"maze3.png");

		Function<Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>, 
					Set<ModalTransition<String,Action,State<String>,CALabel>>> relabel = aut ->
				new RelabelingOperator<String,CALabel>(CALabel::new,s->s,s->s.getState().split("_")[0].equals("(1; 1; 0)"),s->true).apply(aut);
				
		Set<ModalTransition<String,Action,State<String>,CALabel>> maze_tr = relabel.apply(maze);
		Set<ModalTransition<String,Action,State<String>,CALabel>> maze2_tr = relabel.apply(maze);
		
		//		dc.exportMSCA(dir+"maze2.data",maze);
		//		MSCA maze = dc.importMSCA(dir+"maze2.aut.data");


		Stream.of(maze_tr, maze2_tr)
		.forEach(set->{//addInitialState(set); 
						setForbiddenStates(set);});

		System.out.println("composing...");

		MSCACompositionFunction<String> cf = new MSCACompositionFunction<String>(List.of(new Automaton<>(maze_tr),
				new Automaton<>(maze2_tr),driver,door),t->t.getLabel().isRequest());
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>  comp =
				cf.apply(Integer.MAX_VALUE);


		System.out.println("exporting...");

		dc.exportMSCA(dir+"twoagents_maze3", comp);
		jdc.exportMSCA(dir+"twoagents_maze3", comp);
	}

	@SuppressWarnings("unused")
	private static void addInitialState(Set<ModalTransition<String,Action,State<String>,CALabel>> maze_tr) {
		//set initial  state to a state  that is not forbidden
		State<String> init = new State<>(List.of(new BasicState<>("Init",true,false)));
		maze_tr.add(new ModalTransition<>(init, new CALabel(1,0,new OfferAction("start")), 
				maze_tr.parallelStream()
				.flatMap(t->Stream.of(t.getSource(),t.getTarget()))
				.filter(s->!s.getState().get(0).getState().contains("#000000"))
				.findAny().orElseThrow(IllegalArgumentException::new),
				Modality.PERMITTED));
	}
	

	private static void setForbiddenStates(Set<ModalTransition<String,Action,State<String>,CALabel>> maze_tr) {
		//set forbidden states
		maze_tr.addAll(maze_tr.parallelStream()
				.flatMap(t->Stream.of(t.getSource(),t.getTarget()))
				.filter(s->s.getState().get(0).getState().contains("#000000"))
				.map(s->new ModalTransition<>(s,new CALabel(1,0,new RequestAction("forbidden")),s,Modality.URGENT))
				.collect(Collectors.toSet()));
	}

	/**
	 * this private method takes the composition automaton of two agents, where each state of the composition 
	 * is a tuple of the position of each agent, and generates for each state of the composition a json encoding image 
	 * of GraphLogica or a png where in the original starting image the positions of the two agents are emphasised with two 
	 * different colors (red and green)
	 * 
	 * @param json  if true generates json else png
	 */
	private static void generateImagesForEachState(
			Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut, 
			boolean json) throws Exception {
		
		System.out.println("Generating images...");
		
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> maze = pdc.importMSCA(dir+"maze3.png");				

		aut.getStates().parallelStream()
		.forEach(aut_s->{
			final String s1 = aut_s.getState().get(0).getState().split("_")[0];
			final String s2 = aut_s.getState().get(1).getState().split("_")[0];
			final boolean gateClosed = aut_s.getState().get(3).getState().equals("Close");

			Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>
			mazewithagents = new Automaton<>(new RelabelingOperator<String,CALabel>(CALabel::new, s->
			s.split("_")[0].equals(s1)?s.split("_")[0]+"_#FF0000"
					:s.split("_")[0].equals(s2)?s.split("_")[0]+"_#00FF00"
							:gateClosed&&s.split("_")[0].equals(gateCoordinates)?gateCoordinates+"_#0000FF"
									:s, BasicState::isInitial, BasicState::isFinalState) 
					.apply(maze));
			try {
				AutConverter<?,
						Automaton<String,Action,State<String>,ModalTransition<String,Action,State<String>,CALabel>>> ac = json?jdc:pdc;
				ac.exportMSCA(dir+"twoagentsimages/"+JSonConverter.getstate.apply(aut_s), mazewithagents);
			} catch (Exception e) {
				RuntimeException re = new RuntimeException();
				re.addSuppressed(e);
				throw re;
			}		
		});
	}
}

