package com.example.arlifelink;

import static com.mapbox.maps.plugin.gestures.GesturesUtils.getGestures;
import static com.mapbox.maps.plugin.locationcomponent.LocationComponentUtils.getLocationComponent;

import android.content.Intent;
import android.graphics.PointF;
import android.os.AsyncTask;
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
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.ScreenCoordinate;
import com.mapbox.maps.Style;
import com.mapbox.maps.plugin.LocationPuck2D;
import com.mapbox.maps.plugin.gestures.OnMoveListener;
import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener;
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener;

// Annotation Plugin imports
import com.mapbox.maps.plugin.annotation.AnnotationConfig;
import com.mapbox.maps.plugin.annotation.AnnotationPlugin;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager;
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManagerKt;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MapboxLocationPickerActivity extends AppCompatActivity {

    private MapView mapView;
    private FloatingActionButton floatingActionButton;
    private Button confirmLocationButton;
    private Point selectedLocation;
    private String selectedAddress = "";

    // Replace with your actual Mapbox Access Token
    private static final String MAPBOX_ACCESS_TOKEN = "YOUR_MAPBOX_ACCESS_TOKEN";

    // Annotation manager and marker annotation
    private PointAnnotationManager pointAnnotationManager;
    private PointAnnotation selectedPointAnnotation;

    // Permission request for location
    private final ActivityResultLauncher<String> locationPermissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
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
            selectedLocation = point;
            updateMarker(selectedLocation);
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
            ScreenCoordinate screenCoordinate = new ScreenCoordinate(focalPoint.x, focalPoint.y);
            selectedLocation = mapView.getMapboxMap().coordinateForPixel(screenCoordinate);
            updateMarker(selectedLocation);
            return false;
        }

        @Override
        public void onMoveEnd(@NonNull MoveGestureDetector detector) {
            PointF focalPoint = detector.getFocalPoint();
            ScreenCoordinate screenCoordinate = new ScreenCoordinate(focalPoint.x, focalPoint.y);
            selectedLocation = mapView.getMapboxMap().coordinateForPixel(screenCoordinate);
            if (selectedLocation != null) {
                double latitude = selectedLocation.latitude();
                double longitude = selectedLocation.longitude();
                Toast.makeText(MapboxLocationPickerActivity.this,
                        "Location Picked: " + latitude + ", " + longitude,
                        Toast.LENGTH_SHORT).show();
                updateMarker(selectedLocation);
                // Trigger reverse geocoding to fetch the address
                new ReverseGeocodeTask().execute(selectedLocation);
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapbox_location_picker);

        mapView = findViewById(R.id.mapView);
        floatingActionButton = findViewById(R.id.focuslocation);
        confirmLocationButton = findViewById(R.id.confirm_location_button);

        floatingActionButton.hide();

        // Load the map style and then initialize components
        mapView.getMapboxMap().loadStyleUri(Style.STANDARD, new Style.OnStyleLoaded() {
            @Override
            public void onStyleLoaded(@NonNull Style style) {
                initializeLocationComponent();
                // Retrieve the Annotation Plugin using its key
                AnnotationPlugin annotationPlugin = (AnnotationPlugin) mapView.getPlugin("com.mapbox.maps.plugin.annotation.AnnotationPlugin");
                if (annotationPlugin != null) {
                    // Call the Kotlin extension function from Java via the generated class
                    pointAnnotationManager = PointAnnotationManagerKt.createPointAnnotationManager(annotationPlugin, new AnnotationConfig());
                } else {
                    Toast.makeText(MapboxLocationPickerActivity.this, "Annotation plugin not available", Toast.LENGTH_SHORT).show();
                }
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
            resultIntent.putExtra("address", selectedAddress);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "No location selected", Toast.LENGTH_SHORT).show();
        }
    }

    // Update or create a marker at the selected location
    private void updateMarker(Point location) {
        if (pointAnnotationManager == null) return;
        // If a marker already exists, remove it before adding a new one.
        if (selectedPointAnnotation != null) {
            pointAnnotationManager.delete(selectedPointAnnotation);
        }
        // Create new annotation options with the updated location.
        com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions options =
                new com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions().withPoint(location);
        selectedPointAnnotation = pointAnnotationManager.create(options);
    }

    // AsyncTask to perform reverse geocoding using Mapbox Geocoding API
    private class ReverseGeocodeTask extends AsyncTask<Point, Void, String> {
        @Override
        protected String doInBackground(Point... points) {
            Point point = points[0];
            double longitude = point.longitude();
            double latitude = point.latitude();
            String geocodeUrl = "https://api.mapbox.com/geocoding/v5/mapbox.places/"
                    + longitude + "," + latitude
                    + ".json?access_token=" + MAPBOX_ACCESS_TOKEN;
            try {
                URL url = new URL(geocodeUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    JSONObject jsonObject = new JSONObject(response.toString());
                    JSONArray features = jsonObject.getJSONArray("features");
                    if (features.length() > 0) {
                        JSONObject feature = features.getJSONObject(0);
                        return feature.getString("place_name");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Address not found";
        }

        @Override
        protected void onPostExecute(String address) {
            selectedAddress = address;
            Toast.makeText(MapboxLocationPickerActivity.this,
                    "Address: " + address, Toast.LENGTH_SHORT).show();
        }
    }
}
