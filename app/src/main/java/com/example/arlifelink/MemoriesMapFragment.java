package com.example.arlifelink;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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

public class MemoriesMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean recenterEnabled = true;
    private FloatingActionButton fab;
    private FirebaseFirestore db;

    // Launcher for location permission
    private final ActivityResultLauncher<String> requestPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                Toast.makeText(requireContext(),
                        isGranted ? "Permission Granted!" : "Permission not granted!",
                        Toast.LENGTH_SHORT).show();
                if (isGranted) startLocationUpdates();
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        db = FirebaseFirestore.getInstance();
        // Receive location updates
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (map == null || result.getLastLocation() == null) return;
                Location loc = result.getLastLocation();
                LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());

                if (recenterEnabled) {
                    // 1) Build a CameraPosition with lat/lng, zoom and the new bearing
                    CameraPosition newCam = new CameraPosition.Builder()
                            .target(ll)               // center
                            .zoom(18f)                // zoom level
                            .bearing(loc.getBearing())// direction the “camera” is pointing
                            .build();

                    // 2) Animate (or move) to that CameraPosition
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(newCam));
                }
            }

        };
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_memories_map, container, false);
        fab = view.findViewById(R.id.focuslocation);
        fab.hide();

        // Obtain the map fragment and set the callback
        SupportMapFragment mapFrag = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        fab.setOnClickListener(v -> {
            recenterEnabled = true;
            fab.hide();
        });

        // Ask for location permission
        requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        return view;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;

        // Disable the default My Location button (we have our own)
        map.getUiSettings().setMyLocationButtonEnabled(false);

        // Hide recenter FAB until user moves the map
        map.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                recenterEnabled = false;
                fab.show();
            }
        });

        // If permission already granted, enable location layer & start updates
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            map.setMyLocationEnabled(true);
            startLocationUpdates();

            // ** NOW load your notes **
            db.collection("notes")
                    .get()
                    .addOnSuccessListener((QuerySnapshot snap) -> {
                        if (snap.isEmpty()) return;

                        // build a LatLngBounds so we can zoom-to-fit all markers at once
                        LatLngBounds.Builder bounds = new LatLngBounds.Builder();

                        // regex to pull out numbers from your "Lat: x, Lng: y" string
                        Pattern p = Pattern.compile("Lat:\\s*([-0-9\\.]+)\\s*,\\s*Lng:\\s*([-0-9\\.]+)");

                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String locStr = doc.getString("location");
                            String title  = doc.getString("title");

                            if (locStr != null) {
                                Matcher m = p.matcher(locStr);
                                if (m.find()) {
                                    double lat = Double.parseDouble(m.group(1));
                                    double lng = Double.parseDouble(m.group(2));
                                    LatLng pos = new LatLng(lat, lng);

                                    // add a marker
                                    map.addMarker(new MarkerOptions()
                                            .position(pos)
                                            .title(title != null ? title : "Untitled"));

                                    bounds.include(pos);
                                }
                            }
                        }

                        // finally, move & zoom the camera so all markers are in view:
                        map.moveCamera(
                                CameraUpdateFactory.newLatLngBounds(bounds.build(), 100)
                        );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(requireContext(),
                                    "Failed to load note locations",
                                    Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void startLocationUpdates() {
        LocationRequest req = LocationRequest.create()
                .setInterval(5_000)
                .setFastestInterval(2_000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        fusedLocationClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper());
    }

    @Override
    public void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }
}
