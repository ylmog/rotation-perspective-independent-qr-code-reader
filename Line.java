/*
 * created: 2004/09/13
 */
package mypack2;


public class Line {
	int x1, y1, x2, y2;
	
	public Point getP1() {
		return new Point(x1, y1);
	}
	
	public Point getP2() {
		return new Point(x2, y2);
	}
}
