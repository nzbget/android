package net.nzbget.nzbget;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Daemon
{

    private static Daemon instance = null;
    private static String DAEMONSH = "/data/data/net.nzbget.nzbget/lib/libdaemon.so";

    public static Daemon getInstance()
    {
        if (instance == null)
        {
            instance = new Daemon();
        }
        return instance;
    }

    public enum Status { STATUS_NOTINSTALLED, STATUS_STOPPED, STATUS_RUNNING; };

    public Status status()
    {
        if (! new File("/data/data/net.nzbget.nzbget/nzbget").exists())
        {
            return Status.STATUS_NOTINSTALLED;
        }

        File lockFile = new File("/data/data/net.nzbget.nzbget/nzbget/nzbget.lock");
        if (!lockFile.exists())
        {
            return Status.STATUS_STOPPED;
        }

        try
        {
            Process process = Runtime.getRuntime().exec("ps nzbget");
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int num = 0;
            while (br.readLine() != null)
            {
                num++;
            }
            if (num > 1)
            {
                return Status.STATUS_RUNNING;
            }
        }
        catch (IOException e)
        {
            // ignore
        }

        return Status.STATUS_STOPPED;
    }

    public boolean exec(String cmdLine)
    {
        boolean ok = false;
        try
        {
            Process proc = Runtime.getRuntime().exec(cmdLine);
            proc.waitFor();
            ok = proc.exitValue() == 0;
        }
        catch (Exception e)
        {
            Log.e("Daemon", "Command '" + cmdLine + "' failed", e);
        }
        return ok;
    }

    public boolean install()
    {
        return exec(DAEMONSH + " install");
    }

    public boolean remove()
    {
        return exec(DAEMONSH + " remove");
    }

    public boolean start()
    {
        return exec(DAEMONSH + " start");
    }

    public boolean stop()
    {
        return exec(DAEMONSH + " stop");
    }
}
