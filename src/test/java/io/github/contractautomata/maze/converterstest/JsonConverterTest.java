package io.github.contractautomata.maze.converterstest;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.automaton.transition.Transition;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.maze.converters.JSonConverter;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class JsonConverterTest {
	private final AutDataConverter<CALabel> bdc = new AutDataConverter<>(CALabel::new);
	private final String dir = Paths.get(System.getProperty("user.dir")).getParent()
			+"/CATLib_PngConverter/src/test/java/io/github/contractautomata/maze/resources/";
	
	@Test
	public void importTest() throws Exception {
		JSonConverter jc = new JSonConverter();
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut = jc.importMSCA(dir+"maze_nosolution.json");
	    bdc.exportMSCA(dir+"maze_nosolution", aut);

		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut2 = bdc.importMSCA(dir+"maze_nosolution.data");
	    assertEquals(autEquals(aut, aut2),true);
	}
	
	@Test
	public void exportTest() throws Exception {
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut = bdc.importMSCA(dir+"maze_nosolution.data");
		JSonConverter jc = new JSonConverter();
		jc.exportMSCA(dir+"maze_nosolution.json",aut);
		Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut2 = jc.importMSCA(dir+"maze_nosolution.json");
		assertEquals(autEquals(aut, aut2),true);
	}

	public static boolean autEquals(Automaton<?,?,?,?> aut, Automaton<?,?,?,?>  test) {
		Set<String> autTr=aut.getTransition().parallelStream()
				.map(Transition::toString)
				.collect(Collectors.toSet());
		Set<String> testTr=test.getTransition().parallelStream()
				.map(Transition::toString)
				.collect(Collectors.toSet());

		return autTr.parallelStream()
				.allMatch(testTr::contains)
				&&
				testTr.parallelStream()
						.allMatch(autTr::contains);
	}



}
