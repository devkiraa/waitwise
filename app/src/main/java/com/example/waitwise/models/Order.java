package com.example.waitwise.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Order implements Serializable {
    public static final String STATUS_PLACED = "PLACED";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_PREPARING = "PREPARING";
    public static final String STATUS_READY = "READY";
    public static final String STATUS_COLLECTED = "COLLECTED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String PAYMENT_CASH = "CASH_AT_PICKUP";
    public static final String PAYMENT_UPI_DEMO = "UPI (Demo)";
    public static final String PAYMENT_CARD_DEMO = "Card (Demo)";

    public static final String PAYMENT_STATUS_PENDING = "PENDING";
    public static final String PAYMENT_STATUS_COMPLETED = "COMPLETED";

    private String orderId;
    private String userId;
    private long tokenNumber;
    private Map<String, OrderItem> items = new HashMap<>();
    private double subtotal;
    private String paymentMethod;
    private String paymentStatus;
    private String orderStatus;
    private int estimatedPrepTimeMinutes;
    private long estimatedReadyAt;
    private long createdAt;

    public Order() {
    }

    public Order(String orderId, String userId, long tokenNumber, Map<String, OrderItem> items,
                 double subtotal, String paymentMethod, String paymentStatus,
                 String orderStatus, long createdAt) {
        this.orderId = orderId;
        this.userId = userId;
        this.tokenNumber = tokenNumber;
        this.items = items;
        this.subtotal = subtotal;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(long tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public Map<String, OrderItem> getItems() {
        return items != null ? items : new HashMap<>();
    }

    public void setItems(Map<String, OrderItem> items) {
        this.items = items;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public String getPaymentMethod() {
        return paymentMethod != null ? paymentMethod : PAYMENT_CASH;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus != null ? paymentStatus : PAYMENT_STATUS_PENDING;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getOrderStatus() {
        return orderStatus != null ? orderStatus : STATUS_PLACED;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public int getEstimatedPrepTimeMinutes() {
        return estimatedPrepTimeMinutes;
    }

    public void setEstimatedPrepTimeMinutes(int estimatedPrepTimeMinutes) {
        this.estimatedPrepTimeMinutes = estimatedPrepTimeMinutes;
    }

    public long getEstimatedReadyAt() {
        return estimatedReadyAt;
    }

    public void setEstimatedReadyAt(long estimatedReadyAt) {
        this.estimatedReadyAt = estimatedReadyAt;
    }

    public String getFormattedPrepTimeEstimate() {
        if (STATUS_READY.equalsIgnoreCase(orderStatus)
                || STATUS_COLLECTED.equalsIgnoreCase(orderStatus)
                || STATUS_CANCELLED.equalsIgnoreCase(orderStatus)) {
            return "";
        }

        if (estimatedPrepTimeMinutes <= 0 || estimatedReadyAt <= 0) {
            return "Est. Prep Time: Pending";
        }

        long diffMs = estimatedReadyAt - System.currentTimeMillis();
        if (diffMs > 0) {
            int remainingMins = (int) Math.ceil(diffMs / 60000.0);
            return "Est. Ready in ~" + remainingMins + " min" + (remainingMins == 1 ? "" : "s");
        } else {
            return "Est. Ready soon (Finishing up)";
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
