package com.example.waitwise.repositories;

import androidx.annotation.NonNull;

import com.example.waitwise.models.Order;
import com.example.waitwise.models.OrderItem;
import com.example.waitwise.utils.FirebaseManager;
import com.example.waitwise.utils.PriceUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
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
        if (order.getUserId() == null || order.getUserId().isEmpty()) {
            callback.onError("User ID is required to place an order.");
            return;
        }

        if (order.getItems() == null || order.getItems().isEmpty()) {
            callback.onError("Order cart is empty.");
            return;
        }

        // Verify subtotal integrity from item line totals
        double verifiedSubtotal = 0.0;
        for (OrderItem item : order.getItems().values()) {
            if (item != null) {
                double lineTotal = PriceUtils.calculateLineTotal(item.getUnitPrice(), item.getQuantity());
                item.setLineTotal(lineTotal);
                verifiedSubtotal = PriceUtils.add(verifiedSubtotal, lineTotal);
            }
        }
        order.setSubtotal(verifiedSubtotal);

        // Generate random unique 4-digit token number (1000 - 9999)
        long randomToken = 1000L + (long) (Math.random() * 9000L);

        DatabaseReference ordersRef = FirebaseManager.getInstance().getOrdersRef();
        String orderId = ordersRef.push().getKey();
        if (orderId == null) {
            callback.onError("Could not generate order identifier.");
            return;
        }

        order.setOrderId(orderId);
        order.setTokenNumber(randomToken);
        if (order.getCreatedAt() <= 0) {
            order.setCreatedAt(System.currentTimeMillis());
        }

        ordersRef.child(orderId).setValue(order)
                .addOnSuccessListener(aVoid -> callback.onSuccess(order))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
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

    public void updateEstimatedPrepTime(@NonNull String orderId, int prepTimeMinutes, final ActionCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("estimatedPrepTimeMinutes", prepTimeMinutes);
        if (prepTimeMinutes > 0) {
            updates.put("estimatedReadyAt", System.currentTimeMillis() + (prepTimeMinutes * 60 * 1000L));
        } else {
            updates.put("estimatedReadyAt", 0);
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
