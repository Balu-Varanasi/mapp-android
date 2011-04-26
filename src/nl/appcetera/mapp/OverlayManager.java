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
 *
 */
public class OverlayManager
{
	private MapView mv;
	private PolygonData db;
	private Mapp activity;
	
	/**
	 * Constructor
	 * @param mv MapView instance
	 * @param db PolygonData instance
	 */
	public OverlayManager(MapView mv, PolygonData db, Mapp activity)
	{
		this.mv = mv;
		this.db = db;
		this.activity = activity;
	}
	
	/**
	 * Laadt alle polygonen uit de database en plaatst deze in overlays
	 */
	public void loadOverlays()
	{
		Cursor c = db.getAllPolygons(activity);
		
		// Er zijn geen polygonen opgeslagen
		if(!c.moveToFirst())
		{
			/*// Niewe laag + polygoon toevoegen
			PolygonOverlay mapOverlay = new PolygonOverlay(mv);
	        List<Overlay> listOfOverlays = mv.getOverlays();
	        listOfOverlays.clear();
	        
	        // Nieuwe polygoon in database stoppen en het id aan de polygoon geven
	        int id = db.addPolygon(mapOverlay.getManager().getColor(), 
	        		mapOverlay.getManager().getIsClosed());
	        mapOverlay.getManager().setId(id);
	        mapOverlay.getManager().setColor(color)
	        
	        listOfOverlays.add(mapOverlay);*/
			addOverlay();
		}
		else
		{
			List<Overlay> listOfOverlays = mv.getOverlays();
	        listOfOverlays.clear();
	        
			do
			{
				// Niewe laag + polygoon toevoegen
				PolygonOverlay mapOverlay = new PolygonOverlay(mv, c.getInt(1));
				
		        // De polygonmanager goed instellen
		        PolygonManager pm = mapOverlay.getManager();
		        pm.setDbEnable(false);
		        pm.setId(c.getInt(0));
				pm.setColor(c.getInt(1));
		        boolean isClosed = c.getInt(2) != 0;
		        
		        // Punten ophalen en toevoegen aan de manager
		        Cursor c2 = db.getAllPolygonPoints(activity, pm.getId());
		        
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
		        pm.setDbEnable(true);
		        listOfOverlays.add(mapOverlay);
			}
			while(c.moveToNext());
		}
	}
	
	/**
	 * Voegt een overlay toe aan de lijst van overlays
	 * @return de nieuw gemaakte overlay, of null indien er geen werd gemaakt
	 */
	public PolygonOverlay addOverlay()
	{
		List<Overlay> listOfOverlays = mv.getOverlays();
		
		// Check of de polygonen uit de andere lagen wel gesloten zijn
		if(listOfOverlays.size() > 0)
		{
			for(int i = 0; i < listOfOverlays.size(); i++)
			{
				PolygonOverlay p = (PolygonOverlay) listOfOverlays.get(i);
				PolygonManager pm = p.getManager();
				if(!pm.getIsClosed() || p.getIsEditMode())
				{
					// Nee dus, geen nieuwe laag maken
					return null;
				}
			}
		}
		
		// Maak een willekeurige kleur
		Random r = new Random();
		int color = Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256));
		// Maak een nieuwe overlay
		PolygonOverlay po = new PolygonOverlay(mv, color);

		int id = db.addPolygon(color, po.getManager().getIsClosed());
        po.getManager().setId(id);
        po.getManager().setColor(color);
        
        listOfOverlays.add(po);
        
        return po;
	}
}
