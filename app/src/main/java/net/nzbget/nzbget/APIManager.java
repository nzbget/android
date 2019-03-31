package net.nzbget.nzbget;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class APIManager {

    private static MediaType mJsonMediaType = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient mClient = new OkHttpClient();

    private static String mPort;

    private static String RpcUrl(Context context)
    {
        if (mPort == null)
        {
            // Most of the time, dataDir will be "/data/data/net.nzbget.nzbget"
            String dataDir = context.getApplicationInfo().dataDir;
            String configFile = dataDir + "/nzbget/nzbget.conf";

            File file = new File(configFile);
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                while ((line = br.readLine()) != null)
                {
                    if (line.startsWith("ControlPort="))
                    {
                        mPort = line.substring("ControlPort=".length());
                        break;
                    }
                }
                br.close();
            }
            catch (IOException e)
            {
                mPort = "6789";
            }
        }
        
        return "http://localhost:" + mPort + "/jsonrpc";
    }

    public static JSONObject getConfig(Context context) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(RpcUrl(context) + "/config")
                .build();
        Response response = mClient.newCall(request).execute();
        String responseData = response.body().string();
        return new JSONObject(responseData);
    }

    public static JSONObject getHistory(Context context) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(RpcUrl(context) + "/history?=false")
                .build();
        Response response = mClient.newCall(request).execute();
        String responseData = response.body().string();
        return new JSONObject(responseData);
    }

    public static boolean postEditQueue(Context context, String command, String param, int[] ids) throws IOException, JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("method", "editqueue");
        JSONArray params = new JSONArray();
        params.put(command);
        params.put(param);
        params.put(new JSONArray(ids));
        jsonBody.put("params", params);
        RequestBody body = RequestBody.create(mJsonMediaType, jsonBody.toString());
        Request request = new Request.Builder()
                .url(RpcUrl(context))
                .post(body)
                .build();
        Response response = mClient.newCall(request).execute();
        String responseData = response.body().string();
        JSONObject jsonResponse = new JSONObject(responseData);
        return jsonResponse.getBoolean("result");
    }
}
