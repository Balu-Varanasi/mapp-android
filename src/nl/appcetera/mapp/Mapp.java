package nl.appcetera.mapp;

import java.util.List;

import android.content.SharedPreferences;
import android.os.Bundle;
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
 * @author Joost
 * @group AppCetera
 */
public class Mapp extends MapActivity
{
	private MapView mapView;
	private MapController mapController;
	private GeoPoint point;
	private PolygonData database;
	private OverlayManager om;
	private static MetaPopupOverlay metaPopupOverlay;
	private ServerSync s;
	public static final int pointPixelTreshold = 25; // Maximaal verschil tussen 2 punten in pixels voor ze als gelijk worden beschouwd
	public static final String TAG = "AppCetera"; // Log-tag
	public static final int maxTouchDuration = 500;
	public static final int polygonMinDisplayWidth = 5; // Wanneer een polygoon smaller is dan dit wordt ie niet getoond
	public static final int syncInterval = 60*1000; // Interval tussen synchronisaties in milliseconden
	public static final int offlineRetryInterval = 30*60*1000; // Interval tussen sync-attempts als toestel offline is
	
	public static Mapp instance;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 */
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
		// Constructor van de parent aanroepen
        super.onCreate(savedInstanceState);
        
        // Juiste layout (mapview) zetten
        setContentView(R.layout.main);
        
        // Instantie van deze klasse beschikbaar maken met een statische variabele
        Mapp.instance = this;
        
        // Mapview dingetjes
        mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    mapView.setSatellite(true);

        // Databaseklasse opstarten
        database = new PolygonData(this);

        // Opgeslagen overlays laden
        om = new OverlayManager(mapView, database);
        
        // Syncservice starten
        s = new ServerSync(getApplicationContext());
        
        //MetaPopupManager maken
        metaPopupOverlay = new MetaPopupOverlay(mapView, getApplicationContext(), this);
        mapView.getOverlays().add(metaPopupOverlay);
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
		s.stopSync();
	}
	
	/**
	 * De applicatie gaat weer verder
	 */
	@Override
	public void onResume()
	{
		super.onResume();
		
		// Settings ophalen
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		
		// Naar de juiste plaats op de kaart gaan
		mapController = mapView.getController();
	    point = new GeoPoint(settings.getInt("pos_lat", 51824167),
	    		settings.getInt("pos_long", 5867374));
        mapController.setZoom(settings.getInt("zoomlevel", 10));
        mapController.animateTo(point);
        
        // Database opstarten
        database = new PolygonData(this);
        
        // Syncservice hervatten
        s.startSync();
        
        // Juiste groep ophalen en polygonen laden
        //om.setGroup(settings.getInt("group", 1));
        om.setGroup(1);
        om.loadOverlays();
	}
	
	/**
	 * Er komt een andere app overheen, deze wordt gepauzeerd
	 */
	@Override
	public void onPause()
	{
		super.onPause();
		
		// Settings opslaan
		SharedPreferences settings = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("zoomlevel", mapView.getZoomLevel());
		editor.putInt("pos_long", mapView.getMapCenter().getLongitudeE6());
		editor.putInt("pos_lat", mapView.getMapCenter().getLatitudeE6());
		editor.putInt("group", 0);
		editor.commit();
		
		// Database afsluiten
		database.close();
		
		// Syncservice stoppen
		s.stopSync();
		
		OverlayManager.editModeMutex(false);
	}
	
	/**
	 * Verplaatst een overlay naar de bovenste laag
	 * @param po de overlay om naar boven te verplaatsen
	 */
	public static void moveToFront(PolygonOverlay po)
	{
		List<Overlay> listOfOverlays = Mapp.instance.mapView.getOverlays();
		listOfOverlays.remove(metaPopupOverlay);
		listOfOverlays.remove(po);
		listOfOverlays.add(po);
		listOfOverlays.add(metaPopupOverlay);
	}
	
	/**
	 * Controleert of de gegeven overlay de eerste (=onderste) overlay is
	 * @param po de te checken overlay
	 * @return true indien gegeven overlay de onderste laag is
	 */
	public static boolean isFirstOverlay(PolygonOverlay po)
	{
		List<Overlay> listOfOverlays = Mapp.instance.mapView.getOverlays();
		return (listOfOverlays.get(0).equals(po)) 
			|| (listOfOverlays.get(0).equals(metaPopupOverlay) 
					&& listOfOverlays.get(1).equals(po));
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

	/**
	 * Bekijkt of we op dit moment een metapopup aan het tonen zijn
	 * @return de visible-state van de metapopup
	 */
	public boolean displayingMetaPopup() {
		return metaPopupOverlay.isVisible();
	}

	/**
	 * Verbergt de metapopup
	 */
	public void hideMetaPopup() {
		metaPopupOverlay.makeInvisible();
	}

	/**
	 * Toont de metapopup
	 */
	public void showMetaPopup(int x, int y) {
		metaPopupOverlay.makeVisible(x, y);
	}
}