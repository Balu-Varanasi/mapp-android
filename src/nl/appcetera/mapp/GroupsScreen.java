package nl.appcetera.mapp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Klasse die het groups-scherm beheert, waar de gebruiker onder andere uit kan loggen
 * @author Joost
 */
public class GroupsScreen extends Activity {

	public static final int RESULT_OK = 42;
	public static GroupsScreen instance;
	public static final int GROUPADMIN_ACTIVITYCODE = 42;
	public static final String ID_KEY = "ID";
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 * @param savedInstanceState de bundle die de activity meekrijgt wanneer hij wordt gemaakt
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groupsscreen);  
		instance = this;
		SharedPreferences settings = getSharedPreferences(Mapp.SETTINGS_KEY, MODE_PRIVATE);
		
		PolygonData dbase = Mapp.getDatabase();
		String username = settings.getString("username", null);
		
		//tijdelijke hardcode	
		Cursor groupsIDCursor = dbase.getMemberShips(username);
		if (groupsIDCursor.getCount() == 0) {
			dbase.addMembership(username, 1, true, true);
		}
		Log.v(Mapp.TAG, username);
		groupsIDCursor = dbase.getMemberShips(username);
		Log.v(Mapp.TAG, "Groups:"+groupsIDCursor.getCount());
		
		final ListView grouplist = (ListView) findViewById(R.id.grouplist);
		final int groups[] = new int[groupsIDCursor.getCount()];
		final String groupNames[] = new String[groupsIDCursor.getCount()];
		final String groupOwners[] = new String[groupsIDCursor.getCount()];
		int index = 0;
			
		if(groupsIDCursor.moveToFirst()) {
			do
			{
				int id = groupsIDCursor.getInt(0);				
				Cursor groupCursor = dbase.getGroup(id);
				groupCursor.moveToFirst();
				groups[index] = id;
				groupNames[index] = groupCursor.getString(1);
				groupOwners[index] = groupCursor.getString(0);
				/*
				GroupButton groupButton = new GroupButton(this, id, username == owner);
				groupButton.setText(name);
				buttonlayout.addView(groupButton);

				groupButton.setOnClickListener(new View.OnClickListener() {
					/**
					 * Tappen op een groupbutton zorgt ervoor dat we deze groep gaan bekijken
					 * tenzij het een een groep is die we beheren, dan kunnen we 'm eventueel aanpassen
					 *//*
					public void onClick(View v) {
						Bundle bundle = new Bundle();
						bundle.putInt(GroupsScreen.ID_KEY, ((GroupButton) v).getID());
						if (((GroupButton) v).isOwner())
						{
							Intent intent = new Intent(instance, GroupAdminScreen.class);
							intent.putExtras(bundle);
							instance.startActivityForResult(intent, GroupsScreen.GROUPADMIN_ACTIVITYCODE);
						}
						else
						{
	                    	Intent mIntent = new Intent();
	                    	mIntent.putExtras(bundle);
	                    	setResult(RESULT_OK, mIntent);
	                    	finish();
						}
		            }
				});
				*/
			}
			while(groupsIDCursor.moveToNext());
		}
		
		grouplist.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1 , groupNames));
		
		grouplist.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				Log.v(Mapp.TAG, ""+groups[position]);
			}
		});
		
		final Button newgroupbutton = (Button) findViewById(R.id.newgroupbutton);
		newgroupbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de okbutton zorgt ervoor dat de activity wordt getermineerd
			 * en de gebruiker kan gewoon terugkeren naar de hoofdactivity
			 */
			public void onClick(View v) {
				Bundle bundle = new Bundle();
				bundle.putInt(GroupsScreen.ID_KEY, ((GroupButton) v).getID());
				Intent intent = new Intent(instance, GroupAdminScreen.class);
				intent.putExtras(bundle);
				instance.startActivityForResult(intent, GroupsScreen.GROUPADMIN_ACTIVITYCODE);
            }
		});
	}
	
	/**
	 * Deze functie wordt aangeroepen wanneer een Activity die vanuit GroupsScreen is aangeroepen zn finish() aanroept
	 * @param requestCode een int die aangeeft om welke Activity het gaat
	 * @param resultCode een int die de status van terminatie van de Activity aangeeft
	 * @param data een intent die eventuele result-data bevat
	 */
	protected void onActivityResult (int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		if(data == null)
		{
			return;
		}
		
		Bundle bundle = data.getExtras();

		switch(requestCode) {
			case GROUPADMIN_ACTIVITYCODE:
				if (resultCode == GroupAdminScreen.RESULT_TOGROUP)
				{
					Intent mIntent = new Intent();
                	mIntent.putExtras(bundle);
                	setResult(RESULT_OK, mIntent);
                	finish();
				}
				break;
		}	
	}
	
}
