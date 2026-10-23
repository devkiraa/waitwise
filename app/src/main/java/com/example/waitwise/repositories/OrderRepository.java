package com.example.waitwise.repositories;

import androidx.annotation.NonNull;

import com.example.waitwise.models.Order;
import com.example.waitwise.utils.FirebaseManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderRepository {

    public interface OrdersCallback {
        void onOrdersLoaded(List<Order> orders);
        void onError(String errorMessage);
    }

    public interface PlaceOrderCallback {
        void onSuccess(Order createdOrder);
        void onError(String errorMessage);
    }

    public interface ActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private ValueEventListener ordersListener;
    private Query activeOrdersQuery;

    private ValueEventListener allOrdersListener;
    private DatabaseReference allOrdersRef;

    public OrderRepository() {
    }

    /**
     * Atomically allocates a unique sequential token number using Firebase runTransaction
     * and persists the new order to the database.
     */
    public void placeOrder(@NonNull Order order, @NonNull PlaceOrderCallback callback) {
        DatabaseReference tokenRef = FirebaseManager.getInstance().getTokensRef().child("lastToken");

        tokenRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Long currentToken = currentData.getValue(Long.class);
                if (currentToken == null || currentToken < 1000L) {
                    currentToken = 1000L;
                }
                long nextToken = currentToken + 1;
                currentData.setValue(nextToken);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
                if (!committed || error != null || currentData == null) {
                    callback.onError(error != null ? error.getMessage() : "Failed to generate token.");
                    return;
                }

                Long allocatedToken = currentData.getValue(Long.class);
                if (allocatedToken == null) {
                    callback.onError("Could not retrieve allocated token.");
                    return;
                }

                // Generate Firebase push ID for order
                DatabaseReference ordersRef = FirebaseManager.getInstance().getOrdersRef();
                String orderId = ordersRef.push().getKey();
                if (orderId == null) {
                    callback.onError("Could not generate order identifier.");
                    return;
                }

                order.setOrderId(orderId);
                order.setTokenNumber(allocatedToken);
                if (order.getCreatedAt() <= 0) {
                    order.setCreatedAt(System.currentTimeMillis());
                }

                ordersRef.child(orderId).setValue(order)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(order))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Listens to live orders belonging to the specified user.
     */
    public void listenToUserOrders(@NonNull String userId, @NonNull OrdersCallback callback) {
        detachUserListener();

        activeOrdersQuery = FirebaseManager.getInstance().getUserOrdersQuery(userId);
        ordersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Order> orders = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Order order = snap.getValue(Order.class);
                    if (order != null) {
                        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
                            order.setOrderId(snap.getKey());
                        }
                        orders.add(order);
                    }
                }
                Collections.sort(orders, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
                callback.onOrdersLoaded(orders);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        activeOrdersQuery.addValueEventListener(ordersListener);
    }

    /**
     * Listens to ALL orders across the canteen (for administrators).
     */
    public void listenToAllOrders(@NonNull OrdersCallback callback) {
        detachAllOrdersListener();

        allOrdersRef = FirebaseManager.getInstance().getOrdersRef();
        allOrdersListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Order> orders = new ArrayList<>();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Order order = snap.getValue(Order.class);
                    if (order != null) {
                        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
                            order.setOrderId(snap.getKey());
                        }
                        orders.add(order);
                    }
                }
                Collections.sort(orders, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
                callback.onOrdersLoaded(orders);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        };

        allOrdersRef.addValueEventListener(allOrdersListener);
    }

    /**
     * Updates an order's status (e.g. PLACED -> ACCEPTED -> PREPARING -> READY -> COLLECTED).
     */
    public void updateOrderStatus(@NonNull String orderId, @NonNull String newStatus, final ActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("orderStatus", newStatus);

        // When collected, if cash payment was pending, mark payment completed
        if (Order.STATUS_COLLECTED.equalsIgnoreCase(newStatus)) {
            updates.put("paymentStatus", Order.PAYMENT_STATUS_COMPLETED);
        }

        FirebaseManager.getInstance().getOrdersRef().child(orderId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e.getMessage());
                });
    }

    public void detachListener() {
        detachUserListener();
        detachAllOrdersListener();
    }

    private void detachUserListener() {
        if (ordersListener != null && activeOrdersQuery != null) {
            activeOrdersQuery.removeEventListener(ordersListener);
            ordersListener = null;
        }
    }

    private void detachAllOrdersListener() {
        if (allOrdersListener != null && allOrdersRef != null) {
            allOrdersRef.removeEventListener(allOrdersListener);
            allOrdersListener = null;
        }
    }
}
