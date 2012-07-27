package cl.magnet.resty;

import org.json.JSONObject;

public class JSONResponse {
	
	int statusCode;
	JSONObject responseObject;
	
	public int getStatusCode() {
		return statusCode;
	}

	public JSONObject getResponseObject() {
		return responseObject;
	}

	public JSONResponse(int statusCode, JSONObject responseObject) {
		super();
		this.statusCode = statusCode;
		this.responseObject = responseObject;
	}
	
	

}
