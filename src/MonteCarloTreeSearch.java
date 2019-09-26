
import java.math.BigInteger;
import java.util.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import mpi.*;

/**
 * This class runs games using the Monte Carlo tree search.
 * 
 * The amount of game-specific data in this class should be minimal. Only the
 * data that is absolutely necessary (to keep MCTree and MCNode clear of
 * game-specific data) should be used here. Wherever possible, such data should
 * be located in the MCGame subclass and used by the public methods of MCGame
 * (getActons and getSuccessorState).
 * 
 * @author Jared Prince
 * @version 1.0
 * @since 1.0
 */

public class MonteCarloTreeSearch {

	static ArrayList<PlayerMove> playerMove = new ArrayList<PlayerMove>();
	static int[] simulationMoves = {0,0,0,0,0,0,0,0,0,0,0,0};
	/**
	 * Used to randomly pick actions.
	 */
	static Random r = new Random();

	/**
	 * The width (in boxes) of the board.
	 */
	static int width;

	/**
	 * The height (in boxes) of the board.
	 */
	static int height;

	/**
	 * The number of edges on the board.
	 */
	static int edges;

	/**
	 * The uncertainty constant.
	 */
	static double c;

	/**
	 * The game to use for player one.
	 */
	static DotsAndBoxes game = new DotsAndBoxes(2, 2, false, false);

	/**
	 * The game to use for player two.
	 */
	static DotsAndBoxes game2;

	/**
	 * The tree of player one.
	 */
	static MCTree tree;

	/**
	 * The tree of player two.
	 */
	static MCTree tree2;

	/**
	 * A 1D array to hold the first moves of each match to determine a running
	 * average 0: not optimal 1: optimal
	 */
	static int firstMove[];
	static int firstMoveSZ = 0;

	/**
	 * A 2D array representing the times taken for each move made by player one.
	 * Index i is an array of the total time taken by player one during turn i
	 * (in milliseconds) and the total number of times player one took turn i.
	 *  times[i][0] / times[i][1]
	 */
	static long times[][];

	/**
	 * Counters for the 2x2 game: how many times does MCTS learns an outside
	 * edge or note and what happens for an outside edge (losses, draws, wins)
	 * and for an inside edge (l, d, w) must be removed for other board sizes
	 */
	static int badChoice = 0;
	static int goodChoice = 0;
	static int[] startOK = new int[3];
	static int[] startBad = new int[3];

	static int simsDEBUG = -1;
	static int movesCompleted = 0;
	static String errorDir = "";	// director for the error file which 
									// is used to report exceptions etc

	/*
	 * The following constants define the behavior of the search. Variations in
	 * the MCTS algorithm are selected using these constants. All options
	 * related to the MCTS algorithm should be defined here.
	 */

	/**
	 * Defines the behavior in which node creation is dependent upon
	 * NODE_CREATION_COUNT.
	 */
	public static final int BEHAVIOR_EXPANSION_STANDARD = 0;

	/**
	 * Defines the behavior in which a node is created (if the node does not
	 * exist already), regardless of NODE_CREATION_COUNT.
	 */
	public static final int BEHAVIOR_EXPANSION_ALWAYS = 1;

	/**
	 * Defines the behavior in which a node is not created, regardless of
	 * NODE_CREATION_COUNT.
	 */
	public static final int BEHAVIOR_EXPANSION_NEVER = 2;

	/**
	 * Defines the behavior in which only a single node in a new branch is
	 * created when expanding the tree. (Unimplemented)
	 */
	public static final int BEHAVIOR_EXPANSION_SINGLE = 0;

	/**
	 * Defines the behavior in which multiple nodes in a new branch are created
	 * when expanding the tree. (Unimplemented)
	 */
	public static final int BEHAVIOR_EXPANSION_MULTIPLE = 1;

	/**
	 * Defines the behavior in which all nodes of a new branch are created when
	 * expanding the tree. (Unimplemented)
	 */
	public static final int BEHAVIOR_EXPANSION_FULL = 2;

	/**
	 * Defines the behavior in which unexplored nodes are selected in the order
	 * they are tested, and all are selected before any node is selected a
	 * second time. (Unimplemented)
	 */
	public static final int BEHAVIOR_UNEXPLORED_STANDARD = 0;

	/**
	 * Defines the behavior in which unexplored nodes are selected using first
	 * play urgency (FPU). FPU gives unexplored nodes a constant reward value.
	 * This value can be tuned to encourage exploitation in the early game.
	 * (Unimplemented)
	 */
	public static final int BEHAVIOR_UNEXPLORED_FIRST_PLAY_URGENCY = 1;

	/**
	 * Defines the behaviors to be used during this search.
	 */
	static int[] behaviors = { BEHAVIOR_EXPANSION_STANDARD, BEHAVIOR_UNEXPLORED_STANDARD };

	/**
	 * Determines whether the learned tree should be tested in the end
	 */
	static boolean TESTIT = false;
	/*------------------Parallel MCTS-----------------------*/
	/**
	 * The number of simulations made before sharing data between two parallel
	 * trees.
	 */
	static int shareInfoEvery;

	static int rank;
	static final boolean TESTPRINT = false;
	static int maxTasks = 4;
	/*------------------------------------------------------*/

	/**
	 * @param args
	 *            width, height, c, matches, sims1, scored1, sym1, opponent (1
	 *            for MCTS player, 2 for default), parallel
	 * 
	 *            If opponent == 1: scored2, sym2, (sims2)
	 * 
	 *            If parallel: shareInfoEvery, tasks
	 */
	public static void main(String[] args) throws MPIException {
		// System.out.println(Math.random());
		long s = System.currentTimeMillis();

		int matches = 0, sims1 = 0, sims2 = 0, opponent = 0;
		boolean scored1 = false, scored2 = false, sym1 = false, sym2 = false, parallel = false;
		System.out.println("parameters used: " + Arrays.toString(args));

		boolean[] params = new boolean[15];

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];

			int index = arg.indexOf("=") + 1;

			switch (arg.substring(0, index - 1)) {

			case "width":
				width = Integer.parseInt(arg.substring(index));
				params[0] = true;
				break;
			case "height":
				height = Integer.parseInt(arg.substring(index));
				params[1] = true;
				break;
			case "c":
				c = Double.parseDouble(arg.substring(index));
				params[2] = true;
				break;
			case "matches":
				matches = Integer.parseInt(arg.substring(index));
				params[3] = true;
				break;
			case "sims1":
				sims1 = Integer.parseInt(arg.substring(index));
				params[4] = true;
				break;
			case "sims2":
				sims2 = Integer.parseInt(arg.substring(index));
				params[5] = true;
				break;
			case "scored1":
				scored1 = Boolean.parseBoolean(arg.substring(index));
				params[6] = true;
				break;
			case "scored2":
				scored2 = Boolean.parseBoolean(arg.substring(index));
				params[7] = true;
				break;
			case "opponent":
				opponent = Integer.parseInt(arg.substring(index));
				params[8] = true;
				break;
			case "sym1":
				sym1 = Boolean.parseBoolean(arg.substring(index));
				params[9] = true;
				break;
			case "sym2":
				sym2 = Boolean.parseBoolean(arg.substring(index));
				params[10] = true;
				break;
			case "parallel":
				parallel = Boolean.parseBoolean(arg.substring(index));
				params[11] = true;
				break;
			case "shareInfoEvery":
				shareInfoEvery = Integer.parseInt(arg.substring(index));
				params[12] = true;
				break;
			case "tasks":
				maxTasks = Integer.parseInt(arg.substring(index));
				params[13] = true;
				break;
			case "errordir":
				//System.out.println (" errordir = " + params[14]);
				errorDir =  arg.substring(index);
				params[14] = true;
				break;
			}
		
		}

		boolean missingParams = false;

		if (!params[0]) {
			System.out.println("Missing Parameter: height");
			missingParams = true;
		}
		if (!params[1]) {
			System.out.println("Missing Parameter: width");
			missingParams = true;
		}
		if (!params[2]) {
			System.out.println("Missing Parameter: c");
			missingParams = true;
		}
		if (!params[3]) {
			System.out.println("Missing Parameter: matches");
			missingParams = true;
		}
		if (!params[4]) {
			System.out.println("Missing Parameter: sims1");
			missingParams = true;
		}
		if (!params[5]) {
			sims2 = sims1;
		}

		if (!params[6]) {
			System.out.println("Missing Parameter: scored1");
			missingParams = true;
		}

		if (!params[9]) {
			System.out.println("Missing Parameter: sym1");
			missingParams = true;
		}
		
		if (!params[14]) {
			System.out.println("Missing Parameter: errorDirectory");
			missingParams = true;
		}

		if (!params[8]) {
			System.out.println("Missing Parameter: opponent");
			missingParams = true;
		} else if (opponent == 1) {
			if (!params[7]) {
				System.out.println("Missing Parameter: scored2");
				missingParams = true;
			}
			if (!params[10]) {
				System.out.println("Missing Parameter: sym2");
				missingParams = true;
			}
		} else if (opponent != 2) {
			System.out.println("Invalid Parameter: opponent");
			missingParams = true;
		}

		if (!params[11]) {
			System.out.println("Missing Parameter: parallel");
			missingParams = true;
		} else {
			if (parallel) {
				if (!params[12]) {
					System.out.println("Missing Parameter: shareInfoEvery");
				}
				if (!params[13]) {
					System.out.println("Missing Parameter: tasks");
				}
			}
		}

		if (missingParams) {
			return;
		}

		/** -- ADD MESSAGE IF WASTING SIMS -- **/
		
		// indicate next runa
		System.out.println ("about to open file for writing " + errorDir+"/errorInfo.txt");
		File file = new File(errorDir+"/errorInfo.txt");
		FileWriter fr = null;
		BufferedWriter br = null;
		// String dataWithNewLine=data+System.getProperty("line.separator");
		try {
			fr = new FileWriter(file, true);
			br = new BufferedWriter(fr);
			br.write("NEXT run " + Arrays.toString(args) + " \n");
		} catch (IOException e) {
			System.out.println (" error when trying to write to the file...");
			e.printStackTrace();
		} finally {
			try {
				br.close();
				fr.close();
				System.out.println (" closed the file......");
		} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/* All parameters present and valid - Game can begin */

		edges = (height * (width + 1)) + (width * (height + 1));
		times = new long[edges][2];
		game = new DotsAndBoxes(height, width, scored1, sym1);
		firstMove = new int[matches];

		if (parallel) {
			game2 = new DotsAndBoxes(height, width, scored1, sym1);

			if (maxTasks > 1) {
				MPI.Init(args);
				rank = MPI.COMM_WORLD.getRank();
				competitionParallel(tree, game, tree2, game2, sims1 / maxTasks, sims2 / maxTasks, matches);
				MPI.Finalize();
			} else {
				rank = -1;
				System.out.println("DETECTIVE CALLING PARALLEL");
				competitionParallel(tree, game, tree2, game2, sims1, sims2, matches);
			}
		} else {
			if (opponent == 1) {
				game2 = new DotsAndBoxes(height, width, scored2, sym2);
				competition(tree, game, tree2, game2, sims1, sims2, matches);
			} else {
				competition(tree, game, null, null, sims1, sims2, matches);
			}
		}

		System.out.println(System.currentTimeMillis() - s);

	}

	/**
	 * 
	 * @param state
	 *            The current state of the game.
	 * @param controllerNetScore
	 *            The net score for the player currently in control.
	 * @return True if the player in control wins, false otherwise.
	 */
	public boolean endgame(GameState state, int controllerNetScore) {

		return true;
	}

	/**
	 * Plays a number of games between two MCTS players.
	 * 
	 * @param tree
	 *            The tree for player one.
	 * @param game
	 *            The game for player one.
	 * @param tree2
	 *            The tree for player two.
	 * @param game2
	 *            The game for player two.
	 * @param simulationsPerTurn1
	 *            The number of simulations given to player one.
	 * @param simulationsPerTurn2
	 *            The number of simulations given to player two.
	 * @param matches
	 *            The number of games to be played.
	 */
	public static void competition(MCTree tree, DotsAndBoxes game, MCTree tree2, DotsAndBoxes game2,
			int simulationsPerTurn1, int simulationsPerTurn2, int matches) throws MPIException {
		int wins = 0;
		int losses = 0;
		int draws = 0;
		goodChoice = 0;
		badChoice = 0;

		double totalAveDepth = 0;
		long totalNodes = 0;

		/* plays a match */
		for (int i = matches; i > 0; i--) {
			double[] results = match(tree, game, tree2, game2, simulationsPerTurn1, simulationsPerTurn2, false);
			int result = (int) results[0];
			totalAveDepth += results[1];
			totalNodes += results[2];

			if (result == 1)
				wins++;
			else if (result == 0) {
				draws++;
			} else {
				losses++;
			}
			//PRANAY 11/5
			//System.out.println(playerMove + "\n");
			//playerMove = new ArrayList<PlayerMove>();
		}

		/* Results */
		System.out.println(height + "x" + width + " c=" + c + " matches=" + matches + " sims=" + simulationsPerTurn1
				+ "," + simulationsPerTurn2 + " p1=" + (game.scored ? "sc+" : "nsc+") + (game.asymmetrical ? "s" : "ns")
				+ " p2=" + (game2.scored ? "sc+" : "nsc+") + (game2.asymmetrical ? "s" : "ns\n") + 
				
				"l d w\ngood move\nmove bad\n"
				+ losses + " " + draws + " " + wins + "\n"
				+ startOK[0] + " " + startOK[1] + " " + startOK[2] + "\n"
				+ startBad[0] + " " + startBad[1] + " " + startBad[2]
				);

		System.out.println("Average nodes: " + totalNodes / matches);
		System.out.println("average depth: " + (totalAveDepth / matches) + "\nAverage Time: ");

		for (int i = 0; i < times.length; i++) {
			if (times[i][1] == 0) {
				continue;
			}

			System.out.println("Move " + i + ": " + times[i][0] / times[i][1]);
		}
	}

	/**
	 * Plays a single game between two MCTS players.
	 * 
	 * @param tree
	 *            The tree for player one.
	 * @param game
	 *            The game for player one.
	 * @param tree2
	 *            The tree for player two.
	 * @param game2
	 *            The game for player two.
	 * @param simulationsPerTurn1
	 *            The number of simulations given to player one.
	 * @param simulationsPerTurn2
	 *            The number of simulations given to player two.
	 * @param parallel
	 *            True if the tree is parallelized.
	 * @return An array of the form {result, average depth of the final tree for
	 *         player one, number of nodes in the final tree for player one}.
	 */
	public static double[] match(MCTree tree, DotsAndBoxes game, MCTree tree2, DotsAndBoxes game2,
			int simulationsPerTurn1, int simulationsPerTurn2, boolean parallel) throws MPIException {

		tree = game.scored ? new MCTree(game, new GameStateScored(0, 0)) : new MCTree(game, new GameState(0));
		tree2 = game2.scored ? new MCTree(game2, new GameStateScored(0, 0)) : new MCTree(game2, new GameState(0));

		int result = -10;

		/*
		 * This is used as a backup to resolve flawed tests caused by
		 * ArrayIndexOutOfBounds or NullPointer errors during the game. When
		 * these errors occur, they return a result of -10, and the game is
		 * restarted.
		 */
		while (result == -10) {
			if (parallel) {
				result = testGameParallel(tree, game, tree2, game2, simulationsPerTurn1, simulationsPerTurn2);
				
			} else
				result = testGame(tree, game, tree2, game2, simulationsPerTurn1, simulationsPerTurn2);
		}

		double results[] = new double[3];
		results[0] = result;
		results[1] = (double) tree.totalDepth / tree.numNodes;
		results[2] = tree.numNodes;

		if (TESTIT)
			testPolicy(false, tree);

		return results;
	}

	/**
	 * Plays a single game between two MCTS players.
	 * 
	 * @param tree
	 *            The tree for player one.
	 * @param game
	 *            The game for player one.
	 * @param tree2
	 *            The tree for player two.
	 * @param game2
	 *            The game for player two.
	 * @param simulationsPerTurn1
	 *            The number of simulations given to player one.
	 * @param simulationsPerTurn2
	 *            The number of simulations given to player two.
	 * @return An integer representing the result for player one.
	 */
	public static int testGame(MCTree tree, DotsAndBoxes game, MCTree tree2, DotsAndBoxes game2,
			int simulationsPerTurn1, int simulationsPerTurn2) {

		GameState terminalState = null;

		if (edges > 60) {
			terminalState = new GameState(new BigInteger("2").pow(edges).subtract(new BigInteger("1")));
		} else {
			terminalState = new GameState((long) Math.pow(2, edges) - 1);
		}

		// the current node of each tree
		MCNode currentNode = tree.root;
		MCNode currentNode2 = tree2.root;

		// the game variables
		int action = 0;
		boolean playerOneTurn = true;
		int p1Score = 0;
		int p2Score = 0;

		// the number of boxes that are completed or have two edges
		int twoOrFour = 0;

		// board[i] is the number of taken edges for box i
		int[] board = new int[width * height];

		// a clone to pass to the simulate method
		// int[] boardClone = new int[width * height];

		int startsWell = 1;
		// for every turn
		while (!currentNode.state.equals(terminalState)) {

			if (p1Score > (width * width) / 2 || p2Score > (width * width) / 2) {
				break;
			}

			int sims = playerOneTurn ? simulationsPerTurn1 : simulationsPerTurn2;
			simsDEBUG = sims;

			// get the action based on the current player
			if (playerOneTurn) {
				int simsPerformed = 0;
				long start = System.currentTimeMillis();

				// perform the simulations for this move
				while (simsPerformed < sims) {
					// give player one's game, tree, node, and score
					// create a clone of the board to be used by the simulation
					int[] boardClone = new int[width * height];
					for (int bc = 0; bc < width * height; bc++)
						boardClone[bc] = board[bc];
					simulate(currentNode.state, p1Score - p2Score, currentNode, terminalState, tree, game, boardClone,
							twoOrFour);
					simsPerformed++;
					simsDEBUG = simsPerformed;
				}

				long end = System.currentTimeMillis();

				try {
					times[currentNode.depth][1]++;
					times[currentNode.depth][0] = times[currentNode.depth][0] + (end - start);
				} catch (ArrayIndexOutOfBoundsException e) {
					System.out.println("Array Index Error");
					return -10;
				}

				action = currentNode.getNextAction(0);

				
			} else {
				// perform the simulations for this move
				int simsPerformed = 0;
				while (simsPerformed < sims) {
					// give player two's game, tree, node, and score
					int[] boardClone = new int[width * height];
					for (int bc = 0; bc < width * height; bc++)
						boardClone[bc] = board[bc];

					simulate(currentNode2.state, p2Score - p1Score, currentNode2, terminalState, tree2, game2,
							boardClone, twoOrFour);
					simsPerformed++;
					simsDEBUG = simsPerformed;
				}

				action = currentNode2.getNextAction(0);
				
			}

			if (currentNode.state.longState == 0)
				if ((action == 3 || action == 5 || action == 6 || action == 8)) {
					startsWell = 0;
					badChoice++;
				} else
					goodChoice++;

			// get the points for this move
			int taken = 0;

			// increment the edges for each box which adjoins action
			for (int i = 0; i < game.edgeBoxes[action].length; i++) {
				board[game.edgeBoxes[action][i]]++;
				// boardClone[game.edgeBoxes[action][i]]++;

				if (board[game.edgeBoxes[action][i]] == 4) {
					taken++;
					twoOrFour++;
				} else if (board[game.edgeBoxes[action][i]] == 2) {
					twoOrFour++;
				}
			}

			// if both players are symmetrical or both are asymmetrical, the
			// same moves are possible for each
			if (game.asymmetrical == game2.asymmetrical) {
				// update the currentNodes
				currentNode = currentNode.getNode(action, BEHAVIOR_EXPANSION_ALWAYS);
				currentNode2 = currentNode2.getNode(action, BEHAVIOR_EXPANSION_ALWAYS);
			}

			// if the player in control is asymmetrical, translate
			else if (playerOneTurn && game.asymmetrical) {
				// update the currentNodes
				currentNode = currentNode.getNode(action, BEHAVIOR_EXPANSION_ALWAYS);
				currentNode2 = currentNode2.getNode(action, BEHAVIOR_EXPANSION_ALWAYS);
			} else if (!playerOneTurn && game2.asymmetrical) {
				// update the currentNodes
				currentNode = currentNode.getNode(action, BEHAVIOR_EXPANSION_ALWAYS);
				currentNode2 = currentNode2.getNode(action, BEHAVIOR_EXPANSION_ALWAYS);
			}

			// if the player in control is symmetrical, the moves must be
			// translated to a symmetrical one
			else {

				// get the next node for the symmetrical player in control
				if (playerOneTurn) {
					currentNode = currentNode.getNode(action, BEHAVIOR_EXPANSION_ALWAYS);
					currentNode2 = currentNode2.getNode(currentNode.state, BEHAVIOR_EXPANSION_ALWAYS);
				} else {
					currentNode2 = currentNode2.getNode(action, BEHAVIOR_EXPANSION_ALWAYS);
					currentNode = currentNode.getNode(currentNode2.state, BEHAVIOR_EXPANSION_ALWAYS);
				}
			}

			/* possibly circumvent the null pointer */
			if (currentNode == null || currentNode2 == null) {
				System.out.println("Null Error: " + (currentNode == null ? "Player 1" : "Player 2"));
				return -10;
			}

			// catch errors between symmetrical and asymmetrical players
			if (!game.removeSymmetries(currentNode.state).equals(game2.removeSymmetries(currentNode2.state))) {
				System.out.println("Move Error: " + (playerOneTurn ? "Player 1" : "Player 2"));
				return -10;
			}

			if (playerOneTurn) {
				p1Score += taken;
				playerMove.add(new PlayerMove(action, 1, p1Score - p2Score));
			} else {
				p2Score += taken;
				playerMove.add(new PlayerMove(action, 2, p1Score - p2Score));
			}

			playerOneTurn = taken > 0 ? playerOneTurn : !playerOneTurn;
		}

		int p1Net = p1Score - p2Score;

		if (startsWell == 1) {
			if (p1Net < 0)
				startOK[0]++;
			if (p1Net == 0)
				startOK[1]++;
			if (p1Net > 0)
				startOK[2]++;
		} else {
			if (p1Net < 0)
				startBad[0]++;
			if (p1Net == 0)
				startBad[1]++;
			if (p1Net > 0)
				startBad[2]++;

		}

		return p1Net > 0 ? 1 : p1Net < 0 ? -1 : 0;
	}

	/**
	 * Updates the nodes played in a game. This is the backpropogation stage of
	 * the simulation.
	 * 
	 * @param nodes
	 *            An array of all nodes traversed during the game.
	 * @param player
	 *            An array with turns played by player one represented as true
	 *            and turns played by player two represented as false.
	 * @param actions
	 *            An array of all the actions played during the selection
	 *            portion of the game.
	 * @param result
	 *            An integer representing the result for player one (-1 for a
	 *            loss, 0 for a tie, and 1 for a win).
	 */
	public static void backup(MCNode[] nodes, boolean[] player, int[] actions, int result) {
		// System.out.println ("rank " + rank + " UZI in backup - list of
		// actions length and values " + actions.length + " : " +
		// Arrays.toString(actions) +
		// "\n nodes.length = "+ nodes.length + " " + Arrays.toString(nodes));

		for (int i = 0; i < nodes.length; i++) {
			if (nodes[i] == null) {
				break;
			}

			/* switch result if this was player two's move */
			if (!player[i]) {
				result = -result;
			}

			/* add a win, loss, or tie, to the node given the action taken */
			nodes[i].addValue(actions[i], result, c);

			if (!player[i]) {
				result = -result;
			}
		}
	}

	/**
	 * Plays the game from a given point off the tree with a random default
	 * policy. This is the playout stage of simulation.
	 * 
	 * @param state
	 *            The starting state.
	 * @param playerOne
	 *            True if player one is to move, false otherwise.
	 * @param p1Net
	 *            The starting net score for player one.
	 * @param terminalState
	 *            The state at which simulation will cease.
	 * @return An integer representing the result for player one (-1 for a loss,
	 *         0 for a tie, and 1 for a win).
	 */
	public static int simulateDefault(GameState state, boolean playerOne, int p1Net, GameState terminalState) {

		/* play until the terminalState */

		for (int i = 0; i < edges; i++) {

			int action = randomPolicy(state);
			state = game.getSimpleSuccessorState(state, action);

			int taken = game.completedBoxesForEdge(action, state);

			if (taken > 0) {
				p1Net += playerOne ? taken : -taken;
			}

			else {
				playerOne = !playerOne;
			}

			if (state.equals(terminalState)) {
				break;
			}
		}

		p1Net = p1Net > 0 ? 1 : p1Net < 0 ? -1 : 0;

		return p1Net;
	}

	/**
	 * Runs a single simulation and updates the tree accordingly. The majority
	 * of this method constitutes the selection stage of simulation.
	 * 
	 * @param state
	 *            The starting state.
	 * @param p1Net
	 *            The starting net score for player one.
	 * @param pastNode
	 *            A node representing the current position on the tree.
	 * @param terminalState
	 *            The state at which simulation will cease.
	 * @param tree
	 *            The tree to be used and updated. This tree should belong to
	 *            the player running the simulation.
	 * @param game
	 *            The game to be used. This game should belong to the player
	 *            running the simulation.
	 * @param board
	 *            An array representing the number of edges taken for each box.
	 * @param twoOrFour
	 *            The number of boxes which have either 2 or 4 edges.
	 */
	public static void simulate(GameState state, int p1Net, MCNode pastNode, GameState terminalState, MCTree tree,
			DotsAndBoxes game, int[] board, int twoOrFour) {
		boolean playerOne = true;

		int action = 0;
		boolean[] turns = new boolean[edges];
		int[] actionsTaken = new int[edges + 1];
		
		/* keep track of the traversed nodes */
		MCNode[] playedNodes = new MCNode[edges];
		MCNode currentNode = pastNode;

		playedNodes[0] = currentNode;

		/* plays each move until game over or off the tree */
		for (int i = 0; !state.equals(terminalState); i++) {

			turns[i] = playerOne ? true : false;

			/* make a move */
			action = currentNode.getNextAction(c);
			currentNode = currentNode.getNode(action, BEHAVIOR_EXPANSION_STANDARD);
			//if(rank == 0)
				//System.out.println(i + ", ACTION: " + action);
			actionsTaken[i] = action;
			if(i==0) {
				//HERE i=0 is first move of simulation?
				simulationMoves[actionsTaken[0]] += 1;
			}
			/* if someone has more than half the squares, quit early */
			if (p1Net > (height * width) / 2 || p1Net < (-height * width) / 2) {
				state = terminalState;
				break;
			}

			int taken = 0;

			// increment the edges for each box which adjoins action
			if (action == -1) {
				File file = new File(errorDir+"/errorInfo.txt");
				// File file = new File("/home/uta/javaworkspaces/usingopenMPI/DaB28July/errorInfo.txt");
				FileWriter fr = null;
				BufferedWriter br = null;
				// String
				// dataWithNewLine=data+System.getProperty("line.separator");
				try {
					fr = new FileWriter(file, true);
					br = new BufferedWriter(fr);
					br.write("rank + " + rank + " action " + action + " node after currentNode " + currentNode
							+ " state " + state.longState + "\n");
					StringBuilder linkInfo = new StringBuilder();
					for (int kp = 0; kp < pastNode.links.length; kp++)
						if (pastNode.links[kp] == null)
							linkInfo.append("link " + kp + " is null \n");
						else
							linkInfo.append("link " + kp + ": " + pastNode.links[kp].action + " "
									+ pastNode.links[kp].timesChosen + " " + pastNode.links[kp].rewards + " "
									+ pastNode.links[kp].bonus + "\n");
					br.write(linkInfo.toString());
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						br.close();
						fr.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			for (int b = 0; b < game.edgeBoxes[action].length; b++) {
				board[game.edgeBoxes[action][b]]++;

				if (board[game.edgeBoxes[action][b]] == 4) {
					taken++;
					twoOrFour++;
				} else if (board[game.edgeBoxes[action][b]] == 2) {
					twoOrFour++;
				}
			}

			if (currentNode != null) {
				state = currentNode.state;
			}

			else {
				/*
				 * this turns a scored state to unscored, but since it just
				 * feeds into simulateDefault, it doesn't matter
				 */
				state = game.getSuccessorState(state, action);
			}

			/* doesn't add the terminal node */
			if (!state.equals(terminalState)) {
				playedNodes[i + 1] = currentNode;
			}

			if (taken > 0) {
				p1Net += playerOne ? taken : -taken;
			}

			else {
				playerOne = !playerOne;
			}

			if (currentNode == null) {
				break;
			}
		}

		int z; /* the result */

		/* playout if not at terminal state */
		if (!state.equals(terminalState)) {
			z = simulateDefault(state, playerOne, p1Net, terminalState);
		}

		else {
			z = p1Net > 0 ? 1 : p1Net < 0 ? -1 : 0;
		}

		/* backup the nodes */
		backup(playedNodes, turns, actionsTaken, z);
	}

	/**
	 * Gets a random action from a given state.
	 * 
	 * @param state
	 *            The state from which to select an action.
	 * @return An integer representing the action selected.
	 */
	public static int randomPolicy(GameState state) {
		int[] actions = DotsAndBoxes.getAllActions(state, edges);

		int next = r.nextInt(actions.length);
		
		//System.out.println("PA RANK " + rank + ": " + next);
		return actions[next];
	}

	/**
	 * Plays a single game using the tree developed for player one.
	 * 
	 * @param random
	 *            True if random moves should be made during player two's turn.
	 *            False if both players should make moves from the same tree.
	 * @return True if player one wins the game, false otherwise.
	 */
	public static boolean testPolicy(boolean random, MCTree tree) {
		int p1Net = 0;
		GameState state = new GameState(0);
		DotsAndBoxes game = new DotsAndBoxes(2, 2, false, false);
		int action = 0;
		boolean playerOne = true;
		String sequence = "";

		MCNode currentNode = tree.root;

		/* for every move in the game */
		for (int i = 0; i < edges; i++) {

			/* for a random player or when off the tree */
			if ((random && !playerOne) || currentNode == null) {

				action = randomPolicy(state);

				if (currentNode != null) {
					currentNode = currentNode.getNode(action, BEHAVIOR_EXPANSION_STANDARD);
				}
			}

			else {
				/* get the next node, given c */
				action = currentNode.getNextAction(c);
				currentNode = currentNode.getNode(action, BEHAVIOR_EXPANSION_STANDARD);
			}

			if (currentNode != null) {
				state = currentNode.state;
			}

			else {
				state = game.getSuccessorState(state, action);
			}

			int taken = game.completedBoxesForEdge(action, state);

			if (taken > 0) {
				p1Net += playerOne ? taken : -taken;
			}

			else {
				playerOne = !playerOne;
			}

			// prepare output
			int pl = 1;
			if (taken == 0 && playerOne || taken > 0 && !playerOne)
				pl = 2;
			String boxIt = "";
			if (taken > 0)
				boxIt = "T";
			String offTree = "";
			if (currentNode == null)
				offTree = "#";

			sequence += action + "(" + pl + " " + boxIt + " " + offTree + ") ";

		}

		System.out.println("GAMEPLAY: " + sequence + " winner: " + p1Net);

		if (p1Net > 0) {
			return true;
		}

		return false;
	}

	// call the gather function to share the info from the processing nodes to
	// the master
	public static void callMPIGather(ShareInfoNode shareNode, ShareInfoMaster shareMaster, int master)
			throws MPIException {

		MPI.COMM_WORLD.gather(shareNode.nSA0ToMaster, shareNode.nSA0ToMaster.length, MPI.INT, shareMaster.nSA0FromNodes,
				shareNode.nSA0ToMaster.length, MPI.INT, master);

		MPI.COMM_WORLD.gather(shareNode.nSA1ToMaster, shareNode.nSA1ToMaster.length, MPI.INT, shareMaster.nSA1FromNodes,
				shareNode.nSA1ToMaster.length, MPI.INT, master);

		MPI.COMM_WORLD.gather(shareNode.nSA2ToMaster, shareNode.nSA2ToMaster.length, MPI.INT, shareMaster.nSA2FromNodes,
				shareNode.nSA2ToMaster.length, MPI.INT, master);

		MPI.COMM_WORLD.gather(shareNode.rS3ToMaster, shareNode.rS3ToMaster.length, MPI.DOUBLE, shareMaster.rS3FromNodes,
				shareNode.rS3ToMaster.length, MPI.DOUBLE, master);

		MPI.COMM_WORLD.gather(shareNode.nS3ToMaster, shareNode.nS3ToMaster.length, MPI.INT, shareMaster.nS3FromNodes,
				shareNode.nS3ToMaster.length, MPI.INT, master);
		// System.out.println ("rank " + rank + " after gather\nnode: " +
		// shareNode.toString() +
		// " \nmaster: " + shareMaster.toString());
	}

	// call the broadcast function to share the updated data from the master to
	// all nodes (note: to make sure that
	// the master node (which is currently also a processing node) has the
	// updated data, this broadcast the shareNode info
	// from the master.. (
	public static void callMPIBroadcast(ShareInfoNode shareNode, ShareInfoMaster shareMaster, int master)
			throws MPIException {

		// initiate the broadcast
		if (rank == master) {
			// UZI added
			// System.out.println(" and a last time " +
			// Arrays.toString(shareNode.nSA1FromMaster));
			MPI.COMM_WORLD.bcast(shareNode.nSA0FromMaster, shareNode.nSA0FromMaster.length, MPI.INT, master);

			MPI.COMM_WORLD.bcast(shareNode.nSA1FromMaster, shareNode.nSA1FromMaster.length, MPI.INT, master); // ?
																												// won't
																												// work
			//
			MPI.COMM_WORLD.bcast(shareNode.nSA2FromMaster, shareNode.nSA2FromMaster.length, MPI.INT, master); // ?
																												// won't
																												// work
			//
			MPI.COMM_WORLD.bcast(shareNode.rS3FromMaster, shareNode.rS3FromMaster.length, MPI.DOUBLE, master);
			//
			MPI.COMM_WORLD.bcast(shareNode.nS3FromMaster, shareNode.nS3FromMaster.length, MPI.INT, master);
		}

		// receive the bro adcast
		if (rank != master) {
			MPI.COMM_WORLD.bcast(shareNode.nSA0FromMaster, shareNode.nSA0FromMaster.length, MPI.INT, master);

			MPI.COMM_WORLD.bcast(shareNode.nSA1FromMaster, shareNode.nSA1FromMaster.length, MPI.INT, master);
			//
			MPI.COMM_WORLD.bcast(shareNode.nSA2FromMaster, shareNode.nSA2FromMaster.length, MPI.INT, master);
			//
			MPI.COMM_WORLD.bcast(shareNode.rS3FromMaster, shareNode.rS3FromMaster.length, MPI.DOUBLE, master);
			//
			MPI.COMM_WORLD.bcast(shareNode.nS3FromMaster, shareNode.nS3FromMaster.length, MPI.INT, master);
		}

		// System.out.println ("rank " + rank + " after bcast\nnode: " +
		// shareNode.toString() + " \nmaster: " + shareMaster.toString());
	}

	/*-----------------------------------Parallel MCTS----------------------------------------------*/

	public static ArrayList<Integer> combineZeros(ArrayList<Integer> toCombine) throws MPIException {
		int num = 0;
		ArrayList<Integer> finalList = new ArrayList<Integer>();

		for (int i = 0; i < toCombine.size(); i++) {
			if (toCombine.get(i) < 0) {
				num += toCombine.get(i);
			} else if (toCombine.get(i) == 0)
				num += -1;
			else {
				if (num != 0)
					finalList.add(num);
				finalList.add(toCombine.get(i));
				num = 0;
			}
			if (num != 0 && i == toCombine.size() - 1)
				finalList.add(num);
		}

		return finalList;
	}

	// Prints N(0, a) and N(1, a) values
	public static String printCompareInformation(MCNode root) {
		String stringCompareTemp = "[RANK " + rank + " root state: " + root.state.longState
				+ ". Process node has the following numActions for root " + root.links.length + "\n";
		stringCompareTemp += "N(0, a): " + Arrays.toString(root.getTimesActionChosen()) + "\n";
		stringCompareTemp += "N(1, a): ";
		for (int i = 0; i < root.links.length; i++) {
			stringCompareTemp += "CHILD " + i + " has action " + root.links[i].action + " and is " + root.links[i].child
					+ "\n";
			if (root.links[i].child != null) {
				stringCompareTemp += "	has children N(1, i): "
						+ Arrays.toString(root.links[i].child.getTimesActionChosen()) + "\n";
			}
		}

		stringCompareTemp = "\nCompareSendingInformation: " + stringCompareTemp + "]";
		return stringCompareTemp;
	}

	//start with this (12/7 - compare to sims for other)
	public static String printSendingStuff(MCNode root, int[] shareLevel0, int[] finalShareLevel1,
			int[] finalShareLevel2, double[] shareLevelR3, int[] shareLevelN3) {
		String stringPrintTemp = "[RANK " + rank + " sims =" + simsDEBUG + " moves= " + movesCompleted + " root state: "
				+ root.state.longState + ". Process node has the following numActions for root " + root.links.length
				+ "\n";
		stringPrintTemp += "N(0, a): size " + shareLevel0.length + ": " + Arrays.toString(shareLevel0) + "\n";
		stringPrintTemp += "N(1, a): size " + finalShareLevel1.length + ": " + Arrays.toString(finalShareLevel1) + "\n";
		stringPrintTemp += "N(2, a): size " + finalShareLevel2.length + ": " + Arrays.toString(finalShareLevel2) + "\n";
		stringPrintTemp += "R(3S): size " + shareLevelR3.length + ": " + Arrays.toString(shareLevelR3) + "\n";
		stringPrintTemp += "N(3S) size " + shareLevelN3.length + ": " + Arrays.toString(shareLevelN3) + "\n";
		stringPrintTemp = "\nNSendingInformation: " + stringPrintTemp + "]";
		return stringPrintTemp;
	}

	public static int[] callMPIGatherSizes(int size1, int size2) throws MPIException {
		int[] masterAllLengths = new int[2 * maxTasks];
		int maxs[] = new int[2]; // level1 max (for each process node) and
									// level2 max (for each process node)

		// store the sizes for level1 and level2...
		int[] sizeShareLevels = new int[2];
		sizeShareLevels[0] = size1;
		sizeShareLevels[1] = size2;

		MPI.COMM_WORLD.gather(sizeShareLevels, 2, MPI.INT, masterAllLengths, 2, MPI.INT, 0); // send
																								// size
																								// information
																								// for
																								// all
																								// process
																								// node
																								// to
																								// rank
																								// 0
		// [0]-rank0level1, [1]-rank0level2, [2]-rank1level1, [3]-rank1level2...
		// compare [0] with [2] and [1] with [3]

		if (rank == 0) {
			String printLengths = "printLengths: ";
			for (int elem : masterAllLengths)
				printLengths += elem + " ";
			// System.out.println(printLengths); //[0]-rank0level1,
			// [1]-rank0level2, [2]-rank1level1, [3]-rank1level2... compare [0]
			// with [2] and [1] with [3]

			int[] templvl1 = new int[maxTasks]; // store even indices from
												// masterAllLengths
			int[] templvl2 = new int[maxTasks]; // store odd indices from
												// masterAllLengths

			int counter1 = 0;
			int counter2 = 0;

			for (int i = 0; i < masterAllLengths.length; i++) {
				if (i % 2 == 0)
					templvl1[counter1++] = masterAllLengths[i];
				else
					templvl2[counter2++] = masterAllLengths[i];
			}

			// Now find the max value for level1 [0] and level2 [1]
			maxs[0] = -1;
			maxs[1] = -1;

			for (int elem : templvl1)
				if (maxs[0] < elem)
					maxs[0] = elem;

			for (int elem : templvl2)
				if (maxs[1] < elem)
					maxs[1] = elem;
		}
		return maxs;
	}

	public static int[] convertToArray(ArrayList<Integer> toConvert) {
		int[] array = new int[toConvert.size()];
		for (int i = 0; i < toConvert.size(); i++) {
			array[i] = toConvert.get(i);
		}
		return array;
	}

	// create the info at the processing node which must be shared
	// also creates the data structure to hold the result computed and broadcast
	// by the master node
	public static ShareInfoNode gatherShareInfoNode(int[] NSA0Info, int[] NSA1Info, int[] NSA2Info, double[] RS3Info,
			int[] NS3Info) {
		ShareInfoNode share = new ShareInfoNode();

		// share N(s, a) and R(s,a)
		// arrays for each compute node
		int[] nSA0Send, nSA1Send, nSA2Send;
		double[] rS3Send;
		int[] nS3Send;

		// arrays for each compute node
		nSA0Send = NSA0Info;
		nSA1Send = NSA1Info;
		nSA2Send = NSA2Info;
		rS3Send = RS3Info;
		nS3Send = NS3Info;

		share.set(nSA0Send, nSA1Send, nSA2Send, rS3Send, NS3Info);

		return share;

	}

	// create the data structures to hold the info which the nodes will share
	public static ShareInfoMaster gatherShareInfoMaster(int n0a, int n1a, int n2a, int r3, int n3) {
		ShareInfoMaster share = new ShareInfoMaster();

		// arrays for the master node to receive
		int[] n0ToGather, n1ToGather, n2ToGather;
		double[] r3ToGather;
		int[] n3ToGather;

		// arrays for the master node
		// long arrays to store info from each compute node
		n0ToGather = new int[(n0a * maxTasks)]; // number of possible links *
												// tasks
		n1ToGather = new int[(n1a * maxTasks)];
		n2ToGather = new int[(n2a * maxTasks)];
		r3ToGather = new double[(r3 * maxTasks)];
		n3ToGather = new int[n3 * maxTasks];

		share.set(n0ToGather, n1ToGather, n2ToGather, r3ToGather, n3ToGather, maxTasks);
		return share;

	}


	/** Primary Contributor: Pranay Agrawal
	 *
	 * @param currNode the current subtree
	 * @param tree the player 1 tree
	 * @param game the current game simulation that is being played
	 * @param tree2 the player 2 tree
	 * @param game2 the current game for the opponent, which is typically synced to game
	 * @return the node to play next
	 * @throws MPIException Communication failed
	 */
	public static MCNode doStuff(MCNode currNode, MCTree tree, DotsAndBoxes game, MCTree tree2, DotsAndBoxes game2)
			throws MPIException {

		// System.out.println(rank + ": " + tree + " tree " + tree2 + " tree2");
		int currNodeNumActions = DotsAndBoxes.getAllActions(currNode.state, edges).length;
		MCNode toReturn = currNode;

		int numActions = currNode.links.length;
		
		int[] shareLevel0 = currNode.getTimesActionChosen();
		
		printArr("N(0,a): ", currNode.getTimesActionChosen());
		printArr("R(0s,a): ", currNode.getRewards());
		
		ArrayList<Integer> shareLevel1 = new ArrayList<Integer>();
		ArrayList<Integer> shareLevel2 = new ArrayList<Integer>();

		// SHARE N(S, A) of Level 1
		for (int i = 0; i < numActions; i++) {
			if (currNode.links[i].child == null) { // DOES NOT EXIST
				shareLevel1.add(-1 * (numActions - 1)); // numActions is the
														// number of actions we
														// have left, -1 to
														// elimate one choice
														// (edges of
														// currNode-1).
				shareLevel2.add(-1 * (numActions - 1) * (numActions - 2));
			} else { // EXISTS
				int[] tempArray = currNode.links[i].child.getTimesActionChosen();
				for (int elem : tempArray)
					shareLevel1.add(elem);

				/** LEVEL 2 STUFF (LAST LEVEL) **/
				MCNode tempGrandChild = currNode.links[i].child;
				for (int j = 0; j < tempGrandChild.links.length; j++) { // cycle through grandchildren
					if (tempGrandChild.links[j].child == null) {// for lvl2 DOES
																// NOT EXIST
						shareLevel2.add(-1 * (numActions - 2));
					} else { // GRANDCHILD DOES EXIST
						int[] tempArray2 = tempGrandChild.links[j].child.getTimesActionChosen();
						for (int elem : tempArray2)
							shareLevel2.add(elem);
					}
				}
			}
		}
	
		//printList("RANK: " + rank + ", FINAL LVL1: ", shareLevel1);
		//printList(res, arr);
		
		// Combined zeros, "sending" list
		ArrayList<Integer> finalShareLevel1 = combineZeros(shareLevel1);
		ArrayList<Integer> finalShareLevel2 = combineZeros(shareLevel2);

		//
		
		/** We should have all the N(s, a) values **/

		int maxs[] = callMPIGatherSizes(finalShareLevel1.size(), finalShareLevel2.size());

		// initiate the broadcast
		if (rank == 0)
			MPI.COMM_WORLD.bcast(maxs, maxs.length, MPI.INT, 0);
		else
			MPI.COMM_WORLD.bcast(maxs, maxs.length, MPI.INT, 0);

		// System.out.println("maxs: " + Arrays.toString(maxs));

		int addtolvl1 = maxs[0] - finalShareLevel1.size();
		int addtolvl2 = maxs[1] - finalShareLevel2.size();
		for (int i = 0; i < addtolvl1; i++) {
			finalShareLevel1.add(0);
		}
		for (int i = 0; i < addtolvl2; i++) {
			finalShareLevel2.add(0);
		}

		// Generate 2D array of ints, where each row is a 3-length combination
		// of the remaining actions from currNode for R(s) level 3
		generateCombinationArray(currNode);

		/**
		 * At this point, permanentcombList contains all possible combinations
		 * for a given remaining action list. Luckily, since currNode is the
		 * same state, this can be a "global" variable, as it is not dependent
		 * on the process node. Now, we must collect go through all MCNode using
		 * the new getStateAfterActions and then finding that MCNode in the
		 * MCTree tree
		 **/

		int[] shareLevelN3 = new int[permanentcombList.length];
		double[] shareLevelR3 = new double[permanentcombList.length];
		// tree.findNode( MCNode.getStateAfterActions(currNode,
		// permanentcombList[0], 12) );
		for (int j = 0; j < permanentcombList.length; j++) {
			MCNode tempNode = tree.findNode(MCNode.getStateAfterActions(currNode, permanentcombList[j], edges));
			// if(rank == 0) //testcase
			// System.out.println("rank " + rank + ": " +
			// Arrays.toString(permanentcombList[j]) + " results in tempNode: "
			// + tempNode); //testcase
			if (tempNode != null) {
				shareLevelR3[j] = tempNode.getTotalRewards();
				shareLevelN3[j] = tempNode.timesReached;
				// add node N(s) value array too
			} else {
				shareLevelR3[j] = 0;
				shareLevelN3[j] = 0;
			}
			// tempNode is the MCNode after adding the edges
		}

		// FOUR items to share...
		// int[] shareLevel0; //perfect to share (always same length + array
		// data type)
		int[] toSendLevel1 = convertToArray(finalShareLevel1); // perfect to
																// share (has
																// zeros to
																// balance
																// length +
																// array data
																// type)
		int[] toSendLevel2 = convertToArray(finalShareLevel2); // perfect to
																// share (has
																// zeros to
																// balance
																// length +
																// array data
																// type)
		// double[] shareLevelR3; //perfect to share (always same length + array
		// data type)
		// int[] shareLevelN3; //perfect to share (always same length + array
		// data type)

		/** PRINT TESTING **/
//		System.out.println("\n Rank " + rank + " BEFORE SENDING [[[" 
//				+ printSendingStuff(currNode, shareLevel0, toSendLevel1, toSendLevel2, shareLevelR3, shareLevelN3)
//				+ "\n]]]");

		// create data structures to share the info
		ShareInfoNode shareNode = gatherShareInfoNode(shareLevel0, toSendLevel1, toSendLevel2, shareLevelR3,
				shareLevelN3);
		ShareInfoMaster shareMaster = gatherShareInfoMaster(shareLevel0.length, toSendLevel1.length,
				toSendLevel2.length, shareLevelR3.length, shareLevelN3.length);

		if (rank == 0) {
	//		System.out.println(
	//				"RANK0 BEFORE: " + printSendingStuff(currNode, shareNode.nSA0FromMaster, shareNode.nSA1FromMaster,
	//						shareNode.nSA2FromMaster, shareNode.rS3FromMaster, shareNode.nS3FromMaster));
		}

		/** COMBINE PROCESS NODES **/

		// make the gather calls
		// XXXXstart timerXXXX
		callMPIGather(shareNode, shareMaster, 0); // shareNode to shareMaster
		//
		// // master must computer info to be broadcast
		// // computed Info for nodes --> store in node variable
		if (rank == 0) {
			// System.out.println("before... " + printSendingStuff(currNode,
			// shareMaster.nSA0FromNodes, shareMaster.nSA1FromNodes,
			// shareMaster.nSA2FromNodes, shareMaster.rS3FromNodes,
			// shareMaster.nS3FromNodes));
			shareMaster.processInfo(); // summation of process nodess
			shareMaster.scale();
			shareNode.setFromMasterCombine(shareMaster.nSA0ToNodes, shareMaster.nSA1ToNodes, shareMaster.nSA2ToNodes,
					shareMaster.rS3ToNodes, shareMaster.nS3ToNodes);
			// now master node (rank 0) has the combined information in
			// shareNode. So we must broadcast now
			shareNode.setNLength();
			// System.out.println("master... " + printSendingStuff(currNode,
			// shareMaster.nSA0ToNodes, shareMaster.nSA1ToNodes,
			// shareMaster.nSA2ToNodes, shareMaster.rS3ToNodes,
			// shareMaster.nS3ToNodes));

		}

		// initiate the broadcast
		if (rank == 0)
			MPI.COMM_WORLD.bcast(shareNode.nSA1andnSA2Lengths, 2, MPI.INT, 0);
		else {
			MPI.COMM_WORLD.bcast(shareNode.nSA1andnSA2Lengths, 2, MPI.INT, 0);
			shareNode.updateNlengths();
		}

		// broadcast combined info back to process nodes
		callMPIBroadcast(shareNode, shareMaster, 0); // shareNode."*FromMaster"
														// is where the combined
														// info is stored.
														// N(1,a) and N(2,a)
														// wont work (is it
														// because of sharing
														// size is too big?)
		// XXXXend timerXXXX

/*		if (rank == 0) {
			System.out.println(
					"RANK0 AFTER: " + printSendingStuff(currNode, shareNode.nSA0FromMaster, shareNode.nSA1FromMaster,
							shareNode.nSA2FromMaster, shareNode.rS3FromMaster, shareNode.nS3FromMaster));
		}
*/
		shareNode.computeQ3(); // compute the Q(s) values for the third level
		shareNode.qS2Compute = new double[choose(numActions, 2)];
		shareNode.qS1Compute = new double[numActions];
		int[] arrOfActions = new int[numActions];
		for (int i = 0; i < numActions; i++) {
			arrOfActions[i] = currNode.links[i].action; // fill up array with
														// actions
		}

		// update N(s) for level 3 nodes...
		for (int i = 0; i < shareNode.nS3FromMaster.length; i++) {
			if (shareNode.nS3FromMaster[i] != 0) {
				int threeActionsTaken[] = permanentcombList[i]; // 3-edge combo
																// (actions)
				MCNode tempNode = tree.findNode(MCNode.getStateAfterActions(currNode, threeActionsTaken, edges)); // do
																													// I
																													// use
																													// "tree"
																													// for
																													// the
																													// MCTree?
				if (tempNode == null) {
					MCNode tempGameState = MCNode.getStateAfterActions(currNode, threeActionsTaken, edges);
					int[] actions = new int[arrOfActions.length - 3];
					int actionssz = 0;
					for (int lll = 0; lll < arrOfActions.length; lll++) {
						if (threeActionsTaken[0] != arrOfActions[lll] && threeActionsTaken[1] != arrOfActions[lll]
								&& threeActionsTaken[2] != arrOfActions[lll])
							actions[actionssz++] = arrOfActions[lll];
					}
					tempNode = new MCNode(tempGameState.state, 3, actions, tree);
					int findChildEdges[] = { threeActionsTaken[0], threeActionsTaken[1], threeActionsTaken[2], -1 }; // fourth
																														// one
																														// is
																														// child
					int childCounter = 0;
					for (int acts = 0; acts < arrOfActions.length; acts++) {
						if (arrOfActions[acts] != threeActionsTaken[0] && arrOfActions[acts] != threeActionsTaken[1]
								&& arrOfActions[acts] != threeActionsTaken[2]) {
							findChildEdges[3] = arrOfActions[acts];
							tempNode.links[childCounter++].child = tree
									.findNode(MCNode.getStateAfterActions(currNode, findChildEdges, edges));
						}
					}
					tree.addNode(tempNode);

				}
				if (tempNode != null)
					tempNode.timesReached = shareNode.nS3FromMaster[i];
				// we must update the N(s) for this node that has this 3-edge
				// (NOT INDEX) combo
				// 1) find the 3-index edge combo
				// 2) convert index edge to edge combo (arrOfActions[x], ...,
				// ...)
				// 3) find tempNode with this addition
				// 4) if exists, set N(s). If not, create node...
			}
		}

		try {

			// generate all 2-digit edgeIndex:
			for (int i = 0; i < arrOfActions.length; i++) {
				for (int j = i + 1; j < arrOfActions.length; j++) {
					// ij is always a unique edgeIndex combination

					double Q2Val = 0;
					for (int pair = 1; pair <= 2; pair++) {
						int[] pairtype = new int[2];
						if (pair == 1) {
							pairtype[0] = i;
							pairtype[1] = j;
						} else if (pair == 2) {
							pairtype[0] = j;
							pairtype[1] = i;
						}
						// {pairtype[0], pairtype[1]} = 2-digit edgeINDEX combo
						// locate corresponding index in N(1, a) using
						// e1*(numActions-1)+e2-I where I = 1 if(e2>e1); 0 else
						// for pair 23 2*(4-1)+3-1
						int N1I = pairtype[0] * (numActions - 1) + pairtype[1];
						if (pairtype[1] > pairtype[0])
							N1I -= 1; // adjustment if second edge > first edge

						// UZI N1I assumes that the array is NOT using negative
						// numbers to compress
						// the information ... need to find out which element it
						// really is...
						// System.out.println(N1I + " index into " +
						// Arrays.toString(shareNode.nSA1FromMaster) + " of
						// length " + shareNode.nSA1FromMaster.length);
						if (shareNode.nSA1FromMaster[N1I] == 0) {
							if (pairtype[0] == j) {
								shareNode.qS2Compute[shareNode.qS2sz++] = 0;
								// System.out.println("skip action " +
								// arrOfActions[pairtype[0]] + ":" +
								// arrOfActions[pairtype[1]]);
							}
							continue;
						} else {
							int[] N2Is = new int[numActions - 2];
							int NsSum = 0;
							for (int p = 0; p <= numActions - 3; p++) {
								N2Is[p] = N1I * (numActions - 2) + p;
								NsSum += shareNode.nSA2FromMaster[N2Is[p]];
							}
							if (NsSum == 0) {
								if (pairtype[0] == j) {
									shareNode.qS2Compute[shareNode.qS2sz++] = 0;
									// System.out.println("skip action " +
									// arrOfActions[pairtype[0]] + ":" +
									// arrOfActions[pairtype[1]] + " uses
									// N(2,a)" +
									// Arrays.toString(N2Is) + " = " + NsSum);
								}
								continue;
							} else {
								// use the pair i am currently on and discard
								// System.out.println("actions " + arrOfActions[pairtype[0]] + ":" + arrOfActions[pairtype[1]] + " uses N(2,a)" + Arrays.toString(N2Is) + " = " + NsSum);
								int[] twoEdge = { arrOfActions[pairtype[0]], arrOfActions[pairtype[1]] };
								MCNode tempNode = tree.findNode(MCNode.getStateAfterActions(currNode, twoEdge, edges));
								
								
								if (tempNode == null) {
									// tempNode is null
									MCNode tempGameState = MCNode.getStateAfterActions(currNode, twoEdge, edges);
									int[] actions = new int[arrOfActions.length - 2];
									int actionssz = 0;
									for (int lll = 0; lll < arrOfActions.length; lll++) {
										if (arrOfActions[pairtype[0]] != arrOfActions[lll]
												&& arrOfActions[pairtype[1]] != arrOfActions[lll])
											actions[actionssz++] = arrOfActions[lll];
									}
									tempNode = new MCNode(tempGameState.state, 2, actions, tree); //EDIT: 11/2. Old one: tempNode = new MCNode(tempGameState.state, 2, actions, tree);
									
									int findChildEdges[] = { twoEdge[0], twoEdge[1], -1 }; // third one is child
									int childCounter = 0;
									for (int acts = 0; acts < arrOfActions.length; acts++) {
										if (arrOfActions[acts] != twoEdge[0] && arrOfActions[acts] != twoEdge[1]) {
											findChildEdges[2] = arrOfActions[acts];
											tempNode.links[childCounter++].child = tree.findNode(
													MCNode.getStateAfterActions(currNode, findChildEdges, 12));
										}
									}
									// do stuff... (create, set up, link to
									// processnode?
									tree.addNode(tempNode);
								}
								if (tempNode != null) {
									tempNode.timesReached = NsSum; // N(s)
																	// update
									for (int p = 0; p <= numActions - 3; p++) {
										tempNode.links[p].timesChosen = shareNode.nSA2FromMaster[N2Is[p]]; // N(s,
																											// a)
																											// update
										int thirdEdgeTaken = tempNode.links[p].action; // now
																						// we
																						// must
																						// find
																						// the
																						// index
																						// thirdEdgeTaken
																						// is
																						// stored
																						// in
																						// arrOfActions
										int missingIndex = -1;
										for (int ind = 0; ind < arrOfActions.length; ind++) {
											if (thirdEdgeTaken == arrOfActions[ind]) {
												missingIndex = ind;
												break;
											}
										}

										int indexForQ3 = findQ3Index(pairtype[0], pairtype[1], missingIndex,
												numActions);// given
															// a
															// 3-digit
															// combination,
															// find
															// the
															// index
															// where
															// it
															// is
															// located
															// if
															// the
															// list
															// goes
															// like
															// (012,
															// 013,
															// 014,
															// 023,
															// 024,
															// 034,
															// 123,
															// 124,
															// 134...)

										tempNode.links[p].synchUpdate(shareNode.qS3Compute[indexForQ3], NsSum, c);

									}
								}

								for (int thirddigit = 0; thirddigit < arrOfActions.length; thirddigit++) {
									if (thirddigit != pairtype[0] && thirddigit != pairtype[1]) {
										// valid third digit (e.g. 01 -> 012,
										// 013)
										// or (e.g. 12 -> 012 & 123)
										// sort 3 digits (after using
										// arrOfActions[pairtype[0]],
										// arrOfActions[pairtype[1]],
										// arrOfActions[thirddigit]
										int[] tripleEdge = { arrOfActions[pairtype[0]], arrOfActions[pairtype[1]],
												arrOfActions[thirddigit] };
										// Arrays.sort(tripleEdge); //012 or 013
										int indexToCheckN2Is = thirddigit;
										if (thirddigit > pairtype[0])
											indexToCheckN2Is--;
										if (thirddigit > pairtype[1])
											indexToCheckN2Is--;
										int threeEdgeActionVisited = shareNode.nSA2FromMaster[N2Is[indexToCheckN2Is]]; // N(s,
																														// a)
																														// level
																														// 2
										int indexToCheckQ3 = findQ3Index(pairtype[0], pairtype[1], thirddigit,
												numActions);// given
															// an
															// increasingly
															// ordered
															// 3-digit
															// combination,
															// find
															// the
															// index
															// where
															// it
															// is
															// located
															// if
															// the
															// list
															// goes
															// like
															// (012,
															// 013,
															// 014,
															// 023,
															// 024,
															// 034,
															// 123,
															// 124,
															// 134...)

										double tempQ3Val = shareNode.qS3Compute[indexToCheckQ3];

										Q2Val += (double) threeEdgeActionVisited / NsSum * tempQ3Val; // Q(s)
																										// lvl
																										// 3
																										// value
									}
								}
								// I have the Q2Val for {pairtype[0],
								// pairtype[1]}
								// System.out.println("adding Q(2) for " +
								// pairtype[0] + ":" + pairtype[1]);
								shareNode.qS2Compute[shareNode.qS2sz++] = Q2Val;

								break; // should break permutation loop
							}
						}
					}

				}
			}

		} catch (Exception e) {
			System.out.println("HERE .... in simulation " + simsDEBUG + " and move " + movesCompleted);
			e.printStackTrace();
		}

		// Use Q(2) for Q(1) -
		for (int i = 0; i < arrOfActions.length; i++) { // 0,1,2,3,4,5,6,...,11
			int N0I = i;
			int[] N1Is = new int[numActions - 1];
			int NsSum = 0;
			double Q1Val = 0;
			// use N(0, a) to find indices in N(1, a).
			for (int p = 0; p <= numActions - 2; p++) {
				N1Is[p] = N0I * (numActions - 1) + p;
				NsSum += shareNode.nSA1FromMaster[N1Is[p]];
			}
			if (NsSum == 0) {
				shareNode.qS1Compute[shareNode.qS1sz++] = 0; // if that node is
																// never visited
				continue;
			}
			int[] oneEdge = { arrOfActions[i] };
			MCNode tempNode = tree.findNode(MCNode.getStateAfterActions(currNode, oneEdge, edges)); // do
																									// I
																									// use
																									// "tree"
																									// for
																									// the
																									// MCTree?
			if (tempNode == null) {
				MCNode tempGameState = MCNode.getStateAfterActions(currNode, oneEdge, edges);
				int[] actions = new int[arrOfActions.length - 1];
				int actionssz = 0;
				for (int lll = 0; lll < arrOfActions.length; lll++) {
					if (arrOfActions[i] != arrOfActions[lll])
						actions[actionssz++] = arrOfActions[lll];
				}
				tempNode = new MCNode(tempGameState.state, 1, actions, tree);
				int findChildEdges[] = { oneEdge[0], -1 }; // second one is
															// child
				int childCounter = 0;
				for (int acts = 0; acts < arrOfActions.length; acts++) {
					if (arrOfActions[acts] != oneEdge[0]) {
						findChildEdges[1] = arrOfActions[acts];
						tempNode.links[childCounter++].child = tree
								.findNode(MCNode.getStateAfterActions(currNode, findChildEdges, edges));
					}
				}
				tree.addNode(tempNode);
				// do stuff..
			}
			// now node tempNode exists
			if (tempNode != null) {
				tempNode.timesReached = NsSum; // N(s) update
				for (int p = 0; p <= numActions - 2; p++) {
					tempNode.links[p].timesChosen = shareNode.nSA1FromMaster[N1Is[p]]; // N(s,
																						// a)
																						// update

					int secondEdgeTaken = tempNode.links[p].action; // now we
																	// must find
																	// the index
																	// thirdEdgeTaken
																	// is stored
																	// in
																	// arrOfActions
					int missingIndex = -1;
					for (int ind = 0; ind < arrOfActions.length; ind++) {
						if (secondEdgeTaken == arrOfActions[ind]) {
							missingIndex = ind;
							break;
						}
					}

					int indexForQ2 = findQ2Index(i, missingIndex, numActions); // given
																				// 2-digit
																				// combo
																				// (index,
																				// not
																				// edge
																				// itself),
																				// find
																				// position...
					tempNode.links[p].synchUpdate(shareNode.qS2Compute[indexForQ2], NsSum, c);
				}
			}

			for (int seconddigit = 0; seconddigit < arrOfActions.length; seconddigit++) {
				if (seconddigit != i) {
					// valid two digit (e.g. 0 -> 01, 02, 03, 04) or (e.g. 2 ->
					// 20, 21, 23, 24)
					// sort 2 digits (after using arrOfActions[i],
					// arrOfActions[seconddigit]
					int[] twiceEdge = { arrOfActions[i], arrOfActions[seconddigit] };
					// Arrays.sort(tripleEdge); //012 or 013
					int indexToCheckN1Is = seconddigit;
					if (seconddigit > i)
						indexToCheckN1Is--;
					int twoEdgeActionVisited = shareNode.nSA1FromMaster[N1Is[indexToCheckN1Is]]; // N(s,
																									// a)
																									// level
																									// 1
					int indexToCheckQ2 = findQ2Index(i, seconddigit, numActions);// given
																					// an
																					// increasingly
																					// ordered
																					// 3-digit
																					// combination,
																					// find
																					// the
																					// index
																					// where
																					// it
																					// is
																					// located
																					// if
																					// the
																					// list
																					// goes
																					// like
																					// (012,
																					// 013,
																					// 014,
																					// 023,
																					// 024,
																					// 034,
																					// 123,
																					// 124,
																					// 134...)
					double tempQ2Val = shareNode.qS2Compute[indexToCheckQ2];

					Q1Val += (double) twoEdgeActionVisited / NsSum * tempQ2Val; // Q(s)
																				// lvl
																				// 2
																				// value
				}
			}
			shareNode.qS1Compute[shareNode.qS1sz++] = Q1Val;

		}
		if (rank == -1) {
			printArr("Missing Actions: ", arrOfActions);
			printArr("Q(3S) list: ", shareNode.qS3Compute);
			printArr("Q(2S) list: ", shareNode.qS2Compute);
			printArr("Q(1S) list: ", shareNode.qS1Compute);
		}
		// Use Q(1) to update level 0 links and node, NO NEED to find Q(0)
		int nSum = sumMyArray(shareNode.nSA0FromMaster); // find N(s) of root
		currNode.timesReached = nSum; // N(s)
		// if (rank ==1) {
		// File file = new
		// File("/home/uta/javaworkspaces/usingopenMPI/DaB28July/errorInfo.txt");
		// FileWriter fr = null;
		// BufferedWriter br = null;
		// //String dataWithNewLine=data+System.getProperty("line.separator");
		// try{
		// fr = new FileWriter(file, true);
		// br = new BufferedWriter(fr);
		// br.write("rank + " + rank + "level 0 synch step nSum " + nSum + "
		// currnode " + currNode.state.longState + "\n PRIOR TO UPDATE:\n");
		// StringBuilder linkInfo = new StringBuilder();
		// for (int kp=0; kp < currNode.links.length; kp++)
		// if (currNode.links[kp] == null)
		// linkInfo.append("link " + kp + " is null \n");
		// else
		// linkInfo.append("link " + kp + ": "
		// + currNode.links[kp].action + " "
		// + currNode.links[kp].timesChosen + " "
		// + currNode.links[kp].rewards + " "
		// + currNode.links[kp].bonus + "\n"
		// );
		// br.write(linkInfo.toString());
		// } catch (IOException e) {
		// e.printStackTrace();
		// }finally{
		// try {
		// br.close();
		// fr.close();
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		// }
		// }
		double Q0Val = 0; // no need to find Q(S) for level 0 as we do NOT use
							// it to calculate R(s, a) level 0
		for (int i = 0; i < arrOfActions.length; i++) {
			currNode.links[i].timesChosen = shareNode.nSA0FromMaster[i]; // N(s,
																			// a)
																			// update
			currNode.links[i].synchUpdate(shareNode.qS1Compute[i], nSum, c);
			/*
			 * currNode.links[i].rewards = shareNode.qS1Compute[i] *
			 * currNode.links[i].timesChosen; //R(s, a) update
			 * currNode.links[i].updateBonus(nSum, c);
			 */
		}

		return toReturn;

	}

	public static int sumMyArray(int[] arr) {
		int mySum = 0;
		for (int i = 0; i < arr.length; i++) {
			mySum += arr[i];
		}
		return mySum;
	}

	public static int findQ3Index(int one, int two, int three, int numActions) {
		int[] combo = { one, two, three };
		Arrays.sort(combo);
		int skip = 0;
		int n = numActions;
		int k = combo.length;
		for (int i = 0; i < combo[0]; i++) {
			skip += choose(n - (i + 1), k - 1);
		}
		for (int i = 0; i < combo[1] - combo[0] - 1; i++) {
			skip += (n - combo[0] - 1) - (i + 1);
		}
		skip += combo[2] - combo[1] - 1;
		return skip;

	}

	public static int findQ2Index(int one, int two, int numActions) {
		int[] combo = { one, two };
		Arrays.sort(combo);
		int skip = 0;
		int n = numActions; // 24 0-11, skip 0: 11, skip 1: 10
		int k = combo.length;
		for (int i = 0; i < combo[0]; i++) {
			skip += n - (i + 1);
		}
		skip += combo[1] - combo[0] - 1;
		return skip;
	}

	public static int choose(int n, int r) {
		// 5, 3
		int top = 1;
		for (int l = r + 1; l <= n; l++) {
			top = top * l;
		}
		int bot = 1;
		for (int l = 2; l <= (n - r); l++) {
			bot = bot * l;
		}
		return top / bot;

	}

	static int[][] permanentcombList;
	static int sz;

	public static void generateCombinationArray(MCNode root) {
		int[] remainingActions = root.getArrayOfActions();
		int r = 3;
		int n = remainingActions.length;
		int items = (n * (n - 1) * (n - 2)) / 6;
		permanentcombList = new int[items][3];
		sz = 0;
		printCombination(remainingActions, n, r); // arr, 7, 3
	}

	// The main function that prints all combinations of size r
	// in arr[] of size n. This function mainly uses combinationUtil()
	public static void printCombination(int arr[], int n, int r) // arr, 7, 3
	{
		// A temporary array to store all combination one by one
		int data[] = new int[r];

		// Print all combination using temprary array 'data[]'
		combinationUtil(arr, data, 0, n - 1, 0, r); // arr, data[3], 0, 6, 0, 3
	}

	/*
	 * arr[] ---> Input Array data[] ---> Temporary array to store current
	 * combination start & end ---> Staring and Ending indexes in arr[] index
	 * ---> Current index in data[] r ---> Size of a combination to be printed
	 */
	static void combinationUtil(int arr[], int data[], int start, int end, int index, int r) // arr,
																								// data[3],
																								// 0,
																								// 6,
																								// 0,
																								// 3
	{
		// Current combination is ready to be printed, print it
		if (index == r) {
			for (int togo = 0; togo < r; togo++) {
				permanentcombList[sz][togo] = data[togo];
			}
			sz++;
			return;
		}

		// replace index with all possible elements. The condition
		// "end-i+1 >= r-index" makes sure that including one element
		// at index will make a combination with remaining elements
		// at remaining positions
		for (int i = start; i <= end && end - i + 1 >= r - index; i++) {
			data[index] = arr[i];
			combinationUtil(arr, data, i + 1, end, index + 1, r);
		}
	}

	// prints the contents of the list
	public static void printList(String res, ArrayList<Integer> arr) {
		for (int elem : arr)
			res = res + elem + " ";
		System.out.println("RANK " + rank + ": " + res + ", size " + arr.size());
	}

	// prints the contents of the array
	public static void printArr(String res, int[] arr) {
		for (int i = 0; i < arr.length; i++) {
			res = res + arr[i] + " ";
		}
		System.out.println(res);
	}

	public static void printArr(String res, double[] arr) {
		for (int i = 0; i < arr.length; i++) {
			res = res + arr[i] + " ";
			// System.out.print(arr[i]+" ");
		}
		System.out.println(res);
	}

	// arrays must be the same length, multipies them together
	public static double[] arrayMultiply(int[] arr1, double[] arr2) {
		double[] product = new double[arr1.length];
		for (int i = 0; i < arr1.length; i++) {
			product[i] = (arr1[i] * arr2[i]);
		}
		return product;
	}

	// kind of sums an array
	public static double[] sumWithin(double[] arr, int num) {
		double[] sum = new double[num];
		for (int i = 0; i < arr.length; i++) {
			sum[i % num] += arr[i];
		}

		return sum;
	}

	public static int[] sumWithin(int[] arr, int num) { // send masterN with num
														// = 2 (2 tasks)
		int[] sum = new int[num];
		for (int i = 0; i < arr.length; i++) {
			sum[i % num] += arr[i];
		}

		return sum;
	}

	// divides two arrays by index
	public static double[] arrayDivide(int[] arr1, double[] arr2) {
		double[] result = new double[arr1.length];
		for (int i = 0; i < arr1.length; i++) {
			result[i] = (arr2[i] / arr1[i]);
		}
		return result;
	}

	public static void printAveTime(String res, long[][] arr) {
		String printStr = "";
		printStr += res;

		for (int i = 0; i < arr.length; i++) {
			if (arr[i][1] == 0) {
				continue;
			}

			printStr += " Move " + i + ": " + arr[i][0] / (double) arr[i][1];
		}
		System.out.println(printStr);
	}

	public static void printNumTime(String res, long[][] arr) {
		String printStr = "";

		printStr += res;
		for (int i = 0; i < times.length; i++) {
			printStr += " Move " + i + ": " + arr[i][1];
		}
		System.out.println(printStr);
	}

	public static void isGoodFirstAction(int act) {
		// 2x2: outside edge

		if (act == 3 || act == 5 || act == 6 || act == 8) {
			// bad choice
			firstMove[firstMoveSZ++] = 0;
		} else {
			// good choice
			//which move
			firstMove[firstMoveSZ++] = 1;
		}
	}

	/**
	 * Plays a single game between two MCTS players.
	 * 
	 * @param tree
	 *            The tree for player one.
	 * @param game
	 *            The game for player one.
	 * @param tree2
	 *            The tree for player two.
	 * @param game2
	 *            The game for player two.
	 * @param simulationsPerTurn1
	 *            The number of simulations given to player one.
	 * @param simulationsPerTurn2
	 *            The number of simulations given to player two.
	 * @return An integer representing the result for player one.
	 */
	public static int testGameParallel(MCTree tree, DotsAndBoxes game, MCTree tree2, DotsAndBoxes game2,
			int simulationsPerTurn1, int simulationsPerTurn2) throws MPIException {

		GameState terminalState = null;
		if (edges > 60) {
			terminalState = new GameState(new BigInteger("2").pow(edges).subtract(new BigInteger("1")));
		} else {
			terminalState = new GameState((long) Math.pow(2, edges) - 1);
		}

		// the current node of each tree
		MCNode currentNode = tree.root;
		MCNode currentNode2 = tree2.root;

		// the game variables
		int action = 0;
		boolean playerOneTurn = true;
		int p1Score = 0;
		int p2Score = 0;

		// the number of boxes that are completed or have two edges
		int twoOrFour = 0;

		// board[i] is the number of taken edges for box i
		int[] board = new int[width * height];

		// a clone to pass to the simulate method
		// int[] boardClone = new int[width * height];

		int startsWell = 1;

		// for every turn
		movesCompleted = 0;
		boolean stop = false;
		while (!currentNode.state.equals(terminalState) && !stop) {
			if (p1Score > (width * width) / 2 || p2Score > (width * width) / 2) {
				break;
			}

			int sims = playerOneTurn ? simulationsPerTurn1 : simulationsPerTurn2;
			simsDEBUG = sims;

			// get the action based on the current player
			if (playerOneTurn) {
				int simsPerformed = 0;
				long start = System.nanoTime();

				// perform the simulations for this move
				while (simsPerformed < sims) {
					// give player one's game, tree, node, and score
					// System.out.println("rank " + rank + " SIMS: " + sims + ",
					// simulationsPerTurn1: " + simulationsPerTurn1 + ",
					// shareInfoEvery" + shareInfoEvery);
					// System.out.println("TEST1: " + (sims<simulationsPerTurn1)
					// + ", TEST2: " + (sims%shareInfoEvery==0));
					int[] boardClone = new int[width * height];
					for (int bc = 0; bc < width * height; bc++)
						boardClone[bc] = board[bc];
					simulate(currentNode.state, p1Score - p2Score, currentNode, terminalState, tree, game, boardClone,
							twoOrFour);
					simsPerformed++;
					simsDEBUG = simsPerformed;
					//each simulation...
					if (maxTasks > 1) {
						//each simulation... parallel
						
						if (simsPerformed % shareInfoEvery == 0 &&
						// only share if there are atleast 2 moves left
								currentNode.state.getBitCount() </*changed != game.edges-1 to < games.edges-2*/ game.edges-2) {
							
							try {
								doStuff(currentNode, tree, game, tree2, game2);
								 //System.out.println ("PA rank " + rank + " doStuff(): SIMS - done " + sims + " edges drawn: " + currentNode.state.getBitCount() + " edges = " + currentNode.state.getEdgesDrawn());
							} catch (Exception e) {
								// display the rank, the exception message (if
								// any) and the
								// stacktrace

								// System.out.println(" rank " + rank + "
								// crashed " + e.getMessage());
								System.out.println(" rank " + rank + " crashed BETTER STOP NOW " + movesCompleted + " "
										+ simsDEBUG);
								// stop this set of simulations and the game
								sims = 0;
								stop = true;
								// System.out.println("BETTER STOP NOW");
								// e.printStackTrace();

								// abort processing
								// MPI.COMM_WORLD.abort(1);
							}

						}
						//end
						
					}

				}
				
				long end = System.nanoTime();

				try {
					times[currentNode.depth][1]++;
					times[currentNode.depth][0] = times[currentNode.depth][0] + (end - start);
				} catch (ArrayIndexOutOfBoundsException e) {
					System.out.println("Array Index Error");
					return -10;
				}

				action = currentNode.getNextAction(0);
				
			
			} else {
				// perform the simulations for this move
				int simsPerformed = 0;
				while (simsPerformed < sims) {
					// give player two's game, tree, node, and score
					int[] boardClone = new int[width * height];
					for (int bc = 0; bc < width * height; bc++)
						boardClone[bc] = board[bc];
					simulate(currentNode2.state, p2Score - p1Score, currentNode2, terminalState, tree2, game2,
							boardClone, twoOrFour);

					simsPerformed++;
					simsDEBUG = simsPerformed;
				}

				action = currentNode2.getNextAction(0);
				
			}
			if (maxTasks > 1) {
				if (rank == 0) {

					int[] tempAction = { action };
					MPI.COMM_WORLD.bcast(tempAction, 1, MPI.INT, 0);
				}
				if (rank != 0) {
					// this is overriding the action for each compute node with
					// the action selected by the master node
					int[] tempActionCompute = new int[1];
					MPI.COMM_WORLD.bcast(tempActionCompute, 1, MPI.INT, 0);
					action = tempActionCompute[0];
				}
			}

			if (currentNode.state.longState == 0)
				if ((action == 3 || action == 5 || action == 6 || action == 8)) {
					startsWell = 0;
					badChoice++;
				} else
					goodChoice++;

			// get the points for this move
			int taken = 0;

			// increment the edges for each box which adjoins action
			for (int b = 0; b < game.edgeBoxes[action].length; b++) {
				board[game.edgeBoxes[action][b]]++;
				// boardClone[game.edgeBoxes[action][b]]++;

				if (board[game.edgeBoxes[action][b]] == 4) {
					taken++;
					twoOrFour++;
				} else if (board[game.edgeBoxes[action][b]] == 2) {
					twoOrFour++;
				}
			}

			if (rank <= 0 && currentNode.state.longState == 0)
				isGoodFirstAction(action);

			if (TESTPRINT) {
				// get the point for this move
				System.out.println("rank " + rank + " about to determine score with action state " + action + " "
						+ currentNode.state.longState);
			}

			if (TESTPRINT) {
				System.out.println("rank " + rank + " after " + movesCompleted + " moves taken = " + taken);
			}
			if (TESTPRINT) {
				System.out.println("rank " + rank + " action: " + action);
				System.out.println("rank " + rank + " prev-state " + currentNode.state.longState);
			}
			// update the currentNodes
			currentNode = currentNode.getNode(action, BEHAVIOR_EXPANSION_ALWAYS);
			currentNode2 = currentNode2.getNode(action, BEHAVIOR_EXPANSION_ALWAYS);

			// System.out.println ("rank " + rank + " next state edges drawn "+
			// currentNode2.state.getEdgesDrawn());

			/* possibly circumvent the null pointer */
			if (currentNode == null || currentNode2 == null) {
				System.out.println("Null Error: " + currentNode == null ? "Player 1" : "Player 2");
				return -10;
			}

			if (playerOneTurn) {
				p1Score += taken;
				playerMove.add(new PlayerMove(action, 1, p1Score - p2Score));
			} else {
				p2Score += taken;
				playerMove.add(new PlayerMove(action, 2, p1Score - p2Score));
			}
			if (TESTPRINT) {
				System.out.println("rank " + rank + " post-state " + currentNode.state.longState);
				System.out.println(" rank = " + rank + " score after " + movesCompleted + "moves  is " + p1Score);
			}
			playerOneTurn = taken > 0 ? playerOneTurn : !playerOneTurn;
			movesCompleted++;
		}

		//System.out.println("RANK "+ rank + ": " + Arrays.toString(simulationMoves));
		
		int p1Net = p1Score - p2Score;

		if (TESTPRINT) {
			System.out.println(" rank = " + rank + " is done after all moves  and netscore " + p1Net);
		}

		// outside edge selected
		if (startsWell == 1) {
			if (p1Net < 0)
				startOK[0]++;
			if (p1Net == 0)
				startOK[1]++;
			if (p1Net > 0)
				startOK[2]++;
		} else {
			if (p1Net < 0)
				startBad[0]++;
			if (p1Net == 0)
				startBad[1]++;
			if (p1Net > 0)
				startBad[2]++;

		}

		return p1Net > 0 ? 1 : p1Net < 0 ? -1 : 0;
	}

	public static void competitionParallel(MCTree tree, DotsAndBoxes game, MCTree tree2, DotsAndBoxes game2,
			int simulationsPerTurn1, int simulationsPerTurn2, int matches) throws MPIException {

		int wins = 0;
		int losses = 0;
		int draws = 0;
		goodChoice = 0;
		badChoice = 0;

		double totalAveDepth = 0;
		long totalNodes = 0;

		/* plays a match */
		// System.out.println ("DETECTIVE prior to loop in
		// competitionParallel");
		for (int i = matches; i > 0; i--) {
			// System.out.println("Play Match: " + (matches-i+1));
			double[] results = match(tree, game, tree2, game2, simulationsPerTurn1, simulationsPerTurn2, true);
			// if (TESTIT) {
			// testPolicy(false);
			// }

			int result = (int) results[0];
			totalAveDepth += results[1];
			totalNodes += results[2];

			if (result == 1)
				wins++;
			else if (result == 0) {
				draws++;
			} else {
				losses++;
			}
			
			//Pranay Agrawal changes: 11/5
			/**if(rank<=0) {
			System.out.println(playerMove + "\n");
			
			
			ArrayList<Integer> moveP = new ArrayList<Integer>();
			
			//  __ __  
			// |__|__| ==>  
			// |__|__| ==>
			
			
			
			for(int ii=0;ii<3;ii++) {
				moveP.add(playerMove.get(ii).move);
				Collections.sort(moveP);
				
				System.out.println(moveP);
				
				String layer1= "";
				String layer2= "";
				String layer3= "";
				
				boolean is5 = moveP.contains(5);
				boolean done5=true;
				boolean is6 = moveP.contains(6);
				boolean done6=true;
				boolean is10 = moveP.contains(10);
				boolean done10=true;
				boolean is11 = moveP.contains(11);
				boolean done11=true;
				
				for(int j=0; j<moveP.size(); j++) {
					if(moveP.get(j) == 0)
						layer1+= " __ ";
					else
						layer1+= "    ";
					if(moveP.get(j) == 1)
						layer1+= "__ ";
					else
						layer1+= "   ";
					
					//LAYER 2
					if(moveP.get(j) == 2)
						layer2+= "|";
					else
						layer2+= " ";
					
					if(is5 && done5) {
						layer2+= "__";
						done5=false;
					}
					else
						layer2+= "  ";
					
					if(moveP.get(j) == 3)
						layer2+= "|";
					else
						layer2+= " ";
					
					if(is6 && done6) {
						layer2+= "__";
						done6=false;
					}
					else
						layer2+= "  ";
					
					if(moveP.get(j) == 4)
						layer2+= "|";
					else
						layer2+= " ";
					
					//LAYER 3
					if(moveP.get(j) == 7)
						layer3+= "|";
					else
						layer3+= " ";
					
					if(is10 && done10) {
						layer3+= "__";
						done10=false;	
					}
					else
						layer3+= "  ";
					
					if(moveP.get(j) == 8)
						layer3+= "|";
					else
						layer3+= " ";
					
					if(is11 && done11) {
						layer3+= "__";
						done11=false;
					}
					else
						layer3+= "  ";
					
					if(moveP.get(j) == 9)
						layer3+= "|";
					else
						layer3+= " ";
				}	
				
				layer1 += "     ";
				layer2 += " ==> ";
				layer3 += " ==> ";
				
				System.out.println(layer1);
				System.out.println(layer2);
				System.out.println(layer3);
				
			}
			
			//System.out.println();
			
			playerMove = new ArrayList<PlayerMove>();
			}**/
		}

		// System.out.println ("DETECTIVE after to loop in
		// competitionParallel");

		/* Results */
		int numFirstMove = 0;
		if (rank <= 0) {
			for (int move : firstMove)
				if (move == 1)
					numFirstMove++;
			System.out.println(numFirstMove + " 1s. " + rank + " FIRST move made: " + Arrays.toString(firstMove));
		}

		// System.out.println ("DETECTIVE printed results about move 1 in
		// competitionParallel");

		if (rank == 0) {
			System.out.println(height + "x" + width + " c=" + c + " matches=" + matches + " sims=" + simulationsPerTurn1
					+ "," + simulationsPerTurn2 + " p1=" + (game.scored ? "sc+" : "nsc+") + (game.asymmetrical ? "s" : "ns")
					+ " p2=" + (game2.scored ? "sc+" : "nsc+") + (game2.asymmetrical ? "s" : "ns\n") + 
					
					"l d w\ngood move\nmove bad\n"
					+ losses + " " + draws + " " + wins + "\n"
					+ startOK[0] + " " + startOK[1] + " " + startOK[2] + "\n"
					+ startBad[0] + " " + startBad[1] + " " + startBad[2]
					);

			System.out.println("nodes: " + totalNodes / matches);
			System.out.println("average depth: " + (totalAveDepth / matches));

			printAveTime("Average Times RANK " + rank, times);
			printNumTime("Number of Times Chosen " + rank, times);
		}
	}

	/*----------------------------------------------------------------------------------------------*/
}
