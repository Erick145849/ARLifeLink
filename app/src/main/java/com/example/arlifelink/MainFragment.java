package com.example.arlifelink;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Objects;

public class MainFragment extends Fragment {
    private RelativeLayout rootLayout;
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
        rootLayout    = view.findViewById(R.id.rootLayout);
        db = FirebaseFirestore.getInstance();
        notesRef = db.collection("notes");

        // Initialize RecyclerView and FloatingActionButton
        recyclerView = view.findViewById(R.id.recyclerViewNotes);
        fabAddNote = view.findViewById(R.id.fabAddNote);

        // Setup Note List and Adapter
        noteList = new ArrayList<>();
        noteAdapter = new NoteAdapter(getContext(), noteList, this);
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
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // 1) cancel any scheduled alarms
                    cancelReminders(requireContext(), noteId);

                    // 2) delete from Firestore
                    DocumentReference noteRef = db.collection("notes").document(noteId);
                    noteRef.delete()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(getContext(), "Note deleted", Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Error deleting note", Toast.LENGTH_SHORT).show()
                            );

                    // 3) remove from local list & notify adapter
                    int posToRemove = -1;
                    for (int i = 0; i < noteList.size(); i++) {
                        if (noteList.get(i).getId().equals(noteId)) {
                            posToRemove = i;
                            break;
                        }
                    }
                    if (posToRemove != -1) {
                        noteList.remove(posToRemove);
                        noteAdapter.notifyItemRemoved(posToRemove);
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void cancelReminders(Context ctx, String noteId) {
        AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        for (int minutesBefore : new int[]{60, 10}) {
            Intent intent = new Intent(ctx, NotificationReceiver.class);
            int code = (noteId + minutesBefore).hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(
                    ctx,
                    code,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pi != null) am.cancel(pi);
        }
    }


    // Load notes from Firestore
    private void loadNotesFromFirestore() {
        String me = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Listen to notes you own
        notesRef.addSnapshotListener((snapshots, error) -> {
            if (error != null) return;

            noteList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Note n = doc.toObject(Note.class);
                n.setId(doc.getId());
                if (me.equals(n.getOwner())
                        || (n.getSharedWith() != null && n.getSharedWith().contains(me))) {
                    noteList.add(n);
                }
            }
            NoteAnalyzer.analyzeNotes(noteList);
            noteAdapter.notifyDataSetChanged();
            swapBackground();
        });
    }

    private void onNotesChanged(QuerySnapshot snapshots, FirebaseFirestoreException error) {
        if (error != null) {
            Log.e("MainFragment", "Firestore error: " + error.getMessage());
            return;
        }
        noteList.clear();
        for (QueryDocumentSnapshot doc : snapshots) {
            Note note = doc.toObject(Note.class);
            note.setId(doc.getId());
            noteList.add(note);
        }
        NoteAnalyzer.analyzeNotes(noteList);
        noteAdapter.notifyDataSetChanged();
        swapBackground();
    }
    public void promptForShareEmail(Note note) {
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        new AlertDialog.Builder(getContext())
                .setTitle("Share Note")
                .setMessage("Enter the email to share with:")
                .setView(input)
                .setPositiveButton("Share", (dlg, which) -> {
                    String email = input.getText().toString().trim();
                    if (!email.isEmpty()) {
                        shareNoteWithEmail(note.getId(), email);
                    } else {
                        Toast.makeText(getContext(), "Email can’t be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void shareNoteWithEmail(String noteId, String targetEmail) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1) Find the user UID by email
        db.collection("users")
                .whereEqualTo("email", targetEmail)
                .get()
                .addOnSuccessListener(q -> {
                    if (q.isEmpty()) {
                        Toast.makeText(getContext(), "User not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String targetUid = q.getDocuments().get(0).getId();

                    // 2) Add them to sharedWith array
                    db.collection("notes")
                            .document(noteId)
                            .update("sharedWith", FieldValue.arrayUnion(targetUid))
                            .addOnSuccessListener(a ->
                                    Toast.makeText(getContext(),
                                            "Shared successfully with " + targetEmail,
                                            Toast.LENGTH_SHORT).show()
                            )
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(),
                                            "Share failed: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Lookup failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void swapBackground() {
        if (noteList.isEmpty()) {
            rootLayout.setBackgroundResource(R.drawable.bg_empty_notes);
        } else {
            rootLayout.setBackgroundResource(R.drawable.bg_with_notes);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        // Optionally reload notes when returning from AddNoteActivity
        loadNotesFromFirestore();
    }

    public void launchArForNote(String title) {
        Intent i = new Intent(getContext(), HelloArActivity.class);
        i.putExtra("NOTE_TEXT", title);
        startActivity(i);
    }
}
