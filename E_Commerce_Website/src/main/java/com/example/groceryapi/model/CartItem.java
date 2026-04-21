package com.example.groceryapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "cart_items", schema = "public")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;

    @Column(name = "customization", columnDefinition = "text")
    private String customization;

    @Column(name = "sweetener_type")
    private String sweetenerType;

    @Column(name = "sweetener_percent")
    private Integer sweetenerPercent;

    @Column(name = "flour_type")
    private String flourType;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cart getCart() {
        return cart;
    }

    public void setCart(Cart cart) {
        this.cart = cart;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCustomization() {
        return customization;
    }

    public void setCustomization(String customization) {
        this.customization = customization;
    }

    public String getSweetenerType() {
        return sweetenerType;
    }

    public void setSweetenerType(String sweetenerType) {
        this.sweetenerType = sweetenerType;
    }

    public Integer getSweetenerPercent() {
        return sweetenerPercent;
    }

    public void setSweetenerPercent(Integer sweetenerPercent) {
        this.sweetenerPercent = sweetenerPercent;
    }

    public String getFlourType() {
        return flourType;
    }

    public void setFlourType(String flourType) {
        this.flourType = flourType;
    }
}
