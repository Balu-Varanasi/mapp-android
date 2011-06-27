package nl.appcetera.mapp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * Klasse die het scherm beheert waarin gebruikers groepen kunnen bewerken
 * @author Joost
 */
public class GroupAdminScreen extends Activity {

	public static final int RESULT_GROUPSELECT = 42;
	public static final int RESULT_TOGROUP = 41;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 * @param savedInstanceState de bundle die de activity meekrijgt wanneer hij wordt gemaakt
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groupadminscreen);  
		
		PolygonData dbase = Mapp.getDatabase();
		int groupID = savedInstanceState.getInt(GroupsScreen.ID_KEY);
		
		Cursor memberlistCursor = dbase.getGroupMembers(groupID);
		
		String members[]= new String[memberlistCursor.getCount()];
		int index = 0;
		if(!memberlistCursor.moveToFirst()) {
			do
			{
				members[index] = memberlistCursor.getString(0);
				index++;
			}
			while(memberlistCursor.moveToNext());
		}
		
		final ListView memberlist = (ListView) findViewById(R.id.memberlist);
		memberlist.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1 , members));
			
		memberlist.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				//((TextView) view).getText();
			}
		});

		
		final Button okbutton = (Button) findViewById(R.id.okbutton);
		okbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de okbutton zorgt ervoor dat de activity wordt getermineerd
			 * en de gebruiker kan gewoon terugkeren naar de hoofdactivity
			 */
			public void onClick(View v) {
            	Intent mIntent = new Intent();
            	setResult(RESULT_GROUPSELECT, mIntent);
            	finish();
            }
		});
	}
}
