//A Service is the business logic layer.
//It sits between the Controller and the Repository. Think of it as a manager
// — the controller (receptionist) takes the request, passes it to the service (manager), who decides what to do and asks the repository (database clerk) for data
package com.example.groceryapi.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private static final Set<String> ALLOWED_SWEETENERS = Set.of(
            "CANE_SUGAR", "BROWN_SUGAR", "MAPLE_SYRUP", "JAGGERY", "HONEY");

    private static final Set<String> ALLOWED_FLOURS = Set.of(
            "FINGER_MILLET", "BAJRA_MILLET", "LITTLE_MILLET",
            "SORGHUM", "WHOLE_WHEAT", "ALL_PURPOSE");

    private final Repository repository;

    public userService(Repository repository) {
        this.repository = repository;
    }

    public List<Users> fetchUsers() {
        return repository.findAllUsers();
    }

    public Users register(String name, String email, String password, String userType) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        String normalizedType = userType == null ? "" : userType.trim().toLowerCase();
        if (!normalizedType.equals("customer") && !normalizedType.equals("employee")) {
            throw new IllegalArgumentException("userType must be 'customer' or 'employee'");
        }
        if (repository.findUserByEmail(email).isPresent()) {
            throw new IllegalStateException("email already registered: " + email);
        }
        Users user = new Users();
        user.setname(name);
        user.setemail(email);
        user.setpassword(password);
        user.setcreatedat(LocalDateTime.now());
        user = repository.saveUser(user);

        Role role = repository.findRoleByName(normalizedType).orElseGet(() -> {
            Role r = new Role();
            r.setRole(normalizedType);
            r.setFullName(normalizedType);
            return repository.saveRole(r);
        });

        UserRole ur = new UserRole();
        ur.setUser(user);
        ur.setRole(role);
        repository.saveUserRole(ur);

        return user;
    }

    public Users login(String email, String password) {
        if (email == null || password == null) {
            throw new IllegalArgumentException("email and password are required");
        }
        Users user = repository.findUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("invalid email or password"));
        if (!password.equals(user.getpassword())) {
            throw new IllegalArgumentException("invalid email or password");
        }
        return user;
    }

    public CartItem addToCart(int userid, long productId, int quantity,
                              String sweetenerType, Integer sweetenerPercent,
                              String flourType) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        Users user = repository.findUserById(userid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userid));
        Product product = repository.findProductById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        if (product.getStock() != null && product.getStock() < quantity) {
            throw new IllegalArgumentException("Only " + product.getStock() + " in stock");
        }

        String sweetener = normalizeCode(sweetenerType);
        String flour = normalizeCode(flourType);
        if (sweetener != null && !ALLOWED_SWEETENERS.contains(sweetener)) {
            throw new IllegalArgumentException("Invalid sweetener: " + sweetenerType);
        }
        if (flour != null && !ALLOWED_FLOURS.contains(flour)) {
            throw new IllegalArgumentException("Invalid flour: " + flourType);
        }
        if (sweetenerPercent != null && (sweetenerPercent < 0 || sweetenerPercent > 100)) {
            throw new IllegalArgumentException("sweetenerPercent must be between 0 and 100");
        }

        List<Cart> carts = repository.findCartsByUserId(userid);
        Cart cart;
        if (carts.isEmpty()) {
            cart = new Cart();
            cart.setUser(user);
            cart.setCreatedAt(LocalDateTime.now());
            cart = repository.saveCart(cart);
        } else {
            cart = carts.get(0);
        }

        boolean isCustom = sweetener != null || flour != null || sweetenerPercent != null;

        if (!isCustom) {
            List<CartItem> existing = repository.findCartItemsByUserId(userid);
            for (CartItem ci : existing) {
                if (ci.getProduct() != null
                        && ci.getProduct().getId().equals(productId)
                        && ci.getSweetenerType() == null
                        && ci.getFlourType() == null
                        && ci.getSweetenerPercent() == null) {
                    ci.setQuantity((ci.getQuantity() == null ? 0 : ci.getQuantity()) + quantity);
                    return repository.saveCartItem(ci);
                }
            }
        }

        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setQuantity(quantity);
        item.setSweetenerType(sweetener);
        item.setSweetenerPercent(sweetenerPercent);
        item.setFlourType(flour);
        return repository.saveCartItem(item);
    }

    private static String normalizeCode(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    public List<Map<String, Object>> fetchAllProducts() {
        List<Product> products = repository.findAllProducts();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Product p : products) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("description", p.getDescription());
            item.put("price", p.getPrice());
            item.put("stock", p.getStock());
            item.put("category", p.getCategory() == null ? null : Map.of(
                    "id", p.getCategory().getId(),
                    "name", p.getCategory().getName() == null ? "" : p.getCategory().getName()));
            List<ProductImage> images = repository.findImagesByProductId(p.getId());
            item.put("imageUrl", images.isEmpty() ? null : images.get(0).getImageUrl());
            result.add(item);
        }
        return result;
    }

    public List<String> fetchRoleNamesForUser(int userid) {
        return repository.findRolesByUserId(userid).stream()
                .map(Role::getRole)
                .filter(r -> r != null && !r.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
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
        requireEmployee(creator);
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        return saveItems(name, description, items, creator);
    }

    public List<Product> saveProducts(Map<String, Object> request) {
        Users creator = resolveUser((Integer) request.get("userId"));
        requireEmployee(creator);
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

    private void requireEmployee(Users user) {
        boolean isEmployee = repository.findRolesByUserId(user.getuserid()).stream()
                .anyMatch(r -> r.getRole() != null
                        && r.getRole().equalsIgnoreCase("employee"));
        if (!isEmployee) {
            throw new SecurityException("Only employees can add items to sell");
        }
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
