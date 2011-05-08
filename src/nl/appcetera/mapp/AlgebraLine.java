package nl.appcetera.mapp;

import android.util.Log;
import android.graphics.Point;

/**
 * Line-class die algebra uitvoert betreffende een lijn
 * Lijnen in de vorm y = ax + b
 * @author Joost
 */

public class AlgebraLine {
	private Point start;
	private Point end;
	
	/**
	 * Constructor voor lijn uit 2 punten
	 */
	public AlgebraLine(Point start, Point end) {
		//a = (Ay - By) / (Ax - Bx)
		//b = Ay - (Ay - By) / (Ax - Bx) * Ax
		//this(((double)start.y - end.y) / (start.x - end.x), start.y - ((double)start.y - end.y) / (start.x - end.x)*start.x, start, end);
		if (start.x <= end.x) {
			this.start = start;
			this.end = end;
		}
		else {
			this.end = start;
			this.start = end;
		}
		Log.v(Mapp.TAG, "Points: A: "+this.start.x+' '+this.start.y+" B: "+this.end.x+' '+this.end.y);
	}
	
	/**
	 * Constructor voor lijn van vorm y = ax + b
	 */
	public AlgebraLine(int a, int b) {
		Point start = new Point(0, b);
		Point end = new Point(a + b, 1);
		if (start.x <= end.x) {
			this.start = start;
			this.end = end;
		}
		else {
			this.end = start;
			this.start = end;
		}
		Log.v(Mapp.TAG, "Points: A: "+this.start.x+' '+this.start.y+" B: "+this.end.x+' '+this.end.y);
	}
	
	/**
	 * Bekijkt of een punt op of dichtbij genoeg bij de lijn ligt
	 * @param p het punt om te bekijken
	 * @param d de maximale afstand tot de lijn
	 * @return true als het punt dichtbij genoeg ligt
	 */
	public boolean isNear(Point p, double d) {
		//double[] proj = projection(p);
		//Log.v(HelloGoogleMaps.TAG, "Projection: "+proj[0] + ' ' +proj[1]);
		//double distance = distance(p, proj[0], proj[1]);
		if (p.x > end.x + d || p.x < start.x - d) {
			return false;
		}
		if ((end.y > start.y && p.y > end.y + d) || (end.y < start.y && p.y > start.y + d)) {
			return false;
		}
		if ((end.y > start.y && p.y < start.y - d) || (end.y < start.y && p.y < end.y - d)) {
			return false;
		}
		double distance = Math.abs((end.x - start.x) * (start.y - p.y) - (end.y - start.y) * (start.x - p.x)) / distance(start, end);
		//Log.v(HelloGoogleMaps.TAG, "Distance: "+distance);
		return distance <= d;
	}

	/**
	 * Rekent de afstand uit tussen twee punten
	 * @param p het eerste point
	 * @param q het tweede point
	 * @return double de afstand tussen de punten
	 */
	private static double distance(Point p, Point q) {
		return Math.sqrt(Math.pow(p.y - q.y,2) + Math.pow(p.x - q.x,2));
	}

}
