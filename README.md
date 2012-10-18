CRAH
====

A simple and easy to use REST client for Android

Currently work in progress (v0.5.3) 


Installation
-----------

* clone
    git clone git@github.com:magnet-cl/crah.git
* Import
* Add as a library project to your android project (http://developer.android.com/tools/projects/projects-eclipse.html#ReferencingLibraryProject)


Usage
-----

Instantiate a RestClient object, then your can just execute a request in which you have to specify the HTTP method, the parameters (which will be sent as a json object), the headers and files (if any). It will return a response object which includes the response HTTP status code and the json object returned by the server (if any).


Example: 

    JSONObject params = new JSONObject();
    params.put("id", "");
    params.put("request", service.getServerId());
    params.put("surveyQuestion", questionAnswer.getServerId());
    params.put("value", questionAnswer.getValue());

    HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("Accept", "application/json");
    headers.put("Content-type", "application/json");
    HashMap<String, File> files = new HashMap<String, File>();

    JSONResponse response = restClient.executeRequest(RestClient.POST,
        this.serverURL + "/survey-answer/", params, headers, files);
