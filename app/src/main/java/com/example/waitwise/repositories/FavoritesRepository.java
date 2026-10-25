package com.example.waitwise.repositories;

import androidx.annotation.NonNull;

import com.example.waitwise.utils.FirebaseManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

public class FavoritesRepository {

    public interface FavoritesCallback {
        void onFavoritesLoaded(Set<String> favoriteFoodIds);
        void onError(String errorMessage);
    }

    private ValueEventListener favoritesListener;
    private DatabaseReference activeFavoritesRef;

    public FavoritesRepository() {
    }

    public void listenToUserFavorites(@NonNull String userId, @NonNull FavoritesCallback callback) {
        detachListener();

        activeFavoritesRef = FirebaseManager.getInstance().getDatabase().getReference("favorites").child(userId);
        favoritesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Set<String> favoriteIds = new HashSet<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    if (child.getValue(Boolean.class) != null && Boolean.TRUE.equals(child.getValue(Boolean.class))) {
                        favoriteIds.add(child.getKey());
                    }
                }
                callback.onFavoritesLoaded(favoriteIds);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        activeFavoritesRef.addValueEventListener(favoritesListener);
    }

    public void toggleFavorite(@NonNull String userId, @NonNull String foodId, boolean isFavorite) {
        if (userId.isEmpty() || foodId.isEmpty()) return;

        DatabaseReference itemRef = FirebaseManager.getInstance().getDatabase().getReference("favorites").child(userId).child(foodId);
        if (isFavorite) {
            itemRef.setValue(true);
        } else {
            itemRef.removeValue();
        }
    }

    public void detachListener() {
        if (favoritesListener != null && activeFavoritesRef != null) {
            activeFavoritesRef.removeEventListener(favoritesListener);
            favoritesListener = null;
        }
    }
}
