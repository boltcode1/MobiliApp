package com.example.mobiliothonapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private Marker userMarker;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private float currentSpeed = 0.0f;
    private String username;
    FirebaseDatabase database;
    DatabaseReference reference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Location permission is required for this app to work.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableMyLocation() {
        try {
            mMap.setMyLocationEnabled(true);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
        } catch (SecurityException e) {
            e.printStackTrace();
            Toast.makeText(this, "SecurityException: Unable to enable My Location.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
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
                location.getSpeed()
        );

        database = FirebaseDatabase.getInstance();
        reference = database.getReference("Location");

        reference.child(username).setValue(helper).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(MapsActivity.this, "Location saved!", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(MapsActivity.this, "Unable to save location", Toast.LENGTH_SHORT).show();
                }
            }
        });


        // Display the current latitude and longitude in a Toast message
        Toast.makeText(this, "Latitude: " + latitude + ", Longitude: " + longitude, Toast.LENGTH_SHORT).show();

        // Update the user's marker on the map
        if (userMarker != null) {
            userMarker.remove(); // Remove the previous marker
        }
        userMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Your Location"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));


        reference = database.getReference("Location");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mMap.clear(); // Clear existing markers on the map

                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    LocationHelper locationData = userSnapshot.getValue(LocationHelper.class);
                    LatLng userLatLng = new LatLng(locationData.getLatitude(), locationData.getLongitude());

                    // Add markers for each user's location
                    mMap.addMarker(new MarkerOptions()
                            .position(userLatLng)
                            .title(locationData.getUsername())
                            .snippet("Speed: " + locationData.getCurrentSpeed()));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(MapsActivity.this, "Failed to retrieve user locations.", Toast.LENGTH_SHORT).show();
            }
        });
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
}