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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okio.BufferedSink;
import okio.Okio;
import okio.Source;

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
        // Get history fom API
        try {
            JSONObject response = APIManager.getHistory(mCtx);
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
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Clean up recently handled files
            // Everything older than 1 hour will be deleted
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR_OF_DAY, -1);
            Date now = calendar.getTime();
            Iterator<Map.Entry<Integer,Date>> iter = mRecentlyHandledEntriesMap.entrySet().iterator();
            Log.i(mLogTag, " Recently Handled Entries Map size before cleanup: "+mRecentlyHandledEntriesMap.size());
            while (iter.hasNext()) {
                Map.Entry<Integer,Date> entry = iter.next();
                if (entry.getValue().before(now)) {
                    iter.remove();
                }
            }
            Log.i(mLogTag, " Recently Handled Entries Map size after cleanup: "+mRecentlyHandledEntriesMap.size());
        }
    }

    private void processUnhandledHistoryEntry(JSONObject jsonObject) {
        try {
            int nzbId = jsonObject.getInt("NZBID");
            if (mRecentlyHandledEntriesMap.containsKey(nzbId)) {
                // we already handled this entry
                return;
            }
            // Put ID in recently handled entries since moving the files my take a long time and we don't want duplicates
            mRecentlyHandledEntriesMap.put(nzbId, new Date());
            // Get category
            String category = "";
            try {
                category = jsonObject.getString("Category");
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Get Status
            String status = jsonObject.getString("Status");
            if (status.startsWith("SUCCESS/")) { // TODO: Also handle warning (see https://github.com/nzbget/android/issues/11)
                // Download is done
                // Get dir
                String destDir = jsonObject.getString("DestDir");
                // Move files
                moveFinishedDownload(destDir, category);
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
            // Set entry as handled in NZBGet
            boolean success = APIManager.postEditQueue(mCtx, "HistorySetParameter", "HandledByAndroidDaemon=true", new int[]{nzbId});
            if (!success) {
                Log.e(mLogTag, "Could not set history parameter using edit queue");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveFinishedDownload(String downloadDir, String categoryName) {
        // Check if we need to move the download
        String moveUriString = getUriStringForCategory(categoryName);
        if (!moveUriString.isEmpty()) {
            try {
                File srcDir = new File(downloadDir);
                Uri moveUri = Uri.parse(moveUriString);
                DocumentFile targetDir = DocumentFile.fromTreeUri(mCtx, moveUri);
                moveFile(srcDir, targetDir);
                deleteRecursive(srcDir);
                Log.i(mLogTag, "Moved "+downloadDir+" to "+FileUtil.getFullPathFromTreeUri(targetDir.getUri(), mCtx));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // getUriStringForCategory will get the string that represents the uri of the directory that the download should be moved to
    private String getUriStringForCategory(String categoryName) {
        String uriString = "";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mCtx);
        if (!categoryName.isEmpty()) {
            uriString = sharedPreferences.getString(StorageActivity.PATH_NAME_PREF_PREFIX+categoryName, "");
        }
        if (uriString.isEmpty()) {
            // No path is chosen for this category get default path
            uriString = sharedPreferences.getString(StorageActivity.PATH_NAME_PREF_PREFIX+StorageActivity.DEFAULT_PATH_NAME, "");
        }
        return uriString;
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
            // Create destination file
            DocumentFile file = destFile.createFile("", sourceFile.getName());
            try (Source a = Okio.source(sourceFile); BufferedSink b = Okio.buffer(Okio.sink(mCtx.getContentResolver().openOutputStream(file.getUri())))) {
                b.writeAll(a);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
