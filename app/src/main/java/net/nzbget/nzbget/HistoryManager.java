package net.nzbget.nzbget;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryManager {

    private String mLogTag = "NZBGet History Manager";

    private static HistoryManager mInstance;
    private static Context mCtx;
    private static ExecutorService mThreadPoolExecutor;
    private static Map<Integer, Date> mRecentlyHandledEntriesMap; // We will put recently handled history entries in here to prevent race conditions

    private HistoryManager(Context context) {
        mCtx = context;
        mThreadPoolExecutor = Executors.newSingleThreadExecutor(); // Thread Pool Executor makes sure we are not checking the history concurrently
        mRecentlyHandledEntriesMap = new HashMap<Integer, Date>();
    }

    public static synchronized HistoryManager getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new HistoryManager(context);
        }
        return mInstance;
    }


    public void checkHistory() {
        mThreadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                getHistory();
            }
        });
    }

    private void getHistory() {
        Log.i(mLogTag, "GETTING HISTORY");
        try {
            Thread.sleep(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Get history fom API
        try {
            JSONObject response = APIManager.getHistory();
            Log.i(mLogTag, "GOT HISTORY: "+response.toString());
            JSONArray resultArray = response.getJSONArray("result");
            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject object = resultArray.getJSONObject(i);
                // Check if this history entry was already moved
                JSONArray parameters = object.has("Parameters") ? object.getJSONArray("Parameters") : null;
                if (parameters != null) {
                    boolean shouldMoveFile = true;
                    for (int j = 0; j < parameters.length(); j++) {
                        JSONObject parameter = parameters.getJSONObject(j);
                        if (parameter.getString("Name").equals("HandledByAndroidDaemon")) {
                            shouldMoveFile = false;
                            break;
                        }
                    }
                    if (shouldMoveFile) {
                        processUnhandledHistoryEntry(object);
                    } else {
                        Log.i(mLogTag, "FILE WAS ALREADY MOVED!");
                    }
                } else {
                    Log.i(mLogTag, "PARAMETERS IS NULL!");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clean up recently handled files
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, -1);
            Date now = calendar.getTime();
            Iterator<Map.Entry<Integer,Date>> iter = mRecentlyHandledEntriesMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<Integer,Date> entry = iter.next();
                if (entry.getValue().before(now)) {
                    iter.remove();
                }
            }
        }
    }

    private void processUnhandledHistoryEntry(JSONObject jsonObject) {
        try {
            int nzbId = jsonObject.getInt("NZBID");
            if (mRecentlyHandledEntriesMap.containsKey(nzbId)) {
                Log.i(mLogTag, "WE JUST TRIED TO HANDLE A DUPLICATE ENTRY!!!!!!!");
                // we already handled this entry
                return;
            }
            mRecentlyHandledEntriesMap.put(nzbId, new Date());
            // Get Status
            String status = jsonObject.getString("Status");
            if (status.startsWith("SUCCESS/")) { // TODO: Also handle warning (see issue)
                Log.i(mLogTag, "HANDLING FILE!");
                // Download is done
                // Get dir
                String destDir = jsonObject.getString("DestDir");
                // Move files
                moveFinishedDownload(destDir);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            setHandledHistoryEntry(jsonObject);
        }
    }

    private void setHandledHistoryEntry(JSONObject jsonObject) {
        // Get ID
        try {
            int nzbId = jsonObject.getInt("NZBID");
            Log.i(mLogTag, "ID: " + Integer.toString(nzbId));
            // Set entry as handled in NZBGet
            boolean success = APIManager.postEditQueue("HistorySetParameter", "HandledByAndroidDaemon=true", new int[]{nzbId});
            if (!success) {
                Log.e(mLogTag, "Could not set history parameter");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveFinishedDownload(String downloadDir) {
        // Check if we need to move the download
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx);
        String pathDefault = sharedPreferences.getString("pathDefault", "");
        if (!pathDefault.isEmpty()) {
            try {
                File srcDir = new File(downloadDir);
                Uri defaultUri = Uri.parse(pathDefault);
                Log.i(mLogTag, "srcDir.getName(): "+srcDir.getName());
                DocumentFile targetDir = DocumentFile.fromTreeUri(mCtx, defaultUri);
                moveFile(srcDir, targetDir);
                deleteRecursive(srcDir);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }
        fileOrDirectory.delete();
    }

    private void moveFile(File sourceFile, DocumentFile destFile) {
        if (sourceFile.isDirectory()) {
            // Create folder in destination
            destFile = destFile.createDirectory(sourceFile.getName());
            for (File file : sourceFile.listFiles()) {
                moveFile(file, destFile);
            }
        }
        else {
            DocumentFile file = destFile.createFile("", sourceFile.getName());
            try (InputStream in = new FileInputStream(sourceFile)) {
                try (OutputStream out = mCtx.getContentResolver().openOutputStream(file.getUri())) {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
