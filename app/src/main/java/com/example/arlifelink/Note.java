package com.example.arlifelink;

import android.net.Uri;

import com.google.ar.core.Pose;
import com.google.firebase.auth.FirebaseAuth;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Note implements Serializable {
    private String id;
    private String title;
    private String location;
    private String owner;
    private String tags;  // Tags or Categories
    private String dueDate;  // Due Date (String or Timestamp)
    private String reminder;  // Reminder time (could be in timestamp or String format)
    private String priority;  // Priority Level (e.g., High, Medium, Low)
    private String color;  // Color for customization (e.g., color code for UI)
    private String attachment;  // List for attachment links or file paths
    private String smallInfo;  // Additional small info about the note
    private boolean flagged;
    List<String> sharedWith = new ArrayList<>();
    // Required empty constructor for Firestore
    public Note() {}

    public Note(String title, String location, String tags, String dueDate,
                String reminder, String priority, String color, String attachment, String smallInfo, String owner) {
        this.title = title;
        this.location = location;
        this.tags = tags;
        this.dueDate = dueDate;
        this.reminder = reminder;
        this.priority = priority;
        this.color = color;
        this.attachment = attachment;
        this.owner = owner;
        this.smallInfo = smallInfo;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(List<String> list) {
        sharedWith = list;
    }

    public boolean isFlagged() {
        return flagged;
    }
    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
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
    public class PoseData {
        public float tx, ty, tz, qx, qy, qz, qw;
        public PoseData() {}
        public PoseData(Pose p) {
            this.tx = p.tx();  this.ty = p.ty();  this.tz = p.tz();
            float[] q = p.getRotationQuaternion();
            this.qx = q[0];   this.qy = q[1];   this.qz = q[2];   this.qw = q[3];
        }
    }

    private PoseData poseData;

    // And these accessors:
    public PoseData getPoseDate() { return poseData; }
    public void setPoseData(PoseData poseData) { this.poseData = poseData; }
}