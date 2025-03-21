package com.example.arlifelink;

import java.util.List;

public class Note {
    private String id;
    private String title;
    private String location;
    private String tags;  // Tags or Categories
    private String dueDate;  // Due Date (String or Timestamp)
    private String reminder;  // Reminder time (could be in timestamp or String format)
    private String priority;  // Priority Level (e.g., High, Medium, Low)
    private String color;  // Color for customization (e.g., color code for UI)
    private List<String> attachments;  // List for attachment links or file paths
    private String smallInfo;  // Additional small info about the note

    // Required empty constructor for Firestore
    public Note() {}

    public Note(String title, String location, String tags, String dueDate,
                String reminder, String priority, String color, List<String> attachments, String smallInfo) {
        this.title = title;
        this.location = location;
        this.tags = tags;
        this.dueDate = dueDate;
        this.reminder = reminder;
        this.priority = priority;
        this.color = color;
        this.attachments = attachments;
        this.smallInfo = smallInfo;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getReminder() {
        return reminder;
    }

    public void setReminder(String reminder) {
        this.reminder = reminder;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    public String getSmallInfo() {
        return smallInfo;
    }

    public void setSmallInfo(String smallInfo) {
        this.smallInfo = smallInfo;
    }
}