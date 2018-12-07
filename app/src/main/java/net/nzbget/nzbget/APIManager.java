package net.nzbget.nzbget;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class APIManager {

    private static MediaType mJsonMediaType = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient mClient = new OkHttpClient();

    public static JSONObject getConfig() throws IOException, JSONException {
        Request request = new Request.Builder()
                .url("http://localhost:6789/jsonrpc/config")
                .build();
        Response response = mClient.newCall(request).execute();
        String responseData = response.body().string();
        return new JSONObject(responseData);
    }

    public static JSONObject getHistory() throws IOException, JSONException {
        Request request = new Request.Builder()
                .url("http://localhost:6789/jsonrpc/history?=false")
                .build();
        Response response = mClient.newCall(request).execute();
        String responseData = response.body().string();
        return new JSONObject(responseData);
    }

    public static boolean postEditQueue(String command, String param, int[] ids) throws IOException, JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("method", "editqueue");
        JSONArray params = new JSONArray();
        params.put(command);
        params.put(param);
        params.put(new JSONArray(ids));
        jsonBody.put("params", params);
        RequestBody body = RequestBody.create(mJsonMediaType, jsonBody.toString());
        Request request = new Request.Builder()
                .url("http://localhost:6789/jsonrpc")
                .post(body)
                .build();
        Response response = mClient.newCall(request).execute();
        String responseData = response.body().string();
        JSONObject jsonResponse = new JSONObject(responseData);
        return jsonResponse.getBoolean("result");
    }
}
