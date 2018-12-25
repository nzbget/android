package net.nzbget.nzbget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;


public class MessageActivity extends AppCompatActivity
{

    public interface OnClickListener
    {
        public void onClick();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);
        String title = getIntent().getStringExtra("title");
        ((TextView)findViewById(R.id.titleLabel)).setText(title);
        String text = Daemon.getInstance(this).lastLog;
        ((TextView)findViewById(R.id.messageText)).setText(text);
    }

    public void dismiss(View view)
    {
        finish();
    }

    public static void showLogMessage(Context context, String title)
    {
        Intent intent = new Intent(context, MessageActivity.class);
        intent.putExtra("title", title);
        context.startActivity(intent);
    }

    public static void showOkMessage(Context context, String title, String text, final OnClickListener click)
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setTitle(title);
        adb.setMessage(text);
        adb.setIcon(android.R.drawable.ic_dialog_info);
        adb.setCancelable(true);
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                if (click != null)
                {
                    click.onClick();
                }
            }
        });
        AlertDialog alert = adb.create();
        alert.show();
    }

    public static void showErrorMessage(Context context, String title, String text, final OnClickListener click)
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setTitle(title);
        adb.setMessage(text);
        adb.setIcon(android.R.drawable.ic_dialog_info);
        adb.setCancelable(true);
        adb.setPositiveButton("OK", new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int id)
            {
                if (click != null)
                {
                    click.onClick();
                }
            }
        });
        AlertDialog alert = adb.create();
        alert.show();
    }
}
