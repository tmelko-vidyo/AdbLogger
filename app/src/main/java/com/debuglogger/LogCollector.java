package com.debuglogger;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Singleton logs collector.
 */
public class LogCollector {

    public enum LogType {
        DEBUG,

        // INTERNAL
    }

    /* Singleton one time instance holder */
    private static class InstanceHolder {
        private static final LogCollector HOLDER = new LogCollector();
    }

    private static final String LOG_FILE_DIR = "/Logs/";
    private static final String ADB_LOG_FILE = "adb_log_%s.log";

    private Map<LogType, File> logFilesMap = new HashMap<>();

    /**
     * Fetch logger instance.
     *
     * @return {@link LogCollector}
     */
    public static LogCollector getInstance() {
        return InstanceHolder.HOLDER;
    }

    /**
     * Call once per app lifecycle. From {@link android.app.Application} class.
     *
     * @param context {@link Context}
     */
    public void init(Context context) {
        String today = new SimpleDateFormat("MM-dd-yyyy", Locale.ENGLISH).format(new Date(System.currentTimeMillis()));

        File logDir = new File(context.getFilesDir().getAbsolutePath() + LOG_FILE_DIR);

        /* Add as much log files as you need */
        this.logFilesMap.put(LogType.DEBUG, new File(logDir, String.format(Locale.getDefault(), ADB_LOG_FILE, today)));

        /* Create log files */
        for (File file : this.logFilesMap.values()) {
            try {
                if (!file.exists()) {
                    // noinspection ResultOfMethodCallIgnored
                    file.getParentFile().mkdirs();
                    // noinspection ResultOfMethodCallIgnored
                    file.createNewFile();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /* Clear log files not matching today!! Avoid useless memory usage. */

        /* Clear other logs */
        if (logDir.isDirectory() && logDir.exists()) {
            String[] children = logDir.list();

            for (String child : children) {
                File file = new File(logDir, child);
                if (file.getName().contains(today)) {
                    continue;
                }

                // noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    /**
     * Handle on background thread. Then send logs over.
     * This way of using {@link Thread} is very basic.
     *
     * @param context {@link Context}
     * @param logType {@link LogType} type of logs to collect.
     *                Defined during {@link #init(Context)}
     */
    public void collect(final Context context, LogType logType, final LogsUriCallback callback) {
        final File logFile = this.logFilesMap.get(logType);

        if (logFile == null) throw new RuntimeException("Log type is not defined.");

        /* TODO Wrap onto background thread. Avoid using dummy Thread(). Thread pool is a good option... !!! */
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    flushAdbLogs(logFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                callback.onCollected(asUri(context));
            }
        });

        thread.start();
    }

    /**
     * Fetch log files as {@link Uri} list for further sharing.
     *
     * @param context {@link Context}
     * @return list of log files links.
     */
    private List<Uri> asUri(Context context) {
        List<Uri> items = new ArrayList<>();
        for (File file : this.logFilesMap.values()) {
            items.add(FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".file.provider", file));
        }

        return items;
    }

    /* Get runtime logs as one shot */
    private void flushAdbLogs(File logFile) throws IOException {
        List<String> sCommand = new ArrayList<>();
        sCommand.add("logcat");
        sCommand.add("-d");

        Process process = new ProcessBuilder().command(sCommand).start();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        StringBuilder log = new StringBuilder();
        String line;

        while ((line = bufferedReader.readLine()) != null) {
            log.append(line);
            log.append("\n");
        }

        FileOutputStream fOut = new FileOutputStream(logFile, false);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);
        outputStreamWriter.write(log.toString());

        outputStreamWriter.close();
        fOut.flush();
        fOut.close();

        clearLogs();
    }

    /* clear logs dump but it's not happening all the time.
     * So further logs could be duplicated. Would increase file size. */
    private void clearLogs() {
        try {
            new ProcessBuilder().command("logcat", "-c").redirectErrorStream(true).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface LogsUriCallback {
        void onCollected(List<Uri> logs);
    }
}