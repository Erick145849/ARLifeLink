package com.example.arlifelink;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.NoteViewHolder> {

    private ArrayList<Note> noteList;
    private MainFragment mainFragment; // Reference to MainFragment to access deleteNote method

    // Constructor
    public NoteAdapter(ArrayList<Note> notes, MainFragment mainFragment) {
        this.noteList = notes;
        this.mainFragment = mainFragment; // Pass MainFragment reference
    }

    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        Note note = noteList.get(position);

        // Bind the data from the Note object to the TextViews in the ViewHolder
        holder.noteTitle.setText(note.getTitle());
        holder.noteDate.setText(note.getDate());
        holder.noteContext.setText(note.getContext());
        holder.noteLocation.setText(note.getLocation());

        // Set up the delete button
        holder.deleteButton.setOnClickListener(v -> {
            // Call deleteNote() method from MainFragment
            mainFragment.deleteNote(note.getId());
        });
    }


    @Override
    public int getItemCount() {
        return noteList.size();
    }

    // ViewHolder class for binding the note views
    public class NoteViewHolder extends RecyclerView.ViewHolder {

        TextView noteTitle, noteDate, noteContext, noteLocation;
        Button deleteButton;

        public NoteViewHolder(View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.textTitle);
            noteDate = itemView.findViewById(R.id.textDate);
            noteContext = itemView.findViewById(R.id.textContext);
            noteLocation = itemView.findViewById(R.id.textLocation);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }


    // Method to bind the note data and set the delete button click listener
        public void bind(String noteId, String title) {
            noteTitle.setText(title);

            // Set up the delete button
            deleteButton.setOnClickListener(v -> {
                // Call deleteNote() method from MainFragment
                mainFragment.deleteNote(noteId);
            });
        }
    }
}