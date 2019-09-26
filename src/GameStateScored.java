//package MCTS;

import java.math.BigInteger;

/**
 * Represents a state consisting of two integers.
 * @author      Jared Prince
 * @version     1.0
 * @since       1.0
 */

public class GameStateScored extends GameState{

	/** The net score for the player who is in control in this state.
	 */
	int playerNetScore;
	
	/**
	 * Constructor using a BigInteger.
	 * 
	 * @param  state The integer state represented as a BigInteger.
	 * @param  score The net score for the controlling player as an integer.
	 */
	public GameStateScored(BigInteger state, int score) {
		super(state);
		playerNetScore = score;
	}
	
	/**
	 * Constructor using a long.
	 * 
	 * @param  state The integer state represented as a long.
	 * @param  score The net score for the controlling player as an integer.
	 */
	public GameStateScored(long state, int score){
		super(state);
		this.playerNetScore = score;
	}
	
	/**
	 * Constructor using a String.
	 * 
	 * @param  state The integer state represented as a String.
	 * @param  score The net score for the controlling player as an integer.
	 * @param  inBinary True if the state is in binary, false if the state is in decimal.
	 */
	public GameStateScored(String state, int score, boolean inBinary){
		super(state, inBinary);
		playerNetScore = score;
	}

	/**
	 * Determines if another state is equal to this one.
	 * 
	 * @param  secondState The state to be compared.
	 * @return True if this is equal to secondState, false otherwise.
	 */
	public boolean equals(GameStateScored secondState){
		
		if(secondState.playerNetScore != playerNetScore){
			return false;
		}
		
		if(!super.equals(secondState)){
			return false;
		}
		
		return true;
	}
	
	/**
	 * Determines if another state is equal to this one.
	 * 
	 * @param  secondState The state to be compared.
	 * @return True if this is equal to secondState, false otherwise.
	 */
	public boolean equals(GameState secondState){
		if(secondState instanceof GameStateScored){
			return equals((GameStateScored) secondState);
		}
		
		if(!super.equals(secondState)){
			return false;
		}
		
		return true;
	}
	
	/**
	 * Gets the score.
	 * 
	 * @return  The net score for the controlling player.
	 */
	public int getScore(){
		return playerNetScore;
	}
	
	/**
	 * Gets the state as a string.
	 * 
	 * @return  A String representing the integer state in decimal form.
	 */
	public String getString(){
		String str = null;
		
		if(bigState != null){
			str = bigState.toString();
		} else {
			str = Long.toString(longState);
		}
		
		if(this.playerNetScore < 0){
			str = str + playerNetScore;
		} else {
			str = str + "+" + playerNetScore;
		}
		
		return str;
	}
}
