import java.util.Arrays;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

// class to compute info to share and to update nodes based 
// on summary data shared by master


public class ShareInfoNode {

	 int [] nSA0ToMaster = null;
	 int [] nSA1ToMaster = null;
	 int [] nSA2ToMaster = null;
	 double [] rS3ToMaster = null;
	 int [] nS3ToMaster = null;
	 
	 int[] nSA1andnSA2Lengths = new int[2];

	 int [] nSA0FromMaster=null;
	 int [] nSA1FromMaster=null;
	 int [] nSA2FromMaster=null;
	 double [] rS3FromMaster=null;
	 int [] nS3FromMaster=null;
	 
	 double[] qS3Compute=null;
	 double[] qS2Compute=null;
	 double[] qS1Compute=null;
	 
	 int qS2sz = 0;
	 int qS1sz = 0;
	 
	 public void setNLength() {
		 nSA1andnSA2Lengths[0] = nSA1FromMaster.length;
		 nSA1andnSA2Lengths[1] = nSA2FromMaster.length;
	 }
	 
	 public void updateNlengths() {
		 nSA1FromMaster = new int[nSA1andnSA2Lengths[0]];
		 nSA2FromMaster = new int[nSA1andnSA2Lengths[1]];
	 }
	 
	public void set (int [] nSA0, int[] nSA1, int[] nSA2, double [] rS3, int[] nS3) {
		nSA0ToMaster = nSA0;
		nSA1ToMaster = nSA1;
		nSA2ToMaster = nSA2;
		rS3ToMaster = rS3;
		nS3ToMaster = nS3;
		
		nSA0FromMaster = new int [nSA0.length];
		nSA1FromMaster = new int [nSA1.length]; 
		nSA2FromMaster = new int [nSA2.length];
		rS3FromMaster = new double [rS3.length];	
		nS3FromMaster = new int [nS3.length];
	}
	
	
	//Use after combining information to then broadcast
	public void setFromMasterCombine(int [] nSA0, int[] nSA1, int[] nSA2, double [] rS3, int[] nS3) {
		nSA0FromMaster = nSA0;
		nSA1FromMaster = nSA1;
		nSA2FromMaster = nSA2;
		rS3FromMaster = rS3;
		nS3FromMaster = nS3;
		
		// UZI added 
	//	System.out.println(" and again .. " + Arrays.toString(nSA1));
		
		nSA0ToMaster = new int [nSA0.length];
		nSA1ToMaster = new int [nSA1.length]; 
		nSA2ToMaster = new int [nSA2.length];
		rS3ToMaster = new double [rS3.length];	
		nS3ToMaster = new int [nS3.length];
	}

	//NOT UPDATED TO UPDATE 4 ITEMS
	public void postProcessInfo(MCNode root) {
		root.setTimesActionChosen(nSA0FromMaster);
		root.setRewards(rS3FromMaster);
	}

	
	/**
	 * Compute the value of Q(s) level 3 using R(s)/N(s) (remember, R(s) can be from -N(s) to N(s) inclusive), so Q(s) can = [-1, 1]
	 * **/
	public void computeQ3() {
		qS3Compute = new double[nS3FromMaster.length];
		for(int i = 0; i<nS3FromMaster.length; i++) {
			if(nS3FromMaster[i] == 0) {
				qS3Compute[i] = 0;
			}
			else {
				qS3Compute[i] = rS3FromMaster[i]/nS3FromMaster[i];
			}
		}
	}
	
	
	//NOT UPDATED TO PRINT 4 ITEMS
	public String toString () {
	   String res = "lengthInfo: " + nSA0ToMaster.length + " " + rS3ToMaster.length+ " ";
	   if (nSA0FromMaster != null)
		res += nSA0FromMaster.length + " ";
	   else
		res += "0 ";

	   if (rS3FromMaster != null)
		res += rS3FromMaster.length + " ";
	   else
		res += "0 ";

	   String infoRes = "";
	   for (int val: nSA0ToMaster)
	       infoRes += val + " ";
           infoRes+= "\n";


	   for (double val: rS3ToMaster)
	       infoRes += val + " ";
           infoRes+= "\n";

	
           if (nSA0FromMaster == null)
	       infoRes += " empty list ";
	   else 
	     for (int val: nSA0FromMaster)
	       infoRes += val + " ";
           infoRes+= "\n";


           if (rS3FromMaster == null)
	       infoRes += " empty list ";
	   else 
	     for (double val: rS3FromMaster)
	       infoRes += val + " ";
           infoRes+= "\n";

           return res + "\n" + infoRes;
	}
}
