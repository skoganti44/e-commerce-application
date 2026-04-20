package com.example.groceryapi.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductRequest {

    private Integer userId;
    private String name;
    private String description;
    private List<Item> items;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public static class Item {
        private Category category;
        private BigDecimal price;
        private Integer stock;
        private String imageUrl;

        public Category getCategory() { return category; }
        public void setCategory(Category category) { this.category = category; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public Integer getStock() { return stock; }
        public void setStock(Integer stock) { this.stock = stock; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    public static class Category {
        private String categoryName;
        private String type;

        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
