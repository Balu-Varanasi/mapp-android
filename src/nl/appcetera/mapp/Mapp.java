package nl.appcetera.mapp;

import java.util.List;
import java.util.Random;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

import nl.appcetera.mapp.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * Mapp main activity
 * @author Mathijs
 * @group AppCetera
 */
public class Mapp extends MapActivity
{
	private MapView mapView;
	private MapController mapController;
	private GeoPoint point;
	private PolygonData database;
	
	public static final int pointPixelTreshold = 15; // Maximaal verschil tussen 2 punten in pixels voor ze als gelijk worden beschouwd
	public static final String TAG = "AppCetera"; // Log-tag
	public static final int maxTouchDuration = 500;
	
	public static Mapp instance;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 */
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Mapp.instance = this;
        
        mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    mapView.setSatellite(true);
	    mapController = mapView.getController();
	    point = new GeoPoint(51824167,5867374);
        mapController.animateTo(point);
        mapController.setZoom(18);
        
        database = new PolygonData(this);
        
        // Overlay toevoegen
        PolygonOverlay mapOverlay = new PolygonOverlay(mapView);
        List<Overlay> listOfOverlays = mapView.getOverlays();
        listOfOverlays.clear();
        listOfOverlays.add(mapOverlay);
        
	    mapView.invalidate();
    }
	
	/**
	 * Verplaatst een overlay naar de bovenste laag
	 * @param po de overlay om naar boven te verplaatsen
	 */
	public static void moveToFront(PolygonOverlay po)
	{
		List<Overlay> listOfOverlays = Mapp.instance.mapView.getOverlays();
		listOfOverlays.remove(po);
		listOfOverlays.add(po);
	}
	
	/**
	 * Controleert of de gegeven overlay de eerste (=onderste) overlay is
	 * @param po de te checken overlay
	 * @return true indien gegeven overlay de onderste laag is
	 */
	public static boolean isFirstOverlay(PolygonOverlay po)
	{
		List<Overlay> listOfOverlays = Mapp.instance.mapView.getOverlays();
		return listOfOverlays.get(0).equals(po);
	}
	
	/**
	 * Voegt een nieuwe overlay toe
	 * @param het event dat doorgegeven zal worden aan de nieuwe laag
	 */
	public static void addNewOverlay(MotionEvent event)
	{
		List<Overlay> listOfOverlays = Mapp.instance.mapView.getOverlays();
		
		// Check of de polygonen uit de andere lagen wel gesloten zijn
		for(int i = 0; i < listOfOverlays.size(); i++)
		{
			PolygonOverlay p = (PolygonOverlay) listOfOverlays.get(i);
			PolygonManager pm = p.getManager();
			if(!pm.getIsClosed() || p.getIsEditMode())
			{
				// Nee dus, geen nieuwe laag maken
				return;
			}
		}
		
		// Maak een willekeurige kleur
		Random r = new Random();
		int color = Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256));
		// Maak een nieuwe overlay
		PolygonOverlay po = new PolygonOverlay(Mapp.instance.mapView, color);
		// Geef het touchevent door, zodat we gelijk een nieuw punt kunnen maken
		event.setAction(MotionEvent.ACTION_DOWN);
		po.onTouchEvent(event, Mapp.instance.mapView);
		event.setAction(MotionEvent.ACTION_UP);
		po.onTouchEvent(event, Mapp.instance.mapView);
		listOfOverlays.add(po);
		
		Log.v(TAG, "Adding new layer");
	}
	
	/**
	 * Meuk
	 */
	@Override
	protected boolean isRouteDisplayed()
	{
		return false;
	}
	
	/**
	 * Geeft een instantie van de databasemanager terug
	 * @return PolygonData
	 */
	public static PolygonData getDatabase()
	{
		return Mapp.instance.database;
	}
}