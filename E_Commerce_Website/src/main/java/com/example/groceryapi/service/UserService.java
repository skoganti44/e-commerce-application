//A Service is the business logic layer.
//It sits between the Controller and the Repository. Think of it as a manager
// — the controller (receptionist) takes the request, passes it to the service (manager), who decides what to do and asks the repository (database clerk) for data
package com.example.groceryapi.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.groceryapi.model.Cart;
import com.example.groceryapi.model.CartItem;
import com.example.groceryapi.model.Category;
import com.example.groceryapi.model.DailyStock;
import com.example.groceryapi.model.DeliveryIssue;
import com.example.groceryapi.model.DeliveryTrip;
import com.example.groceryapi.model.DiscountCampaign;
import com.example.groceryapi.model.OrderItem;
import com.example.groceryapi.model.Orders;
import com.example.groceryapi.model.Payment;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.ProductAvailable;
import com.example.groceryapi.model.ProductImage;
import com.example.groceryapi.model.RefundRequest;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.ShippingAddress;
import com.example.groceryapi.model.Supply;
import com.example.groceryapi.model.Task;
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.model.User;
import com.example.groceryapi.repository.Repository;

@Service
public class UserService {

    private static final Set<String> ALLOWED_SWEETENERS = Set.of(
            "CANE_SUGAR", "BROWN_SUGAR", "MAPLE_SYRUP", "JAGGERY", "HONEY");

    private static final Set<String> ALLOWED_FLOURS = Set.of(
            "FINGER_MILLET", "BAJRA_MILLET", "LITTLE_MILLET",
            "SORGHUM", "WHOLE_WHEAT", "ALL_PURPOSE");

    private static final Map<String, BigDecimal> SWEETENER_ADDON = Map.of(
            "CANE_SUGAR", new BigDecimal("1"),
            "BROWN_SUGAR", new BigDecimal("1"),
            "JAGGERY", new BigDecimal("2"),
            "MAPLE_SYRUP", new BigDecimal("3"),
            "HONEY", new BigDecimal("3"));

    private static final Map<String, BigDecimal> FLOUR_ADDON = Map.of(
            "ALL_PURPOSE", new BigDecimal("1"),
            "WHOLE_WHEAT", new BigDecimal("2"),
            "FINGER_MILLET", new BigDecimal("5"),
            "BAJRA_MILLET", new BigDecimal("5"),
            "LITTLE_MILLET", new BigDecimal("5"),
            "SORGHUM", new BigDecimal("5"));

    private static BigDecimal addon(Map<String, BigDecimal> table, String code) {
        if (code == null) return BigDecimal.ZERO;
        return table.getOrDefault(code, BigDecimal.ZERO);
    }

    public static BigDecimal computeUnitPrice(Product product, String sweetenerType, String flourType) {
        BigDecimal base = product.getPrice() == null ? BigDecimal.ZERO : product.getPrice();
        return base.add(addon(SWEETENER_ADDON, sweetenerType))
                .add(addon(FLOUR_ADDON, flourType));
    }

    private final Repository repository;

    public UserService(Repository repository) {
        this.repository = repository;
    }

    public List<User> fetchUsers() {
        return repository.findAllUsers();
    }

    private static final Set<String> ALLOWED_DEPARTMENTS = Set.of(
            "bakery", "sales", "kitchen", "delivery", "management");

    private static final Set<String> ALLOWED_TASK_DEPARTMENTS = Set.of(
            "bakery", "kitchen", "delivery", "management", "sales");

    private static final Set<String> ALLOWED_TASK_PRIORITIES = Set.of(
            "low", "normal", "high", "urgent");

    private static final Set<String> ALLOWED_TASK_STATUSES = Set.of(
            "open", "in_progress", "done", "cancelled");

    private static final Set<String> TERMINAL_TASK_STATUSES = Set.of(
            "done", "cancelled");

    private static final Set<String> ALLOWED_TRIP_STATUSES = Set.of(
            "picked_up", "out_for_delivery", "delivered", "failed");

    private static final Set<String> TERMINAL_TRIP_STATUSES = Set.of(
            "delivered", "failed");

    private static final Set<String> ALLOWED_FAILURE_REASONS = Set.of(
            "customer_not_home", "refused", "damaged", "wrong_address", "other");

    private static final Set<String> ALLOWED_ISSUE_TYPES = Set.of(
            "vehicle_breakdown", "traffic_delay", "accident", "other");

    public User register(String name, String email, String password,
                          String userType, String department) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("password is required");
        }
        String rawType = userType == null ? "" : userType.trim().toLowerCase();
        String normalizedType = rawType.equals("customer") ? "customer" : "employee";

        String normalizedDept = null;
        if (normalizedType.equals("employee")) {
            if (department == null || department.isBlank()) {
                throw new IllegalArgumentException("department is required for employees");
            }
            normalizedDept = department.trim().toLowerCase();
            if (!ALLOWED_DEPARTMENTS.contains(normalizedDept)) {
                throw new IllegalArgumentException(
                        "Invalid department. Allowed: " + ALLOWED_DEPARTMENTS);
            }
        }

        if (repository.findUserByEmail(email).isPresent()) {
            throw new IllegalStateException("email already registered: " + email);
        }
        User user = new User();
        user.setname(name);
        user.setemail(email);
        user.setpassword(password);
        user.setcreatedat(LocalDateTime.now());
        user = repository.saveUser(user);

        final String deptForLookup = normalizedDept;
        Role role;
        if (normalizedType.equals("customer")) {
            role = repository.findRoleByName("customer").orElseGet(() -> {
                Role r = new Role();
                r.setRole("customer");
                r.setFullName("Customer");
                return repository.saveRole(r);
            });
        } else {
            role = repository.findRoleByRoleAndDepartment("employee", deptForLookup)
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setRole("employee");
                        r.setDepartment(deptForLookup);
                        r.setFullName("Employee - " + capitalize(deptForLookup));
                        return repository.saveRole(r);
                    });
        }

        UserRole ur = new UserRole();
        ur.setUser(user);
        ur.setRole(role);
        repository.saveUserRole(ur);

        return user;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    public User login(String email, String password) {
        if (email == null || password == null) {
            throw new IllegalArgumentException("email and password are required");
        }
        User user = repository.findUserByEmail(email)
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
        User user = repository.findUserById(userid)
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

    public void updateCartItemQuantity(int userid, Long cartItemId, int quantity) {
        CartItem item = resolveOwnedCartItem(userid, cartItemId);
        if (quantity <= 0) {
            repository.deleteCartItem(item);
            return;
        }
        Product product = item.getProduct();
        if (product != null && product.getStock() != null && product.getStock() < quantity) {
            throw new IllegalArgumentException("Only " + product.getStock() + " in stock");
        }
        item.setQuantity(quantity);
        repository.saveCartItem(item);
    }

    public void removeCartItem(int userid, Long cartItemId) {
        CartItem item = resolveOwnedCartItem(userid, cartItemId);
        repository.deleteCartItem(item);
    }

    private CartItem resolveOwnedCartItem(int userid, Long cartItemId) {
        if (cartItemId == null) {
            throw new IllegalArgumentException("cartItemId is required");
        }
        CartItem item = repository.findCartItemById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found: " + cartItemId));
        Cart itemCart = item.getCart();
        User owner = itemCart == null ? null : itemCart.getUser();
        if (owner == null || owner.getuserid() != userid) {
            throw new IllegalArgumentException("Cart item does not belong to user " + userid);
        }
        return item;
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
            item.put("supportedFlours", splitCodes(p.getSupportedFlours()));
            item.put("supportedSweeteners", splitCodes(p.getSupportedSweeteners()));
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

    public List<String> fetchDepartmentsForUser(int userid) {
        return repository.findRolesByUserId(userid).stream()
                .map(Role::getDepartment)
                .filter(d -> d != null && !d.isBlank())
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

        Map<Long, Map<String, Object>> itemTotals = new java.util.LinkedHashMap<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int totalQuantity = 0;
        for (CartItem ci : items) {
            BigDecimal base = ci.getProduct() == null || ci.getProduct().getPrice() == null
                    ? BigDecimal.ZERO
                    : ci.getProduct().getPrice();
            BigDecimal sAdd = addon(SWEETENER_ADDON, ci.getSweetenerType());
            BigDecimal fAdd = addon(FLOUR_ADDON, ci.getFlourType());
            BigDecimal unit = base.add(sAdd).add(fAdd);
            int qty = ci.getQuantity() == null ? 0 : ci.getQuantity();
            BigDecimal line = unit.multiply(BigDecimal.valueOf(qty));

            Map<String, Object> per = new java.util.LinkedHashMap<>();
            per.put("basePrice", base);
            per.put("sweetenerAddon", sAdd);
            per.put("flourAddon", fAdd);
            per.put("unitPrice", unit);
            per.put("lineTotal", line);
            itemTotals.put(ci.getId(), per);

            subtotal = subtotal.add(line);
            totalQuantity += qty;
        }

        Map<String, Object> totals = new java.util.LinkedHashMap<>();
        totals.put("subtotal", subtotal);
        totals.put("itemCount", items.size());
        totals.put("totalQuantity", totalQuantity);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("cart", carts);
        result.put("items", items);
        result.put("itemTotals", itemTotals);
        result.put("totals", totals);
        return result;
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
        User creator = resolveUser((Integer) request.get("userId"));
        requireEmployee(creator);
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        return saveItems(name, description, items, creator);
    }

    public List<Product> saveProducts(Map<String, Object> request) {
        User creator = resolveUser((Integer) request.get("userId"));
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

    private User resolveUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return repository.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private void requireEmployee(User user) {
        List<String> roles = repository.findRolesByUserId(user.getuserid()).stream()
                .map(Role::getRole)
                .filter(r -> r != null && !r.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
        boolean hasNonCustomerRole = roles.stream().anyMatch(r -> !r.equals("customer"));
        if (!hasNonCustomerRole) {
            throw new SecurityException("Only employees can add items to sell");
        }
    }

    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of(
            "DEBIT_CARD", "CREDIT_CARD", "GIFT_CARD", "COD");

    private static final Set<String> ALLOWED_COUNTER_PAYMENT_METHODS = Set.of(
            "CASH", "DEBIT_CARD", "CREDIT_CARD", "UPI");

    public ShippingAddress saveShippingAddress(int userid, Map<String, Object> body) {
        User user = repository.findUserById(userid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userid));
        ShippingAddress address = buildAddress(body);
        validateAddress(address);
        address.setUser(user);
        return repository.saveShippingAddress(address);
    }

    public ShippingAddress findLatestShippingAddress(int userid) {
        repository.findUserById(userid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userid));
        return repository.findLatestShippingAddressByUserId(userid).orElse(null);
    }

    public Map<String, Object> checkout(int userid, String paymentMethodRaw,
                                        Map<String, Object> addressBody,
                                        String cardLast4) {
        return checkout(userid, paymentMethodRaw, addressBody, cardLast4, null);
    }

    public Map<String, Object> checkout(int userid, String paymentMethodRaw,
                                        Map<String, Object> addressBody,
                                        String cardLast4,
                                        String customerNotes) {
        User user = repository.findUserById(userid)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userid));

        String paymentMethod = normalizeCode(paymentMethodRaw);
        if (paymentMethod == null || !ALLOWED_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method: " + paymentMethodRaw);
        }

        ShippingAddress address = buildAddress(addressBody);
        validateAddress(address);
        address.setUser(user);

        List<CartItem> cartItems = repository.findCartItemsByUserId(userid);
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty for user: " + userid);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem ci : cartItems) {
            BigDecimal unit = computeUnitPrice(ci.getProduct(), ci.getSweetenerType(), ci.getFlourType());
            int qty = ci.getQuantity() == null ? 0 : ci.getQuantity();
            totalAmount = totalAmount.add(unit.multiply(BigDecimal.valueOf(qty)));
        }

        Orders order = new Orders();
        order.setUser(user);
        order.setTotalAmount(totalAmount);
        order.setStatus("COD".equals(paymentMethod) ? "PENDING" : "CONFIRMED");
        order.setChannel("online");
        order.setKitchenStatus("pending");
        if (customerNotes != null && !customerNotes.isBlank()) {
            order.setCustomerNotes(customerNotes.trim());
        }
        order = repository.saveOrder(order);

        for (CartItem ci : cartItems) {
            BigDecimal unit = computeUnitPrice(ci.getProduct(), ci.getSweetenerType(), ci.getFlourType());
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(ci.getProduct());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(unit);
            oi.setCustomization(ci.getCustomization());
            oi.setSweetenerType(ci.getSweetenerType());
            oi.setSweetenerPercent(ci.getSweetenerPercent());
            oi.setFlourType(ci.getFlourType());
            repository.saveOrderItem(oi);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(totalAmount);
        String methodLabel = paymentMethod;
        if ((paymentMethod.equals("DEBIT_CARD") || paymentMethod.equals("CREDIT_CARD"))
                && cardLast4 != null && !cardLast4.isBlank()) {
            methodLabel = paymentMethod + " ****" + cardLast4;
        }
        payment.setPaymentMethod(methodLabel);
        payment.setStatus("COD".equals(paymentMethod) ? "PENDING" : "SUCCESS");
        payment = repository.savePayment(payment);

        address.setOrder(order);
        ShippingAddress savedAddress = repository.saveShippingAddress(address);

        repository.deleteCartItemsByUserId(userid);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("orderId", order.getId());
        result.put("paymentId", payment.getId());
        result.put("addressId", savedAddress.getId());
        result.put("totalAmount", totalAmount);
        result.put("status", order.getStatus());
        result.put("paymentStatus", payment.getStatus());
        result.put("paymentMethod", payment.getPaymentMethod());
        return result;
    }

    public Map<String, Object> recordCounterSale(List<Map<String, Object>> rawItems,
                                                 String paymentMethodRaw,
                                                 BigDecimal cashGiven,
                                                 String customerName,
                                                 String customerNotes) {
        if (rawItems == null || rawItems.isEmpty()) {
            throw new IllegalArgumentException("At least one item is required");
        }
        String paymentMethod = normalizeCode(paymentMethodRaw);
        if (paymentMethod == null || !ALLOWED_COUNTER_PAYMENT_METHODS.contains(paymentMethod)) {
            throw new IllegalArgumentException("Invalid payment method: " + paymentMethodRaw);
        }

        Orders order = new Orders();
        order.setUser(null);
        order.setChannel("instore");
        order.setKitchenStatus("pending");
        order.setStatus("CONFIRMED");

        StringBuilder notes = new StringBuilder();
        if (customerName != null && !customerName.isBlank()) {
            notes.append("Customer: ").append(customerName.trim());
        }
        if (customerNotes != null && !customerNotes.isBlank()) {
            if (notes.length() > 0) notes.append(" — ");
            notes.append(customerNotes.trim());
        }
        if (notes.length() > 0) {
            order.setCustomerNotes(notes.toString());
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new java.util.ArrayList<>();
        for (Map<String, Object> raw : rawItems) {
            Object pid = raw.get("productId");
            Object qty = raw.get("quantity");
            if (pid == null || qty == null) {
                throw new IllegalArgumentException("Each item needs productId and quantity");
            }
            long productId = ((Number) pid).longValue();
            int quantity = ((Number) qty).intValue();
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for product " + productId);
            }
            Product product = repository.findProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
            String sweetenerType = trimToNull(raw.get("sweetenerType"));
            String flourType = trimToNull(raw.get("flourType"));
            BigDecimal unit = computeUnitPrice(product, sweetenerType, flourType);
            totalAmount = totalAmount.add(unit.multiply(BigDecimal.valueOf(quantity)));

            OrderItem oi = new OrderItem();
            oi.setProduct(product);
            oi.setQuantity(quantity);
            oi.setPrice(unit);
            oi.setCustomization(trimToNull(raw.get("customization")));
            oi.setSweetenerType(sweetenerType);
            Object pct = raw.get("sweetenerPercent");
            if (pct instanceof Number) oi.setSweetenerPercent(((Number) pct).intValue());
            oi.setFlourType(flourType);
            orderItems.add(oi);
        }

        BigDecimal changeDue = BigDecimal.ZERO;
        if ("CASH".equals(paymentMethod)) {
            if (cashGiven == null || cashGiven.compareTo(totalAmount) < 0) {
                throw new IllegalArgumentException(
                        "Cash given must be at least the total: " + totalAmount);
            }
            changeDue = cashGiven.subtract(totalAmount);
        }

        order.setTotalAmount(totalAmount);
        order = repository.saveOrder(order);
        for (OrderItem oi : orderItems) {
            oi.setOrder(order);
            repository.saveOrderItem(oi);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(totalAmount);
        payment.setPaymentMethod(paymentMethod);
        payment.setStatus("SUCCESS");
        payment = repository.savePayment(payment);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("orderId", order.getId());
        result.put("paymentId", payment.getId());
        result.put("totalAmount", totalAmount);
        result.put("status", order.getStatus());
        result.put("paymentMethod", payment.getPaymentMethod());
        result.put("paymentStatus", payment.getStatus());
        result.put("cashGiven", cashGiven);
        result.put("changeDue", changeDue);
        result.put("channel", "instore");
        return result;
    }

    private ShippingAddress buildAddress(Map<String, Object> body) {
        if (body == null) {
            throw new IllegalArgumentException("Address is required");
        }
        ShippingAddress a = new ShippingAddress();
        a.setFullName(trimToNull(body.get("fullName")));
        a.setPhone(trimToNull(body.get("phone")));
        a.setLine1(trimToNull(body.get("line1")));
        a.setLine2(trimToNull(body.get("line2")));
        a.setLandmark(trimToNull(body.get("landmark")));
        a.setCity(trimToNull(body.get("city")));
        a.setState(trimToNull(body.get("state")));
        a.setPincode(trimToNull(body.get("pincode")));
        a.setCountry(trimToNull(body.get("country")));
        a.setInstructions(trimToNull(body.get("instructions")));
        String type = trimToNull(body.get("addressType"));
        a.setAddressType(type == null ? "HOME" : type.toUpperCase());
        return a;
    }

    private static String trimToNull(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private void validateAddress(ShippingAddress a) {
        if (a.getFullName() == null) throw new IllegalArgumentException("fullName is required");
        if (a.getPhone() == null || !a.getPhone().matches("\\d{10}"))
            throw new IllegalArgumentException("phone must be 10 digits");
        if (a.getLine1() == null) throw new IllegalArgumentException("line1 is required");
        if (a.getCity() == null) throw new IllegalArgumentException("city is required");
        if (a.getState() == null) throw new IllegalArgumentException("state is required");
        if (a.getPincode() == null || !a.getPincode().matches("\\d{5}(-\\d{4})?"))
            throw new IllegalArgumentException("ZIP must be 5 digits or ZIP+4 (e.g. 12345 or 12345-6789)");
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
                                    List<Map<String, Object>> items, User creator) {
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
            product.setSupportedFlours(
                    normalizeCodeList(item.get("supportedFlours"), ALLOWED_FLOURS, "flour"));
            product.setSupportedSweeteners(
                    normalizeCodeList(item.get("supportedSweeteners"), ALLOWED_SWEETENERS, "sweetener"));
            product = repository.saveProduct(product);

            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl((String) item.get("imageUrl"));
            repository.saveProductImage(image);

            saved.add(product);
        }
        return saved;
    }

    private static List<String> splitCodes(String csv) {
        if (csv == null || csv.isBlank()) return new ArrayList<>();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String normalizeCodeList(Object raw, Set<String> allowed, String label) {
        if (raw == null) return null;
        List<String> values;
        if (raw instanceof List) {
            values = ((List<?>) raw).stream()
                    .map(v -> v == null ? null : v.toString())
                    .collect(Collectors.toList());
        } else {
            values = Arrays.asList(raw.toString().split(","));
        }
        List<String> cleaned = values.stream()
                .map(UserService::normalizeCode)
                .filter(v -> v != null && !v.isBlank())
                .distinct()
                .collect(Collectors.toList());
        for (String code : cleaned) {
            if (!allowed.contains(code)) {
                throw new IllegalArgumentException("Invalid " + label + ": " + code);
            }
        }
        return cleaned.isEmpty() ? null : String.join(",", cleaned);
    }

    private static final List<String> KITCHEN_ALLOWED_STATUSES =
            List.of("pending", "baking", "ready", "done", "cancelled");
    private static final List<String> KITCHEN_VISIBLE_STATUSES =
            List.of("pending", "baking", "ready");

    public List<Map<String, Object>> fetchKitchenOrders(String channel) {
        return fetchKitchenOrders(channel, KITCHEN_VISIBLE_STATUSES);
    }

    public List<Map<String, Object>> fetchKitchenOrders(String channel, List<String> statuses) {
        String normalizedChannel = channel == null ? "online" : channel.toLowerCase();
        List<String> statusFilter = (statuses == null || statuses.isEmpty())
                ? KITCHEN_VISIBLE_STATUSES
                : statuses.stream().map(String::toLowerCase).collect(Collectors.toList());
        List<Orders> orders = repository.findOrdersByChannelAndKitchenStatuses(
                normalizedChannel, statusFilter);
        if (orders.isEmpty()) {
            return List.of();
        }
        List<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toList());
        List<OrderItem> items = repository.findOrderItemsByOrderIds(orderIds);
        Map<Long, List<OrderItem>> itemsByOrder = items.stream()
                .collect(Collectors.groupingBy(oi -> oi.getOrder().getId()));
        return orders.stream()
                .map(o -> toKitchenOrderMap(o, itemsByOrder.getOrDefault(o.getId(), List.of())))
                .collect(Collectors.toList());
    }

    private Map<String, Object> toKitchenOrderMap(Orders o, List<OrderItem> items) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", o.getId());
        m.put("customerName", o.getUser() == null ? "Walk-in" : o.getUser().getname());
        m.put("channel", o.getChannel());
        m.put("kitchenStatus",
                o.getKitchenStatus() == null ? "pending" : o.getKitchenStatus());
        m.put("status", o.getStatus());
        m.put("totalAmount", o.getTotalAmount());
        m.put("customerNotes", o.getCustomerNotes());
        m.put("kitchenNotes", o.getKitchenNotes());
        m.put("createdAt", o.getCreatedAt() == null ? null : o.getCreatedAt().toString());
        List<Map<String, Object>> itemMaps = items.stream().map(oi -> {
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("itemId", oi.getId());
            im.put("productId", oi.getProduct() == null ? null : oi.getProduct().getId());
            im.put("productName", oi.getProduct() == null ? "Unknown" : oi.getProduct().getName());
            im.put("quantity", oi.getQuantity());
            im.put("price", oi.getPrice());
            im.put("customization", oi.getCustomization());
            im.put("sweetenerType", oi.getSweetenerType());
            im.put("sweetenerPercent", oi.getSweetenerPercent());
            im.put("flourType", oi.getFlourType());
            return im;
        }).collect(Collectors.toList());
        m.put("items", itemMaps);
        return m;
    }

    public Map<String, Object> updateKitchenOrderStatus(long orderId, String newStatus) {
        return updateKitchenOrderStatus(orderId, newStatus, null);
    }

    public Map<String, Object> updateKitchenOrderStatus(long orderId, String newStatus, String reason) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        String normalized = newStatus.trim().toLowerCase();
        if (!KITCHEN_ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid kitchen status: " + newStatus
                    + ". Allowed: " + KITCHEN_ALLOWED_STATUSES);
        }
        Orders order = repository.findOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Order not found: " + orderId));
        order.setKitchenStatus(normalized);
        if (reason != null && !reason.isBlank()) {
            order.setKitchenNotes(reason.trim());
        }
        Orders saved = repository.saveOrder(order);
        List<OrderItem> items = repository.findOrderItemsByOrderIds(List.of(saved.getId()));
        return toKitchenOrderMap(saved, items);
    }

    public Map<String, Object> adjustDailyStockPrepared(long stockId, int delta) {
        DailyStock ds = repository.findDailyStockById(stockId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Daily stock row not found: " + stockId));
        int current = ds.getPreparedCount() == null ? 0 : ds.getPreparedCount();
        int next = Math.max(0, current + delta);
        ds.setPreparedCount(next);
        DailyStock saved = repository.saveDailyStock(ds);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", saved.getId());
        m.put("productId", saved.getProduct() == null ? null : saved.getProduct().getId());
        m.put("productName",
                saved.getProduct() == null ? "Unknown" : saved.getProduct().getName());
        m.put("targetCount", saved.getTargetCount());
        m.put("preparedCount", saved.getPreparedCount());
        int target = saved.getTargetCount() == null ? 0 : saved.getTargetCount();
        m.put("remaining", Math.max(target - next, 0));
        m.put("stockDate", saved.getStockDate() == null ? null : saved.getStockDate().toString());
        return m;
    }

    private static final Set<String> ALLOWED_SUPPLY_CATEGORIES = Set.of(
            "flour", "sweetener", "dairy", "egg", "nut_seed",
            "flavour", "leavening", "packaging", "cleaning", "other");

    private static final Set<String> ALLOWED_SUPPLY_UNITS = Set.of(
            "kg", "g", "l", "ml", "pcs", "box", "pack");

    private static final Set<String> ALLOWED_SUPPLY_ORDER_STATUSES = Set.of(
            "received", "waiting", "urgency");

    public List<Map<String, Object>> fetchSupplies() {
        seedSuppliesIfEmpty();
        migrateLegacySupplyStock();
        return repository.findAllSupplies().stream()
                .filter(s -> {
                    String st = s.getOrderStatus() == null
                            ? "received" : s.getOrderStatus().toLowerCase();
                    return !"received".equals(st);
                })
                .map(this::toSupplyMap)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> fetchInStockSupplies() {
        seedSuppliesIfEmpty();
        migrateLegacySupplyStock();
        return repository.findAllSupplies().stream()
                .filter(s -> {
                    BigDecimal in = s.getInStock() == null
                            ? BigDecimal.ZERO : s.getInStock();
                    return in.signum() > 0;
                })
                .map(this::toSupplyMap)
                .collect(Collectors.toList());
    }

    private void migrateLegacySupplyStock() {
        for (Supply s : repository.findAllSupplies()) {
            boolean changed = false;
            if (s.getInStock() == null) {
                BigDecimal cur = s.getCurrentStock() == null
                        ? BigDecimal.ZERO : s.getCurrentStock();
                s.setInStock(cur);
                s.setCurrentStock(BigDecimal.ZERO);
                changed = true;
            }
            if (s.getRequestedQty() == null) {
                s.setRequestedQty(BigDecimal.ZERO);
                changed = true;
            }
            if (s.getOrderStatus() == null) {
                s.setOrderStatus("received");
                changed = true;
            }
            if (changed) {
                repository.saveSupply(s);
            }
        }
    }

    public Map<String, Object> adjustSupplyStock(long supplyId, BigDecimal delta, String note) {
        if (delta == null) {
            throw new IllegalArgumentException("delta is required");
        }
        Supply s = repository.findSupplyById(supplyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Supply not found: " + supplyId));
        BigDecimal current = s.getCurrentStock() == null ? BigDecimal.ZERO : s.getCurrentStock();
        BigDecimal next = current.add(delta);
        if (next.signum() < 0) {
            next = BigDecimal.ZERO;
        }
        s.setCurrentStock(next);
        if (note != null && !note.isBlank()) {
            s.setNotes(note.trim());
        }
        Supply saved = repository.saveSupply(s);
        return toSupplyMap(saved);
    }

    public Map<String, Object> saveSupply(Map<String, Object> body) {
        if (body == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        Long id = body.get("id") == null ? null : ((Number) body.get("id")).longValue();
        String name = trimToNull(body.get("name"));
        if (name == null) {
            throw new IllegalArgumentException("name is required");
        }
        String unit = trimToNull(body.get("unit"));
        if (unit == null) {
            throw new IllegalArgumentException("unit is required");
        }
        String unitLc = unit.toLowerCase();
        if (!ALLOWED_SUPPLY_UNITS.contains(unitLc)) {
            throw new IllegalArgumentException(
                    "Invalid unit. Allowed: " + ALLOWED_SUPPLY_UNITS);
        }
        String category = trimToNull(body.get("category"));
        String categoryLc = category == null ? "other" : category.toLowerCase();
        if (!ALLOWED_SUPPLY_CATEGORIES.contains(categoryLc)) {
            throw new IllegalArgumentException(
                    "Invalid category. Allowed: " + ALLOWED_SUPPLY_CATEGORIES);
        }
        BigDecimal inStock = toDecimal(body.get("inStock"));
        if (inStock == null) {
            inStock = toDecimal(body.get("currentStock"));
        }
        BigDecimal threshold = toDecimal(body.get("threshold"));
        String notes = trimToNull(body.get("notes"));

        Supply supply;
        if (id != null) {
            supply = repository.findSupplyById(id)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Supply not found: " + id));
        } else {
            supply = repository.findSupplyByName(name).orElseGet(Supply::new);
        }
        supply.setName(name);
        supply.setUnit(unitLc);
        supply.setCategory(categoryLc);
        if (inStock != null) {
            supply.setInStock(inStock);
        } else if (supply.getInStock() == null) {
            supply.setInStock(BigDecimal.ZERO);
        }
        if (supply.getCurrentStock() == null) {
            supply.setCurrentStock(BigDecimal.ZERO);
        }
        if (supply.getRequestedQty() == null) {
            supply.setRequestedQty(BigDecimal.ZERO);
        }
        if (supply.getOrderStatus() == null) {
            supply.setOrderStatus("received");
        }
        supply.setThreshold(threshold == null ? BigDecimal.ZERO : threshold);
        supply.setNotes(notes);
        Supply saved = repository.saveSupply(supply);
        return toSupplyMap(saved);
    }

    public List<Map<String, Object>> updateSupplyOrderStatuses(List<Map<String, Object>> updates) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("updates is required");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : updates) {
            if (row == null || row.get("id") == null) continue;
            long id = ((Number) row.get("id")).longValue();
            String raw = (String) row.get("orderStatus");
            String status = raw == null ? null : raw.trim().toLowerCase();
            if (status == null || !ALLOWED_SUPPLY_ORDER_STATUSES.contains(status)) {
                throw new IllegalArgumentException(
                        "Invalid orderStatus for supply " + id + ": " + raw);
            }
            BigDecimal requestedQty = toDecimal(row.get("requestedQty"));

            Supply s = repository.findSupplyById(id)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Supply not found: " + id));
            String prev = s.getOrderStatus() == null ? "received" : s.getOrderStatus();

            if ("received".equals(status)) {
                BigDecimal delivered = s.getCurrentStock() == null
                        ? BigDecimal.ZERO : s.getCurrentStock();
                BigDecimal pantry = s.getInStock() == null
                        ? BigDecimal.ZERO : s.getInStock();
                s.setInStock(pantry.add(delivered));
                s.setCurrentStock(BigDecimal.ZERO);
                s.setRequestedQty(BigDecimal.ZERO);
                s.setRequestedAt(null);
            } else {
                if (requestedQty != null) {
                    s.setRequestedQty(requestedQty.signum() < 0
                            ? BigDecimal.ZERO : requestedQty);
                }
                if (!status.equals(prev)) {
                    s.setRequestedAt(LocalDateTime.now());
                }
            }
            s.setOrderStatus(status);
            result.add(toSupplyMap(repository.saveSupply(s)));
        }
        return result;
    }

    public Map<String, Object> requestMoreSupply(long supplyId, BigDecimal requestedQty, String urgency) {
        return requestMoreSupplyByTeam(supplyId, requestedQty, urgency, null);
    }

    private static final Set<String> ALLOWED_REQUEST_TEAMS =
            Set.of("kitchen", "bakery", "sales", "delivery", "management");

    public Map<String, Object> requestMoreSupplyByTeam(long supplyId, BigDecimal requestedQty,
                                                       String urgency, String teamRaw) {
        if (requestedQty == null || requestedQty.signum() <= 0) {
            throw new IllegalArgumentException("requestedQty must be positive");
        }
        String status = urgency == null ? "waiting" : urgency.trim().toLowerCase();
        if (!ALLOWED_SUPPLY_ORDER_STATUSES.contains(status) || "received".equals(status)) {
            status = "waiting";
        }
        String team = teamRaw == null || teamRaw.isBlank()
                ? "kitchen" : teamRaw.trim().toLowerCase();
        if (!ALLOWED_REQUEST_TEAMS.contains(team)) {
            throw new IllegalArgumentException(
                    "team must be one of: " + ALLOWED_REQUEST_TEAMS);
        }
        Supply s = repository.findSupplyById(supplyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Supply not found: " + supplyId));
        s.setRequestedQty(requestedQty);
        s.setOrderStatus(status);
        s.setRequestedByTeam(team);
        s.setRequestedAt(LocalDateTime.now());
        return toSupplyMap(repository.saveSupply(s));
    }

    public List<Map<String, Object>> fetchRequestedSupplies() {
        return repository.findAllSupplies().stream()
                .filter(s -> {
                    String st = s.getOrderStatus() == null ? "received" : s.getOrderStatus();
                    return !"received".equalsIgnoreCase(st);
                })
                .map(this::toSupplyMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> fulfillSupply(long supplyId, BigDecimal receivedQty, String note) {
        if (receivedQty == null || receivedQty.signum() <= 0) {
            throw new IllegalArgumentException("receivedQty must be positive");
        }
        Supply s = repository.findSupplyById(supplyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Supply not found: " + supplyId));
        BigDecimal current = s.getCurrentStock() == null ? BigDecimal.ZERO : s.getCurrentStock();
        s.setCurrentStock(current.add(receivedQty));
        if (note != null && !note.isBlank()) {
            s.setNotes(note.trim());
        }
        return toSupplyMap(repository.saveSupply(s));
    }

    public Map<String, Object> seedSuppliesIfEmpty() {
        long count = repository.countSupplies();
        if (count > 0) {
            return Map.of("seeded", 0, "existing", count);
        }
        List<Supply> seeds = defaultSupplySeeds();
        for (Supply s : seeds) {
            repository.saveSupply(s);
        }
        return Map.of("seeded", seeds.size(), "existing", 0L);
    }

    private List<Supply> defaultSupplySeeds() {
        List<Supply> list = new ArrayList<>();
        list.add(seed("Finger Millet (Ragi) Flour", "kg", "flour", "10.0", "3.0"));
        list.add(seed("Bajra Millet Flour", "kg", "flour", "6.0", "2.0"));
        list.add(seed("Little Millet Flour", "kg", "flour", "5.0", "2.0"));
        list.add(seed("Sorghum (Jowar) Flour", "kg", "flour", "5.0", "2.0"));
        list.add(seed("Whole Wheat Flour", "kg", "flour", "12.0", "4.0"));
        list.add(seed("All-Purpose Flour", "kg", "flour", "10.0", "3.0"));
        list.add(seed("Cane Sugar", "kg", "sweetener", "8.0", "2.0"));
        list.add(seed("Brown Sugar", "kg", "sweetener", "4.0", "1.0"));
        list.add(seed("Jaggery", "kg", "sweetener", "5.0", "1.5"));
        list.add(seed("Honey", "l", "sweetener", "3.0", "1.0"));
        list.add(seed("Maple Syrup", "l", "sweetener", "2.0", "0.5"));
        list.add(seed("Butter (unsalted)", "kg", "dairy", "4.0", "1.0"));
        list.add(seed("Milk", "l", "dairy", "6.0", "2.0"));
        list.add(seed("Curd / Yogurt", "kg", "dairy", "2.0", "0.5"));
        list.add(seed("Eggs", "pcs", "egg", "60", "24"));
        list.add(seed("Baking Powder", "g", "leavening", "500", "150"));
        list.add(seed("Baking Soda", "g", "leavening", "500", "150"));
        list.add(seed("Yeast (instant)", "g", "leavening", "250", "50"));
        list.add(seed("Vanilla Essence", "ml", "flavour", "500", "100"));
        list.add(seed("Cocoa Powder", "kg", "flavour", "1.5", "0.5"));
        list.add(seed("Salt", "kg", "flavour", "2.0", "0.5"));
        list.add(seed("Almonds", "kg", "nut_seed", "1.5", "0.5"));
        list.add(seed("Cashews", "kg", "nut_seed", "1.5", "0.5"));
        list.add(seed("Sesame Seeds", "kg", "nut_seed", "1.0", "0.3"));
        list.add(seed("Cookie Boxes (small)", "pcs", "packaging", "120", "40"));
        list.add(seed("Cake Boxes", "pcs", "packaging", "60", "20"));
        list.add(seed("Paper Liners", "pack", "packaging", "10", "3"));
        list.add(seed("Parchment Paper", "pack", "packaging", "6", "2"));
        return list;
    }

    private Supply seed(String name, String unit, String category,
                        String current, String threshold) {
        Supply s = new Supply();
        s.setName(name);
        s.setUnit(unit);
        s.setCategory(category);
        s.setCurrentStock(BigDecimal.ZERO);
        s.setInStock(new BigDecimal(current));
        s.setRequestedQty(BigDecimal.ZERO);
        s.setThreshold(new BigDecimal(threshold));
        s.setOrderStatus("received");
        return s;
    }

    private Map<String, Object> toSupplyMap(Supply s) {
        Map<String, Object> m = new LinkedHashMap<>();
        BigDecimal current = s.getCurrentStock() == null ? BigDecimal.ZERO : s.getCurrentStock();
        BigDecimal inStock = s.getInStock() == null ? BigDecimal.ZERO : s.getInStock();
        BigDecimal requested = s.getRequestedQty() == null ? BigDecimal.ZERO : s.getRequestedQty();
        BigDecimal threshold = s.getThreshold() == null ? BigDecimal.ZERO : s.getThreshold();
        boolean out = inStock.signum() <= 0;
        boolean low = !out && inStock.compareTo(threshold) <= 0;
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("unit", s.getUnit());
        m.put("category", s.getCategory());
        m.put("currentStock", current);
        m.put("inStock", inStock);
        m.put("requestedQty", requested);
        m.put("threshold", threshold);
        m.put("notes", s.getNotes());
        m.put("lowStock", low);
        m.put("outOfStock", out);
        m.put("orderStatus",
                s.getOrderStatus() == null ? "received" : s.getOrderStatus());
        m.put("requestedByTeam", s.getRequestedByTeam());
        m.put("requestedAt",
                s.getRequestedAt() == null ? null : s.getRequestedAt().toString());
        m.put("updatedAt", s.getUpdatedAt() == null ? null : s.getUpdatedAt().toString());
        return m;
    }

    private static BigDecimal toDecimal(Object raw) {
        if (raw == null) return null;
        if (raw instanceof BigDecimal bd) return bd;
        if (raw instanceof Number n) return new BigDecimal(n.toString());
        String s = raw.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number: " + s);
        }
    }

    public List<Map<String, Object>> fetchDailyStock(LocalDate stockDate) {
        LocalDate date = stockDate == null ? LocalDate.now() : stockDate;
        List<DailyStock> rows = repository.findDailyStockByDate(date);
        return rows.stream().map(ds -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ds.getId());
            m.put("productId", ds.getProduct() == null ? null : ds.getProduct().getId());
            m.put("productName",
                    ds.getProduct() == null ? "Unknown" : ds.getProduct().getName());
            m.put("targetCount", ds.getTargetCount());
            m.put("preparedCount",
                    ds.getPreparedCount() == null ? 0 : ds.getPreparedCount());
            int remaining = (ds.getTargetCount() == null ? 0 : ds.getTargetCount())
                    - (ds.getPreparedCount() == null ? 0 : ds.getPreparedCount());
            m.put("remaining", Math.max(remaining, 0));
            m.put("stockDate", ds.getStockDate() == null ? null : ds.getStockDate().toString());
            return m;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> salesAnalytics(String fromStr, String toStr) {
        LocalDate today = LocalDate.now();
        LocalDate fromDate = parseDateOrDefault(fromStr, today.minusDays(29), "from");
        LocalDate toDate = parseDateOrDefault(toStr, today, "to");
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("'from' date must be on or before 'to' date");
        }
        LocalDateTime fromTs = fromDate.atStartOfDay();
        LocalDateTime toTs = toDate.plusDays(1).atStartOfDay();

        List<Orders> orders = repository.findOrdersInRange(fromTs, toTs);
        List<Long> orderIds = orders.stream().map(Orders::getId).collect(Collectors.toList());
        List<OrderItem> items = repository.findOrderItemsByOrderIds(orderIds);
        List<Payment> payments = repository.findPaymentsByOrderIds(orderIds);

        BigDecimal totalRevenue = orders.stream()
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int orderCount = orders.size();
        BigDecimal avgOrderValue = orderCount == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", fromDate.toString());
        result.put("to", toDate.toString());
        result.put("totalRevenue", totalRevenue);
        result.put("orderCount", orderCount);
        result.put("avgOrderValue", avgOrderValue);
        result.put("revenueByChannel", revenueByChannel(orders));
        result.put("revenueByPaymentMethod", revenueByPaymentMethod(payments));
        result.put("ordersByStatus", countByStatus(orders));
        result.put("topProducts", topProducts(items, 5));
        result.put("dailyTrend", dailyTrend(orders, fromDate, toDate));
        return result;
    }

    private static LocalDate parseDateOrDefault(String s, LocalDate fallback, String label) {
        if (s == null || s.isBlank()) return fallback;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid '" + label + "' date (expected YYYY-MM-DD)");
        }
    }

    private static Map<String, BigDecimal> revenueByChannel(List<Orders> orders) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        for (Orders o : orders) {
            String key = (o.getChannel() == null || o.getChannel().isBlank())
                    ? "online" : o.getChannel().toLowerCase();
            BigDecimal amt = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
            out.merge(key, amt, BigDecimal::add);
        }
        return out;
    }

    private static Map<String, BigDecimal> revenueByPaymentMethod(List<Payment> payments) {
        Map<String, BigDecimal> out = new LinkedHashMap<>();
        for (Payment p : payments) {
            if (p.getStatus() != null && p.getStatus().equalsIgnoreCase("FAILED")) continue;
            String key = p.getPaymentMethod() == null ? "UNKNOWN" : p.getPaymentMethod().toUpperCase();
            BigDecimal amt = p.getAmount() == null ? BigDecimal.ZERO : p.getAmount();
            out.merge(key, amt, BigDecimal::add);
        }
        return out;
    }

    private static Map<String, Integer> countByStatus(List<Orders> orders) {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (Orders o : orders) {
            String key = o.getStatus() == null ? "UNKNOWN" : o.getStatus().toUpperCase();
            out.merge(key, 1, Integer::sum);
        }
        return out;
    }

    private static List<Map<String, Object>> topProducts(List<OrderItem> items, int limit) {
        Map<Long, long[]> qtyByProduct = new HashMap<>();
        Map<Long, BigDecimal> revByProduct = new HashMap<>();
        Map<Long, String> nameByProduct = new HashMap<>();
        for (OrderItem oi : items) {
            if (oi.getProduct() == null) continue;
            Long pid = oi.getProduct().getId();
            int qty = oi.getQuantity() == null ? 0 : oi.getQuantity();
            BigDecimal unit = oi.getPrice() == null ? BigDecimal.ZERO : oi.getPrice();
            qtyByProduct.computeIfAbsent(pid, k -> new long[]{0})[0] += qty;
            revByProduct.merge(pid, unit.multiply(BigDecimal.valueOf(qty)), BigDecimal::add);
            nameByProduct.putIfAbsent(pid, oi.getProduct().getName());
        }
        return qtyByProduct.entrySet().stream()
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("productId", e.getKey());
                    row.put("name", nameByProduct.get(e.getKey()));
                    row.put("quantitySold", e.getValue()[0]);
                    row.put("revenue", revByProduct.getOrDefault(e.getKey(), BigDecimal.ZERO));
                    return row;
                })
                .sorted(Comparator.comparing(
                        (Map<String, Object> r) -> (BigDecimal) r.get("revenue")).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Map<String, Object> createTask(int createdByUserId,
                                          String assignedToDepartmentRaw,
                                          Integer assignedToUserId,
                                          String title,
                                          String description,
                                          String priorityRaw,
                                          String dueDateStr,
                                          Long relatedOrderId) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("title must be at most 200 characters");
        }
        String dept = assignedToDepartmentRaw == null ? "" : assignedToDepartmentRaw.trim().toLowerCase();
        if (!ALLOWED_TASK_DEPARTMENTS.contains(dept)) {
            throw new IllegalArgumentException(
                    "assignedToDepartment must be one of: " + ALLOWED_TASK_DEPARTMENTS);
        }
        String priority = priorityRaw == null || priorityRaw.isBlank()
                ? "normal" : priorityRaw.trim().toLowerCase();
        if (!ALLOWED_TASK_PRIORITIES.contains(priority)) {
            throw new IllegalArgumentException(
                    "priority must be one of: " + ALLOWED_TASK_PRIORITIES);
        }
        LocalDate dueDate = null;
        if (dueDateStr != null && !dueDateStr.isBlank()) {
            try {
                dueDate = LocalDate.parse(dueDateStr.trim());
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid dueDate (expected YYYY-MM-DD)");
            }
        }
        User creator = repository.findUserById(createdByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Creator user not found: " + createdByUserId));
        User assignee = null;
        if (assignedToUserId != null) {
            assignee = repository.findUserById(assignedToUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Assignee user not found: " + assignedToUserId));
        }
        if (relatedOrderId != null) {
            repository.findOrderById(relatedOrderId)
                    .orElseThrow(() -> new IllegalArgumentException("Related order not found: " + relatedOrderId));
        }

        Task t = new Task();
        t.setCreatedBy(creator);
        t.setAssignedToDepartment(dept);
        t.setAssignedToUser(assignee);
        t.setTitle(title.trim());
        t.setDescription(description == null || description.isBlank() ? null : description.trim());
        t.setPriority(priority);
        t.setStatus("open");
        t.setDueDate(dueDate);
        t.setRelatedOrderId(relatedOrderId);
        LocalDateTime now = LocalDateTime.now();
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        Task saved = repository.saveTask(t);
        return toTaskMap(saved);
    }

    public List<Map<String, Object>> listTasks(String department, Integer createdByUserId, String status) {
        List<Task> tasks;
        if (department != null && !department.isBlank()) {
            String dept = department.trim().toLowerCase();
            if (!ALLOWED_TASK_DEPARTMENTS.contains(dept)) {
                throw new IllegalArgumentException(
                        "department must be one of: " + ALLOWED_TASK_DEPARTMENTS);
            }
            tasks = repository.findTasksByDepartment(dept);
        } else if (createdByUserId != null) {
            tasks = repository.findTasksByCreatedBy(createdByUserId);
        } else {
            tasks = repository.findAllTasks();
        }
        if (status != null && !status.isBlank()) {
            String s = status.trim().toLowerCase();
            if (!ALLOWED_TASK_STATUSES.contains(s)) {
                throw new IllegalArgumentException("status must be one of: " + ALLOWED_TASK_STATUSES);
            }
            tasks = tasks.stream()
                    .filter(t -> s.equalsIgnoreCase(t.getStatus()))
                    .collect(Collectors.toList());
        }
        return tasks.stream().map(UserService::toTaskMap).collect(Collectors.toList());
    }

    public Map<String, Object> updateTaskStatus(long taskId,
                                                String newStatusRaw,
                                                Integer actingUserId,
                                                String resolutionNotes) {
        if (newStatusRaw == null || newStatusRaw.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        String newStatus = newStatusRaw.trim().toLowerCase();
        if (!ALLOWED_TASK_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException("status must be one of: " + ALLOWED_TASK_STATUSES);
        }
        Task t = repository.findTaskById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        String current = t.getStatus() == null ? "open" : t.getStatus().toLowerCase();
        if (TERMINAL_TASK_STATUSES.contains(current)) {
            throw new IllegalArgumentException(
                    "Task is already " + current + " and cannot change status");
        }
        t.setStatus(newStatus);
        LocalDateTime now = LocalDateTime.now();
        t.setUpdatedAt(now);
        if (TERMINAL_TASK_STATUSES.contains(newStatus)) {
            t.setCompletedAt(now);
            if (actingUserId != null) {
                User u = repository.findUserById(actingUserId)
                        .orElseThrow(() -> new IllegalArgumentException("Acting user not found: " + actingUserId));
                t.setCompletedBy(u);
            }
            if (resolutionNotes != null && !resolutionNotes.isBlank()) {
                t.setResolutionNotes(resolutionNotes.trim());
            }
        }
        return toTaskMap(repository.saveTask(t));
    }

    private static Map<String, Object> toTaskMap(Task t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("title", t.getTitle());
        m.put("description", t.getDescription());
        m.put("assignedToDepartment", t.getAssignedToDepartment());
        m.put("assignedToUserId",
                t.getAssignedToUser() == null ? null : t.getAssignedToUser().getuserid());
        m.put("assignedToUserName",
                t.getAssignedToUser() == null ? null : t.getAssignedToUser().getname());
        m.put("createdByUserId",
                t.getCreatedBy() == null ? null : t.getCreatedBy().getuserid());
        m.put("createdByName",
                t.getCreatedBy() == null ? null : t.getCreatedBy().getname());
        m.put("priority", t.getPriority());
        m.put("status", t.getStatus());
        m.put("dueDate", t.getDueDate() == null ? null : t.getDueDate().toString());
        m.put("relatedOrderId", t.getRelatedOrderId());
        m.put("createdAt", t.getCreatedAt() == null ? null : t.getCreatedAt().toString());
        m.put("updatedAt", t.getUpdatedAt() == null ? null : t.getUpdatedAt().toString());
        m.put("completedAt", t.getCompletedAt() == null ? null : t.getCompletedAt().toString());
        m.put("completedByName",
                t.getCompletedBy() == null ? null : t.getCompletedBy().getname());
        m.put("resolutionNotes", t.getResolutionNotes());
        return m;
    }

    public Map<String, Object> pickUpTrip(long orderId, int driverId) {
        Orders order = repository.findOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        String channel = order.getChannel() == null ? "" : order.getChannel().toLowerCase();
        if (!"online".equals(channel)) {
            throw new IllegalArgumentException("Only online orders can be delivered");
        }
        String kitchen = order.getKitchenStatus() == null ? "" : order.getKitchenStatus().toLowerCase();
        if (!"done".equals(kitchen)) {
            throw new IllegalArgumentException(
                    "Order is not ready for pickup (kitchen status must be 'done')");
        }
        repository.findTripByOrderId(orderId).ifPresent(t -> {
            String s = t.getStatus() == null ? "" : t.getStatus().toLowerCase();
            if (!"failed".equals(s)) {
                throw new IllegalArgumentException(
                        "A trip already exists for this order (status: " + s + ")");
            }
        });
        User driver = repository.findUserById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

        DeliveryTrip trip = new DeliveryTrip();
        trip.setOrder(order);
        trip.setDriver(driver);
        trip.setStatus("picked_up");
        LocalDateTime now = LocalDateTime.now();
        trip.setPickedUpAt(now);
        trip.setCreatedAt(now);
        trip.setUpdatedAt(now);

        order.setKitchenStatus("picked_up");
        repository.saveOrder(order);

        return toTripMap(repository.saveTrip(trip));
    }

    public Map<String, Object> markOutForDelivery(long tripId, int driverId) {
        DeliveryTrip trip = loadDriverTrip(tripId, driverId);
        String s = trip.getStatus() == null ? "" : trip.getStatus().toLowerCase();
        if (!"picked_up".equals(s)) {
            throw new IllegalArgumentException(
                    "Trip must be in 'picked_up' state to go out for delivery (was: " + s + ")");
        }
        trip.setStatus("out_for_delivery");
        LocalDateTime now = LocalDateTime.now();
        trip.setOutAt(now);
        trip.setUpdatedAt(now);
        if (trip.getOtpCode() == null || trip.getOtpCode().isBlank()) {
            trip.setOtpCode(generateOtp());
        }
        if (trip.getOrder() != null) {
            trip.getOrder().setKitchenStatus("out_for_delivery");
            repository.saveOrder(trip.getOrder());
        }
        return toTripMap(repository.saveTrip(trip));
    }

    public Map<String, Object> markDelivered(long tripId,
                                              int driverId,
                                              String otp,
                                              String photoUrl,
                                              BigDecimal codAmount,
                                              BigDecimal tipAmount,
                                              BigDecimal distanceKm,
                                              String notes) {
        DeliveryTrip trip = loadDriverTrip(tripId, driverId);
        String s = trip.getStatus() == null ? "" : trip.getStatus().toLowerCase();
        if (!"out_for_delivery".equals(s)) {
            throw new IllegalArgumentException(
                    "Trip must be 'out_for_delivery' to deliver (was: " + s + ")");
        }
        boolean hasPhoto = photoUrl != null && !photoUrl.isBlank();
        boolean hasOtp = otp != null && !otp.isBlank();
        if (!hasPhoto && !hasOtp) {
            throw new IllegalArgumentException(
                    "Proof of delivery is required: enter the customer OTP or upload a photo");
        }
        if (hasOtp && trip.getOtpCode() != null && !trip.getOtpCode().equals(otp.trim())) {
            throw new IllegalArgumentException("OTP does not match");
        }
        if (codAmount != null && codAmount.signum() < 0) {
            throw new IllegalArgumentException("COD amount cannot be negative");
        }
        if (tipAmount != null && tipAmount.signum() < 0) {
            throw new IllegalArgumentException("Tip cannot be negative");
        }
        if (distanceKm != null && distanceKm.signum() < 0) {
            throw new IllegalArgumentException("Distance cannot be negative");
        }

        LocalDateTime now = LocalDateTime.now();
        trip.setStatus("delivered");
        trip.setDeliveredAt(now);
        trip.setUpdatedAt(now);
        if (hasPhoto) trip.setPhotoProofUrl(photoUrl.trim());
        if (codAmount != null && codAmount.signum() > 0) {
            trip.setCodAmount(codAmount);
            trip.setCodCollectedAt(now);
        }
        if (tipAmount != null) trip.setTipAmount(tipAmount);
        if (distanceKm != null) trip.setDistanceKm(distanceKm);
        if (notes != null && !notes.isBlank()) trip.setNotes(notes.trim());

        if (trip.getOrder() != null) {
            trip.getOrder().setKitchenStatus("delivered");
            trip.getOrder().setStatus("delivered");
            repository.saveOrder(trip.getOrder());
        }
        return toTripMap(repository.saveTrip(trip));
    }

    public Map<String, Object> markTripFailed(long tripId,
                                               int driverId,
                                               String reasonRaw,
                                               String notes) {
        if (reasonRaw == null || reasonRaw.isBlank()) {
            throw new IllegalArgumentException("Failure reason is required");
        }
        String reason = reasonRaw.trim().toLowerCase();
        if (!ALLOWED_FAILURE_REASONS.contains(reason)) {
            throw new IllegalArgumentException(
                    "Failure reason must be one of: " + ALLOWED_FAILURE_REASONS);
        }
        DeliveryTrip trip = loadDriverTrip(tripId, driverId);
        String s = trip.getStatus() == null ? "" : trip.getStatus().toLowerCase();
        if (TERMINAL_TRIP_STATUSES.contains(s)) {
            throw new IllegalArgumentException(
                    "Trip is already " + s + " and cannot be failed");
        }
        LocalDateTime now = LocalDateTime.now();
        trip.setStatus("failed");
        trip.setFailedAt(now);
        trip.setUpdatedAt(now);
        trip.setFailureReason(reason);
        if (notes != null && !notes.isBlank()) trip.setNotes(notes.trim());

        if (trip.getOrder() != null) {
            trip.getOrder().setKitchenStatus("delivery_failed");
            repository.saveOrder(trip.getOrder());
        }
        return toTripMap(repository.saveTrip(trip));
    }

    public List<Map<String, Object>> listTripsForDriver(int driverId, String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank()) {
            String s = statusFilter.trim().toLowerCase();
            if (!"active".equals(s) && !ALLOWED_TRIP_STATUSES.contains(s)) {
                throw new IllegalArgumentException(
                        "status must be 'active' or one of: " + ALLOWED_TRIP_STATUSES);
            }
            if ("active".equals(s)) {
                return repository.findActiveTripsByDriver(driverId).stream()
                        .map(this::toTripMap)
                        .collect(Collectors.toList());
            }
            return repository.findTripsByDriver(driverId).stream()
                    .filter(t -> s.equalsIgnoreCase(t.getStatus()))
                    .map(this::toTripMap)
                    .collect(Collectors.toList());
        }
        return repository.findTripsByDriver(driverId).stream()
                .map(this::toTripMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> logDeliveryIssue(int driverId,
                                                 String issueTypeRaw,
                                                 String description,
                                                 Long tripId) {
        if (issueTypeRaw == null || issueTypeRaw.isBlank()) {
            throw new IllegalArgumentException("Issue type is required");
        }
        String type = issueTypeRaw.trim().toLowerCase();
        if (!ALLOWED_ISSUE_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                    "Issue type must be one of: " + ALLOWED_ISSUE_TYPES);
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description is required");
        }
        if (description.length() > 500) {
            throw new IllegalArgumentException("Description must be at most 500 characters");
        }
        User driver = repository.findUserById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));
        DeliveryIssue issue = new DeliveryIssue();
        issue.setDriver(driver);
        issue.setIssueType(type);
        issue.setDescription(description.trim());
        issue.setReportedAt(LocalDateTime.now());
        if (tripId != null) {
            DeliveryTrip trip = repository.findTripById(tripId)
                    .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
            issue.setTrip(trip);
        }
        return toIssueMap(repository.saveIssue(issue));
    }

    public List<Map<String, Object>> listIssuesForDriver(int driverId) {
        return repository.findIssuesByDriver(driverId).stream()
                .map(UserService::toIssueMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> shiftSummary(int driverId, String fromStr, String toStr) {
        LocalDate from;
        LocalDate to;
        try {
            from = (fromStr == null || fromStr.isBlank())
                    ? LocalDate.now() : LocalDate.parse(fromStr);
            to = (toStr == null || toStr.isBlank())
                    ? from : LocalDate.parse(toStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Dates must be ISO format yyyy-MM-dd");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be on or before 'to'");
        }
        LocalDateTime fromTs = from.atStartOfDay();
        LocalDateTime toTs = to.plusDays(1).atStartOfDay();
        List<DeliveryTrip> trips = repository.findTripsByDriverInRange(driverId, fromTs, toTs);
        int total = trips.size();
        long delivered = trips.stream().filter(t -> "delivered".equalsIgnoreCase(t.getStatus())).count();
        long failed = trips.stream().filter(t -> "failed".equalsIgnoreCase(t.getStatus())).count();
        long inFlight = total - delivered - failed;
        BigDecimal codTotal = trips.stream()
                .filter(t -> "delivered".equalsIgnoreCase(t.getStatus()) && t.getCodAmount() != null)
                .map(DeliveryTrip::getCodAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tipsTotal = trips.stream()
                .filter(t -> t.getTipAmount() != null)
                .map(DeliveryTrip::getTipAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal distanceTotal = trips.stream()
                .filter(t -> t.getDistanceKm() != null)
                .map(DeliveryTrip::getDistanceKm)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> byFailureReason = new LinkedHashMap<>();
        for (DeliveryTrip t : trips) {
            if ("failed".equalsIgnoreCase(t.getStatus()) && t.getFailureReason() != null) {
                byFailureReason.merge(t.getFailureReason(), 1L, Long::sum);
            }
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("driverId", driverId);
        m.put("from", from.toString());
        m.put("to", to.toString());
        m.put("totalTrips", total);
        m.put("delivered", delivered);
        m.put("failed", failed);
        m.put("inFlight", inFlight);
        m.put("codCollected", codTotal);
        m.put("tipsTotal", tipsTotal);
        m.put("distanceKm", distanceTotal);
        m.put("failuresByReason", byFailureReason);
        return m;
    }

    private DeliveryTrip loadDriverTrip(long tripId, int driverId) {
        DeliveryTrip trip = repository.findTripById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        if (trip.getDriver() == null || trip.getDriver().getuserid() != driverId) {
            throw new IllegalArgumentException("This trip is not assigned to you");
        }
        return trip;
    }

    private static String generateOtp() {
        int n = (int) (Math.random() * 9000) + 1000;
        return String.valueOf(n);
    }

    private Map<String, Object> toTripMap(DeliveryTrip t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("status", t.getStatus());
        Long orderId = t.getOrder() == null ? null : t.getOrder().getId();
        m.put("orderId", orderId);
        m.put("orderTotal",
                t.getOrder() == null ? null : t.getOrder().getTotalAmount());
        m.put("customerName",
                (t.getOrder() == null || t.getOrder().getUser() == null)
                        ? null : t.getOrder().getUser().getname());
        if (orderId != null) {
            repository.findShippingAddressByOrderId(orderId).ifPresent(sa -> {
                StringBuilder addr = new StringBuilder();
                if (sa.getLine1() != null) addr.append(sa.getLine1());
                if (sa.getLine2() != null && !sa.getLine2().isBlank())
                    addr.append(", ").append(sa.getLine2());
                if (sa.getCity() != null) addr.append(", ").append(sa.getCity());
                if (sa.getPincode() != null) addr.append(" ").append(sa.getPincode());
                m.put("shippingAddress", addr.toString());
                m.put("customerPhone", sa.getPhone());
            });
        }
        m.put("driverId", t.getDriver() == null ? null : t.getDriver().getuserid());
        m.put("driverName", t.getDriver() == null ? null : t.getDriver().getname());
        m.put("otpCode", t.getOtpCode());
        m.put("photoProofUrl", t.getPhotoProofUrl());
        m.put("codAmount", t.getCodAmount());
        m.put("codCollectedAt",
                t.getCodCollectedAt() == null ? null : t.getCodCollectedAt().toString());
        m.put("tipAmount", t.getTipAmount());
        m.put("distanceKm", t.getDistanceKm());
        m.put("failureReason", t.getFailureReason());
        m.put("notes", t.getNotes());
        m.put("pickedUpAt",
                t.getPickedUpAt() == null ? null : t.getPickedUpAt().toString());
        m.put("outAt", t.getOutAt() == null ? null : t.getOutAt().toString());
        m.put("deliveredAt",
                t.getDeliveredAt() == null ? null : t.getDeliveredAt().toString());
        m.put("failedAt",
                t.getFailedAt() == null ? null : t.getFailedAt().toString());
        m.put("createdAt",
                t.getCreatedAt() == null ? null : t.getCreatedAt().toString());
        m.put("updatedAt",
                t.getUpdatedAt() == null ? null : t.getUpdatedAt().toString());
        return m;
    }

    private static Map<String, Object> toIssueMap(DeliveryIssue i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", i.getId());
        m.put("driverId", i.getDriver() == null ? null : i.getDriver().getuserid());
        m.put("driverName", i.getDriver() == null ? null : i.getDriver().getname());
        m.put("tripId", i.getTrip() == null ? null : i.getTrip().getId());
        m.put("issueType", i.getIssueType());
        m.put("description", i.getDescription());
        m.put("reportedAt",
                i.getReportedAt() == null ? null : i.getReportedAt().toString());
        m.put("resolvedAt",
                i.getResolvedAt() == null ? null : i.getResolvedAt().toString());
        return m;
    }

    // =========================================================================
    //  Management aggregations: Live Ops, Orders Audit, Deliveries Audit,
    //  Day P&L, Staff Performance.  All read-only.
    // =========================================================================

    private static final int KITCHEN_SLA_MINUTES  = 30;
    private static final int DELIVERY_SLA_MINUTES = 60;

    public Map<String, Object> managementOps() {
        List<Orders> pipeline = repository.findOrdersInPipeline();
        Map<String, Map<String, Long>> kitchenByChannel = new LinkedHashMap<>();
        kitchenByChannel.put("online", emptyKitchenCounts());
        kitchenByChannel.put("instore", emptyKitchenCounts());

        List<Map<String, Object>> breaches = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Orders o : pipeline) {
            String channel = o.getChannel() == null ? "online" : o.getChannel().toLowerCase();
            String ks = (o.getKitchenStatus() == null ? "pending" : o.getKitchenStatus()).toLowerCase();
            kitchenByChannel.computeIfAbsent(channel, k -> emptyKitchenCounts());
            kitchenByChannel.get(channel).merge(ks, 1L, Long::sum);

            // SLA breach: order has been sitting in kitchen pipeline too long.
            if (o.getCreatedAt() != null && isKitchenSide(ks)) {
                long mins = ChronoUnit.MINUTES.between(o.getCreatedAt(), now);
                if (mins >= KITCHEN_SLA_MINUTES) {
                    Map<String, Object> b = new LinkedHashMap<>();
                    b.put("type", "kitchen");
                    b.put("orderId", o.getId());
                    b.put("status", ks);
                    b.put("channel", channel);
                    b.put("ageMinutes", mins);
                    b.put("customerName",
                            o.getUser() == null ? null : o.getUser().getname());
                    breaches.add(b);
                }
            }
        }

        // Delivery in-flight counts and SLA breaches (out_for_delivery > 60 min).
        long pickedUp = 0;
        long outForDelivery = 0;
        for (DeliveryTrip t : repository.findAllTrips()) {
            String s = t.getStatus() == null ? "" : t.getStatus().toLowerCase();
            if ("picked_up".equals(s)) pickedUp++;
            else if ("out_for_delivery".equals(s)) {
                outForDelivery++;
                LocalDateTime since = t.getOutAt() != null ? t.getOutAt() : t.getCreatedAt();
                if (since != null) {
                    long mins = ChronoUnit.MINUTES.between(since, now);
                    if (mins >= DELIVERY_SLA_MINUTES) {
                        Map<String, Object> b = new LinkedHashMap<>();
                        b.put("type", "delivery");
                        b.put("tripId", t.getId());
                        b.put("orderId",
                                t.getOrder() == null ? null : t.getOrder().getId());
                        b.put("status", s);
                        b.put("ageMinutes", mins);
                        b.put("driverName",
                                t.getDriver() == null ? null : t.getDriver().getname());
                        breaches.add(b);
                    }
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("kitchenQueue", kitchenByChannel);
        Map<String, Long> dlv = new LinkedHashMap<>();
        dlv.put("pickedUp", pickedUp);
        dlv.put("outForDelivery", outForDelivery);
        dlv.put("total", pickedUp + outForDelivery);
        result.put("deliveryInFlight", dlv);
        result.put("breaches", breaches);
        result.put("kitchenSlaMinutes", KITCHEN_SLA_MINUTES);
        result.put("deliverySlaMinutes", DELIVERY_SLA_MINUTES);
        result.put("asOf", now.toString());
        return result;
    }

    private static Map<String, Long> emptyKitchenCounts() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String s : List.of("pending", "preparing", "ready", "done",
                                "picked_up", "out_for_delivery")) {
            m.put(s, 0L);
        }
        return m;
    }

    private static boolean isKitchenSide(String s) {
        return s == null
                || List.of("pending", "preparing", "ready", "done").contains(s);
    }

    public Map<String, Object> managementOrdersAudit(String fromStr, String toStr,
                                                      String channelFilter,
                                                      String paymentFilter) {
        LocalDate[] range = parseDateRange(fromStr, toStr);
        LocalDateTime fromTs = range[0].atStartOfDay();
        LocalDateTime toTs   = range[1].plusDays(1).atStartOfDay();

        List<Orders> orders = repository.findOrdersInRange(fromTs, toTs);
        if (channelFilter != null && !channelFilter.isBlank()
                && !"all".equalsIgnoreCase(channelFilter)) {
            String c = channelFilter.toLowerCase();
            orders = orders.stream()
                    .filter(o -> c.equalsIgnoreCase(o.getChannel()))
                    .collect(Collectors.toList());
        }

        List<Long> ids = orders.stream().map(Orders::getId).collect(Collectors.toList());
        List<Payment> payments = repository.findPaymentsByOrderIds(ids);
        Map<Long, Payment> paymentByOrder = new HashMap<>();
        for (Payment p : payments) {
            if (p.getOrder() != null && p.getOrder().getId() != null) {
                paymentByOrder.put(p.getOrder().getId(), p);
            }
        }

        if (paymentFilter != null && !paymentFilter.isBlank()
                && !"all".equalsIgnoreCase(paymentFilter)) {
            String pm = paymentFilter.toLowerCase();
            orders = orders.stream().filter(o -> {
                Payment p = paymentByOrder.get(o.getId());
                String method = p == null || p.getPaymentMethod() == null
                        ? "" : p.getPaymentMethod().toLowerCase();
                return pm.equals(method);
            }).collect(Collectors.toList());
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        Map<String, BigDecimal> byChannel = new LinkedHashMap<>();
        Map<String, BigDecimal> byPaymentMethod = new LinkedHashMap<>();
        Map<String, Long> countByPaymentMethod = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Orders o : orders) {
            BigDecimal amt = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
            totalRevenue = totalRevenue.add(amt);
            String channel = o.getChannel() == null ? "unknown" : o.getChannel().toLowerCase();
            byChannel.merge(channel, amt, BigDecimal::add);

            Payment p = paymentByOrder.get(o.getId());
            String method = p == null || p.getPaymentMethod() == null
                    ? "unknown" : p.getPaymentMethod().toLowerCase();
            byPaymentMethod.merge(method, amt, BigDecimal::add);
            countByPaymentMethod.merge(method, 1L, Long::sum);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("orderId", o.getId());
            row.put("customerName",
                    o.getUser() == null ? null : o.getUser().getname());
            row.put("channel", channel);
            row.put("status", o.getStatus());
            row.put("kitchenStatus", o.getKitchenStatus());
            row.put("totalAmount", amt);
            row.put("paymentMethod", method);
            row.put("paymentStatus",
                    p == null ? null : p.getStatus());
            row.put("createdAt",
                    o.getCreatedAt() == null ? null : o.getCreatedAt().toString());
            rows.add(row);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", range[0].toString());
        result.put("to",   range[1].toString());
        result.put("count", orders.size());
        result.put("totalRevenue", totalRevenue);
        result.put("revenueByChannel", byChannel);
        result.put("revenueByPaymentMethod", byPaymentMethod);
        result.put("countByPaymentMethod", countByPaymentMethod);
        result.put("orders", rows);
        return result;
    }

    public Map<String, Object> managementDeliveriesAudit(String fromStr, String toStr,
                                                          Integer driverId,
                                                          String statusFilter) {
        LocalDate[] range = parseDateRange(fromStr, toStr);
        LocalDateTime fromTs = range[0].atStartOfDay();
        LocalDateTime toTs   = range[1].plusDays(1).atStartOfDay();

        List<DeliveryTrip> trips = repository.findTripsInRange(fromTs, toTs);
        if (driverId != null) {
            trips = trips.stream()
                    .filter(t -> t.getDriver() != null && t.getDriver().getuserid() == driverId)
                    .collect(Collectors.toList());
        }
        if (statusFilter != null && !statusFilter.isBlank()
                && !"all".equalsIgnoreCase(statusFilter)) {
            String s = statusFilter.toLowerCase();
            trips = trips.stream()
                    .filter(t -> s.equalsIgnoreCase(t.getStatus()))
                    .collect(Collectors.toList());
        }

        long delivered = trips.stream().filter(t -> "delivered".equalsIgnoreCase(t.getStatus())).count();
        long failed = trips.stream().filter(t -> "failed".equalsIgnoreCase(t.getStatus())).count();
        long inFlight = trips.size() - delivered - failed;
        BigDecimal codTotal = trips.stream()
                .filter(t -> t.getCodAmount() != null && "delivered".equalsIgnoreCase(t.getStatus()))
                .map(DeliveryTrip::getCodAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tipsTotal = trips.stream()
                .filter(t -> t.getTipAmount() != null)
                .map(DeliveryTrip::getTipAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Map<String, Object>> rows = trips.stream()
                .map(this::toTripMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", range[0].toString());
        result.put("to",   range[1].toString());
        result.put("count", trips.size());
        result.put("delivered", delivered);
        result.put("failed", failed);
        result.put("inFlight", inFlight);
        result.put("codCollected", codTotal);
        result.put("tipsTotal", tipsTotal);
        result.put("trips", rows);
        return result;
    }

    public Map<String, Object> managementDayPnl(String dateStr) {
        LocalDate day;
        try {
            day = (dateStr == null || dateStr.isBlank())
                    ? LocalDate.now() : LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("date must be ISO format yyyy-MM-dd");
        }
        LocalDateTime fromTs = day.atStartOfDay();
        LocalDateTime toTs   = day.plusDays(1).atStartOfDay();

        List<Orders> orders = repository.findOrdersInRange(fromTs, toTs);
        List<Long> ids = orders.stream().map(Orders::getId).collect(Collectors.toList());
        List<Payment> payments = repository.findPaymentsByOrderIds(ids);
        Map<Long, Payment> payByOrder = new HashMap<>();
        for (Payment p : payments) {
            if (p.getOrder() != null && p.getOrder().getId() != null) {
                payByOrder.put(p.getOrder().getId(), p);
            }
        }
        BigDecimal onlineRevenue = BigDecimal.ZERO;
        BigDecimal counterRevenue = BigDecimal.ZERO;
        BigDecimal cashRevenue = BigDecimal.ZERO;
        BigDecimal cardRevenue = BigDecimal.ZERO;
        BigDecimal upiRevenue = BigDecimal.ZERO;
        long onlineCount = 0;
        long counterCount = 0;
        for (Orders o : orders) {
            BigDecimal amt = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
            String channel = o.getChannel() == null ? "" : o.getChannel().toLowerCase();
            if ("online".equals(channel)) {
                onlineRevenue = onlineRevenue.add(amt);
                onlineCount++;
            } else if ("instore".equals(channel) || "pos".equals(channel)) {
                counterRevenue = counterRevenue.add(amt);
                counterCount++;
            }
            Payment p = payByOrder.get(o.getId());
            String method = p == null || p.getPaymentMethod() == null
                    ? "" : p.getPaymentMethod().toLowerCase();
            switch (method) {
                case "cash" -> cashRevenue = cashRevenue.add(amt);
                case "card" -> cardRevenue = cardRevenue.add(amt);
                case "upi" -> upiRevenue = upiRevenue.add(amt);
                default -> { /* unknown */ }
            }
        }

        // Delivery COD + tips for the same day
        List<DeliveryTrip> trips = repository.findTripsInRange(fromTs, toTs);
        BigDecimal cod = trips.stream()
                .filter(t -> "delivered".equalsIgnoreCase(t.getStatus()) && t.getCodAmount() != null)
                .map(DeliveryTrip::getCodAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal tips = trips.stream()
                .filter(t -> t.getTipAmount() != null)
                .map(DeliveryTrip::getTipAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = onlineRevenue.add(counterRevenue);
        BigDecimal grossInflow = totalRevenue.add(tips);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", day.toString());
        result.put("orderCount", onlineCount + counterCount);
        result.put("onlineOrderCount", onlineCount);
        result.put("counterOrderCount", counterCount);
        result.put("onlineRevenue", onlineRevenue);
        result.put("counterRevenue", counterRevenue);
        result.put("totalRevenue", totalRevenue);
        Map<String, Object> byPm = new LinkedHashMap<>();
        byPm.put("cash", cashRevenue);
        byPm.put("card", cardRevenue);
        byPm.put("upi", upiRevenue);
        result.put("revenueByPaymentMethod", byPm);
        result.put("codCollected", cod);
        result.put("tipsCollected", tips);
        result.put("grossInflow", grossInflow);
        // Supplier spend / refunds: not yet tracked at the entity level — left as 0
        // for honest accounting until those slices land.
        result.put("supplierSpend", BigDecimal.ZERO);
        result.put("refunds", BigDecimal.ZERO);
        result.put("net", grossInflow);
        return result;
    }

    public Map<String, Object> managementStaffPerformance(String fromStr, String toStr) {
        LocalDate[] range = parseDateRange(fromStr, toStr);
        LocalDateTime fromTs = range[0].atStartOfDay();
        LocalDateTime toTs   = range[1].plusDays(1).atStartOfDay();

        // Drivers — trip stats per driver
        List<DeliveryTrip> trips = repository.findTripsInRange(fromTs, toTs);
        Map<Integer, Map<String, Object>> driverStats = new LinkedHashMap<>();
        for (DeliveryTrip t : trips) {
            if (t.getDriver() == null) continue;
            int uid = t.getDriver().getuserid();
            Map<String, Object> row = driverStats.computeIfAbsent(uid, k -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("userId", uid);
                r.put("name", t.getDriver().getname());
                r.put("trips", 0L);
                r.put("delivered", 0L);
                r.put("failed", 0L);
                r.put("cod", BigDecimal.ZERO);
                r.put("tips", BigDecimal.ZERO);
                r.put("distanceKm", BigDecimal.ZERO);
                return r;
            });
            row.put("trips", ((Long) row.get("trips")) + 1L);
            String s = t.getStatus() == null ? "" : t.getStatus().toLowerCase();
            if ("delivered".equals(s)) row.put("delivered", ((Long) row.get("delivered")) + 1L);
            else if ("failed".equals(s)) row.put("failed", ((Long) row.get("failed")) + 1L);
            if (t.getCodAmount() != null && "delivered".equals(s)) {
                row.put("cod", ((BigDecimal) row.get("cod")).add(t.getCodAmount()));
            }
            if (t.getTipAmount() != null) {
                row.put("tips", ((BigDecimal) row.get("tips")).add(t.getTipAmount()));
            }
            if (t.getDistanceKm() != null) {
                row.put("distanceKm", ((BigDecimal) row.get("distanceKm")).add(t.getDistanceKm()));
            }
        }

        // Tasks completed per user, grouped by their department
        List<Task> doneTasks = repository.findTasksCompletedInRange(fromTs, toTs);
        Map<String, Map<Integer, Map<String, Object>>> staffByDept = new LinkedHashMap<>();
        for (Task t : doneTasks) {
            if (t.getCompletedBy() == null) continue;
            String dept = t.getAssignedToDepartment() == null
                    ? "unknown" : t.getAssignedToDepartment().toLowerCase();
            int uid = t.getCompletedBy().getuserid();
            staffByDept.computeIfAbsent(dept, k -> new LinkedHashMap<>());
            Map<String, Object> row = staffByDept.get(dept).computeIfAbsent(uid, k -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("userId", uid);
                r.put("name", t.getCompletedBy().getname());
                r.put("tasksCompleted", 0L);
                return r;
            });
            row.put("tasksCompleted", ((Long) row.get("tasksCompleted")) + 1L);
        }

        // Tasks created per sales user
        List<Task> createdTasks = repository.findTasksCreatedInRange(fromTs, toTs);
        Map<Integer, Map<String, Object>> salesStats = new LinkedHashMap<>();
        for (Task t : createdTasks) {
            if (t.getCreatedBy() == null) continue;
            int uid = t.getCreatedBy().getuserid();
            Map<String, Object> row = salesStats.computeIfAbsent(uid, k -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("userId", uid);
                r.put("name", t.getCreatedBy().getname());
                r.put("tasksCreated", 0L);
                return r;
            });
            row.put("tasksCreated", ((Long) row.get("tasksCreated")) + 1L);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", range[0].toString());
        result.put("to",   range[1].toString());
        result.put("drivers", new ArrayList<>(driverStats.values()));
        Map<String, List<Map<String, Object>>> deptOut = new LinkedHashMap<>();
        for (var e : staffByDept.entrySet()) {
            deptOut.put(e.getKey(), new ArrayList<>(e.getValue().values()));
        }
        result.put("staffByDepartment", deptOut);
        result.put("salesActivity", new ArrayList<>(salesStats.values()));
        return result;
    }

    // =========================================================================
    //  Refund / cancellation / damaged-goods write-off requests
    // =========================================================================

    private static final Set<String> ALLOWED_REFUND_TYPES =
            Set.of("refund", "cancellation", "damage_writeoff");
    private static final Set<String> ALLOWED_REFUND_DECISIONS =
            Set.of("approved", "rejected");

    public Map<String, Object> raiseRefundRequest(long orderId, int raisedById,
                                                   String typeRaw, String reason,
                                                   BigDecimal amount) {
        if (typeRaw == null || typeRaw.isBlank()) {
            throw new IllegalArgumentException("requestType is required");
        }
        String type = typeRaw.trim().toLowerCase();
        if (!ALLOWED_REFUND_TYPES.contains(type)) {
            throw new IllegalArgumentException(
                    "requestType must be one of: " + ALLOWED_REFUND_TYPES);
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("reason is required");
        }
        if (reason.length() > 500) {
            throw new IllegalArgumentException("reason must be at most 500 characters");
        }
        if (amount == null || amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be zero or positive");
        }
        Orders order = repository.findOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        User raisedBy = repository.findUserById(raisedById)
                .orElseThrow(() -> new IllegalArgumentException("Raiser user not found: " + raisedById));

        RefundRequest r = new RefundRequest();
        r.setOrder(order);
        r.setRaisedBy(raisedBy);
        r.setRequestType(type);
        r.setReason(reason.trim());
        r.setAmount(amount);
        r.setStatus("pending");
        r.setCreatedAt(LocalDateTime.now());
        return toRefundRequestMap(repository.saveRefundRequest(r));
    }

    public List<Map<String, Object>> listRefundRequests(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank() || "all".equalsIgnoreCase(statusFilter)) {
            return repository.findAllRefundRequests().stream()
                    .map(UserService::toRefundRequestMap)
                    .collect(Collectors.toList());
        }
        String s = statusFilter.trim().toLowerCase();
        if (!s.equals("pending") && !ALLOWED_REFUND_DECISIONS.contains(s)) {
            throw new IllegalArgumentException(
                    "status must be 'pending', 'approved', 'rejected' or 'all'");
        }
        return repository.findRefundRequestsByStatus(s).stream()
                .map(UserService::toRefundRequestMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> decideRefundRequest(long requestId, int managerUserId,
                                                    String decisionRaw, String notes) {
        if (decisionRaw == null || decisionRaw.isBlank()) {
            throw new IllegalArgumentException("decision is required");
        }
        String decision = decisionRaw.trim().toLowerCase();
        if (!ALLOWED_REFUND_DECISIONS.contains(decision)) {
            throw new IllegalArgumentException(
                    "decision must be one of: " + ALLOWED_REFUND_DECISIONS);
        }
        RefundRequest r = repository.findRefundRequestById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Refund request not found: " + requestId));
        if (!"pending".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalArgumentException(
                    "Refund request is already " + r.getStatus() + " and cannot be changed");
        }
        User manager = repository.findUserById(managerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found: " + managerUserId));

        r.setStatus(decision);
        r.setDecidedBy(manager);
        r.setDecisionNotes(notes == null || notes.isBlank() ? null : notes.trim());
        r.setDecidedAt(LocalDateTime.now());

        // If approved, mark the underlying order to reflect the outcome.
        if ("approved".equals(decision) && r.getOrder() != null) {
            String type = r.getRequestType() == null ? "" : r.getRequestType().toLowerCase();
            switch (type) {
                case "cancellation" -> {
                    r.getOrder().setStatus("cancelled");
                    r.getOrder().setKitchenStatus("cancelled");
                }
                case "refund", "damage_writeoff" -> {
                    r.getOrder().setStatus("refunded");
                }
                default -> { /* unknown type — no order mutation */ }
            }
            repository.saveOrder(r.getOrder());
        }
        return toRefundRequestMap(repository.saveRefundRequest(r));
    }

    // =========================================================================
    //  Cash reconciliation: counter sales vs COD collected vs deliveries
    // =========================================================================

    public Map<String, Object> cashReconciliation(String dateStr,
                                                   BigDecimal openingFloat,
                                                   BigDecimal countedCash) {
        LocalDate day;
        try {
            day = (dateStr == null || dateStr.isBlank())
                    ? LocalDate.now() : LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("date must be ISO format yyyy-MM-dd");
        }
        if (openingFloat != null && openingFloat.signum() < 0) {
            throw new IllegalArgumentException("openingFloat cannot be negative");
        }
        if (countedCash != null && countedCash.signum() < 0) {
            throw new IllegalArgumentException("countedCash cannot be negative");
        }
        LocalDateTime fromTs = day.atStartOfDay();
        LocalDateTime toTs   = day.plusDays(1).atStartOfDay();

        List<Orders> orders = repository.findOrdersInRange(fromTs, toTs);
        List<Long> ids = orders.stream().map(Orders::getId).collect(Collectors.toList());
        List<Payment> payments = repository.findPaymentsByOrderIds(ids);
        Map<Long, Payment> payByOrder = new HashMap<>();
        for (Payment p : payments) {
            if (p.getOrder() != null && p.getOrder().getId() != null) {
                payByOrder.put(p.getOrder().getId(), p);
            }
        }

        BigDecimal counterCash = BigDecimal.ZERO;
        BigDecimal counterCard = BigDecimal.ZERO;
        BigDecimal counterUpi  = BigDecimal.ZERO;
        long counterCashCount = 0;
        for (Orders o : orders) {
            String channel = o.getChannel() == null ? "" : o.getChannel().toLowerCase();
            if (!"instore".equals(channel) && !"pos".equals(channel)) continue;
            Payment p = payByOrder.get(o.getId());
            String method = p == null || p.getPaymentMethod() == null
                    ? "" : p.getPaymentMethod().toLowerCase();
            BigDecimal amt = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
            switch (method) {
                case "cash" -> {
                    counterCash = counterCash.add(amt);
                    counterCashCount++;
                }
                case "card" -> counterCard = counterCard.add(amt);
                case "upi"  -> counterUpi = counterUpi.add(amt);
                default -> { /* unknown */ }
            }
        }

        // Cash collected on delivery (COD).
        List<DeliveryTrip> trips = repository.findTripsInRange(fromTs, toTs);
        BigDecimal cod = trips.stream()
                .filter(t -> "delivered".equalsIgnoreCase(t.getStatus()) && t.getCodAmount() != null)
                .map(DeliveryTrip::getCodAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal opening = openingFloat == null ? BigDecimal.ZERO : openingFloat;
        BigDecimal expectedCash = opening.add(counterCash).add(cod);
        BigDecimal actual = countedCash;
        BigDecimal variance = actual == null ? null : actual.subtract(expectedCash);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("date", day.toString());
        result.put("openingFloat", opening);
        result.put("counterCash", counterCash);
        result.put("counterCashCount", counterCashCount);
        result.put("counterCard", counterCard);
        result.put("counterUpi", counterUpi);
        result.put("codCollected", cod);
        result.put("expectedCashInDrawer", expectedCash);
        result.put("countedCash", actual);
        result.put("variance", variance);
        result.put("balanced",
                actual != null && variance != null && variance.signum() == 0);
        return result;
    }

    // =========================================================================
    //  Discount / promo campaign approval
    // =========================================================================

    private static final Set<String> ALLOWED_CAMPAIGN_DECISIONS =
            Set.of("approved", "rejected");

    public Map<String, Object> proposeDiscountCampaign(int proposedById, String name,
                                                        String categoryFilter,
                                                        BigDecimal discountPercent,
                                                        String startsOnStr,
                                                        String endsOnStr) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (name.length() > 200) {
            throw new IllegalArgumentException("name must be at most 200 characters");
        }
        if (discountPercent == null) {
            throw new IllegalArgumentException("discountPercent is required");
        }
        if (discountPercent.signum() <= 0
                || discountPercent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("discountPercent must be between 0 and 100");
        }
        LocalDate startsOn;
        LocalDate endsOn;
        try {
            startsOn = (startsOnStr == null || startsOnStr.isBlank())
                    ? LocalDate.now() : LocalDate.parse(startsOnStr);
            endsOn = (endsOnStr == null || endsOnStr.isBlank())
                    ? startsOn : LocalDate.parse(endsOnStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Dates must be ISO format yyyy-MM-dd");
        }
        if (endsOn.isBefore(startsOn)) {
            throw new IllegalArgumentException("endsOn cannot be before startsOn");
        }
        User proposer = repository.findUserById(proposedById)
                .orElseThrow(() -> new IllegalArgumentException("Proposer not found: " + proposedById));

        DiscountCampaign d = new DiscountCampaign();
        d.setName(name.trim());
        d.setCategoryFilter(categoryFilter == null || categoryFilter.isBlank()
                ? null : categoryFilter.trim());
        d.setDiscountPercent(discountPercent);
        d.setStartsOn(startsOn);
        d.setEndsOn(endsOn);
        d.setStatus("pending");
        d.setProposedBy(proposer);
        d.setCreatedAt(LocalDateTime.now());
        return toCampaignMap(repository.saveDiscountCampaign(d));
    }

    public List<Map<String, Object>> listDiscountCampaigns(String statusFilter) {
        if (statusFilter == null || statusFilter.isBlank()
                || "all".equalsIgnoreCase(statusFilter)) {
            return repository.findAllDiscountCampaigns().stream()
                    .map(UserService::toCampaignMap)
                    .collect(Collectors.toList());
        }
        String s = statusFilter.trim().toLowerCase();
        if (!s.equals("pending") && !ALLOWED_CAMPAIGN_DECISIONS.contains(s)) {
            throw new IllegalArgumentException(
                    "status must be 'pending', 'approved', 'rejected' or 'all'");
        }
        return repository.findDiscountCampaignsByStatus(s).stream()
                .map(UserService::toCampaignMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> decideDiscountCampaign(long campaignId, int managerUserId,
                                                       String decisionRaw, String notes) {
        if (decisionRaw == null || decisionRaw.isBlank()) {
            throw new IllegalArgumentException("decision is required");
        }
        String decision = decisionRaw.trim().toLowerCase();
        if (!ALLOWED_CAMPAIGN_DECISIONS.contains(decision)) {
            throw new IllegalArgumentException(
                    "decision must be one of: " + ALLOWED_CAMPAIGN_DECISIONS);
        }
        DiscountCampaign d = repository.findDiscountCampaignById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));
        if (!"pending".equalsIgnoreCase(d.getStatus())) {
            throw new IllegalArgumentException(
                    "Campaign is already " + d.getStatus() + " and cannot be changed");
        }
        User manager = repository.findUserById(managerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found: " + managerUserId));

        d.setStatus(decision);
        d.setDecidedBy(manager);
        d.setDecisionNotes(notes == null || notes.isBlank() ? null : notes.trim());
        d.setDecidedAt(LocalDateTime.now());
        return toCampaignMap(repository.saveDiscountCampaign(d));
    }

    private static Map<String, Object> toCampaignMap(DiscountCampaign d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("name", d.getName());
        m.put("categoryFilter", d.getCategoryFilter());
        m.put("discountPercent", d.getDiscountPercent());
        m.put("startsOn", d.getStartsOn() == null ? null : d.getStartsOn().toString());
        m.put("endsOn", d.getEndsOn() == null ? null : d.getEndsOn().toString());
        m.put("status", d.getStatus());
        m.put("proposedById",
                d.getProposedBy() == null ? null : d.getProposedBy().getuserid());
        m.put("proposedByName",
                d.getProposedBy() == null ? null : d.getProposedBy().getname());
        m.put("decidedById",
                d.getDecidedBy() == null ? null : d.getDecidedBy().getuserid());
        m.put("decidedByName",
                d.getDecidedBy() == null ? null : d.getDecidedBy().getname());
        m.put("decisionNotes", d.getDecisionNotes());
        m.put("createdAt",
                d.getCreatedAt() == null ? null : d.getCreatedAt().toString());
        m.put("decidedAt",
                d.getDecidedAt() == null ? null : d.getDecidedAt().toString());
        return m;
    }

    // =========================================================================
    //  Corporate / catering order sign-off
    // =========================================================================

    private static final Set<String> ALLOWED_APPROVAL_DECISIONS = Set.of("approved", "rejected");

    public Map<String, Object> flagOrderForApproval(long orderId, String notes) {
        Orders order = repository.findOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setRequiresApproval(true);
        if (order.getApprovalStatus() == null
                || "pending".equalsIgnoreCase(order.getApprovalStatus())) {
            order.setApprovalStatus("pending");
        } else {
            throw new IllegalArgumentException(
                    "Order already has decision: " + order.getApprovalStatus());
        }
        if (notes != null && !notes.isBlank()) {
            order.setApprovalNotes(notes.trim());
        }
        // Until approved, kitchen should not start.
        if (order.getKitchenStatus() == null
                || "pending".equalsIgnoreCase(order.getKitchenStatus())) {
            order.setKitchenStatus("awaiting_approval");
        }
        return toApprovalMap(repository.saveOrder(order));
    }

    public List<Map<String, Object>> listOrdersPendingApproval() {
        return repository.findOrdersPendingApproval().stream()
                .map(UserService::toApprovalMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> decideOrderApproval(long orderId, int managerUserId,
                                                    String decisionRaw, String notes) {
        if (decisionRaw == null || decisionRaw.isBlank()) {
            throw new IllegalArgumentException("decision is required");
        }
        String decision = decisionRaw.trim().toLowerCase();
        if (!ALLOWED_APPROVAL_DECISIONS.contains(decision)) {
            throw new IllegalArgumentException(
                    "decision must be one of: " + ALLOWED_APPROVAL_DECISIONS);
        }
        Orders order = repository.findOrderById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getRequiresApproval() == null || !order.getRequiresApproval()) {
            throw new IllegalArgumentException("Order is not flagged for approval");
        }
        if (order.getApprovalStatus() != null
                && !"pending".equalsIgnoreCase(order.getApprovalStatus())) {
            throw new IllegalArgumentException(
                    "Order is already " + order.getApprovalStatus());
        }
        User manager = repository.findUserById(managerUserId)
                .orElseThrow(() -> new IllegalArgumentException("Manager not found: " + managerUserId));

        order.setApprovalStatus(decision);
        order.setApprovedBy(manager);
        order.setApprovedAt(LocalDateTime.now());
        if (notes != null && !notes.isBlank()) {
            order.setApprovalNotes(notes.trim());
        }
        if ("approved".equals(decision)) {
            // Release the order to the kitchen.
            order.setKitchenStatus("pending");
        } else {
            order.setStatus("cancelled");
            order.setKitchenStatus("cancelled");
        }
        return toApprovalMap(repository.saveOrder(order));
    }

    private static Map<String, Object> toApprovalMap(Orders o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("orderId", o.getId());
        m.put("totalAmount", o.getTotalAmount());
        m.put("channel", o.getChannel());
        m.put("status", o.getStatus());
        m.put("kitchenStatus", o.getKitchenStatus());
        m.put("customerName", o.getUser() == null ? null : o.getUser().getname());
        m.put("requiresApproval", o.getRequiresApproval());
        m.put("approvalStatus", o.getApprovalStatus());
        m.put("approvalNotes", o.getApprovalNotes());
        m.put("approvedById",
                o.getApprovedBy() == null ? null : o.getApprovedBy().getuserid());
        m.put("approvedByName",
                o.getApprovedBy() == null ? null : o.getApprovedBy().getname());
        m.put("approvedAt",
                o.getApprovedAt() == null ? null : o.getApprovedAt().toString());
        m.put("createdAt",
                o.getCreatedAt() == null ? null : o.getCreatedAt().toString());
        return m;
    }

    private static Map<String, Object> toRefundRequestMap(RefundRequest r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("orderId", r.getOrder() == null ? null : r.getOrder().getId());
        m.put("orderTotal",
                r.getOrder() == null ? null : r.getOrder().getTotalAmount());
        m.put("customerName",
                (r.getOrder() == null || r.getOrder().getUser() == null)
                        ? null : r.getOrder().getUser().getname());
        m.put("requestType", r.getRequestType());
        m.put("reason", r.getReason());
        m.put("amount", r.getAmount());
        m.put("status", r.getStatus());
        m.put("raisedById",
                r.getRaisedBy() == null ? null : r.getRaisedBy().getuserid());
        m.put("raisedByName",
                r.getRaisedBy() == null ? null : r.getRaisedBy().getname());
        m.put("decidedById",
                r.getDecidedBy() == null ? null : r.getDecidedBy().getuserid());
        m.put("decidedByName",
                r.getDecidedBy() == null ? null : r.getDecidedBy().getname());
        m.put("decisionNotes", r.getDecisionNotes());
        m.put("createdAt",
                r.getCreatedAt() == null ? null : r.getCreatedAt().toString());
        m.put("decidedAt",
                r.getDecidedAt() == null ? null : r.getDecidedAt().toString());
        return m;
    }

    private static LocalDate[] parseDateRange(String fromStr, String toStr) {
        LocalDate from;
        LocalDate to;
        try {
            from = (fromStr == null || fromStr.isBlank())
                    ? LocalDate.now() : LocalDate.parse(fromStr);
            to = (toStr == null || toStr.isBlank()) ? from : LocalDate.parse(toStr);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Dates must be ISO format yyyy-MM-dd");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("'from' must be on or before 'to'");
        }
        return new LocalDate[]{from, to};
    }

    private static List<Map<String, Object>> dailyTrend(List<Orders> orders, LocalDate from, LocalDate to) {
        Map<LocalDate, BigDecimal> revByDay = new LinkedHashMap<>();
        Map<LocalDate, Integer> countByDay = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            revByDay.put(d, BigDecimal.ZERO);
            countByDay.put(d, 0);
        }
        for (Orders o : orders) {
            if (o.getCreatedAt() == null) continue;
            LocalDate d = o.getCreatedAt().toLocalDate();
            if (!revByDay.containsKey(d)) continue;
            BigDecimal amt = o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount();
            revByDay.merge(d, amt, BigDecimal::add);
            countByDay.merge(d, 1, Integer::sum);
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (LocalDate d : revByDay.keySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", d.toString());
            row.put("revenue", revByDay.get(d));
            row.put("orderCount", countByDay.get(d));
            rows.add(row);
        }
        return rows;
    }
}
