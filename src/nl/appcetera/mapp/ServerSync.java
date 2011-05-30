package nl.appcetera.mapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
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
			HttpClient httpclient = new DefaultHttpClient();
			
			HttpGet httpget = new HttpGet("http://192.168.2.4/MVics/Mappserver/v1/polygons/group_id/" + OverlayManager.getGroupId());
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials("test@example.com", "098f6bcd4621d373cade4e832627b4f6");
			
			try
			{
				httpget.addHeader(new BasicScheme().authenticate(creds, httpget));
			} 
			catch (AuthenticationException e1)
			{
				return "Server authentication failed";
			}

			String result = null;
			
		    try
		    {
		        // Execute HTTP GET Request
		        HttpResponse response = httpclient.execute(httpget);
		        InputStream is = response.getEntity().getContent();
		        
		        // Lees de uitvoer
			    BufferedReader r = new BufferedReader(new InputStreamReader(is));
			    StringBuilder total = new StringBuilder();
			    String line;
			    while((line = r.readLine()) != null)
			    {
			        total.append(line);
			    }
			    
			    result = total.toString();
		    }
		    catch (ClientProtocolException e)
		    {
		    	e.printStackTrace();
		        return e.getMessage();
		    }
		    catch (IOException e)
		    {
		    	e.printStackTrace();
		        return e.getMessage();
		    }
		    
		    Log.v(Mapp.TAG, result);
			
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
