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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class StorageActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_storage);
        // Set text field values
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String defaultPath = sharedPreferences.getString("defaultPath", "");
        if (defaultPath != "") {
            ((TextView)findViewById(R.id.textDefaultPath)).setText(FileUtil.getFullPathFromTreeUri(Uri.parse(defaultPath), this));
            ((Button)findViewById(R.id.removeDefaultPath)).setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.path_remove_botton_enabled));
        }
        String moviePath = sharedPreferences.getString("moviePath", "");
        if (moviePath != "") {
            ((TextView)findViewById(R.id.textMoviePath)).setText(FileUtil.getFullPathFromTreeUri(Uri.parse(moviePath), this));
            ((Button)findViewById(R.id.removeMoviePath)).setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.path_remove_botton_enabled));
        }
        String tvPath = sharedPreferences.getString("tvPath", "");
        if (tvPath != "") {
            ((TextView)findViewById(R.id.textTVPath)).setText(FileUtil.getFullPathFromTreeUri(Uri.parse(tvPath), this));
            ((Button)findViewById(R.id.removeTVPath)).setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.path_remove_botton_enabled));
        }
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
            switch (requestCode) {
                case 10:
                    // Default path
                    setChosenPath("defaultPath", pickedDir.getUri(), (TextView)findViewById(R.id.textDefaultPath), (Button)findViewById(R.id.removeDefaultPath));
                    break;
                case 20:
                    // Movie path
                    setChosenPath("moviePath", pickedDir.getUri(), (TextView)findViewById(R.id.textMoviePath), (Button)findViewById(R.id.removeMoviePath));
                    break;
                case 30:
                    // TV path
                    setChosenPath("tvPath", pickedDir.getUri(), (TextView)findViewById(R.id.textTVPath), (Button)findViewById(R.id.removeTVPath));
                    break;
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
        // Check request code
        switch (requestCode) {
            case 1:
                showFolderPicker(10);
                break;
            case 2:
                showFolderPicker(20);
                break;
            case 3:
                showFolderPicker(30);
                break;
        }
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

    public void chooseDefaultPath(View view) {
        // Check storage permission first
        if (!PermissionManager.isStoragePermissionGranted(this, 1)) {
            return;
        }
        showFolderPicker(10);
    }

    public void chooseMoviePath(View view) {
        // Check storage permission first
        if (!PermissionManager.isStoragePermissionGranted(this, 2)) {
            return;
        }
        showFolderPicker(20);
    }

    public void chooseTVPath(View view) {
        // Check storage permission first
        if (!PermissionManager.isStoragePermissionGranted(this, 3)) {
            return;
        }
        showFolderPicker(30);
    }

    public void removeDefaultPath(View view) {
        removeChosenPath("defaultPath", (TextView)findViewById(R.id.textDefaultPath), (Button)findViewById(R.id.removeDefaultPath));
    }

    public void removeMoviePath(View view) {
        removeChosenPath("moviePath", (TextView)findViewById(R.id.textMoviePath), (Button)findViewById(R.id.removeMoviePath));
    }

    public void removeTVPath(View view) {
        removeChosenPath("tvPath", (TextView)findViewById(R.id.textTVPath), (Button)findViewById(R.id.removeTVPath));
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
