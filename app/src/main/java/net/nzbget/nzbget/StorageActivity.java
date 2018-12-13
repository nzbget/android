package net.nzbget.nzbget;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class StorageActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private String mLogTag = "StorageActivity";

    public static String DEFAULT_PATH_NAME = "Default (no category)";
    public static String PATH_NAME_PREF_PREFIX = "PATH_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);
        setUpActivity();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode != RESULT_OK)
            return;
        else {
            Uri treeUri = resultData.getData();
            DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);
            grantUriPermission(getPackageName(), treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            // Get path view
            LinearLayout containerLinearLayout = (LinearLayout)findViewById(R.id.pathContainer);
            if (requestCode < containerLinearLayout.getChildCount()) {
                LinearLayout pathLinearLayout = (LinearLayout)containerLinearLayout.getChildAt(requestCode);
                setChosenPath(PATH_NAME_PREF_PREFIX+((TextView) pathLinearLayout.findViewById(R.id.textTitlePath)).getText().toString(), pickedDir.getUri(), (TextView) pathLinearLayout.findViewById(R.id.textDisplayPath), (Button) pathLinearLayout.findViewById(R.id.buttonRemovePath));
            } else {
                Log.i(mLogTag, "Received invalid resultCode in onActivityResult.");
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        // At least one result must be granted
        if (grantResults.length < 1) {
            MessageActivity.showErrorMessage(this, "Permission not granted", "NZBGet needs storage permission to write to external paths.", null);
            return;
        }
        // Verify that each required permission has been granted
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                MessageActivity.showErrorMessage(this, "Permission not granted", "NZBGet needs storage permission to write to external paths.", null);
                return;
            }
        }
        // Show folder picker
        showFolderPicker(requestCode);
    }

    private void setUpActivity() {
        // Add a view to set the default path
        addCategoryLayout(DEFAULT_PATH_NAME);
        // Get categories from API
        // HTTP request must not be done on the main thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    JSONObject response = APIManager.getConfig();
                    // Get queue path
                    JSONArray resultArray = response.getJSONArray("result");
                    for (int i = 0; i < resultArray.length(); i++) {
                        JSONObject object = resultArray.getJSONObject(i);
                        String name = object.getString("Name");
                        if (name != null && name.matches("^Category\\d\\.Name$")) {
                            String categoryName = object.getString("Value");
                            addCategoryLayout(categoryName);
                        }
                    }
                } catch (Exception e) {
                    StorageActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            MessageActivity.showErrorMessage(StorageActivity.this, "Warning", "Could not get NZBGet categories. Make sure the NZBGet daemon is running.", new MessageActivity.OnClickListener() {
                                @Override
                                public void onClick() {
                                    StorageActivity.this.finish();
                                }
                            });
                        }
                    });
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void addCategoryLayout(final String categoryName) {
        // We can only add to view on UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(StorageActivity.this);
                final LinearLayout containerLinearLayout = (LinearLayout) findViewById(R.id.pathContainer);
                final int layoutIndex = containerLinearLayout.getChildCount();

                final LinearLayout pathLinearLayout = (LinearLayout) LayoutInflater.from(StorageActivity.this).inflate(R.layout.linearlayout_path_entry, null);
                ((TextView)pathLinearLayout.findViewById(R.id.textTitlePath)).setText(categoryName);
                ((Button)pathLinearLayout.findViewById(R.id.buttonChoosePath)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // Check storage permission first
                        if (!PermissionManager.isStoragePermissionGranted(StorageActivity.this, layoutIndex)) {
                            return;
                        }
                        showFolderPicker(layoutIndex);
                    }
                });
                final TextView textDisplayPath = (TextView)pathLinearLayout.findViewById(R.id.textDisplayPath);
                final Button buttonRemovePath = (Button)pathLinearLayout.findViewById(R.id.buttonRemovePath);
                buttonRemovePath.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        removeChosenPath(PATH_NAME_PREF_PREFIX+categoryName, textDisplayPath, buttonRemovePath);
                    }
                });

                String pathFromSharedPreferences = sharedPreferences.getString(PATH_NAME_PREF_PREFIX+categoryName, "");
                if (pathFromSharedPreferences != "") {
                    textDisplayPath.setText(FileUtil.getFullPathFromTreeUri(Uri.parse(pathFromSharedPreferences), StorageActivity.this));
                    buttonRemovePath.setBackgroundTintList(ContextCompat.getColorStateList(StorageActivity.this, R.color.path_remove_botton_enabled));
                }

                containerLinearLayout.addView(pathLinearLayout);
            }
        });
    }

    public void showFolderPicker(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                        | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    private void setChosenPath(String sharedPrefsKey, Uri path, TextView pathTextView, Button removePathButton) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putString(sharedPrefsKey, path.toString()).commit();
        pathTextView.setText(FileUtil.getFullPathFromTreeUri(path, this));
        removePathButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.path_remove_botton_enabled));
    }

    private void removeChosenPath(String sharedPrefsKey, TextView pathTextView, Button removePathButton) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().remove(sharedPrefsKey).commit();
        pathTextView.setText("Not chosen");
        removePathButton.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.path_remove_botton_disabled));
    }

}
