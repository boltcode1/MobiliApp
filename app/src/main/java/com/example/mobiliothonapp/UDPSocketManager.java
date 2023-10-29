package com.example.mobiliothonapp;
import java.net.DatagramSocket;
import java.net.SocketException;

public class UDPSocketManager {
    private DatagramSocket socket;
    private int port = 3000;

    public UDPSocketManager() {
        try {
            socket = new DatagramSocket(port);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public DatagramSocket getSocket() {
        return socket;
    }
}
