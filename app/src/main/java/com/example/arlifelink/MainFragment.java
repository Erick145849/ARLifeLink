package com.example.arlifelink;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

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
            EditText inputDate = dialogView.findViewById(R.id.inputDate);
            EditText inputContext = dialogView.findViewById(R.id.inputContext);
            EditText inputLocation = dialogView.findViewById(R.id.inputLocation);

            builder.setView(dialogView);
            builder.setPositiveButton("Add", (dialog, which) -> {
                String title = inputTitle.getText().toString().trim();
                String date = inputDate.getText().toString().trim();
                String context = inputContext.getText().toString().trim();
                String location = inputLocation.getText().toString().trim();

                if (!title.isEmpty()) {
                    // Create a new note
                    Note newNote = new Note(title, date, context, location);

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
                    Log.d("MainFragment", "Loaded Note: " + note.getTitle() + ", " + note.getDate() + ", " + note.getContext() + ", " + note.getLocation());
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