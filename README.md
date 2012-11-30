CRAH
====

A simple and easy to use REST client for Android

Currently work in progress (v0.5.3) 


Installation
-----------

* git clone git@github.com:magnet-cl/crah.git
* Import
* Add as a library project to your android project (http://developer.android.com/tools/projects/projects-eclipse.html#ReferencingLibraryProject)


Usage
-----

Instantiate a RestClient object, then your can just execute a request in which you have to specify the HTTP method, the parameters (which will be sent as a json object), the headers and files (if any). It will return a response object which includes the response HTTP status code and the json object returned by the server (if any).


Example
-------

The following code will generate a post request:

    // create your json object
    JSONObject params = new JSONObject();
    params.put("id", 3);
    params.put("key_1", "Place your values here");
    params.put("key_2", "9001");

    // set the headers of your request
    HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("Accept", "application/json");
    headers.put("Content-type", "application/json");

    HashMap<String, File> files = new HashMap<String, File>();

    // send your request, and obtain your response
    JSONResponse response = restClient.executeRequest(RestClient.POST,
        serverUrl, params, headers, files);

    // check the status code
    int statusCode = response.getStatusCode();

    // check the returned json
    JSONObject object= (JSONObject)response.getResponseObject();
