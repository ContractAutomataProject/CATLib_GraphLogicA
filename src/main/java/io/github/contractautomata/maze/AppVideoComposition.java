package io.github.contractautomata.maze;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.label.action.RequestAction;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.catlib.operations.MSCACompositionFunction;
import io.github.contractautomata.catlib.operations.MpcSynthesisOperator;
import io.github.contractautomata.catlib.operations.RelabelingOperator;
import io.github.contractautomata.catlib.requirements.StrongAgreement;
import io.github.contractautomata.maze.converters.PngConverter;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This app generates the frames of a video published at https://youtu.be/08_iok6R9sw
 *
 * This demo shows the usage of the contract automata library and its bounded composition to solve a  maze problem.
 * The maze is an image encoded as an automaton requirement, each pixel is a location and edges are connecting neighbour pixels.
 * The initial location is set to a blue pixel, the final location is the green (top left) pixel, and the walls are forbidden locations.
 * Edges are labelled with requests of moving in a specific direction.
 * A driver automaton is being composed with the maze requirement automaton to produce a solution.
 * The driver offers to move in each direction an unbounded number of steps.
 * The video shows that the composition operator is implemented with a bounded breadth-first visit of the automaton.
 * Each frame of the video is a composition for a specific bound. The red colour indicates the reached portion within bound steps.
 *
 */
public class AppVideoComposition 
{
	public static void main( String[] args ) throws Exception
	{
		PngConverter pdc = new PngConverter();
		AutDataConverter<CALabel> dc = new AutDataConverter<>(CALabel::new);

		System.out.println( "Maze Demo!" );

		String dir = System.getProperty("user.dir")+"/src/test/java/io/github/contractautomata/maze/resources/";
		
		System.out.println("importing...");

		Automaton<String, Action, State<String>, ModalTransition<String, Action, State<String>,CALabel>> maze =
				pdc.importMSCA(dir+"maze2.png");

		//dc.exportMSCA(dir+"maze2.data",maze);
		//MSCA maze = dc.importMSCA(dir+"maze2.aut.data");
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> driver =
				dc.importMSCA(dir+"driver.data");

		System.out.println("resetting states...");

		maze = new Automaton<>(new RelabelingOperator<String,CALabel>(CALabel::new,
				s->s,
				s->s.getState().contains("#0000FF"),//set initial state to a blue node
				s->!s.getState().contains("#000000")) //set all non-black states to final
				.apply(maze));


		//set forbidden states
		Set<ModalTransition<String, Action, State<String>,CALabel>> mazeTr = maze.getTransition();
		mazeTr.addAll(maze.getStates().parallelStream()
				.filter(ca->ca.getState().get(0).getState().contains("#000000"))
				.map(ca->new ModalTransition<>
						(ca,new CALabel(1,0,new RequestAction("forbidden")),ca,
						ModalTransition.Modality.URGENT))
				.collect(Collectors.toSet()));

		maze = new Automaton<>(mazeTr);

		System.out.println("composing...");
		MSCACompositionFunction<String> cf = new MSCACompositionFunction<>(Arrays.asList(maze,driver),
				t->!t.getLabel().isMatch());

		int bound=0;
		do {
			bound++;
			Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> comp = cf.apply(bound);//555);
			System.out.println("saving "+bound);
			ImageIO.write(pdc.overlap(ImageIO.read(new File(dir+"maze2.png")),pdc.getBufferedImage(comp)),
					"png",  
					new File(dir+"video/"+bound+".png"));//"maze2_strategy_overlay.png"));
		} while (!cf.isFrontierEmpty());
	}
}