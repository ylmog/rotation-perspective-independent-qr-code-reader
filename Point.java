/*
 * created: 2004/09/13
 */
package mypack2;

public class Point{
	public static final int RIGHT = 1;
	public static final int BOTTOM = 2;
	public static final int LEFT = 4;
	public static final int TOP = 8;
	
	int x;
	int y;
	
	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
}
