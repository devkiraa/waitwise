package com.example.waitwise.utils;

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
import com.example.waitwise.models.InAppNotification;
import com.example.waitwise.models.Order;
import com.example.waitwise.repositories.InAppNotificationRepository;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class InAppNotificationManager {

    public interface OnNotificationListener {
        void onNotificationReceived(InAppNotification notification);
    }

    private static InAppNotificationManager instance;
    private final Map<String, String> lastKnownOrderStatuses = new HashMap<>();
    private final InAppNotificationRepository notificationRepository;

    private Query activeOrdersQuery;
    private ValueEventListener ordersListener;
    private OnNotificationListener activeUiListener;
    private String currentMonitoredUserId;

    private InAppNotificationManager() {
        notificationRepository = new InAppNotificationRepository();
    }

    public static synchronized InAppNotificationManager getInstance() {
        if (instance == null) {
            instance = new InAppNotificationManager();
        }
        return instance;
    }

    public void setUiListener(OnNotificationListener listener) {
        this.activeUiListener = listener;
    }

    public void clearUiListener() {
        this.activeUiListener = null;
    }

    public void startListeningForOrderUpdates(Context context, @NonNull String userId) {
        if (userId.isEmpty()) return;

        // If monitoring same user and listener already attached, return
        if (userId.equals(currentMonitoredUserId) && ordersListener != null) {
            return;
        }

        stopListening();
        currentMonitoredUserId = userId;

        activeOrdersQuery = FirebaseManager.getInstance().getUserOrdersQuery(userId);
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Order order = snap.getValue(Order.class);
                    if (order == null) continue;

                    String orderId = order.getOrderId();
                    if (orderId == null || orderId.isEmpty()) {
                        orderId = snap.getKey();
                        order.setOrderId(orderId);
                    }

                    String currentStatus = order.getOrderStatus();
                    if (currentStatus == null) currentStatus = Order.STATUS_PLACED;

                    String previousStatus = lastKnownOrderStatuses.get(orderId);

                    if (previousStatus == null) {
                        // Initial snapshot when listener attaches - record status without alerting
                        lastKnownOrderStatuses.put(orderId, currentStatus);
                    } else if (!previousStatus.equalsIgnoreCase(currentStatus)) {
                        // Status has transitioned!
                        lastKnownOrderStatuses.put(orderId, currentStatus);

                        if (isNotifiableStatus(currentStatus)) {
                            triggerNotification(context, userId, order, currentStatus);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handled or ignored
            }
        };

        activeOrdersQuery.addValueEventListener(ordersListener);
    }

    private boolean isNotifiableStatus(String status) {
        return Order.STATUS_ACCEPTED.equalsIgnoreCase(status)
                || Order.STATUS_PREPARING.equalsIgnoreCase(status)
                || Order.STATUS_READY.equalsIgnoreCase(status)
                || Order.STATUS_CANCELLED.equalsIgnoreCase(status);
    }

    private void triggerNotification(Context context, String userId, Order order, String currentStatus) {
        long tokenNumber = order.getTokenNumber();
        String title;
        String message;

        switch (currentStatus.toUpperCase()) {
            case Order.STATUS_ACCEPTED:
                title = "Order Accepted!";
                message = "Your order (Token #" + tokenNumber + ") has been accepted by the kitchen.";
                break;
            case Order.STATUS_PREPARING:
                title = "Order in Preparation";
                message = "Your order (Token #" + tokenNumber + ") is now being prepared in the kitchen.";
                break;
            case Order.STATUS_READY:
                title = "Your order is ready!";
                message = "Token #" + tokenNumber + " is ready for pickup. Please collect at the counter.";
                break;
            case Order.STATUS_CANCELLED:
                title = "Order Cancelled";
                message = "Your order (Token #" + tokenNumber + ") was cancelled.";
                break;
            default:
                title = "Order Status Update";
                message = "Your order (Token #" + tokenNumber + ") status changed to " + currentStatus + ".";
                break;
        }

        InAppNotification notification = new InAppNotification(
                null,
                order.getOrderId(),
                tokenNumber,
                title,
                message,
                currentStatus,
                System.currentTimeMillis(),
                false
        );

        // 1. Persist notification in Firebase under /notifications/{userId}
        notificationRepository.saveNotification(userId, notification);

        // 2. Notify active UI listener (if activity is visible in foreground)
        if (activeUiListener != null) {
            activeUiListener.onNotificationReceived(notification);
        }

        // 3. Trigger local Android system notification
        if (context != null) {
            showSystemNotification(context, notification);
        }
    }

    private void showSystemNotification(Context context, InAppNotification notification) {
        String channelId = "waitwise_order_updates";
        String channelName = "Order Status Updates";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Intent intent = new Intent(context, MyTokensActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("orderId", notification.getOrderId());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(notification.getTitle())
                .setContentText(notification.getMessage())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notification.getMessage()))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            int notificationId = (notification.getOrderId() != null)
                    ? notification.getOrderId().hashCode()
                    : (int) System.currentTimeMillis();
            notificationManager.notify(notificationId, builder.build());
        } catch (SecurityException ignored) {
            // Permission denied or suppressed
        }
    }

    public void stopListening() {
        if (activeOrdersQuery != null && ordersListener != null) {
            activeOrdersQuery.removeEventListener(ordersListener);
            ordersListener = null;
            activeOrdersQuery = null;
        }
        currentMonitoredUserId = null;
    }

    public void clearHistory() {
        stopListening();
        lastKnownOrderStatuses.clear();
        clearUiListener();
    }
}
