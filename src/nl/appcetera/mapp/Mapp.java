package nl.appcetera.mapp;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
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
	private ServerSync s;
	public SharedPreferences settings;
	public static final int pointPixelTreshold = 25; // Maximaal verschil tussen 2 punten in pixels voor ze als gelijk worden beschouwd
	public static final String TAG = "AppCetera"; // Log-tag
	public static final int maxTouchDuration = 500;
	public static final int polygonMinDisplayWidth = 5; // Wanneer een polygoon smaller is dan dit wordt ie niet getoond
	public static final int syncInterval = 60*1000; // Interval tussen synchronisaties in milliseconden
	public static final int offlineRetryInterval = 30*60*1000; // Interval tussen sync-attempts als toestel offline is
	public static final int metaTouchDuration = 1000; //touch-duration waarna we naar de meta-activity gaan
	public static final int META_EDITSCREEN_ACTIVITYCODE = 42;
	private static final int MENU1 = Menu.FIRST;
	public static Mapp instance;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 * @param savedInstanceState de bundle die de activity meekrijgt wanneer hij wordt gemaakt
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
        s = new ServerSync(getApplicationContext(), database);
        
        mapView.invalidate();
        
		// Settings ophalen
        settings = getPreferences(MODE_PRIVATE);
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
		return (listOfOverlays.get(0).equals(po));
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
	 * MapActivities moeten deze functie verplicht overriden
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
	 * Herlaad overlays
	 */
	public static void reload()
	{
		Mapp.instance.om.loadOverlays();
	}
	
	/**
	 * Deze functie wordt aangeroepen wanneer een Activity die vanuit Mapp is aangeroepen zn setResult aanroept
	 * @param requestCode een int die aangeeft om welke Activity het gaat
	 * @param resultCode een int die de status van terminatie van de Activity aangeeft
	 * @param data een intent die eventuele result-data bevat
	 */
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if(data == null)
		{
			return;
		}
		
		Bundle bundle = data.getExtras();

		switch(requestCode) {
			case META_EDITSCREEN_ACTIVITYCODE:
				if (resultCode == MetaEditScreen.RESULT_SAVE)
				{
					int id = bundle.getInt(MetaEditScreen.ID_KEY);
					int color = bundle.getInt(MetaEditScreen.COLOR_KEY);
					String name = bundle.getString(MetaEditScreen.NAME_KEY);
					String description = bundle.getString(MetaEditScreen.DESCRIPTION_KEY);
					database.editPolygon(id, color, true, name, description);
				}
				else if (resultCode == MetaEditScreen.RESULT_DELETE)
				{
					int id = bundle.getInt(MetaEditScreen.ID_KEY);
					database.removePolygon(id, true);
				}
				break;
		}	
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mainmenu, menu);
	    return true;
	}
}
