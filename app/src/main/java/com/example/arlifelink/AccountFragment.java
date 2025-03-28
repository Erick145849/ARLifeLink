package com.example.arlifelink;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;

public class AccountFragment extends Fragment {

    private FirebaseAuth mAuth;
    private Button btnLogout, saveButton;
    private TextView txtUserEmail, userNameTextView, emailTextView;
    private ImageView profileImageView;
    private Switch darkModeSwitch, notificationsSwitch;
    private static final int PICK_IMAGE_REQUEST = 1; // Code for picking image
    private static final int TAKE_PHOTO_REQUEST = 2;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;

    private SharedPreferences sharedPreferences;

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        profileImageView = view.findViewById(R.id.profileImageView);
        userNameTextView = view.findViewById(R.id.userNameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        notificationsSwitch = view.findViewById(R.id.notificationsSwitch);
        saveButton = view.findViewById(R.id.saveButton);
        btnLogout = view.findViewById(R.id.btnLogout);

        // Initialize SharedPreferences
        profileImageView.setOnClickListener(v -> showProfileImageOptionsDialog());
        sharedPreferences = getActivity().getSharedPreferences("UserPreferences", Context.MODE_PRIVATE);

        // Load saved preferences
        loadPreferences();

        // Set the user's info (if logged in)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            emailTextView.setText(currentUser.getEmail());
            loadProfileImage(currentUser);
        }

        // Set listener for the save button
        saveButton.setOnClickListener(v -> savePreferences());

        // Set listener for the logout button
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            getActivity().finish();
        });

        return view;
    }

    private void showProfileImageOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Profile Picture Options")
                .setItems(new CharSequence[]{"Change Picture", "Delete Picture"}, (dialog, which) -> {
                    if (which == 0) { // Change Picture selected
                        showImagePickerDialog();
                    } else if (which == 1) { // Delete Picture selected
                        deleteProfilePicture();
                    }
                })
                .create().show();
    }

    private void deleteProfilePicture() {
        // Reset the profile image to the default one
        profileImageView.setImageResource(R.drawable.default_profile);  // Replace "default_profile" with your default image

        // Optionally, show a Toast message indicating success
        Toast.makeText(getActivity(), "Profile picture deleted", Toast.LENGTH_SHORT).show();
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose Profile Picture")
                .setItems(new CharSequence[]{"Camera", "Gallery"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) { // Camera option selected
                            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                                    != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
                            } else {
                                openCamera();
                            }
                        } else if (which == 1) { // Gallery option selected
                            openGallery();
                        }
                    }
                })
                .create().show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    // Open the camera to take a photo
    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, TAKE_PHOTO_REQUEST);
    }
    // Method to load saved preferences (dark mode, notifications)
    private void loadPreferences() {
        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", false);
        boolean isNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true);

        darkModeSwitch.setChecked(isDarkMode);
        notificationsSwitch.setChecked(isNotificationsEnabled);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == getActivity().RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                Uri selectedImageUri = data.getData();
                try {
                    Bitmap selectedImageBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImageUri);
                    profileImageView.setImageBitmap(selectedImageBitmap);  // Set the image in the ImageView
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getActivity(), "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == TAKE_PHOTO_REQUEST) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                profileImageView.setImageBitmap(photo);  // Set the captured photo in the ImageView
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(getActivity(), "Camera permission is required to take a photo", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(getActivity(), "Storage permission is required to pick an image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Method to save preferences
    private void savePreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean isDarkMode = darkModeSwitch.isChecked();
        editor.putBoolean("dark_mode", isDarkMode);
        editor.putBoolean("notifications_enabled", notificationsSwitch.isChecked());

        // Optionally, you can save user info as well
        editor.putString("user_name", userNameTextView.getText().toString());
        editor.putString("user_email", emailTextView.getText().toString());

        // Apply the changes
        editor.apply();

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        // Provide feedback to the user
        Toast.makeText(getActivity(), "Settings saved successfully!", Toast.LENGTH_SHORT).show();
    }

    // Method to load the user's profile image from Firebase Authentication (getPhotoUrl)
    private void loadProfileImage(FirebaseUser user) {
        if (user.getPhotoUrl() != null) {
            // If the user has a photo, load it into the ImageView
            Glide.with(this)
                    .load(user.getPhotoUrl())
                    .into(profileImageView);

        } else {
            // Set a default image if the user doesn't have a profile photo
            profileImageView.setImageResource(R.drawable.default_profile);
        }
    }
}
