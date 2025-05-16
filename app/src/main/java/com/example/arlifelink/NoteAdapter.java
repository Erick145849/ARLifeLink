package com.example.arlifelink;

import static androidx.core.content.ContextCompat.startActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private Context context;
    private ArrayList<Note> noteList;
    private MainFragment mainFragment; // Reference to MainFragment to access deleteNote method

    // Constructor
    public NoteAdapter(Context context, ArrayList<Note> notes, MainFragment mainFragment) {
        this.context = context;
        this.noteList = notes;
        this.mainFragment = mainFragment; // Pass MainFragment reference
    }


    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view); // Pass the inflated view to the ViewHolder
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        Note note = noteList.get(position);

        // Bind the data from the Note object to the TextViews in the ViewHolder
        holder.noteTitle.setText(note.getTitle() != null ? note.getTitle() : "");
        holder.noteDate.setText(note.getDueDate() != null ? note.getDueDate() : "");
        holder.noteLocation.setText(note.getLocation() != null ? note.getLocation() : "");
        holder.noteCategory.setText(note.getTags() != null ? note.getTags() : "");
        holder.notePriority.setText(note.getPriority() != null ? note.getPriority() : "");
        holder.noteSmallInfo.setText(note.getSmallInfo() != null ? note.getSmallInfo() : "");


        holder.viewAttachmentsButton.setVisibility(View.VISIBLE);
        holder.viewAttachmentsButton.setOnClickListener(v -> {
            String attachmentUriString = note.getAttachment();

            if (attachmentUriString != null && !attachmentUriString.isEmpty()) {
                Intent intent = new Intent(context, AttachmentViewerActivity.class);
                intent.putExtra("attachmentUri", attachmentUriString);
                context.startActivity(intent);
            } else {
                Toast.makeText(context, "No attachment found!", Toast.LENGTH_SHORT).show();
            }
        });



        // Set dynamic background color with rounded corners
        GradientDrawable background = (GradientDrawable) holder.itemView.getBackground();
        String color = note.getColor();

        if (color != null && !color.equals("0") && !color.isEmpty()) {
            try {
                background.setColor(Color.parseColor(color));  // Apply note color
            } catch (IllegalArgumentException e) {
                Log.e("NoteAdapter", "Invalid color for note ID " + note.getId() + ": " + color);
                background.setColor(Color.LTGRAY);  // Default fallback color
            }
        } else {
            background.setColor(Color.LTGRAY);  // Default if no color provided
        }
        holder.shareButton.setVisibility(View.VISIBLE);
        holder.shareButton.setOnClickListener(v -> {
            // Pop up a dialog to enter recipient email
            final EditText input = new EditText(context);
            new AlertDialog.Builder(context)
                    .setTitle("Share Note")
                    .setMessage("Enter email to share with:")
                    .setView(input)
                    .setPositiveButton("Share", (dialog, which) -> {
                        String email = input.getText().toString().trim();
                        if (!email.isEmpty()) {
                            shareNoteInRTDB(
                                    FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                    note.getId(),
                                    input.getText().toString()
                            );
                        } else {
                            Toast.makeText(context, "Email cannot be empty", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        // Set up the delete button
        holder.deleteButton.setOnClickListener(v -> {
            // Call deleteNote() method from MainFragment
            mainFragment.deleteNote(note.getId());
        });
    }

    private Context getContext() {
        return this.context;
    }

    private void shareNoteInRTDB(String ownerUid, String noteId, String recipientEmail) {
        // 1) Find the recipient’s UID by their email
        DatabaseReference usersRef = FirebaseDatabase.getInstance()
                .getReference("users");
        Query query = usersRef.orderByChild("email")
                .equalTo(recipientEmail.trim().toLowerCase(Locale.US));
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(context, "No user with that email", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Grab the first match
                String recipientUid = snapshot.getChildren().iterator().next().getKey();

                // 2) Update only that one note’s sharedWith list
                DatabaseReference sharedRef = FirebaseDatabase.getInstance()
                        .getReference("userNotes")
                        .child(ownerUid)
                        .child(noteId)
                        .child("sharedWith")
                        .child(recipientUid);

                sharedRef.setValue(true)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(context, "Shared!", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Share failed: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show()
                        );
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(context, "Lookup failed: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    public int getItemCount() {
        return noteList.size();  // Return the size of noteList
    }

    // ViewHolder class for binding the note views
    public class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView noteTitle, noteDate, noteCategory, notePriority, noteLocation, noteSmallInfo, noteAttachments;
        Button deleteButton, viewAttachmentsButton, shareButton;
        View noteColor; // Change to View instead of LinearLayout
        public ImageView attachmentImageView;

        @SuppressLint("WrongViewCast")
        public NoteViewHolder(View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.textTitle);
            noteDate = itemView.findViewById(R.id.textDate);
            noteLocation = itemView.findViewById(R.id.textLocation);
            noteCategory = itemView.findViewById(R.id.textCategory);
            notePriority = itemView.findViewById(R.id.textPriority);
            noteSmallInfo = itemView.findViewById(R.id.textSmallInfo);

            // Make sure to initialize the viewAttachmentsButton
            viewAttachmentsButton = itemView.findViewById(R.id.viewAttachmentsButton);  // This should match the ID in the layout
            shareButton = itemView.findViewById(R.id.shareButton);
            // Initialize the delete button
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
