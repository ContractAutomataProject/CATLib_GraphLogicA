package io.github.contractautomata.maze.converters;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.label.action.OfferAction;
import io.github.contractautomata.catlib.automaton.label.action.RequestAction;
import io.github.contractautomata.catlib.automaton.state.BasicState;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.converters.AutConverter;
import org.json.JSONArray;
import org.json.JSONObject;


/**
 * Import JSon format (used by GraphLogicA tool).
 *
 * @author Davide Basile
 *
 */
public class JSonConverter implements AutConverter<Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>,Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>> {
	private final static Predicate<String> isTopLeftCorner = s -> s.equals("0")||s.equals("(0, 0, 0)");


	public static final Function<State<String>,String> getstate = ca ->
			ca.getState().stream()
					.map(x->x.getState().split("_")[0].replaceAll(";", ","))
					.collect(Collectors.joining(","));

	@Override
	public Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> importMSCA(String filename) throws IOException {

		if (!filename.endsWith(".json"))
			throw new IllegalArgumentException("Not a .json format");


		String content = Files.readAllLines(Paths.get(filename)).stream()
				.collect(Collectors.joining(" "));

		JSONObject obj = new JSONObject(content);

		JSONArray nodes = obj.getJSONArray("nodes");

		Map<String, State<String>> id2ct = IntStream.range(0, nodes.length())
				.mapToObj(nodes::getJSONObject)
				.collect(Collectors.toMap(n->n.getString("id"), n-> {
					JSONArray atoms = n.getJSONArray("atoms");
					String label=n.getString("id").replace(",", ";")+"_"+IntStream.range(0,atoms.length())
							.mapToObj(atoms::getString)
							.collect(Collectors.joining("_"));
					return new State<>(
							Collections.singletonList(new BasicState<>(label,
									isTopLeftCorner.test(n.getString("id")),
									isTopLeftCorner.test(n.getString("id"))))//,0, 0
					);
				}));

		JSONArray arcs = obj.getJSONArray("arcs");

		return new Automaton<>(IntStream.range(0, arcs.length())
				.mapToObj(arcs::getJSONObject)
				.map(n-> new ModalTransition<>(
						id2ct.get(n.getString("source")),
						new CALabel(computeLabel(id2ct.get(n.getString("source")),id2ct.get(n.getString("target")))),
						id2ct.get(n.getString("target")),
						ModalTransition.Modality.PERMITTED))
				.collect(Collectors.toSet()));
	}

	private List<Action> computeLabel(State<String> source, State<String> target) {
		String regex = "\\(([0-9]*);\\s([0-9]*);\\s([0-9]*)\\)(.)*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(source.getState().get(0).getState());
		System.out.println(source);
		matcher.find();

		int x_source = Integer.parseInt(matcher.group(1));
		int y_source = Integer.parseInt(matcher.group(2));
		int z_source = Integer.parseInt(matcher.group(3));

		matcher = pattern.matcher(target.getState().get(0).getState());
		matcher.find();

		int x_target = Integer.parseInt(matcher.group(1));
		int y_target = Integer.parseInt(matcher.group(2));
		int z_target = Integer.parseInt(matcher.group(3));

		if (x_source==x_target+1)
			return List.of(new RequestAction("goleft"));
		else if (x_source==x_target-1)
			return List.of(new RequestAction("goright"));
		else if (y_source==y_target+1)
			return List.of(new RequestAction("goup"));
		else if (y_source==y_target-1)
			return List.of(new RequestAction("godown"));
		else if (z_source==z_target+1)
			return List.of(new RequestAction("gobackward"));
		else if (z_source==z_target-1)
			return List.of(new RequestAction("goforward"));
		else
			return List.of(new OfferAction("dummy"));
	}

	@Override
	public void exportMSCA(String filename, Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut) throws IOException {
		JSONObject file = new JSONObject();

		final Function<State<String>,String> getattr = ca ->
				ca.getState().stream()
						.map(x->x.getState().split("_"))
						.filter(x->x.length>1)
						.map(x->x[1])
						.collect(Collectors.joining(","));

		JSONArray nodes = new JSONArray();
		aut.getStates()
				.forEach(ca->{
					JSONObject node = new JSONObject();
					node.put("id", getstate.apply(ca));
					node.append("atoms", getattr.apply(ca));
					nodes.put(node);
				});

		file.put("nodes", nodes);

		JSONArray arcs = new JSONArray();
		aut.getTransition()
				.forEach(t->{
					JSONObject tr = new JSONObject();
					tr.put("source", getstate.apply(t.getSource()));
					tr.put("target", getstate.apply(t.getTarget()));
					arcs.put(tr);
				});
		file.put("arcs", arcs);

		if (filename.isEmpty())
			throw new IllegalArgumentException("Empty file name");

		String suffix=(filename.endsWith(".json"))?"":".json";
		try (PrintWriter pr = new PrintWriter(filename+suffix))
		{
			file.write(pr);
			pr.flush();
		}
	}
}