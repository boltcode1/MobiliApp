package com.example.mobiliothonapp;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class UDPServer extends Service {
    private static DatagramSocket socket;
    private boolean isRunning = true;
    private Handler toastHandler;

    public class LocalBinder extends Binder {
        UDPServer getService() {
            // Return this instance of UDPServer so that clients can call public methods
            return UDPServer.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Ensure the socket is created only once
        if (socket == null) {
            try {
                socket = new DatagramSocket(3000);
                Log.i("UDPServer", "Server started and listening on port 3000...");
            } catch (SocketException e) {
                e.printStackTrace();
                Log.e("UDPServer", "Failed to create the socket");
            }
        }
        // Initialize the handler to display toasts
        toastHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start the UDP server in the service
        Thread serverThread = new Thread(new UDPServerRunnable());
        serverThread.start();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            showToast("Server stopped");
            socket.close();
        }

    }

    private class UDPServerRunnable implements Runnable {
        @Override
        public void run() {
            if (socket == null) {
                showToast("Socket NULL");
                return;
            }

            try {
                byte[] buffer = new byte[5000];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.setSoTimeout(5000); // Set a 5-second timeout (adjust as needed)

                while (isRunning) {
                    try {
                        socket.receive(packet);
                        String receivedData = new String(packet.getData(), 0, packet.getLength());

//                        showToast("Received message from " + packet.getAddress() + ":" + packet.getPort() +
//                                " (length: " + packet.getLength() + " bytes)");
//                        showToast("Received data: " + receivedData);

                        Log.i("UDPServer", "Received message from " + packet.getAddress() + ":" + packet.getPort() +  " (length: " + packet.getLength() + " bytes)");
                        Log.i("UDPServer", "Received data: " + receivedData);

                        // Check for thread interruption
                        if (Thread.interrupted()) {
                            showToast("Thread interrupted. Stopping the server.");
                            return;
                        }
                    } catch (SocketTimeoutException e) {
                        showToast("Timeout: No data received");
                    } catch (SocketException se) {
                        if (isRunning) {
                            se.printStackTrace();
                            showToast("An error occurred in the UDP server: " + se.getMessage());
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                showToast("An error occurred in the UDP server: " + e.getMessage());
            } finally {
                if (socket != null && !socket.isClosed()) {
                    showToast("Socket closed");
                    socket.close();
                }
            }
        }
    }

    private void showToast(final String message) {
        toastHandler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(UDPServer.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
