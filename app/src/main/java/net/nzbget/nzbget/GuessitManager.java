package net.nzbget.nzbget;

import android.content.Context;

import com.kabukky.guessit.Guessit;

import org.json.JSONException;
import org.json.JSONObject;

public class GuessitManager {

    private static GuessitManager mInstance;
    private static  Context mContext;

    private GuessitManager(Context context) {
        mContext = context;
    }

    public static synchronized GuessitManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new GuessitManager(context);
        }
        return mInstance;
    }

    public String getType(String downloadname) {
        String type = "";
        try {
            JSONObject guess = Guessit.getInstance(mContext).guessit(downloadname);
            type = guess.getString("type");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return type;
    }
}
