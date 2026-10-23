package com.example.waitwise.models;

import java.io.Serializable;

public class OrderItem implements Serializable {
    private String foodId;
    private String name;
    private double unitPrice;
    private int quantity;
    private double lineTotal;

    public OrderItem() {
    }

    public OrderItem(String foodId, String name, double unitPrice, int quantity, double lineTotal) {
        this.foodId = foodId;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.lineTotal = lineTotal;
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(double lineTotal) {
        this.lineTotal = lineTotal;
    }
}
