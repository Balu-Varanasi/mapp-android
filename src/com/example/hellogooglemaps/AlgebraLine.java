package com.example.hellogooglemaps;

import android.util.Log;
import android.graphics.Point;

/**
 * Line-class die algebra uitvoert betreffende een lijn
 * Lijnen in de vorm y = ax + b
 * @author Joost
 */

public class AlgebraLine {
	private double a;
	private double b;
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
		Log.v(HelloGoogleMaps.TAG, "Points: A: "+this.start.x+' '+this.start.y+" B: "+this.end.x+' '+this.end.y);
	}
	
	/**
	 * Constructor voor lijn van vorm y = ax + b
	 */
	public AlgebraLine(double a, double b, Point start, Point end) {
		this.a = a;
		this.b = b;
		if (start.x <= end.x) {
			this.start = start;
			this.end = end;
		}
		else {
			this.end = start;
			this.start = end;
		}
		Log.v(HelloGoogleMaps.TAG, "Points: A: "+this.start.x+' '+this.start.y+" B: "+this.end.x+' '+this.end.y);
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
	 * Geeft de projectie van een punt op een vector.
	 * Vector is in dit geval een punt op de vectorlijn, maar dat maakt voor het principe niet uit
	 * @param p het punt om te bekijken
	 * @param V de vector om te bekijken
	 * @return GeoPoint met de coordinaten van de projectie
	 */
	private double[] projection(Point p) {
		double[] proj = new double[2];
		//http://www.codeguru.com/forum/showthread.php?t=194400
		double r = segmentalInproduct(start, p, start, end) / Math.pow(distance(start, end),2);
		Log.v(HelloGoogleMaps.TAG, "r: "+r);
		if (r <= 0) {
			proj[0] = start.x;
			proj[1] = start.y;
		}
		else if (r >= 1) {
			proj[0] = end.x;
			proj[1] = end.y;
		}
		else {
			proj[0] = start.x + r*(end.x - start.x);
			proj[1] = start.y + r*(end.y - start.y);
		}
		return proj;	
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
	
	/**
	 * Rekent de afstand uit tussen twee punten, waarvan 1 bestaande uit doubles
	 * @param p het eerste point
	 * @param qx het x-coordinaat van het eerste punt
	 * @param qy het y-coordinaat van het tweede punt
	 * @return double de afstand tussen de punten
	 */
	private static double distance(Point p, double qx, double qy) {
		return Math.sqrt(Math.pow(p.y - qy,2) + Math.pow(p.x - qx,2));
	}
	
	/**
	 * Rekent het inproduct uit van twee segmenten
	 * @param A het eerste point van segment AB
	 * @param B het tweede point van segment AB
	 * @param C het eerste point van segment CD
	 * @param D het tweede point van segment CD
	 * @return int het inproduct van de twee points
	 */
	private static int segmentalInproduct(Point A, Point B, Point C, Point D) {
		return (A.x - B.x) * (C.x - D.x) + (A.x - B.x) * (C.x - D.x);
	}

}
