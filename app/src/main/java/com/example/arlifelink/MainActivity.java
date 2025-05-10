package com.example.arlifelink;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 1234;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load default fragment
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new MainFragment()).commit();

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            if(item.getItemId()==R.id.nav_main)
                selectedFragment = new MainFragment();
            else if(item.getItemId()==R.id.nav_ar) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                            },
                            CAMERA_PERMISSION_CODE
                    );
                    // don’t swap fragments yet – wait for the user to grant
                    return false;
                }
                selectedFragment = new ARFragment();
            }
            else if(item.getItemId()==R.id.nav_memories_map)
                selectedFragment = new MemoriesMapFragment();
            else if(item.getItemId()==R.id.nav_account)
                selectedFragment = new AccountFragment();
            getSupportFragmentManager().beginTransaction().replace(R.id.container, selectedFragment).commit();
            return true;
        });

    }
    @Override
    protected void onStart() {
        super.onStart();

        // Check if the user is already signed in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // If not signed in, redirect to LoginActivity
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            boolean granted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (granted) {
                // now that permissions are in, actually load the ARFragment
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.container, new ARFragment())
                        .commit();
            } else {
                Toast.makeText(this,
                                "Camera & location permissions are required for AR",
                                Toast.LENGTH_LONG)
                        .show();
            }
        }
    }

}