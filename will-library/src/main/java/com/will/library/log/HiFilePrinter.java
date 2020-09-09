package com.will.library.log;

import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

public class HiFilePrinter implements HiLogPrinter {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private String logPath;
    private long retentionTime;
    private LogWriter writer;
    private volatile PrintWorker worker;
    private static HiFilePrinter instance;

    public static synchronized HiFilePrinter getInstance(String path, long retentionTime) {
        if (instance == null) {
            instance = new HiFilePrinter(path, retentionTime);
        }
        return instance;
    }

    private HiFilePrinter(String path, long retentionTIme) {
        logPath = path;
        this.retentionTime = retentionTIme;
        this.writer = new LogWriter();
        this.worker = new PrintWorker();
        cleanExpiredLog();
    }

    @Override
    public void print(@NonNull HiLogConfig config, int level, String tag, @NonNull String printString) {
        long timeMillis = System.currentTimeMillis();
        if (!worker.isRunning) {
            worker.start();
        }
        worker.put(new HiLogMo(timeMillis, level, tag, printString));
    }

    private void doPrint(HiLogMo logMo) {
        String lastFileName = writer.getPreFileName();
        if (lastFileName == null) {
            String newFileName = createFileName();
            if (writer.isReady()) {
                writer.close();
            }
            if (!writer.ready(newFileName)) {
                return;
            }
        }
        writer.append(logMo.flattenedLog());
    }

    private String createFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(System.currentTimeMillis()));
    }

    private void cleanExpiredLog() {
        if (retentionTime <= 0) {
            return;
        }
        long currentTimeMillis = System.currentTimeMillis();
        File logDir = new File(logPath);
        File[] files = logDir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (currentTimeMillis - file.lastModified() > retentionTime) {
                file.delete();
            }
        }
    }

    private class PrintWorker implements Runnable {
        BlockingDeque<HiLogMo> logs = new LinkedBlockingDeque<>();

        private volatile boolean isRunning;

        void put(HiLogMo logMo) {
            try {
                logs.put(logMo);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        boolean isRunning() {
            synchronized (this) {
                return isRunning;
            }
        }

        void start() {
            synchronized (this) {
                EXECUTOR.execute(this);
                isRunning = true;
            }
        }

        @Override
        public void run() {
            HiLogMo log;

            try {
                while (true) {
                    log = logs.take();
                    doPrint(log);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private class LogWriter {
        private String preFileName;
        private File logFile;
        private BufferedWriter bufferedWriter;

        String getPreFileName() {
            return preFileName;
        }

        boolean isReady() {
            return bufferedWriter != null;
        }

        boolean ready(String newFileName) {
            preFileName = newFileName;
            logFile = new File(logPath, newFileName);

            if (!logFile.exists()) {
                try {
                    File parent = logFile.getParentFile();
                    Log.d("DEBUG", parent.getCanonicalPath());
                    if (!parent.exists()) {
                        boolean result = parent.mkdirs();
                        Log.d("DEBUG", result + "");
                    }
                    logFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                    preFileName = null;
                    logFile = null;
                    return false;
                }
            }

            try {
                bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
            } catch (IOException e) {
                e.printStackTrace();
                preFileName = null;
                logFile = null;
                return false;
            }

            return true;
        }

        boolean close() {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                } finally {
                    bufferedWriter = null;
                    logFile = null;
                    preFileName = null;
                }
            }
            return true;
        }

        void append(String flattenedLog) {
            try {
                bufferedWriter.write(flattenedLog);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
