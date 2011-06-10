package nl.appcetera.mapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class MetaEditScreen extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.metascreen);

		//First Extract the bundle from intent
		Log.v(Mapp.TAG, "META-Activity started");
		//Bundle bundle = getIntent().getExtras();
		
		//Log.v(Mapp.TAG, " "+bundle.getInt("COLOR"));
		/*
		//Next extract the values using the key as
		String name = bundle.getString("NAME");
		String company = bundle.getString("COMPANY");*/
	}
	
}
