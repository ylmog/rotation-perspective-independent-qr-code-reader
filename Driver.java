package mypack2;

import java.awt.Image;

import mypack2.ProcessingClass;


public class Driver {

	public static void main(String[] args) {

		if(args.length != 1) {
			System.out.println("Argument needed as an input image file!");
			System.exit(0);
		}
		
		ProcessingClass instance = new ProcessingClass();
		
		// 1 Open Image
		
		//Image acilan=instance.openImage("file:///home/oguzyil/eclipse-workspace/example.png");
		Image acilan=instance.openImage("file:///home/oguzyil/eclipse-workspace/"+args[0]);
	   
		for(int tt=0; tt<15; tt++) {
			
			long  startTime = System.currentTimeMillis();
	
			String result = instance.decodeBin(acilan);
	
			long elapsedTime = System.currentTimeMillis() - startTime;
			System.out.println("Time Spent: " + elapsedTime + " milliseconds.");
			
			System.out.println("Decoded: " + result);
		
		}
		
	}

}
