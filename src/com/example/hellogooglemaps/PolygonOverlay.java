package com.example.hellogooglemaps;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

class PolygonOverlay extends com.google.android.maps.Overlay
{
	private HelloGoogleMaps instance = null;
	private PolygonManager polygon;
	private Long timer;
	private boolean movingPoint = false;
	private GeoPoint movingGeoPoint;
	private static final String TAG = HelloGoogleMaps.TAG;
	private MapView mapView;
	private Paint pointPaint;
	private Paint shapePaint;
	
	/**
	 * Constructor
	 */
	public PolygonOverlay(HelloGoogleMaps hgm, PolygonManager polygon, MapView mv)
	{
		this.instance = hgm;
		this.polygon  = polygon;
		this.mapView  = mv;
		
		// Hiermee stellen we een aantal paint-eigenschappen in,
        // zoals de kleur waarmee we verven, de dikte van de lijn en
        // de vorm van de uiteinden van de lijnen
		this.pointPaint = new Paint();
		this.pointPaint.setColor(Color.RED);
		this.pointPaint.setStrokeWidth(2);
		this.pointPaint.setStrokeCap(Cap.ROUND);
		
		this.shapePaint = new Paint();
		this.shapePaint.setColor(Color.RED);
		this.shapePaint.setStrokeWidth(2);
		this.shapePaint.setStrokeCap(Cap.ROUND);
		this.shapePaint.setStyle(Style.FILL_AND_STROKE);
		this.shapePaint.setAlpha(50);
		this.shapePaint.setAntiAlias(true);
	}
	
	/**
	 * Methode om de overlay te tekenen
	 */
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) 
    {
        super.draw(canvas, mapView, shadow);                   

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
              	canvas.drawCircle(screenPts.x, screenPts.y, 5, this.pointPaint);
               	canvas.drawLine(screenPts2.x, screenPts2.y, screenPts.x, screenPts.y, this.pointPaint);
            }
            else
            {
               	path.lineTo(screenPts.x, screenPts.y);
            }
        }
            
        if(polygon.getIsClosed())
        {
           	path.close();
           	canvas.drawPath(path, this.shapePaint);
        }
        polygon.reset();
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
    	}
    	
    	if(event.getAction() == MotionEvent.ACTION_MOVE)
    	{
    		// We zijn een punt aan het verplaatsen
        	if(movingPoint)
    		{
        		GeoPoint p = mapView.getProjection().fromPixels(
            			(int) event.getX(),
                        (int) event.getY());
    			Log.v(TAG, "Moving polygon point");
    			Log.v(TAG, polygon.editPoint(movingGeoPoint, p) ? "Success" : "Fail");
    			movingPoint = false;
    		}
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
            // Check of dit punt ongeveer samenvalt met het eerste punt
            // Indien ja, sluiten we de polygoon
	        polygon.reset();
	        if(polygon.hasNextPoint())
	        {
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