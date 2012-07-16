package cl.magnet.resty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.ExifInterface;
import android.os.Build;
import android.text.format.Time;
import android.util.Log;

public class RestClientBackup extends RestClient {

	public static final int PRODUCTION = 0;
	public static final int TESTING = 1;
	public static final int DEVELOPMENT = 2;
	public static final int WRONG_VIDEO_LENGTH_ERROR = 116;
	public static final String ME = "-1";
	public static final String TAG = "RestClient";

	private String URL;
	private String requestNewMediaIdURL;
	private String userLoginURL;
	private String userConnectURL;
	private String userRegistrationURL;
	private String userSeedURL;
	private String getEventTagsURL;
	private String cancelMediaURL;
	private String galleryInfoURL;
	private String sendRegistrationIdURL;
	private String deleteRegistrationIdURL;
	private String modifyOBjectURL;

	private Context context;

	private static final String siteNameProduction = "hadza.com";
	private static final String siteNameTesting = "hadza.com";
	private static final String siteNameDevelopment = "hadza.no-ip.org"; //"190.101.40.108";
 	private static String siteName = siteNameProduction;
	private static final String wsPrefix = "/ws/";
	public static final String URLProd = "http://" + siteNameProduction;
	public static final String URLTesting = "http://" + siteNameTesting + ":7799";
	public static final String URLDevelopment = "https://" + siteNameDevelopment;

	private static String DICTIONARY_NAME = "dictionary";

	private static final int portProd = 9977;
	private static final int portTesting = 9978;
	private static final int portDevelopment = 9979;
	
	private static final int tcpBurstSize = 51200;  //In order to check for lost connections we'll send the bytes in bursts
	private static int sendFileTimeout = 7500;
	
	private static int port;

	private static RestClient staticRestClient = null; 

	private RestClient(Context context){
		this.context = context;
		this.setURL(RestClient.URLProd);
		port = portProd;
	}

	public static RestClient getRestClient(Context context){

		if(staticRestClient == null){
			staticRestClient = new RestClient(context);
			return staticRestClient;
		}else {
			return staticRestClient;
		}
	}

	public void setURL(String URL){
		this.URL = URL;
		this.requestNewMediaIdURL = this.URL + RestClient.wsPrefix + "media/new_id/";
		this.userLoginURL = this.URL + RestClient.wsPrefix + "user/";
		this.userConnectURL = this.URL + RestClient.wsPrefix + "user/connect/";
		this.userRegistrationURL = this.URL + RestClient.wsPrefix + "user/register/";
		this.userSeedURL = this.URL + RestClient.wsPrefix + "user/seed/";
		this.getEventTagsURL = this.URL + RestClient.wsPrefix + "event/tags/";
		this.cancelMediaURL = this.URL + RestClient.wsPrefix + "media/cancel/";
		this.galleryInfoURL = this.URL + RestClient.wsPrefix + "user/gallery/";
		this.sendRegistrationIdURL = this.URL + RestClient.wsPrefix + "user/notifications/set_token/";
		this.deleteRegistrationIdURL = this.URL + RestClient.wsPrefix + "user/notifications/unset_token/";
		this.modifyOBjectURL = this.URL + RestClient.wsPrefix + "media/update/";
	}

	public void setMode(int mode){

		if(mode == PRODUCTION){
			this.setURL(URLProd);
			port = portProd;
			siteName = siteNameProduction;
		}else if(mode == TESTING){
			this.setURL(URLTesting);
			port = portTesting;
			siteName = siteNameTesting;
		}else if(mode == DEVELOPMENT){
			this.setURL(URLDevelopment);
			port = portDevelopment;
			siteName = siteNameDevelopment;
		}
	}

	private String convertStreamToString(InputStream is) throws IOException  {
		if (is != null) {
			Writer writer = new StringWriter();
			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} finally {
				is.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}

	private JSONObject parseEntity(HttpEntity entity) throws IllegalStateException, IOException{
		String response = null;
		JSONObject object = null;

		if (entity != null) {
			InputStream is = entity.getContent();
			response = this.convertStreamToString(is);

			Log.d(TAG, response);

			try {
				object = (JSONObject) new JSONTokener(response).nextValue();

			} catch (JSONException e) {
				e.printStackTrace();
				return null;
			} catch (Exception e){
				e.printStackTrace();
				return null;
			}
			Log.i(TAG, "response: " + response);
			is.close();
		}
		return object;
	}

	public RequestNewMediaIdResult requestNewMediaId(MediaObject mediaObject, String username, String password){

		JSONObject resultJSONObject;

		resultJSONObject = this.executeRequest(POST, this.requestNewMediaIdURL, this.buildParameters(mediaObject, username, password));

		if(resultJSONObject == null){
			return new RequestNewMediaIdResult("", "", -1, 0, false);
		}

		long mediaId = resultJSONObject.optLong("media_id", 0);
		String message = resultJSONObject.optString("message", "");
		String reason = resultJSONObject.optString("reason", "");
		int errorCode = resultJSONObject.optInt("error_code", 0);
		boolean success = resultJSONObject.optBoolean("success", false);

		return new RequestNewMediaIdResult(message, reason, errorCode, mediaId, success);
	}
	
	/**
	 * Takes a media object and from it builds a JSONObject used to store all metadata needed.
	 * 
	 * @param mediaObject object that contains all of the media's metadata.
	 */
	public JSONObject buildParameters(MediaObject mediaObject, String username, String password){

		long numberOfPackages = 0;
		JSONObject parameters = new JSONObject();

		// build time object from data
		Time startTime = new Time();
		startTime.switchTimezone(Time.TIMEZONE_UTC);
		startTime.set(mediaObject.getStartTime());

		numberOfPackages = mediaObject.getTotalBytes();

		File auxFile = new File(mediaObject.getFilename());
		//build JSONObject
		try {
			parameters.put("latitude", Double.toString(mediaObject.getLatitude()));
			parameters.put("longitude", Double.toString(mediaObject.getLongitude()));
			String timestampString = startTime.format3339(false);
			parameters.put("timestamp", timestampString);
			parameters.put("millis", mediaObject.getStartTime()%1000);
			parameters.put("extension", mediaObject.getExtension());
			parameters.put("media_type", Integer.toString(mediaObject.getMediaType()));
			parameters.put("duration", Long.toString(mediaObject.getDuration()) );
			parameters.put("number_of_packages", Long.toString(numberOfPackages) );
			parameters.put("username", username);
			parameters.put("password", password);
			parameters.put("tags", new JSONArray(mediaObject.getTags()));
			parameters.put("checksum", mediaObject.getChecksum());
			parameters.put("size", Long.toString(auxFile.length()));
			parameters.put("package_size", Integer.toString(tcpBurstSize));

			switch (mediaObject.getRotation()) {

			case ExifInterface.ORIENTATION_NORMAL:  
				parameters.put("rotation", 0);       
				break;
			case ExifInterface.ORIENTATION_ROTATE_90:  
				parameters.put("rotation", 270);      
				break;
			case ExifInterface.ORIENTATION_ROTATE_180:  
				parameters.put("rotation", 180);         
				break;
			case ExifInterface.ORIENTATION_ROTATE_270:  
				parameters.put("rotation", 90);         
				break;
			}

			parameters.put("gps_accuracy", Float.toString(mediaObject.getGpsAccuracy()));
			long utcOffset =  MetadataObtainer.getUTCOffset();
			parameters.put("offset_utc", Long.toString(utcOffset));

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return parameters;
	}

	public MediaUpdateResult modifyMedia(long mediaId, boolean privateMedia, String username, String password ){

		JSONObject resultJSONObject;

		JSONObject parameters = new JSONObject();
		try {
			parameters.put("username", username);
			parameters.put("password", password);
			parameters.put("id", mediaId);
			parameters.put("private", privateMedia);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		resultJSONObject = this.executeRequest(POST, this.modifyOBjectURL, parameters);

		if(resultJSONObject == null){
			return new MediaUpdateResult("", "", -1, false);
		}

		String message = resultJSONObject.optString("message", "");
		String reason = resultJSONObject.optString("reason", "");
		int errorCode = resultJSONObject.optInt("error_code", 0);
		boolean success = resultJSONObject.optBoolean("success", false);

		return new MediaUpdateResult(message, reason, errorCode, success);
	}

	public MediaUploadResult uploadMedia(File fileToSend, long mediaId, long alreadySentBytes, long totalBytes, UploadNotifiable listener){

		MediaUploadResult result;

		TcpClient tcpClient = new TcpClient(RestClient.siteName, RestClient.port);

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		long bytesSent = 0;
		Log.d(TAG, "Client created.");

		long serverUploadedBytes = tcpClient.open(mediaId);

		if(tcpClient.isOpen()){
			Log.d(TAG, "Client open.");
		}else{
			Log.d(TAG, "Client couldn't open. Error code " + tcpClient.getOpeningErrorCode() );
			tcpClient.close();
			return new MediaUploadResult( tcpClient.getOpeningErrorCode(), 0, -1);
		}

		tcpClient.setNotifiable(listener);
		bytesSent = tcpClient.sendFile(fileToSend, mediaId, serverUploadedBytes, totalBytes, sendFileTimeout, tcpBurstSize);	

		result = new MediaUploadResult( tcpClient.getErrorCode(), serverUploadedBytes, bytesSent);

		Log.d(TAG, "Client sent file. Uploaded " + (serverUploadedBytes + bytesSent) + " bytes. And received error code " +tcpClient.getErrorCode());

		tcpClient.close();

		return result;
	}

	public LoginResult userExists(JSONObject parameters){

		JSONObject resultJSONObject;

		resultJSONObject = this.executeRequest(GET, this.userLoginURL, parameters);

		if(resultJSONObject == null){
			return new LoginResult(false, false, false);
		}

		boolean success = resultJSONObject.optBoolean("success", false);
		boolean tester = resultJSONObject.optBoolean("tester", false);
		boolean youtube = resultJSONObject.optBoolean("youtube", false);

		return new LoginResult(success, tester, youtube);
	}

	public LoginResult connectedUserExists(JSONObject parameters){

		JSONObject resultJSONObject;

		resultJSONObject = this.executeRequest(POST, this.userConnectURL, parameters);

		if(resultJSONObject == null){
			return new LoginResult(false, false, false);
		}

		boolean success = resultJSONObject.optBoolean("success", false);
		boolean tester = resultJSONObject.optBoolean("tester", false);
		boolean youtube = resultJSONObject.optBoolean("youtube", false);
		String username = resultJSONObject.optString("username", null);

		return new LoginResult(success, tester, youtube, username);
	}

	public RegistrationResult userRegister(JSONObject parameters){

		JSONObject resultJSONObject;

		resultJSONObject = this.executeRequest(POST, this.userRegistrationURL, parameters);

		if(resultJSONObject == null){
			return new RegistrationResult(false, -1, "");
		}

		boolean result = resultJSONObject.optBoolean("success", false);
		int error = resultJSONObject.optInt("error", 1);
		String message = resultJSONObject.optString("reason", "");

		return new RegistrationResult(result, error, message);
	}

	public UserSeedResult getUserSeed(String username){

		JSONObject parameters = new JSONObject();
		JSONObject resultJSONObject;

		try 
		{
			parameters.put("user_name", username);
			resultJSONObject =  this.executeRequest(GET, this.userSeedURL, parameters);	

			if(resultJSONObject == null){
				return new UserSeedResult(null, false, -1, "");
			}

			boolean result = resultJSONObject.optBoolean("success", false);
			int error = resultJSONObject.optInt("error_code", 1);
			String message = resultJSONObject.optString("reason", "");
			String seed =  resultJSONObject.optString("seed", null);

			return new UserSeedResult(seed, result, error, message);
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}			

		return new UserSeedResult(null, false, -1, "");		
	}

	public JSONArray getEventTags(JSONObject parameters){

		JSONObject resultJSONObject;

		resultJSONObject = this.executeRequest(GET, this.getEventTagsURL, parameters);

		if(resultJSONObject == null){
			return new JSONArray();
		}

		JSONArray tags = resultJSONObject.optJSONArray("tags");

		if(tags != null){
			return tags;
		}else{

			return new JSONArray();
		}
	}

	public void cancelUpload(String username, String password, long id){

		JSONObject parameters = new JSONObject();

		try 
		{
			parameters.put("username", username);
			parameters.put("password", password);
			parameters.put("service_id", id);
			this.executeRequest(POST, this.cancelMediaURL, parameters);			
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}	
	}

	public void sendRegistrationId(String username, String password, String old_id, String new_id){

		JSONObject parameters = new JSONObject();

		try 
		{
			parameters.put("username", username);
			parameters.put("password", password);
			parameters.put("token", new_id);

			this.executeRequest(POST, this.sendRegistrationIdURL, parameters);			
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}	
	}

	public void deleteRegistrationId(String username, String password, String id){

		JSONObject parameters = new JSONObject();

		try 
		{
			parameters.put("username", username);
			parameters.put("password", password);
			parameters.put("token", id);

			this.executeRequest(POST, this.deleteRegistrationIdURL, parameters);			
		}

		catch (Exception e)
		{
			e.printStackTrace();
		}	
	}

	public JSONObject getGalleryInfo(String username, String password, String selector, String page, String userId, String eventId) {
		JSONObject resultJSONObject, parameters = new JSONObject();

		try {
			parameters.put("username", username);
			parameters.put("password", password);
			parameters.put("p", page);
			
			if(selector.equals("events")){
				if(userId != null){
					if(!userId.equals(ME)){
						parameters.put("user", userId);
					}
				}else{
					parameters.put("new", true);
				}
			}else if (selector.equals("videos")){
				
				if(eventId != null){
					parameters.put("event", eventId);
				}else if(userId != null){
					parameters.put("user", userId );
				}
			}
			
			resultJSONObject = this.executeRequest(GET, this.galleryInfoURL + selector, parameters);

		} catch (JSONException e) {

			e.printStackTrace();
			return null;
		}

		if(resultJSONObject == null){
			resultJSONObject = new JSONObject();
			try {
				resultJSONObject.put("message", "There was an error");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return resultJSONObject;
		}

		return resultJSONObject;
	}

	private void addPhoneParameters(JSONObject parameters){

		try {
			parameters.put("os", "android");
			try {
				parameters.put("app_version", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
			} catch (NameNotFoundException e) {
				parameters.put("app_version","0");
			}
			parameters.put("sdk_version", Build.VERSION.SDK_INT);
			parameters.put("carrier_brand", Build.BRAND);
			parameters.put("manufacturer", Build.MANUFACTURER);
			parameters.put("model", Build.MODEL);
			parameters.put("product", Build.PRODUCT);
			parameters.put("unique_id", Installation.id(context));

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private JSONObject executeRequest(int method, String URL, JSONObject parameters) {

		HashMap<String, File> files = new HashMap<String, File>();
		return this.executeRequest(method, URL, parameters, files);
	}

	private JSONObject executeRequest(int method, String URL, JSONObject parameters, HashMap<String, File> files){

		HashMap<String, String> headers = new HashMap<String, String>();
		HashMap<String, String> params = new HashMap<String, String>();
		HttpEntity resultEntity;

		this.addPhoneParameters(parameters);
		String dictionary = parameters.toString();

		params.put(RestClient.DICTIONARY_NAME, dictionary);

		try {
			resultEntity = this.executeRequest(method, URL, params, headers, files);

			if(resultEntity != null){
				return this.parseEntity(resultEntity);
			}else{
				return null;
			}
		}catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
