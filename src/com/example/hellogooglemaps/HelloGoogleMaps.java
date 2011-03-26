package com.example.hellogooglemaps;

import java.util.List;

import android.os.Bundle;

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
	private PolygonManager polygon = new PolygonManager();
	
	public static final int pointPixelTreshold = 10; // Maximaal verschil tussen 2 punten in pixels voor ze als gelijk worden beschouwd
	public static final String TAG = "AppCetera"; // Log-tag
	public static final int maxTouchDuration = 500;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 */
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    mapView.setSatellite(true);
	    mapController = mapView.getController();
	    point = new GeoPoint(51824167,5867374);
        mapController.animateTo(point);
        mapController.setZoom(18);
        
        // Overlay toevoegen
        PolygonOverlay mapOverlay = new PolygonOverlay(this, polygon, mapView);
        List<Overlay> listOfOverlays = mapView.getOverlays();
        listOfOverlays.clear();
        listOfOverlays.add(mapOverlay);
        
	    mapView.invalidate();
    }
	
	/**
	 * Wanneer het scherm is aangeraakt ontvangen we daar hier een bericht van
	 * @param het aangeraakte punt, vertaald naar mapcoördinaten
	 */
	public void notifyTouch(GeoPoint p)
	{
		polygon.addPoint(p);
		PolygonOverlay mapOverlay = new PolygonOverlay(this, polygon, mapView);
        List<Overlay> listOfOverlays = mapView.getOverlays();
        listOfOverlays.clear();
        listOfOverlays.add(mapOverlay);
        
	    mapView.invalidate();
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