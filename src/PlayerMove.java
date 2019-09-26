public class PlayerMove {

	public int move;
	public int player;
	public int score;
	
	
	public PlayerMove(int m, int p, int s) {
		move = m;
		player = p;
		score = s;
	}
	
	public String toString() {
		return "[move: " + move + ", player: " + player + ", score: " + score + "]"; 
	}
	
}