//A Service is the business logic layer.
//It sits between the Controller and the Repository. Think of it as a manager
// — the controller (receptionist) takes the request, passes it to the service (manager), who decides what to do and asks the repository (database clerk) for data
package com.example.groceryapi.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.groceryapi.model.Cart;
import com.example.groceryapi.model.CartItem;
import com.example.groceryapi.model.Category;
import com.example.groceryapi.model.OrderItem;
import com.example.groceryapi.model.Orders;
import com.example.groceryapi.model.Payment;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.ProductAvailable;
import com.example.groceryapi.model.ProductImage;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.model.Users;
import com.example.groceryapi.repository.Repository;

@Service
public class userService {

    private final Repository repository;

    public userService(Repository repository) {
        this.repository = repository;
    }

    public List<Users> fetchUsers() {
        return repository.findAllUsers();
    }

    public List<Role> fetchRoles(String department) {
        if (department == null || department.isBlank()) {
            return repository.findAllRoles();
        }
        return repository.findRolesByDepartment(department);
    }

    public Map<String, Object> fetchCartByUserId(int userid) {
        List<Cart> carts = repository.findCartsByUserId(userid);
        if (carts.isEmpty()) {
            throw new IllegalArgumentException("No cart found for userId: " + userid);
        }
        List<CartItem> items = repository.findCartItemsByUserId(userid);
        return Map.of(
                "cart", carts,
                "items", items);
    }

    public List<Payment> fetchPaymentsByUserId(int userid, boolean includeAll) {
        repository.findUserById(userid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userid));
        List<Payment> payments = repository.findPaymentsByUserId(userid);
        if (includeAll) {
            return payments;
        }
        return filterOutFailedWhenSuccessExists(payments);
    }

    private List<Payment> filterOutFailedWhenSuccessExists(List<Payment> payments) {
        java.util.Map<Long, List<Payment>> byOrder = new java.util.LinkedHashMap<>();
        for (Payment p : payments) {
            byOrder.computeIfAbsent(p.getOrder().getId(), k -> new ArrayList<>()).add(p);
        }
        List<Payment> result = new ArrayList<>();
        for (List<Payment> group : byOrder.values()) {
            boolean hasSuccess = group.stream()
                    .anyMatch(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()));
            if (hasSuccess) {
                for (Payment p : group) {
                    if ("SUCCESS".equalsIgnoreCase(p.getStatus())) {
                        result.add(p);
                    }
                }
            } else {
                result.addAll(group);
            }
        }
        return result;
    }

    public Map<String, Object> fetchOrdersForCustomer(int userid) {
        repository.findUserById(userid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userid));

        List<Role> roles = repository.findRolesByUserId(userid);
        boolean isCustomer = roles.stream()
                .anyMatch(r -> r.getRole() != null && r.getRole().equalsIgnoreCase("customer"));
        if (!isCustomer) {
            throw new SecurityException("User " + userid + " is not a customer");
        }

        List<Orders> orders = repository.findOrdersByUserId(userid);
        List<OrderItem> items = repository.findOrderItemsByUserId(userid);
        return Map.of(
                "orders", orders,
                "items", items);
    }

    public List<UserRole> fetchUserRoles(Integer userid, Integer roleid) {
        if (userid != null) {
            return repository.findUserRolesByUserId(userid);
        }
        if (roleid != null) {
            return repository.findUserRolesByRoleId(roleid);
        }
        return repository.findAllUserRoles();
    }

    public List<Product> saveProduct(Map<String, Object> request) {
        Users creator = resolveUser((Integer) request.get("userId"));
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        return saveItems(name, description, items, creator);
    }

    public List<Product> saveProducts(Map<String, Object> request) {
        Users creator = resolveUser((Integer) request.get("userId"));
        List<Map<String, Object>> products = (List<Map<String, Object>>) request.get("products");
        List<Product> saved = new ArrayList<>();
        for (Map<String, Object> p : products) {
            String name = (String) p.get("name");
            String description = (String) p.get("description");
            List<Map<String, Object>> items = (List<Map<String, Object>>) p.get("items");
            saved.addAll(saveItems(name, description, items, creator));
        }
        return saved;
    }

    private Users resolveUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return repository.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    public Map<String, Object> cleanupForUser(int userid) {
        repository.findUserById(userid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userid));

        List<OrderItem> items = repository.findOrderItemsByUserId(userid);
        List<Payment> payments = repository.findPaymentsByUserId(userid);

        Set<Long> successOrderIds = payments.stream()
                .filter(p -> "SUCCESS".equalsIgnoreCase(p.getStatus()))
                .map(p -> p.getOrder().getId())
                .collect(Collectors.toSet());

        Set<Long> archivedProductIds = new HashSet<>();
        int archived = 0;
        for (OrderItem oi : items) {
            Long orderId = oi.getOrder().getId();
            if (successOrderIds.contains(orderId)) {
                continue;
            }
            Product p = oi.getProduct();
            if (!archivedProductIds.add(p.getId())) {
                continue;
            }

            ProductAvailable pa = new ProductAvailable();
            pa.setName(p.getName());
            pa.setDescription(p.getDescription());
            pa.setPrice(p.getPrice());
            pa.setStock(p.getStock());
            pa.setCategory(p.getCategory());
            pa.setCreatedBy(p.getCreatedBy());

            List<ProductImage> images = repository.findImagesByProductId(p.getId());
            if (!images.isEmpty()) {
                pa.setImageUrl(images.get(0).getImageUrl());
            }

            repository.saveProductAvailable(pa);
            archived++;
        }

        int paymentsDeleted = repository.deletePaymentsByUserId(userid);
        int orderItemsDeleted = repository.deleteOrderItemsByUserId(userid);
        int ordersDeleted = repository.deleteOrdersByUserId(userid);

        return Map.of(
                "userId", userid,
                "archived", archived,
                "paymentsDeleted", paymentsDeleted,
                "orderItemsDeleted", orderItemsDeleted,
                "ordersDeleted", ordersDeleted);
    }

    private List<Product> saveItems(String name, String description,
                                    List<Map<String, Object>> items, Users creator) {
        List<Product> saved = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> cat = (Map<String, Object>) item.get("category");

            Category category = new Category();
            category.setName((String) cat.get("categoryName"));
            category.setDescription((String) cat.get("type"));
            category = repository.saveCategory(category);

            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(new BigDecimal(item.get("price").toString()));
            product.setStock(((Number) item.get("stock")).intValue());
            product.setCategory(category);
            product.setCreatedBy(creator);
            product = repository.saveProduct(product);

            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl((String) item.get("imageUrl"));
            repository.saveProductImage(image);

            saved.add(product);
        }
        return saved;
    }
}
