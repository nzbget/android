package net.nzbget.nzbget;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;


public class InstallActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install);
    }

    private boolean downloading = false;
    private String downloadName;
    private long downloadId;

    private void setStatusText(String text) {
        if (text == null) {
            text = "Choose NZBGet version to install:";
        }
        ((TextView) findViewById(R.id.titleLabel)).setText(text);
    }

    private void enableButtons(boolean enabled) {
        findViewById(R.id.stableReleaseButton).setEnabled(enabled);
        findViewById(R.id.stableDebugButton).setEnabled(enabled);
        findViewById(R.id.testingReleaseButton).setEnabled(enabled);
        findViewById(R.id.testingDebugButton).setEnabled(enabled);
        findViewById(R.id.customButton).setEnabled(enabled);
    }

    public void installDaemon(View view) {
        setStatusText("Downloading installer package...");

        if (view.getId() == R.id.stableReleaseButton) {
            downloadName = "nzbget-latest-bin-linux.run";
        } else if (view.getId() == R.id.stableDebugButton) {
            downloadName = "nzbget-latest-bin-linux-debug.run";
        } else if (view.getId() == R.id.testingReleaseButton) {
            downloadName = "nzbget-testing-latest-bin-linux.run";
        } else if (view.getId() == R.id.testingDebugButton) {
            downloadName = "nzbget-testing-latest-bin-linux-debug.run";
        }

        downloading = true;
        enableButtons(false);
        download();
    }

    public void installCustom(View view) {

        String downloadName = null;
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = dir.listFiles();
        for (File inFile : files) {
            String curName = inFile.getName();
            if (curName.startsWith("nzbget-") && curName.endsWith(".run") &&
                    (downloadName == null || downloadName.compareTo(curName) > 0)) {
                downloadName = curName;
            }
        }

        enableButtons(false);
        if (downloadName != null) {
            installFile(downloadName);
        } else {
            MessageActivity.showErrorMessage(this, "NZBGet daemon installer", "Could not find NZBGet daemon installer in Download-directory.", null);
        }
        enableButtons(true);
        setStatusText(null);
    }

    private BroadcastReceiver onDownloadFinishReceiver;
    private BroadcastReceiver onNotificationClickReceiver;

    private void download() {
        String url = "http://nzbget.net/download/" + downloadName;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("NZBGet daemon installer");
        request.setDescription(downloadName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadName);

        registerReceiver(onDownloadFinishReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent i) {
                downloadCompleted(i.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        registerReceiver(onNotificationClickReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                downloadTouched();
            }
        }, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = manager.enqueue(request);
    }

    private void downloadTouched() {
        Log.i("InstallActivity", "Cancelling download");
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("NZBGet daemon installer");
        adb.setMessage("Cancel download and installation?");
        adb.setIcon(android.R.drawable.ic_dialog_alert);
        adb.setCancelable(true);
        adb.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                cancelDownload();
            }
        });
        adb.setNegativeButton("No", null);
        AlertDialog alert = adb.create();
        alert.show();
    }

    private void cancelDownload() {
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.remove(downloadId);
        setStatusText(null);
        enableButtons(true);
        downloading = false;
    }

    private void installFile(String downloadName) {
        try {
            new File("/sdcard/data/nzbget/installer").mkdirs();
            File dest = new File("/sdcard/data/nzbget/installer/nzbget-bin-linux.run");
            dest.delete();
            copy(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + downloadName), dest);
        } catch (IOException e) {
            MessageActivity.showErrorMessage(this, "NZBGet daemon installer", "File copy failed.", null);
            return;
        }

        install();
    }

    protected void downloadCompleted(long downloadId) {
        Log.i("InstallActivity", "downloadCompleted");
        try {
            unregisterReceiver(onDownloadFinishReceiver);
            unregisterReceiver(onNotificationClickReceiver);
        } catch (IllegalArgumentException e) {
            //Patch for bug: http://code.google.com/p/android/issues/detail?id=6191
        }

        boolean ok = false;
        if (validDownload(downloadId)) {
            Log.i("InstallActivity", "download successful");
            installFile(downloadName);
        } else {
            MessageActivity.showErrorMessage(this, "NZBGet daemon installer", "Download failed.", null);
        }

        setStatusText(null);
        enableButtons(true);
        downloading = false;
    }

    private boolean validDownload(long downloadId) {
        //Verify if download is a success
        DownloadManager manager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor c = manager.query(new DownloadManager.Query().setFilterById(downloadId));
        if (c.moveToFirst()) {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                return true;
            }
        }
        return false;
    }

    public void copy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    private void install() {
        setStatusText("Installing...");
        boolean ok = Daemon.getInstance().install();
        if (ok) {
            MessageActivity.showOkMessage(this, "Installation", "NZBGet daemon has been successfully installed.",
                    new MessageActivity.OnClickListener() {
                        public void onClick() {
                            finish();
                        }
                    });
        } else {
            MessageActivity.showLogMessage(this, "NZBGet daemon installation failed.");
        }
    }
}
