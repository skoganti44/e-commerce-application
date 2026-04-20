package com.example.groceryapi.dto;

import java.util.List;

public class ProductsRequest {

    private Integer userId;
    private List<ProductEntry> products;

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public List<ProductEntry> getProducts() { return products; }
    public void setProducts(List<ProductEntry> products) { this.products = products; }

    public static class ProductEntry {
        private String name;
        private String description;
        private List<ProductRequest.Item> items;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<ProductRequest.Item> getItems() { return items; }
        public void setItems(List<ProductRequest.Item> items) { this.items = items; }
    }
}
