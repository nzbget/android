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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;


public class InstallActivity extends ActionBarActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_install);
    }

    private enum InstallerKind { STABLE_RELEASE, STABLE_DEBUG, TESTING_RELEASE, TESTING_DEBUG }

    private boolean downloading = false;
    private boolean installing = false;
    private InstallerKind installerKind;
    private String downloadName;
    private long downloadId;
    private String downloadUrl;

    @Override
    public void onBackPressed()
    {
        if (!(downloading || installing))
        {
            super.onBackPressed();
        }
    }

    private void setStatusText(String text)
    {
        if (text == null)
        {
            text = "Choose NZBGet version to install:";
        }
        ((TextView)findViewById(R.id.titleLabel)).setText(text);
    }

    private void enableButtons(boolean enabled)
    {
        findViewById(R.id.stableReleaseButton).setEnabled(enabled);
        findViewById(R.id.stableDebugButton).setEnabled(enabled);
        findViewById(R.id.testingReleaseButton).setEnabled(enabled);
        findViewById(R.id.testingDebugButton).setEnabled(enabled);
        findViewById(R.id.customButton).setEnabled(enabled);
    }

    private void finished()
    {
        setStatusText(null);
        enableButtons(true);
        downloading = false;
    }

    public void installDaemon(View view)
    {
        setStatusText("Downloading installer package...");

        if (view.getId() == R.id.stableReleaseButton)
        {
            installerKind = InstallerKind.STABLE_RELEASE;
        }
        else if (view.getId() == R.id.stableDebugButton)
        {
            installerKind = InstallerKind.STABLE_DEBUG;
        }
        else if (view.getId() == R.id.testingReleaseButton)
        {
            installerKind = InstallerKind.TESTING_RELEASE;
        }
        else if (view.getId() == R.id.testingDebugButton)
        {
            installerKind = InstallerKind.TESTING_DEBUG;
        }

        downloading = true;
        enableButtons(false);
        downloadInfo();
    }

    public void installCustom(View view)
    {
        String downloadName = null;
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File[] files = dir.listFiles();
        if (files != null) {
            for (File inFile : files) {
                String curName = inFile.getName();
                if (curName.startsWith("nzbget-") && curName.endsWith(".run") &&
                        (downloadName == null || downloadName.compareTo(curName) > 0)) {
                    downloadName = curName;
                }
            }
        }

        enableButtons(false);
        if (downloadName != null)
        {
            installFile(downloadName);
        }
        else
        {
            MessageActivity.showErrorMessage(this, "NZBGet daemon installer",
                    "Could not find NZBGet daemon installer in Download-directory.", null);
            finished();
        }
    }

    private BroadcastReceiver onDownloadFinishReceiver;
    private BroadcastReceiver onNotificationClickReceiver;

    private void downloadInfo()
    {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "nzbget-version-android.json");
        file.delete();

        String url = "http://nzbget.net/info/nzbget-version-android.json";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("NZBGet daemon installer info");
        request.setDescription("nzbget-version-android.json");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "nzbget-version-android.json");

        registerReceiver(onDownloadFinishReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent i)
            {
                infoCompleted(i.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        registerReceiver(onNotificationClickReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                downloadTouched();
            }
        }, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = manager.enqueue(request);
    }

    private void infoCompleted(long aLong)
    {
        Log.i("InstallActivity", "download info completed");
        try
        {
            unregisterReceiver(onDownloadFinishReceiver);
            unregisterReceiver(onNotificationClickReceiver);
        }
        catch (IllegalArgumentException e)
        {
            //Patch for bug: http://code.google.com/p/android/issues/detail?id=6191
        }

        downloadUrl = null;
        downloadName = null;

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "nzbget-version-android.json");
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null)
            {
                if (((installerKind == InstallerKind.STABLE_RELEASE ||
                        installerKind == InstallerKind.STABLE_DEBUG) &&
                        line.indexOf("stable-download") > -1) ||
                        ((installerKind == InstallerKind.TESTING_RELEASE ||
                                installerKind == InstallerKind.TESTING_DEBUG) &&
                                line.indexOf("testing-download") > -1))
                {
                    downloadUrl = line.substring(line.indexOf('"', line.indexOf(":")) + 1, line.length() - 2);
                    if (installerKind == InstallerKind.STABLE_DEBUG ||
                            installerKind == InstallerKind.TESTING_DEBUG)
                    {
                        downloadUrl = downloadUrl.substring(0, downloadUrl.lastIndexOf(".run")) + "-debug.run";
                    }
                    Log.i("InstallActivity", "URL: " + downloadUrl);
                    downloadName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
                    Log.i("InstallActivity", "Name: " + downloadName);
                    break;
                }

                Log.i("InstallActivity", line);
            }
            br.close();
            file.delete();
        }
        catch (IOException e)
        {
            MessageActivity.showErrorMessage(this, "NZBGet daemon installer",
                    "Could not read version info:" + e.getMessage(), null);
            finished();
            return;
        }

        if (downloadUrl.indexOf("nzbget-20.") > -1)
        {
            MessageActivity.showErrorMessage(this, "NZBGet daemon installer",
                    "This installer requires version 21.0, which seems to be not released yet. Please install the testing version instead.", null);
            finished();
            return;
        }

        if (downloadUrl == null)
        {
            MessageActivity.showErrorMessage(this, "NZBGet daemon installer",
                    "Could not read version info: file format error.", null);
            finished();
            return;
        }

        downloadInstaller();
    }

    private void downloadInstaller()
    {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), downloadName);
        file.delete();

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
        request.setTitle("NZBGet daemon installer");
        request.setDescription(downloadName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, downloadName);

        registerReceiver(onDownloadFinishReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent i)
            {
                downloadCompleted(i.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        registerReceiver(onNotificationClickReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                downloadTouched();
            }
        }, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        // get download service and enqueue file
        DownloadManager manager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = manager.enqueue(request);
    }

    private void downloadTouched()
    {
        Log.i("InstallActivity", "Cancelling download");
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("NZBGet daemon installer");
        adb.setMessage("Cancel download and installation?");
        adb.setIcon(android.R.drawable.ic_dialog_alert);
        adb.setCancelable(true);
        adb.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                cancelDownload();
            }
        });
        adb.setNegativeButton("No", null);
        AlertDialog alert = adb.create();
        alert.show();
    }

    private void cancelDownload()
    {
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.remove(downloadId);

        try
        {
            unregisterReceiver(onDownloadFinishReceiver);
            unregisterReceiver(onNotificationClickReceiver);
        }
        catch (IllegalArgumentException e)
        {
            //Patch for bug: http://code.google.com/p/android/issues/detail?id=6191
        }

        finished();
    }

    protected void downloadCompleted(long downloadId)
    {
        downloading = false;

        Log.i("InstallActivity", "download installer completed");
        try
        {
            unregisterReceiver(onDownloadFinishReceiver);
            unregisterReceiver(onNotificationClickReceiver);
        }
        catch (IllegalArgumentException e)
        {
            //Patch for bug: http://code.google.com/p/android/issues/detail?id=6191
        }

        if (!validDownload(downloadId))
        {
            MessageActivity.showErrorMessage(this, "NZBGet daemon installer", "Download failed.", null);
            finished();
            return;
        }

        Log.i("InstallActivity", "download installer successful");
        installFile(downloadName);
    }

    private boolean validDownload(long downloadId)
    {
        //Verify if download is a success
        DownloadManager manager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
        Cursor c = manager.query(new DownloadManager.Query().setFilterById(downloadId));
        if (c.moveToFirst())
        {
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
            if (status == DownloadManager.STATUS_SUCCESSFUL)
            {
                return true;
            }
        }
        return false;
    }

    public void copy(File src, File dst) throws IOException
    {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    private void installFile(String downloadName)
    {
        setStatusText("Installing...");

        installing = true;
        InstallTask task = new InstallTask(this, downloadName);
        Thread thread = new Thread(task);
        thread.start();
    }

    private void installCompleted(final boolean ok, final String errMessage)
    {
        installing = false;
        final InstallActivity activity = this;

        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if (ok)
                {
                    MessageActivity.showOkMessage(activity, "Installation", "NZBGet daemon has been successfully installed.",
                            new MessageActivity.OnClickListener()
                            {
                                public void onClick()
                                {
                                    finish();
                                }
                            });
                }
                else if (errMessage != null)
                {
                    MessageActivity.showErrorMessage(activity, "NZBGet daemon installer", errMessage, null);
                }
                else
                {
                    MessageActivity.showLogMessage(activity, "NZBGet daemon installation failed.");
                }

                finished();
            }
        });
    }

    private class InstallTask implements Runnable
    {

        private InstallActivity activity;
        public String downloadName;

        public InstallTask(InstallActivity activity, String downloadName)
        {
            this.activity = activity;
            this.downloadName = downloadName;
        }

        @Override
        public void run()
        {
            try
            {
                new File("/sdcard/data/nzbget/installer").mkdirs();
                File dest = new File("/sdcard/data/nzbget/installer/nzbget-bin-android.run");
                dest.delete();
                copy(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + downloadName), dest);
            }
            catch (IOException e)
            {
                activity.installCompleted(false, "File copy failed.");
                return;
            }

            boolean ok = Daemon.getInstance().install();
            activity.installCompleted(ok, null);
        }
    }
}
