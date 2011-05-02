package nl.appcetera.mapp;

import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

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
	private MetaPopupManager metaPopupManager;
	private ServerSync s;
	public static final int pointPixelTreshold = 15; // Maximaal verschil tussen 2 punten in pixels voor ze als gelijk worden beschouwd
	public static final String TAG = "AppCetera"; // Log-tag
	public static final int maxTouchDuration = 500;
	public static final int polygonMinDisplayWidth = 5; // Wanneer een polygoon smaller is dan dit wordt ie niet getoond
	private static final int METAPOPUP_ID = 0;
	
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
        om = new OverlayManager(mapView, database, this);
        om.setGroup(0);
        om.loadOverlays();
        
        // Syncservice starten
        s = new ServerSync(getApplicationContext());
    	s.startSync();
        
        mapView.invalidate();
        
        showMetaPopup();

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
        om.setGroup(settings.getInt("group", 0));
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

	public boolean displayingMetaPopup() {
		return metaPopupManager.isVisible();
	}

	public void hideMetaPopup() {
		dismissDialog(METAPOPUP_ID);
	}

	public void showMetaPopup() {
		showDialog(METAPOPUP_ID);
	}
	
	protected Dialog onCreateDialog(int id) {
	    Dialog dialog = null;
	    Toast.makeText(getApplicationContext(), "attempt show dialog", Toast.LENGTH_LONG).show();
	    switch(id) {
	    case METAPOPUP_ID:
	    	AlertDialog.Builder builder;
	    	AlertDialog alertDialog;

	    	Context mContext = getApplicationContext();
	    	LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
	    	View layout = inflater.inflate(R.layout.metapopup,
	    	                               (ViewGroup) findViewById(R.id.LinearLayout01));

	    	ImageView image = (ImageView) layout.findViewById(R.id.ImageView01);
	    	image.setImageResource(R.drawable.androidmarker);

	    	builder = new AlertDialog.Builder(mContext);
	    	builder.setView(layout);
	    	alertDialog = builder.create();
	        break;
	    }
	    return dialog;
	}
}