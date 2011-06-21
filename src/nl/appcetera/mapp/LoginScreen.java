package nl.appcetera.mapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Klasse die het login-scherm beheert, waar de gebruiker kan registreren of inloggen
 * @author Joost
 */
public class LoginScreen extends Activity {

	public static final int RESULT_OK = 42;

	public static final String USERNAME_KEY = "USERNAME";
	public static final String PASSWORD_KEY = "PASSWORD";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loginscreen);  
		
		final EditText usernameField = (EditText) findViewById(R.id.input_username);
		final EditText passwordField = (EditText) findViewById(R.id.input_password);
	
		final Button loginbutton = (Button) findViewById(R.id.login_loginbutton);
		loginbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de loginbutton zorgt ervoor dat de activity wordt getermineerd
			 * en de username en password worden opgeleverd in een Bundle
			 */
			public void onClick(View v) {
				SharedPreferences settings = getSharedPreferences(Mapp.SETTINGS_KEY, MODE_PRIVATE);
				SharedPreferences.Editor editor = settings.edit();
				editor.putString("username", usernameField.getText().toString());
				editor.putString("password", passwordField.getText().toString());
				editor.commit();
            	
            	Intent mIntent = new Intent();
            	setResult(RESULT_OK, mIntent);
            	finish();
            }
		});
	}
}
