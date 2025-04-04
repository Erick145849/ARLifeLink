package com.example.arlifelink;

import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

public class MemoriesMapFragment extends Fragment {
    private MapView mapView;
    private FloatingActionButton floatingActionButton;
    private PointAnnotationManager pointAnnotationManager;

    // Permission Request
    private final ActivityResultLauncher<String> locationPermissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Toast.makeText(requireContext(),
                            isGranted ? "Permission Granted!" : "Permission not granted!",
                            Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final OnIndicatorBearingChangedListener onIndicatorBearingChangedListener = new OnIndicatorBearingChangedListener() {
        @Override
        public void onIndicatorBearingChanged(double bearing) {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder().bearing(bearing).build());
        }
    };

    private final OnIndicatorPositionChangedListener onIndicatorPositionChangedListener = new OnIndicatorPositionChangedListener() {
        @Override
        public void onIndicatorPositionChanged(@NonNull Point point) {
            mapView.getMapboxMap().setCamera(new CameraOptions.Builder().center(point).zoom(18.0).build());
            getGestures(mapView).setFocalPoint(mapView.getMapboxMap().pixelForCoordinate(point));
        }
    };

    private final OnMoveListener onMoveListener = new OnMoveListener() {
        @Override
        public void onMoveBegin(@NonNull MoveGestureDetector detector) {
            LocationComponentPlugin locationComponent = getLocationComponent(mapView);
            locationComponent.removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
            locationComponent.removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
            getGestures(mapView).removeOnMoveListener(onMoveListener);
            floatingActionButton.show();
        }

        @Override
        public boolean onMove(@NonNull MoveGestureDetector detector) {
            return false;
        }

        @Override
        public void onMoveEnd(@NonNull MoveGestureDetector detector) { }
    };

    public MemoriesMapFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_memories_map, container, false);
        mapView = view.findViewById(R.id.mapView);
        floatingActionButton = view.findViewById(R.id.focuslocation);
        floatingActionButton.hide();

        mapView.getMapboxMap().loadStyleUri(Style.STANDARD, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                initializeLocationComponent();
                initializeAnnotations(style);
            }
        });

        floatingActionButton.setOnClickListener(v -> resetLocationTracking());
        locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
        return view;
    }

    private void initializeLocationComponent() {
        LocationComponentPlugin locationComponent = getLocationComponent(mapView);
        locationComponent.setEnabled(true);
        locationComponent.setLocationPuck(new LocationPuck2D());
        locationComponent.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
        locationComponent.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
        getGestures(mapView).addOnMoveListener(onMoveListener);
    }

    private void resetLocationTracking() {
        LocationComponentPlugin locationComponent = getLocationComponent(mapView);
        locationComponent.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener);
        locationComponent.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener);
        getGestures(mapView).addOnMoveListener(onMoveListener);
        floatingActionButton.hide();
    }

    /**
     * Initializes the annotation plugin and loads note markers from Firestore.
     */
    private void initializeAnnotations(Style style) {
        // Retrieve the annotation plugin using the key provided by Mapbox SDK.
        AnnotationPlugin annotationPlugin = (AnnotationPlugin) mapView.getPlugin("com.mapbox.maps.plugin.annotation.AnnotationPlugin");
        if (annotationPlugin != null) {
            // Create the PointAnnotationManager using the Kotlin extension (accessed from Java via the generated class).
            pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, new AnnotationConfig());
            // Load and display markers for the notes.
            loadNotesAndDisplayMarkers();
        } else {
            Toast.makeText(requireContext(), "Annotation plugin not available", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads notes from Firestore and adds a marker for each note.
     * Assumes your Note class has getLatitude() and getLongitude() methods returning Double.
     */
    private void loadNotesAndDisplayMarkers() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("notes").get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot != null) {
                for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                    Note note = doc.toObject(Note.class);
                    if (note != null && note.getLocation() != null) {
                        Point point = parseLocation(note.getLocation());
                        if (point != null) {
                            addMarker(point, note);
                        }
                    }
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(requireContext(), "Error loading notes", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Parses a location string in the format "Lat: x, Lng: y" into a Mapbox Point.
     * Mapbox expects Point.fromLngLat(longitude, latitude).
     */
    private Point parseLocation(String locationString) {
        if (locationString == null || locationString.isEmpty()) {
            return null;
        }
        try {
            // Split the string by comma
            String[] parts = locationString.split(",");
            if (parts.length < 2) return null;
            // parts[0] should be "Lat: x" and parts[1] should be "Lng: y"
            String latPart = parts[0].trim().replace("Lat:", "").trim();
            String lngPart = parts[1].trim().replace("Lng:", "").trim();
            double latitude = Double.parseDouble(latPart);
            double longitude = Double.parseDouble(lngPart);
            return Point.fromLngLat(longitude, latitude);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Adds a marker at the specified point on the map.
     */
    private void addMarker(Point point, Note note) {
        if (pointAnnotationManager == null) return;
        // Create annotation options. You can customize the marker appearance here.
        PointAnnotationOptions options = new PointAnnotationOptions().withPoint(point);
        pointAnnotationManager.create(options);
    }
}
