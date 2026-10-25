package com.example.waitwise.repositories;

import androidx.annotation.NonNull;

import com.example.waitwise.models.FoodItem;
import com.example.waitwise.utils.FirebaseManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuRepository {

    public interface MenuCallback {
        void onMenuLoaded(List<FoodItem> items);
        void onError(String errorMessage);
    }

    public interface SingleItemCallback {
        void onItemLoaded(FoodItem item);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private final DatabaseReference menuRef;
    private ValueEventListener menuListener;

    public MenuRepository() {
        this.menuRef = FirebaseManager.getInstance().getMenuRef();
    }

    /**
     * Attaches a live listener to fetch all menu items from Firebase Realtime Database.
     */
    public void listenToMenu(@NonNull MenuCallback callback) {
        detachListener();

        menuListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FoodItem> items = new ArrayList<>();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    FoodItem food = itemSnapshot.getValue(FoodItem.class);
                    if (food != null) {
                        if (food.getId() == null || food.getId().isEmpty()) {
                            food.setId(itemSnapshot.getKey());
                        }
                        items.add(food);
                    }
                }
                callback.onMenuLoaded(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        menuRef.addValueEventListener(menuListener);
    }

    public void getMenuOnce(@NonNull MenuCallback callback) {
        menuRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<FoodItem> items = new ArrayList<>();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    FoodItem food = itemSnapshot.getValue(FoodItem.class);
                    if (food != null) {
                        if (food.getId() == null || food.getId().isEmpty()) {
                            food.setId(itemSnapshot.getKey());
                        }
                        items.add(food);
                    }
                }
                callback.onMenuLoaded(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Detaches the listener to prevent memory leaks when the Activity/View is destroyed.
     */
    public void detachListener() {
        if (menuListener != null) {
            menuRef.removeEventListener(menuListener);
            menuListener = null;
        }
    }

    /**
     * Single fetch of a food item by ID.
     */
    public void getFoodItem(String foodId, @NonNull SingleItemCallback callback) {
        menuRef.child(foodId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                FoodItem food = snapshot.getValue(FoodItem.class);
                if (food != null) {
                    if (food.getId() == null || food.getId().isEmpty()) {
                        food.setId(snapshot.getKey());
                    }
                    callback.onItemLoaded(food);
                } else {
                    callback.onError("Food item not found");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Adds a new product to Firebase Realtime Database with a unique push ID.
     */
    public void addProduct(@NonNull FoodItem food, final ActionCallback callback) {
        String newId = menuRef.push().getKey();
        if (newId == null) {
            if (callback != null) callback.onError("Failed to generate product identifier.");
            return;
        }
        food.setId(newId);
        if (food.getCreatedAt() <= 0) {
            food.setCreatedAt(System.currentTimeMillis());
        }

        menuRef.child(newId).setValue(food)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Updates an existing food product.
     */
    public void updateProduct(@NonNull FoodItem food, final ActionCallback callback) {
        if (food.getId() == null || food.getId().isEmpty()) {
            if (callback != null) callback.onError("Product ID is required for updating.");
            return;
        }

        menuRef.child(food.getId()).setValue(food)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Deletes a food product from the menu.
     * Note: Does not touch historical order items.
     */
    public void deleteProduct(@NonNull String foodId, final ActionCallback callback) {
        menuRef.child(foodId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Toggles availability of a product (true = available, false = sold out / unavailable).
     */
    public void toggleProductAvailability(@NonNull String foodId, boolean available, final ActionCallback callback) {
        Map<String, Object> update = new HashMap<>();
        update.put("available", available);

        menuRef.child(foodId).updateChildren(update)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }
}
