package nl.appcetera.mapp;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.maps.GeoPoint;

/**
 * Beheert een polygoonstructuur
 * @author Mathijs
 * @author Joost
 */
public class PolygonManager
{
	private ArrayList<GeoPoint> polygon = new ArrayList<GeoPoint>();
	private int polygonPointer = 0;
	private boolean isClosed = false;
	private int polygonId = 0;
	private int color;
	private String name = "";
	private String description = "";
	private boolean dbEnable = true;
	
	/**
	 * Voegt een punt toe aan de polygoon
	 * @param p: het toe te voegen punt
	 */
	public void addPoint(GeoPoint p)
	{
		if(!isClosed)
		{
			if(dbEnable)
			{
				Mapp.getDatabase().addPolygonPoint(polygonId, p.getLatitudeE6(), 
						p.getLongitudeE6(), polygon.size());
			}
			polygon.add(p);
		}
	}
	
	/**
	 * Voegt een punt toe aan de polygoon op de huidige plaats, en schuift de rest 1 plaats op
	 * @param p: het toe te voegen punt
	 */
	public void addIntermediatePoint(GeoPoint p, int index)
	{
		if(dbEnable)
		{
			Mapp.getDatabase().movePolygonPointsIndexes(polygonId, index, 1);
			Mapp.getDatabase().addPolygonPoint(polygonId, p.getLatitudeE6(), 
					p.getLongitudeE6(), index);
		}
		polygon.add(index, p);
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
				if(dbEnable)
				{
					Mapp.getDatabase().removePolygonPoint(polygonId, i);
					Mapp.getDatabase().movePolygonPointsIndexes(polygonId, i, -1);
				}
				polygon.remove(i);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Past een punt uit de polygoon aan
	 * @param p het te vervangen punt
	 * @param p2 het nieuwe punt
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
	 * Sla de nieuwe positie van een verplaatst punt op
	 * @param id de index van het punt
	 * @param p de nieuwe positie van het punt
	 * @return
	 */
	public boolean saveMovedPoint(int id, GeoPoint p)
	{	
		if(dbEnable)
		{
			Mapp.getDatabase().editPolygonPoint(polygonId, p.getLatitudeE6(), p.getLongitudeE6(), id);
			return true;
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
	 * Geeft een specifiek punt aan de hand van een index
	 * @return het gevraagde punt
	 */
	public GeoPoint getPoint(int a)
	{
		GeoPoint p = polygon.get(a % polygon.size());
		return p;
	}

	/**
	 * Geeft het vorige punt uit de polygoon
	 * @return het vorige punt uit de polygoon
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
		if(dbEnable)
		{
			Mapp.getDatabase().editPolygon(polygonId, color, val, name);
		}
		isClosed = val;
	}

	/**
	 * Geeft het laatste punt van deze polygoon terug
	 * @return laatste punt van de polygoon
	 */
	public GeoPoint getLastPoint()
	{
		return getPoint(polygon.size()-1 );
	}
	
	/**
	 * Zet het id van deze polygoon
	 * @param id het id van de polygoon
	 */
	public void setId(int id)
	{
		polygonId = id;
	}
	
	/**
	 * Geeft het id van deze polygoon terug
	 * @return id van de polygoon
	 */
	public int getId()
	{
		return polygonId;
	}
	
	/**
	 * Zet de kleur van deze polygoon
	 * @param color de kleur van de polygoon
	 */
	public void setColor(int color)
	{
		if(dbEnable)
		{
			Mapp.getDatabase().editPolygon(polygonId, color, isClosed, name);
		}
		this.color = color;
	}
	
	/**
	 * Geeft de kleur van deze polygoon
	 * @return kleur van de polygoon
	 */
	public int getColor()
	{
		return this.color;
	}
	
	/**
	 * Geeft de huidige waarde van de pointer terug
	 * @return
	 */
	public int getPointer()
	{
		return polygonPointer;
	}
	
	/**
	 * Zet de enable state van de database
	 * Tijdens het initialiseren van de polygonmanager met waarden uit de database,
	 * moet de enable op false staan om onnodige queries te voorkomen
	 * @param enable true voor enable, false voor niet enable
	 */
	public void setDbEnable(boolean enable)
	{
		dbEnable = enable;
	}
	
	/**
	 * Deze functie geeft het aantal punten dat de polygoon bevat
	 * @return het aantal punten van de polygon
	 */
	public int getPointCount()
	{
		return polygon.size();
	}
	
	/**
	 * Stelt de naam van deze polygoon in op de gegeven string
	 * @param name de nieuwe naam
	 */
	public void setName(String name)
	{
		this.name = name;
		
		if(dbEnable)
		{
			Mapp.getDatabase().editPolygon(polygonId, color, isClosed, name);
		}
	}
	
	/**
	 * Geef de huidige omschrijving van deze polygoon terug
	 * @return de omschrijving van de polygoon
	 */
	public String getDescription()
	{
		return this.description;
	}
	
	/**
	 * Stelt de omschrijving van deze polygoon in op de gegeven string
	 * @param description de nieuwe omschrijving
	 */
	public void setDescription(String description)
	{
		this.description = description;
		
		if(dbEnable)
		{
			//Mapp.getDatabase().editPolygon(polygonId, color, isClosed, name);
		}
	}
	
	/**
	 * Geef de huidige naam van deze polygoon terug
	 * @return de naam van de polygoon
	 */
	public String getName()
	{
		return this.name;
	}

	/**
	 * Deze functie begint een nieuwe activity waarmee we metadata kunnen aanpassen
	 * @param instance 
	 */
	public void editMetaData(Mapp instance) {
		
		Log.v(Mapp.TAG, "checkpoint 1");
		Intent intent = new Intent(instance, MetaEditScreen.class);
	
		//Next create the bundle and initialize it
		Bundle bundle = new Bundle();
	
		//Add the parameters to bundle as
		bundle.putInt("ID",polygonId);
		bundle.putInt("COLOR",color);
		bundle.putString("NAME",name);
		bundle.putString("DESCRIPTION",description);
		
		//Add this bundle to the intent
		intent.putExtras(bundle);
		
		//Start next activity
		instance.startActivity(intent);
		Log.v(Mapp.TAG, "checkpoint 6");
	}
}
