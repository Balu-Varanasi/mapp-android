package nl.appcetera.mapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.view.View;

/**
 * Klasse om de metapopup te maken en beheren
 * @author Joost
 */

public class MetaPopupManager extends View {

	private boolean isVisible = false;
	private Point p;
	//private AlertDialog.Builder builder;
	//private AlertDialog alertDialog;
	
	public MetaPopupManager(Context context) {

		super(context);
		p = new Point(0,0);
		/*		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.metapopup,
		                               (ViewGroup) findViewById(R.id.LinearLayout01));

		ImageView image = (ImageView) layout.findViewById(R.id.ImageView01);
		image.setImageResource(R.drawable.androidmarker);

		builder = new AlertDialog.Builder(context);
		builder.setView(layout);

		alertDialog = builder.create();*/

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
	 * Deze functie maakt de metapopup zichtbaar (gaat uit van een onzichtbare popup)
	 */
	public void makeVisible() {
		isVisible = false;
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
	
	public void onDraw(Canvas canvas) {
		Bitmap myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.metapopup);
	    canvas.drawBitmap(myBitmap, p.x, p.y, null);
	}

}
