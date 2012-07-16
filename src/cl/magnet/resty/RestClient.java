package cl.magnet.resty;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

// TODO getPostRequest, getPUTRequest and getPATCHRequest are too similar, should refactor that
// TODO repeated code wehn detecting if it's PATCH, PUT or POST

public class RestClient {

	public static final String TAG = RestClient.class.toString();
	public static final int GET = 0;
	public static final int POST = 1;
	public static final int PUT = 2;
	public static final int PATCH = 3;
	public static final int DELETE = 4;

	protected DefaultHttpClient mHttpClient;

	/**
	 * Constructor for RestClient
	 */
	public RestClient(){

		DefaultHttpClient client = new DefaultHttpClient();
		ClientConnectionManager mgr = client.getConnectionManager();
		HttpParams params = client.getParams();
		SchemeRegistry registry =  mgr.getSchemeRegistry();
		ClientConnectionManager manager = new ThreadSafeClientConnManager(params, registry);
		mHttpClient = new DefaultHttpClient(manager, params);
	}

	/**
	 * Transforms the entity contents into a JSONObject if possible, returns null otherwise
	 * @param entity
	 * @return JSONObject if the entity is parseable as a JSONObject it will return it
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	private JSONObject parseEntity(HttpEntity entity) throws IllegalStateException, IOException{
		String response = null;
		JSONObject object = null;

		if (entity != null) {
			response = EntityUtils.toString(entity);;

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
		}
		return object;
	}

	private String buildParamsString(HashMap<String, String> params) throws UnsupportedEncodingException {
		Iterator<Entry<String, String>> it = params.entrySet().iterator();
		String res = "?";
		while (it.hasNext()) {
			Entry<String, String> pair = it.next();
			String add = pair.getKey() + "="
					+ URLEncoder.encode(pair.getValue(), "UTF-8");
			if (params.size() > 1) {
				res += "&" + add;
			} else {
				res += add;
			}
		}
		return res;
	}

	private JSONObject buildParamsJSONObject(HashMap<String, String> params) throws UnsupportedEncodingException {
		Iterator<Entry<String, String>> it = params.entrySet().iterator();
		JSONObject object = new JSONObject();

		while (it.hasNext()) {
			Entry<String, String> pair = it.next();

			try {
				object.put(pair.getKey(), pair.getValue());
			} catch (JSONException e) {
				Log.d(TAG, "Could not add parameter to JSON object. Key was: " + pair.getKey() + ", value was: " + pair.getValue());
			}
		}
		return object;
	}

	private void addHeaders(HashMap<String, String> headers, HttpRequestBase request) throws UnsupportedEncodingException {

		Iterator<Entry<String, String>> it = headers.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, String> header = it.next();
			request.addHeader(header.getKey(), header.getValue());
		}
	}

	/*private void addPostEntity(HttpPost request, HashMap<String, File> files, HashMap<String, String> params) throws UnsupportedEncodingException {

		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

		if(files != null){
			Iterator<Entry<String, File>> it = files.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, File> header = it.next();
				FileBody fileBody = new FileBody(header.getValue());	
				reqEntity.addPart(header.getKey(), fileBody);
			}
		}

		if(params != null){
			Iterator<Entry<String, String>> it = params.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> header = it.next();
				StringBody stringBody = new StringBody(header.getValue());	
				reqEntity.addPart(header.getKey(), stringBody);
			}
		}

		request.setEntity(reqEntity);
	}*/

	/*public static final int GET = 0;
	public static final int POST = 1;
	public static final int PUT = 2;
	public static final int PATCH = 3;
	public static final int DELETE = 4;*/
	
	//TODO the default case is not handled
	private JSONObject executeRequest(int method, String URL, HashMap<String, String> params, HashMap<String, String> headers, HashMap<String, File> files){

		HttpEntity resultEntity;

		try {
			HttpRequestBase request;
			HttpContext localContext = new BasicHttpContext();

			switch(method){
			case RestClient.GET:
				request = this.getGETResquest(URL, params);
				break;
			case RestClient.POST:
				request = this.getPOSTRequest(URL, params, files);
				break;
			case RestClient.PUT:
				request = this.getPUTRequest(URL, params, files);
				break;
			case RestClient.PATCH:
				request = this.getPATCHRequest(URL, params, files);
				break;
			case RestClient.DELETE:
				request = this.getDELETERequest(URL);
				break;
			default:
				Log.d(TAG, "HTTP method not supported, returning null.");
				return null;
			}

			// add headers
			this.addHeaders(headers, request);

			// exec request
			HttpResponse resp;

			resp = mHttpClient.execute(request, localContext);

			resultEntity = resp.getEntity();

			if(resultEntity != null){
				return this.parseEntity(resultEntity);
			}else{
				return null;
			}
		}catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalStateException e) {

			Log.e(TAG, e.getMessage());
			return null;
		}
	}

	private HttpGet getGETResquest(String URL, HashMap<String, String> params){

		try {

			// parameters are url encoded in the URL
			HttpGet request = new HttpGet(URL + buildParamsString(params));

			return request;
		} catch (UnsupportedEncodingException e) {

			Log.e(TAG, "Could not build GET request.");
			return null;
		}
	}

	private void addMultiPartEntityToRequest(HttpRequest request, HashMap<String, String> params, HashMap<String, File> files) throws UnsupportedEncodingException{

		HttpPost postRequest = null;
		HttpPut putRequest = null;
		HttpPatch patchRequest = null;

		if(request.getClass().toString().equals(HttpPost.class.toString())){
			postRequest = (HttpPost) request;
		}else if(request.getClass().toString().equals(HttpPut.class.toString())){
			putRequest = (HttpPut) request;
		}else if(request.getClass().toString().equals(HttpPatch.class.toString())){
			patchRequest = (HttpPatch) request;
		}else{
			Log.d(TAG, "Could not add Multipart entity to the request, it was neither POST nor PATCH nor PUT.");
			return;
		}

		MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);

		if(files != null){
			Iterator<Entry<String, File>> it = files.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, File> header = it.next();
				FileBody fileBody = new FileBody(header.getValue());	
				reqEntity.addPart(header.getKey(), fileBody);
			}
		}

		if(params != null){
			Iterator<Entry<String, String>> it = params.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, String> header = it.next();
				StringBody stringBody = new StringBody(header.getValue());	
				reqEntity.addPart(header.getKey(), stringBody);
			}
		}

		if(request.getClass().toString().equals(HttpPost.class.toString())){
			postRequest.setEntity(reqEntity);
		}else if(request.getClass().toString().equals(HttpPut.class.toString())){
			putRequest.setEntity(reqEntity);
		}else if(request.getClass().toString().equals(HttpPut.class.toString())){
			patchRequest.setEntity(reqEntity);
		}else{
			Log.d(TAG, "Could not add Multipart entity to the request, it was neither POST nor PATCH nor PUT.");
		}
	}

	private void addStringBodyEntityToRequest(HttpRequest request, HashMap<String, String> params) throws UnsupportedEncodingException{

		HttpPost postRequest = null;
		HttpPut putRequest = null;
		HttpPatch patchRequest = null;

		if(request.getClass().toString().equals(HttpPost.class.toString())){
			postRequest = (HttpPost) request;
		}else if(request.getClass().toString().equals(HttpPut.class.toString())){
			putRequest = (HttpPut) request;
		}else if(request.getClass().toString().equals(HttpPatch.class.toString())){
			patchRequest = (HttpPatch) request;
		}else{
			Log.d(TAG, "Could not add String entity to the request, it was neither POST nor PATCH nor PUT.");
			return;
		}

		JSONObject holder = this.buildParamsJSONObject(params);

		//passes the results to a string builder/entity
		StringEntity reqEntity = new StringEntity(holder.toString());

		//sets the post request as the resulting string
		if(request.getClass().toString().equals(HttpPost.class.toString())){
			postRequest.setEntity(reqEntity);
		}else if(request.getClass().toString().equals(HttpPut.class.toString())){
			putRequest.setEntity(reqEntity);
		}else if(request.getClass().toString().equals(HttpPut.class.toString())){
			patchRequest.setEntity(reqEntity);
		}else{
			Log.d(TAG, "Could not add String entity to the request, it was neither POST nor PATCH nor PUT.");
		}
	}

	private HttpPost getPOSTRequest(String URL, HashMap<String, String> params, HashMap<String, File> files){

		try {
			HttpPost request = new HttpPost(URL);

			if(files.size() != 0){
				this.addMultiPartEntityToRequest(request, params, files);
			}else{
				this.addStringBodyEntityToRequest(request, params);
			}

			return request;
		} catch (UnsupportedEncodingException e) {

			Log.e(TAG, "Could not build GET request.");
			return null;
		}
	}

	private HttpPut getPUTRequest(String URL, HashMap<String, String> params, HashMap<String, File> files){

		try {
			HttpPut request = new HttpPut(URL);

			if(files.size() != 0){
				this.addMultiPartEntityToRequest(request, params, files);
			}else{
				this.addStringBodyEntityToRequest(request, params);
			}

			return request;
		} catch (UnsupportedEncodingException e) {

			Log.e(TAG, "Could not build PUT request.");
			return null;
		}
	}

	private HttpPatch getPATCHRequest(String URL, HashMap<String, String> params, HashMap<String, File> files){

		try {
			HttpPatch request = new HttpPatch(URL);

			if(files.size() != 0){
				this.addMultiPartEntityToRequest(request, params, files);
			}else{
				this.addStringBodyEntityToRequest(request, params);
			}

			return request;
		} catch (UnsupportedEncodingException e) {

			Log.e(TAG, "Could not build PATCH request.");
			return null;
		}
	}

	private HttpDelete getDELETERequest(String URL){

		HttpDelete request = new HttpDelete(URL);

		return request;
	}

	/*public HttpEntity executeRequest(int method, String URL, HashMap<String, String> params, HashMap<String, String> headers, HashMap<String, File> files) {

		HttpRequestBase request;
		HttpContext localContext = new BasicHttpContext();

		try{
			// add params and create request
			if(method == RestClient.GET){
				request = new HttpGet(URL + buildParamsString(params));
			}
			else if (method == RestClient.POST){

				request = new HttpPost(URL);
				this.addPostEntity((HttpPost)request,files, params);
			}
			else{
				return null;
			}

			// add headers
			this.addHeaders(headers, request);

			// exec request
			HttpResponse resp;

			resp = mHttpClient.execute(request, localContext);

			HttpEntity entity = resp.getEntity();
			return entity;

		} catch (IllegalStateException e) {

			Log.e(TAG, e.getMessage());
			return null;
		} catch (ClientProtocolException e) {
			Log.e(TAG, e.getMessage());
			return null;
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
			return null;
		}
	}*/

	/*private HttpClient getNewSSLClient() {

		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));

		HttpParams params = new BasicHttpParams();
		params.setParameter(ConnManagerPNames.MAX_TOTAL_CONNECTIONS, 30);
		params.setParameter(ConnManagerPNames.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(30));
		params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
		HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);

		ClientConnectionManager cm = new SingleClientConnManager(params, schemeRegistry);
		return new DefaultHttpClient(cm, params);
	}*/
}

