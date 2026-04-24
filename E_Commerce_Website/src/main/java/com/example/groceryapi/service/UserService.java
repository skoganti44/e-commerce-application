//A Service is the business logic layer.
//It sits between the Controller and the Repository. Think of it as a manager
// — the controller (receptionist) takes the request, passes it to the service (manager), who decides what to do and asks the repository (database clerk) for data
package com.example.groceryapi.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.example.groceryapi.model.OrderItem;
import com.example.groceryapi.model.Orders;
import com.example.groceryapi.model.Payment;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.ProductAvailable;
import com.example.groceryapi.model.ProductImage;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.ShippingAddress;
import com.example.groceryapi.model.Supply;
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
        if (requestedQty == null || requestedQty.signum() <= 0) {
            throw new IllegalArgumentException("requestedQty must be positive");
        }
        String status = urgency == null ? "waiting" : urgency.trim().toLowerCase();
        if (!ALLOWED_SUPPLY_ORDER_STATUSES.contains(status) || "received".equals(status)) {
            status = "waiting";
        }
        Supply s = repository.findSupplyById(supplyId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Supply not found: " + supplyId));
        s.setRequestedQty(requestedQty);
        s.setOrderStatus(status);
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
}
