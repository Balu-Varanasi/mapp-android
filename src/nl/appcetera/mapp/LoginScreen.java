package nl.appcetera.mapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Klasse die het login-scherm beheert, waar de gebruiker kan registreren of inloggen
 * @author Joost
 */
public class LoginScreen extends Activity {

	public static final int RESULT_OK = 42;

	public static final String USERNAME_KEY = "USERNAME";
	public static final String PASSWORD_KEY = "PASSWORD";
	EditText usernameField;
	EditText passwordField;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.loginscreen);  
		
		usernameField = (EditText) findViewById(R.id.input_username);
		passwordField = (EditText) findViewById(R.id.input_password);
	
		if (Mapp.LOGIN_DISABLED) {
			usernameField.setText("broodje_kroket@student.ru.nl");
			passwordField.setText("boe");
			confirmLogin();
		}
		
		final Button loginbutton = (Button) findViewById(R.id.login_loginbutton);
		loginbutton.setOnClickListener(new View.OnClickListener() {
			/**
			 * Tappen op de loginbutton zorgt ervoor dat de activity wordt getermineerd
			 * en de username en password worden opgeleverd in een Bundle
			 */
			public void onClick(View v) {
				String username = usernameField.getText().toString();
				username.replace("@", "%40");
				String password = passwordField.getText().toString();
				if (username == "" || username == null) {
					toastMessage("Please fill in an email address");
				}
				else if (password == "" || password == null) {
					toastMessage("Please fill in a password");
				}
				else
				{
					String loginError = null;
					try
					{
						validCredentials(username, md5(password));
						confirmLogin();
						return;
					}
					catch(SyncException e)
					{
						loginError = e.getMessage();
					}
					String result = accountExists(username);
					if (result == "unregistered")
					{
						try
						{
							registerAccount(username, password);
							confirmLogin();
							return;
						}
						catch (SyncException e)
						{
							toastMessage("Your account could not be registered.");
						}
					}
					else
					{
						if (result != "registered")
						{
							toastMessage(result);
						}
						else
						{
							toastMessage(loginError);
						}
					}
				}
            }
		});
	}
	
	/**
	 * Rondt de loginprocedure af door het e=mailadres en wachtwoord op te slaan in de SharedPreferences
	 * en de activity te termineren
	 */
	private void confirmLogin() {
		SharedPreferences settings = getSharedPreferences(Mapp.SETTINGS_KEY, MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		editor.putString("username", usernameField.getText().toString().toLowerCase());
		editor.putString("password", md5(passwordField.getText().toString().toLowerCase()));
		editor.commit();
    	
    	Intent mIntent = new Intent();
    	setResult(RESULT_OK, mIntent);
    	finish();
	}
	/**
	 * Maakt een md5-string
	 * @param s string om te md5'en
	 * @return md5 van gegeven string
	 * @source http://www.androidsnippets.com/create-a-md5-hash-and-dump-as-a-hex-string
	 */
	private String md5(String s) 
	{
		try
	    {
	        // Create MD5 Hash
	        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
	        digest.update(s.getBytes());
	        byte messageDigest[] = digest.digest();
	        
	        // Create Hex String
	        StringBuffer hexString = new StringBuffer();
	        for (int i=0; i<messageDigest.length; i++)
	        {
	        	if(((int) messageDigest[i] & 0xff) < 0x10)
	        	{
	        		hexString.append("0");
	        	}
	        	
	        	hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
	        }
	        return hexString.toString();
	        
	    }
		catch (NoSuchAlgorithmException e)
		{
	        e.printStackTrace();
	    }
		
	    return "";
	}
	
	/**
	 * Functie die controleert of een e-mailadres al aan een account gekoppeld is
	 * @param email het e-mailadres dat we willen verifi�ren
	 * @return String "unregistered" als het account niet bestaat, anders een foutmelding
	 */
	private String accountExists(String email) {
		
		HttpGet httpg;
		try
		{
			httpg = new HttpGet(SyncClient.serverUrl + "is_registered/email/" + 
					URLEncoder.encode(email.toLowerCase(), "UTF8"));
		}
		catch (UnsupportedEncodingException e1)
		{
			return "Encoding error";
		}	
	    HttpResponse response;
	        
	    try
	    {
	    	response = SyncClient.getClient().execute(httpg);
				
			// Lees het resultaat van de actie in
			InputStream is = response.getEntity().getContent();
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			StringBuilder total = new StringBuilder();
			String line;
			while((line = r.readLine()) != null)
			{
			    total.append(line);
			}

			if(response.getStatusLine().getStatusCode() == 404)
			{
				return "Unknown error: unable to check e-mail existance";
			}
			else if(response.getStatusLine().getStatusCode() != 200)
		    {
				// Er is iets mis gegaan.
				JSONObject result = new JSONObject(total.toString());
				Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
			    return (result.getString("message"));
		    }
			else
			{
				// Alles is blijkbaar goed gegaan
				JSONArray result = new JSONArray(total.toString());
				JSONObject jsonResult = result.getJSONObject(0);
				return (jsonResult.getBoolean("registered") ? "registered" : "unregistered");
			}
		}
	    catch (ClientProtocolException e)
	    {
			Log.e(Mapp.TAG, e.getMessage());
			return "Epic HTTP failure";
		}
	    catch (IOException e)
	    {
	       	Log.e(Mapp.TAG, e.getMessage());
	       	return "Error during server synchronisation";
		}
	    catch (JSONException e)
		{
			Log.e(Mapp.TAG, "Sync failed. Response is no valid JSON or expected variable not found.");
			return "Invalid server response";
		}
	}
	
	/**
	 * Functie die controleert of een combinatie van e-mail en password toegang geeft tot dit account
	 * @param username het e-mailadres wat we willen controleren
	 * @param password het bijbehorende wachtwoord
	 * @return true indien de combinatie e-mail en password klopt
	 * @throws SyncException 
	 */
	private void validCredentials(String username, String password) throws SyncException
	{
		HttpGet httpg = new HttpGet(SyncClient.serverUrl + "user/");
		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username.toLowerCase(), password.toLowerCase());
		try
		{
			httpg.addHeader(new BasicScheme().authenticate(creds, httpg));
		} 
		catch (AuthenticationException e1)
		{
			Log.e(Mapp.TAG, e1.getMessage());
			throw new SyncException("Authentication failed. Please try again.");
		}
		
		HttpResponse response;
		       
		try
		{
			response = SyncClient.getClient().execute(httpg);
			
			if(response.getStatusLine().getStatusCode() == 401)
			{
				throw new SyncException("Authentication failed. Please try again.");
			}
			else if(response.getStatusLine().getStatusCode() != 200)
			{
				throw new SyncException("Server error. Please try again later.");
			}
			else
			{
				return;
			}
		}
		catch (ClientProtocolException e) 
		{
			e.printStackTrace();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
		throw new SyncException("Authentication failed. Please try again.");
	}
	
	private void registerAccount(String username, String password) throws SyncException
	{
		HttpPut httpp = new HttpPut(SyncClient.serverUrl + "new_user");
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("email", username.toLowerCase()));
        nameValuePairs.add(new BasicNameValuePair("password", md5(password.toLowerCase())));
		
        HttpResponse response;
        
        try
        {
			response = SyncClient.getClient().execute(httpp);
			
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
		    else if(response.getStatusLine().getStatusCode() != 200)
	        {
				// Er is iets mis gegaan.
		        Log.e(Mapp.TAG, "Sync error: " + result.getString("message"));
		        throw new SyncException(result.getString("message"));
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
        	throw new SyncException("Error during server synchronisation");
		}
        catch (JSONException e)
		{
			Log.e(Mapp.TAG, "Sync failed. Response is no valid JSON or expected variable not found.");
			throw new SyncException("Invalid server response");
		}
	}
	
	/**
	 * Deze methode toont een toastberichtje - vooral een shortcode omdat 't anders steeds 3 regels kost
	 * @param text het bericht dat getoond moet worden
	 */
	private void toastMessage(CharSequence text) {
		Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
		toast.show();
	}
}
 