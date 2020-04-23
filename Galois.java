package mypack2;

/**
 *
 * @author Masayuki Miyazaki
 * http://sourceforge.jp/projects/reedsolomon/
 */
//OKK
public final class Galois {
	public static final int POLYNOMIAL = 0x1d;
	private static final Galois instance = new Galois();
	private int[] expTbl = new int[255 * 2];	
	private int[] logTbl = new int[255 + 1];

	//OKK
	private Galois() {
		initGaloisTable();
	}

	public static Galois getInstance() {
		return instance;
	}

	//OKK
	private void initGaloisTable() {
		int d = 1;
		for(int i = 0; i < 255; i++) {
			expTbl[i] = expTbl[255 + i] = d;
			logTbl[d] = i;
			d <<= 1;
			if((d & 0x100) != 0) {
				d = (d ^ POLYNOMIAL) & 0xff;
			}
		}
	}

	//OKK
	public int toExp(int a) {
		return expTbl[a];
	}


	//OKK
	public int toLog(int a) {
		return logTbl[a];
	}


	//OKK
	public int toPos(int length, int a) {
		return length - 1 - logTbl[a];
	}


	//OKK
	public int mul(int a, int b)	{
		return (a == 0 || b == 0)? 0 : expTbl[logTbl[a] + logTbl[b]];
	}


	//OKK
	public int mulExp(int a, int b)	{
		return (a == 0)? 0 : expTbl[logTbl[a] + b];
	}

	//OKK
	public int div(int a, int b) {
		return (a == 0)? 0 : expTbl[logTbl[a] - logTbl[b] + 255];
	}

	//OKK
	public int divExp(int a, int b) {
		return (a == 0)? 0 : expTbl[logTbl[a] - b + 255];
	}

	//OKK
	public void mulPoly(int[] seki, int[] a, int[] b) {
		int ia=0;
		for(ia=0;ia<seki.length;ia++){
			seki[ia]=0;
		}
		for( ia= 0; ia < a.length; ia++) {
			if(a[ia] != 0) {
				int loga = logTbl[a[ia]];
				int ib2 = Math.min(b.length, seki.length - ia);
				for(int ib = 0; ib < ib2; ib++) {
					if(b[ib] != 0) {
						seki[ia + ib] ^= expTbl[loga + logTbl[b[ib]]];	// = a[ia] * b[ib]
					}
				}
			}
		}
	}

	//OKK
	public boolean calcSyndrome(int[] data, int length, int[] syn) {
		int hasErr = 0;
		for(int i = 0; i < syn.length;  i++) {
			int wk = 0;
			for(int idx = 0; idx < length; idx++) {
				 wk = data[idx] ^ ((wk == 0)? 0 : expTbl[logTbl[wk] + i]);		// wk = data + wk * α^i
			}
			syn[i] = wk;
			hasErr |= wk;
		}
		return hasErr == 0;
	}
}
