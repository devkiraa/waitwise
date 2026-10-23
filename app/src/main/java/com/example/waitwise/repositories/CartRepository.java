package com.example.waitwise.repositories;

import androidx.annotation.NonNull;

import com.example.waitwise.models.CartItem;
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

public class CartRepository {

    public interface CartCallback {
        void onCartLoaded(List<CartItem> items);
        void onError(String errorMessage);
    }

    public interface CartActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private ValueEventListener cartListener;
    private DatabaseReference activeUserCartRef;

    public CartRepository() {
    }

    /**
     * Listens to the current user's cart in Firebase Realtime Database.
     */
    public void listenToUserCart(@NonNull String userId, @NonNull CartCallback callback) {
        detachListener();

        activeUserCartRef = FirebaseManager.getInstance().getUserCartRef(userId);
        cartListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CartItem> cartList = new ArrayList<>();
                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    CartItem cartItem = itemSnap.getValue(CartItem.class);
                    if (cartItem != null) {
                        if (cartItem.getFoodId() == null || cartItem.getFoodId().isEmpty()) {
                            cartItem.setFoodId(itemSnap.getKey());
                        }
                        cartList.add(cartItem);
                    }
                }
                callback.onCartLoaded(cartList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        activeUserCartRef.addValueEventListener(cartListener);
    }

    public void detachListener() {
        if (cartListener != null && activeUserCartRef != null) {
            activeUserCartRef.removeEventListener(cartListener);
            cartListener = null;
        }
    }

    /**
     * Adds an item to the cart or increments its quantity if it already exists.
     */
    public void addToCart(@NonNull String userId, @NonNull FoodItem food, final CartActionCallback callback) {
        if (!food.isAvailable()) {
            if (callback != null) callback.onError("This item is currently unavailable.");
            return;
        }

        DatabaseReference itemRef = FirebaseManager.getInstance()
                .getUserCartRef(userId)
                .child(food.getId());

        itemRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int newQuantity = 1;
                if (snapshot.exists()) {
                    CartItem existing = snapshot.getValue(CartItem.class);
                    if (existing != null) {
                        newQuantity = existing.getQuantity() + 1;
                    }
                }

                Map<String, Object> update = new HashMap<>();
                update.put("foodId", food.getId());
                update.put("quantity", newQuantity);

                itemRef.setValue(update)
                        .addOnSuccessListener(aVoid -> {
                            if (callback != null) callback.onSuccess();
                        })
                        .addOnFailureListener(e -> {
                            if (callback != null) callback.onError(e.getMessage());
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onError(error.getMessage());
            }
        });
    }

    /**
     * Updates an existing cart item's quantity. If quantity <= 0, the item is removed.
     */
    public void updateQuantity(@NonNull String userId, @NonNull String foodId, int quantity, final CartActionCallback callback) {
        DatabaseReference itemRef = FirebaseManager.getInstance()
                .getUserCartRef(userId)
                .child(foodId);

        if (quantity <= 0) {
            itemRef.removeValue()
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onError(e.getMessage());
                    });
        } else {
            Map<String, Object> update = new HashMap<>();
            update.put("foodId", foodId);
            update.put("quantity", quantity);

            itemRef.setValue(update)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) callback.onError(e.getMessage());
                    });
        }
    }

    /**
     * Removes an item from the cart.
     */
    public void removeFromCart(@NonNull String userId, @NonNull String foodId, final CartActionCallback callback) {
        FirebaseManager.getInstance()
                .getUserCartRef(userId)
                .child(foodId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    /**
     * Clears all items in the user's cart.
     */
    public void clearCart(@NonNull String userId, final CartActionCallback callback) {
        FirebaseManager.getInstance()
                .getUserCartRef(userId)
                .removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }
}
