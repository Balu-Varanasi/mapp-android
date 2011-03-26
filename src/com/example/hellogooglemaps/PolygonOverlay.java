package com.example.hellogooglemaps;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;
import android.view.MotionEvent;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;

/**
 * Polygon-overlay voor op de Google Maps kaart
 * @author Mathijs
 *
 */
class PolygonOverlay extends com.google.android.maps.Overlay
{
	private HelloGoogleMaps instance = null;
	private PolygonManager polygon;
	private Long timer;
	private boolean movingPoint = false;
	private boolean polygonEditMode = false;
	private GeoPoint movingGeoPoint;
	private static final String TAG = HelloGoogleMaps.TAG;
	private MapView mapView;
	private Paint pointPaint;
	private Paint shapePaint;
	private Paint movingPointPaint;
	private Region pathRegion = null;
	
	/**
	 * Constructor
	 * @param instantie van HelloGoogleMaps
	 * @param instantie van de polygonmanager
	 * @param instantie van de mapview
	 */
	public PolygonOverlay(HelloGoogleMaps hgm, PolygonManager polygon, MapView mv, int color)
	{
		this.instance = hgm;
		this.polygon  = polygon;
		this.mapView  = mv;
		
		// Hiermee stellen we een aantal paint-eigenschappen in,
        // zoals de kleur waarmee we verven, de dikte van de lijn en
        // de vorm van de uiteinden van de lijnen
		this.pointPaint = new Paint();
		this.pointPaint.setColor(color);
		this.pointPaint.setStrokeWidth(2);
		this.pointPaint.setStrokeCap(Cap.ROUND);
		
		this.movingPointPaint = new Paint();
		this.movingPointPaint.setColor(Color.GRAY);
		this.movingPointPaint.setStrokeWidth(2);
		this.movingPointPaint.setStrokeCap(Cap.ROUND);
		
		this.shapePaint = new Paint();
		this.shapePaint.setColor(color);
		this.shapePaint.setStrokeWidth(2);
		this.shapePaint.setStrokeCap(Cap.ROUND);
		this.shapePaint.setStyle(Style.FILL_AND_STROKE);
		this.shapePaint.setAlpha(50);
		this.shapePaint.setAntiAlias(true);
	}
	
	/**
	 * Constructor
	 * @param hgm
	 * @param polygon
	 * @param mv
	 */
	public PolygonOverlay(HelloGoogleMaps hgm, PolygonManager polygon, MapView mv)
	{
		this(hgm, polygon, mv, Color.RED);
	}	
		
	/**
	 * Methode om de overlay te tekenen
	 * @param canvas
	 * @param mapView
	 * @param shadow
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
           	if(!polygon.getIsClosed() || this.polygonEditMode)
           	{
	            mapView.getProjection().toPixels(polygon.getPreviousPoint(), screenPts2);
           	}
           	
           	GeoPoint next = polygon.getNextPoint();
          	Point screenPts = new Point();
            mapView.getProjection().toPixels(next, screenPts);
            if(!polygon.getIsClosed() || this.polygonEditMode)
            {
            	canvas.drawLine(screenPts2.x, screenPts2.y, screenPts.x, screenPts.y, this.pointPaint);
            	
            	// Ander kleurtje geven als we deze aan het verplaatsen zijn
            	if(this.movingGeoPoint != null && next.equals(this.movingGeoPoint) && this.movingPoint)
            	{
            		canvas.drawCircle(screenPts.x, screenPts.y, 8, this.movingPointPaint);
            	}
            	else
            	{
            		canvas.drawCircle(screenPts.x, screenPts.y, 8, this.pointPaint);
            	}
            }
            
            if(polygon.getIsClosed())
            {
               	path.lineTo(screenPts.x, screenPts.y);
            }
        }
            
        // De polygoon is gesloten, vul hem op
        if(polygon.getIsClosed())
        {
           	path.close();
           	canvas.drawPath(path, this.shapePaint);
           	
           	RectF rectF = new RectF();
           	path.computeBounds(rectF, true);
           	pathRegion = new Region();
           	pathRegion.setPath(path, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
        }
        polygon.reset();
    }
    
    /**
     * Bij aanraken scherm
     * @param event
     * @param mapView
     * @return boolean
     */
    public boolean onTouchEvent(MotionEvent event, MapView mapView) 
    {   
    	// Wanneer de gebruiker zijn vinger op het touchscreen drukt
    	if(event.getAction() == MotionEvent.ACTION_DOWN)
    	{        	
    		return notifyTouchDown(event);
    	}
    	
    	if(event.getAction() == MotionEvent.ACTION_MOVE)
    	{
    		return notifyTouchMove(event);
    	}
    	
        // Wanneer gebruiker zijn vinger optilt
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
        	return notifyTouchUp(event);
        }       
        
        return false;
    }    
    
    /**
     * Listener voor het touch-up (gebruiker laat vinger los) event
     * @param event
     * @return boolean
     */
    public boolean notifyTouchUp(MotionEvent event)
    {
    	if(this.movingPoint)
    	{
    		movingPoint = false;
    	}
    	else if(this.polygonEditMode)
    	{
    		
    	}
    	else
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
    	
    	return false;
    }
    
    /**
     * Listener voor touch-down event (gebruiker raakt scherm aan)
     * @param event
     * @return boolean
     */
    public boolean notifyTouchDown(MotionEvent event)
    {    	
    	// We starten een tijdsmeting, omdat we alleen een touch willen registreren
		// als deze korter duurt dan een bepaalde tijdsduur
		timer = System.currentTimeMillis();
		
		// Testen of we in een polygoon getapped hebben
		if(this.pathRegion != null && !this.polygonEditMode)
		{
	    	if(this.pathRegion.contains((int) event.getX(), (int) event.getY()))
	    	{
	    		this.polygonEditMode = true;
	    		return false;
	    	}
		}
		
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
	           
	        if(divx < HelloGoogleMaps.pointPixelTreshold 
	        		&& divy < HelloGoogleMaps.pointPixelTreshold)
	        {
	        	if(point.equals(polygon.getFirstPoint()) && !polygon.getIsClosed())
	        	{
	        		return false;
	        	}
	        	
	           	movingPoint = true;
	           	movingGeoPoint = point;
	           	return true;
	        }
    	}
    	
    	// Als we ergens klikken, in editmode zijn, en geen punt hebben aangetapt,
    	// dan schakelen we de editmode weer uit.
    	if(this.pathRegion != null && this.polygonEditMode && !this.movingPoint)
		{
			this.polygonEditMode = false;
		}
    	
    	return false;
    }
    
    /**
     * Listener voor move event (gebruiker verplaatst vinger)
     * @param event
     * @return boolean
     */
    public boolean notifyTouchMove(MotionEvent event)
    {
    	// We zijn een punt aan het verplaatsen
    	if(this.movingPoint)
		{
    		GeoPoint p = mapView.getProjection().fromPixels(
        			(int) event.getX(),
                    (int) event.getY());
			Log.v(TAG, "Moving polygon point");
			Log.v(TAG, polygon.editPoint(movingGeoPoint, p) ? "Success" : "Fail");
			movingGeoPoint = p;
			return true;
		}
    	
    	return false;
    }
} 