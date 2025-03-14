package com.example.arlifelink;

import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import com.mapbox.maps.Mapbox;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.plugin.annotation.AnnotationManager;
import com.mapbox.maps.plugin.annotation.CircleAnnotation;
import com.mapbox.maps.plugin.annotation.CircleAnnotationOptions;
import com.mapbox.maps.plugin.gestures.OnMapClickListener;

public class MapActivity extends ComponentActivity {
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Mapbox with your access token
        Mapbox.getInstance(this, "YOUR_MAPBOX_ACCESS_TOKEN");

        // Create a MapView programmatically
        mapView = new MapView(this);

        // Set the camera options (center, zoom, etc.)
        mapView.getMapboxMap().setCamera(
                new CameraOptions.Builder()
                        .center(Point.fromLngLat(-98.0, 39.5))
                        .pitch(0.0)
                        .zoom(2.0)
                        .bearing(0.0)
                        .build()
        );

        // Set the content view to the MapView
        setContentView(mapView);

        // Add a click listener to pick location
        mapView.getMapboxMap().addOnMapClickListener(new OnMapClickListener() {
            @Override
            public void onMapClick(Point point) {
                // The tapped location (latitude, longitude)
                double latitude = point.latitude();
                double longitude = point.longitude();

                // Show a marker (circle) at the tapped location
                addMarker(latitude, longitude);

                // Display the picked location in a toast (or you can use it elsewhere)
                Toast.makeText(MapActivity.this, "Location picked: " + latitude + ", " + longitude, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMarker(double latitude, double longitude) {
        // Create a CircleAnnotation (a simple marker) at the picked location
        AnnotationManager annotationManager = mapView.getAnnotationPlugin().getAnnotationManager();
        CircleAnnotationOptions circleOptions = new CircleAnnotationOptions()
                .withLatLng(new com.mapbox.geojson.Point(longitude, latitude)) // Position
                .withCircleColor("#FF0000") // Color of the marker
                .withCircleRadius(10f); // Size of the marker

        CircleAnnotation circle = annotationManager.create(circleOptions);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release the MapView when the activity is destroyed to avoid memory leaks
        mapView.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
