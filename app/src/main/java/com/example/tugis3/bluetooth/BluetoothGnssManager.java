package com.example.tugis3.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothGnssManager {
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    // Discovery
    private BroadcastReceiver discoveryReceiver;
    private final AtomicBoolean discovering = new AtomicBoolean(false);
    private ScanCallback currentScanCallback;

    // Common SPP UUID
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothGnssManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public interface ScanCallback {
        void onDeviceFound(BluetoothDevice device);
        void onFinished();
        void onError(String message);
    }

    public boolean isBluetoothSupported() {
        return bluetoothAdapter != null;
    }

    public boolean isEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    public android.content.Intent getEnableBluetoothIntent() {
        return new android.content.Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    }

    @SuppressLint("MissingPermission")
    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> list = new ArrayList<>();
        if (bluetoothAdapter == null) return list;
        try {
            Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
            if (paired != null) list.addAll(paired);
        } catch (SecurityException se) {
            // İzin yoksa boş liste döneriz.
        }
        return list;
    }

    /**
     * Klasik Bluetooth discovery (inquiry) başlatır. Runtime izinleri çağıran kısımda kontrol edilmelidir.
     */
    @SuppressLint("MissingPermission")
    public boolean startScan(Context context, ScanCallback callback) {
        if (!isBluetoothSupported()) { if (callback != null) callback.onError("Bluetooth desteklenmiyor"); return false; }
        if (!isEnabled()) { if (callback != null) callback.onError("Bluetooth kapalı"); return false; }
        if (discovering.get()) { if (callback != null) callback.onError("Tarama zaten sürüyor"); return false; }
        currentScanCallback = callback; discovering.set(true);
        if (callback != null) for (BluetoothDevice d : getPairedDevices()) callback.onDeviceFound(d);
        discoveryReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context ctx, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    try {
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        if (device != null && currentScanCallback != null) currentScanCallback.onDeviceFound(device);
                    } catch (Exception ignored) {}
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) { finishDiscoveryInternal(ctx); }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        context.registerReceiver(discoveryReceiver, filter);
        boolean ok;
        try { ok = bluetoothAdapter.startDiscovery(); } catch (SecurityException se) { ok = false; }
        if (!ok) { stopScan(context); if (callback != null) callback.onError("Discovery başlatılamadı (izin?)"); }
        return ok;
    }

    public void stopScan(Context context) { if (!discovering.get()) return; try { bluetoothAdapter.cancelDiscovery(); } catch (Exception ignored) {} finishDiscoveryInternal(context); }
    private void finishDiscoveryInternal(Context context) { if (!discovering.getAndSet(false)) return; try { if (discoveryReceiver != null) context.unregisterReceiver(discoveryReceiver); } catch (IllegalArgumentException ignored) {} if (currentScanCallback != null) currentScanCallback.onFinished(); discoveryReceiver = null; currentScanCallback = null; }
    public boolean isScanning() { return discovering.get(); }

    @SuppressLint("MissingPermission")
    public void connectToDevice(BluetoothDevice device) throws IOException {
        disconnect(); if (device == null) throw new IllegalArgumentException("Device is null");
        try { bluetoothAdapter.cancelDiscovery(); } catch (Exception ignored) {}
        socket = device.createRfcommSocketToServiceRecord(SPP_UUID); socket.connect();
        inputStream = socket.getInputStream(); outputStream = socket.getOutputStream(); }

    public void disconnect() { try { if (inputStream != null) inputStream.close(); } catch (IOException ignored) {} try { if (outputStream != null) outputStream.close(); } catch (IOException ignored) {} try { if (socket != null) socket.close(); } catch (IOException ignored) {} socket = null; inputStream = null; outputStream = null; }
    public boolean isConnected() { return socket != null && socket.isConnected(); }
    public int readAvailable(byte[] buffer) throws IOException { if (inputStream == null) return -1; int available = inputStream.available(); if (available <= 0) return 0; return inputStream.read(buffer, 0, Math.min(buffer.length, available)); }
    public void write(byte[] data) throws IOException { if (outputStream == null) throw new IOException("Not connected"); outputStream.write(data); outputStream.flush(); }

    @SuppressLint("MissingPermission")
    public boolean pairDevice(BluetoothDevice device) { if (device == null) return false; try { if (device.getBondState() == BluetoothDevice.BOND_BONDED) return true; return device.createBond(); } catch (SecurityException se) { return false; } catch (Exception e) { return false; } }
}
