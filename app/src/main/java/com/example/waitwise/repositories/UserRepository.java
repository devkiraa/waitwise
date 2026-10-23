package com.example.waitwise.repositories;

import androidx.annotation.NonNull;

import com.example.waitwise.models.User;
import com.example.waitwise.utils.FirebaseManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class UserRepository {

    public interface UserCallback {
        void onUserLoaded(User user);
        void onError(String errorMessage);
    }

    public interface RoleCallback {
        void onRoleLoaded(String role);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    public UserRepository() {
    }

    /**
     * Retrieves the role ("customer" or "admin") for a user.
     * Defaults safely to "customer" if role is missing or not set.
     */
    public void getUserRole(@NonNull String userId, @NonNull RoleCallback callback) {
        DatabaseReference roleRef = FirebaseManager.getInstance().getUserRef(userId).child("role");
        roleRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getValue() != null) {
                    String role = snapshot.getValue(String.class);
                    callback.onRoleLoaded(role != null ? role.trim().toLowerCase() : User.ROLE_CUSTOMER);
                } else {
                    callback.onRoleLoaded(User.ROLE_CUSTOMER);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // On error or denial, fail safely to customer
                callback.onError(error.getMessage());
            }
        });
    }

    public void getUserProfile(@NonNull String userId, @NonNull UserCallback callback) {
        DatabaseReference userRef = FirebaseManager.getInstance().getUserRef(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    if (user.getId() == null || user.getId().isEmpty()) {
                        user.setId(snapshot.getKey());
                    }
                    callback.onUserLoaded(user);
                } else {
                    callback.onError("User profile not found.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public void saveUserProfile(@NonNull User user, final ActionCallback callback) {
        if (user.getId() == null || user.getId().isEmpty()) {
            if (callback != null) callback.onError("Invalid user ID.");
            return;
        }

        FirebaseManager.getInstance()
                .getUserRef(user.getId())
                .setValue(user)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }
}
