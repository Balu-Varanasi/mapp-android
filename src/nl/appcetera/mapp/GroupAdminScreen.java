package nl.appcetera.mapp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

/**
 * Klasse die het scherm beheert waarin gebruikers groepen kunnen bewerken
 * @author Joost
 */
public class GroupAdminScreen extends Activity {

	public static final int RESULT_GROUPSELECT = 42;
	public static final int RESULT_TOGROUP = 41;
	
	private PolygonData dbase;
	private int groupID;
	private String groupName;
	private String groupOwner;
	private EditText nameField;
	private EditText newuserField;
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 * @param savedInstanceState de bundle die de activity meekrijgt wanneer hij wordt gemaakt
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.groupadminscreen);  

		nameField = (EditText) findViewById(R.id.edtInputName);
		newuserField = (EditText) findViewById(R.id.edtInputMember);
		dbase = Mapp.getDatabase();
		Bundle bundle = getIntent().getExtras();
		groupID = bundle.getInt(GroupsScreen.ID_KEY);

		loadMemberList();
		
		Cursor groupNameCursor = dbase.getGroup(groupID);
		
		if (groupNameCursor.getCount() > 0) {
			if(groupNameCursor.moveToFirst()) {
				groupOwner = groupNameCursor.getString(0);
				groupName = groupNameCursor.getString(1);
			}
		}
		nameField.setText(groupName);
		
		final Button togroupbutton = (Button) findViewById(R.id.togroupbutton);
		togroupbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de togroupbutton zorgt ervoor dat de activity wordt getermineerd
			 * en de gebruiker kan gewoon terugkeren naar de hoofdactivity
			 */
			public void onClick(View v) {
            	Intent mIntent = new Intent();
            	setResult(RESULT_TOGROUP, mIntent);
            	finish();
            }
		});
		
		final Button savebutton = (Button) findViewById(R.id.savebutton);
		savebutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de savebutton zorgt ervoor dat de activity wordt getermineerd
			 * en de gebruiker terugkeert naar de group-activity
			 */
			public void onClick(View v) {
				groupName = nameField.getText().toString();
				dbase.editGroup(groupID, groupOwner, groupName, true);
            	Intent mIntent = new Intent();
            	setResult(RESULT_GROUPSELECT, mIntent);
            	finish();
            }
		});
		
		final Button addbutton = (Button) findViewById(R.id.addmemberbutton);
		addbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de addbutton zorgt ervoor dat de activity wordt getermineerd
			 * en de gebruiker terugkeert naar de group-activity
			 */
			public void onClick(View v) {
				toastMessage("Invite sent!");
				dbase.addMembership(newuserField.getText().toString(), groupID, false, true);
				newuserField.setText("");
            }
		});
	}
	
	private void loadMemberList() {
		Cursor memberlistCursor = dbase.getGroupMembers(groupID);
		String members[]= new String[memberlistCursor.getCount()];
		if (memberlistCursor.getCount() > 0) {
			int index = 0;
			if(memberlistCursor.moveToFirst()) {
				do
				{
					members[index] = memberlistCursor.getString(0);
					Log.v(Mapp.TAG, ""+members[index]);
					index++;
				}
				while(memberlistCursor.moveToNext());
			}
		}
		
		memberlistCursor.close();
		final ListView memberlist = (ListView) findViewById(R.id.memberlist);
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1 , members);
		memberlist.setAdapter(arrayAdapter);

		memberlist.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				//((TextView) view).getText();
			}
		});
	}
	
	/**
	 * Deze methode toont een toastberichtje - vooral een shortcode omdat 't anders steeds 3 regels kost
	 * @param text het bericht dat getoond moet worden
	 */
	private void toastMessage(CharSequence text) {
		Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
		toast.show();
	}
}
