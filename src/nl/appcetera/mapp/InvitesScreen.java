package nl.appcetera.mapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * Klasse die het invites-scherm beheert, waar de gebruiker onder andere uit kan loggen
 * @author Joost
 */
public class InvitesScreen extends Activity {

	public static final int RESULT_OK = 42;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 * @param savedInstanceState de bundle die de activity meekrijgt wanneer hij wordt gemaakt
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.invitesscreen);  
		
		final Button okbutton = (Button) findViewById(R.id.okbutton);
		okbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de okbutton zorgt ervoor dat de activity wordt getermineerd
			 * en de gebruiker kan gewoon terugkeren naar de hoofdactivity
			 */
			public void onClick(View v) {
            	Intent mIntent = new Intent();
            	setResult(RESULT_OK, mIntent);
            	finish();
            }
		});
	}
}
