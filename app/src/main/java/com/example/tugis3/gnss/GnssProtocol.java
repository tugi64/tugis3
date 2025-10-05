package com.example.tugis3.gnss;

public interface GnssProtocol {
    String getName();
    void start();
    void stop();
    boolean isRunning();
    void sendCommand(String cmd);
}

