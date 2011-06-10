package nl.appcetera.mapp;

import java.util.Timer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Handler;
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
	private PolygonManager polygon;
	private Long timer = (long) 0;
	private Handler metaHandler = new Handler();
	private boolean movingPoint = false;
	private int movingPointId = 0;
	private boolean polygonEditMode = false;
	private GeoPoint movingGeoPoint;
	private MapView mapView;
	private Paint pointPaint;
	private Paint shapePaint;
	private Paint movingPointPaint;
	private Paint shapeLinePaint;
	private Region pathRegion = null;
	private boolean eventConsumed = false;
	//Runnable om de Handler van de timer iets te doen te geven
	private Runnable editMetaCallback = new Runnable() {
		public void run() {
			polygon.editMetaData(Mapp.instance);
	   }
	};
	
	/**
	 * Constructor
	 * @param instantie van de mapview
	 */
	public PolygonOverlay(MapView mv)
	{
		this(mv, Color.RED);
	}
	
	/**
	 * Constructor
	 * @param hgm
	 * @param polygon
	 * @param mv
	 */
	public PolygonOverlay(MapView mv, int color)
	{
		this.polygon  = new PolygonManager();
		this.mapView  = mv;
		
		// Hiermee stellen we een aantal paint-eigenschappen in,
        // zoals de kleur waarmee we verven, de dikte van de lijn en
        // de vorm van de uiteinden van de lijnen
		
		// Voor het tekenen van een hoekpunt/lijn van een polygoon
		this.pointPaint = new Paint();
		this.pointPaint.setColor(color);
		this.pointPaint.setStrokeWidth(2);
		this.pointPaint.setStrokeCap(Cap.ROUND);
		
		// Voor het tekenen van een hoekpunt van een polygoon dat verplaatst wordt
		this.movingPointPaint = new Paint();
		this.movingPointPaint.setColor(Color.GRAY);
		this.movingPointPaint.setStrokeWidth(2);
		this.movingPointPaint.setStrokeCap(Cap.ROUND);
		
		// Voor het tekenen van een hele polygoon
		this.shapePaint = new Paint();
		this.shapePaint.setColor(color);
		this.shapePaint.setStyle(Style.FILL);
		this.shapePaint.setAlpha(75);
		this.shapePaint.setAntiAlias(true);
		
		// Voor het tekenen van de lijnen om een polygoon heen
		this.shapeLinePaint = new Paint();
		this.shapeLinePaint.setColor(color);
		this.shapeLinePaint.setStrokeWidth(3);
		this.shapeLinePaint.setStrokeCap(Cap.ROUND);
		this.shapeLinePaint.setAlpha(150);
		this.shapeLinePaint.setAntiAlias(true);
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

        Path path = new Path();
        	
        if(polygon.getIsClosed())
        {
        	Point screenPts = new Point();
            mapView.getProjection().toPixels(polygon.getFirstPoint(), screenPts);
            path.moveTo(screenPts.x, screenPts.y);
        }
        
        for (int i = 0; i < polygon.getNumPoints(); i++)
        { 
          	// 'vertaalt' een punt naar pixels op het scherm
           	Point screenPts2 = new Point();
           	//if(!polygon.getIsClosed() || this.polygonEditMode)
           	//{
	            mapView.getProjection().toPixels(polygon.getPoint(i), screenPts2);
           	//}
           	
           	GeoPoint next = polygon.getPoint(i+1);
          	Point screenPts = new Point();
            mapView.getProjection().toPixels(next, screenPts);
            if(!polygon.getIsClosed() || this.polygonEditMode)
            {
            	if (i < polygon.getNumPoints() - 1 || polygon.getIsClosed()) {
            		canvas.drawLine(screenPts2.x, screenPts2.y, screenPts.x, screenPts.y, this.pointPaint);
            	}
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
               	// Toch maar een lijntje om de gesloten polygoon heen tekenen,
               	// omdat Joël dat leuk vindt
               	if (i < polygon.getNumPoints() && !this.polygonEditMode)
               	{
               		canvas.drawLine(screenPts2.x, screenPts2.y, screenPts.x, screenPts.y, this.shapeLinePaint);
               	}
            }
        }
        
            
        // De polygoon is gesloten, vul hem op
        if(polygon.getIsClosed())
        {
        	Point screenPts = new Point();
            mapView.getProjection().toPixels(polygon.getFirstPoint(), screenPts);
        	path.lineTo(screenPts.x, screenPts.y);
           	path.close();
           	
           	RectF rectF = new RectF();
           	path.computeBounds(rectF, true);
           	
           	// Niet opvullen als de polygoon toch te klein wordt op de kaart
           	if(Math.abs(rectF.width()) > Mapp.polygonMinDisplayWidth)
           	{
           		canvas.drawPath(path, this.shapePaint);
           	
           		pathRegion = new Region();
           		pathRegion.setPath(path, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));
           	}
           	else
           	{
           		// Maar in plaats daarvan een marker tonen
           		Bitmap bmp = BitmapFactory.decodeResource(
           				Mapp.instance.getResources(), R.drawable.polygonmarker);
           		canvas.drawBitmap(bmp, rectF.centerX()-(bmp.getWidth()/2), rectF.centerY()-(bmp.getHeight()), null);
           	}
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
    	Log.v(Mapp.TAG,"Enter");
    	// Wanneer de gebruiker zijn vinger op het touchscreen drukt
    	if(event.getAction() == MotionEvent.ACTION_DOWN)
    	{   
    		this.eventConsumed = notifyTouchDown(event);
    		Log.v(Mapp.TAG,"Consumed");
    		return eventConsumed;
    	}
    	if(event.getAction() == MotionEvent.ACTION_MOVE)
    	{
    		boolean consumeEvent = false;
    		consumeEvent = notifyTouchMove(event);
    		
    		if(consumeEvent && !this.eventConsumed)
    		{
    			this.eventConsumed = true;
    		}
    		
    		return consumeEvent;
    	}
    	
        // Wanneer gebruiker zijn vinger optilt
        if (event.getAction() == MotionEvent.ACTION_UP)
        {
        	boolean consumeEvent = false;
        	consumeEvent = notifyTouchUp(event);
        	// Het event is niet opgeëist door een listener, en de touch duurde minder lang
        	// dan maxTouchDuration
        	if(!consumeEvent && !this.eventConsumed && Mapp.isFirstOverlay(this)
        			&& (System.currentTimeMillis()-timer < Mapp.maxTouchDuration))
        	{
        		// Voeg een nieuwe laag toe
        		Mapp.addNewOverlay(event);
        	}
        	
        	return consumeEvent;
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
    	metaHandler.removeCallbacks(editMetaCallback);
    	if(this.movingPoint)
    	{
    		// We waren een punt aan het verplaatsen maar hebben het scherm losgelaten
    		// Schakel puntverplaatsing uit en sla de nieuwe positie op
    		movingPoint = false;
    		GeoPoint p = mapView.getProjection().fromPixels(
        			(int) event.getX(),
                    (int) event.getY());
    		polygon.saveMovedPoint(movingPointId, p);
    		
    		// Checken of we het punt op een ander punt hebben gesleept
        	polygon.reset();
        	while(polygon.hasNextPoint())
        	{
    	        GeoPoint point = polygon.getNextPoint();
    	        Point screenPts = mapView.getProjection().toPixels(point, null);
    	        int divx, divy;
    	        divx = Math.abs(screenPts.x-(int) event.getX());
    	        divy = Math.abs(screenPts.y-(int) event.getY());
    	           
    	        if(divx < Mapp.pointPixelTreshold 
    	        		&& divy < Mapp.pointPixelTreshold)
    	        {
    	        	// Indien er nog meer dan 3 punten over zijn, verwijderen we dit punt
    	        	if(polygon.getNumPoints() > 3 && !movingGeoPoint.equals(point))
    	        	{
    	        		polygon.removePoint(point);
    	        		return true;
    	        	}
    	        }
        	}
    		return true;
    	}
    	
    	if(!polygon.getIsClosed() && !this.polygonEditMode) // Nieuwe polygoon aan 't maken
    	{
	    	// Alleen een punt tekenen als de touch minder dan maxTouchDuration duurde
	    	long diff = System.currentTimeMillis()-timer;
	    	GeoPoint p = mapView.getProjection().fromPixels(
	    			(int) event.getX(),
	                (int) event.getY());
	    	
	    	if(!movingPoint && diff < Mapp.maxTouchDuration)
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
			            
			        if(divx < Mapp.pointPixelTreshold && divy < Mapp.pointPixelTreshold)
			        {
			        	if (polygon.getPointCount() >= 3)
			        	{
			        		polygon.setIsClosed(true);
			        		OverlayManager.editModeMutex(false);
			        	}
			        	return true;
			        }
			        else
			        {
			           	polygon.addPoint(p);
			           	return true;
			        }
		        }
		        else
		        {
		           	polygon.addPoint(p);
		           	return true;
		        }
	    	}
	    	
	    	return false;
    	}
    	/*
    	else if (polygon.getIsClosed() && (this.pathRegion.contains((int) event.getX(), (int) event.getY()))) {
    		if (!this.polygonEditMode && Mapp.instance.displayingMetaPopup()) {
    			Mapp.instance.hideMetaPopup();
    			return true;
    		}
    		else {
    			Mapp.instance.showMetaPopup((int) event.getX(), (int) event.getY());
    			return true;
    		}
    	}
    	*/
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
		
		// Testen of we in deze polygoon getapped hebben
		if(this.pathRegion != null && !this.polygonEditMode && this.polygon.getIsClosed())
		{
	    	if(this.pathRegion.contains((int) event.getX(), (int) event.getY()))
	    	{
	    		// Ja dus, schakel editmode in en toon een metapopupje
	    		if(OverlayManager.editModeMutex(true))
	    		{
	    			this.polygonEditMode = true;
	    			//Mapp.instance.showMetaPopup((int) event.getX(), (int) event.getY());
	    			//MetaTimer.schedule(task, Mapp.metaTouchDuration);
	    			metaHandler.postDelayed(editMetaCallback, Mapp.metaTouchDuration);
		    		Mapp.moveToFront(this);
		    		return true;
	    		}
	    		return false;
	    	}
		}
		
		// Checken of we hier toevallig op een al geplaatst punt touchen (en in editmode zijn)
    	polygon.reset();
    	while(polygon.hasNextPoint() && (this.polygonEditMode || !this.polygon.getIsClosed()))
    	{
	        GeoPoint point = polygon.getNextPoint();
	        Point screenPts = mapView.getProjection().toPixels(point, null);
	        int divx, divy;
	        divx = Math.abs(screenPts.x-(int) event.getX());
	        divy = Math.abs(screenPts.y-(int) event.getY());
	           
	        if(divx < Mapp.pointPixelTreshold 
	        		&& divy < Mapp.pointPixelTreshold)
	        {
	        	if(point.equals(polygon.getFirstPoint()) && !polygon.getIsClosed())
	        	{
	        		return false;
	        	}
	        	
	           	movingPoint = true;
	           	movingGeoPoint = point;
	           	movingPointId = polygon.getPointer()-1;
	           	return true;
	        }
    	}
    	
    	// Als we ergens klikken, in editmode zijn, en geen punt hebben aangetapt,
    	if(this.pathRegion != null && this.polygonEditMode && !this.movingPoint)
		{
    		//controleren of we op een lijn klikken
    		polygon.reset();
    		Point pointP = new Point((int) event.getX(), (int) event.getY());
           	GeoPoint point = mapView.getProjection().fromPixels(pointP.x, pointP.y);

        	for (int i = 0; i < polygon.getNumPoints(); i++)
        	{
    	        Point pointA = mapView.getProjection().toPixels(polygon.getPoint(i), null);
    	        Point pointB = mapView.getProjection().toPixels(polygon.getPoint(i+1), null);
    	        
        		AlgebraLine line = new AlgebraLine(pointA, pointB);
        		if (line.isNear(pointP, Mapp.pointPixelTreshold)) {
    	           	movingPoint = true;
    	           	polygon.addIntermediatePoint(point, i+1);
    	           	movingGeoPoint = point;
    	           	movingPointId = (i+1);
    	           	return true;
        		}
        	}
        	if(!this.pathRegion.contains((int) event.getX(), (int) event.getY()))
	    	{
	    		// anders schakelen we de editmode weer uit.
	    		this.polygonEditMode = false;
	    		OverlayManager.editModeMutex(false);
	    		//Mapp.instance.hideMetaPopup();
	    	}
        	else {
        		metaHandler.postDelayed(editMetaCallback, Mapp.metaTouchDuration);
        	}
    		return true;
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
    	metaHandler.removeCallbacks(editMetaCallback);
    	// We zijn een punt aan het verplaatsen
    	if(this.movingPoint)
		{
    		GeoPoint p = mapView.getProjection().fromPixels(
        			(int) event.getX(),
                    (int) event.getY());
    		polygon.editPoint(movingGeoPoint, p);
			movingGeoPoint = p;
			return true;
		}
    	
    	return false;
    } 
    
    /**
     * Geeft de polygonmanager van deze overlay
     * @return de polygonmanager van deze overlay
     */
    public PolygonManager getManager()
    {
    	return polygon;
    }
    
    /**
     * Geeft aan of de polygoon in editmode is of niet
     * @return true als de polygoon in editmode is
     */
    public boolean getIsEditMode()
    {
    	return this.polygonEditMode;
    }
    
} 