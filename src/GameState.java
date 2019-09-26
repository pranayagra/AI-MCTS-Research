//package MCTS;

import java.math.BigInteger;

/**
 * Represents a state consisting of a single integer.
 * @author      Jared Prince
 * @version     1.0
 * @since       1.0
 */

public class GameState {

	/** The state represented as a long (when small enough).
	 */
	public long longState;
	
	/** The state represented as a BigInteger.
	 */
	BigInteger bigState = null;
	
	/**
	 * Constructor using a long.
	 * 
	 * @param  state The integer state represented as a long.
	 */
	public GameState(long state){
		this.longState = state;
	}
	
	/**
	 * Constructor using a BigInteger.
	 * 
	 * @param  state The integer state represented as a BigInteger.
	 */
	public GameState(BigInteger state){
		this.bigState = state;
	}
	
	/**
	 * Constructor using a String.
	 * 
	 * @param  state The integer state represented as a String.
	 * @param  inBinary True if the state is in binary, false if the state is in decimal.
	 */
	public GameState(String state, boolean inBinary){
		if(inBinary){
			try{
				longState = new Long(Long.parseLong(state,2));
			} catch (Exception e) {
				//parse binary to big int
				bigState = new BigInteger(state, 2);
			}
		}
		
		else{
			try{
				longState = new Long(state);
			} catch (NumberFormatException e) {
				bigState = new BigInteger(state);
			}
		}
	}
	
	/**
	 * Determines if another state is equal to this one.
	 * 
	 * @param  secondState The state to be compared.
	 * @return True if this is equal to secondState, false otherwise.
	 */
	public boolean equals(GameState secondState){		
		if(longState == secondState.longState){
			if(bigState == null && secondState.bigState == null){
				return true;
			}
			
			if(bigState != null && secondState.bigState != null){
				if(bigState.equals(secondState.bigState)){
					return true;
				}
			}
			
			return false;
		}
		
		return false;
	}
	
	
	/**
	 * Gets the state as a binary string.
	 * 
	 * @return  A String representing the integer state in binary form.
	 */
	public String getBinaryString(){
		if(bigState != null){
			return bigState.toString(2);
		}
		
		return Long.toBinaryString(longState);
	}
	
	/**
	 * Gets the state as a string.
	 * 
	 * @return  A String representing the integer state in decimal form.
	 */
	public String getString(){
		if(bigState != null){
			return bigState.toString();
		}
		
		return Long.toString(longState);
	}

	/**  Get string of edges in state
	*  @return A string containing the drawn edges
	**/
	public String getEdgesDrawn() {
	   String binary = getBinaryString();
	   while (binary.length() < 12)
                 // add padding
                 binary = "0" + binary;
   
	   String res = " start: " + getString()  + " " + binary + " ";
	   for (int i=0; i< binary.length(); i++) {
		if (binary.substring(i,i+1).equals("1"))
		   res += i + " ";
	   }
	   return res;
	
	}
	
	/**
	* Get number of edges in state 
	* @return Number of edges in state
	**/
	public int getBitCount(){
	  if (bigState != null)
	     return bigState.bitCount();

	  return Long.bitCount(longState);
	}
}
