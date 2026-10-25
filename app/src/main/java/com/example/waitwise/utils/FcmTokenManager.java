package com.example.waitwise.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.waitwise.repositories.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;

public class FcmTokenManager {

    public static final int REQUEST_CODE_POST_NOTIFICATIONS = 101;

    /**
     * Fetches current FCM token from FirebaseMessaging and saves it to Firebase Realtime Database
     * under users/{userId}/fcmToken.
     */
    public static void syncFcmToken(String userId) {
        if (userId == null || userId.isEmpty()) return;

        FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    if (token != null && !token.isEmpty()) {
                        new UserRepository().updateFcmToken(userId, token);
                    }
                })
                .addOnFailureListener(e -> {
                    // Ignored
                });
    }

    /**
     * Clears FCM token from Firebase Realtime Database for the specified user upon sign-out.
     */
    public static void clearFcmToken(String userId) {
        if (userId == null || userId.isEmpty()) return;
        new UserRepository().clearFcmToken(userId);
    }

    /**
     * Requests POST_NOTIFICATIONS permission on Android 13+ (API 33+) if not already granted.
     */
    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_POST_NOTIFICATIONS
                );
            }
        }
    }
}
