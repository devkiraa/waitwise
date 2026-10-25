package com.example.waitwise.repositories;

import androidx.annotation.NonNull;

import com.example.waitwise.models.InAppNotification;
import com.example.waitwise.utils.FirebaseManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InAppNotificationRepository {

    public interface NotificationsCallback {
        void onNotificationsLoaded(List<InAppNotification> notifications);
        void onError(String errorMessage);
    }

    private ValueEventListener notificationsListener;
    private DatabaseReference activeUserNotificationsRef;

    public InAppNotificationRepository() {
    }

    public void listenToUserNotifications(@NonNull String userId, @NonNull NotificationsCallback callback) {
        detachListener();

        activeUserNotificationsRef = FirebaseManager.getInstance().getDatabase().getReference("notifications").child(userId);
        notificationsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<InAppNotification> list = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    InAppNotification notif = snap.getValue(InAppNotification.class);
                    if (notif != null) {
                        if (notif.getNotificationId() == null || notif.getNotificationId().isEmpty()) {
                            notif.setNotificationId(snap.getKey());
                        }
                        list.add(notif);
                    }
                }
                Collections.sort(list, (n1, n2) -> Long.compare(n2.getCreatedAt(), n1.getCreatedAt()));
                callback.onNotificationsLoaded(list);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        activeUserNotificationsRef.addValueEventListener(notificationsListener);
    }

    public void saveNotification(@NonNull String userId, @NonNull InAppNotification notification) {
        if (userId.isEmpty()) return;

        DatabaseReference userNotifsRef = FirebaseManager.getInstance().getDatabase().getReference("notifications").child(userId);
        String notifId = notification.getNotificationId();
        if (notifId == null || notifId.isEmpty()) {
            notifId = userNotifsRef.push().getKey();
            notification.setNotificationId(notifId);
        }

        if (notifId != null) {
            userNotifsRef.child(notifId).setValue(notification);
        }
    }

    public void markAsRead(@NonNull String userId, @NonNull String notificationId) {
        if (userId.isEmpty() || notificationId.isEmpty()) return;

        FirebaseManager.getInstance().getDatabase().getReference("notifications")
                .child(userId)
                .child(notificationId)
                .child("read")
                .setValue(true);
    }

    public void detachListener() {
        if (notificationsListener != null && activeUserNotificationsRef != null) {
            activeUserNotificationsRef.removeEventListener(notificationsListener);
            notificationsListener = null;
        }
    }
}
