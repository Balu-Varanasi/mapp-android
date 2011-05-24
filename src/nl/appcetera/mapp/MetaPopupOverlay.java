package nl.appcetera.mapp;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import android.view.View;

/**
 * Klasse om de metapopup te maken en beheren
 * @author Joost
 */

public class MetaPopupOverlay extends com.google.android.maps.Overlay {

	private boolean isVisible;
	private Point p;
	private Context context;
	private MapView mapView;
	private Mapp activity;

	/**
	 * Constructor
	 * @param instantie van de mapview
	 */
	public MetaPopupOverlay(MapView mv, Context context, Mapp mapp)
	{
		super();
		isVisible = false;
		p = new Point(30, 30);
		this.mapView  = mv;
		this.context = context;
		this.activity = mapp;
	}
	
	/**
	 * Methode om de popup te tekenen
	 * @param canvas
	 * @param mapView
	 * @param shadow
	 */
	@Override
    public void draw(android.graphics.Canvas canvas, MapView mapView, boolean shadow)
    {
		super.draw(canvas, mapView, shadow);   
		Log.v("Draw van metapopup","Aangeroepen");
    	if (isVisible) {
    		Log.v("Draw van metapopup","voorbij de check");
	    	Bitmap popupImage = BitmapFactory.decodeResource(context.getResources(), R.drawable.metapopup);
	        canvas.drawBitmap(popupImage,
	            p.x - popupImage.getWidth() / 2,
	            p.y - popupImage.getHeight(), null);
    	}
    }
	/**
	 * Functie die teruggeeft of de popup op dit moment visible is of niet
	 * @return
	 */
	public boolean isVisible() {
		return isVisible;
	}
	
	/**
	 * Functie die de popup van visible naar invisible zet of andersom, als dat nodig is
	 * @param visible forceer een doel-state
	 */
	public void toggleVisible(boolean visible) {
		if (visible != isVisible) {
			if (isVisible)
				makeInvisible();
			else
				makeVisible();
		}
	}
	
	/**
	 * Deze functie maakt de metapopup onzichtbaar (gaat uit van een zichtbare popup)
	 */
	public void makeInvisible() {
		isVisible = false;
	}
	
	/**
	 * Een lazy-mans makeVisible, zonder de coordinaten aan te hoeven passen
	 */
	public void makeVisible() {
		makeVisible(p.x, p.y);
	}
	
	/**
	 * Deze functie maakt de metapopup zichtbaar (gaat uit van een onzichtbare popup)
	 */
	public void makeVisible(int x, int y) {
		p = new Point(x, y);
		isVisible = true;
	}
	
	/**
	 * Deze functie definieert de onscreen-coordinaten van de popup
	 * @param p het punt dat de coordinaten bevat
	 */
	public void setCoords(Point q) {
		p.x = q.x;
		p.y = q.y;
	}
	
	/**
	 * Deze functie geeft de onscreen-coordinaten van de popup terug als een Point-object
	 * @return Point een punt met de coordinaten
	 */
	public Point getCoords() {
		return new Point(p);
	}

}
