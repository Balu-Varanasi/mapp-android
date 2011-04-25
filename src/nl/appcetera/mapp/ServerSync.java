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
	public ServerSync(PolygonData db, Context c)
	{
		this.db = db;
		ServerSync.c = c;
	}
	
	public void startSync()
	{
		enable = true;
		syncHandler.removeCallbacks(this);
		syncHandler.post(this);
	}
	
	public void run()
	{
		new DataSyncTask().execute(db);
        syncHandler.postDelayed(this, 60000);
	}
	
	private class DataSyncTask extends AsyncTask<PolygonData, Void, String>
	{
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
