package nl.appcetera.mapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;

/**
 * Synchronisatie-client
 * Meer een handige klasse om wat structuur aan te brengen in het appcetera-synchronisatie-oerwoud.
 * @author Mathijs
 *
 */
public class SyncClient
{
	private PolygonData db;
	private HttpClient httpclient;
	private String mappUser = "test@example.com";
	private String mappPass = "098f6bcd4621d373cade4e832627b4f6";
	//private static final String serverUrl = "http://192.168.2.2/MVics/Mappserver/v1/";
	private static final String serverUrl = "http://mapp.joelcox.org/v1/";
	private static final boolean development = true;
	private String error = "";
	
	/**
	 * Constructor
	 * @param db instantie van de databaseklasse
	 */
	public SyncClient(PolygonData db)
	{
		this.httpclient = new DefaultHttpClient();
		this.db = db;
	}
	
	/**
	 * Voert synchronisatie uit voor de gegeven groep
	 * @param group de groep om te syncen
	 * @return true indien synchronisatie geslaagd is, false bij problemen
	 */
	public synchronized boolean sync(int group, SharedPreferences settings)
	{
		if(development)
		{
			return true;
		}
		
		try
		{
			deletePolygons(group);
			putPolygons(group);
			postPolygons(group);
			getPolygons(group, settings.getLong("lastSync", 0));
			settings.edit().putLong("lastSync", System.currentTimeMillis()/1000).commit();
		}
		catch(SyncException s)
		{
			this.error = s.getMessage();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Geeft de laatst gegenereerde error terug
	 * @return het laatste foutbericht
	 */
	public String getError()
	{
		return error;
	}
	
	/**
	 * Synchroniseert verwijderde polygonen met de server
	 * @param group groupid om polygonen uit te syncen
	 * @throws SyncException 
	 * TODO polygonen die niet meer op de server zijn verwijderen
	 */
	private void deletePolygons(int group) throws SyncException
	{
		Cursor c = db.getRemovedPolygons(group);
		int polygonid = 0;
		
		if(!c.moveToFirst())
		{
			return; // Niks te syncen, dus gelijk klaar!
		}
		
		do
		{
			polygonid = c.getInt(0);
			HttpDelete httpd = new HttpDelete(serverUrl + "polygon/?id=" + polygonid);
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(mappUser, mappPass);
			
			try
			{
				httpd.addHeader(new BasicScheme().authenticate(creds, httpd));
			} 
			catch (AuthenticationException e1)
			{
				Log.e(Mapp.TAG, e1.getStackTrace().toString());
				throw new SyncException("Authentication failed");
			}
			
	        HttpResponse response;
	        
	        try
	        {
				response = httpclient.execute(httpd);
				
				if(response.getStatusLine().getStatusCode() == 418)
				{
					throw new SyncException("Unable to synchronize because the server is a teapot.");
				}
				else if(response.getStatusLine().getStatusCode() != 200)
		        {
					// Er is iets mis gegaan.
		        	// Lees de uitvoer
		        	InputStream is = response.getEntity().getContent();
				    BufferedReader r = new BufferedReader(new InputStreamReader(is));
				    StringBuilder total = new StringBuilder();
				    String line;
				    while((line = r.readLine()) != null)
				    {
				        total.append(line);
				    }

				    JSONObject result = null;
				    result = new JSONObject(total.toString());
			        Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			        throw new SyncException(result.getString("message"));
		        }
				else
				{
					db.removeRemovedPolygon(polygonid);
				}
			}
	        catch (ClientProtocolException e)
	        {
				Log.e(Mapp.TAG, e.getMessage());
				throw new SyncException("Epic HTTP failure");
			}
	        catch (IOException e)
	        {
	        	Log.e(Mapp.TAG, e.getMessage());
	        	throw new SyncException("Exception during server synchronisation");
			}
	        catch (JSONException e)
			{
				Log.e(Mapp.TAG, "Sync failed. Getting status message from JSON response failed.");
				throw new SyncException("Invalid server response");
			}
		}
		while(c.moveToNext());
	}
	
	/**
	 * Synchroniseert nieuwe polygonen met de server
	 * @param group het id van de groep waaruit polygonen gesynct moeten worden
	 * @throws SyncException 
	 */
	private void putPolygons(int group) throws SyncException
	{
		Cursor c = db.getNewPolygons(group);
		int polygonid 	= 0;
		String name 	= "";
		String color 	= "";
		String desc		= "";
		
		if(!c.moveToFirst())
		{
			return; // Niks te syncen, dus gelijk klaar!
		}
		
		do
		{
			HttpPut httpp = new HttpPut(serverUrl + "polygon");
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(mappUser, mappPass);
			
			polygonid 	= c.getInt(0);
			name 		= c.getString(2);
			color		= c.getString(1);
			desc		= c.getString(3);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("name", name));
	        nameValuePairs.add(new BasicNameValuePair("group_id", group + ""));
	        nameValuePairs.add(new BasicNameValuePair("color", color));
	        nameValuePairs.add(new BasicNameValuePair("description", desc));

	        // Nu de punten van de polygoon ophalen en die bijvoegen
	        Cursor points = db.getAllPolygonPoints(polygonid);
	        if(!points.moveToFirst())
	        {
	        	// Hier klopt iets niet, de polygoon heeft geen punten!
	        	db.removePolygon(polygonid, true);
	        	return;
	        }
	        
	        do
	        {
	        	nameValuePairs.add(new BasicNameValuePair("points[]", 
	        			points.getString(0) + "," 
	        			+ points.getString(1) + "," 
	        			+ points.getString(2)));
	        }
	        while(points.moveToNext());
			
			try
			{
				httpp.addHeader(new BasicScheme().authenticate(creds, httpp));
				httpp.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			} 
			catch (AuthenticationException e1)
			{
				Log.e(Mapp.TAG, e1.getStackTrace().toString());
				throw new SyncException("Authentication failed");
			}
			catch (UnsupportedEncodingException e)
			{
				Log.e(Mapp.TAG, e.getStackTrace().toString());
				throw new SyncException("Failed to encode data");
			}
			
	        HttpResponse response;
	        
	        try
	        {
				response = httpclient.execute(httpp);
				
				// Lees het resultaat van de actie in
				JSONObject result = null;
				InputStream is = response.getEntity().getContent();
			    BufferedReader r = new BufferedReader(new InputStreamReader(is));
			    StringBuilder total = new StringBuilder();
			    String line;
			    while((line = r.readLine()) != null)
			    {
			        total.append(line);
			    }
Log.v("APC",total.toString());
			    result = new JSONObject(total.toString());
				
			    if(response.getStatusLine().getStatusCode() == 418)
				{
					throw new SyncException("Unable to synchronize because the server is a teapot.");
				}
			    else if(response.getStatusLine().getStatusCode() != 200)
		        {
					// Er is iets mis gegaan.
			        Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			        throw new SyncException(result.getString("message"));
		        }
				else
				{
					// De polygoon een nieuw id geven en het 'nieuw'-vlaggetje verwijderen
					db.updatePolygonId(polygonid, result.getInt("polygon_id"));
					db.setPolygonIsSynced(result.getInt("polygon_id"));
				}
			}
	        catch (ClientProtocolException e)
	        {
				Log.e(Mapp.TAG, e.getMessage());
				throw new SyncException("Epic HTTP failure");
			}
	        catch (IOException e)
	        {
	        	Log.e(Mapp.TAG, e.getMessage());
	        	throw new SyncException("Exception during server synchronisation");
			}
	        catch (JSONException e)
			{
				Log.e(Mapp.TAG, "Sync failed. Response is no valid JSON or expected variable not found.");
				throw new SyncException("Invalid server response");
			}
		}
		while(c.moveToNext());
	}
	
	/**
	 * Synchroniseert gewijzigde polygonen met de server
	 * @param group het id van de groep waaruit polygonen gesynct moeten worden
	 * @throws SyncException 
	 */
	private void postPolygons(int group) throws SyncException
	{
		Cursor c = db.getChangedPolygons(group);
		int polygonid 	= 0;
		String name 	= "";
		String color 	= "";
		String desc		= "";
		
		if(!c.moveToFirst())
		{
			return; // Niks te syncen, dus gelijk klaar!
		}
		
		do
		{
			HttpPost httpp = new HttpPost(serverUrl + "polygon");
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(mappUser, mappPass);
			
			polygonid 	= c.getInt(0);
			name 		= c.getString(2);
			color		= c.getString(1);
			desc		= c.getString(3);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
			nameValuePairs.add(new BasicNameValuePair("id", polygonid + ""));
	        nameValuePairs.add(new BasicNameValuePair("name", name));
	        nameValuePairs.add(new BasicNameValuePair("group_id", group + ""));
	        nameValuePairs.add(new BasicNameValuePair("color", color));
	        nameValuePairs.add(new BasicNameValuePair("description", desc));

	        // Nu de punten van de polygoon ophalen en die bijvoegen
	        Cursor points = db.getAllPolygonPoints(polygonid);
	        if(!points.moveToFirst())
	        {
	        	// Hier klopt iets niet, de polygoon heeft geen punten!
	        	db.removePolygon(polygonid, true);
	        	return;
	        }
	        
	        do
	        {
	        	nameValuePairs.add(new BasicNameValuePair("points[]", 
	        			points.getString(0) + "," 
	        			+ points.getString(1) + "," 
	        			+ points.getString(2)));
	        }
	        while(points.moveToNext());
			
			try
			{
				httpp.addHeader(new BasicScheme().authenticate(creds, httpp));
				httpp.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			} 
			catch (AuthenticationException e1)
			{
				Log.e(Mapp.TAG, e1.getMessage());
				throw new SyncException("Authentication failed");
			}
			catch (UnsupportedEncodingException e)
			{
				Log.e(Mapp.TAG, e.getMessage());
				throw new SyncException("Failed to encode data");
			}
			
	        HttpResponse response;
	        
	        try
	        {
				response = httpclient.execute(httpp);
				
				if(response.getStatusLine().getStatusCode() == 418)
				{
					throw new SyncException("Unable to synchronize because the server is a teapot.");
				}
				else if(response.getStatusLine().getStatusCode() != 200)
		        {
					// Er is iets mis gegaan.
					
					// Lees het resultaat van de actie in
					JSONObject result = null;
					InputStream is = response.getEntity().getContent();
				    BufferedReader r = new BufferedReader(new InputStreamReader(is));
				    StringBuilder total = new StringBuilder();
				    String line;
				    while((line = r.readLine()) != null)
				    {
				        total.append(line);
				    }
				    
				    result = new JSONObject(total.toString());
				    
			        Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			        throw new SyncException(result.getString("message"));
		        }
				else
				{
					db.setPolygonIsSynced(polygonid);
				}
			}
	        catch (ClientProtocolException e)
	        {
				Log.e(Mapp.TAG, e.getMessage());
				throw new SyncException("Epic HTTP failure");
			}
	        catch (IOException e)
	        {
	        	Log.e(Mapp.TAG, e.getMessage());
	        	throw new SyncException("Exception during server synchronisation");
			}
	        catch (JSONException e)
			{
				Log.e(Mapp.TAG, "Sync failed. Response is no valid JSON or expected variable not found.");
				throw new SyncException("Invalid server response");
			}
		}
		while(c.moveToNext());
	}
	
	/**
	 * Synchroniseert gewijzigde polygonen met de server
	 * @param group het id van de groep waaruit polygonen gesynct moeten worden
	 * @throws SyncException 
	 */
	private void getPolygons(int group, long lastSync) throws SyncException
	{		
		HttpGet httpg = new HttpGet(serverUrl + "polygons/group_id/" + group);
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(mappUser, mappPass);

		try
		{
			httpg.addHeader(new BasicScheme().authenticate(creds, httpg));
		} 
		catch (AuthenticationException e1)
		{
			Log.e(Mapp.TAG, e1.getMessage());
			throw new SyncException("Authentication failed");
		}
		
	    HttpResponse response;
	        
	    try
	    {
	    	response = httpclient.execute(httpg);
				
			// Lees het resultaat van de actie in
			JSONArray result = null;
			InputStream is = response.getEntity().getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder total = new StringBuilder();
			String line;
			while((line = r.readLine()) != null)
			{
			    total.append(line);
			}
			result = new JSONArray(total.toString());
			
			if(response.getStatusLine().getStatusCode() == 418)
			{
				throw new SyncException("Unable to synchronize because the server is a teapot.");
			}
			else if(response.getStatusLine().getStatusCode() == 404)
			{
				// Geen polygonen hier
				db.removePolygonsFromGroup(group, false);
			}
			else if(response.getStatusLine().getStatusCode() != 200)
		    {
				// Er is iets mis gegaan.
				Log.e(Mapp.TAG, "Sync error: " + result.getString(0));
			    throw new SyncException(result.getString(0));
		    }
			else
			{
				// Alles is blijkbaar goed gegaan
				// Loop over de polygonen heen
				for(int i = 0; i < result.length(); i++)
				{
					JSONObject polygon = result.getJSONObject(i);

					if(polygon.getInt("created") > lastSync)
					{
						// Deze polygoon is nieuw of gewijzigd dus we updaten 'm of voeren 'm in
						db.addPolygonFromServer(polygon.getInt("id"), polygon.getInt("group_id"), polygon.getInt("color"),
								polygon.getString("name"), polygon.optString("description"), polygon.getLong("created"));
						
						// Nu de punten invoeren
						JSONArray points = result.getJSONObject(i).getJSONArray("points");
						for(int j = 0; j < points.length(); j++)
						{
							JSONObject point = points.getJSONObject(j);
							db.addPolygonPoint(polygon.getInt("id"), point.getLong("latitude"), 
									point.getLong("longitude"), point.getInt("order"));
						}
					}
				}
			}
		}
	    catch (ClientProtocolException e)
	    {
			Log.e(Mapp.TAG, e.getMessage());
			throw new SyncException("Epic HTTP failure");
		}
	    catch (IOException e)
	    {
	       	Log.e(Mapp.TAG, e.getMessage());
	       	throw new SyncException("Exception during server synchronisation");
		}
	    catch (JSONException e)
		{
			Log.e(Mapp.TAG, "Sync failed. Response is no valid JSON or expected variable not found.");
			throw new SyncException("Invalid server response");
		}
	}
}
