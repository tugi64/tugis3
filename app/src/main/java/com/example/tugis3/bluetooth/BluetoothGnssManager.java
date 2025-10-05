package com.example.tugis3.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothGnssManager {
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket socket;
    private InputStream inputStream;
    private OutputStream outputStream;

    // Common SPP UUID
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public BluetoothGnssManager() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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

    public List<BluetoothDevice> getPairedDevices() {
        List<BluetoothDevice> list = new ArrayList<>();
        if (bluetoothAdapter == null) return list;
        Set<BluetoothDevice> paired = bluetoothAdapter.getBondedDevices();
        if (paired != null) {
            list.addAll(paired);
        }
        return list;
    }

    public void connectToDevice(BluetoothDevice device) throws IOException {
        disconnect();
        if (device == null) throw new IllegalArgumentException("Device is null");
        socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
        bluetoothAdapter.cancelDiscovery();
        socket.connect();
        inputStream = socket.getInputStream();
        outputStream = socket.getOutputStream();
    }

    public void disconnect() {
        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException e) { /* ignore */ }
        try {
            if (outputStream != null) outputStream.close();
        } catch (IOException e) { /* ignore */ }
        try {
            if (socket != null) socket.close();
        } catch (IOException e) { /* ignore */ }
        socket = null;
        inputStream = null;
        outputStream = null;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public int readAvailable(byte[] buffer) throws IOException {
        if (inputStream == null) return -1;
        int available = inputStream.available();
        if (available <= 0) return 0;
        int read = inputStream.read(buffer, 0, Math.min(buffer.length, available));
        return read;
    }

    public void write(byte[] data) throws IOException {
        if (outputStream == null) throw new IOException("Not connected");
        outputStream.write(data);
        outputStream.flush();
    }
}

