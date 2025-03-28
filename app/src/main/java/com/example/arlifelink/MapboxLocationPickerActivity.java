package com.example.arlifelink;

import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;

import android.content.Intent;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.ScreenCoordinate;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.gestures.GesturesPlugin;
import com.mapbox.geojson.Point;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

public class MapboxLocationPickerActivity extends AppCompatActivity {
    private MapView mapView;
    private FloatingActionButton floatingActionButton;
    private Button confirmLocationButton;
    private Point selectedLocation;

    // Permission Request
    private final ActivityResultLauncher<String> locationPermissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Toast.makeText(MapboxLocationPickerActivity.this,
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
            selectedLocation = point;  // Set selected location to current position
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
            PointF focalPoint = detector.getFocalPoint();

            // Convert PointF (x, y) to ScreenCoordinate (Mapbox uses ScreenCoordinate for pixel-based calculations)
            ScreenCoordinate screenCoordinate = new ScreenCoordinate(focalPoint.x, focalPoint.y);

            // Convert the screen coordinates to map coordinates (latitude, longitude)
            selectedLocation = mapView.getMapboxMap().coordinateForPixel(screenCoordinate);
            // Update selected location
            return false;
        }

        @Override
        public void onMoveEnd(@NonNull MoveGestureDetector detector) {
            PointF focalPoint = detector.getFocalPoint();

            // Convert PointF (x, y) to ScreenCoordinate (Mapbox uses ScreenCoordinate for pixel-based calculations)
            ScreenCoordinate screenCoordinate = new ScreenCoordinate(focalPoint.x, focalPoint.y);

            // Convert the screen coordinates to map coordinates (latitude, longitude)
            selectedLocation = mapView.getMapboxMap().coordinateForPixel(screenCoordinate);


            if (selectedLocation != null) {
                double latitude = selectedLocation.latitude();
                double longitude = selectedLocation.longitude();
                Toast.makeText(MapboxLocationPickerActivity.this,
                        "Location Picked: " + latitude + ", " + longitude,
                        Toast.LENGTH_SHORT).show();
            }
        }

    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapbox_location_picker);

        mapView = findViewById(R.id.mapView);
        floatingActionButton = findViewById(R.id.focuslocation);
        confirmLocationButton = findViewById(R.id.confirm_location_button); // Button to confirm selection

        floatingActionButton.hide();

        mapView.getMapboxMap().loadStyleUri(Style.STANDARD, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                initializeLocationComponent();
            }
        });

        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetLocationTracking();
            }
        });

        confirmLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                returnSelectedLocation();
            }
        });

        locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION);
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

    private void returnSelectedLocation() {
        if (selectedLocation != null) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("latitude", selectedLocation.latitude());
            resultIntent.putExtra("longitude", selectedLocation.longitude());
            setResult(RESULT_OK, resultIntent);
            finish(); // Close the activity and return the result
        } else {
            Toast.makeText(this, "No location selected", Toast.LENGTH_SHORT).show();
        }
    }
}
