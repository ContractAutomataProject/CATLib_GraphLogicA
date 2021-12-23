package io.github.davidebasile.converters;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.davidebasile.contractautomata.automaton.MSCA;
import io.github.davidebasile.contractautomata.automaton.label.CALabel;
import io.github.davidebasile.contractautomata.automaton.state.BasicState;
import io.github.davidebasile.contractautomata.automaton.state.CAState;
import io.github.davidebasile.contractautomata.automaton.transition.MSCATransition;
import io.github.davidebasile.contractautomata.converters.MSCAConverter;

/**
 * Import JSon format (used by GraphLogicA tool).
 * 
 * @author Davide Basile
 *
 */
public class JSonConverter implements MSCAConverter {
	private static Predicate<String> isTopLeftCorner = s -> s.equals("0")||s.equals("(0, 0, 0)");

	@Override
	public MSCA importMSCA(String filename) throws IOException {

		if (!filename.endsWith(".json"))
			throw new IllegalArgumentException("Not a .json format");

		
		String content = Files.readAllLines(Paths.get(filename)).stream()
				.collect(Collectors.joining(" "));

		JSONObject obj = new JSONObject(content);

		JSONArray nodes = obj.getJSONArray("nodes");	
	
		Map<String, CAState> id2ct = IntStream.range(0, nodes.length())
				.mapToObj(nodes::getJSONObject)
				.collect(Collectors.toMap(n->n.getString("id"), n-> {
					JSONArray atoms = n.getJSONArray("atoms");
					String label=n.getString("id").replace(",", ";")+"_"+IntStream.range(0,atoms.length())
					.mapToObj(atoms::getString)
					.collect(Collectors.joining("_"));
					return new CAState(
							new ArrayList<BasicState>(Arrays.asList(new BasicState(label,isTopLeftCorner.test(n.getString("id")),isTopLeftCorner.test(n.getString("id")))))//,0, 0
							);
				}));

		JSONArray arcs = obj.getJSONArray("arcs");

		return new MSCA(IntStream.range(0, arcs.length())
				.mapToObj(arcs::getJSONObject)
				.map(n-> new MSCATransition(
						id2ct.get(n.getString("source")),
						new CALabel(1,0,computeLabel(id2ct.get(n.getString("source")),id2ct.get(n.getString("target")))),
						id2ct.get(n.getString("target")),
						MSCATransition.Modality.PERMITTED))
				.collect(Collectors.toSet()));
	}
	
	private String computeLabel(CAState source, CAState target) {
		String regex = "\\(([0-9]*);\\s([0-9]*);\\s([0-9]*)\\)(.)*";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(source.getState().get(0).getState());
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
			return "?goleft";
		else if (x_source==x_target-1)
			return "?goright";
		else if (y_source==y_target+1)
			return "?goup";
		else if (y_source==y_target-1)
			return "?godown";
		else if (z_source==z_target+1)
			return "?gobackward";
		else if (z_source==z_target-1)
			return "?goforward";
		else
			return "!dummy";
	}
	
	@Override
	public void exportMSCA(String filename, MSCA aut) throws IOException {	
		JSONObject file = new JSONObject();
		final Function<CAState,String> getstate = ca -> ca.getState().get(0).getState().split("_")[0].replaceAll(";", ",");
		final Function<CAState,String> getattr = ca -> ca.getState().get(0).getState().split("_")[1];

		JSONArray nodes = new JSONArray();
		aut.getStates()
		.forEach(ca->{
			JSONObject node = new JSONObject();
			node.put("id", getstate.apply(ca));
			node.append("attr", getattr.apply(ca));
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
		
		if (filename=="")
			throw new IllegalArgumentException("Empty file name");
		
		String suffix=(filename.endsWith(".json"))?"":"json";
		try (PrintWriter pr = new PrintWriter(filename+suffix))
		{
			file.write(pr);
			pr.flush();
		}
	}
}