package com.example.arlifelink;

import android.net.Uri;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public class Note implements Serializable {
    private String id;
    private String title;
    private String location;
    private String dueDate;
    private String tags;  // Tags or Categories
    private String reminder;  // Reminder time (could be in timestamp or String format)
    private String priority;  // Priority Level (e.g., High, Medium, Low)
    private String color;  // Color for customization (e.g., color code for UI)
    private String attachment;  // List for attachment links or file paths
    private String smallInfo;  // Additional small info about the note
    private boolean flagged;

    // Required empty constructor for Firestore
    public Note() {}

    public Note(String title, String location, String tags, String dueDate,
                String reminder, String priority, String color, String attachment, String smallInfo) {
        this.title = title;
        this.location = location;
        this.tags = tags;
        this.dueDate = dueDate;
        this.reminder = reminder;
        this.priority = priority;
        this.color = color;
        this.attachment = attachment;
        this.smallInfo = smallInfo;
        this.flagged = false;
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

    public boolean isFlagged() {
        return flagged;
    }
    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
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

    public String  getAttachment() {
        return attachment;
    }

    public void setAttachment(String attachment) {
        this.attachment = attachment;
    }

    public String getSmallInfo() {
        return smallInfo;
    }

    public void setSmallInfo(String smallInfo) {
        this.smallInfo = smallInfo;
    }
}