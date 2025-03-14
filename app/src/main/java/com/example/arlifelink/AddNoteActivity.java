package com.example.arlifelink;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;

public class AddNoteActivity extends AppCompatActivity {
    private EditText titleInput, smallInfoInput;
    private Button dateButton, timeButton, attachButton, btnAddNote;
    private Spinner prioritySpinner, colorSpinner, tagSpinner;
    private TextView locationText;
    private Calendar selectedDateTime;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_note);

        // Initialize UI components
        btnAddNote = findViewById(R.id.addNoteButton);
        titleInput = findViewById(R.id.titleInput);
        smallInfoInput = findViewById(R.id.smallInfoInput);
        dateButton = findViewById(R.id.dateButton);
        timeButton = findViewById(R.id.timeButton);
        attachButton = findViewById(R.id.attachButton);
        prioritySpinner = findViewById(R.id.prioritySpinner);
        colorSpinner = findViewById(R.id.colorSpinner);
        tagSpinner = findViewById(R.id.tagSpinner);
        locationText = findViewById(R.id.locationText);
        selectedDateTime = Calendar.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        setupDateTimePickers();
        setupSpinners();
        setupAttachmentButton();
        fetchLocation();

        // Handle adding note
        btnAddNote.setOnClickListener(v -> {
            // Gather input data
            String title = titleInput.getText().toString();
            String smallInfo = smallInfoInput.getText().toString();
            String tag = (tagSpinner.getSelectedItem() != null) ? tagSpinner.getSelectedItem().toString() : "Default Tag";
            String priority = prioritySpinner.getSelectedItem().toString();
            String color = colorSpinner.getSelectedItem().toString();
            String location = locationText.getText().toString();
            String dueDate = dateButton.getText().toString(); // Use the selected date
            String reminder = timeButton.getText().toString(); // Use the selected time

            // Create a new Note object
            Note newNote = new Note(title, location, tag, dueDate, reminder, priority, color, new ArrayList<>(), smallInfo);

            // Save note to Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("notes")
                    .add(newNote)
                    .addOnSuccessListener(documentReference -> {
                        // Successfully added the note
                        Toast.makeText(AddNoteActivity.this, "Note added", Toast.LENGTH_SHORT).show();

                        // After successfully adding, return to MainFragment and load the notes
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        // Handle error
                        Toast.makeText(AddNoteActivity.this, "Error adding note", Toast.LENGTH_SHORT).show();
                    });
            Intent intent = new Intent(AddNoteActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Clear the current activity stack
            startActivity(intent);  // Start MainActivity and return to MainFragment
        });
    }

    private void setupDateTimePickers() {
        // Set up date picker for dateButton
        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePicker = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDateTime.set(year, month, dayOfMonth);
                        dateButton.setText(dayOfMonth + "/" + (month + 1) + "/" + year);
                    },
                    selectedDateTime.get(Calendar.YEAR),
                    selectedDateTime.get(Calendar.MONTH),
                    selectedDateTime.get(Calendar.DAY_OF_MONTH));
            datePicker.show();
        });

        // Set up time picker for timeButton
        timeButton.setOnClickListener(v -> {
            TimePickerDialog timePicker = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedDateTime.set(Calendar.MINUTE, minute);
                        timeButton.setText(hourOfDay + ":" + String.format("%02d", minute));
                    },
                    selectedDateTime.get(Calendar.HOUR_OF_DAY),
                    selectedDateTime.get(Calendar.MINUTE),
                    true);
            timePicker.show();
        });
    }

    private void setupSpinners() {
        // Set up spinner for priority options
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_options, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        // Set up spinner for color options
        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(this,
                R.array.note_colors, android.R.layout.simple_spinner_item);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(colorAdapter);

        // Set up spinner for tags (If you have tags in a string array, otherwise just remove this part)
        ArrayAdapter<CharSequence> tagAdapter = ArrayAdapter.createFromResource(this,
                R.array.tag_options, android.R.layout.simple_spinner_item);
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagSpinner.setAdapter(tagAdapter);
    }

    private void setupAttachmentButton() {
        // Handle attachment button to pick images
        attachButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 100);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Toast.makeText(this, "Attachment added successfully!", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchLocation() {
        // Request location permissions and fetch location
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        locationText.setText("Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                    }
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }
    }
}
