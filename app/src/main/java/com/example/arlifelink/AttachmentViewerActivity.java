package com.example.arlifelink;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class AttachmentViewerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attachment_viewer);

        // Check if permission is needed for Android 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadImage();
            } else {
                requestStoragePermission();
            }
        } else {
            // For Android 10 and below, request standard read permission
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                loadImage();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
            startActivity(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImage();
            } else {
                Toast.makeText(this, "Storage permission is required to view attachments", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadImage() {
        ImageView imageView = findViewById(R.id.imageViewAttachment);
        Intent intent = getIntent();
        String attachmentUriString = intent.getStringExtra("attachmentUri");

        if (attachmentUriString != null && !attachmentUriString.isEmpty()) {
            try {
                Uri attachmentUri = Uri.parse(attachmentUriString);
                Log.d("AttachmentViewer", "Attachment URI: " + attachmentUriString); // Debug log
                imageView.setImageURI(attachmentUri);
            } catch (Exception e) {
                Log.e("AttachmentViewer", "Error parsing URI", e);
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e("AttachmentViewer", "No valid attachment URI received");
            Toast.makeText(this, "No attachment found", Toast.LENGTH_SHORT).show();
        }
    }



}
