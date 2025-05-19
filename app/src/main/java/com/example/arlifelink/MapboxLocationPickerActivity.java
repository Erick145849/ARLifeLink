package com.example.arlifelink;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import android.animation.ValueAnimator;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MapboxLocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private Marker pickMarker;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean recenterEnabled = true;
    private FloatingActionButton fab;
    private Button btnSubmit;

    // Launcher for location permission
    private LatLng selectedLocation;

    @SuppressLint("MissingPermission")
    private final ActivityResultLauncher<String> requestPermission =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        Toast.makeText(this,
                                isGranted ? "Permission Granted!" : "Permission not granted!",
                                Toast.LENGTH_SHORT).show();

                        if (isGranted && map != null) {
                            map.setMyLocationEnabled(true);
                            startLocationUpdates();
                        }
                    }
            );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapbox_location_picker);

        // 1) Setup location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 2) Find your FAB and hide until user drags
        fab = findViewById(R.id.focuslocation);
        btnSubmit = findViewById(R.id.btn_submit);
        fab.hide();
        fab.setOnClickListener(v -> {
            recenterEnabled = true;
            fab.hide();
        });
        btnSubmit.setOnClickListener(v -> {
            if (selectedLocation != null) {
                Intent result = new Intent();
                result.putExtra("latitude",  selectedLocation.latitude);
                result.putExtra("longitude", selectedLocation.longitude);
                setResult(RESULT_OK, result);
                finish();
            } else {
                Toast.makeText(this, "No location selected yet", Toast.LENGTH_SHORT).show();
            }
        });

        // 3) Prep the map fragment
        SupportMapFragment mapFrag = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        // 4) Define what happens on each location update
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (map == null) return;
                Location loc = result.getLastLocation();
                if (loc == null) return;

                LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
                if (recenterEnabled) {
                    CameraPosition cam = new CameraPosition.Builder()
                            .target(ll)
                            .zoom(18f)
                            .bearing(loc.getBearing())
                            .build();
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cam));
                }
            }
        };

        // 5) Ask for permission (this will trigger our launcher callback)
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // Turn off the built-in MyLocation button
        map.getUiSettings().setMyLocationButtonEnabled(false);

        // If we already have permission, enable the layer & start updates
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            map.setMyLocationEnabled(true);
            startLocationUpdates();
        }

        // Show FAB when the user drags the map
        map.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                recenterEnabled = false;
                fab.show();
            }
        });

        map.setOnCameraIdleListener(() -> {
            LatLng newCenter = map.getCameraPosition().target;

            if (pickMarker == null) {
                // first‐time drop
                pickMarker = map.addMarker(new MarkerOptions()
                        .position(newCenter)
                        .title("Picked Location"));
            } else {
                // animate from old position to newCenter
                LatLng start = pickMarker.getPosition();
                ValueAnimator animator = ValueAnimator.ofFloat(0, 1);
                animator.setDuration(600); // half-second
                animator.addUpdateListener(animation -> {
                    float frac = animation.getAnimatedFraction();
                    double lat = (newCenter.latitude  * frac) + (start.latitude  * (1 - frac));
                    double lng = (newCenter.longitude * frac) + (start.longitude * (1 - frac));
                    pickMarker.setPosition(new LatLng(lat, lng));
                });
                animator.start();
            }

            selectedLocation = newCenter;
        });
    }

    private void startLocationUpdates() {
        LocationRequest req = LocationRequest.create()
                .setInterval(5_000)
                .setFastestInterval(2_000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        fusedLocationClient.requestLocationUpdates(
                req, locationCallback, Looper.getMainLooper());
    }

    @Override
    protected void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {

            startLocationUpdates();
        }
    }
}
