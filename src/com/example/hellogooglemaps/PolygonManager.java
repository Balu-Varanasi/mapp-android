package com.example.hellogooglemaps;

import java.util.ArrayList;

import com.google.android.maps.GeoPoint;

/**
 * Beheert een polygoonstructuur
 * @author Mathijs
 *
 */
public class PolygonManager
{
	private ArrayList<GeoPoint> polygon = new ArrayList<GeoPoint>();
	private int polygonPointer = 0;
	private boolean isClosed = false;
	
	/**
	 * Voegt een punt toe aan de polygoon
	 * @param p: het toe te voegen punt
	 */
	public void addPoint(GeoPoint p)
	{
		if(!isClosed)
		{
			polygon.add(p);
		}
	}
	
	/**
	 * Verwijdert het opgegeven punt uit de polygoon
	 * @param p: het te verwijderen punt
	 * @return true als het punt verwijdert is, false als het niet gevonden kon worden
	 */
	public boolean removePoint(GeoPoint p)
	{
		for(int i = 0; i < polygon.size(); i++)
		{
			if(polygon.get(i).equals(p))
			{
				polygon.remove(i);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Past een punt uit de polygoon aan
	 * @param het te vervangen punt
	 * @param het nieuwe punt
	 * @return true als het punt vervangen is, false als het niet gevonden kon worden
	 */
	public boolean editPoint(GeoPoint p, GeoPoint p2)
	{
		for(int i = 0; i < polygon.size(); i++)
		{
			if(polygon.get(i).equals(p))
			{
				polygon.set(i, p2);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checkt of er nog een volgend punt is om op te vragen
	 * @return true indien er nog een volgend punt is
	 */
	public boolean hasNextPoint()
	{
		return (polygonPointer < polygon.size()) && (polygon.size() != 0);
	}
	
	/**
	 * Geeft het volgende punt in de polygoon
	 * @return volgende punt uit de polygoon
	 */
	public GeoPoint getNextPoint()
	{
		GeoPoint p = polygon.get(polygonPointer);
		polygonPointer++;
		return p;
	}
	
	/**
	 * Geeft het huidige punt uit de polygoon
	 * @return het huidige punt uit de polygoon
	 */
	public GeoPoint getPreviousPoint()
	{
		if(polygonPointer == 0)
		{
			return polygon.get(polygonPointer);
		}
		
		return polygon.get(polygonPointer-1);
	}
	
	/**
	 * Geeft het eerste punt uit de polygoon, mits er minimaal 1 punt is
	 * @return het eerste punt uit de polygoon
	 */
	public GeoPoint getFirstPoint()
	{
		if(polygon.size() > 0)
		{
			return polygon.get(0);
		}
		
		return new GeoPoint(0,0);
	}
	
	/**
	 * Geeft het aantal punten in de polygoon
	 * @return aantal punten in de polygoon
	 */
	public int getNumPoints()
	{
		return polygon.size();
	}
	
	/**
	 * Reset de interne polygonpointer
	 */
	public void reset()
	{
		polygonPointer = 0;
	}
	
	/**
	 * Geeft aan of de polygoon gesloten is
	 * @return true indien polygoon gesloten
	 */
	public boolean getIsClosed()
	{
		return isClosed;
	}
	
	/**
	 * Geeft aan of de polygoon gesloten is
	 * @param true indien de polygoon gesloten is
	 */
	public void setIsClosed(boolean val)
	{
		isClosed = val;
	}
}