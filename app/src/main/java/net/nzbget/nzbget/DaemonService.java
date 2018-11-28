package net.nzbget.nzbget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

public class DaemonService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        String channelId = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            channelId = createNotificationChannel();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle(getString(R.string.daemon_name))
                .setContentText("Running")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(contentIntent);
        Notification notification = builder.build();
        startForeground(1, notification);
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
            showMessage("NZBGet daemon has been successfully started and now is running in the background.");
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

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel() {
        String notificationChannelId = "net.nzbget.nzbget";
        String channelName = "NZBGet Daemon Service";
        NotificationChannel chan = new NotificationChannel(notificationChannelId, channelName, NotificationManager.IMPORTANCE_HIGH);
        chan.setLightColor(Color.GREEN);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(chan);
        return notificationChannelId;
    }

}
