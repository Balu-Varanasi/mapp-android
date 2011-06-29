package nl.appcetera.mapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
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
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
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
	private String mappUser = "";
	private String mappPass = "";
	//public static final String serverUrl = "http://192.168.2.4/MVics/Mappserver/v1/";
	public static final String serverUrl = "http://mapp.joelcox.org/v1/";
	private static final boolean development = false;
	private String error = "";
	private static HttpClient httpclient = null;
	
	/**
	 * Geeft een (Thread-safe) instantie van de httpclient
	 * @return instantie van httpclient
	 */
	public static HttpClient getClient()
	{
		// Er mag maar één instantie van de thread-safe httpclient zijn, dus we maken er ook maar één
		if(SyncClient.httpclient == null)
		{
			// Bron van onderstaande:
			// http://svn.apache.org/repos/asf/httpcomponents/httpclient/branches/4.0.x/httpclient/src/examples/org/apache/http/examples/client/ClientMultiThreadedExecution.java
			
			// Create and initialize HTTP parameters
	        HttpParams params = new BasicHttpParams();
	        ConnManagerParams.setMaxTotalConnections(params, 100);
	        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	        
	        // Create and initialize scheme registry 
	        SchemeRegistry schemeRegistry = new SchemeRegistry();
	        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	        
	        // Create an HttpClient with the ThreadSafeClientConnManager.
	        // This connection manager must be used if more than one thread will
	        // be using the HttpClient.
	        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
	        SyncClient.httpclient = new DefaultHttpClient(cm, params);
		}
		
		return SyncClient.httpclient;
	}
	
	/**
	 * Constructor
	 * @param db instantie van de databaseklasse
	 */
	public SyncClient(PolygonData db)
	{
		
		SyncClient.httpclient = SyncClient.getClient();
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
		
		mappUser = settings.getString("username", "");
		mappPass = settings.getString("password", "");
		
		try
		{
			int syncTime = getSyncTimeStamp();
			deleteGroups();
			putGroups();
			updateGroups();
			deleteMemberships();
			putMemberships();
			postMemberships();
			deletePolygons(group);
			removeDeletedPolygons(group);
			putPolygons(group);
			postPolygons(group);
			getPolygons(group, settings.getLong("lastSync", 1));
			settings.edit().putLong("lastSync", syncTime).commit();
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

			HttpDelete httpd = new HttpDelete(serverUrl + "polygon/id/" + polygonid);
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
				else if(response.getStatusLine().getStatusCode() == 401)
				{
					throw new SyncException("Not authorized");
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
		
		/**
		 * Als er tijdens het syncen een andere polygoon een ander id krijgt (omdat zijn
		 * id nodig was), klopt onze cursor misschien niet meer dus moeten we opnieuw query'en.
		 */
		boolean requery = false;
		
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

			    result = new JSONObject(total.toString());
				
			    if(response.getStatusLine().getStatusCode() == 418)
				{
					throw new SyncException("Unable to synchronize because the server is a teapot.");
				}
			    else if(response.getStatusLine().getStatusCode() == 401)
				{
					throw new SyncException("Not authorized");
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
					db.setPolygonIsSynced(polygonid);
					requery = db.updatePolygonId(polygonid, result.getInt("polygon_id"));
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
	        
	        if(requery)
	        {
	        	c = db.getNewPolygons(group); // Opnieuw laden omdat polygoonid's gewijzigd kunnen zijn inmiddels
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
				else if(response.getStatusLine().getStatusCode() == 401)
				{
					throw new SyncException("Not authorized");
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
	 * Verwijdert polygonen lokaal als ze niet meer op de server staan.
	 * Naam van deze methode (C) Joost
	 * Maar de inhoud van deze methode wel (C) Mathijs
	 * @param polygonIds een lijstje met polygonid's die op de server voorkomen (in deze groep)
	 * @throws SyncException 
	 */
	private void removeDeletedPolygons(int group) throws SyncException
	{
		Cursor c = db.getAllPolygons(group);
		if(!c.moveToFirst())
		{
			return; // Lokale db is al leeg
		}
		
		// Lijstje maken met alle id's op de server
		ArrayList<Integer> polygonIds = new ArrayList<Integer>();
		
		HttpGet httpg = new HttpGet(serverUrl + "polygons/group_id/" + group + "/since/1");
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
			InputStream is = response.getEntity().getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder total = new StringBuilder();
			String line;
			while((line = r.readLine()) != null)
			{
			    total.append(line);
			}

			if(response.getStatusLine().getStatusCode() == 418)
			{
				throw new SyncException("Unable to synchronize because the server is a teapot.");
			}
			else if(response.getStatusLine().getStatusCode() == 401)
			{
				throw new SyncException("Not authorized");
			}
			else if(response.getStatusLine().getStatusCode() == 404)
			{
				// Geen polygonen hier
				do
			    {
			    	if(!(c.getInt(3) == 1))
			    	{
			    		db.removePolygon(c.getInt(0), false);
			    	}
			    }
			    while(c.moveToNext());
				return;
			}
			else if(response.getStatusLine().getStatusCode() != 200)
		    {
				// Er is iets mis gegaan.
				JSONObject result = new JSONObject(total.toString());
				Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			    throw new SyncException(result.getString("message"));
		    }
			else
			{
				// Alles is blijkbaar goed gegaan
				JSONArray result = new JSONArray(total.toString());
				
				// Loop over de polygonen heen
				for(int i = 0; i < result.length(); i++)
				{
					JSONObject polygon = result.getJSONObject(i);
					polygonIds.add(polygon.getInt("id"));
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
	    
	    // Nu we eindelijk alle id's hebben, kijken of er in de lokale db id's voorkomen die niet in onze nieuwe lijst zitten
	    do
	    {
	    	if(!polygonIds.contains(c.getInt(0)) && c.getInt(3) != 1)
	    	{
	    		db.removePolygon(c.getInt(0), false);
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
		HttpGet httpg = new HttpGet(serverUrl + "polygons/group_id/" + group + "/since/" + lastSync);
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
			InputStream is = response.getEntity().getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder total = new StringBuilder();
			String line;
			while((line = r.readLine()) != null)
			{
			    total.append(line);
			}

			if(response.getStatusLine().getStatusCode() == 418)
			{
				throw new SyncException("Unable to synchronize because the server is a teapot.");
			}
			else if(response.getStatusLine().getStatusCode() == 401)
			{
				throw new SyncException("Not authorized");
			}
			else if(response.getStatusLine().getStatusCode() == 404)
			{
				// Geen nieuwe polygonen hier
				return;
			}
			else if(response.getStatusLine().getStatusCode() != 200)
		    {
				// Er is iets mis gegaan.
				JSONObject result = new JSONObject(total.toString());
				Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			    throw new SyncException(result.getString("message"));
		    }
			else
			{
				// Alles is blijkbaar goed gegaan
				JSONArray result = new JSONArray(total.toString());
				
				// Loop over de polygonen heen
				for(int i = 0; i < result.length(); i++)
				{
					JSONObject polygon = result.getJSONObject(i);

					// Deze polygoon is nieuw of gewijzigd dus we updaten 'm of voeren 'm in
					db.addPolygonFromServer(polygon.getInt("id"), polygon.getInt("group_id"), polygon.getInt("color"),
							polygon.getString("name"), polygon.optString("description"), polygon.getLong("created"));
						
					// Nu de punten invoeren
					JSONArray points = result.getJSONObject(i).getJSONArray("points");
					for(int j = 0; j < points.length(); j++)
					{
						JSONObject point = points.getJSONObject(j);
						db.addPolygonPoint(polygon.getInt("id"), point.getLong("latitude"), 
								point.getLong("longitude"), point.getInt("order"), false);
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

	/**
	 * Geeft de huidige servertijd terug
	 * @return de huidige servertijd als unix-timestamp
	 * @throws SyncException 
	 */
	private int getSyncTimeStamp() throws SyncException
	{
		HttpGet httpg = new HttpGet(serverUrl + "time/");
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
			InputStream is = response.getEntity().getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder total = new StringBuilder();
			String line;
			while((line = r.readLine()) != null)
			{
			    total.append(line);
			}

			if(response.getStatusLine().getStatusCode() == 418)
			{
				throw new SyncException("Unable to synchronize because the server is a teapot.");
			}
			else if(response.getStatusLine().getStatusCode() != 200)
		    {
				// Er is iets mis gegaan.
				JSONObject result = new JSONObject(total.toString());
				Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			    throw new SyncException(result.getString("message"));
		    }
			else
			{
				// Alles is blijkbaar goed gegaan
				JSONObject result = new JSONObject(total.toString());
				return result.getInt("current_time");
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
	
	/**
	 * Werkt groepen bij
	 * @throws SyncException
	 */
	private void updateGroups() throws SyncException
	{		
	    // Alle groepen die nog wel lokaal staan, maar waar volgens de server de gebruiker niet meer in zit,
	    // keihard wegmikken.
	    Cursor c = db.getGroups();
	    if(!c.moveToFirst())
	    {
	    	return;// Snel klaar.
	    }
	    
	    // Checken welke groepen op de server aangemeld zijn
		HttpGet httpg = new HttpGet(serverUrl + "user");
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
	    ArrayList<Integer> groupsOnServer = new ArrayList<Integer>();
	        
	    try
	    {
	    	response = httpclient.execute(httpg);
				
			// Lees het resultaat van de actie in
			InputStream is = response.getEntity().getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder total = new StringBuilder();
			String line;
			while((line = r.readLine()) != null)
			{
			    total.append(line);
			}

			if(response.getStatusLine().getStatusCode() == 418)
			{
				throw new SyncException("Unable to synchronize because the server is a teapot.");
			}
			else if(response.getStatusLine().getStatusCode() == 401)
			{
				throw new SyncException("Not authorized");
			}
			else if(response.getStatusLine().getStatusCode() != 200)
		    {
				// Er is iets mis gegaan.
				JSONObject result = new JSONObject(total.toString());
				Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			    throw new SyncException(result.getString("message"));
		    }
			else
			{
				// Alles is blijkbaar goed gegaan
				JSONObject result = new JSONObject(total.toString());
				JSONArray groups = result.getJSONArray("groups");
				//TODO GET memberships
				for(int i = 0; i < groups.length(); i++)
				{
					JSONObject o = groups.getJSONObject(i);
					groupsOnServer.add(o.getInt("id"));
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
	    
	    // Alles verwijderen wat niet meer op de server zit
	    ArrayList<Integer> groupsInDb = new ArrayList<Integer>();
	    
	    do
	    {
	    	groupsInDb.add(c.getInt(0));
	    	if(!groupsOnServer.contains(c.getInt(0)))
	    	{
	    		groupsInDb.remove(new Integer(c.getInt(0)));
	    		db.removePolygonsFromGroup(c.getInt(0), false);
	    		db.deleteMemberships(c.getInt(0));
	    		db.removeGroup(c.getInt(0), false);
	    	}
	    }
	    while(c.moveToNext());
	    
	    // En nu alles toevoegen wat nog niet lokaal zit / updaten
	    for(int i : groupsOnServer)
	    {
	    	updateGroup(i, groupsInDb.contains(i));
	    }
	}
	
	/**
	 * Stuurt alle nieuwe polygonen naar de server
	 * @throws SyncException
	 */
	private void putGroups() throws SyncException
	{
		Cursor c = db.getNewGroups();
		String name = "";
		int id = 0;
		
		if(!c.moveToFirst())
		{
			return; // Niks te syncen, dus gelijk klaar!
		}

		/**
		 * Als er tijdens het syncen een andere groep een ander id krijgt (omdat zijn
		 * id nodig was), klopt onze cursor misschien niet meer dus moeten we opnieuw query'en.
		 */
		boolean requery = false;
		
		do
		{
			HttpPut httpp = new HttpPut(serverUrl + "group");
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(mappUser, mappPass);
			
			id   = c.getInt(0);
			name = c.getString(1);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
	        nameValuePairs.add(new BasicNameValuePair("name", name));
			
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

			    result = new JSONObject(total.toString());

			    if(response.getStatusLine().getStatusCode() == 418)
				{
					throw new SyncException("Unable to synchronize because the server is a teapot.");
				}
			    else if(response.getStatusLine().getStatusCode() == 401)
				{
					throw new SyncException("Not authorized");
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
					db.setGroupIsSynced(id);
					requery = db.updateGroupId(id, result.getInt("group_id"));
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
	        
	        if(requery)
	        {
	        	c = db.getNewGroups();// Opnieuw laden omdat polygoonid's gewijzigd kunnen zijn inmiddels
	        }
		}
		while(c.moveToNext());
	}
	
	/**
	 * Verwijdert groepen op de server die lokaal verwijderd zijn
	 * @throws SyncException
	 */
	private void deleteGroups() throws SyncException
	{
		Cursor c = db.getRemovedGroups();
		if(!c.moveToFirst())
		{
			return; // Dat ging snel.
		}
		
		do
		{
			int polygonid = c.getInt(0);
			HttpDelete httpd = new HttpDelete(serverUrl + "group/id/" + polygonid);
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
				else if(response.getStatusLine().getStatusCode() == 401)
				{
					throw new SyncException("Not authorized");
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
					db.removeRemovedGroup(polygonid);
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
	 * Haalt info over de gegeven groep op en slaat deze lokaal op
	 * @param i het id van de op te halen groep
	 * @param update of het een update betreft of een nieuwe polygoon is
	 * @throws SyncException 
	 */
	private void updateGroup(int id, boolean update) throws SyncException
	{
		HttpGet httpg = new HttpGet(serverUrl + "group/id/" + id);
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
			InputStream is = response.getEntity().getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder total = new StringBuilder();
			String line;
			while((line = r.readLine()) != null)
			{
			    total.append(line);
			}
			
			if(response.getStatusLine().getStatusCode() == 418)
			{
				throw new SyncException("Unable to synchronize because the server is a teapot.");
			}
			else if(response.getStatusLine().getStatusCode() == 401)
			{
				throw new SyncException("Not authorized");
			}
			else if(response.getStatusLine().getStatusCode() != 200)
		    {
				// Er is iets mis gegaan.
				JSONObject result = new JSONObject(total.toString());
				Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			    throw new SyncException(result.getString("message"));
		    }
			else
			{
				// Alles is blijkbaar goed gegaan
				JSONObject result = new JSONObject(total.toString());
				
				if(!update)
				{
					db.addGroupFromServer(id, result.getString("email"), result.getString("name"));
				}
				else
				{
					db.editGroupFromServer(id, result.getString("email"), result.getString("name"));
					db.deleteMemberships(id);
				}
				
				JSONArray members = result.getJSONArray("members");
				
				for(int i = 0; i < members.length(); i++)
				{
					JSONObject o = members.getJSONObject(i);
					db.addMembership(o.getString("email"), id, (o.getString("accepted").equals("true")), false);
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
	
	/**
	 * Stuurt nieuwe lidmaatschappen naar 't servertje
	 * @throws SyncException 
	 */
	private void putMemberships() throws SyncException
	{
		Cursor c = db.getNewMemberships();
		String email = "";
		int id = 0;
		
		if(!c.moveToFirst())
		{
			return; // Niks te syncen, dus gelijk klaar!
		}

		do
		{
			HttpPut httpp = new HttpPut(serverUrl + "membership");
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(mappUser, mappPass);
			
			id   = c.getInt(0);
			email = c.getString(1);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("email", email));
	        nameValuePairs.add(new BasicNameValuePair("group_id", id + ""));
	        
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

			    result = new JSONObject(total.toString());
				
			    if(response.getStatusLine().getStatusCode() == 418)
				{
					throw new SyncException("Unable to synchronize because the server is a teapot.");
				}
			    else if(response.getStatusLine().getStatusCode() == 401)
				{
					throw new SyncException("Not authorized");
				}
			    else if(response.getStatusLine().getStatusCode() != 200)
		        {
					// Er is iets mis gegaan.
			        Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			        throw new SyncException(result.getString("message"));
		        }
				else
				{
					// Het 'nieuw'-vlaggetje verwijderen
					db.setMembershipIsSynced(id, email);
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
	 * Verwijdert memberships op de server die lokaal verwijderd zijn
	 * @throws SyncException
	 */
	private void deleteMemberships() throws SyncException
	{
		Cursor c = db.getRemovedMemberships();
		if(!c.moveToFirst())
		{
			return; // Dat ging snel.
		}
		
		do
		{
			int groupid = c.getInt(0);
			HttpDelete httpd = new HttpDelete(serverUrl + "membership/id/" + groupid);
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
				else if(response.getStatusLine().getStatusCode() == 401)
				{
					throw new SyncException("Not authorized");
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
					db.removeRemovedMembership(groupid);
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
	 * Stuurt geaccepteerde memberships naar den servert
	 * @throws SyncException 
	 */
	private void postMemberships() throws SyncException
	{
		Cursor c = db.getChangedMemberships();
		int groupid 	= 0;
		
		if(!c.moveToFirst())
		{
			return; // Niks te syncen, dus gelijk klaar!
		}
		
		do
		{
			HttpPost httpp = new HttpPost(serverUrl + "polygon");
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(mappUser, mappPass);
			
			groupid 	= c.getInt(0);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("group_id", groupid + ""));

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
				else if(response.getStatusLine().getStatusCode() == 401)
				{
					throw new SyncException("Not authorized");
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
					db.setMembershipIsSynced(groupid, c.getString(1));
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
}
 
