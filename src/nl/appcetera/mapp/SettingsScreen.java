package nl.appcetera.mapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

/**
 * Deze klasse beheert het settings-scherm, waar de gebruiker zijn voorkeuren kan instellen (satellite/map, syncinterval)
 * @author Joost
 */
public class SettingsScreen extends Activity  {

	public static final String SATMODE_KEY = "SATMODE";
	public static final String SYNCINTERVAL_KEY = "SYNCINTERVAL";
	public static final String ZOOMCONTROLS_KEY = "ZOOMCONTROLS";
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
		
		final CheckBox zoomcontrolBox = (CheckBox) findViewById(R.id.settings_zoomcontrolbox);
		zoomcontrolBox.setChecked(bundle.getBoolean(ZOOMCONTROLS_KEY));
		final RadioButton satbutton = (RadioButton) findViewById(R.id.settings_satbutton);
		satbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Als er op de satellite-radiobutton getapt wordt, gaan we naar satellite mode
			 */
			public void onClick(View v) {
				satelliteMode = true;
            }
		});

		final RadioButton mapbutton = (RadioButton) findViewById(R.id.settings_mapbutton);
		mapbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Als er op de map-radiobutton getapt wordt, gaan we naar niet-satellite mode (= map mode)
			 */
			public void onClick(View v) {
				satelliteMode = false;
            }
		});
		
		final Button savebutton = (Button) findViewById(R.id.settings_savebutton);
		savebutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de savebutton zorgt ervoor dat de activity wordt getermineerd
			 * en de nieuwe instellingen worden opgeleverd in een Bundle
			 */
			public void onClick(View v) {
            	Bundle bundle = new Bundle();

            	bundle.putBoolean(SATMODE_KEY, satelliteMode);
            	bundle.putBoolean(ZOOMCONTROLS_KEY, zoomcontrolBox.isChecked());
            	bundle.putInt(SYNCINTERVAL_KEY, syncTime);
            	
            	Intent mIntent = new Intent();
            	mIntent.putExtras(bundle);
            	setResult(RESULT_SAVE, mIntent);
            	finish();
            }
		});
		
		final Button cancelbutton = (Button) findViewById(R.id.settings_cancelbutton);
		cancelbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de cancelbutton zorgt er ook voor dat de activity wordt getermineerd
			 * maar hier is er geen Bundle met nieuwe informatie
			 */
			public void onClick(View v) {
            	Intent mIntent = new Intent();
            	setResult(RESULT_CANCEL, mIntent);
            	finish();
            }
		});
		
		//afhankelijk van de huidige sat-mode moet een van de twee radiobuttons alvast getoggled worden
		(satelliteMode ? satbutton : mapbutton).toggle();
		
		updateSyncTextField();
		final SeekBar syncSeekbar = (SeekBar)findViewById(R.id.sync_seekbar);
		syncSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			/**
			 * Deze functie wordt aangeroepen elke keer wanneer de 'vooruitgang' van de Seekbar wordt gewijzigd
			 */
			public void onProgressChanged(SeekBar v, int progress, boolean fromTouch) {
				syncTime = (int)((progress*0.5 + 0.5)*60*1000);
				updateSyncTextField();
			}
			
			/**
			 * Verplichte seekbar-methode, die wij niet gaan gebruiken
			 */
			public void onStartTrackingTouch(SeekBar seekBar) {
				// Auto-generated method stub
			}
			/**
			 * Verplichte seekbar-methode, die wij niet gaan gebruiken
			 */
			public void onStopTrackingTouch(SeekBar seekBar) {
				// Auto-generated method stub
			}
			
		});
		syncSeekbar.setProgress((int)(2 * (((double)syncTime-0.5)/60000)));
	}
	
	/**
	 * Deze functie update de text in de textview onder de seekbar (op basis van de synctime)
	 */
	private void updateSyncTextField() {
		final TextView syncIntervalDisp = (TextView)findViewById(R.id.syncintervaldisplay);
		syncIntervalDisp.setText("Syncing every "+((double)syncTime/60000)+" minute"+(((double)syncTime/60000) != 1 ? "s" : ""));
	}
}
