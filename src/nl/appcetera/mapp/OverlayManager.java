package nl.appcetera.mapp;

import java.util.List;
import java.util.Random;

import android.database.Cursor;
import android.graphics.Color;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * Beheert de overlays in de mapview
 * @author Mathijs
 */
public class OverlayManager
{
	private MapView mv;
	private PolygonData db;
	private static final String newPolygonNamePrefix = "New Polygon";
	private static final String newPolygonDescription = "<No description yet>";
	private static int groupId = 1;
	private static boolean editModeMutex = false; //False indien niemand in editmode zit, true indien dat wel 't geval is
	
	/**
	 * Constructor
	 * @param mv MapView instance
	 * @param db PolygonData instance
	 */
	public OverlayManager(MapView mv, PolygonData db)
	{
		this.mv = mv;
		this.db = db;
	}
	
	/**
	 * Zet het nieuwe id van de groep die je wilt bekijken
	 * @param group het id van de groep om polygonen uit te tonen
	 */
	public static void setGroup(int group)
	{
		OverlayManager.groupId = group;
	}
	
	/**
	 * Handige statische methode om het huidige groepid op te vragen
	 * @return groepid dat nu wordt bekeken
	 */
	public static int getGroupId()
	{
		return OverlayManager.groupId;
	}
	
	/**
	 * Laadt alle polygonen uit de database en plaatst deze in overlays
	 */
	public void loadOverlays()
	{
		List<Overlay> listOfOverlays = mv.getOverlays();
		listOfOverlays.clear();
		
		OverlayManager.editModeMutex = false; // De mutex vrijgeven
        
		Cursor c = db.getAllPolygons(groupId);
		
		// Er zijn geen polygonen opgeslagen
		if(!c.moveToFirst())
		{
			addOverlay();
		}
		else
		{
			do
			{
				// Niewe laag + polygoon toevoegen
				PolygonOverlay mapOverlay = new PolygonOverlay(mv, c.getInt(1));
				
		        // De polygonmanager goed instellen
		        PolygonManager pm = mapOverlay.getManager();
		        pm.setDbEnable(false);
		        pm.setId(c.getInt(0));
				pm.setColor(c.getInt(1));
				pm.setName(c.getString(4));
				pm.setDescription(c.getString(5));
		        boolean isClosed = (c.getInt(2) == 1);

		        // Punten ophalen en toevoegen aan de manager
		        Cursor c2 = db.getAllPolygonPoints(pm.getId());
		        
		        if(c2.moveToFirst())
		        {
			        do
			        {
			        	GeoPoint p = new GeoPoint((int) c2.getLong(0), (int) c2.getLong(1));
			        	pm.addPoint(p);
			        }
			        while(c2.moveToNext());
		        }
		        
		        pm.setIsClosed(isClosed);
		        /*if(!isClosed)
		        {
		        	mapOverlay.setIsEditMode();
		        }*/
		        pm.setDbEnable(true);
		        listOfOverlays.add(mapOverlay);
			}
			while(c.moveToNext());
		}

		this.mv.postInvalidate();
	}
	
	/**
	 * Voegt een overlay toe aan de lijst van overlays
	 * @return de nieuw gemaakte overlay, of null indien er geen werd gemaakt
	 */
	public PolygonOverlay addOverlay()
	{
		if(!OverlayManager.editModeMutex(true))
		{
			return null;
		}
		
		List<Overlay> listOfOverlays = mv.getOverlays();
		
		// Check of de polygonen uit de andere lagen wel gesloten zijn
		if(listOfOverlays.size() > 0)
		{
			for(int i = 0; i < listOfOverlays.size(); i++)
			{
				Overlay q = (Overlay) listOfOverlays.get(i);
				if (q.getClass().getName().equals("PolygonOverlay")) {
					PolygonOverlay p = (PolygonOverlay) q;
					PolygonManager pm = p.getManager();
					if(!pm.getIsClosed() || p.getIsEditMode())
					{
						// Nee dus, geen nieuwe laag maken
						return null;
					}
				}
			}
		}
		
		// Maak een willekeurige kleur
		Random r = new Random();
		int color = Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256));
		// Maak een nieuwe overlay
		PolygonOverlay po = new PolygonOverlay(mv, color);
		po.getManager().setDbEnable(false);

		int id = db.addPolygon(color, false, groupId);
        po.getManager().setId(id);
        po.getManager().setColor(color);
        
        po.getManager().setDbEnable(true);
        po.getManager().setName(newPolygonNamePrefix + " " + Mapp.getDatabase().getNumPolygons(groupId));
        po.getManager().setDescription(newPolygonDescription);
        listOfOverlays.add(po);
        
        return po;
	}
	
	/**
	 * Zet de mutex state op gegeven parameter
	 * @param b true indien je de editmode in gaat, false indien je d'r uit gaat
	 * @return true indien de mutex vrij is
	 */
	public static boolean editModeMutex(boolean b)
	{
		if(b && OverlayManager.editModeMutex)
		{
			return false;
		}
		
		OverlayManager.editModeMutex = b;
		return true;
	}
}
