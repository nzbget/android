package net.nzbget.nzbget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends ActionBarActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        updateStatus();
        new Timer().schedule(new UpdateTimeTask(), 0, 1000);
    }

    class UpdateTimeTask extends TimerTask
    {
        public void run()
        {
            statusHandler.post(statusRunnable);
        }
    }

    final Runnable statusRunnable = new Runnable()
    {
        public void run()
        {
            updateStatus();
        }
    };

    final Handler statusHandler = new Handler();

    private Daemon.Status lastStatus;

    public void updateStatus()
    {
        Daemon.Status curStatus = Daemon.getInstance().status();
        if (curStatus != lastStatus)
        {
            switch (curStatus)
            {
                case STATUS_NOTINSTALLED:
                    findViewById(R.id.startButton).setEnabled(false);
                    findViewById(R.id.stopButton).setEnabled(false);
                    findViewById(R.id.showWebUIButton).setEnabled(false);
                    findViewById(R.id.removeButton).setEnabled(false);
                    ((TextView) findViewById(R.id.statusLabel)).setText("Daemon Status: not installed");
                    break;

                case STATUS_STOPPED:
                    findViewById(R.id.startButton).setEnabled(true);
                    findViewById(R.id.stopButton).setEnabled(false);
                    findViewById(R.id.showWebUIButton).setEnabled(false);
                    findViewById(R.id.removeButton).setEnabled(true);
                    ((TextView) findViewById(R.id.statusLabel)).setText("Daemon Status: stopped");
                    break;

                case STATUS_RUNNING:
                    findViewById(R.id.startButton).setEnabled(false);
                    findViewById(R.id.stopButton).setEnabled(true);
                    findViewById(R.id.showWebUIButton).setEnabled(true);
                    findViewById(R.id.removeButton).setEnabled(true);
                    ((TextView) findViewById(R.id.statusLabel)).setText("Daemon Status: running");
                    break;
            }
        }
        lastStatus = curStatus;
    }

    static final int UPDATE_STATUS_REQUEST = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == UPDATE_STATUS_REQUEST)
        {
            updateStatus();
        }
    }
    public void installDaemon(View view)
    {
        Intent intent = new Intent(this, InstallActivity.class);
        startActivityForResult(intent, UPDATE_STATUS_REQUEST);
    }

    public void startDaemon(View view)
    {
        Intent intent = new Intent(this, DaemonService.class);
        intent.putExtra("command", "start");
        startService(intent);
        updateStatus();
    }

    public void stopDaemon(View view)
    {
        Intent intent = new Intent(this, DaemonService.class);
        intent.putExtra("command", "stop");
        startService(intent);
        updateStatus();
    }

    public void removeDaemon(View view)
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle("Daemon Removal");
        adb.setMessage("Really remove NZBGet daemon?\n\nThe daemon and configuration file will be removed. All downloaded files (on SDCard) remain.");
        adb.setIcon(android.R.drawable.ic_dialog_alert);
        adb.setCancelable(true);
        final Context me = this;
        adb.setPositiveButton("Yes", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                boolean ok = Daemon.getInstance().remove();
                if (ok)
                {
                    MessageActivity.showOkMessage(me, "Daemon Removal", "NZBGet daemon has been successfully removed.", null);
                }
                else
                {
                    MessageActivity.showLogMessage(me, "NZBGet daemon removal failed.");
                }
                updateStatus();
            }
        });
        adb.setNegativeButton("No", null);
        AlertDialog alert = adb.create();
        alert.show();
    }

    public void showWebUI(View view)
    {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:6789"));
        startActivity(browserIntent);
    }
}
