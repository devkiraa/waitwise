package com.example.waitwise.models;

import java.io.Serializable;

public class CartItem implements Serializable {
    private String foodId;
    private int quantity;

    // Transient/convenience field for joined food item details during UI rendering
    private FoodItem foodItem;

    public CartItem() {
    }

    public CartItem(String foodId, int quantity) {
        this.foodId = foodId;
        this.quantity = quantity;
    }

    public CartItem(String foodId, int quantity, FoodItem foodItem) {
        this.foodId = foodId;
        this.quantity = quantity;
        this.foodItem = foodItem;
    }

    public String getFoodId() {
        return foodId;
    }

    public void setFoodId(String foodId) {
        this.foodId = foodId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public FoodItem getFoodItem() {
        return foodItem;
    }

    public void setFoodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
    }

    public double getLineTotal() {
        if (foodItem != null) {
            return foodItem.getPrice() * quantity;
        }
        return 0.0;
    }
}
