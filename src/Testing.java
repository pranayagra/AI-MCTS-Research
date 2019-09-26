


public class Testing {


public static void main (String [] args) {
    DotsAndBoxes game = new DotsAndBoxes(2, 2, false, false);

  
    // set the current state and create an MCNode
    GameState start = new GameState(0L);


    // add edges 3, 8, 9,10
    int [] addMe = {3, 8, 9, 10};
    int [] edges2= {2,1,5, 6, 11};


    // Testing only with game and gamestate
    System.out.println ("TESTING - using getSuccessorState function of the game class");
    GameState secondState = game.getSuccessorState (start, addMe); 

    System.out.println ("edges in second state: " + secondState.getEdgesDrawn());	

    GameState thirdState = game.getSuccessorState(secondState, edges2);
    System.out.println ("edges in third state: " + thirdState.getEdgesDrawn());


    // Now testing with MCNode
    
    System.out.println ("\n\nTESTING - using getStateAfterActions  function ");
    MCNode testNode = new MCNode(start);
    MCNode second = MCNode.getStateAfterActions(testNode, addMe,12);
    System.out.println("edges added to MCNode " + second.state.getEdgesDrawn());	
	

    MCNode third = MCNode.getStateAfterActions(second, edges2,12);
    System.out.println("edges added to MCNode " + third.state.getEdgesDrawn());	
    }    

}


