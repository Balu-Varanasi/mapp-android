package nl.appcetera.mapp;

import java.util.List;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;

import nl.appcetera.mapp.R;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;

/**
 * Mapp main activity
 * @author Mathijs
 * @author Joost
 * @group AppCetera
 */
public class Mapp extends MapActivity
{
	private MapView mapView;
	private MapController mapController;
	private GeoPoint point;
	private PolygonData database;
	private OverlayManager om;
	private ServerSync s;
	public SharedPreferences settings;
	public static final String SETTINGS_KEY = "MAPP_SETTINGS";
	public static final int pointPixelTreshold = 25; // Maximaal verschil tussen 2 punten in pixels voor ze als gelijk worden beschouwd
	public static final String TAG = "AppCetera"; // Log-tag
	public static final int maxTouchDuration = 500;
	public static final int polygonMinDisplayWidth = 5; // Wanneer een polygoon smaller is dan dit wordt ie niet getoond
	public static int syncInterval = 60*1000; // Interval tussen synchronisaties in milliseconden
	public static final int offlineRetryInterval = 30*60*1000; // Interval tussen sync-attempts als toestel offline is
	public static final int metaTouchDuration = 1000; //touch-duration waarna we naar de meta-activity gaan
	public static final int META_EDITSCREEN_ACTIVITYCODE = 42;
	public static final int SETTINGSSCREEN_ACTIVITYCODE = 314;
	public static final int ACCOUNTSCREEN_ACTIVITYCODE = 271;
	public static final int LOGINSCREEN_ACTIVITYCODE = 404;
	private static final int GROUPSSCREEN_ACTIVITYCODE = 162;
	private static final int INVITESSCREEN_ACTIVITYCODE = 505;
	public static Mapp instance;
	
	/**
	 * Wordt aangeroepen wanneer deze activity wordt aangemaakt
	 * @param savedInstanceState de bundle die de activity meekrijgt wanneer hij wordt gemaakt
	 */
	@Override
    public void onCreate(Bundle savedInstanceState)
    {
		// Constructor van de parent aanroepen
        super.onCreate(savedInstanceState);

        // Juiste layout (mapview) zetten
        setContentView(R.layout.main);
        
        // Instantie van deze klasse beschikbaar maken met een statische variabele
        Mapp.instance = this;
        
		// Settings ophalen
        settings = getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE);

        // Mapview ophalen
        mapView = (MapView) findViewById(R.id.mapview);
	    
        // Databaseklasse opstarten
        database = new PolygonData(this);

        // Opgeslagen overlays laden
        om = new OverlayManager(mapView, database);
        
        // Syncservice starten
        s = new ServerSync(getApplicationContext(), database);
        
        mapView.invalidate();
        applySettings();
    }
	
	private void applySettings() {
		syncInterval = settings.getInt("syncInterval", syncInterval);
		mapView.setBuiltInZoomControls(settings.getBoolean("zoomControls", true));
	    mapView.setSatellite(settings.getBoolean("satelliteMode", true));
	}

	/**
	 * Wanneer de app gekilled wordt
	 */
	@Override
	public void onDestroy()
	{
		super.onDestroy();
		database.close();
		s.stopSync();
	}
	
	/**
	 * De applicatie gaat weer verder
	 */
	@Override
	public void onResume()
	{
		super.onResume();
		Log.v(TAG, "In de onresume van Mapp");
		if (isLoggedIn()) {
			// Naar de juiste plaats op de kaart gaan
			mapController = mapView.getController();
		    point = new GeoPoint(settings.getInt("pos_lat", 51824167),
		    		settings.getInt("pos_long", 5867374));
	        mapController.setZoom(settings.getInt("zoomlevel", 10));
	        mapController.animateTo(point);
	        
	        // Database opstarten
	        database = new PolygonData(this);
	        
	        // Syncservice hervatten
	        s.startSync();
	        
	        // Juiste groep ophalen en polygonen laden
	        //om.setGroup(settings.getInt("group", 1));
	        om.setGroup(1);
	        om.loadOverlays();
		}
		else
		{
			database = new PolygonData(this);
			database.empty();
			settings.edit().clear().commit();
			showLoginScreen();
		}
	}
	
	/**
	 * Er komt een andere app overheen, deze wordt gepauzeerd
	 */
	@Override
	public void onPause()
	{
		super.onPause();
		
		// Settings opslaan
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt("zoomlevel", mapView.getZoomLevel());
		editor.putInt("pos_long", mapView.getMapCenter().getLongitudeE6());
		editor.putInt("pos_lat", mapView.getMapCenter().getLatitudeE6());
		editor.putInt("group", 0);
		editor.commit();
		
		// Database afsluiten
		database.close();
		
		// Syncservice stoppen
		s.stopSync();
		
		OverlayManager.editModeMutex(false);
	}
	
	/**
	 * Verplaatst een overlay naar de bovenste laag
	 * @param po de overlay om naar boven te verplaatsen
	 */
	public static void moveToFront(PolygonOverlay po)
	{
		List<Overlay> listOfOverlays = Mapp.instance.mapView.getOverlays();
		listOfOverlays.remove(po);
		listOfOverlays.add(po);
	}
	
	/**
	 * Controleert of de gegeven overlay de eerste (=onderste) overlay is
	 * @param po de te checken overlay
	 * @return true indien gegeven overlay de onderste laag is
	 */
	public static boolean isFirstOverlay(PolygonOverlay po)
	{
		List<Overlay> listOfOverlays = Mapp.instance.mapView.getOverlays();
		return (listOfOverlays.get(0).equals(po));
	}
	
	/**
	 * Voegt een nieuwe overlay toe
	 * @param het event dat doorgegeven zal worden aan de nieuwe laag
	 */
	public static void addNewOverlay(MotionEvent event)
	{
		PolygonOverlay po = Mapp.instance.om.addOverlay();
		
		if(po != null)
		{
			// Geef het touchevent door, zodat we gelijk een nieuw punt kunnen maken
			event.setAction(MotionEvent.ACTION_DOWN);
			po.onTouchEvent(event, Mapp.instance.mapView);
			event.setAction(MotionEvent.ACTION_UP);
			po.onTouchEvent(event, Mapp.instance.mapView);
		}
	}
	
	/**
	 * MapActivities moeten deze functie verplicht overriden
	 */
	@Override
	protected boolean isRouteDisplayed()
	{
		return false;
	}
	
	/**
	 * Geeft een instantie van de databasemanager terug
	 * @return PolygonData
	 */
	public static PolygonData getDatabase()
	{
		return Mapp.instance.database;
	}
	
	/**
	 * Herlaad overlays
	 */
	public static void reload()
	{
		Mapp.instance.om.loadOverlays();
	}
	
	/**
	 * Deze functie wordt aangeroepen wanneer een Activity die vanuit Mapp is aangeroepen zn setResult aanroept
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
			case META_EDITSCREEN_ACTIVITYCODE:
				if (resultCode == MetaEditScreen.RESULT_SAVE)
				{
					int id = bundle.getInt(MetaEditScreen.ID_KEY);
					int color = bundle.getInt(MetaEditScreen.COLOR_KEY);
					String name = bundle.getString(MetaEditScreen.NAME_KEY);
					String description = bundle.getString(MetaEditScreen.DESCRIPTION_KEY);
					database.editPolygon(id, color, true, name, description);
				}
				else if (resultCode == MetaEditScreen.RESULT_DELETE)
				{
					int id = bundle.getInt(MetaEditScreen.ID_KEY);
					database.removePolygon(id, true);
				}
				break;
			case SETTINGSSCREEN_ACTIVITYCODE:
				if (resultCode == SettingsScreen.RESULT_SAVE)
				{
					SharedPreferences.Editor editor = settings.edit();
					editor.putBoolean("satelliteMode", bundle.getBoolean(SettingsScreen.SATMODE_KEY));
					editor.putBoolean("zoomControls", bundle.getBoolean(SettingsScreen.ZOOMCONTROLS_KEY));
					editor.putInt("syncInterval", bundle.getInt(SettingsScreen.SYNCINTERVAL_KEY));
					editor.commit();
					
					applySettings();
				}
				break;
			case ACCOUNTSCREEN_ACTIVITYCODE:
				if (resultCode == AccountScreen.RESULT_LOGOUT)
				{
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("username", null);
					editor.putString("password", null);
					editor.commit();
					
					showLoginScreen();
				}
				break;
			case LOGINSCREEN_ACTIVITYCODE:
				if (resultCode == LoginScreen.RESULT_OK)
				{
					//TODO
				}
				break;
		}	
	}
	
	@Override
	/**
	 * Deze functie wordt aangeroepen wanneer iemand op de menuknop duwt
	 * Het menu uit mainmenu.xml wordt geopend
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mainmenu, menu);
	    return true;
	}
	
	@Override
	/**
	 * Deze functie wordt aangeroepen wanneer een item uit het main-menu wordt aangetapt
	 * @param item het item van het menu dat is aangetapt
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.groupsbutton:
	    	showGroupsMenu();
	        return true;
	    case R.id.invitesbutton:
	    	showInvites();
	        return true;
	    case R.id.accountbutton:    
	        showAccountMenu();
	    	return true;
	    case R.id.settingsbutton:    
	        showSettings();
	    	return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	
	/**
	 * Deze functie wordt aangeroepen wanneer de gebruiker het groups-scherm probeert te openen
	 * De functie start een nieuwe activity, van het type SettingsScreen
	 */
	private void showGroupsMenu() {
		Intent intent = new Intent(instance, GroupsScreen.class);
		instance.startActivityForResult(intent, Mapp.GROUPSSCREEN_ACTIVITYCODE);
	}

	/**
	 * Deze functie wordt aangeroepen wanneer de gebruiker het accountscherm probeert te openen
	 * De functie start een nieuwe activity, van het type SettingsScreen
	 */
	private void showAccountMenu() {
		Intent intent = new Intent(instance, AccountScreen.class);
		Bundle bundle = new Bundle();
		bundle.putString(AccountScreen.USERNAME_KEY, settings.getString("username", "Joe"));
		intent.putExtras(bundle);
		instance.startActivityForResult(intent, Mapp.ACCOUNTSCREEN_ACTIVITYCODE);
	}
	
	/**
	 * Deze functie wordt aangeroepen wanneer de gebruiker het invitesmenu probeert te openen
	 * De functie start een nieuwe activity, van het type SettingsScreen
	 */
	private void showInvites() {
		Intent intent = new Intent(instance, InvitesScreen.class);
		instance.startActivityForResult(intent, Mapp.INVITESSCREEN_ACTIVITYCODE);
	}

	/**
	 * Deze functie wordt aangeroepen wanneer de gebruiker het settingsmenu probeert te openen
	 * De functie start een nieuwe activity, van het type SettingsScreen
	 */
	private void showSettings() {
		Intent intent = new Intent(instance, SettingsScreen.class);
		
		//We maken een nieuwe bundle om data in mee te sturen
		Bundle bundle = new Bundle();

		//De data wordt aan de bundle toegevoegd
		bundle.putBoolean(SettingsScreen.SATMODE_KEY, mapView.isSatellite());
		bundle.putBoolean(SettingsScreen.ZOOMCONTROLS_KEY, settings.getBoolean("zoomControls", true));
		
		bundle.putInt(SettingsScreen.SYNCINTERVAL_KEY, syncInterval);
		//En we voegen de bundle bij de intent
		intent.putExtras(bundle);
 
		//We starten een nieuwe Activity
		instance.startActivityForResult(intent, Mapp.SETTINGSSCREEN_ACTIVITYCODE);
	}
	
	/**
	 * Deze functie kijkt of er op dit moment een gebruiker ingelogd is
	 * @return true indien er een gebruiker ingelogd is
	 */
	private boolean isLoggedIn() {
		Log.v(TAG, "Checking if logged");
		return settings.getString("username", null) != null && settings.getString("password", null) != null;
	}
	
	/**
	 * Deze functie wordt aangeroepen wanneer de gebruiker nog niet ingelogd is
	 * en we dus naar het loginscherm moeten
	 */
	private void showLoginScreen() {
		Intent intent = new Intent(instance, LoginScreen.class);
		instance.startActivityForResult(intent, Mapp.LOGINSCREEN_ACTIVITYCODE);
	}
}
