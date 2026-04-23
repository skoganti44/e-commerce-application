//A Service is the business logic layer.
//It sits between the Controller and the Repository. Think of it as a manager
// — the controller (receptionist) takes the request, passes it to the service (manager), who decides what to do and asks the repository (database clerk) for data
package com.example.groceryapi.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.example.groceryapi.model.ShippingAddress;
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
        String rawType = userType == null ? "" : userType.trim().toLowerCase();
        String normalizedType = rawType.equals("customer") ? "customer" : "employee";
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
        Users owner = itemCart == null ? null : itemCart.getUser();
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
        Users user = repository.findUserById(userid)
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
        Users user = repository.findUserById(userid)
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
        order = repository.saveOrder(order);

        for (CartItem ci : cartItems) {
            BigDecimal unit = computeUnitPrice(ci.getProduct(), ci.getSweetenerType(), ci.getFlourType());
            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(ci.getProduct());
            oi.setQuantity(ci.getQuantity());
            oi.setPrice(unit);
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
                .map(userService::normalizeCode)
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
}
