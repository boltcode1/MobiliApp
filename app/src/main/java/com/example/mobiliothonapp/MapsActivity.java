package com.example.mobiliothonapp;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private Marker userMarker;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private float currentSpeed = 0.0f;
    private String username;
    FirebaseDatabase database;
    DatabaseReference reference;
    private static final double MAX_DISTANCE = 5000.0;
    private boolean initialLocationSet = false;
    private boolean UDPLocation = false;
    BitmapDescriptor customMarker, pedMarker, aidMarker;
    private Vibrator vibrator;
    private Handler handler = new Handler();
    private AlertDialog alertDialog;

    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UDPServer.CUSTOM_ACTION.equals(intent.getAction())) {
                boolean isDataReceived = intent.getBooleanExtra("isDataReceived", false);

                // Use the boolean variable as needed
                if (isDataReceived) {
                    UDPLocation = isDataReceived;
                } else {
                    UDPLocation = false;
                }
            }
        }
    };

    // Maintain data structures to track markers for other users
    private List<Marker> otherUserMarkers = new ArrayList<>();
    private Map<String, Marker> otherUserMarkersMap = new HashMap<>();
    private Map<String, Boolean> alerts = new HashMap<>();
    private List<Marker> allMarkers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
        customMarker = BitmapDescriptorFactory.fromResource(R.drawable.car_pin);
        pedMarker = BitmapDescriptorFactory.fromResource(R.drawable.ped_pin);
        aidMarker = BitmapDescriptorFactory.fromResource(R.drawable.emergency);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
                initializeUserLocation();
            } else {
                Toast.makeText(this, "Location permission is required for this app to work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void initializeUserLocation() {
        try {
            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                onLocationChanged(lastLocation);
                renderAllUserLocations(); // Render existing user locations
            }
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "SecurityException: Unable to get last known location.", Toast.LENGTH_SHORT).show();
        }
    }

    private void enableMyLocation() {
        try {
            mMap.setMyLocationEnabled(true);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "SecurityException: Unable to enable My Location.", Toast.LENGTH_SHORT).show();
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Location services are not enabled, prompt the user to enable them
            new AlertDialog.Builder(this)
                    .setTitle("Location Services Required")
                    .setMessage("Please enable location services to use this app.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        database = FirebaseDatabase.getInstance();

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        currentSpeed = location.getSpeed();
        LatLng latLng = new LatLng(latitude, longitude);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            username = extras.getString("username");
        }

        LocationHelper helper = new LocationHelper(
                username,
                location.getLongitude(),
                location.getLatitude(),
                location.getSpeed(),
                false,
                false
        );

        reference = database.getReference("Location");

        if (UDPLocation == false) {
            reference.child(username).setValue(helper).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(MapsActivity.this, "Location saved!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MapsActivity.this, "Unable to save location", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        // Update the user's marker on the map
        if (userMarker != null) {
            userMarker.setPosition(latLng); // Update the existing marker's position
        } else {
            userMarker = mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            ); // Create a new marker if it doesn't exist

            // Zoom in on the main user's location only once
            if (!initialLocationSet) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                initialLocationSet = true;
            }
        }

        renderAllUserLocations();
    }

    private void renderAllUserLocations() {
        reference = database.getReference("Location");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Marker> newMarkers = new ArrayList<>();
                mMap.clear();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    LocationHelper locationData = userSnapshot.getValue(LocationHelper.class);
                    LatLng userLatLng = new LatLng(locationData.getLatitude(), locationData.getLongitude());
                    boolean c = locationData.isCar();
                    boolean emergency = locationData.isEmergency();

                    // Calculate distance between your location and the user's location
                    float[] distanceResult = new float[1];
                    Location.distanceBetween(
                            userMarker.getPosition().latitude, userMarker.getPosition().longitude,
                            userLatLng.latitude, userLatLng.longitude,
                            distanceResult
                    );

                    // Check if the user is within the specified range
                    if (distanceResult[0] <= MAX_DISTANCE) {
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(userLatLng)
                                .title(locationData.getUsername())
                                .snippet("Speed: " + locationData.getCurrentSpeed());

                        if (!c) {
                            markerOptions.icon(pedMarker);
                        } else {
                            if(emergency){
                                String curr = locationData.getUsername();
                                Log.i("Usernames", curr);
                                if((!alerts.containsKey(curr)) && (distanceResult[0] <= 10)){
                                    Drawable customIcon = getResources().getDrawable(R.drawable.baseline_warning_24);
                                    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

                                    // Create an AlertDialog.Builder and set the custom icon
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                                    builder.setIcon(customIcon);

                                    // Set the title and message for the dialog
                                    builder.setTitle("Alert!");
                                    builder.setMessage("Emergency Vehicle Nearby.");

                                    // Create the "OK" button
                                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            // Handle the "OK" button click event
                                            dialog.dismiss(); // Close the dialog
                                            handler.removeCallbacksAndMessages(null); // Remove any delayed dismiss callbacks
                                        }
                                    });

                                    // Create the AlertDialog
                                    alertDialog = builder.create();

                                    // Show the dialog
                                    alertDialog.show();
                                    vibrator.vibrate(500);

                                    // Post a delayed dismissal of the dialog after 5 seconds
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (alertDialog.isShowing()) {
                                                alertDialog.dismiss();
                                            }
                                        }
                                    }, 5000); // 5000 milliseconds
                                    alerts.put(curr, true);
                                }
                                if(distanceResult[0] > 10){
                                    alerts.remove(curr);
                                }

                                markerOptions.icon(aidMarker);
                            }
                            else {
                                markerOptions.icon(customMarker);
                            }
                        }
                        Marker marker = mMap.addMarker(markerOptions);
                        newMarkers.add(marker);
                    } else {
                        // User is outside the range, you can choose to remove the marker
                        // or keep track of it for later removal
                        // In this example, I'm keeping track of markers to remove later
                        if (otherUserMarkersMap.containsKey(locationData.getUsername())) {
                            Marker otherUserMarker = otherUserMarkersMap.get(locationData.getUsername());
                            otherUserMarkersMap.remove(locationData.getUsername());
                            otherUserMarkers.remove(otherUserMarker);
                            otherUserMarker.remove();
                        }
                    }
                }
                clearAndAddMarkers(newMarkers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MapsActivity.this, "Failed to retrieve user locations.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void clearAndAddMarkers(List<Marker> newMarkers) {
        // Clear existing markers from the map
        for (Marker marker : allMarkers) {
            marker.remove();
        }
        allMarkers.clear(); // Clear the list of existing markers

        // Add new markers to the map and the global list
        allMarkers.addAll(newMarkers);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Handle status changes if needed
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Handle provider enabled event if needed
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Handle provider disabled event if needed
    }

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(this); // Stop listening for location updates when the app is paused.
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        }
    }
}