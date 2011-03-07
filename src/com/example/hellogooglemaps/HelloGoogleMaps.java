package com.example.hellogooglemaps;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class HelloGoogleMaps extends MapActivity implements LocationListener {
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
	    
	    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);    
    
	    //MyLocationListener locationListener = new MyLocationListener();
    
	    lm.requestLocationUpdates(
	        LocationManager.GPS_PROVIDER, 
	        0, 
	        0, 
	        this);    
	}
    
    @Override
    protected boolean isRouteDisplayed() {
        return false;
    }

	public void onLocationChanged(Location location) {
		Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
	    HelloItemizedOverlay itemizedoverlay = new HelloItemizedOverlay(drawable, this);
		MapView mapView = (MapView) findViewById(R.id.mapview);
		List<Overlay> mapOverlays = mapView.getOverlays();
		
		GeoPoint point = new GeoPoint((int) (location.getLatitude()*1E6), (int)(location.getLongitude()*1E6));
	    OverlayItem overlayitem = new OverlayItem(point, "Nuttige info", "Je hebt op een groen mannetje getapt.");
	    MapController mapController = mapView.getController();
	    mapController.animateTo(point);
	    mapController.setZoom(18);
	    mapView.invalidate();
	    
	    itemizedoverlay.addOverlay(overlayitem);
	    mapOverlays.add(itemizedoverlay);
		
	}

	public void test(int a) {
		int test = a;
	}
	
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}
    
   /* private class MyLocationListener implements LocationListener 
    {
        public void onLocationChanged(Location loc) {
            if (loc != null) {
            	
                Toast.makeText(getBaseContext(), 
                    "Location changed : Lat: " + loc.getLatitude() + 
                    " Lng: " + loc.getLongitude(), 
                    Toast.LENGTH_SHORT).show();
            }
        }

        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }

        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        public void onStatusChanged(String provider, int status, 
            Bundle extras) {
            // TODO Auto-generated method stub
        }
    }  */      
}