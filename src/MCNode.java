//package MCTS;

import java.util.Random;

/**
 * A single node of a Monte Carlo tree.
 * 
 * @author Jared Prince
 * @version 1.0
 * @since 1.0
 *
 * 
 */

public class MCNode {

	/**
	 * Random number generator used to get a random action during ties.
	 */
	public static Random r = new Random();

	/**
	 * The state represented by this node.
	 */
	public GameState state;

	/**
	 * The number of times this node has been reached - N(s)
	 */
	public int timesReached;

	/**
	 * The depth of this node in a full tree. Usually equivalent to the number
	 * of moves made to reach this state.
	 */
	public int depth;

	/**
	 * The number of nodes of which this node is a child. This is used to delete
	 * branches of the tree without removing nodes who are children of other
	 * nodes. It is necessary because the nodes are contained in a Hashtable, so
	 * just deleting all parents does not destroy the node.
	 */
	public int parents = 0;

	/**
	 * True if this node is a leaf (has no children).
	 */
	public boolean isLeaf = true;

	/**
	 * An array representing the possible moves from this node.
	 */
	public ActionLink[] links;

	/**
	 * Constructor for MCNode - only providing the games state; e.g. to use
	 *    with the findNode method in the MCTree class
	 *
	 * @param state The state represented by this node
	**/
	public MCNode(GameState state) {
		tree = null;

		this.state = state;
		depth = -1;

		timesReached = 1;

		links = null;

	}	

	/**
	 *  @param current The MCNode with the current state
 	 *  @param edges The array of integers for the edges to be added
  	 *  @return The MCNode with the state with the edge added
	**/
	public static MCNode getStateAfterActions (MCNode current, int [] edges, int maxEdges) {
		long newState = current.state.longState;
                for (int edge : edges) {
              //      System.out.print (" adding edge " + edge + " ");
                    long toAdd = (1 << (maxEdges - edge -1));
              //      System.out.print (toAdd + " ");
                    newState = newState | toAdd;
              //      System.out.println (" results in " + newState);
                }
		
		return new MCNode(new GameState(newState));
    }
	
	/**
	 * The tree to which this node belongs. Used to search for a node before an
	 * equivalent one is created. Also used to update tree statistics, including
	 * number of nodes and average depth.
	 */
	public MCTree tree;

	/**
	 * Constructor for the MCNode.
	 * 
	 * @param state
	 *            The state represented by this node.
	 * @param depth
	 *            The depth in the tree of this node.
	 * @param actions
	 *            The array of possible actions from this node.
	 * @param tree
	 *            The tree to which this node belongs.
	 */
	public MCNode(GameState state, int depth, int[] actions, MCTree tree) {
		this.tree = tree;

		this.state = state;
		this.depth = depth;

		timesReached = 1;

		links = new ActionLink[actions.length];

		for (int i = 0; i < links.length; i++) {
			links[i] = new ActionLink(actions[i], null);
		}
	}

	/**
	 * Gets the next action based on the average result Q(s,a) and the
	 * uncertainty bonus.
	 * 
	 * @param c
	 *            The uncertainty constant to be applied when calculating the
	 *            bonuses of each action.
	 * @return An integer representing the action selected.
	 */
	public int getNextAction(double c) {

		/* By default, the links are sorted in order by value + bonus 
		IS NOT TRUE FOR THE PARALLELIZED VERSION */
		//if (c > 0) {
		//	return links[0].action;
		//}

		int action = -1;
		double max = -50;

		/* find the action with the largest average reward W(s,a) */
		/* determine whether bonus should be applied */
		boolean applyBonus = false;
		if (c > 0)
		  applyBonus = true;
		for (int i = 0; i < links.length; i++) {
			
			// make sure that bias is applied if c > 0
			double val = links[i].getValue(applyBonus);

			/*
			 * Equal actions should be chosen semi-randomly. Apart from the
			 * first few times actions are chosen, two values should almost
			 * never be equal. The probability of more than two equal values is
			 * vanishingly small, so there are assumed to be only ties of two.
			 */
			if (val > max || (val == max && r.nextDouble() < .5)) {
				max = val;
				action = links[i].action;
			}
		}

		return action;
	}
	
	//get an array of all remaining actions
	public int[] getArrayOfActions() {
		int[] allActions = new int[links.length];
		
		for(int i = 0; i<links.length; i++)
			allActions[i] = links[i].action;
		
		return allActions;
	}

	/**
	 * Gets the successor of this node based on the given action.
	 * 
	 * @param action
	 *            An integer representing the action to be made.
	 * @param behavior
	 *            Defines under which conditions a node is created.
	 * 
	 * @return The successor or null.
	 */
	public MCNode getNode(int action, int behavior) {

		/* check every action to find the one specified */
		for (int i = 0; i < links.length; i++) {

			/* Get the corresponding child */
			if (links[i].action == action) {

				if (links[i].child != null) {
					return links[i].child;
				}

				/* Create a new node */
				else if (behavior == MonteCarloTreeSearch.BEHAVIOR_EXPANSION_ALWAYS
						|| (links[i].timesChosen == MCTree.NODE_CREATION_COUNT
								&& behavior == MonteCarloTreeSearch.BEHAVIOR_EXPANSION_STANDARD)) {

					MCNode newNode = getNextNode(action);
					links[i].child = tree.addNode(newNode);

					if (!isLeaf) {
						isLeaf = true;
						tree.leaves--;
					}

					return links[i].child;
				}
			}
		}

		return null;
	}

	/**
	 * Gets the successor of this node based on the state and action given.
	 * 
	 * @param state
	 *            The state equivalent to the one needed.
	 * @param behavior
	 *            Defines under what condition a node is created.
	 * @return The successor or null.
	 * 
	 */
	public MCNode getNode(GameState state, int behavior) {

		String canon = ((DotsAndBoxes) tree.game).removeSymmetries(state).getString();

		/* check every action to find the one specified */
		for (int i = 0; i < links.length; i++) {

			/* Get the corresponding child */
			if (links[i].child != null) {

				// if the asymmetrical state is the same as the canon of the
				// state given
				if (links[i].child.state.getString().equals(canon)) {
					return links[i].child;
				}
			}

			else {

				GameState linkState = tree.game.getSuccessorState(state, links[i].action);

				// if the nonsymmetrical state is the same as the canon of the
				// state given
				if (((DotsAndBoxes) tree.game).removeSymmetries(linkState).getString().equals(canon)) {

					/* Create a new node */
					if (behavior == MonteCarloTreeSearch.BEHAVIOR_EXPANSION_ALWAYS
							|| (links[i].timesChosen == MCTree.NODE_CREATION_COUNT
									&& behavior == MonteCarloTreeSearch.BEHAVIOR_EXPANSION_STANDARD)) {

						MCNode newNode = getNextNode(links[i].action);
						links[i].child = tree.addNode(newNode);

						if (isLeaf) {
							isLeaf = false;
							tree.leaves--;
						}

						return links[i].child;
					}
				}
			}
		}

		return null;
	}

	/**
	 * Checks if this node is equivalent to another. For the purpose of this
	 * method, two nodes are equal is their states are equal.
	 * 
	 * @param p
	 *            The node to be compared to this one.
	 * @return True if the nodes are equivalent, false otherwise.
	 */
	public boolean equals(MCNode p) {
		return p.state.equals(this.state);
	}

	/**
	 * Adds the given reward to the total rewards for an action.
	 * 
	 * @param action
	 *            An integer representing the action selected.
	 * @param value
	 *            The reward to be added.
	 * @param c
	 *            The uncertainty constant to be applied to the updated bonus.
	 */
	public void addValue(int action, int value, double c) {
		timesReached++;

		/* find the index of the action */
		int index = -1;
		for (int i = 0; i < links.length; i++) {
			if (links[i].action == action) {
				index = i;
				break;
			}
		}

		links[index].update(value);

		/* update the bonuses and reorder the list */
		for (int i = 0; i < links.length; i++) {
			links[i].updateBonus(timesReached, c);

//			sortLink(links, i);
		}
	}
	
	/**
	 * Sorts an updated value into an already sorted array starting with its current index.
	 * 
	 * @param links The array being sorted.
	 * @param i The index of the element to sort.
	 */
	public static void sortLink(ActionLink[] links, int i){

		/* move link up the queue while it's value is greater than the link
		 * before it
		 */
		while (i > 0 && links[i].getValue(true) > links[i - 1].getValue(true)) {
			ActionLink tempLink = links[i];
			links[i] = links[i - 1];
			links[i - 1] = tempLink;

			i--;
		}

		/*
		 * move link down the queue while it's value is less than the link
		 * after it
		 */
		while (i < links.length - 1 && links[i].getValue(true) < links[i + 1].getValue(true)) {
			ActionLink tempLink = links[i];
			links[i] = links[i + 1];
			links[i + 1] = tempLink;

			i++;
		}
	}

	/**
	 * Creates a new node which is the successor of this node given an action.
	 * 
	 * @param action
	 *            An integer representing the action selected.
	 * @return The newly created node.
	 */
	private MCNode getNextNode(int action) {
		GameState newState = tree.game.getSuccessorState(state, action);
		return new MCNode(newState, depth + 1, tree.game.getActions(newState), tree);
	}

	/**
	 * Decrements the count of parents for all children of this node.
	 */
	public void delinkChildren() {
		for (int i = 0; i < links.length; i++) {
			if (links[i].child != null) {
				links[i].child.parents--;
			}
		}
	}

	/**
	 * Merges this node with another.
	 * 
	 * Assumptions: The two nodes are equivalent (have equivalent states). When
	 * both nodes have pointers for a child, the pointers are to the same child,
	 * not just an equivalent one.
	 * 
	 * The primitive values of the given node are combined with this one.
	 * Objects that exist in both nodes are kept from this node. Objects which
	 * exist in the given node but not this one are added.
	 * 
	 * It is the duty of the tree to merge the nodes recursively (deepest nodes
	 * first) to ensure that all links point to the same children.
	 *
	 * @param node
	 *            The node with which to be merged.
	 */
	public void mergeNode(MCNode node) {
		if (node == null) {
			return;
		}

		timesReached += node.timesReached;
		isLeaf = (isLeaf && node.isLeaf);

		for (int i = 0; i < links.length; i++) {
			links[i].merge(node.links[i]);
		}
	}
	
	
	
	/*----------------------------------Parallel MCTS-------------------------------------------*/
	
	//returns the array of number of times a link was chosen for each action link
	public int[] getTimesActionChosen() {
		int[] timesActionChosen = new int[links.length];
		for(int i=0; i<timesActionChosen.length; i++){
			timesActionChosen[i]= links[i].getTimesChosen();
		}
		return timesActionChosen;
	}
	
	//returns the array of each reward/child average for each action link
	public double[] getRewards() {
		double[] rewards = new double[links.length];
		for(int i=0; i< rewards.length; i++){
			rewards[i]= links[i].getRewards();
		}
		return rewards;
	}
	
	public double getTotalRewards() {
		double[] rewards = getRewards();
		double sum = 0;
		for(double reward: rewards)
			sum+=reward;
		return sum;
	}
	
	public void setRewards(double[] rewardsToSet){
		for(int i=0; i<links.length; i++){
			links[i].rewards= rewardsToSet[i];
		}
	}
	
	public void setTimesActionChosen(int[] timesActionChosen){
		for(int i=0; i<links.length; i++){
			links[i].timesChosen= timesActionChosen[i];
		}
	}
	
	/*------------------------------------------------------------------------------------------*/

	/**
	 * Represents a single possible action from the parent node.
	 * 
	 * @author Jared Prince
	 * @version 1.0
	 * @since 1.0
	 */
	public class ActionLink {

		/**
		 * An integer representing the action.
		 */
		int action;

		/**
		 * The number of times this action was chosen.
		 */
		int timesChosen = 0;

		/**
		 * The total rewards resulting from selecting this action.
		 */
		double rewards;

		/**
		 * The bonus applied to the average u(s, a).
		 */
		double bonus = 1;

		/**
		 * The successor node of the parent after this action is made.
		 */
		MCNode child;

		/**
		 * Constructor for the ActionLink.
		 * 
		 * @param action
		 *            An integer representing the action of this link.
		 * @param child
		 *            The successor node of the parent after this action is
		 *            made.
		 */
		public ActionLink(int action, MCNode child) {
			this.child = child;
			this.action = action;
		}

		public double getRewards() {
			return rewards;
		}

		public int getTimesChosen() {
			return timesChosen;
		}

		/**
		 * Updates the node with a given reward.
		 * 
		 * @param reward
		 *            The reward to be added.
		 */
		public void update(int reward) {
			this.rewards += reward;
			timesChosen++;
		}
		
		public void synchUpdate(double qVal, int numTotalChosen, double cVal) {
			this.rewards = qVal * this.timesChosen;
			this.updateBonus(numTotalChosen, cVal);
		}
		

		/**
		 * Updates the bonus of this action.
		 * 
		 * @param timesReached
		 *            The number of times the parent node was reached.
		 * @param c
		 *            The uncertainty constant to be applied to the bonus.
		 */
		public void updateBonus(int timesReached, double c) {
			this.bonus = c * Math.sqrt(Math.log(timesReached) / timesChosen);
		}

		/**
		 * Gets the value of the action.
		 * 
		 * @param applyBonus
		 *            True if the uncertainty bonus should be applied, false
		 *            otherwise.
		 * @return The total value of this action.
		 */
		public double getValue(boolean applyBonus) {
			if (timesChosen == 0) {
				return bonus;
			}

			return (rewards / timesChosen) + (applyBonus ? bonus : 0);
		}

		/**
		 * Merges this link with another.
		 * 
		 * @param link
		 *            The link with which to be merged.
		 */
		public void merge(ActionLink link) {
			if (child == null && link.child != null) {
				child = link.child;
			}

			timesChosen += link.timesChosen;
			rewards += link.rewards;
		}
	}
}
