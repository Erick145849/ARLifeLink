package com.example.arlifelink;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.LinearLayout;

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
        return new NoteViewHolder(view); // Pass the inflated view to the ViewHolder
    }

    @Override
    public void onBindViewHolder(NoteViewHolder holder, int position) {
        Note note = noteList.get(position);

        // Bind the data from the Note object to the TextViews in the ViewHolder
        holder.noteTitle.setText(note.getTitle() != null ? note.getTitle() : "No Title");
        holder.noteDate.setText(note.getDueDate() != null ? note.getDueDate() : "No Date");
        holder.noteLocation.setText(note.getLocation() != null ? note.getLocation() : "No Location");
        holder.noteCategory.setText(note.getTags() != null ? note.getTags() : "No Category");
        holder.notePriority.setText(note.getPriority() != null ? note.getPriority() : "No Priority");
        holder.noteSmallInfo.setText(note.getSmallInfo() != null ? note.getSmallInfo() : "No Info");

        if (note.getAttachments() != null && !note.getAttachments().isEmpty()) {
            holder.noteAttachments.setText("Attachments: " + note.getAttachments().size());
        } else {
            holder.noteAttachments.setText("No attachments");
        }

        // Set color dynamically
        String color = note.getColor();
        if (color != null && !color.equals("0") && !color.isEmpty()) {
            try {
                holder.noteColor.setBackgroundColor(Color.parseColor(color));  // Set color to noteColor view
            } catch (IllegalArgumentException e) {
                Log.e("NoteAdapter", "Invalid color string: " + color);
                holder.noteColor.setBackgroundColor(Color.GRAY);  // Default color in case of invalid input
            }
        } else {
            holder.noteColor.setBackgroundColor(Color.GRAY);  // Default color if it's invalid
        }

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

        TextView noteTitle, noteDate, noteCategory, notePriority, noteLocation, noteSmallInfo, noteAttachments;
        Button deleteButton;
        View noteColor; // Change to View instead of LinearLayout

        @SuppressLint("WrongViewCast")
        public NoteViewHolder(View itemView) {
            super(itemView);
            noteTitle = itemView.findViewById(R.id.textTitle);
            noteDate = itemView.findViewById(R.id.textDate);
            noteCategory = itemView.findViewById(R.id.textCategory);
            notePriority = itemView.findViewById(R.id.textPriority);
            noteLocation = itemView.findViewById(R.id.textLocation);
            noteSmallInfo = itemView.findViewById(R.id.textSmallInfo);
            noteAttachments = itemView.findViewById(R.id.textAttachments);
            noteColor = itemView.findViewById(R.id.noteColor); // noteColor is now a View
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
