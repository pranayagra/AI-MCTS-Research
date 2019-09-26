//package MCTS;

/**
 * The methods to be implemented by any game used for the Monte Carlo tree search.
 * @author      Jared Prince
 * @version     1.0
 * @since       1.0
 */

public abstract class MCGame {
	
	/**
	 * Finds all possible actions from a given state.
	 * 
	 * @param  state The state before the move is selected.
	 * @return Integer array representing all possible moves from the given state.
	 */
	public abstract int[] getActions(GameState state);
	
	/**
	 * Gets the successor of a given state.
	 * <p>
	 * This is the function called by the MCNode when a new node is created. Because the states in all MCNodes are cast to the 
	 * superclass (GameState), it is the job of the MCGame to downcast to the subclass, get the successor, and cast back to the 
	 * superclass before returning (if a subclass is used).
	 * 
	 * @param  state The state from which a move is made.
	 * @param  action An integer representing which move is made.
	 * @return The state after the move is made.
	 */
	public abstract GameState getSuccessorState(GameState state, int action);

	/**
	 * Gets the successor of a given state.
	 * <p>
	 * This is the function called by the MCNode when a new node is created. Because the states in all MCNodes are cast to the 
	 * superclass (GameState), it is the job of the MCGame to downcast to the subclass, get the successor, and cast back to the 
	 * superclass before returning (if a subclass is used).
	 * 
	 * @param  state The state from which the moves are made.
	 * @param  actions An array of integers representing which moves are made.
	 * @return The state after the moves are made.
	 */
	public abstract GameState getSuccessorState(GameState state, int [ ] actions);
}
