package com.example.waitwise.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class FirebaseManager {

    private static FirebaseManager instance;
    private final FirebaseAuth auth;
    private final FirebaseDatabase database;

    private static final String DATABASE_URL = "https://waitwise-in-default-rtdb.asia-southeast1.firebasedatabase.app";

    private FirebaseManager() {
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(DATABASE_URL);
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public FirebaseDatabase getDatabase() {
        return database;
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    public String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public boolean isUserLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    public void signOut() {
        auth.signOut();
    }

    // Node references according to schema:
    public DatabaseReference getMenuRef() {
        return database.getReference("menu");
    }

    public DatabaseReference getUsersRef() {
        return database.getReference("users");
    }

    public DatabaseReference getUserRef(String uid) {
        return database.getReference("users").child(uid);
    }

    public DatabaseReference getCartsRef() {
        return database.getReference("carts");
    }

    public DatabaseReference getUserCartRef(String uid) {
        return database.getReference("carts").child(uid);
    }

    public DatabaseReference getOrdersRef() {
        return database.getReference("orders");
    }

    public Query getUserOrdersQuery(String uid) {
        return database.getReference("orders").orderByChild("userId").equalTo(uid);
    }

    public DatabaseReference getTokensRef() {
        return database.getReference("tokens");
    }
}
