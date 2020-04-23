package mypack2;

//OKK
public class BCH15_5 {
	int[][] gf16;
	boolean[] receiveData;
	int numCorrectedError;

	//OKK
	public BCH15_5(boolean[] source) {
		gf16 = createGF16();
		receiveData = source;
		//printBit("receive data", receiveData);		
	}
	
	//OKK
	public boolean[] correct() {
		int[] s = calcSyndrome(receiveData);
		
		int[] errorPos = detectErrorBitPosition(s);
		boolean[] output = correctErrorBit(receiveData, errorPos);
		return output;
	}
	
	//OKK
	int[][] createGF16() {
		gf16 = new int[16][4];
		int[] seed = {1, 1, 0, 0};
		for (int i = 0; i < 4; i++)
			gf16[i][i] = 1;
		for (int i = 0; i < 4; i++)
			gf16[4][i] = seed[i];
		for (int i = 5; i < 16; i++) {
			for (int j = 1; j < 4; j++) {
				gf16[i][j] = gf16[i - 1][j - 1];
			}
			if (gf16[i - 1][3] == 1) {
				for (int j = 0; j < 4; j++)
					gf16[i][j] = (gf16[i][j] + seed[j]) % 2;
			}
		}
		return gf16;
	}
	
	//OKK
	int searchElement(int[] x) {
		int k;
		for (k = 0; k < 15; k++) {
			if (   x[0] == gf16[k][0]
					&& x[1] == gf16[k][1]
					&& x[2] == gf16[k][2]
			    && x[3] == gf16[k][3]
				 ) break;
		}
		return k;
	}
	
	//OKK
	int addGF(int arg1, int arg2) {		
		int[] p = new int[4];
		for (int m = 0; m < 4; m++) {
			int w1 = (arg1 < 0 || arg1 >= 15) ? 0 : gf16[arg1][m];
			int w2 = (arg2 < 0 || arg2 >= 15) ? 0 : gf16[arg2][m];
			p[m] = (w1 + w2) % 2;
		}
		return searchElement(p);
	}

	//OKK
	int[] calcSyndrome(boolean[] y) {
		int[] s = new int[5];
		int[] p = new int[4];
		int k;
		for (k = 0; k < 15; k++) {
			if (y[k] == true) for (int m = 0; m < 4; m++) 
				p[m] = (p[m] + gf16[k][m]) % 2;
		}
		k = searchElement(p);
		s[0] = (k >= 15)? -1 : k;


		
		s[1] = (s[0] < 0) ? -1 : (s[0] * 2) % 15;


		
		p = new int[4];
		for (k = 0; k < 15; k++) {
			if (y[k] == true) for (int m = 0; m < 4; m++) 
				p[m] = (p[m] + gf16[(k * 3) % 15][m]) % 2;
		}
			
		k = searchElement(p);

		s[2] = (k >= 15) ? -1 : k;


		
		s[3] = (s[1] < 0) ? -1 : (s[1] * 2) % 15;


		
		p = new int[4];
		for (k = 0; k < 15; k++) {
			if (y[k] == true) for (int m = 0; m < 4; m++) 
				p[m] = (p[m] + gf16[(k * 5) % 15][m]) % 2; 
		}
		k = searchElement(p);
		s[4] = (k >= 15)? -1 : k;

		
		return s;
	}

	//OKK
	int[] calcErrorPositionVariable(int[] s) {
		int[] e = new int[4];

		e[0] = s[0];

		int t = (s[0] + s[1]) % 15;
		int mother = addGF(s[2], t);
		mother = (mother >= 15) ? -1 : mother;
		
		t = (s[2] + s[1]) % 15;
		int child = addGF(s[4], t);
		child = (child >= 15) ? -1 : child;
		e[1] = (child < 0 && mother < 0) ? -1 : (child - mother + 15) % 15;
				
		t = (s[1] + e[0]) % 15;
		int t1 = addGF(s[2], t);
		t = (s[0] + e[1]) % 15;
		e[2] = addGF(t1, t);
		
		return e;
	}
	
	//OKK
	int[] detectErrorBitPosition(int[] s) {
		
		int[] e = calcErrorPositionVariable(s);
		int[] errorPos = new int[4];
		if (e[0] == -1) {
			//System.out.println("No errors.");
			return errorPos;
		}
		else if (e[1] == -1) {
			/*System.out.println("1 error. position is "+ 
					String.valueOf(e[0]) +
					" (" + bitName[e[0]] + ")")*/;
			errorPos[0] = 1;
			errorPos[1] = e[0];
			return errorPos;
		}
		//else {
			//System.out.println("2 or more errors.");			
		//}
		//int numError = 0;
		//int[] p;
		int x3, x2, x1;
		int t, t1, t2, anError;
		//error detection
		for (int i = 0; i < 15; i++) {
			x3 = (i * 3) % 15;
			x2 = (i * 2) % 15;
			x1 = i;
			
			//p = new int[4];
			
			t = (e[0] + x2) % 15;
			t1 = addGF(x3, t);
			
			t = (e[1] + x1) % 15;
			t2 = addGF(t, e[2]);
			
			anError = addGF(t1,t2);
			
			if (anError >= 15) {
				/*System.out.println("Error found. position is " + 
						String.valueOf(i) +
						"(" + bitName[i]+ ")");*/
				errorPos[0]++;
				errorPos[errorPos[0]] = i;
			}
		}
		
		return errorPos;
	}
	
	//OKK
	boolean[] correctErrorBit(boolean[] y, int[] errorPos) {
		for (int i = 1; i <= errorPos[0]; i++)
			y[errorPos[i]] = !y[errorPos[i]];
		
		numCorrectedError = errorPos[0];
		//printBit("Collected data", y);
		return y;
	}

}


