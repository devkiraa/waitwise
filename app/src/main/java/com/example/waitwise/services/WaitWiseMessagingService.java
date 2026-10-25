package com.example.waitwise.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.waitwise.R;
import com.example.waitwise.activities.MyTokensActivity;
import com.example.waitwise.repositories.UserRepository;
import com.example.waitwise.utils.FirebaseManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class WaitWiseMessagingService extends FirebaseMessagingService {

    public static final String CHANNEL_ID = "waitwise_order_updates";
    public static final String CHANNEL_NAME = "Order Status Updates";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        String userId = FirebaseManager.getInstance().getCurrentUserId();
        if (userId != null && !userId.isEmpty()) {
            new UserRepository().updateFcmToken(userId, token);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "WaitWise Order Status";
        String body = "Your order status has been updated.";
        String orderId = null;
        String tokenNumber = null;

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        Map<String, String> data = remoteMessage.getData();
        if (data != null && !data.isEmpty()) {
            if (data.containsKey("title")) title = data.get("title");
            if (data.containsKey("body")) body = data.get("body");
            if (data.containsKey("orderId")) orderId = data.get("orderId");
            if (data.containsKey("tokenNumber")) tokenNumber = data.get("tokenNumber");
        }

        showNotification(title, body, orderId, tokenNumber);
    }

    private void showNotification(String title, String body, String orderId, String tokenNumber) {
        createNotificationChannel();

        Intent intent = new Intent(this, MyTokensActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (orderId != null) {
            intent.putExtra("orderId", orderId);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            int notificationId = (tokenNumber != null) ? tokenNumber.hashCode() : (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Permission denied or suppressed
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for order preparation and pickup alerts");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
