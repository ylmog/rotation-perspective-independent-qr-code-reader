package mypack2;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

import javax.imageio.ImageIO;

public class ProcessingClass {
//	static boolean showDetail=true;//true false
	boolean useTwoUncor=true; //true false
	private static final int reduc=1;

	private double scaRound(double x){
		if (x < 0){
		    return Math.ceil(x - 0.5);
		} else {
			return Math.floor(x + 0.5);
		}
	}
	
	//private void ImageWindow(String windowTitle, int[][] arr, boolean whatIsthis) {
	private void ImageWindow(String fileName, int[][] arr) {
	
		int xLenght = arr.length;
		int yLength = arr[0].length;
		BufferedImage b = new BufferedImage(xLenght, yLength, 3);

		for(int x = 0; x < xLenght; x++) {
		    for(int y = 0; y < yLength; y++) {
		        int rgb = (int)arr[x][y] | (int)arr[x][y]  | (int)arr[x][y];
		        b.setRGB(x, y, rgb);
		    }
		}
		
		try {

			ImageIO.write(b, "Doublearray", new File(fileName));

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String decodeBin(Image acilan){ // String path, String folder
	     

        //startTime = System.currentTimeMillis();
     //   if(showDetail) this.ImageWindow(folder+"1gray.jpg",ima);
        
		
		// 2 Create 2Dim matrix and GrayScale conversion
		int[][] ima = createImageMatrix(acilan);
		
		
        //try {	ima=imresize(ima,0.25);
		//} catch (NoninvertibleTransformException e) {e.printStackTrace();}
        if(reduc!=1)
        	ima=myimresize(ima,reduc);
        //if(showDetail){new ImageWindow("2 - Resized Image",ima,false);}
        //adaphisteq(ima,20);
        //histeq(ima,0,ima.length,0,ima[0].length);
        //if(showDetail){
        	//new ImageWindow("3 - Adaptive Light Balanced",ima,true);
        //}

               
        thresholdOtsu(ima);

        //if(showDetail){new ImageWindow("3 - Binarized Image",ima,true);}
        double[][] coor=new double[3][4];
        int distance=finderPattern(ima,coor);
        double cell=cellSize(coor);
        //if(showDetail)System.out.println("Cell size : " + cell);
        //if(showDetail)System.out.println("Distance : " + distance);

        ima = imTwoDTransform(ima,coor,cell,distance);
        //if(showDetail){new ImageWindow("4 - Transformed Image",ima,true);}
        
        useMyfilter(ima,coor,4,cell);
        //if(showDetail){new ImageWindow("4.5 - Filtered Transformed Image",ima,true);}
        
        double[][] corners = findCorners(ima,coor,(int)scaRound(cell),4);
        //if(showDetail)dispCorners(corners);
        
        ima=imPerspecTransform5(ima,corners,coor);
        if(useTwoUncor){
        	//ima=imPerspecTransform2(ima,corners,coor);
        } else {
        	//ima=imPerspecTransform3(ima,corners,coor);
        	double dmm=0;
        	for(int i=0;i<4;i++){
        		dmm=coor[1][i];
        		coor[1][i]=coor[2][i];
        		coor[2][i]=dmm;
        	}
        }
        //if(showDetail){new ImageWindow("5 - Perspective Transformed Image",ima,true);}
        useMyfilter(ima,coor,3,cell);
        //if(showDetail){new ImageWindow("5.5 - Filtered Perspective Transformed Image",ima,true);}

        corners = findCorners(ima,coor,(int)scaRound(cell),3);
        int[][] bounds=new int[2][2];
        bounds[0][0]=(int)(corners[0][0]);
        bounds[0][1]=(int)(corners[0][1]);
        bounds[1][0]=(int)(corners[1][0]);
        bounds[1][1]=(int)(corners[2][1]);
        //if(showDetail){new ImageWindow("6 - Croppped Image",ima,bounds);}
        
        int bits=numberofCells2(ima,coor,cell);
        //TODO burda reverse yerine direk toBinary icinde reverse et. Bir daha reverse olusturma!.
        boolean[][] image=toBinary2(ima,bounds,bits);//0 true, 1 false
        //if(showDetail){new ImageWindow("7 - Binarized Image",image);}
        checkCorrectness(image);
        if(useTwoUncor){
        	image=reverse(image);
        }      
        MatrixToString inst = new MatrixToString(image);
        String rety=inst.decode();
        //rety = ContentConverter.convert(rety);//TODO bun aktiflestirebilirm.
				
		return rety;
	}
	
	
	private void useMyfilter(int[][] image,double[][] coor,int kackose,double cell){
		int row=image.length;
		int col=image[0].length;
		if(kackose==3){
			myfilter(image,1,(int)(coor[0][1]+1.5*cell),1,(int)(coor[0][0]+1.5*cell));
			myfilter(image,1,(int)(coor[1][1]+1.5*cell),(int)(coor[1][0]-1.5*cell),col-2);
			myfilter(image,(int)(coor[2][1]-1.5*cell),row-2,1,(int)(coor[2][0]+1.5*cell));
		} else if(kackose==4){
			myfilter(image,1,(int)coor[0][1]+1,1,(int)coor[0][0]+1);
			myfilter(image,1,(int)coor[2][1],(int)coor[1][0]-1,col-2);
			myfilter(image,(int)coor[2][1]-1,row-2,1,col-2);
		}
	}
	private void myfilter(int[][] image,int y,int yb,int x,int xb){
		int q,w,say;
		for(q=y;q<=yb;q++){
			for(w=x;w<=xb;w++){
				say=image[q+1][w]+image[q][w+1]+image[q-1][w]+image[q][w-1];
				if(image[q][w]==1 && say<=1){
					image[q][w]=0;
				} else if(image[q][w]==0 && say>=3){
					image[q][w]=1;
				}
			}
		}
		for(q=yb;q>=y;q--){
			for(w=xb;w>=x;w--){
				say=image[q+1][w]+image[q][w+1]+image[q-1][w]+image[q][w-1];
				if(image[q][w]==1 && say<=1){
					image[q][w]=0;
				} else if(image[q][w]==0 && say>=3){
					image[q][w]=1;
				}
			}
		}
	}
	/*private void dispBinIm(boolean[][] image){
		for(int t=0;t<image.length;t++){
			for(int e=0;e<image.length;e++){
				if(image[t][e]){
					System.out.print("0 ");
				} else {				
					System.out.print("1 ");
				}
			}
			System.out.println();
		}
	}*/
	/*private boolean[][] toBool(int[][] image){
		int len=image.length;
		boolean[][] retima=new boolean[len][len];
		for(int t=0;t<len;t++){
	    	for(int e=0;e<len;e++){
	    		retima[t][e]=image[t][e]==1;
			}
		}
		return retima;
	}*/
	private boolean[][] reverse(boolean[][] image){
		int len=image.length;
		boolean[][] retima=new boolean[len][len];
		int e;
		for(int t=0;t<len;t++){
	    	for(e=0;e<len;e++){
	    		retima[e][t]=image[t][e];
			}
		}
		return retima;
	}
	/*private boolean[][] toBinary(int[][] image,int[][] bounds,int bits){
		//nearest neigbor
		int row=bounds[1][1]-bounds[0][1]+1;
		int col=bounds[1][0]-bounds[0][0]+1;
		boolean[][] retima=new boolean[bits][bits];
		double oranx=(double)col/bits;
		double orany=(double)row/bits;
		for(row=0;row<bits;row++){
	    	for(col=0;col<bits;col++){
	    		retima[row][col]=image[bounds[0][1]+(int)scaRound(orany*((double)row+0.5))]
	    		                   [bounds[0][0]+(int)scaRound(oranx*((double)col+0.5))]==0;
			}
		}
		return retima;
	}*/
	private boolean[][] toBinary2(int[][] image,int[][] bounds,int bits){
		//nearest neigbor
		int row=bounds[1][1]-bounds[0][1]+1;
		int col=bounds[1][0]-bounds[0][0]+1;
		boolean[][] retima=new boolean[bits][bits];
		double oranx=(double)col/bits;
		double orany=(double)row/bits;
		//if(showDetail) System.out.println("oranx "+ oranx+" orany : "+orany);
		int tb,et;
		//int tbb,etb;
		boolean decision=false;
		int say=0;
		for(row=0;row<bits;row++){
	    	for(col=0;col<bits;col++){
	    		tb=bounds[0][1]+(int)scaRound(orany*((double)row)+(orany-1)*(0.5));
	    		et=bounds[0][0]+(int)scaRound(oranx*((double)col)+(oranx-1)*(0.5));
	    		//if((t==3 && e==8) || (t==3 && e==11) || (t==20 && e==12))
	    		//if( showDetail)if((row>=13 && col>=8) && (row<=20 && col<=20))System.out.println("Now at point - x : "+et+" y : "+tb+" for x : "+col+" y : "+row);
	    		say=image[tb][et-1]+image[tb-1][et]+image[tb+1][et]+image[tb][et+1];
	    		if(say>=3 && image[tb][et]==1){
	    			decision=false;
	    		} else if(say<=1 && image[tb][et]==0){
	    			decision=true;
	    		} else {
	    			//bura pek bi i�e yaram�yor.
		    		if(sum(image,tb-1,tb+1,et-1,et+1)>4){
	    				decision=false;
		    		} else {
	    				decision=true;
		    		}
	    			/*tb=bounds[0][1]+(int)scaRound(orany*((double)t+0.5));
		    		et=bounds[0][0]+(int)scaRound(oranx*((double)e+0.5));
		    		tbb=bounds[0][1]+(int)scaRound(orany*((double)t+1))-1;
		    		etb=bounds[0][0]+(int)scaRound(oranx*((double)e+1))-1;
		    		say=sum(image,tb,tbb,et,etb);
		    		tb=(tbb-tb)*(etb-et)/2;//ozellikle double casti yok
	    			if(say>tb){
	    				decision=false;
	    			} else {
	    				decision=true;
	    			}*/
	    			
	    			//if(showDetail)System.out.println("Decide : "+sum(image,tb-1,tb+1,et-1,et+1));
	    		}
	    		retima[row][col]=decision;
			}
		}
		return retima;
	}
	private void checkCorrectness(boolean[][] image){
		int len=image.length;
		int say=0,q;
		for(q=2;q<=5;q++){
			if(checkline(image,q,0)){
				say++;
			}
			if(checkline(image,q,len-7)){
				say++;
			}
		}
		for(q=len-5;q<=len-3;q++){
			if(checkline(image,q,0)){
				say++;
			}
		}
		if(say<4){
			throw new IllegalArgumentException("DIM MISMATCH!");
		}
	}
	private boolean checkline(boolean[][] image,int row,int x){
		return image[row][x]&&image[row][x+2]&&image[row][x+3]&&image[row][x+4]&&image[row][x+6]&&!image[row][x+1]&&!image[row][x+5];
	}

	private int numberofCells2(int[][] image,double[][] coord,double cell) {
		int[][] coorint=new int[3][2];
		int h,i;
		for(h=0;h<3;h++){
			coorint[h][0]=(int)scaRound(coord[h][0]);			
			coorint[h][1]=(int)scaRound(coord[h][1]);
		}
		//double hata = 2;
		double lower=(0.5)*cell;

		int inc=0;
		int xpos=coorint[0][0];
		int ypos=coorint[0][1];
		
		int[] ara=new int[(int)(5*cell)];
		int aralen=ara.length;
		for(h=xpos;h<xpos+aralen;h++){
			ara[h-xpos]=image[ypos][h];
		}
		int lastpos=0;
		for(h=1;h<=lower;h++){
			lastpos=0;
			for(i=0;i<aralen-1;i++){
				if(ara[i]!=ara[i+1]){
					if(i-lastpos<h+1){
						for(inc=lastpos;inc<=i;inc++){
							ara[inc]=ara[i+1];
						}
					}
					lastpos=i;
				}
			}
		}
		inc=0;
		for(h=0;h<aralen-1;h++){
			if(ara[h]!=ara[h+1]){
				inc++;
				if(inc==2){
					xpos=h+1+2*xpos;
				} else if(inc==3){
					xpos=(int)scaRound((double)(h+xpos)/2);
					break;
				}
			}
		}		
		//if(showDetail)System.out.println("Cell counting at x : "+xpos +" ypos : "+ypos);

		
		ara=new int[coorint[2][1]-ypos+1];
		aralen=ara.length;
		for(h=ypos;h<=coorint[2][1];h++){
			ara[h-ypos]=image[h][xpos];
		}
		
		lastpos=0;
		for(h=1;h<=lower;h++){
			lastpos=0;
			for(i=0;i<aralen-1;i++){
				if(ara[i]!=ara[i+1]){
					if(i-lastpos<h+1){
						for(inc=lastpos;inc<=i;inc++){
							ara[inc]=ara[i+1];
						}
					}
					lastpos=i;
				}
			}
		}
		inc=0;
		for(h=0;h<aralen-1;h++){
			if(ara[h]!=ara[h+1]){
				inc++;
			}
		}
		int num=inc+13;
		//if(showDetail)System.out.println("Number of cell1 : "+num);
		//% % Double Check % % % % % % % % % % % % % % % % % % % % % % % % % % % % % 
		inc=0;
		xpos=coorint[0][0];
		ypos=coorint[0][1];
		
		ara=new int[(int)(5*cell)];
		aralen=ara.length;
		for(h=ypos;h<ypos+aralen;h++){
			ara[h-ypos]=image[h][xpos];
		}
		lastpos=0;
		for(h=1;h<=lower;h++){
			lastpos=0;
			for(i=0;i<aralen-1;i++){
				if(ara[i]!=ara[i+1]){
					if(i-lastpos<h+1){
						for(inc=lastpos;inc<=i;inc++){
							ara[inc]=ara[i+1];
						}
					}
					lastpos=i;
				}
			}
		}
		inc=0;
		for(h=0;h<aralen-1;h++){
			if(ara[h]!=ara[h+1]){
				inc++;
				if(inc==2){
					ypos=h+1+2*ypos;
				} else if(inc==3){
					ypos=(int)scaRound((int)(h+ypos)/2);
					break;
				}
			}
		}		
		//if(showDetail)System.out.println("Cell counting at x : "+xpos +" ypos : "+ypos);

		ara=new int[coorint[1][0]-xpos+1];
		aralen=ara.length;
		for(i=xpos;i<=coorint[1][0];i++){
			ara[i-xpos]=image[ypos][i];
		}
		for(h=1;h<=lower;h++){
			lastpos=1;
			for(i=0;i<aralen-1;i++){
				if(ara[i]!=ara[i+1]){
					if(i-lastpos<h+1){
						for(inc=lastpos;inc<=i;inc++){
							ara[inc]=ara[i+1];
						}
					}
					lastpos=i;
				}
			}
		}
		inc=0;
		for(h=0;h<aralen-1;h++){
			if(ara[h]!=ara[h+1]){
				inc++;
			}
		}
		inc+=13;
		//if(showDetail)System.out.println("Number of cell1 : "+inc);
		if(inc!=num){
			throw new IllegalArgumentException("Cannot find Dimension!");
		}
		return num;
	}
	
	/*private int numberofCells1(int[][] image,double[][] coord,double cell) {
		int row=image.length;
		int col = image[0].length;
		int[][] coorint=new int[3][2];
		int h,i;
		for(h=0;h<3;h++){
			coorint[h][0]=(int)scaRound(coord[h][0]);			
			coorint[h][1]=(int)scaRound(coord[h][1]);
		}
		//double hata = 2;
		double lower=(0.5)*cell;

		int inc=0;
		int xpos=coorint[0][0];
		int ypos=coorint[0][1];
		
		for(h=xpos;h<col-1;h++){
			if(image[ypos][h]!=image[ypos][h+1]){
				inc++;
				if(inc==2){
					xpos=h+1;
				} else if(inc==3){
					xpos=(int)scaRound((double)(h+xpos)/2);
					break;
				}
			}
		}
		//if(showDetail)System.out.println("Cell counting at x : "+xpos +" ypos : "+ypos);
		int[] ara=new int[coorint[2][1]-ypos+1];
		int aralen=ara.length;
		for(h=ypos;h<=coorint[2][1];h++){
			ara[h-ypos]=image[h][xpos];
		}
		
		int lastpos=0;
		for(h=1;h<=lower;h++){
			lastpos=0;
			for(i=0;i<aralen-1;i++){
				if(ara[i]!=ara[i+1]){
					if(i-lastpos<h+1){
						for(inc=lastpos;inc<=i;inc++){
							ara[inc]=ara[i+1];
						}
					}
					lastpos=i;
				}
			}
		}
		inc=0;
		for(h=0;h<aralen-1;h++){
			if(ara[h]!=ara[h+1]){
				inc++;
			}
		}
		int num=inc+13;
		//if(showDetail)System.out.println("Number of cell1 : "+num);
		//% % Double Check % % % % % % % % % % % % % % % % % % % % % % % % % % % % % 
		inc=0;
		xpos=coorint[0][0];
		ypos=coorint[0][1];
		for(h=ypos;h<row-1;h++){
			if(image[h][xpos]!=image[h+1][xpos]){
				inc++;
				if(inc==2){
					ypos=h+1;
				} else if(inc==3){
					ypos=(int)scaRound((int)(h+ypos)/2);
					break;
				}
			}
		}		
		//if(showDetail)System.out.println("Cell counting at x : "+xpos +" ypos : "+ypos);

		ara=new int[coorint[1][0]-xpos+1];
		aralen=ara.length;
		for(i=xpos;i<=coorint[1][0];i++){
			ara[i-xpos]=image[ypos][i];
		}
		for(h=1;h<=lower;h++){
			lastpos=1;
			for(i=0;i<aralen-1;i++){
				if(ara[i]!=ara[i+1]){
					if(i-lastpos<h+1){
						for(inc=lastpos;inc<=i;inc++){
							ara[inc]=ara[i+1];
						}
					}
					lastpos=i;
				}
			}
		}
		inc=0;
		for(h=0;h<aralen-1;h++){
			if(ara[h]!=ara[h+1]){
				inc++;
			}
		}
		inc+=13;
		//if(showDetail)System.out.println("Number of cell1 : "+inc);
		if(inc!=num){
			throw new IllegalArgumentException("Cannot find Dimension!");
		}
		return num;
	}*/
	
	/*private void dispCorners(double[][] corners){
		for(int i=0;i<corners.length;i++){
			System.out.println("x : " + corners[i][0] +" y : "+ corners[i][1]);
		}
	}*/
	private double[][] findCorners(int[][] image,double[][] coor,int cell,int kackose){
		int row=image.length;
		int col=image[0].length;
		double[][] corners=new double[4][2];
		double[][] alignment=new double[4][2];
		int[][] coorint=new int[3][2];
		int h;//forlar icin
		for(h=0;h<3;h++){
			coorint[h][0]=(int)scaRound(coor[h][0]);			
			coorint[h][1]=(int)scaRound(coor[h][1]);
		}
		int inc=0;
		int xpos=coorint[0][0];
		int ypos=coorint[0][1];
		for(h=ypos;h>=1;h--){
			if(image[h][xpos]!=image[h-1][xpos]){
				inc++;
				if(inc==2){
					ypos=h-1;
					break;
				}
			}
		}
		while(true){
			if(image[ypos][xpos-1]==1){
				if(image[ypos-1][xpos+1]==1){
					break;
				} else {
					ypos--;
					xpos++;
				}
			} else {
				xpos--;
			}
		}
		corners[0][0]=xpos;			
		corners[0][1]=ypos;
		//if(showDetail)System.out.println("xpos : "+xpos +" ypos : "+ypos);
		//if(showDetail)System.out.println("start : "+(ypos-1) +" stop : "+(ypos-2*cell));
		if(kackose==3){
			for(h=ypos-1;h>=ypos-2*cell;h--){
				if(sum(image,h,h,xpos,xpos+4*cell)==4*cell+1){
					corners[0][1]=(double)(ypos+h+1)/2;
					break;
				}
			}
			for(h=xpos-1;h>=xpos-2*cell;h--){
				if(sum(image,ypos,ypos+4*cell,h,h)==4*cell+1){
					corners[0][0]=(double)(xpos+h+1)/2;
					break;
				}
			}
		}
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		inc=0;
		xpos=coorint[1][0];
		ypos=coorint[1][1];
		for(h=xpos;h<col-1;h++){
			if(image[ypos][h]!=image[ypos][h+1]){
				inc++;
				if(inc==2){
					xpos=h+1;
					break;
				}
			}
		}
		int xposcon=xpos;
		int yposcon=ypos;
		while(true){
			if(image[ypos-1][xpos]==1){
				if(image[ypos+1][xpos+1]==1){
					break;
				} else {
					ypos++;
					xpos++;
				}
			} else {
				ypos--;
			}
		}
		corners[1][0]=xpos;
		corners[1][1]=ypos;
		if(kackose==4){
			xpos=xposcon;
			ypos=yposcon;
			while(true){
				if(image[ypos+1][xpos]==1){
					if(image[ypos-1][xpos+1]==1){
						break;
					} else {
						ypos--;
						xpos++;
					}
				} else {
					ypos++;
				}
			}
			alignment[1][0]=xpos;
			alignment[1][1]=ypos;
		} else if(kackose==3){
			for(h=ypos-1;h>=ypos-2*cell;h--){
				if(sum(image,h,h,xpos-4*cell,xpos)==4*cell+1){
					corners[1][1]=(double)(h+1+ypos)/2;
					break;
				}
			}
			for(h=xpos+1;h<=xpos+2*cell;h++){
				if(sum(image,ypos,ypos+4*cell,h,h)==4*cell+1){
					corners[1][0]=(double)(h-1+xpos)/2;
					break;
				}
			}
		}
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		inc=0;
		xpos=coorint[2][0];
		ypos=coorint[2][1];
		for(h=ypos;h<row-1;h++){
			if(image[h][xpos]!=image[h+1][xpos]){
				inc++;
				if(inc==2){
					ypos=h+1;
					break;
				}
			}
		}
		xposcon=xpos;
		yposcon=ypos;
		while(true){
			if(image[ypos][xpos-1]==1){
				if(image[ypos+1][xpos+1]==1){
					break;
				} else {
					ypos++;
					xpos++;
				}
			} else {
				xpos--;
			}
		}
		corners[2][0]=xpos;
		corners[2][1]=ypos;
		if(kackose==4){
		    xpos=xposcon;
		    ypos=yposcon;
		    while(true){
		    	if(image[ypos][xpos+1]==1){
		    		if(image[ypos+1][xpos-1]==1){
		    			break;
		    		} else {
		    			ypos++;
		    			xpos--;
		    		}
		    	} else {
		    		xpos++;
		    	}
		    }
		    alignment[2][0]=xpos;
		    alignment[2][1]=ypos;
		} else if(kackose==3){
			for(h=ypos+1;h<=ypos+2*cell;h++){
				if(sum(image,h,h,xpos,xpos+4*cell)==4*cell+1){
					corners[2][1]=(double)(ypos+h-1)/2;
					break;
				}
			}
			for(h=xpos-1;h>=xpos-2*cell;h--){
				if(sum(image,ypos-4*cell,ypos,h,h)==4*cell+1){
					corners[2][0]=(double)(xpos+h+1)/2;
					break;
				}
			}
		}
		//if(showDetail)System.out.println("4 Corners (First 3 one)");
		//if(showDetail)dispCorners(corners);
		//if(showDetail)System.out.println("Alignment");
		//if(showDetail)dispCorners(alignment);
		//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		if(kackose==3){
			   corners[0][1]=Math.ceil((corners[0][1]+corners[1][1])/2);
			   corners[0][0]=Math.ceil((corners[0][0]+corners[2][0])/2);
			   corners[1][0]=Math.floor(corners[1][0]);
			   //corners[1][1]=Math.ceil(corners[1][1]);
			   //corners[2][0]=Math.ceil(corners[2][0]);
			   corners[2][1]=Math.floor(corners[2][1]);
			   return corners;
		}
		int step=(int)scaRound((double)cell/3);
		int around=(int)scaRound((double)cell/4);
		
		double[][] endpoint=new double[4][2];
		for(h=0;h<4;h++){
			endpoint[h][0]=alignment[h][0];
			endpoint[h][1]=alignment[h][1];
		}
		endpoint[0][0]=-corners[1][0]+alignment[1][0];
		endpoint[0][1]=-corners[1][1]+alignment[1][1];
		endpoint[3][0]=-corners[2][0]+alignment[2][0];
		endpoint[3][1]=-corners[2][1]+alignment[2][1];
		double[][] progss=new double[4][2];
		for(h=0;h<4;h++){
			progss[h][0]=endpoint[h][0];
			progss[h][1]=endpoint[h][1];
		}
		boolean found=false;//0-false, 1-true
		double boundry=alignment[1][0]+2*cell;
		if(boundry>col){
			boundry=col;
		}
		double bounds=0;
		for(h=(int)alignment[2][0]+step;h<boundry;h+=step){
			progss[2][0]=h;
			progss[2][1]=corners[2][1]+scaRound(((double)h-corners[2][0])*endpoint[3][1]/endpoint[3][0]);
			if(progss[2][1]>=row || progss[2][1]<0){
				break;
			}
			//if( showDetail)System.out.println("Progress x : "+progss[2][0]+" y :"+progss[2][1]);
			bounds=progss[2][1];//bunu sak�n de�i�tirme
			if(image[(int) progss[2][1]][(int) progss[2][0]]==0){ //down
				for(inc=(int) bounds;inc<row;inc++){
					if(image[inc][(int)progss[2][0]]==1){
						progss[2][1]=inc-1;
						found=true;
						break;
					}
				}
			} else { //up
				for(inc=(int)bounds;inc>=bounds-around;inc--){
					if(image[inc][(int) progss[2][0]]==0){
						progss[2][1]=inc;
						found=true;
						break;
					}
				}
			}
			if(found){
		        endpoint[2][0]=progss[2][0];
		        endpoint[2][1]=progss[2][1];
		        endpoint[3][0]=-corners[2][0]+progss[2][0];
		        endpoint[3][1]=-corners[2][1]+progss[2][1];
				//if( showDetail)System.out.println("End Point x : "+endpoint[2][0]+" y :"+endpoint[2][1]);
			}
			found=false;
		}
		
		////%%%%
		
		found=false;
		boundry=alignment[2][1]+2*cell;
		if(boundry>row){
			boundry=row;
		}
		for(h=(int)(alignment[1][1]+step);h<boundry;h+=step){
			progss[1][1]=h;
			progss[1][0]=corners[1][0]+scaRound((h-corners[1][1])*endpoint[0][0]/endpoint[0][1]);
			if(progss[1][0]>col || progss[1][0]<0){
				break;
			}
			bounds=progss[1][0];
			if(image[(int) progss[1][1]][(int) progss[1][0]]==0){//right
				for(inc=(int) bounds;inc<col;inc++){
					if(image[(int) progss[1][1]][inc]==1){
						progss[1][0]=inc-1;
						found=true;
						break;
					}
				}
			} else { //left
				for(inc=(int)bounds;inc>=bounds-around;inc--){
					if(image[(int)progss[1][1]][inc]==0){
						progss[1][0]=inc;
						found=true;
						break;
					}
				}
			}
			if(found){
		        endpoint[1][0]=progss[1][0];
		        endpoint[1][1]=progss[1][1];
		        endpoint[0][0]=-corners[1][0]+progss[1][0];
		        endpoint[0][1]=-corners[1][1]+progss[1][1];
			}
			found=false;
		}
		alignment[1][0]=endpoint[1][0];
		alignment[1][1]=endpoint[1][1];
		alignment[2][0]=endpoint[2][0];
		alignment[2][1]=endpoint[2][1];
		//if(showDetail)System.out.println("New Alignments ");
		//if(showDetail)dispCorners(alignment);
		double down=(alignment[1][1]-corners[1][1])*(alignment[2][0]-corners[2][0])-
						(alignment[1][0]-corners[1][0])*(alignment[2][1]-corners[2][1]);
		if(down==0){
			throw new IllegalArgumentException("Intersection doesn't exist!");
		}
		double upx=corners[1][1]*alignment[1][0]-corners[1][0]*alignment[1][1];//y1*x2-x1*y
		double upy=corners[2][1]*alignment[2][0]-corners[2][0]*alignment[2][1];//y3*x4-y4*x3
		boundry=upy*(alignment[1][0]-corners[1][0])-(alignment[2][0]-corners[2][0])*upx;//upx
		upy=upy*(alignment[1][1]-corners[1][1])-(alignment[2][1]-corners[2][1])*upx;
		upx=boundry;
		corners[3][0]=upx/down;
		corners[3][1]=upy/down;
		//if(showDetail)System.out.println("All 4 corners !");
		//if(showDetail)dispCorners(corners);
		if(corners[3][0]>=col || corners[3][1]>=row || corners[3][0]<0 || corners[3][1]<0){
			throw new IllegalArgumentException("Intersection is in outside of the image!");
		}
		return corners;
	}
	private int sum(int[][] image,int y,int yb,int x,int xb){
		int ret=0;
		int xx;
		for(int yy=y;yy<=yb;yy++){
			for(xx=x;xx<=xb;xx++){
				ret+=image[yy][xx];
			}
		}
		return ret;
	}
	private double cellSize(double[][] coor) {
		double up=0,down=0;
		for(int i=0;i<3;i++){
			up+=coor[i][2]*coor[i][3];
			down+=coor[i][2];
		}
		return up/down;
	}
	/*private void disp(double[][] coor){
		for (int uu=0;uu<coor.length;uu++){
			System.out.println("Center at : " + coor[uu][0] + " , " + coor[uu][1]+
			        " Length : " + coor[uu][3] + " Pixel : " + coor[uu][2]);
		}
	}
	private void disp(ArrayList<Double[]> coor){
		for (int uu=0;uu<coor.size();uu++){
			System.out.println("Center at : " + coor.get(uu)[0] + " , " + coor.get(uu)[1]+
			        " Length : " + coor.get(uu)[3] + " Pixel : " + coor.get(uu)[2]);
		}
	}*/
	private int finderPattern(int[][] gg,double[][] coor){
		int row=gg.length;
		int col=gg[0].length;
		for(int i=0;i<row;i++){
			gg[i][0]=1;
		}
		Vector coordh=new Vector();
		coordh.addElement(new double[4]);
		((double[])coordh.elementAt(0))[2]=1.0;
		((double[])coordh.elementAt(0))[1]=0.0;
		((double[])coordh.elementAt(0))[0]=0.0;
		((double[])coordh.elementAt(0))[3]=0.0;
		double rllen=col/200;
		double hataOrani=2.25;
		int ind;
		int[] a=new int[6];
		double len;
		boolean dogru=false;
		double err=0;
		int sol,sag;
		int solb,sagb;
		double vert;
		int h,k,i;
		for(h=1;h<row;h++){
			ind=0;
			for(k=0;k<a.length;k++){
				a[k]=0;
			}
			for(k=1;k<col;k++){
				if(gg[h][k-1]!=gg[h][k]){
					a[ind]=k-1;
					ind++;
					if(ind==6){
						len=(double)(a[3]-a[2])/3;
						dogru=false;
						if(len>=rllen && a[0]-len>-1 && a[5]+len<=col){
							err=len/hataOrani;
							if(Math.abs(a[1]-a[0]-len)<=err && Math.abs(a[5]-a[4]-len)<=err && 
									Math.abs(a[2]-a[1]-len)<=err &&Math.abs(a[4]-a[3]-len)<=err){
								sol=0;sag=0;
								solb=a[0]-(int)scaRound(len)+1;
								sagb=a[5]+(int)scaRound(len);
								if(solb>=0 && sagb<col)
								{
									for(i=solb;i<=a[0];i++){
										sol+=gg[h][i];
									}
									for(i=a[5]+1;i<=sagb;i++){
										sag+=gg[h][i];
									}
									if(sol>=len-err && sag>=len-err){
										dogru=true;
									}
								}
							}
						}
						if(!dogru){
		                    a[0]=a[2];a[1]=a[3];
		                    a[2]=a[4];a[3]=a[5];
		                    ind=4;
						} else {
							vert=(a[2]+a[3]+1)/2;
							for(i=0;i<coordh.size();i++){
								if(((double[])coordh.elementAt(i))[0]==0.0 || (Math.abs((((double[])coordh.elementAt(i))[0]/((double[])coordh.elementAt(i))[2])-vert)<10.0 
										&& Math.abs((((double[])coordh.elementAt(i))[1]/((double[])coordh.elementAt(i))[2])-h)<=3*(((double[])coordh.elementAt(i))[3]/((double[])coordh.elementAt(i))[2]))){
									if(((double[])coordh.elementAt(i))[0]!=0){
										((double[])coordh.elementAt(i))[2]++;
									} else {
										coordh.addElement(new double[4]);
										((double[])coordh.elementAt(coordh.size()-1))[2]=1.0;
										((double[])coordh.elementAt(coordh.size()-1))[1]=0.0;
										((double[])coordh.elementAt(coordh.size()-1))[0]=0.0;
										((double[])coordh.elementAt(coordh.size()-1))[3]=0.0;										
									}
									((double[])coordh.elementAt(i))[1]+=h;
									((double[])coordh.elementAt(i))[0]+=vert;
									((double[])coordh.elementAt(i))[3]+=len;
									break;
								}
							}
							for(i=0;i<a.length;i++){
								a[i]=0;
							}
							ind=0;
						}
					}
				}
			}
		}		
		//if(showDetail)System.out.println("All patterns");
		//if(showDetail)disp(coordh);
		if(coordh.size()<3)
			throw new IllegalArgumentException("Not enough number of pattern is found");
		int mini=3;
		for(i=3;i<coordh.size();){
			if(mini>=0){
				mini=-1;
				if(((double[])coordh.elementAt(0))[2]<((double[])coordh.elementAt(1))[2] && ((double[])coordh.elementAt(0))[2]<((double[])coordh.elementAt(2))[2]){
					mini=0;
				} else if(((double[])coordh.elementAt(1))[2]<((double[])coordh.elementAt(2))[2]){
					mini=1;
				} else {
					mini=2;
				}
			}
			if(((double[])coordh.elementAt(i))[2]>((double[])coordh.elementAt(0))[2] || 
					((double[])coordh.elementAt(i))[2]>((double[])coordh.elementAt(1))[2] || ((double[])coordh.elementAt(i))[2]>((double[])coordh.elementAt(2))[2]){
				coordh.removeElementAt(mini);
				mini=3;
			} else {
				coordh.removeElementAt(i);
			}
		}

		for(i=0;i<coordh.size();i++){
			((double[])coordh.elementAt(i))[0]/=((double[])coordh.elementAt(i))[2];
			((double[])coordh.elementAt(i))[1]/=((double[])coordh.elementAt(i))[2];
			((double[])coordh.elementAt(i))[3]/=((double[])coordh.elementAt(i))[2];			
		}
		//if(showDetail)System.out.println("3 patterns");
		//if(showDetail)disp(coordh);
		double[] dist=new double[3];
		dist[0]=Math.sqrt((((double[])coordh.elementAt(1))[0]-((double[])coordh.elementAt(2))[0])*(((double[])coordh.elementAt(1))[0]-((double[])coordh.elementAt(2))[0])
				+(((double[])coordh.elementAt(1))[1]-((double[])coordh.elementAt(2))[1])*(((double[])coordh.elementAt(1))[1]-((double[])coordh.elementAt(2))[1]));		
		dist[1]=Math.sqrt((((double[])coordh.elementAt(0))[0]-((double[])coordh.elementAt(2))[0])*(((double[])coordh.elementAt(0))[0]-((double[])coordh.elementAt(2))[0])
				+(((double[])coordh.elementAt(0))[1]-((double[])coordh.elementAt(2))[1])*(((double[])coordh.elementAt(0))[1]-((double[])coordh.elementAt(2))[1]));
		dist[2]=Math.sqrt((((double[])coordh.elementAt(0))[0]-((double[])coordh.elementAt(1))[0])*(((double[])coordh.elementAt(0))[0]-((double[])coordh.elementAt(1))[0])
				+(((double[])coordh.elementAt(0))[1]-((double[])coordh.elementAt(1))[1])*(((double[])coordh.elementAt(0))[1]-((double[])coordh.elementAt(1))[1]));
		if(dist[0]>dist[1] && dist[0]>dist[2]){
			ind=0;
		} else if(dist[1]>dist[2]){
			ind=1;
			double[] dum=(double[])coordh.elementAt(0);
			coordh.removeElementAt(0);
			coordh.insertElementAt(dum,1);
		} else {
			double[] dum=(double[])coordh.elementAt(0);
			coordh.removeElementAt(0);
			coordh.addElement(dum);
			dum=(double[])coordh.elementAt(1);
			coordh.removeElementAt(1);
			coordh.insertElementAt(dum,0);
			ind=2;
		}
		//if(showDetail)System.out.println("3 patterns - middle corrected");
		//if(showDetail)disp(coordh);
		//ArrayList<Double[]> c=(ArrayList<Double[]>) coordh.clone();
		double[][] c=new double[3][2];
		for(i=0;i<3;i++){
			for(k=0;k<2;k++){
				c[i][k]=((double[])coordh.elementAt(i))[k];
			}
		}
		for(i=0;i<2;i++){
			c[2][i]-=c[0][i];			
			c[1][i]-=c[0][i];
		}
		c[2][1]*=-1;		
		c[1][1]*=-1;
		//if(showDetail)System.out.println("Angle Coordinates.");
		//if(showDetail)for(i=1;i<3;i++){for(k=0;k<2;k++){System.out.print(c[i][k]+" ");}System.out.println();}
		
		//if(showDetail)System.out.println("Angles Seperately "+ Math.atan2(c[1][1],c[1][0])+" " +Math.atan2(c[2][1],c[2][0]));
		double angleBet=aTan2(c[1][1],c[1][0])-aTan2(c[2][1],c[2][0]);
		//if(showDetail)System.out.println("Angle between : "+ angleBet);
		if(c[1][1]<0){
			angleBet+=2*Math.PI;
		}
		if(c[2][1]<0){
			angleBet-=2*Math.PI;
		}
		angleBet+=2*Math.PI;
		angleBet%=2*Math.PI;
		//if(showDetail)System.out.println("Angle between : "+ angleBet);
		//if(showDetail)if(angleBet>2*Math.PI || angleBet<0)System.out.println("HATATATTATATA");
		if(angleBet>Math.PI/3 && angleBet<2*Math.PI/3){
			//Do nothing
		} else if(angleBet<5*Math.PI/3 && angleBet>4*Math.PI/3){
			double[] dum=(double[])coordh.elementAt(1);
			coordh.removeElementAt(1);
			coordh.addElement(dum);
		} else {
			throw new IllegalArgumentException("Irreducible shear or Patterns are not found!");
		}
		//if(showDetail)System.out.println("Corrected patterns");
		//if(showDetail)disp(coordh);
		for(i=0;i<3;i++)
			for(k=0;k<4;k++)
				coor[i][k]=((double[])coordh.elementAt(i))[k];
		return (int)scaRound(dist[ind]*0.707);     
	}
	private double aTan2(double y, double x) {
		double coeff_1 = Math.PI / 4d;
		double coeff_2 = 3d * coeff_1;
		double abs_y = Math.abs(y);
		double angle;
		if (x >= 0d) {
			double r = (x - abs_y) / (x + abs_y);
			angle = coeff_1 - coeff_1 * r;
		} else {
			double r = (x + abs_y) / (abs_y - x);
			angle = coeff_2 - coeff_1 * r;
		}
		return y < 0d ? -angle : angle;
	}

	/*private void adaphisteq(int[][] image,int block){
		//8*8
		int row=image.length;
		int col=image[0].length;
		for(int i=0;i<row-block;i+=block){
			for(int j=0;j<col-block;j+=block){
				histeq(image,i,i+block,j,j+block);
			}
		}
	}
	private void histeq(int[][] image,int irow,int frow,int icol,int fcol){
		double[] counts=new double[256];
		for(int i=irow;i<frow;i++){
			for(int j=icol;j<fcol;j++){
				counts[image[i][j]]++;
			}
		}
		int size=(frow-irow)*(fcol-icol);
		counts[0]=255*counts[0]/size;
		for(int i=1;i<counts.length;i++){
			counts[i]=255*counts[i]/size;
			counts[i]+=counts[i-1];
			counts[i-1]=scaRound(counts[i-1]);
		}
		counts[255]=scaRound(counts[255]);
		for(int i=irow;i<frow;i++){
			for(int j=icol;j<fcol;j++){
				image[i][j]=(int) counts[image[i][j]];
			}
		}
	}*/
	
	private int[][] myimresize(int[][] image,int inc) {
		int row=(int)Math.floor((double)image.length/inc);
		int col=(int)Math.floor((double)image[0].length/inc);	
		int i,j;
		int[][] nima=new int[row][col];
		for(i=0;i<row;i++){
			for(j=0;j<col;j++){
				nima[i][j]=image[i*inc][j*inc];			
			}
		}
		return nima;
	}
	private int[][] imPerspecTransform5(int[][] image,double[][] corners,double[][] coor){
		int size=image.length;
		int i,j;
		for(i=0;i<4;i++){
			corners[i][0]=scaRound(corners[i][0]);			
			corners[i][1]=scaRound(corners[i][1]);
		}
		double dist=corners[1][0]-corners[0][0];
		//if(showDetail)System.out.println("4 input points for 3D transform");
		//if(showDetail)for(i=0;i<4;i++){System.out.println("x : "+corners[i][0]+" y : "+corners[i][1]);}
		//if(showDetail)System.out.println("distance : "+dist);
		double[][] matrx=new double[8][8];
		double[] b=new double[8];
		double[][] matrxd=new double[8][8];
		double[] bd=new double[8];
		for(i=0;i<8;i+=2){
			b[i]=corners[0][0];
			matrx[i][2]=1;			
			matrx[i][5]=0;
			matrx[i][0]=corners[i/2][0];
			matrx[i][1]=corners[i/2][1];
			matrx[i][3]=0;
			matrx[i][4]=0;
			matrx[i][6]=-corners[0][0]*corners[i/2][0];
			matrx[i][7]=-corners[0][0]*corners[i/2][1];
		}
		for(i=1;i<8;i+=2){
			b[i]=corners[0][1];
			matrx[i][2]=0;			
			matrx[i][5]=1;			
			matrx[i][0]=0;
			matrx[i][1]=0;
			matrx[i][3]=corners[(i-1)/2][0];
			matrx[i][4]=corners[(i-1)/2][1];
			matrx[i][6]=-corners[0][1]*corners[(i-1)/2][0];
			matrx[i][7]=-corners[0][1]*corners[(i-1)/2][1];
		}
		matrx[2][6]-=dist*corners[1][0];
		matrx[2][7]-=dist*corners[1][1];
		matrx[6][6]-=dist*corners[3][0];
		matrx[6][7]-=dist*corners[3][1];
		matrx[5][6]-=dist*corners[2][0];
		matrx[5][7]-=dist*corners[2][1];
		matrx[7][6]-=dist*corners[3][0];
		matrx[7][7]-=dist*corners[3][1];
		b[2]+=dist;
		b[6]+=dist;
		b[5]+=dist;
		b[7]+=dist;
		
		//if(showDetail)System.out.println("Matrix and B - 3");
		//if(showDetail)for(i=0;i<8;i++){for(j=0;j<8;j++){System.out.print(matrx[i][j]+" ");}System.out.println();}
		//if(showDetail)for(i=0;i<8;i++){System.out.println(b[i]);}
		//if(showDetail)System.out.println("End of Matrix and B");
		
		double[] soln=null;
		try {
			for(i=0;i<8;i++){
				for(j=0;j<8;j++){
					matrxd[i][j]=matrx[i][j];
				}
				bd[i]=b[i];
			}
			soln=solve(matrxd,bd);
			//throw new IllegalArgumentException("dfgdf");
		} catch(IllegalArgumentException e){
			useTwoUncor=false;
			System.out.println("\n%%%%'''''''%%%%'''''''''''Trying other transform''''''%%%%''''''''''''''''\n");
			//e.printStackTrace();
			matrx[4][6]-=dist*corners[1][0];
			matrx[4][7]-=dist*corners[1][1];
			matrx[2][6]+=dist*corners[1][0];
			matrx[2][7]+=dist*corners[1][1];
			matrx[5][6]+=dist*corners[2][0];
			matrx[5][7]+=dist*corners[2][1];
			matrx[3][6]-=dist*corners[2][0];
			matrx[3][7]-=dist*corners[2][1];
			b[2]-=dist;
			b[5]-=dist;
			b[4]+=dist;
			b[3]+=dist;
			//if(showDetail)System.out.println("Matrix and B - 2");
			//if(showDetail)for(i=0;i<8;i++){for(j=0;j<8;j++){System.out.print(matrx[i][j]+" ");}System.out.println();}
			//if(showDetail)for(i=0;i<8;i++){System.out.println(b[i]);}
			//if(showDetail)System.out.println("End of Matrix and B");
			soln=solve(matrx,b);
		}
		
		double[][] matrx2=new double[3][3];
		for(i=0;i<3;i++){
			for(j=0;j<3;j++){
				if(3*i+j!=8){
					matrx2[i][j]=soln[3*i+j];
				}
			}
		}
		matrx2[2][2]=1;
		double w,dumx;
		for(i=0;i<3;i++){
			w=matrx2[2][0]*coor[i][0]+matrx2[2][1]*coor[i][1]+1;
			dumx=(matrx2[0][0]*coor[i][0]+matrx2[0][1]*coor[i][1]+matrx2[0][2])/w;
			coor[i][1]=(matrx2[1][0]*coor[i][0]+matrx2[1][1]*coor[i][1]+matrx2[1][2])/w;
			coor[i][0]=dumx;
		}
		//if(showDetail)System.out.println("3D transformed centers");
		//if(showDetail)disp(coor);
		
		double[][] invmatrx=new double[3][3];
		for(i=0;i<3;i++){
			for(j=0;j<3;j++){
				invmatrx[i][j]=matrx2[i][j];
			}
		}
		//if(showDetail)System.out.println("Matrix to be inverted for 3D transform");
		//if(showDetail)for(i=0;i<3;i++){for(j=0;j<3;j++){System.out.print(matrx2[i][j]+" ");}System.out.println();}
		invmatrx=inverse(invmatrx);
		//if(showDetail)System.out.println("inverted for 3D transform");
		//if(showDetail)for(i=0;i<3;i++){for(j=0;j<3;j++){System.out.print(invmatrx[i][j]+" ");}System.out.println();}
		
		int[][] retima=new int[size][size];
		w=0;
		int x,y;
		for(i=0;i<size;i++){
			for(j=0;j<size;j++){
				//xp=;
				w=1/(invmatrx[2][0]*j+invmatrx[2][1]*i+invmatrx[2][2]);
				x=(int)(scaRound(invmatrx[0][0]*j+invmatrx[0][1]*i+invmatrx[0][2])*w);
				y=(int)(scaRound(invmatrx[1][0]*j+invmatrx[1][1]*i+invmatrx[1][2])*w);
				////if(showDetail)System.out.println(j+" "+i+" <- "+x+" "+y);
				if(x<0 || y<0 || x>=size || y>=size) {
					//if(showDetail)System.out.println("Skipped : i(y) : "+i+" j(x) : "+j);
					//throw new IllegalArgumentException("Skipped item!");
					retima[i][j]=1;//make it white
					continue;
				}
				retima[i][j]=image[y][x];
				//if(showDetail)System.out.println("Point : x : "+x+" , y : "+y);
			}
		}
		return retima;
	}
			
	private double[][] inverse(double[][] mat){
		int size=mat.length;
		double[][] m=new double[size][size];
		double[][] inv=new double[size][size];
		for(int i=0;i<size;i++){
			inv[i][i]=1;
		}
		//dummy variables
		double[] holdm;
		double[] holdinv;

		int k,i,j,p;

		//This block finds LU of the matrix
		for(k=0;k<size;k++){
			p=k;
			for(i=k+1;i<size;i++)
				if(Math.abs(mat[p][k])<Math.abs(mat[i][k])) p=i;
			if(p!=k){
				holdm=mat[p];mat[p]=mat[k];mat[k]=holdm;
				holdinv=inv[p];inv[p]=inv[k];inv[k]=holdinv;
			}
			if(mat[k][k]==0.0)
				throw new IllegalArgumentException("The matrix can not be inversed");
			for(i=k+1;i<size;i++)
				m[i][k]=mat[i][k]/mat[k][k];
			for(i=k+1;i<size;i++){
				for(j=0;j<k+1;j++){
					inv[i][j]-=m[i][k]*inv[k][j];
				}
			}
			for(i=k+1;i<size;i++){
				for(j=k+1;j<size;j++){
					mat[i][j]-=m[i][k]*mat[k][j];
					inv[i][j]-=m[i][k]*inv[k][j];
				}
			}
		}
		//if(showDetail)System.out.println("Ara i�lemler");
		//if(showDetail)for(i=0;i<size;i++){for(j=0;j<size;j++){System.out.printf(" "+mat[i][j]);}System.out.printf("\n");}
		//if(showDetail)System.out.print("--------\n");
		//if(showDetail)for(i=0;i<size;i++){for(j=0;j<size;j++){System.out.print(" "+inv[i][j]);}System.out.println();}
		
		//the matrix is reduced to be diagonal
		for(j=size-1;j>-1;j--){
			for(i=0;i<size;i++)
				inv[j][i]/=mat[j][j];
			for(k=j-1;k>=0;k--)
				for(i=0;i<size;i++)
					inv[k][i]-=mat[k][j]*inv[j][i];
		}

		//if(showDetail)System.out.println("Inverse");
		//if(showDetail)for(i=0;i<size;i++){for(j=0;j<size;j++){System.out.print(" "+inv[i][j]);}System.out.println();}
		return inv;
	}
	
	private int[][] imTwoDTransform(int[][] image,double[][] coor,double cell,double distance) {	
		
		double[][] matrx1=new double[3][3];
		double[][] matrx2=new double[3][3];
		int i,j;
		for(i=0;i<3;i++){
			for(j=0;j<2;j++){
				matrx1[i][j]=5*cell;
				matrx2[i][j]=5*cell;
			}
			matrx1[i][2]=1;
			matrx2[i][2]=1;
		}
		matrx1[1][0]+=distance;
		matrx2[1][0]+=distance;
		matrx1[2][1]+=distance;
		matrx2[2][1]+=distance;
		

		//if(showDetail)System.out.println("Input Coordinates");
		//if(showDetail)disp(coor);
		//if(showDetail)System.out.println("Output Coordinates");
		//if(showDetail)for(i=0;i<3;i++){System.out.println("x : "+matrx1[i][0]+" y : "+matrx1[i][1]);}
		double[] inp1=new double[3];
		double[] inp2=new double[3];
		for(i=0;i<3;i++){
			inp1[i]=coor[i][0];
			inp2[i]=coor[i][1];
		}
		for(i=0;i<3;i++){
			for(j=0;j<3;j++){
				matrx1[i][j]=scaRound(matrx1[i][j]);
				matrx2[i][j]=scaRound(matrx2[i][j]);
			}
			inp1[i]=scaRound(inp1[i]);
			inp2[i]=scaRound(inp2[i]);
		}
		
		//if(showDetail)System.out.println("Input Matrix");
		//if(showDetail)for(i=0;i<3;i++){for(j=0;j<3;j++){System.out.print(matrx1[i][j]+" ");}System.out.println();}
		//if(showDetail)for(i=0;i<3;i++){System.out.println(inp1[i]);}
		//if(showDetail)for(i=0;i<3;i++){System.out.println(inp2[i]);}
		
		double[] out1=solve(matrx1,inp1);
		
		double[] out2=solve(matrx2,inp2);
		
		for(i=0;i<2;i++){
			for(j=0;j<3;j++){
				matrx1[j][i]=coor[j][i];
				matrx2[j][i]=coor[j][i];
			}
		}
		for(i=0;i<3;i++){
			matrx1[i][2]=1;
			matrx2[i][2]=1;
			inp1[i]=5*cell;
			inp2[i]=5*cell;
		}
		inp1[1]+=distance;
		inp2[2]+=distance;
		
		for(i=0;i<3;i++){
			for(j=0;j<3;j++){
				matrx1[i][j]=scaRound(matrx1[i][j]);
				matrx2[i][j]=scaRound(matrx2[i][j]);
			}
			inp1[i]=scaRound(inp1[i]);
			inp2[i]=scaRound(inp2[i]);
		}
		
		inp1=solve(matrx1,inp1);
		
		inp2=solve(matrx2,inp2);
		
		double dummy;
		for(i=0;i<3;i++){
			dummy=coor[i][0];
			coor[i][0]=inp1[0]*coor[i][0]+inp1[1]*coor[i][1]+inp1[2];
			coor[i][1]=inp2[0]*dummy+inp2[1]*coor[i][1]+inp2[2];
		}
		//if(showDetail)System.out.println("Translated centers");
		//if(showDetail)disp(coor);
		int size=(int)(distance+13*cell);
		int[][] nima=new int[size][size];
		int x=0,y=0;
		int xb=image[0].length,yb=image.length;
		for(i=0;i<size;i++){
			for(j=0;j<size;j++){
				x=(int)scaRound(out1[0]*j+out1[1]*i+out1[2]);
				y=(int)scaRound(out2[0]*j+out2[1]*i+out2[2]);
				if(x<0 || y<0 || x>=xb || y>=yb) {
					//if(showDetail)System.out.println("Skipped : i(y) : "+i+" j(x) : "+j);
					//throw new IllegalArgumentException("Skipped item!");
					continue;
				}
				nima[i][j]=image[y][x];
				//if(showDetail)System.out.println("Point : x : "+x+" , y : "+y);
			}
		}
		return nima;
	}
	
	
	private double[] solve(double[][] mat,double[] vector){
		int size=mat.length;
		double[][] m=new double[size][size];
		double[] soln=new double[size];//Solution vector
		//ArrayList<Integer[]> changes=new ArrayList<Integer[]>();
		//This block finds LU of the matrix
		int k,i,j,p;
		for(k=0;k<size;k++){
			p=k;
			for(i=k+1;i<size;i++)
				if(Math.abs(mat[p][k])<Math.abs(mat[i][k])) p=i;
			if(p!=k){
				double holdvec;
				for(i=0;i<size;i++){
					holdvec=mat[p][i];
					mat[p][i]=mat[k][i];
					mat[k][i]=holdvec;
				}
				holdvec=vector[p];
				vector[p]=vector[k];
				vector[k]=holdvec;
				//Integer[] dumm={p,k};
				//changes.add(dumm);
				//if(showDetail)System.out.println("Change existed!!!!!");
			}
			//if(showDetail)System.out.println(k);
			if(mat[k][k]==0.0)
				throw new IllegalArgumentException("Solution doesn't exist to the transform!");
			for(i=k+1;i<size;i++)
				m[i][k]=mat[i][k]/mat[k][k];
			for(i=k+1;i<size;i++){
				for(j=k+1;j<size;j++){
					mat[i][j]-=m[i][k]*mat[k][j];
				}
				vector[i]-=m[i][k]*vector[k];
			}
		}
		/*
		if(debug)
			for(i=0;i<size;i++){
				for(j=0;j<size;j++){
					printf("%lf ",mat[i][j]);
			}
			printf("\n");
		}*/

		//Back substitution for Upper Triangular system
		for(j=size-1;j>-1;j--){
			soln[j]=vector[j]/mat[j][j];
			for(i=0;i<j;i++)
				vector[i]-=mat[i][j]*soln[j];
		}
		
		//if(showDetail)System.out.println("Solution");
		//if(showDetail)for(i=0;i<size;i++)System.out.println(" "+soln[i]);
		
		/*double dummy=0;
		while(changes.size()>0){
			int f=changes.size()-1;
			dummy=soln[changes.get(f)[0]];
			soln[changes.get(f)[0]]=soln[changes.get(f)[1]];
			soln[changes.get(f)[1]]=dummy;
			changes.remove(f);
		}
		
		//if(showDetail)System.out.println("Corrected Solution");
		//if(showDetail)for(i=0;i<size;i++)System.out.println(" "+soln[i]);*/
		
		return soln;
	}

	/*private int[][] imresize(int[][] image,double scale) throws NoninvertibleTransformException {
		PerspectiveTransform transf=new PerspectiveTransform();
		transf.scale(scale,scale);
		Point pointS=new Point(0,0);		
		Point pointD=new Point(0,0);
		
		int row=(int)Math.floor(image.length*scale);
		int col=(int)Math.floor(image[0].length*scale);		
		int[][] nima=new int[row][col];
		
		for(pointS.y=0;pointS.y<row;pointS.y++){
			for(pointS.x=0;pointS.x<col;pointS.x++){
				transf.inverseTransform(pointS,pointD);
				nima[pointS.y][pointS.x]=image[pointD.y][pointD.x];
				//if(showDetail)System.out.println(pointD.getX()+" "+pointD.getY());
			}
		}
		return nima;
	}*/
	private void thresholdOtsu(int[][] image){
		double[] counts=new double[256];
		int row=image.length;
		int col=image[0].length;
		int i,j;
		for(i=0;i<row;i++){
			for(j=0;j<col;j++){
				counts[image[i][j]]++;
			}
		}
		//for(int y=0;y<256;y++)
		//if(showDetail)System .out.println(counts[y]);
		
		//counts=omega mu=mu
		double[] mu=new double[256];
		int size=row*col;
		counts[0]/=size;
		mu[0]=counts[0];
		for(i=1;i<counts.length;i++){
			counts[i]/=size;
			mu[i]=counts[i]*(i+1)+mu[i-1];
			counts[i]+=counts[i-1];
		}
		//sigma_b_squared
		double max=0;
		for(i=0;i<counts.length;i++){
			counts[i]=((mu[255]*counts[i]-mu[i])*(mu[255]*counts[i]-mu[i]))/(counts[i]*(1-counts[i]));
			if(counts[i]>max){
				max=counts[i];
			}
			//if(showDetail)System.out.println(counts[i]);
		}

		int ind=0;
		double top=0;
		for(i=0;i<counts.length;i++){
			if(counts[i]==max){
				top+=i;
				ind++;
			}
		}
		top/=ind;
		for(i=0;i<row;i++){
			for(j=0;j<col;j++){
				if(image[i][j]>top)
					image[i][j]=1;
				else					
					image[i][j]=0;
			}
		}
		//return ((top)/(255));//level
	}
	
	public int[][] createImageMatrix(Image acilan){
		BufferedImage OSC = new BufferedImage(acilan.getWidth(null),acilan.getHeight(null),BufferedImage.TYPE_INT_RGB);
        OSC.getGraphics().drawImage(acilan,0,0,null);
        int[][] ima=new int[OSC.getHeight()][OSC.getWidth()];
        int red,green,blue,rgb,y,x;
        for(y=0;y<ima.length;y++){
      	  for(x=0;x<ima[0].length;x++){
      		  rgb = OSC.getRGB(x,y);
      		  red = (rgb >> 16) & 0xFF;
		      green = (rgb >> 8) & 0xFF;
		      blue = rgb & 0xFF;
      		  
		      //Gray threshold
		      ima[y][x]=(int) scaRound((double)(red+green+blue)/3);
      		  
      		  //YIQ - Y
      		  //ima[y][x]=(int) scaRound((0.29889531*(double)red+0.58662247*(double)green+0.11448223*(double)blue)/3);
      		  
      		  //YIQ - I
      		  //ima[y][x]=(int) scaRound((0.59597799*(double)red-0.27417610*(double)green+0.32180189*(double)blue)/3);

      		  //ima[y][x]=((red+green+blue)/3);
		      //if(showDetail)System.out.print(ima[y][x]+" ");
      	  }
	      //if(showDetail)System.out.println();
        }
        return ima;
	}
	
	public Image openImage(String file){
		Image acilan=null;
		try {
			acilan=Toolkit.getDefaultToolkit().createImage(
					new URL(file));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while(acilan.getHeight(null)<0 || acilan.getWidth(null)<0 ){
			try {
				int time=4000/reduc;
				//if(showDetail)System.out.println("Sleeping for "+ time +"ms");
				Thread.sleep(time);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return acilan;
	}

}



