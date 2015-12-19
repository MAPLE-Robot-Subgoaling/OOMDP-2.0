package burlap.behavior.singleagent.planning.deterministic.uninformed.dfs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import burlap.behavior.singleagent.planning.deterministic.DeterministicPlanner;
import burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy;
import burlap.behavior.singleagent.planning.deterministic.SearchNode;
import burlap.debugtools.DPrint;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.auxiliary.stateconditiontest.StateConditionTest;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.core.states.State;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import burlap.oomdp.statehashing.HashableState;
import burlap.oomdp.statehashing.HashableStateFactory;

/**
 * Implements depth-first search. A maxDepth may be specified will will stop
 * descent if it is crossed, and cause the valueFunction to roll back. This
 * implementation of DFS will not explore any path that yields a state that is
 * already on the current path. Optionally, it can be set to maintain a closed
 * which, which means it will not expand nodes that it has already visited in
 * its search, even if they were explored outside of the current path. However,
 * enabling the closed list removes the linear space complexity of DFS and makes
 * it exponential like BFS. Finally, this implementation of DFS has some special
 * support for options in which it can be set to always explore paths that are
 * generated by options first; this heuristic is useful if it is expected that
 * options are more likely to lead to the goal state.
 * 
 * <p/>
 * If a terminal function is provided via the setter method defined for OO-MDPs,
 * then the search algorithm will not expand any nodes that are terminal states,
 * as if there were no actions that could be executed from that state. Note that
 * terminal states are not necessarily the same as goal states, since there
 * could be a fail condition from which the agent cannot act, but that is not
 * explicitly represented in the transition dynamics.
 * 
 * @author James MacGlashan
 * 
 */
public class DFS extends DeterministicPlanner {

	/**
	 * The max depth of the search tree that will be explored. -1 indicates that
	 * there is no limit.
	 */
	protected int maxDepth;

	/**
	 * Whether to keep track of a closed list to prevent exploring already seen
	 * nodes.
	 */
	protected boolean maintainClosed;

	/**
	 * Whether to explore paths generated by options first
	 */
	protected boolean optionsFirst;

	/**
	 * A random object for random walks
	 */
	protected Random rand;

	/**
	 * Planning statistic for keeping track of how many nodes DFS expanded.
	 * States that are visited multiple times will be counted each time they are
	 * visited.
	 */
	protected int numVisted;

	/**
	 * Basic constructor for standard DFS without a depth limit
	 * 
	 * @param domain
	 *            the domain in which to plan
	 * @param gc
	 *            indicates the goal states
	 * @param hashingFactory
	 *            the state hashing factory to use
	 */
	public DFS(Domain domain, StateConditionTest gc,
			HashableStateFactory hashingFactory) {
		this.DFSInit(domain, new NullTermination(), gc, hashingFactory, -1,
				false, false);
	}

	/**
	 * Basic constructor for standard DFS with a depth limit
	 * 
	 * @param domain
	 *            the domain in which to plan
	 * @param gc
	 *            indicates the goal states
	 * @param hashingFactory
	 *            the state hashing factory to use
	 * @param maxDepth
	 *            depth limit of DFS. -1 specifies no limit.
	 */
	public DFS(Domain domain, StateConditionTest gc,
			HashableStateFactory hashingFactory, int maxDepth) {
		this.DFSInit(domain, new NullTermination(), gc, hashingFactory,
				maxDepth, false, false);
	}

	/**
	 * Constructor of DFS with specification of depth limit and whether to
	 * maintain a closed list that affects exploration.
	 * 
	 * @param domain
	 *            the domain in which to plan
	 * @param gc
	 *            indicates the goal states
	 * @param hashingFactory
	 *            the state hashing factory to use
	 * @param maxDepth
	 *            depth limit of DFS. -1 specifies no limit.
	 * @param maintainClosed
	 *            whether to maintain a closed list or not
	 */
	public DFS(Domain domain, StateConditionTest gc,
			HashableStateFactory hashingFactory, int maxDepth,
			boolean maintainClosed) {
		this.DFSInit(domain, new NullTermination(), gc, hashingFactory,
				maxDepth, maintainClosed, false);
	}

	/**
	 * Constructor of DFS with specification of depth limit, whether to maintain
	 * a closed list that affects exploration, and whether paths generated by
	 * options should be explored first.
	 * 
	 * @param domain
	 *            the domain in which to plan
	 * @param gc
	 *            indicates the goal states
	 * @param hashingFactory
	 *            the state hashing factory to use
	 * @param maxDepth
	 *            depth limit of DFS. -1 specifies no limit.
	 * @param maintainClosed
	 *            whether to maintain a closed list or not
	 * @param optionsFirst
	 *            whether to explore paths generated by options first.
	 */
	public DFS(Domain domain, StateConditionTest gc,
			HashableStateFactory hashingFactory, int maxDepth,
			boolean maintainClosed, boolean optionsFirst) {
		this.DFSInit(domain, new NullTermination(), gc, hashingFactory,
				maxDepth, maintainClosed, optionsFirst);
	}

	/**
	 * Runs DFS from a given search node, keeping track of its current depth.
	 * This method is recursive.
	 * 
	 * @param n
	 *            the current search node
	 * @param depth
	 *            the current depth of the search
	 * @param statesOnPath
	 *            the states that have bee explored on the current search path
	 * @return the SearchNode with a goal, or null if it cannot be found from
	 *         this state.
	 */
	protected SearchNode dfs(SearchNode n, int depth,
			Set<HashableState> statesOnPath) {

		numVisted++;

		if (gc.satisfies(n.s.s)) {
			// found goal!
			return n;
		}

		if (maxDepth != -1 && depth > maxDepth) {
			return null; // back track
		}

		if (this.tf.isTerminal(n.s.s)) {
			return null; // treat like dead end
		}

		// otherwise we need to generate successors and search them

		statesOnPath.add(n.s);

		// shuffle actions for a random walk, but keep options as priority if
		// set that way
		List<GroundedAction> gas = this.getAllGroundedActions(n.s.s);
		if (optionsFirst) {
			int no = this.numOptionsInGAs(gas);
			this.shuffleGroundedActions(gas, 0, no);
			this.shuffleGroundedActions(gas, no, gas.size());
		} else {
			this.shuffleGroundedActions(gas, 0, gas.size());
		}

		// generate a search successors from the order of grounded actions
		for (GroundedAction ga : gas) {
			HashableState shp = this.stateHash(ga.executeIn(n.s.s));
			if (!statesOnPath.contains(shp)) {
				SearchNode snp = new SearchNode(shp, ga, n);
				SearchNode result = this.dfs(snp, depth + 1, statesOnPath);
				if (result != null) {
					return result;
				}
			}
		}

		// no successors found a solution
		if (!maintainClosed) {
			statesOnPath.remove(n.s);
		}
		return null;
	}

	/**
	 * Constructor of DFS with specification of depth limit, whether to maintain
	 * a closed list that affects exploration, and whether paths generated by
	 * options should be explored first.
	 * 
	 * @param domain
	 *            the domain in which to plan
	 * @param gc
	 *            indicates the goal states
	 * @param hashingFactory
	 *            the state hashing factory to use
	 * @param maxDepth
	 *            depth limit of DFS. -1 specifies no limit.
	 * @param maintainClosed
	 *            whether to maintain a closed list or not
	 * @param optionsFirst
	 *            whether to explore paths generated by options first.
	 */
	protected void DFSInit(Domain domain, TerminalFunction tf,
			StateConditionTest gc, HashableStateFactory hashingFactory,
			int maxDepth, boolean maintainClosed, boolean optionsFirst) {
		this.deterministicPlannerInit(domain, new UniformCostRF(), tf, gc,
				hashingFactory);
		this.maxDepth = maxDepth;
		this.maintainClosed = maintainClosed;
		if (optionsFirst) {
			this.setOptionsFirst();
		}

		rand = RandomFactory.getMapped(0);
	}

	/**
	 * Returns the number of search nodes visited/expanded. If a state was
	 * visited multiple times in a search, it will be counted multiple times.
	 * 
	 * @return the number of search nodes visited/expanded
	 */
	public int getNumVisited() {
		return numVisted;
	}

	/**
	 * Returns the number of options present in a list of possible actions.
	 * 
	 * @param gas
	 *            a list of possible actions
	 * @return the number of options present in a list of possible actions.
	 */
	protected int numOptionsInGAs(List<GroundedAction> gas) {
		for (int i = 0; i < gas.size(); i++) {
			if (gas.get(i).action.isPrimitive()) {
				return i;
			}
		}
		return gas.size();
	}

	/**
	 * Plans and returns a
	 * {@link burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy}
	 * . If a {@link burlap.oomdp.core.states.State} is not in the solution path
	 * of this planner, then the
	 * {@link burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy}
	 * will throw a runtime exception. If you want a policy that will
	 * dynamically replan for unknown states, you should create your own
	 * {@link burlap.behavior.singleagent.planning.deterministic.DDPlannerPolicy}
	 * .
	 * 
	 * @param initialState
	 *            the initial state of the planning problem
	 * @return a
	 *         {@link burlap.behavior.singleagent.planning.deterministic.SDPlannerPolicy}
	 *         .
	 */
	@Override
	public SDPlannerPolicy planFromState(State initialState) {

		if (optionsFirst) {
			this.sortActionsWithOptionsFirst();
		}

		numVisted = 0;

		HashableState sih = this.stateHash(initialState);

		if (mapToStateIndex.containsKey(sih)) {
			return new SDPlannerPolicy(this); // no need to plan since this is
												// already solved
		}

		Set<HashableState> statesOnPath = new HashSet<HashableState>();
		SearchNode sin = new SearchNode(sih);
		SearchNode result = this.dfs(sin, 0, statesOnPath);

		if (result != null) {
			this.encodePlanIntoPolicy(result);
		}

		DPrint.cl(debugCode, "Num visted: " + numVisted);

		return new SDPlannerPolicy(this);

	}

	@Override
	public void resetSolver() {
		super.resetSolver();
		this.numVisted = 0;
	}

	/**
	 * Sets the valueFunction to explore nodes generated by options first.
	 */
	public void setOptionsFirst() {

		optionsFirst = true;

		List<Action> optionOrdered = new ArrayList<Action>();

		for (Action a : actions) {
			if (!a.isPrimitive()) {
				optionOrdered.add(a);
			}
		}

		for (Action a : actions) {
			if (a.isPrimitive()) {
				optionOrdered.add(a);
			}
		}

		actions = optionOrdered;

	}

	/**
	 * Shuffles the order of actions on the index range [s, e)
	 * 
	 * @param gas
	 *            a list of actions
	 * @param s
	 *            the start index from which actions should be shuffled
	 * @param e
	 *            the end index of actions that should be shuffled.
	 */
	protected void shuffleGroundedActions(List<GroundedAction> gas, int s, int e) {

		int r = e - s;

		for (int i = s; i < e; i++) {
			GroundedAction ga = gas.get(i);
			int j = rand.nextInt(r) + s;
			gas.set(i, gas.get(j));
			gas.set(j, ga);
		}

	}

	/**
	 * Reorders the planners action list so that options are in the front of the
	 * list.
	 */
	protected void sortActionsWithOptionsFirst() {
		List<Action> sactions = new ArrayList<Action>(actions.size());
		for (Action a : actions) {
			if (!a.isPrimitive()) {
				sactions.add(a);
			}
		}
		for (Action a : actions) {
			if (a.isPrimitive()) {
				sactions.add(a);
			}
		}

		actions = sactions;
	}

}
