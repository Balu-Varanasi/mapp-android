package nl.appcetera.mapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Klasse die het account-scherm beheert, waar de gebruiker onder andere uit kan loggen
 * @author Joost
 */
public class AccountScreen extends Activity {

	public static final int RESULT_LOGOUT = 42;
	public static final int RESULT_CANCEL = 41;
	public static final String USERNAME_KEY = "USERNAME";
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 * @param savedInstanceState de bundle die de activity meekrijgt wanneer hij wordt gemaakt
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.accountscreen);  
		
		Bundle bundle = getIntent().getExtras();
		
		final TextView syncIntervalDisp = (TextView)findViewById(R.id.accountlabel);
		syncIntervalDisp.setText("You are currently logged in as:\n"+bundle.getString(USERNAME_KEY));
		
		
		final Button logoutbutton = (Button) findViewById(R.id.account_logoutbutton);
		logoutbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de logoutbutton zorgt ervoor dat de activity wordt getermineerd
			 * en de keuze om uit te loggen wordt doorgestuurd
			 */
			public void onClick(View v) {            	
            	Intent mIntent = new Intent();
            	setResult(RESULT_LOGOUT, mIntent);
            	finish();
            }
		});
		
		final Button cancelbutton = (Button) findViewById(R.id.account_cancelbutton);
		cancelbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de cancelbutton zorgt ervoor dat de activity wordt getermineerd
			 * en de gebruiker kan gewoon terugkeren naar de hoofdactivity
			 */
			public void onClick(View v) {
            	Intent mIntent = new Intent();
            	setResult(RESULT_CANCEL, mIntent);
            	finish();
            }
		});
	}
}
