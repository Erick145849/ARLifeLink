package com.example.arlifelink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button btnLogout;
    private TextView txtUserEmail;
    private Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        btnLogout = findViewById(R.id.btnLogout);
        txtUserEmail = findViewById(R.id.txtUserEmail);
        button = findViewById(R.id.ARbutton);

        if (mAuth.getCurrentUser() != null) {
            txtUserEmail.setText("Welcome, " + mAuth.getCurrentUser().getEmail());
        }

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sceneViewerIntent = new Intent(Intent.ACTION_VIEW);
                Uri intentUri =
                        Uri.parse("https://arvr.google.com/scene-viewer/1.0").buildUpon()
                                .appendQueryParameter("file", "https://raw.githubusercontent.com/Erick145849/ARPicture/refs/heads/main/scene.gltf")
                                .appendQueryParameter("mode", "ar_only")
                                .build();
                sceneViewerIntent.setData(intentUri);
                sceneViewerIntent.setPackage("com.google.android.googlequicksearchbox");
                startActivity(sceneViewerIntent);
            }
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
}