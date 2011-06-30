package nl.appcetera.mapp;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;

/**
 * Klasse voor synchronisatie met de server
 * @author Mathijs
 *
 */
public class ServerSync implements Runnable
{
	private PolygonData db;
	private static Context c;
	private boolean enable = false;
	private Handler syncHandler = new Handler();
	private SyncClient syncClient;
	
	/**
	 * Constructor
	 * @param db
	 */
	public ServerSync(Context c, PolygonData db)
	{
		this.db = db;
		ServerSync.c = c;
		this.syncClient = new SyncClient(db);
	}
	
	/**
	 * Start het synchroniseren
	 */
	public void startSync()
	{
		enable = true;
		syncHandler.removeCallbacks(this);
		syncHandler.post(this);
	}
	
	/**
	 * Stopt het synchroniseren
	 */
	public void stopSync()
	{
		enable = false;
		syncHandler.removeCallbacks(this);
	}
	
	/**
	 * Voert ŽŽn synchronisatiecyclus uit
	 */
	public void run()
	{
		if(enable)
		{
			if(this.deviceIsOnline())
			{
				new DataSyncTask().execute(db);
				syncHandler.postDelayed(this, Mapp.syncInterval);
			}
			else
			{
				CharSequence text = "Device offline, next synchronisation attempt in " + (Mapp.offlineRetryInterval/60000) + " minutes";
				Toast toast = Toast.makeText(ServerSync.c, text, Toast.LENGTH_LONG);
				toast.show();
				syncHandler.postDelayed(this, Mapp.offlineRetryInterval);
			}
		}
	}
	
	/**
	 * De werkelijke synchronisatie vindt, in een aparte thread, plaats in deze klasse
	 * @author Mathijs
	 *
	 */
	private class DataSyncTask extends AsyncTask<PolygonData, Void, String>
	{		
		/**
		 * Sync op de achtergrond
		 */
		@Override
		protected String doInBackground(PolygonData... db)
		{
			if(!syncClient.sync(OverlayManager.getGroupId(), Mapp.instance.settings))
			{
				return syncClient.getError();
			}
			return "Ok";
		}
		
		/**
		 * Wordt aangeroepen wanneer sync klaar is.
		 * Deze methode draait in de UI thread en is dus 'blocking'
		 */
		@Override
		protected void onPostExecute(String result)
		{
			if(result == "Ok")
			{				
				Mapp.reload();
				CharSequence text = "Sync complete";
				int duration = Toast.LENGTH_LONG;
				Toast toast = Toast.makeText(ServerSync.c, text, duration);
				toast.show();
			}
			else
			{
				CharSequence text = "Error: " + result;
				int duration = Toast.LENGTH_LONG;
				Toast toast = Toast.makeText(ServerSync.c, text, duration);
				toast.show();
			}
		}
	}
	
	/**
	 * Checkt of de telefoon online is
	 * @return true indien online
	 */
	protected boolean deviceIsOnline()
	{
		ConnectivityManager cm = (ConnectivityManager) Mapp.instance.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = cm.getActiveNetworkInfo();
	    return (netInfo != null && netInfo.isConnectedOrConnecting());
	}
}
