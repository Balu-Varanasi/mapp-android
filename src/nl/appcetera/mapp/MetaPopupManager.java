package nl.appcetera.mapp;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

/**
 * Klasse om de metapopup te maken en beheren
 * @author Joost
 */

public class MetaPopupManager extends Dialog {

	private boolean isVisible = false;
	private AlertDialog.Builder builder;
	//Wordt nog nodig, is nu nog niet gelezen dus vandaar de foutmelding
	private AlertDialog alertDialog;
	
	public MetaPopupManager(Context context) {

		super(context);
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflater.inflate(R.layout.metapopup,
		                               (ViewGroup) findViewById(R.id.LinearLayout01));

		ImageView image = (ImageView) layout.findViewById(R.id.ImageView01);
		image.setImageResource(R.drawable.androidmarker);

		builder = new AlertDialog.Builder(context);
		builder.setView(layout);

		alertDialog = builder.create();

	}

	public boolean isVisible() {
		return isVisible;
	}

}
