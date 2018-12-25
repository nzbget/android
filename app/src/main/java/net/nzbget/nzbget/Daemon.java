package net.nzbget.nzbget;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Daemon
{

    private static Daemon instance = null;
    private static String mDataDir; // Most of the time, dataDir will be "/data/data/net.nzbget.nzbget"

    public static Daemon getInstance(Context context)
    {
        if (instance == null)
        {
            instance = new Daemon();
        }
        if (mDataDir == null) {
            mDataDir = context.getApplicationInfo().dataDir;
        }
        return instance;
    }

    public enum Status { STATUS_NOTINSTALLED, STATUS_STOPPED, STATUS_RUNNING; };

    public Status status()
    {
        if (! new File(mDataDir,"nzbget").exists())
        {
            return Status.STATUS_NOTINSTALLED;
        }

        File lockFile = new File(mDataDir, "nzbget/nzbget.lock");
        if (!lockFile.exists())
        {
            //Toast.makeText(context, lockFile.getPath() + " does not exist", Toast.LENGTH_SHORT).show();
            return Status.STATUS_STOPPED;
        }

        try
        {
            Process process = Runtime.getRuntime().exec(new File(mDataDir, "xbin/ps").getPath()+" -ww");
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null)
            {
                if (line.indexOf("net.nzbget.nzbget/nzbget/nzbget") > -1)
                {
                    return Status.STATUS_RUNNING;
                }
            }
        }
        catch (IOException e)
        {
            // ignore
        }
        return Status.STATUS_STOPPED;
    }

    public boolean execDaemon(String command)
    {
        boolean ok = false;
        try
        {
            String DaemonShPath = new File(mDataDir, "lib/libdaemon.so").getPath();
            ProcessBuilder builder = new ProcessBuilder(DaemonShPath, command, mDataDir);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            lastLog = "";
            String line;
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = br.readLine()) != null)
            {
                lastLog += line + "\n";
            }

            process.waitFor();
            ok = process.exitValue() == 0;
        }
        catch (Exception e)
        {
            Log.e("Daemon", "Command '" + command + "' failed", e);
        }
        return ok;
    }

    public String lastLog;

    public boolean install()
    {
        return execDaemon("install");
    }

    public boolean remove()
    {
        return execDaemon("remove");
    }

    public boolean start()
    {
        return execDaemon("start");
    }

    public boolean stop()
    {
        return execDaemon("stop");
    }
}
