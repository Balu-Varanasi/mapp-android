package nl.appcetera.mapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;

public class SettingsScreen extends Activity  {

	public static final String SATMODE_KEY = "SATMODE";
	public static final String SYNCINTERVAL_KEY = "SYNCINTERVAL";
	public static final int RESULT_SAVE = 42;
	public static final int RESULT_CANCEL = 41;
	private int syncTime;
	private boolean satelliteMode;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 * @param savedInstanceState de bundle die de activity meekrijgt wanneer hij wordt gemaakt
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settingsscreen);

		Bundle bundle = getIntent().getExtras();
		satelliteMode = bundle.getBoolean(SATMODE_KEY);
		syncTime = bundle.getInt(SYNCINTERVAL_KEY);
		
		final RadioButton satbutton = (RadioButton) findViewById(R.id.settings_satbutton);
		satbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				satelliteMode = true;
            }
		});

		final RadioButton mapbutton = (RadioButton) findViewById(R.id.settings_mapbutton);
		mapbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				satelliteMode = false;
            }
		});

		final Button savebutton = (Button) findViewById(R.id.settings_savebutton);
		savebutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
            	Bundle bundle = new Bundle();

            	bundle.putBoolean(SATMODE_KEY, satelliteMode);
            	bundle.putInt(SYNCINTERVAL_KEY, syncTime);
            	
            	Intent mIntent = new Intent();
            	mIntent.putExtras(bundle);
            	setResult(RESULT_SAVE, mIntent);
            	finish();
            }
		});
		
		final Button cancelbutton = (Button) findViewById(R.id.settings_cancelbutton);
		cancelbutton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
            	Intent mIntent = new Intent();
            	setResult(RESULT_CANCEL, mIntent);
            	finish();
            }
		});
		
		(satelliteMode ? satbutton : mapbutton).toggle();
	}
}
