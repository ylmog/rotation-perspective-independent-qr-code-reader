package mypack2;

import java.io.BufferedReader;
import java.io.FileReader;

public class MatrixToString {

	public boolean[][] image;
	QRCodeSymbol qrCodeSymbol;
	int numLastCorrectionFailures = 0;


	public MatrixToString(boolean[][] image) {
		this.image = image;
	}

	public String decode() {
		//long startTime = System.currentTimeMillis();
		qrCodeSymbol = new QRCodeSymbol(image);
		int[] blocks = qrCodeSymbol.getBlocks();
		blocks = correctDataBlocks(blocks);
		if (numLastCorrectionFailures != 0)
			throw new IllegalArgumentException("Correction Error!");
		String decodedString = null;
		try {
			byte[] decodedByteArray = null;
			QRCodeDataBlockReader reader = new QRCodeDataBlockReader(blocks,
					qrCodeSymbol.getVersion(), qrCodeSymbol
							.getNumErrorCollectionCode());
			try {
				decodedByteArray = reader.getDataByte();
			} catch (IllegalArgumentException e) {
				throw e;
			}
			decodedString = new String(decodedByteArray);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		//long elapsedTime = System.currentTimeMillis() - startTime;
		//System.out.println((elapsedTime / 1000.0) + " seconds.");

		return decodedString;
	}

	private int[] correctDataBlocks(int[] blocks) {
		int numSucceededCorrections = 0;
		int numCorrectionFailures = 0;
		int dataCapacity = qrCodeSymbol.getDataCapacity();
		int[] dataBlocks = new int[dataCapacity];
		int numErrorCollectionCode = qrCodeSymbol.getNumErrorCollectionCode();
		int numRSBlocks = qrCodeSymbol.getNumRSBlocks();
		int eccPerRSBlock = numErrorCollectionCode / numRSBlocks;
		if (numRSBlocks == 1) {
			RsDecode corrector = new RsDecode(eccPerRSBlock / 2);
			int ret = corrector.decode(blocks);
			if (ret > 0)
				numSucceededCorrections += ret;
			else if (ret < 0)
				numCorrectionFailures++;
			return blocks;
		} else { // we have to interleave data blocks because symbol has 2 or
			// more RS blocks
			int numLongerRSBlocks = dataCapacity % numRSBlocks;
			if (numLongerRSBlocks == 0) { // symbol has only 1 type of RS block
				int lengthRSBlock = dataCapacity / numRSBlocks;
				int[][] RSBlocks = new int[numRSBlocks][lengthRSBlock];
				// obtain RS blocks
				for (int i = 0; i < numRSBlocks; i++) {
					for (int j = 0; j < lengthRSBlock; j++) {
						RSBlocks[i][j] = blocks[j * numRSBlocks + i];
					}
					RsDecode corrector = new RsDecode(eccPerRSBlock / 2);
					int ret = corrector.decode(RSBlocks[i]);
					if (ret > 0)
						numSucceededCorrections += ret;
					else if (ret < 0)
						numCorrectionFailures++;
				}
				// obtain only data part
				int p = 0;
				for (int i = 0; i < numRSBlocks; i++) {
					for (int j = 0; j < lengthRSBlock - eccPerRSBlock; j++) {
						dataBlocks[p++] = RSBlocks[i][j];
					}
				}
			} else { // symbol has 2 types of RS blocks
				int lengthShorterRSBlock = dataCapacity / numRSBlocks;
				int lengthLongerRSBlock = dataCapacity / numRSBlocks + 1;
				int numShorterRSBlocks = numRSBlocks - numLongerRSBlocks;
				int[][] shorterRSBlocks = new int[numShorterRSBlocks][lengthShorterRSBlock];
				int[][] longerRSBlocks = new int[numLongerRSBlocks][lengthLongerRSBlock];
				for (int i = 0; i < numRSBlocks; i++) {
					if (i < numShorterRSBlocks) { // get shorter RS Block(s)
						int mod = 0;
						for (int j = 0; j < lengthShorterRSBlock; j++) {
							if (j == lengthShorterRSBlock - eccPerRSBlock)
								mod = numLongerRSBlocks;
							shorterRSBlocks[i][j] = blocks[j * numRSBlocks + i
							                               + mod];
						}
						RsDecode corrector = new RsDecode(eccPerRSBlock / 2);
						int ret = corrector.decode(shorterRSBlocks[i]);
						if (ret > 0)
							numSucceededCorrections += ret;
						else if (ret < 0)
							numCorrectionFailures++;

					} else { // get longer RS Blocks
						int mod = 0;
						for (int j = 0; j < lengthLongerRSBlock; j++) {
							if (j == lengthShorterRSBlock - eccPerRSBlock)
								mod = numShorterRSBlocks;
							longerRSBlocks[i - numShorterRSBlocks][j] = blocks[j* numRSBlocks + i - mod];
						}
						RsDecode corrector = new RsDecode(eccPerRSBlock / 2);
						int ret = corrector.decode(longerRSBlocks[i- numShorterRSBlocks]);
						if (ret > 0)
							numSucceededCorrections += ret;
						else if (ret < 0)
							numCorrectionFailures++;
					}
				}
				int p = 0;
				for (int i = 0; i < numRSBlocks; i++) {
					if (i < numShorterRSBlocks) {
						for (int j = 0; j < lengthShorterRSBlock- eccPerRSBlock; j++) {
							dataBlocks[p++] = shorterRSBlocks[i][j];
						}
					} else {
						for (int j = 0; j < lengthLongerRSBlock - eccPerRSBlock; j++) {
							dataBlocks[p++] = longerRSBlocks[i - numShorterRSBlocks][j];
						}
					}
				}
			}
			numLastCorrectionFailures = numCorrectionFailures;
			return dataBlocks;
		}
	}

}
