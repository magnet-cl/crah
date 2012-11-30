package cl.magnet.resty;

public class JSONResponse {
	
	int statusCode;
	Object responseObject;
	
	public int getStatusCode() {
		return statusCode;
	}

	public Object getResponseObject() {
		return responseObject;
	}

	public JSONResponse(int statusCode, Object responseObject) {
		super();
		this.statusCode = statusCode;
		this.responseObject = responseObject;
	}
}
