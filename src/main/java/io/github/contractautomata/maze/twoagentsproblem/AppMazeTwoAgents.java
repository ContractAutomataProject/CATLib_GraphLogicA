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
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
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
	//private final static JSonConverter jdc = new JSonConverter();

	public static void main( String[] args ) throws Exception
	{
//		initial1 forbidden1
//		initial1 forbidden2
//		initial2 forbidden1 - expected empty

		String message = "Usage : java -jar -Xss1000M AppMazeTwoAgents.jar [options] \n" +
				"where options are :\n" +
				"-composition   (compute and store the whole composition, generate an image for each state of the composition) \n" +
				"-legalcomposition   (compute and store the composition with only legal moves) \n" +
				"-synthesis [1|2] (mark the composition with the properties for either experiment 1 or 2, synthesise and store the strategy) \n";

		boolean computeComposition=false;
		boolean computeMarkingOfComposition=false;
		boolean legal=false;
		boolean firstexperiment=false;
		boolean secondexperiment=false;

		Instant start, stop;
		long elapsedTime;

		System.out.println( "Maze example with two agents." );

		if (args==null || args.length==0) {
			System.out.println(message);
			return;
		}

		for(int i = 0; i < args.length; i++) {
			if(args[i].equals("-composition")) {
				computeComposition=true;
				legal=false;
			}
			else if(args[i].equals("-legalcomposition")) {
				computeComposition=true;
				legal=true;
			}
			else if(args[i].equals("-synthesis")) {
				computeMarkingOfComposition=true;
				computeComposition=false;
				String exp = args[++i];
				firstexperiment=(exp=="1");
				secondexperiment=(exp=="2");
				if (!firstexperiment&&!secondexperiment){
					System.out.println(message);
					return;
				}
			}
		}

		if (computeComposition) {

			start = Instant.now();
			System.out.println("Starting..."+start);
			Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> comp = computesCompositionAndSaveIt(legal);


			stop = Instant.now();
			elapsedTime = Duration.between(start, stop).toMillis();
			System.out.println("Elapsed time "+elapsedTime);

			if (!legal){
				start = Instant.now();
				System.out.println("Start generating images..."+start);
				generateImagesForEachState(comp,false);
				stop = Instant.now();
				elapsedTime = Duration.between(start, stop).toMillis();
				System.out.println("Elapsed time "+elapsedTime);
			}
		}

		if (computeMarkingOfComposition)
		{
			System.out.println("Importing legal composition ");
			Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> comp =  loadFile("twoagents_maze_legal.data",false);// dc.importMSCA(dir+"twoagents_maze3.data");


			start = Instant.now();
			System.out.println("Start marking automaton with voxlogica output at "+start);
			Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> marked = readVoxLogicaOutputAndMarkStates(comp, "initial1",
					firstexperiment?"forbidden1":"forbidden2", firstexperiment); //computing

			stop = Instant.now();
			elapsedTime = Duration.between(start, stop).toMillis();
			System.out.println("Elapsed time "+elapsedTime);

			System.out.println("Exporting marked composition");
			dc.exportMSCA(dir + "twoagents_maze_marked_"+(firstexperiment?"firstexp":"secondexp"), marked);


			start = Instant.now();
			System.out.println("Start computing the synthesis at "+start);
			Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> strategy =
					new MpcSynthesisOperator<String>(new Agreement()).apply(marked);

			stop = Instant.now();
			elapsedTime = Duration.between(start, stop).toMillis();
			System.out.println("Elapsed time "+elapsedTime);

			System.out.println("Exporting the strategy");
			dc.exportMSCA(dir + "strategy", strategy);
		}
	}

	private static Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> computesCompositionAndSaveIt(boolean legal) throws IOException, ParserConfigurationException, SAXException {

		//compute the composition of two agents and a driver and export it
		System.out.println("create driver and door...");

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

		System.out.println("importing the maze image");

		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>  maze = loadFile("io/github/contractautomata/maze/twoagentsproblem/resources/maze3.png",true);//pdc.importMSCA(dir+"maze3.png");

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
				new Automaton<>(maze2_tr),driver,door),t->t.getLabel().isRequest() || (legal&&badState.test(t.getTarget())));

		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>  comp =
				cf.apply(Integer.MAX_VALUE);


		System.out.println("exporting...");

		dc.exportMSCA(dir+"twoagents_maze"+(legal?"_legal":""), comp);

		return comp;
	}

//	private static void setForbiddenStates(Set<ModalTransition<String,Action,State<String>,CALabel>> maze_tr, Predicate<State<String>> isForbidden) {
//		//set forbidden states
//		maze_tr.addAll(maze_tr.parallelStream()
//				.flatMap(t->Stream.of(t.getSource(),t.getTarget()))
//				.filter(isForbidden)
//				.map(s->new ModalTransition<>(s,new CALabel(s.getRank(),0,new RequestAction("forbidden")),s,Modality.URGENT))
//				.collect(Collectors.toSet()));
//	}

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

		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> maze = loadFile("io/github/contractautomata/maze/twoagentsproblem/resources/maze3.png",true);// pdc.importMSCA(dir+"maze3.png");

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
								Automaton<String,Action,State<String>,ModalTransition<String,Action,State<String>,CALabel>>> ac = pdc; //json?jdc:pdc;
						ac.exportMSCA(dir+"twoagentsimages/png/"+JSonConverter.getstate.apply(aut_s), mazewithagents);
					} catch (Exception e) {
						RuntimeException re = new RuntimeException();
						re.addSuppressed(e);
						throw re;
					}
				});
	}


	private static Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> readVoxLogicaOutputAndMarkStates(Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut,
																																				   String initialkey, String forbiddenkey, boolean firstexperiment) throws IOException {
		System.out.println("reading voxlogica computed file");
		//parse the voxlogica json output and extract the information about initial, final and forbidden states
		String content = Files.readAllLines(Paths.get(dir+"voxlogicaoutput/experiment2.json"))
				.stream()
				.collect(Collectors.joining(" "));
		JSONArray obj = new JSONArray(content);

		final Set<String> initialstate = extractFromJSON(obj, o-> o.getString("filename").contains("Close") && o.getJSONObject("results").getBoolean(initialkey));
		if (initialstate.size()!=1)
			throw new IllegalArgumentException();

		final Set<String> finalstates = extractFromJSON(obj,o->o.getJSONObject("results").getBoolean(firstexperiment?"final":forbiddenkey));
		final Set<String> forbiddenstates = extractFromJSON(obj,o->o.getJSONObject("results").getBoolean(firstexperiment?forbiddenkey:"final"));

		finalstates.removeAll(forbiddenstates); //final states cannot be forbidden

		//System.out.println(finalstates);

		BiPredicate<State<String>,Set<String>> pred =  (s,set) -> set.parallelStream()
				.anyMatch(x->x.equals(JSonConverter.getstate.apply(s)));

		System.out.println("Updating the automaton");

		//reset initial and final states to false
		RelabelingOperator<String,CALabel> ro = new RelabelingOperator<>(CALabel::new,x->x,x->false,x->false);


		System.out.println("Reset initial and final states, and selected agents to uncontrollable");
		//turn the moves of the opponent to uncontrollable
		Set<ModalTransition<String, Action, State<String>, CALabel>> setr = ro.apply(aut).parallelStream()
				.map(t->new ModalTransition<>(t.getSource(),t.getLabel(),t.getTarget(),
						(t.getLabel().getContent().get(3) instanceof IdleAction)?
								(firstexperiment?Modality.PERMITTED: ModalTransition.Modality.URGENT) //first experiment: agents controllable, gate uncontrollable
								:(firstexperiment?Modality.URGENT: ModalTransition.Modality.PERMITTED)//second experiment: agents uncontrollable, gate controllable

				))
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

	private static Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>> loadFile(String filename, boolean image) throws IOException, ParserConfigurationException, SAXException {
		InputStream in = AppMazeTwoAgents.class.getClassLoader().getResourceAsStream(filename);
		File f = new File(filename);
		FileUtils.copyInputStreamToFile(in, f);

		Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>, CALabel>>  aut = (image?pdc:dc).importMSCA(filename);
		f.delete();
		return aut;
	}

}

