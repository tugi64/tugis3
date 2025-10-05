package com.example.tugis3.gnss;

import android.util.Log;

import com.example.tugis3.bluetooth.BluetoothGnssManager;

import java.io.IOException;

public class SurvStarProtocol implements GnssProtocol {
    private static final String TAG = "SurvStarProtocol";
    private final BluetoothGnssManager btManager;
    private volatile boolean running = false;
    private Thread readerThread;
    private StringBuilder buffer = new StringBuilder();

    public SurvStarProtocol(BluetoothGnssManager btManager) {
        this.btManager = btManager;
    }

    @Override
    public String getName() {
        return "SurvStar 2025";
    }

    @Override
    public void start() {
        if (running) return;
        running = true;
        readerThread = new Thread(() -> {
            byte[] buf = new byte[1024];
            while (running) {
                try {
                    int n = btManager.readAvailable(buf);
                    if (n > 0) {
                        String s = new String(buf, 0, n);
                        // Accumulate and split into lines
                        buffer.append(s);
                        String all = buffer.toString();
                        String[] lines = all.split("\r?\n");
                        // process all complete lines except possibly last (partial)
                        int processed = 0;
                        for (int i = 0; i < lines.length - 1; i++) {
                            String line = lines[i].trim();
                            if (!line.isEmpty()) {
                                // Try parse as NMEA
                                NmeaParser.Sentence sent = NmeaParser.parse(line);
                                if (sent != null) {
                                    Log.d(TAG, "NMEA parsed: " + NmeaParser.quickSummary(sent));
                                } else {
                                    Log.d(TAG, "Raw (SurvStar): " + line);
                                }
                            }
                            processed++;
                        }
                        // keep last partial line
                        if (lines.length > 0) {
                            buffer.setLength(0);
                            buffer.append(lines[lines.length - 1]);
                        }
                    }
                    try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                } catch (IOException e) {
                    Log.e(TAG, "Read error", e);
                    stop();
                }
            }
            Log.d(TAG, "Reader thread ended");
        }, "SurvStar-Reader");
        readerThread.start();
        // Gönderilecek başlangıç komutları burada konabilir (örneğin baud veya binary mod set)
        try {
            btManager.write(("START_SURVSTAR\r\n").getBytes());
        } catch (IOException e) {
            Log.w(TAG, "Could not send start command: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        running = false;
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void sendCommand(String cmd) {
        try {
            btManager.write((cmd + "\r\n").getBytes());
        } catch (IOException e) {
            Log.w(TAG, "sendCommand error: " + e.getMessage());
        }
    }
}
