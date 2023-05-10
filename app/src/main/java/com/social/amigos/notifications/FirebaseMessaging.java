package com.social.amigos.notifications;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.social.amigos.ChatActivity;
import com.social.amigos.FriendRequestsActivity;
import com.social.amigos.GroupChatActivity;
import com.social.amigos.ThereProfileActivity;

public class FirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        SharedPreferences sp = getSharedPreferences("SP_USER", MODE_PRIVATE);
        String savedCurrentUser = sp.getString("Current_USERID", "None");

        String notificationType = remoteMessage.getData().get("notificationType");
        assert notificationType != null;
        switch (notificationType) {

            case "ChatNotification": {

                String sent = remoteMessage.getData().get("sent");
                String user = remoteMessage.getData().get("user");

                FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

                assert sent != null;
                if (fUser != null && sent.equals(fUser.getUid())) {
                    assert savedCurrentUser != null;
                    if (!savedCurrentUser.equals(user)) {

                        String icon = remoteMessage.getData().get("icon");
                        String title = remoteMessage.getData().get("title");
                        String body = remoteMessage.getData().get("body");

                        assert user != null;
                        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
                        Intent intent = new Intent(this, ChatActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("hisUid", user);
                        intent.putExtras(bundle);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT);

                        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        OreoAndAboveNotification notification1 = new OreoAndAboveNotification(this);
                        NotificationCompat.Builder builder = notification1.getONotifications(title, body, pIntent, defSoundUri, icon);

                        int j = 0;
                        if (i > 0) {
                            j = i;
                        }
                        notification1.getManager().notify(j, builder.build());
                    }
                }
                break;
            }

            case "GroupChatNotification": {

                String sent = remoteMessage.getData().get("sent");
                String user = remoteMessage.getData().get("user");
                String groupId = remoteMessage.getData().get("groupId");

                FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

                assert sent != null;
                if (fUser != null && sent.equals(fUser.getUid())) {
                    assert savedCurrentUser != null;
                    if (!savedCurrentUser.equals(user)) {
                        String icon = remoteMessage.getData().get("icon");
                        String title = remoteMessage.getData().get("title");
                        String body = remoteMessage.getData().get("body");

                        assert user != null;
                        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
                        Intent intent = new Intent(this, GroupChatActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("groupId", groupId);
                        intent.putExtras(bundle);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT);

                        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        OreoAndAboveNotification notification1 = new OreoAndAboveNotification(this);
                        NotificationCompat.Builder builder = notification1.getONotifications(title, body, pIntent, defSoundUri, icon);

                        int j = 0;
                        if (i > 0) {
                            j = i;
                        }
                        notification1.getManager().notify(j, builder.build());
                    }
                }
                break;
            }

            case "SendRequestNotification": {

                String sent = remoteMessage.getData().get("sent");
                String user = remoteMessage.getData().get("user");

                FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

                assert sent != null;
                if (fUser != null && sent.equals(fUser.getUid())) {
                    assert savedCurrentUser != null;
                    if (!savedCurrentUser.equals(user)) {

                        String icon = remoteMessage.getData().get("icon");
                        String title = remoteMessage.getData().get("title");
                        String body = remoteMessage.getData().get("body");

                        assert user != null;
                        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
                        Intent intent = new Intent(this, FriendRequestsActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT);

                        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        OreoAndAboveNotification notification1 = new OreoAndAboveNotification(this);
                        NotificationCompat.Builder builder = notification1.getONotifications(title, body, pIntent, defSoundUri, icon);

                        int j = 0;
                        if (i > 0) {
                            j = i;
                        }
                        notification1.getManager().notify(j, builder.build());
                    }
                }
                break;
            }

            case "AcceptRequestNotification": {

                String sent = remoteMessage.getData().get("sent");
                String user = remoteMessage.getData().get("user");

                FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();

                assert sent != null;
                if (fUser != null && sent.equals(fUser.getUid())) {
                    assert savedCurrentUser != null;
                    if (!savedCurrentUser.equals(user)) {

                        String icon = remoteMessage.getData().get("icon");
                        String title = remoteMessage.getData().get("title");
                        String body = remoteMessage.getData().get("body");

                        assert user != null;
                        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
                        Intent intent = new Intent(this, ThereProfileActivity.class);
                        Bundle bundle = new Bundle();
                        bundle.putString("uid", user);
                        intent.putExtras(bundle);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        @SuppressLint("UnspecifiedImmutableFlag") PendingIntent pIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT);

                        Uri defSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

                        OreoAndAboveNotification notification1 = new OreoAndAboveNotification(this);
                        NotificationCompat.Builder builder = notification1.getONotifications(title, body, pIntent, defSoundUri, icon);

                        int j = 0;
                        if (i > 0) {
                            j = i;
                        }
                        notification1.getManager().notify(j, builder.build());
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            updateToken(s);
        }
    }

    private void updateToken(String tokenRefresh) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token = new Token(tokenRefresh);
        assert user != null;
        ref.child(user.getUid()).setValue(token);
    }
}