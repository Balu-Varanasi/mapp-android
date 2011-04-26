package nl.appcetera.mapp;

import android.content.Context;
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
	
	/**
	 * Constructor
	 * @param db
	 */
	public ServerSync(Context c)
	{
		this.db = new PolygonData(c);
		ServerSync.c = c;
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
	}
	
	/**
	 * Voert één synchronisatiecyclus uit
	 */
	public void run()
	{
		//if(enable)
		{
			new DataSyncTask().execute(db);
			syncHandler.postDelayed(this, 60000);
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
			try
			{
				Thread.sleep(20000);
			} 
			catch (InterruptedException e)
			{
				e.printStackTrace();
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
				CharSequence text = "Sync complete";
				int duration = Toast.LENGTH_LONG;
				Toast toast = Toast.makeText(ServerSync.c, text, duration);
				toast.show();
			}
		}
	}
}
