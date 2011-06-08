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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

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
	private static final String serverUrl = "http://192.168.2.2/MVics/Mappserver/v1/";
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
	public boolean sync(int group)
	{
		try
		{
			deletePolygons(group);
			putPolygons(group);
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
	 * @return "Ok" indien alles goed ging, of een foutmelding als er iets mis was
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
				
				if(response.getStatusLine().getStatusCode() != 200)
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
				Log.e(Mapp.TAG, e.getStackTrace().toString());
				throw new SyncException("Epic HTTP failure");
			}
	        catch (IOException e)
	        {
	        	Log.e(Mapp.TAG, e.getStackTrace().toString());
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
	 * @return "Ok" indien alles goed ging, of een foutmelding indien er wat mis ging
	 * @throws SyncException 
	 */
	private void putPolygons(int group) throws SyncException
	{
		Cursor c = db.getNewPolygons(group);
		int polygonid 	= 0;
		String name 	= "";
		String color 	= "";
		
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
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("name", name));
	        nameValuePairs.add(new BasicNameValuePair("group_id", group + ""));
	        nameValuePairs.add(new BasicNameValuePair("color", color));

	        // Nu de punten van de polygoon ophalen en die bijvoegen
	        Cursor points = db.getAllPolygonPoints(polygonid);
	        if(!points.moveToFirst())
	        {
	        	// Hier klopt iets niet, de polygoon heeft geen punten!
	        	db.removePolygon(polygonid);
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
			    Log.v("APPC", total.toString());
			    result = new JSONObject(total.toString());
				
				if(response.getStatusLine().getStatusCode() != 200)
		        {
					// Er is iets mis gegaan.
			        Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			        throw new SyncException(result.getString("message"));
		        }
				else
				{
					db.updatePolygonId(polygonid, result.getInt("polygon_id"));
				}
			}
	        catch (ClientProtocolException e)
	        {
				Log.e(Mapp.TAG, e.getStackTrace().toString());
				throw new SyncException("Epic HTTP failure");
			}
	        catch (IOException e)
	        {
	        	Log.e(Mapp.TAG, e.getStackTrace().toString());
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
	 * Verstuurt alle opgeslagen polygonen uit de huidige groep naar de server
	 * @return http statuscode, of 0 indien er een andere fout optrad
	 */
	public int submitPolygons()
	{
		HttpPost httppost = new HttpPost("http://192.168.2.2/MVics/Mappserver/v1/polygon/");
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials("test@example.com", "098f6bcd4621d373cade4e832627b4f6");
		
		try
		{
			httppost.addHeader(new BasicScheme().authenticate(creds, httppost));
		} 
		catch (AuthenticationException e1)
		{
			return 0;
		}
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("name", "12345"));
        nameValuePairs.add(new BasicNameValuePair("color", "12345"));
        
        try
        {
			httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		}
        catch (UnsupportedEncodingException e)
		{
			return 0;
		}
		
        HttpResponse response;
        
        try
        {
			response = httpclient.execute(httppost);
		}
        catch (ClientProtocolException e)
        {
			return 0;
		}
        catch (IOException e)
        {
			return 0;
		}
        
		return response.getStatusLine().getStatusCode();
	}
}
