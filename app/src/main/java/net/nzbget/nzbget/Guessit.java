package net.nzbget.nzbget;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

public class Guessit {

    private static Guessit mInstance;
    private static  Context mContext;

    private Guessit(Context context) {
        mContext = context;
    }

    public static synchronized Guessit getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new Guessit(context);
        }
        return mInstance;
    }

    public String getType(String downloadname) {
        String type = "";
        try {
            JSONObject guess = com.kabukky.guessit.Guessit.getInstance(mContext).guessit(downloadname);
            type = guess.getString("type");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return type;
    }
}
