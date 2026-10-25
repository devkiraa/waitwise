package com.example.waitwise.models;

import java.io.Serializable;

public class InAppNotification implements Serializable {
    private String notificationId;
    private String orderId;
    private long tokenNumber;
    private String title;
    private String message;
    private String orderStatus;
    private long createdAt;
    private boolean read;

    public InAppNotification() {
    }

    public InAppNotification(String notificationId, String orderId, long tokenNumber,
                             String title, String message, String orderStatus,
                             long createdAt, boolean read) {
        this.notificationId = notificationId;
        this.orderId = orderId;
        this.tokenNumber = tokenNumber;
        this.title = title;
        this.message = message;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
        this.read = read;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public long getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(long tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public String getTitle() {
        return title != null ? title : "";
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOrderStatus() {
        return orderStatus != null ? orderStatus : "";
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}
