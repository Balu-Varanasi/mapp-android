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
		this(((double)start.y - end.y) / (start.x - end.x), start.y - ((double)start.y - end.y) / (start.x - end.x)*start.x, start, end);
	}
	
	/**
	 * Constructor voor lijn van vorm y = ax + b
	 */
	public AlgebraLine(double a, double b, Point start, Point end) {
		this.a = a;
		this.b = b;
		this.start = start;
		this.end = end;
	}
	
	/**
	 * Bekijkt of een punt op of dichtbij genoeg bij de lijn ligt
	 * @param p het punt om te bekijken
	 * @param d de maximale afstand tot de lijn
	 * @return true als het punt dichtbij genoeg ligt
	 */
	public boolean isNear(Point p, double d) {
		double[] proj = projection(p, this.start);
		if (start.x <= end.x) {
			if (proj[0] < start.x)
				proj[0] = start.x;
			if (proj[0] > end.x)
				proj[0] = end.x;
		}
		else {
			if (proj[0] > start.x)
				proj[0] = start.x;
			if (proj[0] < end.x)
				proj[0] = end.x;
		}
		if (start.y <= end.y) {
			if (proj[1] < start.y)
				proj[1] = start.y;
			if (proj[1] > end.y)
				proj[1] = end.y;
		}
		else {
			if (proj[1] > start.y)
				proj[1] = start.y;
			if (proj[1] < end.y)
				proj[1] = end.y;
		}
		Log.v(HelloGoogleMaps.TAG, "Projection: "+proj[0] + ' ' +proj[1]);
		double distance = Math.sqrt(Math.pow(p.y - proj[1],2) + Math.pow(p.x - proj[0],2));
		Log.v(HelloGoogleMaps.TAG, "Distance: "+distance);
		return distance <= d;
	}

	/**
	 * Geeft de projectie van een punt op een vector.
	 * Vector is in dit geval een punt op de vectorlijn, maar dat maakt voor het principe niet uit
	 * @param p het punt om te bekijken
	 * @param V de vector om te bekijken
	 * @return GeoPoint met de coordinaten van de projectie
	 */
	public static double[] projection(Point p, Point V) {
		double a = ((double)innerproduct(p, V)) / innerproduct(V, V);
		double[] r = new double[2];
		r[0] = a * p.x;
		r[1] = a * p.y;
		return r;
	}
	
	/**
	 * Rekent het inproduct uit van twee GeoPoints (waar ze dus als vectoren worden beschouwd)
	 * @param p het eerste geopoint
	 * @param q het tweede geopoint
	 * @return int het inproduct van de twee geopoints
	 */
	private static int innerproduct(Point p, Point q) {
		return p.y * q.y + p.x * q.x;
	}
}
