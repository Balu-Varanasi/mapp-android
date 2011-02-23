package com.example.hellogooglemaps;

import java.util.List;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class HelloGoogleMaps extends MapActivity {
    /** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	    MapView mapView = (MapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    mapView.setSatellite(true);
	    
	    List<Overlay> mapOverlays = mapView.getOverlays();
	    Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
	    HelloItemizedOverlay itemizedoverlay = new HelloItemizedOverlay(drawable, this);
	    
	    GeoPoint point = new GeoPoint(51824167,5867374);
	    OverlayItem overlayitem = new OverlayItem(point, "Nuttige info", "Je hebt op een groen mannetje getapt.");
	    MapController mapController = mapView.getController();
	    mapController.animateTo(point);
	    mapController.setZoom(18);
	    mapView.invalidate();
	    
	    itemizedoverlay.addOverlay(overlayitem);
	    mapOverlays.add(itemizedoverlay);
	}
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }
}