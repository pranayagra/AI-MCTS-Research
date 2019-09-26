
public class TestBitWise {

	public static void main(String[] args) {
		
		 //getBitEdge(edge)
	     
		
//		System.out.println(00111 & 10101); //65
//		System.out.println(Integer.bitCount(00111 & 10101)); //2
//		System.out.println();
		
		
//		*state is long! 
		System.out.println(Integer.bitCount(2397 & getBitEdge(7))); //10101
		
	}
	
	public static int getBitEdge(int edge) {
		int x = 1;
		x = x<<(12-edge-1);
		return x;
	}

}
