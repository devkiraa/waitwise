package com.example.waitwise.models;

import java.io.Serializable;

public class FoodItem implements Serializable {
    private String id;
    private String name;
    private String description;
    private double price;
    private String category;
    private String imageUrl;
    private boolean available = true;
    private long createdAt;

    // Default constructor for Firebase deserialization
    public FoodItem() {
    }

    public FoodItem(String id, String name, String description, double price, String category, String imageUrl, boolean available) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.available = available;
        this.createdAt = System.currentTimeMillis();
    }

    public FoodItem(String id, String name, String description, double price, String category, String imageUrl, boolean available, long createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.imageUrl = imageUrl;
        this.available = available;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category != null ? category : "Breakfast";
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl != null ? imageUrl : "";
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
