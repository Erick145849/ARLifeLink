package com.example.arlifelink;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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

        // Open Mapbox location picker when the button is clicked
        openMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddNoteActivity.this, MapboxLocationPickerActivity.class);
                startActivityForResult(intent, 200);
            }
        });

        // Handle adding note
        btnAddNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String title = titleInput.getText().toString().trim();
                if (title.isEmpty()) title = "Untitled";
                String smallInfo = smallInfoInput.getText().toString();
                String tag = (tagSpinner.getSelectedItem() != null) ? tagSpinner.getSelectedItem().toString() : "Default Tag";
                String priority = prioritySpinner.getSelectedItem().toString();
                String color = colorSpinner.getSelectedItem().toString();
                String location = locationText.getText().toString();
                String dueDate = dateButton.getText().toString() + " " + timeButton.getText().toString();
                String reminder = timeButton.getText().toString();

                // Create a new Note object.
                // Make sure your Note class implements Serializable or Parcelable.
                Note newNote = new Note(title, location, tag, dueDate, reminder, priority, color, attachmentUri, smallInfo);

                // Save note to Firestore and finish AddNoteActivity.
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("notes")
                        .add(newNote)
                        .addOnSuccessListener(documentReference -> {
                            // Optionally, set the document ID in the note if needed.
                            newNote.setId(documentReference.getId());
                            Toast.makeText(AddNoteActivity.this, "Note added", Toast.LENGTH_SHORT).show();
                            scheduleNotification(newNote);

                            // Return to MainFragment.
                            setResult(RESULT_OK);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AddNoteActivity.this, "Error adding note", Toast.LENGTH_SHORT).show();
                        });
                Intent resultIntent = new Intent();
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }
    private void scheduleNotification(Note note) {
        // Prepare AlarmManager and PendingIntent for notification
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("noteTitle", note.getTitle());
        intent.putExtra("noteMessage", "Your note is scheduled at " + note.getDueDate());

        // Use a unique request code (e.g., hash code of note ID or current time)
        int requestCode = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Parse due date string: expected format "dd/MM/yyyy HH:mm"
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        try {
            Date dueDate = sdf.parse(note.getDueDate());
            if (dueDate != null) {
                long triggerTime = dueDate.getTime() - 3600000L; // 1 hour before due date
                // Schedule the alarm
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void setupDateTimePickers() {
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
        ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(this,
                R.array.priority_options, android.R.layout.simple_spinner_item);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        prioritySpinner.setAdapter(priorityAdapter);

        ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(this,
                R.array.note_colors, android.R.layout.simple_spinner_item);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(colorAdapter);

        ArrayAdapter<CharSequence> tagAdapter = ArrayAdapter.createFromResource(this,
                R.array.tag_options, android.R.layout.simple_spinner_item);
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagSpinner.setAdapter(tagAdapter);
    }

    private void setupAttachmentButton() {
        attachButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 100);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Handle image attachment result
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            attachmentUri = data.getData().toString();
            Toast.makeText(this, "Attachment added successfully!", Toast.LENGTH_SHORT).show();
        }
        // Handle location picker result
        else if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            double latitude = data.getDoubleExtra("latitude", 0.0);
            double longitude = data.getDoubleExtra("longitude", 0.0);
            String address = data.getStringExtra("address");
            String pickedLocation = address + " (Lat: " + latitude + ", Lng: " + longitude + ")";
            locationText.setText(pickedLocation);
        }
    }

    private void fetchLocation() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    locationText.setText("Lat: " + location.getLatitude() + ", Lng: " + location.getLongitude());
                }
            });
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
        }
    }
}
