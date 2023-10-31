package com.example.mobiliothonapp;

import static android.content.Intent.getIntent;

import android.content.Intent;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class UDPServer extends Service {
    private static DatagramSocket socket;
    private boolean isRunning = true;
    private Handler toastHandler;
    FirebaseDatabase database;
    DatabaseReference reference;
    public String username;
    public static final String CUSTOM_ACTION = "com.example.mobiliothonapp.CUSTOM_ACTION";
    private boolean isDataReceived = false;

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
        if (intent != null) {
            username = intent.getStringExtra("username");
        } else {
            username = "none";
        }

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
                Intent broadcastIntent = new Intent(CUSTOM_ACTION);

                while (isRunning) {

                    try {
                        socket.receive(packet);
                        String receivedData = new String(packet.getData(), 0, packet.getLength(), "UTF-8");

//                          showToast("Received message from " + packet.getAddress() + ":" + packet.getPort() +
//                                  " (length: " + packet.getLength() + " bytes)");
//                          showToast("Received data: " + receivedData);

                          // Sending data to server
                        isDataReceived = true;
                        broadcastIntent.putExtra("isDataReceived", isDataReceived);
                        sendBroadcast(broadcastIntent);
                        try {
                            database = FirebaseDatabase.getInstance();
                            reference = database.getReference("Location");

                            JSONObject json = new JSONObject(receivedData);
                            double latitude = (double) json.get("latitude");
                            double longitude = (double) json.get("longitude");
//                            float currentSpeed = (float) json.get("speed");
                            float currentSpeed = 0.0f;
                            LatLng latLng = new LatLng(latitude, longitude);

//                            Toast.makeText(UDPServer.this, "Latitude: " + latitude + ", Longitude: " + longitude, Toast.LENGTH_SHORT).show();

                            LocationHelper helper = new LocationHelper(
                                    username,
                                    longitude,
                                    latitude,
                                    currentSpeed,
                                    true
                            );

                            reference.child(username).setValue(helper).addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(UDPServer.this, "UDP Location saved!", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(UDPServer.this, "Unable to save location", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });


                            // Handle any exceptions that may occur during parsing
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }

                        Log.i("UDPServer", "Received message from " + packet.getAddress() + ":" + packet.getPort() +  " (length: " + packet.getLength() + " bytes)");
                        Log.i("UDPServer", "Received data: " + receivedData);


                        // Check for thread interruption
                        if (Thread.interrupted()) {
                            showToast("Thread interrupted. Stopping the server.");
                            return;
                        }
                    } catch (SocketTimeoutException e) {
                        isDataReceived = false;
                        broadcastIntent.putExtra("isDataReceived", isDataReceived);
                        sendBroadcast(broadcastIntent);
                        showToast("Timeout: No data received" + username);
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
