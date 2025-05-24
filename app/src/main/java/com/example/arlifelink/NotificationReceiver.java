package com.example.arlifelink;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String EXTRA_NOTE_ID    = "extra_note_id";
    public static final String EXTRA_NOTE_TITLE = "extra_note_title";
    public static final String EXTRA_OFFSET     = "extra_offset";
    private static final String CHANNEL_ID      = "arlifelink_notes";

    @Override
    public void onReceive(Context context, Intent intent) {
        // 1) Make sure notifications are allowed
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return;

        // 2) Create channel if needed
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                    new NotificationChannel(CHANNEL_ID,
                            "Note reminders",
                            NotificationManager.IMPORTANCE_HIGH)
            );
        }

        // 3) Build & fire the notification
        String title  = intent.getStringExtra(EXTRA_NOTE_TITLE);
        String offset = intent.getStringExtra(EXTRA_OFFSET);  // “hour” or “ten”
        String when   = offset.equals("hour") ? "in 1 hour" : "in 10 minutes";

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)    // add this drawable
                .setContentTitle("Reminder: " + title)
                .setContentText("Your note \"" + title + "\" is due " + when)
                .setAutoCancel(true);

        nm.notify((title + offset).hashCode(), b.build());
    }
}
