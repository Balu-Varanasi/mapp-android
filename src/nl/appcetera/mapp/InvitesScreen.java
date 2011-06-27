package nl.appcetera.mapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Klasse die het invites-scherm beheert, waar de gebruiker onder andere uit kan loggen
 * @author Joost
 */
public class InvitesScreen extends Activity {

	public static final int RESULT_OK = 42;
	private InvitesScreen instance;
	private PolygonData dbase;
	private String username;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 * @param savedInstanceState de bundle die de activity meekrijgt wanneer hij wordt gemaakt
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.invitesscreen);
		
		instance = this;
		
		dbase = Mapp.getDatabase();
		SharedPreferences settings = getSharedPreferences(Mapp.SETTINGS_KEY, MODE_PRIVATE);
		username = settings.getString("username", null);
		Cursor groupCursor = dbase.getInvites(username);
		
		String groupNames[]= new String[groupCursor.getCount()];
		final int groupID[]= new int[groupCursor.getCount()];
		if (groupCursor.getCount() > 0) {
			int index = 0;
			if(groupCursor.moveToFirst()) {
				do
				{
					groupID[index] = groupCursor.getInt(0);
					Cursor groupNameCursor = dbase.getGroup(groupID[index]);
					groupNameCursor.moveToFirst();
					groupNames[index] = groupCursor.getString(1);
					groupNameCursor.close();
					index++;
				}
				while(groupCursor.moveToNext());
			}
		}
		
		groupCursor.close();
		
		final ListView invitelist = (ListView) findViewById(R.id.invitelist);
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 , groupNames);
		invitelist.setAdapter(arrayAdapter);

		invitelist.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				final int id = arg2;
				
				//vraag de gebruiker of hij wil accepteren of weigeren
                new AlertDialog.Builder(instance)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.invite_dialogtitle)
                .setMessage(R.string.invite_dialogtext)
                .setPositiveButton(R.string.invite_positivebutton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	dbase.acceptMembership(username, groupID[id]);
                    }
                })
                .setNegativeButton(R.string.invite_negativebutton, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    	dbase.deleteMembership(groupID[id], username);
                    	dbase.removeGroup(groupID[id], false);
                    }
                })
                .show();
			}
		});
		
	}
}
