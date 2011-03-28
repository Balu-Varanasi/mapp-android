package com.example.hellogooglemaps;

import java.util.List;
import java.util.Random;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * Polygoon-testklasse
 * @author Mathijs
 * @group AppCetera
 */
public class HelloGoogleMaps extends MapActivity
{
	private MapView mapView;
	private MapController mapController;
	private GeoPoint point;
	
	public static final int pointPixelTreshold = 15; // Maximaal verschil tussen 2 punten in pixels voor ze als gelijk worden beschouwd
	public static final String TAG = "AppCetera"; // Log-tag
	public static final int maxTouchDuration = 500;
	
	public static HelloGoogleMaps instance;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 */
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        HelloGoogleMaps.instance = this;
        
        mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    mapView.setSatellite(true);
	    mapController = mapView.getController();
	    point = new GeoPoint(51824167,5867374);
        mapController.animateTo(point);
        mapController.setZoom(18);
        
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
		List<Overlay> listOfOverlays = HelloGoogleMaps.instance.mapView.getOverlays();
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
		List<Overlay> listOfOverlays = HelloGoogleMaps.instance.mapView.getOverlays();
		return listOfOverlays.get(0).equals(po);
	}
	
	/**
	 * Voegt een nieuwe overlay toe
	 */
	public static void addNewOverlay()
	{
		// Maak een willekeurige kleur
		Random r = new Random();
		int color = Color.rgb(r.nextInt(256), r.nextInt(256), r.nextInt(256));
		// Maak een nieuwe overlay
		List<Overlay> listOfOverlays = HelloGoogleMaps.instance.mapView.getOverlays();
		listOfOverlays.add(new PolygonOverlay(HelloGoogleMaps.instance.mapView, color));
		
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
	
}