package com.example.arlifelink;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RegisterActivity extends AppCompatActivity {

    private EditText emailSignUp, passwordSignUp, confirmPasswordSignUp;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register); // Keep the layout the same

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        emailSignUp = findViewById(R.id.emailSignUp);
        passwordSignUp = findViewById(R.id.passwordSignUp);
        confirmPasswordSignUp = findViewById(R.id.confirmPasswordSignUp);
        Button btnSignUp = findViewById(R.id.btnSignUp);

        btnSignUp.setOnClickListener(v -> {
            String email = emailSignUp.getText().toString().trim();
            String password = passwordSignUp.getText().toString();
            String confirmPassword = confirmPasswordSignUp.getText().toString();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(RegisterActivity.this, "Fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(RegisterActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Get the newly registered user
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // 1. Save user profile (email) in Firestore
                                Map<String, Object> userProfile = new HashMap<>();
                                userProfile.put("email", email);
                                db.collection("users")
                                        .document(user.getUid())
                                        .set(userProfile);

                                // 2. Send verification email
                                user.sendEmailVerification()
                                        .addOnCompleteListener(emailTask -> {
                                            if (emailTask.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this,
                                                        "Registration successful. Verification email sent!",
                                                        Toast.LENGTH_LONG).show();
                                                mAuth.signOut(); // Sign out to force email verification
                                                startActivity(new Intent(
                                                        RegisterActivity.this, LoginActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(RegisterActivity.this,
                                                        "Failed to send verification email.",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(RegisterActivity.this,
                                    "Sign Up failed: " +
                                            Objects.requireNonNull(task.getException()).getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        });

        TextView txtAlreadyHaveAccount = findViewById(R.id.txtAlreadyHaveAccount);
        txtAlreadyHaveAccount.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();  // Close RegisterActivity so the user can't go back
        });
    }
}
