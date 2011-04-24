package nl.appcetera.mapp;

import java.util.List;

import android.content.SharedPreferences;
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
	private OverlayManager om;
	
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

        // Databaseklasse opstarten
        database = new PolygonData(this);

        // Opgeslagen overlays laden
        om = new OverlayManager(mapView, database, this);
        om.loadOverlays();
        
        mapView.invalidate();
    }
	
	/**
	 * Wanneer de app gekilled wordt
	 */
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		database.close();
	}
	
	/**
	 * De applicatie gaat weer verder
	 */
	@Override
	public void onResume()
	{
		super.onResume();
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		mapController = mapView.getController();
	    point = new GeoPoint(settings.getInt("pos_lat", 51824167),
	    		settings.getInt("pos_long", 5867374));
        mapController.setZoom(settings.getInt("zoomlevel", 10));
        mapController.animateTo(point);
	}
	
	/**
	 * Er komt een andere app overheen, deze wordt gepauzeerd
	 */
	@Override
	public void onPause()
	{
		super.onPause();
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("zoomlevel", mapView.getZoomLevel());
		editor.putInt("pos_long", mapView.getMapCenter().getLongitudeE6());
		editor.putInt("pos_lat", mapView.getMapCenter().getLatitudeE6());
		editor.commit();
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
		PolygonOverlay po = Mapp.instance.om.addOverlay();
		
		if(po != null)
		{
			// Geef het touchevent door, zodat we gelijk een nieuw punt kunnen maken
			event.setAction(MotionEvent.ACTION_DOWN);
			po.onTouchEvent(event, Mapp.instance.mapView);
			event.setAction(MotionEvent.ACTION_UP);
			po.onTouchEvent(event, Mapp.instance.mapView);
			
			Log.v(TAG, "Adding new layer");
		}
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