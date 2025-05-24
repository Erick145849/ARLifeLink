package com.example.arlifelink;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AddNoteActivity extends AppCompatActivity {
    private EditText titleInput, smallInfoInput;
    private Button dateButton, timeButton, attachButton, btnAddNote, openMapButton;
    private Spinner prioritySpinner, colorSpinner, tagSpinner;
    private TextView locationText;
    private Calendar selectedDateTime;
    private FusedLocationProviderClient fusedLocationClient;
    private String attachmentUri = "";

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
        openMapButton = findViewById(R.id.locationPicker);

        setupDateTimePickers();
        setupSpinners();
        setupAttachmentButton();
        fetchLocation();

        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddNoteActivity.this, MapboxLocationPickerActivity.class);
                startActivityForResult(intent, 200);
            }
        });

        // Handle adding note
        btnAddNote.setOnClickListener(v -> {
            // Gather input data
            String title = titleInput.getText().toString().trim();
            if (title.isEmpty()) title = "Untitled";
            String smallInfo = smallInfoInput.getText().toString();
            String tag = (tagSpinner.getSelectedItem() != null) ? tagSpinner.getSelectedItem().toString() : "Default Tag";
            String priority = prioritySpinner.getSelectedItem().toString();
            String color = colorSpinner.getSelectedItem().toString();
            String location = locationText.getText().toString();
            String dueDate = dateButton.getText().toString(); // Use the selected date
            String reminder = timeButton.getText().toString(); // Use the selected time
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            // Create a new Note object
            Note newNote = new Note(title, location, tag, dueDate, reminder, priority, color, attachmentUri, smallInfo, uid);

            // Save note to Firestore
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("notes")
                    .add(newNote)
                    .addOnSuccessListener(documentReference -> {
                        // Successfully added the note
                        String noteId = documentReference.getId();
                        String noteTitle = titleInput.getText().toString().trim();
                        long noteTimeMillis = selectedDateTime.getTimeInMillis();
                        scheduleReminder(this, noteId, noteTitle, noteTimeMillis, 60);
                        scheduleReminder(this, noteId, noteTitle, noteTimeMillis, 10);
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
    private void scheduleReminder(Context ctx,
                                  String noteId,
                                  String title,
                                  long noteTimeMillis,
                                  int minutesBefore) {
        long triggerAt = noteTimeMillis - minutesBefore * 60_000L;
        if (triggerAt < System.currentTimeMillis()) return; // don’t schedule past

        Intent intent = new Intent(ctx, NotificationReceiver.class)
                .putExtra("extra_note_id", noteId)
                .putExtra("extra_note_title", title)
                .putExtra("extra_offset", minutesBefore == 60 ? "hour" : "ten");

        int requestCode = (noteId + minutesBefore).hashCode();
        PendingIntent pi = PendingIntent.getBroadcast(
                ctx,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                // If you call from an Activity, you can cast ctx to Activity;
                // otherwise add FLAG_ACTIVITY_NEW_TASK:
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ctx.startActivity(i);
                return;
            }
        }
        AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(triggerAt, pi);
        am.setAlarmClock(info, pi);

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
            attachmentUri = data.getData().toString();  // Get the URI of the picked image
            Toast.makeText(this, "Attachment added successfully!", Toast.LENGTH_SHORT).show();
        }
        else if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            double lat = data.getDoubleExtra("latitude", 0.0);
            double lng = data.getDoubleExtra("longitude", 0.0);

            // format however you like
            String picked = String.format("Lat: %.5f, Lng: %.5f", lat, lng);
            locationText.setText(picked);
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
