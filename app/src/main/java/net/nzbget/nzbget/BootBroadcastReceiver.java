package net.nzbget.nzbget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // System just booted up
        if (Daemon.getInstance().status() == Daemon.Status.STATUS_STOPPED) {
            // Daemon is installed and stopped
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPreferences.getBoolean("autostart", true)) { // Default autostart value is true
                // Daemon should start on startup
                Intent serviceIntent = new Intent(context, DaemonService.class);
                serviceIntent.putExtra("command", "start");
                context.startService(serviceIntent);
            }
        }
    }
}
