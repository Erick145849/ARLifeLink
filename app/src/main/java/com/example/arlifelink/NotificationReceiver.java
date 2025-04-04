package com.example.arlifelink;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "note_notifications_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String noteTitle = intent.getStringExtra("noteTitle");
        String noteMessage = intent.getStringExtra("noteMessage");

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification) // Replace with your icon
                .setContentTitle(noteTitle)
                .setContentText(noteMessage)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
