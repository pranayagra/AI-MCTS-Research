import java.util.ArrayList;
import java.util.Arrays;

// class to deal with shared info at the master node

public class ShareInfoMaster {

	int[] nSA0FromNodes = null;
	int[] nSA0ToNodes = null; // to send

	int[] nSA1FromNodes = null;
	int[] nSA1ToNodes = null;

	int[] nSA2FromNodes = null;
	int[] nSA2ToNodes = null;

	double[] rS3FromNodes = null;
	double[] rS3ToNodes = null;

	int[] nS3FromNodes = null;
	int[] nS3ToNodes = null;

	private int maxTasks;

	//UZI ShareInfoMaster constructor
	// since this is the only constructor, maxTasks is always 1 ...
	public ShareInfoMaster() {
		this(1);
	}
	
	// UZI added constructor for ShareInfoMaster
	public ShareInfoMaster (int maxTasks) {
		super();
		this.maxTasks = maxTasks;
	}

	/**
	 * Init the number and reward arrays and create the corresponding arrays
	 * from synchronized info from master node.
	 * 
	 * @param nSA
	 *            Array for N (s, a) the number of visits for each action in
	 *            state s
	 * @param rSA
	 *            Array for R(s, a) the cummulative rewards for each actionin
	 *            state s
	 * @param tasks
	 *            The total number of nodes working on this
	 **/
	public void set(int[] nSA0, int[] nSA1, int[] nSA2, double[] rS3, int[] nS3, int tasks) {
		nSA0FromNodes = nSA0; // contains gathered information
		nSA1FromNodes = nSA1; // contains gathered information (including
								// negative numbers)
		nSA2FromNodes = nSA2; // contains gathered information (including
								// negative numbers)
		rS3FromNodes = rS3; // contains gathered information
		nS3FromNodes = nS3; // contains gathered information
		maxTasks = tasks;

		nSA0ToNodes = new int[nSA0.length / maxTasks];
		nSA1ToNodes = new int[nSA1.length / maxTasks];
		nSA2ToNodes = new int[nSA2.length / maxTasks];
		rS3ToNodes = new double[rS3.length / maxTasks];
		nS3ToNodes = new int[nS3.length / maxTasks];
	}
	
	/**
	 *   Scales the learned info by 1/maxTasks 
	 *
	 */
	public void scale () {
		for (int x = 0;  x< nSA0ToNodes.length; x++)
			nSA0ToNodes[x] = nSA0ToNodes[x]/maxTasks;
		
		for (int x = 0;  x< nSA1ToNodes.length; x++)
			nSA1ToNodes[x] = nSA1ToNodes[x]/maxTasks;
		
		for (int x = 0;  x< nSA2ToNodes.length; x++)
			nSA2ToNodes[x] = nSA2ToNodes[x]/maxTasks;
		
		for (int x = 0;  x< rS3ToNodes.length; x++)
			rS3ToNodes[x] = rS3ToNodes[x]/maxTasks;
		
		for (int x = 0;  x< nS3ToNodes.length; x++)
			nS3ToNodes[x] = nS3ToNodes[x]/maxTasks;
	}


	/**
	 * The master node processes the info shared from worker nodes
	 **/
	public void processInfo() {
		// summarize the N (s,a) info
		// System.out.println("RAN 0BE: " +
		// Arrays.toString(node.nSA0FromMaster));
		sumNSA0();
		nSA1ToNodes = sumNSANegatives(nSA1FromNodes); // give it this array, and
														// it will return the
														// nSA1ToNodes array
		//UZI added sysout
		//System.out.println(" using this info nSA1FromNodes  = " + Arrays.toString(nSA1FromNodes));
		//System.out.println(" first time after sumNSANegatives nSA1ToNodes = " + Arrays.toString(nSA1ToNodes));
		nSA2ToNodes = sumNSANegatives(nSA2FromNodes);
		sumRS3();
		sumNS3();
		
	}

	/**
	 * Sum the N(s,a) values in nSAFromNodes into nSAToNodes The info is
	 * maxTasks many sets of info - one per process node
	 **/
	public void sumNSA0() {
		int len = nSA0ToNodes.length;
		for (int i = 0; i < nSA0FromNodes.length; i++)
			nSA0ToNodes[i % len] += nSA0FromNodes[i];
	}

	/**
	 * Sum the N(s,a) values that contain negative numbers, as those values are
	 * really zeros that are suppressed
	 **/
	public int[] sumNSANegatives(int[] type) {
		int[] array1 = type;

		int numElem = 0; // number of TRUE elements in half the length,
							// uncompressing the negative values

		// UZI : incorrect logic - at least one of the process nodes had data of
		// length array1.length/maxTasks
		// otherwise the max would have been smaller.....
		// UZI : replaced loop below with the current statement
		// numElem = array1.length/maxTasks;

		// for(int i=0; i<array1.length/maxTasks; i++) {
		// if(array1[i] == 0)
		// break;
		// if(array1[i]<0)
		// numElem += Math.abs(array1[i]);
		// else {
		// numElem+=1;
		// }
		// }

		// UZI the array to be returned might be longer ... since it does not
		// contain
		// any 'compressed' negative value
		// int[] sumArr = new int[numElem]; //array to return
		ArrayList<Integer> sumArrLst = new ArrayList<Integer>();
		int[] counters = new int[maxTasks];

		int indicesPerTask = array1.length / maxTasks; // actual index size
		counters[0] = 0;

		for (int i = 1; i < maxTasks; i++)
			counters[i] = counters[i - 1] + indicesPerTask; // set up counters
															// (based on
															// indices)

		// UZI - since array may be longer we cannot stop at numElem
		boolean stop = false;
		//int i = 0;
		while (!stop) {
			// for(int i = 0; i<numElem; i++) { //for every elem in the gathered
			// array
			int currSum = 0; // sum of the current indice we are on
			for (int j = 0; j < counters.length; j++) { // how many counters =
														// maxTasks to check
					if (array1[counters[j]] >= 0) { // if the array value at the
													// counter is >=0, that
													// means that we can add
													// that value to the current
													// counter and increase the
													// counter by 1
						currSum += array1[counters[j]]; // increase counter
						counters[j] += 1; // add one to counter j
					} else { // otherwise, if it is a suppressed zero chain
								// (negative number)
						array1[counters[j]] += 1; // add one to the negative
													// value
						if (array1[counters[j]] == 0) { // if the value is zero
														// now, skip it (as -1
														// and 0 are the same
														// meaning)
							counters[j] += 1; // add one to counter j
						}
					}
					//  UZI added 
					//  if all elements from process j have been processed
					//  indicate that
					if (counters[j] == (j + 1) * indicesPerTask)
						stop=true;

			}
			// UZI - changed due to use of arraylist
			// sumArr[i] = currSum; //set that value to sumArr
			sumArrLst.add(currSum);
			//i++;
		}
		
		// UZI to addjust for arraylist
		// from https://stackoverflow.com/questions/718554/how-to-convert-an-arraylist-containing-integers-to-primitive-int-array
		int[] sumArr = new int[sumArrLst.size()];
		for(int ec = 0; ec<sumArr.length; ec++) {
			sumArr[ec] = sumArrLst.get(ec);
		}
	//	int[] sumArr = sumArrLst.stream().mapToInt(uzi -> uzi).toArray();
		//Integer [] sumArr = new Integer [sumArrLst.size()];
		//sumArrLst.toArray(sumArr);
		return sumArr; // return the combined array... However, this array does
						// contain zeros (not the extra zeros though)
	}

	/**
	 * Sum the R(s,a) values in rS3FromNodes into rS3ToNodes The info is
	 * maxTasks many sets of info - one per process node
	 **/
	public void sumRS3() {
		int len = rS3ToNodes.length;
		for (int i = 0; i < rS3FromNodes.length; i++)
			rS3ToNodes[i % len] += rS3FromNodes[i];
	}

	/**
	 * Sum the N(s,a) values in nS3FromNodes into nS3ToNodes The info is
	 * maxTasks many sets of info - one per process node
	 **/
	public void sumNS3() {
		int len = nS3ToNodes.length;
		for (int i = 0; i < nS3FromNodes.length; i++)
			nS3ToNodes[i % len] += nS3FromNodes[i];
	}

	/**
	 * Display the length and then the content of the four data arrays; the
	 * order is nSAFromNodes, rSAFromNodes, nSAToNodes, rSAToNodes.
	 **/
	public String toString() {
		String res = "lengthInfo: " + nSA0FromNodes.length + " " + rS3FromNodes.length + " ";
		if (nSA0ToNodes != null)
			res += nSA0ToNodes.length + " ";
		else
			res += "0 ";

		if (rS3ToNodes != null)
			res += rS3ToNodes.length + " ";
		else
			res += "0 ";

		String infoRes = "";
		for (int val : nSA0FromNodes)
			infoRes += val + " ";
		infoRes += "\n";

		for (double val : rS3FromNodes)
			infoRes += val + " ";
		infoRes += "\n";

		if (nSA0ToNodes == null)
			infoRes += " empty list";
		else
			for (int val : nSA0ToNodes)
				infoRes += val + " ";
		infoRes += "\n";

		if (rS3ToNodes == null)
			infoRes += " empty list ";
		else
			for (double val : rS3ToNodes)
				infoRes += val + " ";
		infoRes += "\n";

		return res + "\n" + infoRes;
	}

}
