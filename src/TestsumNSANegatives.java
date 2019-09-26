import java.util.Arrays;

public class TestsumNSANegatives {

	static int maxTasks = 2;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		 
		// create an object of type ShareInfoMaster
		ShareInfoMaster im = new ShareInfoMaster (2);
		
		// madeup: both lists contain info about 6 items; first list 5 are 0; second list 4 are 0
		int [] madeUp = {4, -5, 0, 0, -2, 8, -2, 1};
		
		int [] sum = im.sumNSANegatives(madeUp);
		
		System.out.println(Arrays.toString(sum));
		
		
		// 

	}

}
