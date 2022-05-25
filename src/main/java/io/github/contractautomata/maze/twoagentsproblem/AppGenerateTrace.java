package io.github.contractautomata.maze.twoagentsproblem;

import io.github.contractautomata.catlib.automaton.Automaton;
import io.github.contractautomata.catlib.automaton.label.CALabel;
import io.github.contractautomata.catlib.automaton.label.action.Action;
import io.github.contractautomata.catlib.automaton.state.State;
import io.github.contractautomata.catlib.automaton.transition.ModalTransition;
import io.github.contractautomata.catlib.converters.AutDataConverter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

public class AppGenerateTrace {

    private final static String dir = System.getProperty("user.dir")+"/src/test/java/io/github/contractautomata/maze/resources/";
    private final static AutDataConverter<CALabel> dc = new AutDataConverter<>(CALabel::new);

    public static void main(String[] args) throws IOException {
        Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> aut = dc.importMSCA(dir + "twoagents_maze3_marked");
        Automaton<String, Action, State<String>, ModalTransition<String,Action,State<String>,CALabel>> strategy = dc.importMSCA(dir + "strategy_twoagents_maze3");

        //all transitions in aut with source state in strategy and target state not in strategy

        Set<String> strategyStates = strategy.getStates()
                .parallelStream()
                .map(State::toString)
                .collect(Collectors.toSet());

        Set<ModalTransition<String,Action,State<String>,CALabel>> liables = aut.getTransition().parallelStream()
                .filter(t->strategyStates.contains(t.getSource().toString()) && !strategyStates.contains(t.getTarget().toString()))
                .collect(Collectors.toSet());

        System.out.println(liables);
    }
}
