package net.nzbget.nzbget;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class DaemonService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.daemon_name))
                .setContentText("NZBGet server is running.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);
        Notification notification = builder.build();
        startForeground(6789, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Handle command
        String command = intent.getExtras().getString("command");
        switch (command) {
            case "start":
                startDaemon();
                break;
            case "stop":
                stopDaemon();
                break;
            default:
                showMessage("NZBGet daemon service received an unknown command.");
                break;
        }

        // If we get killed, restart and redeliver the intent
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }


    private void startDaemon()
    {
        boolean ok = Daemon.getInstance().start();
        if (ok)
        {
            showMessage("NZBGet daemon has been successfully started and now is running in background.");
        }
        else
        {
            showMessage("NZBGet daemon could not be started.");
        }
    }

    private void stopDaemon()
    {
        boolean ok = Daemon.getInstance().stop();
        if (ok)
        {
            showMessage("NZBGet daemon has been shut down.");
            stopSelf();
        }
        else
        {
            showMessage("NZBGet daemon could not be stopped.");
        }
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

}
