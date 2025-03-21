package com.example.arlifelink;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class MainFragment extends Fragment {

    private RecyclerView recyclerView;
    private NoteAdapter noteAdapter;
    private ArrayList<Note> noteList;
    private FloatingActionButton fabAddNote;
    private FirebaseFirestore db;
    private CollectionReference notesRef;
    private static final int REQUEST_CODE_ADD_NOTE = 1;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

        fabAddNote.setOnClickListener(v -> {
            // Start the AddNoteActivity to create a new note
            Intent intent = new Intent(getActivity(), AddNoteActivity.class);
            startActivityForResult(intent, REQUEST_CODE_ADD_NOTE);
        });

        return view;
    }

    // Handle the result when AddNoteActivity returns
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_ADD_NOTE && resultCode == getActivity().RESULT_OK && data != null) {
            // Retrieve the entire Note object from the Intent
            Note newNote = (Note) data.getSerializableExtra("newNote"); // Assuming Note implements Serializable

            if (newNote != null) {
                // Add the new note to Firestore
                notesRef.add(newNote)
                        .addOnSuccessListener(documentReference -> {
                            newNote.setId(documentReference.getId()); // Set the document ID
                            noteList.add(newNote);  // Add to the local list
                            noteAdapter.notifyItemInserted(noteList.size() - 1);  // Notify the adapter for the new item
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error adding note", Toast.LENGTH_SHORT).show();
                        });
            }
        }
    }


    public void deleteNote(final String noteId) {
        // Show confirmation dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Proceed with deleting the note from Firestore
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

                        // Optionally remove the note from the local list (RecyclerView)
                        for (Note note : noteList) {
                            if (note.getId().equals(noteId)) {
                                noteList.remove(note);
                                noteAdapter.notifyItemRemoved(noteList.indexOf(note));  // Notify the adapter to remove the item
                                break;
                            }
                        }
                    }
                })
                .setNegativeButton("No", null)  // Cancel the delete operation
                .show();
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

                // Clear the list and repopulate with updated notes
                noteList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    Note note = doc.toObject(Note.class);
                    note.setId(doc.getId()); // Set the document ID as the note's ID
                    Log.d("MainFragment", "Loaded Note: " + note.getTitle());
                    noteList.add(note);
                }
                // Notify the adapter to update the RecyclerView
                noteAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Optionally reload notes when returning from AddNoteActivity
        loadNotesFromFirestore();
    }
}
