package com.example.arlifelink;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

public class AccountFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST    = 1;
    private static final int TAKE_PHOTO_REQUEST    = 2;
    private static final int CAMERA_PERMISSION     = 100;
    private static final int STORAGE_PERMISSION    = 101;

    private ImageView profileImageView;
    private FirebaseAuth    mAuth;
    private FirebaseFirestore db;
    private DocumentReference userRef;
    private TextView userNameEditText;
    private TextView emailTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_account, container, false);

        profileImageView = view.findViewById(R.id.profileImageView);
        userNameEditText = view.findViewById(R.id.userNameTextView);
        emailTextView = view.findViewById(R.id.emailTextView);
        Button btnChangePassword = view.findViewById(R.id.btnChangePassword);
        Button btnDeleteAccount  = view.findViewById(R.id.btnDeleteAccount);
        TextView tvAppVersion     = view.findViewById(R.id.tvAppVersion);
        Button btnLogout          = view.findViewById(R.id.btnLogout);
        profileImageView.setOnClickListener(v -> showImagePickerDialog());
        userNameEditText.setOnClickListener(v -> {
            EditText input = new EditText(requireContext());
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            input.setText(userNameEditText.getText());

            new AlertDialog.Builder(requireContext())
                    .setTitle("Change Username")
                    .setView(input)
                    .setPositiveButton("Save", (dlg, which) -> {
                        String newName = input.getText().toString().trim();
                        if (newName.isEmpty()) {
                            Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // merge into Firestore
                        userRef
                                .set(Collections.singletonMap("userName", newName), SetOptions.merge())
                                .addOnSuccessListener(a -> {
                                    userNameEditText.setText(newName);
                                    Toast.makeText(getContext(), "Username updated", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(getContext(),
                                                "Update failed: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()
                                );
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        // Firebase setup
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return view;

        db      = FirebaseFirestore.getInstance();
        userRef = db.collection("users").document(user.getUid());

        // load saved URI
        userRef.get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String uri = document.getString("profileImageUri");
                        if (uri != null) loadImage(uri);
                    }
                })
                .addOnFailureListener(e -> {
                    // optionally log e.getMessage()
                });
        userRef.get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("userName");
                        if (name != null) {
                            userNameEditText.setText(name);
                        }
                    }
                });
        if (user != null) {
            // load email
            emailTextView.setText(user.getEmail());
        }
        btnChangePassword.setOnClickListener(v -> {
            if (user != null) {
                String email = user.getEmail();
                new AlertDialog.Builder(requireContext())
                        .setTitle("Reset Password")
                        .setMessage("Send password reset email to:\n" + email + "?")
                        .setPositiveButton("Send", (dlg, which) -> {
                            mAuth.sendPasswordResetEmail(email)
                                    .addOnSuccessListener(a ->
                                            Toast.makeText(getContext(),
                                                    "Password reset email sent", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getContext(),
                                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Account")
                    .setMessage("This will remove all your notes and cannot be undone. Proceed?")
                    .setPositiveButton("Delete", (dlg, which) -> {
                        if (user != null) {
                            // remove Firestore docs
                            db.collection("notes")
                                    .whereEqualTo("userId", user.getUid())
                                    .get()
                                    .addOnSuccessListener(snap -> {
                                        for (DocumentSnapshot doc : snap) {
                                            doc.getReference().delete();
                                        }
                                        // then delete user auth
                                        user.delete()
                                                .addOnSuccessListener(x -> {
                                                    Toast.makeText(getContext(),
                                                            "Account deleted", Toast.LENGTH_SHORT).show();
                                                    // return to login
                                                    startActivity(new Intent(getActivity(), LoginActivity.class)
                                                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
                                                    requireActivity().finish();
                                                })
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(getContext(),
                                                                "Error deleting user: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show());
                                    });
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        try {
            String version = requireContext()
                    .getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0)
                    .versionName;
            tvAppVersion.setText("App Version: " + version);
        } catch (Exception ignored) {}

// 4) Logout as before
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            requireActivity().finish();
        });
        return view;
    }

    private void showImagePickerDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Profile Picture")
                .setItems(new CharSequence[]{"Take Photo", "Choose from Gallery", "Delete Picture"},
                        (d, which) -> {
                            switch (which) {
                                case 0: takePhoto();       break;
                                case 1: pickFromGallery(); break;
                                case 2: deletePicture();   break;
                            }
                        })
                .show();
    }

    private void pickFromGallery() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                STORAGE_PERMISSION);
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        startActivityForResult(i, PICK_IMAGE_REQUEST);
    }

    private void takePhoto() {
        ActivityCompat.requestPermissions(getActivity(),
                new String[]{android.Manifest.permission.CAMERA},
                CAMERA_PERMISSION);
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(i, TAKE_PHOTO_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickFromGallery();
            } else {
                Toast.makeText(getContext(),
                        "Storage permission is required to pick an image",
                        Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhoto();
            } else {
                Toast.makeText(getContext(),
                        "Camera permission is required to take a photo",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int req, int res, @Nullable Intent data) {
        super.onActivityResult(req, res, data);
        if (res != getActivity().RESULT_OK || data == null) return;

        Uri uri;
        if (req == PICK_IMAGE_REQUEST) {
            uri = data.getData();
        } else if (req == TAKE_PHOTO_REQUEST) {
            Bundle extras = data.getExtras();
            if (extras != null && extras.get("data") instanceof Bitmap) {
                Bitmap bmp = (Bitmap) extras.get("data");
                String path = MediaStore.Images.Media.insertImage(
                        requireActivity().getContentResolver(), bmp, "ProfilePic", null);
                uri = Uri.parse(path);
            } else {
                uri = null;
            }
        } else {
            uri = null;
        }

        if (uri == null) return;

        // **Single** Firestore write + UI update
        userRef
                .set(
                        Collections.singletonMap("profileImageUri", uri.toString()),
                        SetOptions.merge()
                )
                .addOnSuccessListener(aVoid -> {
                    Glide.with(this)
                            .load(uri)
                            .placeholder(R.drawable.default_profile)
                            .into(profileImageView);
                    Toast.makeText(getContext(),
                            "Profile saved!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Save failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
                );
    }

    private void loadImage(String uri) {
        Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.default_profile)
                .into(profileImageView);
    }

    private void deletePicture() {
        userRef.update("profileImageUri", FieldValue.delete())
                .addOnSuccessListener(v -> profileImageView.setImageResource(R.drawable.default_profile))
                .addOnFailureListener(e -> Toast.makeText(getContext(),
                        "Error deleting: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
