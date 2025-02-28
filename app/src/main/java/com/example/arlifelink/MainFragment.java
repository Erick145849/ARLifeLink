package com.example.arlifelink;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentReference;

import java.util.ArrayList;

public class MainFragment extends Fragment {

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private ArrayList<Note> noteList;
    private FloatingActionButton fabAddNote;
    private FirebaseFirestore db;
    private CollectionReference notesRef;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        notesRef = db.collection("notes");

        // Initialize RecyclerView and FloatingActionButton
        recyclerView = view.findViewById(R.id.recyclerViewNotes);
        fabAddNote = view.findViewById(R.id.fabAddNote);

        // Setup Note List and Adapter
        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(noteList, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(noteAdapter);

        // Load notes from Firestore
        loadNotesFromFirestore();

        // Floating Action Button for Adding Notes
        fabAddNote.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("New Note");

            View dialogView = inflater.inflate(R.layout.dialog_add_note, null);
            EditText inputTitle = dialogView.findViewById(R.id.inputTitle);
            EditText inputLocation = dialogView.findViewById(R.id.inputLocation);
            EditText inputDueDate = dialogView.findViewById(R.id.inputDueDate);
            EditText inputReminder = dialogView.findViewById(R.id.inputReminder);
            EditText inputSmallInfo = dialogView.findViewById(R.id.inputSmallInfo);

            Spinner spinnerPriority = dialogView.findViewById(R.id.inputPriority);
            Spinner spinnerTags = dialogView.findViewById(R.id.inputCategory);
            Spinner spinnerColor = dialogView.findViewById(R.id.inputColor);  // Color Spinner

            // Set up spinners with default selections
            ArrayAdapter<CharSequence> priorityAdapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.priority_levels, android.R.layout.simple_spinner_item);
            priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerPriority.setAdapter(priorityAdapter);
            spinnerPriority.setSelection(0);  // Ensure a default value is selected

            ArrayAdapter<CharSequence> tagsAdapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.categories, android.R.layout.simple_spinner_item);
            tagsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTags.setAdapter(tagsAdapter);
            spinnerTags.setSelection(0);  // Ensure a default value is selected

            ArrayAdapter<CharSequence> colorAdapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.color_options, android.R.layout.simple_spinner_item);
            colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerColor.setAdapter(colorAdapter);
            spinnerColor.setSelection(0);  // Ensure a default value is selected

            builder.setView(dialogView);
            builder.setPositiveButton("Add", (dialog, which) -> {
                String title = inputTitle.getText().toString().trim();
                String location = inputLocation.getText().toString().trim();
                String dueDate = inputDueDate.getText().toString().trim();
                String reminder = inputReminder.getText().toString().trim();
                String smallInfo = inputSmallInfo.getText().toString().trim();

                // Get spinner selections safely, use default values if null
                String priority = spinnerPriority.getSelectedItem() != null ? spinnerPriority.getSelectedItem().toString() : "Default Priority";
                String tags = spinnerTags.getSelectedItem() != null ? spinnerTags.getSelectedItem().toString() : "Default Category";
                String color = spinnerColor.getSelectedItem() != null ? spinnerColor.getSelectedItem().toString() : "#FFFFFF";  // Default white color

                if (!title.isEmpty()) {
                    // Create a new note with the color
                    Note newNote = new Note(title, location, tags, dueDate, reminder, priority,
                            color, new ArrayList<>(), smallInfo);  // Pass color to Note constructor

                    // Add the note to Firestore
                    notesRef.add(newNote)
                            .addOnSuccessListener(documentReference -> {
                                String noteId = documentReference.getId();
                                newNote.setId(noteId);
                            })
                            .addOnFailureListener(e -> Log.e("MainFragment", "Error adding note", e));
                }
            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show();
        });

        return view;
    }

    // Load notes from Firestore
    private void loadNotesFromFirestore() {
        notesRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("MainFragment", "Firestore error: " + error.getMessage());
                    return;
                }

                // Clear the list and repopulate
                noteList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Note note = doc.toObject(Note.class);
                    note.setId(doc.getId());
                    Log.d("MainFragment", "Loaded Note: " + note.getTitle() + ", " + note.getDueDate());
                    noteList.add(note);
                }
                noteAdapter.notifyDataSetChanged();
            }
        });
    }

    // Method to delete a note from Firestore
    public void deleteNote(String noteId) {
        DocumentReference noteRef = db.collection("notes").document(noteId);

        noteRef.delete()
                .addOnSuccessListener(aVoid -> {
                    // Successfully deleted the note
                    Toast.makeText(getContext(), "Note deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Failed to delete the note
                    Toast.makeText(getContext(), "Error deleting note", Toast.LENGTH_SHORT).show();
                });
    }
}
