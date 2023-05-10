package com.social.amigos.notifications;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;

import androidx.core.app.NotificationCompat;

public class OreoAndAboveNotification extends ContextWrapper {

    private static final String ID = "some_id";
    private static final String NAME = "FirebaseAPP";

    private NotificationManager notificationManager;

    public OreoAndAboveNotification(Context base) {
        super(base);
        createChannel();
    }

    private void createChannel() {
        NotificationChannel notificationChannel = new NotificationChannel(ID, NAME, NotificationManager.IMPORTANCE_HIGH);
        notificationChannel.enableLights(true);
        notificationChannel.enableVibration(true);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        getManager().createNotificationChannel(notificationChannel);
    }

    public NotificationManager getManager() {
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return notificationManager;
    }

    public NotificationCompat.Builder getONotifications(String title, String body, PendingIntent pIntent, Uri soundUri, String icon) {
        return new NotificationCompat.Builder(getApplicationContext(), ID).setContentIntent(pIntent).setContentTitle(title).setContentText(body).setSound(soundUri).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_MAX).setSmallIcon(Integer.parseInt(icon));
    }
}