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
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpParams;
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
	protected PolygonData db;
	protected HttpClient httpclient;
	protected String mappUser = "test@example.com";
	protected String mappPass = "098f6bcd4621d373cade4e832627b4f6";
	protected static final String serverUrl = "http://192.168.2.2/MVics/Mappserver/v1/";
	
	public SyncClient(PolygonData db)
	{
		@SuppressWarnings("unused")
		HttpClient httpclient = new DefaultHttpClient();
		this.db = db;
	}
	
	public String deletePolygons(int group)
	{
		Cursor c = db.getRemovedPolygons(group);
		c.moveToFirst();
		
		do
		{
			HttpDelete httpd = new HttpDelete(serverUrl + "polygon/?id=" + c.getInt(0));
			UsernamePasswordCredentials creds = new UsernamePasswordCredentials(mappUser, mappPass);
			
			try
			{
				httpd.addHeader(new BasicScheme().authenticate(creds, httpd));
			} 
			catch (AuthenticationException e1)
			{
				return "Authentication failed";
			}
			
	        HttpResponse response;
	        
	        try
	        {
				response = httpclient.execute(httpd);
				
				if(response.getStatusLine().getStatusCode() != 200)
		        {
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
		        }
			}
	        catch (ClientProtocolException e)
	        {
				Log.e(Mapp.TAG, e.getStackTrace().toString());
				return "Exception during server synchronisation";
			}
	        catch (IOException e)
	        {
	        	Log.e(Mapp.TAG, e.getStackTrace().toString());
	        	return "Exception during server synchronisation";
			}
	        catch (JSONException e)
			{
				Log.e(Mapp.TAG, "Sync failed. Getting status message from JSON response failed.");
				return "Exception during server synchronisation";
			}
		}
		while(c.moveToNext());
		
		return "Ok";
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
