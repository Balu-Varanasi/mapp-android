package nl.appcetera.mapp;

import java.io.IOException;
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
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

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
	
	public SyncClient(PolygonData db)
	{
		@SuppressWarnings("unused")
		HttpClient httpclient = new DefaultHttpClient();
		this.db = db;
	}
	
	public String deletePolygons()
	{
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
