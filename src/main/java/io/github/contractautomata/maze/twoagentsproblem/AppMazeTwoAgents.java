package io.github.contractautomata.maze.twoagentsproblem;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.label.action.IdleAction;
import io.github.contractautomata.catlib.automaton.label.action.OfferAction;
import io.github.contractautomata.catlib.automaton.label.action.RequestAction;
import io.github.contractautomata.catlib.automaton.state.BasicState;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition.Modality;
import io.github.contractautomata.catlib.converters.AutConverter;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.catlib.operations.MSCACompositionFunction;
import io.github.contractautomata.catlib.operations.MpcSynthesisOperator;
import io.github.contractautomata.catlib.operations.RelabelingOperator;
import io.github.contractautomata.catlib.requirements.Agreement;
import io.github.contractautomata.maze.converters.JSonConverter;
import io.github.contractautomata.maze.converters.PngConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class AppMazeTwoAgents
{

	private final static String gateCoordinates = "(2; 7; 0)";
	private final static String dir = System.getProperty("user.dir")+"/src/test/java/io/github/contractautomata/maze/resources/";

	private final static PngConverter pdc = new PngConverter();
	private final static AutDataConverter<CALabel> dc = new AutDataConverter<>(CALabel::new);
	private final static JSonConverter jdc = new JSonConverter();

	public static void main( String[] args ) throws Exception
	{
//		initial1 forbidden1
//		initial1 forbidden2
//		initial2 forbidden1 - expected empty

		System.out.println( "Maze Demo!" );

		boolean computeComposition = false;
		boolean computeMarkingOfComposition = true;

		Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> marked;
		if (computeMarkingOfComposition)
		{
			Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> comp;
			if (computeComposition)
				comp = dc.importMSCA(dir+"twoagents_maze3.data");
			else
				comp = computesCompositionAndSaveIt();

			//generateImagesForEachState(comp,false);

			marked = readVoxLogicaOutputAndMarkStates(comp, "initial1", "forbidden1"); //computing
			System.out.println("Exporting marked composition");
			dc.exportMSCA(dir + "twoagents_maze3_marked", marked);
		}
		else
		{
			System.out.println("Importing marked composition");
			marked = dc.importMSCA(dir + "twoagents_maze3_marked.data"); //importing
		}

		System.out.println("...computing the synthesis... ");
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> strategy =
				new MpcSynthesisOperator<String>(new Agreement()).apply(marked);

		dc.exportMSCA(dir + "strategy_twoagents_maze3", strategy);
	}

	private static Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> computesCompositionAndSaveIt() throws IOException {

		//compute the composition of two agents and a driver and export it
		System.out.println("importing...");

		final State<String> stateDriver = new State<>(List.of(new BasicState<>("Driver",true,true)));
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> driver =
				new Automaton<>(Stream.of("goup","godown","goleft","goright")
						.map(s->new CALabel(1,0,new OfferAction(s)))
						.map(act->new ModalTransition<>(stateDriver,act,stateDriver,Modality.PERMITTED))
						.collect(Collectors.toSet()));


		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> door =
				new Automaton<>(Map.of(new State<>(List.of(new BasicState<>("Close",true,false))),
								new State<>(List.of(new BasicState<>("Open",false,true))))
						.entrySet().stream()
						.flatMap(e->Stream.of(new ModalTransition<>(e.getKey(), new CALabel(1,0,new OfferAction("open")), e.getValue(), Modality.PERMITTED),
								new ModalTransition<>(e.getValue(), new CALabel(1,0,new OfferAction("close")), e.getKey(), Modality.PERMITTED)))
						.collect(Collectors.toSet()));


		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>  maze = pdc.importMSCA(dir+"maze3.png");

		BiFunction<String, Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>,
						Set<ModalTransition<String,Action,State<String>,CALabel>>> relabel = (l,aut) ->
				new RelabelingOperator<String,CALabel>(CALabel::new,s->s,s->s.getState().split("_")[0].equals(l),
						s->!s.getState().contains("#000000")).apply(aut);

		Set<ModalTransition<String,Action,State<String>,CALabel>> maze_tr = relabel.apply("(1; 1; 0)",maze);
		Set<ModalTransition<String,Action,State<String>,CALabel>> maze2_tr = relabel.apply("(1; 2; 0)",maze);

		//		dc.exportMSCA(dir+"maze2.data",maze);
		//		MSCA maze = dc.importMSCA(dir+"maze2.aut.data");


//		Stream.of(maze_tr, maze2_tr)
//				.forEach(set->{//addInitialState(set);
//					setForbiddenStates(set,s->s.getState().get(0).getState().contains("#000000"));});

		Predicate<State<String>> badState = s->
				//two agents on the same cell
				s.getState().get(0).getState().split("_")[0].equals(s.getState().get(1).getState().split("_")[0])
						||
				IntStream.range(0,2)
				.mapToObj(i->s.getState().get(i).getState())
				.anyMatch(l->(l.contains("#000000")) //one agent on black
						||
						(l.contains(gateCoordinates) && s.getState().get(3).getState().contains("Close")));
						//one agent on the gate whilst the gate is closed

		System.out.println("composing...");

		MSCACompositionFunction<String> cf = new MSCACompositionFunction<>(List.of(new Automaton<>(maze_tr),
				new Automaton<>(maze2_tr),driver,door),t->t.getLabel().isRequest() || badState.test(t.getTarget()));
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>  comp =
				cf.apply(Integer.MAX_VALUE);

		Set<ModalTransition<String,Action,State<String>,CALabel>> set_comp = comp.getTransition();

//		System.out.println(comp);

//		setForbiddenStates(set_comp, s->IntStream.range(0,2)
//				.mapToObj(i->s.getState().get(i).getState())
//				.anyMatch(l->l.contains(gateCoordinates) && s.getState().get(3).getState().contains("Close")));

//		System.out.println("...computing the synthesis... ");
//		MpcSynthesisOperator<String> mso = new MpcSynthesisOperator<>(new Agreement());
//		comp = mso.apply(new Automaton<>(set_comp));

		System.out.println("exporting...");

		dc.exportMSCA(dir+"twoagents_maze3", comp);
		jdc.exportMSCA(dir+"twoagents_maze3", comp);

		return comp;
	}

	private static void setForbiddenStates(Set<ModalTransition<String,Action,State<String>,CALabel>> maze_tr, Predicate<State<String>> isForbidden) {
		//set forbidden states
		maze_tr.addAll(maze_tr.parallelStream()
				.flatMap(t->Stream.of(t.getSource(),t.getTarget()))
				.filter(isForbidden)
				.map(s->new ModalTransition<>(s,new CALabel(s.getRank(),0,new RequestAction("forbidden")),s,Modality.URGENT))
				.collect(Collectors.toSet()));
	}

	/**
	 * this private method takes the composition automaton, where each state of the composition
	 * is a tuple of the position of each agent and the door state, and generates for each state of the composition a json encoding image
	 * of GraphLogica or a png where in the original starting image the positions of the two agents are emphasised with two 
	 * different colors (red and green) whilst the door is blue
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
							gateClosed&&s.split("_")[0].equals(gateCoordinates)?gateCoordinates+"_#0000FF": //if the gate is closed it must be draw first
									s.split("_")[0].equals(s1)?s.split("_")[0]+"_#FF0000"
											:s.split("_")[0].equals(s2)?s.split("_")[0]+"_#00FF00"
											:s, BasicState::isInitial, BasicState::isFinalState)
							.apply(maze));
					try {
						AutConverter<?,
								Automaton<String,Action,State<String>,ModalTransition<String,Action,State<String>,CALabel>>> ac = json?jdc:pdc;
						ac.exportMSCA(dir+"twoagentsimages/png/"+JSonConverter.getstate.apply(aut_s), mazewithagents);
					} catch (Exception e) {
						RuntimeException re = new RuntimeException();
						re.addSuppressed(e);
						throw re;
					}
				});
	}


	private static Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> readVoxLogicaOutputAndMarkStates(Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut,
																																				   String initialkey, String forbiddenkey) throws IOException {
		System.out.println("reading voxlogica computed file");
		//parse the voxlogica json output and extract the information about initial, final and forbidden states
		String content = Files.readAllLines(Paths.get(dir+"voxlogicaoutput/experiment2.json"))
				.stream()
				.collect(Collectors.joining(" "));
		JSONArray obj = new JSONArray(content);

		final Set<String> initialstate = extractFromJSON(obj, o-> o.getString("filename").contains("Close") && o.getJSONObject("results").getBoolean(initialkey));
		if (initialstate.size()!=1)
			throw new IllegalArgumentException();

		final Set<String> finalstates = extractFromJSON(obj,o->o.getJSONObject("results").getBoolean("final"));
		final Set<String> forbiddenstates = extractFromJSON(obj,o->o.getJSONObject("results").getBoolean(forbiddenkey));

		finalstates.removeAll(forbiddenstates); //final states cannot be forbidden

		//System.out.println(finalstates);

		BiPredicate<State<String>,Set<String>> pred =  (s,set) -> set.parallelStream()
				.anyMatch(x->x.equals(JSonConverter.getstate.apply(s)));

		System.out.println("Updating the automaton");

		//reset initial and final states to false
		RelabelingOperator<String,CALabel> ro = new RelabelingOperator<>(CALabel::new,x->x,x->false,x->false);


		System.out.println("Reset initial and final states, and selected agents to uncontrollable");
		//uncontrollable: Mr Green
		//turn the moves of the second principal (mrGreen) to uncontrollable
		Set<ModalTransition<String, Action, State<String>, CALabel>> setr = ro.apply(aut).parallelStream()
				.map(t->new ModalTransition<>(t.getSource(),t.getLabel(),t.getTarget(),
						(t.getLabel().getContent().get(3) instanceof IdleAction)?
								Modality.PERMITTED: ModalTransition.Modality.URGENT))
				.collect(Collectors.toSet());

		System.out.println("State Marking ... ");

		//mark initial, final and forbidden states
		BasicState<String> bsi = new BasicState<>("Init",true,false);
		final State<String> init = new State<>(List.of(bsi,bsi,bsi,bsi));
		final State<String> finalstate = new State<>(IntStream.range(0,4)
				.mapToObj(i->new BasicState<>("Final",false,true))
				.collect(Collectors.toList()));

		Function<State<String>, ModalTransition<String, Action, State<String>, CALabel>> f_forbidden =
				(State<String> s)->new ModalTransition<>(s,new CALabel(s.getRank(),0,new RequestAction("forbidden")),s,Modality.URGENT);

		setr.addAll(Map.of(initialstate,(State<String> s)-> new ModalTransition<>(init,new CALabel(s.getRank(),0,new OfferAction("start")),s,Modality.PERMITTED),
						finalstates,(State<String> s)->new ModalTransition<>(s, new CALabel(s.getRank(),0,new OfferAction("stop")),	finalstate,Modality.PERMITTED),
						forbiddenstates,f_forbidden)
				.entrySet().parallelStream()
				.flatMap(e->setr.parallelStream()
						.flatMap(t->Stream.of(t.getSource(),t.getTarget()))
						.filter(s->pred.test(s,e.getKey()))
						.map(e.getValue()))
				.collect(Collectors.toSet()));

		return new Automaton<>(setr);
	}

	private static Set<String> extractFromJSON(JSONArray obj, Predicate<JSONObject> pred){
		return IntStream.range(0,obj.length())
				.mapToObj(obj::getJSONObject)
				//	.peek(System.out::println)
				.filter(pred)
				.map(o->o.getString("filename").split(".png")[0])
				.collect(Collectors.toSet());
	}
}

