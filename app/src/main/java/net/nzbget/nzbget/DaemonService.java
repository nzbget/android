package net.nzbget.nzbget;

import android.app.IntentService;
import android.content.Intent;
import android.widget.Toast;

public class DaemonService extends IntentService {

    public DaemonService() {
        super("NzbGetDaemonService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent,flags,startId);
        // If we get killed, restart and redeliver the intent
        return START_REDELIVER_INTENT;
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
        }
        else
        {
            showMessage("NZBGet daemon could not be stopped.");
        }
        stopSelf();
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

}
