package com.debuglogger;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        toolbar.setTitle("Collect debug logs");

        findViewById(R.id.send_logs).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Collecting logs...", Toast.LENGTH_SHORT).show();

                LogCollector.getInstance().collect(MainActivity.this, LogCollector.LogType.DEBUG, new LogCollector.LogsUriCallback() {

                    @Override
                    public void onCollected(List<Uri> logs) {
                        /* Fallback onto BACKGROUND thread but we don't care in this case */
                        sendSupportEmail(MainActivity.this, logs);
                    }
                });
            }
        });

        findViewById(R.id.init_crash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                throw new RuntimeException("TEST ERROR! Initiate dummy crash and make sure it's getting tracked within logs.");
            }
        });
    }

    /**
     * Send support email with logs.
     */
    public void sendSupportEmail(Context context, List<Uri> logs) {
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("message/rfc822");
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"need.some.help@support.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Support Message");
        intent.putExtra(Intent.EXTRA_TEXT, "Please find logs attached...");

        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, new ArrayList<>(logs));

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(Intent.createChooser(intent, "Email support"));
        } catch (Exception sendReportEx) {
            sendReportEx.printStackTrace();
        }
    }
}