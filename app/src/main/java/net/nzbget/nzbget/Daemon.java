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
            Process process = Runtime.getRuntime().exec("ps");
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null)
            {
                if (line.indexOf("/data/data/net.nzbget.nzbget/nzbget/nzbget") > -1)
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
            ProcessBuilder builder = new ProcessBuilder(DAEMONSH, command);
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
