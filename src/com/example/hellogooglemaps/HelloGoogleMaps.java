package com.example.hellogooglemaps;

import java.util.List;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;

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
	private static final String TAG = "AppCetera"; // Log-tag
	private static final int maxTouchDuration = 500;
	private static final int pointPixelTreshold = 10; // Maximaal verschil tussen 2 punten in pixels voor ze als gelijk worden beschouwd
	
	/**
	 * Mapoverlay subklasse
	 * @author Mathijs
	 *
	 */
	class MapOverlay extends com.google.android.maps.Overlay
    {
		HelloGoogleMaps instance = null;
		PolygonManager polygon;
		Long timer;
		boolean movingPoint = false;
		GeoPoint movingGeoPoint;
		
		/**
		 * Constructor
		 */
		public MapOverlay(HelloGoogleMaps hgm)
		{
			instance = hgm;
		}
		
		/**
		 * Constructor
		 */
		public MapOverlay(HelloGoogleMaps hgm, PolygonManager polygon)
		{
			instance = hgm;
			this.polygon = polygon;
		}
		
		/**
		 * Methode om de overlay te tekenen
		 */
        @Override
        public void draw(Canvas canvas, MapView mapView, boolean shadow) 
        {
            super.draw(canvas, mapView, shadow);                   
 
            // Hiermee stellen we een aantal paint-eigenschappen in,
            // zoals de kleur waarmee we verven, de dikte van de lijn en
            // de vorm van de uiteinden van de lijnen
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStrokeWidth(2);
            paint.setStrokeCap(Cap.ROUND);
            paint.setStyle(Style.FILL_AND_STROKE);

            if(polygon != null)
            {
            	Log.v(TAG, "Aantal polygoonpunten: " + polygon.getNumPoints());
            	Path path = new Path();
            	
            	if(polygon.getIsClosed())
            	{
            		Point screenPts = new Point();
	                mapView.getProjection().toPixels(polygon.getPreviousPoint(), screenPts);
	                path.moveTo(screenPts.x, screenPts.y);
            	}
            	
	            while(polygon.hasNextPoint())
	            {
	            	// 'vertaalt' een punt naar pixels op het scherm
	            	Point screenPts2 = new Point();
	            	if(!polygon.getIsClosed())
	            	{
		                mapView.getProjection().toPixels(polygon.getPreviousPoint(), screenPts2);
	            	}
	            	
	            	Point screenPts = new Point();
	                mapView.getProjection().toPixels(polygon.getNextPoint(), screenPts);
	                if(!polygon.getIsClosed())
	                {
	                	canvas.drawCircle(screenPts.x, screenPts.y, 5, paint);
	                	canvas.drawLine(screenPts2.x, screenPts2.y, screenPts.x, screenPts.y, paint);
	                }
	                else
	                {
	                	path.lineTo(screenPts.x, screenPts.y);
	                }
	            }
	            
	            if(polygon.getIsClosed())
	            {
	            	path.close();
	            	canvas.drawPath(path, paint);
	            }
	            polygon.reset();
            }
        }
        
        /**
         * Bij aanraken scherm
         */
        public boolean onTouchEvent(MotionEvent event, MapView mapView) 
        {   
        	// Wanneer de gebruiker zijn vinger op het touchscreen drukt
        	if(event.getAction() == MotionEvent.ACTION_DOWN)
        	{
        		// We zijn een punt aan het verplaatsen
            	/*if(movingPoint)
        		{
            		GeoPoint p = mapView.getProjection().fromPixels(
                			(int) event.getX(),
                            (int) event.getY());
        			Log.v(TAG, "Moving polygon point");
        			Log.v(TAG, polygon.editPoint(movingGeoPoint, p) ? "Success" : "Fail");
        			movingPoint = false;
        		}*/
            	
        		// We starten een tijdsmeting, omdat we alleen een touch willen registreren
        		// als deze korter duurt dan een bepaalde tijdsduur
        		timer = System.currentTimeMillis();
        		
        		// Checken of we hier toevallig op een al geplaatst punt touchen
        		/*if(polygon != null)
        		{
	        		polygon.reset();
	        		while(polygon.hasNextPoint())
	        		{
			            Point screenPts = new Point();
			            GeoPoint point = polygon.getNextPoint();
			            mapView.getProjection().toPixels(point, screenPts);
			            int divx, divy;
			            divx = Math.abs(screenPts.x-(int) event.getX());
			            divy = Math.abs(screenPts.y-(int) event.getY());
			            
			            if(divx < HelloGoogleMaps.pointPixelTreshold && divy < HelloGoogleMaps.pointPixelTreshold)
			            {
			            	movingPoint = true;
			            	movingGeoPoint = point;
			            	break;
			            }
	        		}
        		}*/
        	}
        	
            // Wanneer gebruiker zijn vinger optilt
            if (event.getAction() == MotionEvent.ACTION_UP)
            {
            	notifyTouchUp(event);
            }                            
            return false;
        }    
        
        /**
         * Listener voor het touch-up (gebruiker laat vinger los) event
         * @param event
         */
        public void notifyTouchUp(MotionEvent event)
        {
        	// Alleen een punt tekenen als de touch minder dan maxTouchDuration duurde
        	long diff = System.currentTimeMillis()-timer;
        	GeoPoint p = mapView.getProjection().fromPixels(
        			(int) event.getX(),
                    (int) event.getY());
        	
        	if(!movingPoint && diff < HelloGoogleMaps.maxTouchDuration)
        	{                
        		if(polygon != null)
        		{
	                // Check of dit punt ongeveer samenvalt met het eerste punt
	                // Indien ja, sluiten we de polygoon
		            polygon.reset();
		            Point screenPts = new Point();
		            GeoPoint point = polygon.getNextPoint();
		            mapView.getProjection().toPixels(point, screenPts);
		            int divx, divy;
		            divx = Math.abs(screenPts.x-(int) event.getX());
		            divy = Math.abs(screenPts.y-(int) event.getY());
		            
		            if(divx < HelloGoogleMaps.pointPixelTreshold && divy < HelloGoogleMaps.pointPixelTreshold)
		            {
		              	instance.notifyTouch(point);
		               	polygon.setIsClosed(true);
		            }
		            else
		            {
		               	instance.notifyTouch(p);
		            }
        		}
        		else
	            {
	               	instance.notifyTouch(p);
	            }
                Log.v(TAG, "Touchevent: " + p.getLatitudeE6() / 1E6 + "/" + p.getLongitudeE6() /1E6);
        	}
        }
    } 
	
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
        MapOverlay mapOverlay = new MapOverlay(this);
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
		MapOverlay mapOverlay = new MapOverlay(this, polygon);
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
/*package com.example.hellogooglemaps;

import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;

public class HelloGoogleMaps extends MapActivity implements LocationListener {
	
	private HelloMapView mapView;
	
    /** Called when the activity is first created. */
	/*@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.main);
	   
	    mapView = (HelloMapView) findViewById(R.id.mapview);
	    mapView.setBuiltInZoomControls(true);
	    mapView.setSatellite(true);
	    mapView.register(this);
	    
	    GeoPoint point2 = new GeoPoint(35410000, 139460000);
	    placeLittleGreenMan(point2, "Sekai, konichiwa!", "I'm in Japan!", false);
	    GeoPoint point = new GeoPoint(51824167,5867374);
	    placeLittleGreenMan(point, "Nuttige info", "Je hebt op een groen mannetje getapt.", true);
	    
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
		GeoPoint point = new GeoPoint((int) (location.getLatitude()*1E6), (int)(location.getLongitude()*1E6));
		placeLittleGreenMan(point, "Nuttige info", "Je hebt op een groen mannetje getapt.", true);
	}
	
	public void placeLittleGreenMan(GeoPoint point, String title, String content, boolean navigateToGreenMan) {
		Drawable drawable = this.getResources().getDrawable(R.drawable.androidmarker);
	    HelloItemizedOverlay itemizedoverlay = new HelloItemizedOverlay(drawable, this);
		List<Overlay> mapOverlays = mapView.getOverlays();
		OverlayItem overlayitem = new OverlayItem(point, title, content);
	    if (navigateToGreenMan) {
	    	MapController mapController = mapView.getController();
	        mapController.animateTo(point);
	        mapController.setZoom(18);
	    }
	    mapView.invalidate();
	    itemizedoverlay.addOverlay(overlayitem);
	    mapOverlays.add(itemizedoverlay);
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
//}