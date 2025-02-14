package com.example.arlifelink;

public class Note {
    private String id;
    private String title;
    private String date;
    private String context;
    private String location;

    // Required empty constructor for Firestore
    public Note() {}

    public Note(String title, String date, String context, String location) {
        this.title = title;
        this.date = date;
        this.context = context;
        this.location = location;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public String getContext() {
        return context;
    }

    public String getLocation() {
        return location;
    }
}
