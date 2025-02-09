package io.github.contractautomata.maze.twoagentsproblem;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.Label;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.state.BasicState;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.converters.AutDataConverter;
import io.github.contractautomata.catlib.operations.*;
import io.github.contractautomata.maze.converters.PngConverter;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AppGenerateInfo {


    private final static PngConverter pdc = new PngConverter();
    private static String path_to_image = System.getProperty("user.dir")+"/../IdeaProjects/CATLib_PngConverter/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/trainExample.png";
    private final static String dir = System.getProperty("user.dir")+"/../IdeaProjects/CATLib_PngConverter/src/main/java/io/github/contractautomata/maze/twoagentsproblem/resources/";
    private final static AutDataConverter<CALabel> dc = new AutDataConverter<>(CALabel::new);

    public static void main(String[] args) throws IOException {
//         Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> strategy = dc.importMSCA(dir + "strategy_twoagents_maze3.data");
//        // printCut(strategy);
//
//        Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> shortest = shortestPath(strategy);
//        dc.exportMSCA(dir + "strategyShortest", shortest );

        printAutomataInfo();
    }


    private static void printAutomataInfo() throws IOException {
//        Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>>  railway = pdc.importMSCA(path_to_image);
//        dc.exportMSCA(dir+"agentRailway",railway);
         List.of("agent","agentRailway","composition_1","composition_2","composition_3", "marked_1","marked_2","marked_3","strategy_1","strategy_3")
                .forEach(s->{
                    try {
                        Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut = dc.importMSCA(dir+s+ ".data");
                        System.out.println(s + " has "+aut.getTransition().size() + " transitions and "+aut.getStates().size()+" states.");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    };});
    }

    private static void printCut( Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> strategy) throws IOException {
        Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut = dc.importMSCA(dir + "twoagents_maze_marked_firstexp.data");

        //all transitions in aut with source state in strategy and target state not in strategy

        Set<String> strategyStates = strategy.getStates()
                .parallelStream()
                .map(State::toString)
                .collect(Collectors.toSet());

        Set<ModalTransition<String,Action,State<String>,CALabel>> liables = aut.getTransition().parallelStream()
                .filter(t->strategyStates.contains(t.getSource().toString()) && !strategyStates.contains(t.getTarget().toString()))
                .collect(Collectors.toSet());

        System.out.println(liables.stream()
                .map(ModalTransition::toString).collect(Collectors.joining("\n")));

    }


    private static Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> shortestPath(Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> strategy){
        Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> temp;

        int bound=0;
        MSCACompositionFunction<String> comp = new MSCACompositionFunction<>(List.of(strategy),t->false);
        do{
            bound++;
            temp= comp.apply(bound);
        } while(temp==null);

        //remove dangling states

        final int size = bound+1;

        temp = new Automaton<>(temp.getTransition()
                .parallelStream()
                .map(t->new ModalTransition<>(t.getSource(),t.getLabel(),t.getTarget(), ModalTransition.Modality.PERMITTED))
                .collect(Collectors.toSet()));

        List<State<String>> states = IntStream.range(0,size)
                .mapToObj(i->new State<String>(List.of(new BasicState<>(i+"",i==0,i==size-1))))
                .collect(Collectors.toList());

        Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,Label<Action>>> unfolder = new Automaton<>(
                IntStream.range(0,size-1)
                        .mapToObj(i->
                                List.of("godown","goup","goleft","goright","open","close","start","stop")//all actions
                                        .stream()
                                        .map(act->new ModalTransition(states.get(i),new Label<>(List.of(new Action(act))), states.get(i+1), ModalTransition.Modality.PERMITTED)))
                        .flatMap(Function.identity())
                        .collect(Collectors.toSet()));

        //    System.out.println(temp);
        return new MpcSynthesisOperator<String>(l->true,unfolder).apply(temp);

    }
}
