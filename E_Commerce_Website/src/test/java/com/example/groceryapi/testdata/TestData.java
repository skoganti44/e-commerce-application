package com.example.groceryapi.testdata;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.groceryapi.model.Cart;
import com.example.groceryapi.model.CartItem;
import com.example.groceryapi.model.Category;
import com.example.groceryapi.model.OrderItem;
import com.example.groceryapi.model.Orders;
import com.example.groceryapi.model.Payment;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.model.Users;

public final class TestData {

    private TestData() {}

    public static final LocalDateTime JOHN_CREATED_AT = LocalDateTime.of(2025, 1, 1, 10, 0);
    public static final LocalDateTime JANE_CREATED_AT = LocalDateTime.of(2025, 2, 1, 12, 0);
    public static final LocalDateTime MARCH_CREATED_AT = LocalDateTime.of(2025, 3, 15, 9, 30);

    public static final String FRESH_PRODUCTS = "Fresh Products";
    public static final String FRESH_ORGANIC_PRODUCTS = "Fresh Organic Products";
    public static final String IT = "IT";
    public static final String SALES = "Sales";

    public static Users john() {
        return user(1, "John", "john@example.com", "pass123", JOHN_CREATED_AT);
    }

    public static Users jane() {
        return user(2, "Jane", "jane@example.com", "pass456", JANE_CREATED_AT);
    }

    public static Users johnMarch() {
        return user(1, "John", "john@example.com", "pass123", MARCH_CREATED_AT);
    }

    public static Users newJohn() {
        return user(0, "John", "john@example.com", "pass123", LocalDateTime.now());
    }

    public static Users newJane() {
        return user(0, "Jane", "jane@example.com", "pass456", LocalDateTime.now());
    }

    public static Users user(int id, String name, String email, String password, LocalDateTime createdAt) {
        Users u = new Users();
        u.setuserid(id);
        u.setname(name);
        u.setemail(email);
        u.setpassword(password);
        u.setcreatedat(createdAt);
        return u;
    }

    public static Role alice() {
        return role(1, "Alice Smith", "Manager", SALES);
    }

    public static Role bob() {
        return role(2, "Bob Johnson", "Engineer", IT);
    }

    public static Role joeJonnas() {
        return role(3, "Joe Jonnas", "Manager", FRESH_PRODUCTS);
    }

    public static Role steveWooten() {
        return role(4, "Steve Wooten", "sales Manager", FRESH_PRODUCTS);
    }

    public static Role carol() {
        return role(5, "Carol White", "Lead", FRESH_PRODUCTS);
    }

    public static Role role(int id, String fullName, String roleName, String department) {
        Role r = new Role();
        r.setId(id);
        r.setFullName(fullName);
        r.setRole(roleName);
        r.setDepartment(department);
        return r;
    }

    public static List<Users> users() {
        return List.of(john(), jane());
    }

    public static List<Role> rolesInFreshProducts() {
        return List.of(joeJonnas(), steveWooten());
    }

    public static UserRole userRole(int id, Users user, Role role) {
        UserRole ur = new UserRole();
        ur.setUserroleid(id);
        ur.setUser(user);
        ur.setRole(role);
        return ur;
    }

    public static UserRole johnAsManager() {
        return userRole(1, john(), joeJonnas());
    }

    public static UserRole janeAsEngineer() {
        return userRole(2, jane(), bob());
    }

    public static List<UserRole> userRoles() {
        return List.of(johnAsManager(), janeAsEngineer());
    }

    public static Category category(Long id, String name, String description) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setDescription(description);
        return c;
    }

    public static Product product(Long id, String name, BigDecimal price, Integer stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setDescription(name + " description");
        p.setPrice(price);
        p.setStock(stock);
        return p;
    }

    public static Product apple() {
        return product(1L, "Apple", new BigDecimal("2.00"), 50);
    }

    public static Product milk() {
        return product(3L, "Milk", new BigDecimal("5.26"), 30);
    }

    public static Product bread() {
        return product(5L, "Bread", new BigDecimal("1.00"), 100);
    }

    public static Cart cart(Long id, Users user, LocalDateTime createdAt) {
        Cart c = new Cart();
        c.setId(id);
        c.setUser(user);
        c.setCreatedAt(createdAt);
        return c;
    }

    public static Cart johnsCart() {
        return cart(1L, john(), JOHN_CREATED_AT);
    }

    public static Cart newCart(Users user) {
        Cart c = new Cart();
        c.setUser(user);
        return c;
    }

    public static CartItem cartItem(Long id, Cart cart, Product product, Integer quantity) {
        CartItem ci = new CartItem();
        ci.setId(id);
        ci.setCart(cart);
        ci.setProduct(product);
        ci.setQuantity(quantity);
        return ci;
    }

    public static CartItem newCartItem(Cart cart, Product product, Integer quantity) {
        CartItem ci = new CartItem();
        ci.setCart(cart);
        ci.setProduct(product);
        ci.setQuantity(quantity);
        return ci;
    }

    public static List<CartItem> johnsCartItems() {
        Cart cart = johnsCart();
        return List.of(
                cartItem(1L, cart, apple(), 3),
                cartItem(2L, cart, milk(), 6),
                cartItem(3L, cart, bread(), 10));
    }

    public static Role customerRole() {
        return role(10, "Customer One", "customer", SALES);
    }

    public static Role managerRole() {
        return role(11, "Manager One", "Manager", SALES);
    }

    public static Orders order(Long id, Users user, BigDecimal totalAmount, String status) {
        Orders o = new Orders();
        o.setId(id);
        o.setUser(user);
        o.setTotalAmount(totalAmount);
        o.setStatus(status);
        return o;
    }

    public static Orders newOrder(Users user, BigDecimal totalAmount, String status) {
        Orders o = new Orders();
        o.setUser(user);
        o.setTotalAmount(totalAmount);
        o.setStatus(status);
        return o;
    }

    public static Orders johnsOrder() {
        return order(1L, john(), new BigDecimal("1049.99"), "PLACED");
    }

    public static OrderItem orderItem(Long id, Orders order, Product product, Integer quantity, BigDecimal price) {
        OrderItem oi = new OrderItem();
        oi.setId(id);
        oi.setOrder(order);
        oi.setProduct(product);
        oi.setQuantity(quantity);
        oi.setPrice(price);
        return oi;
    }

    public static OrderItem newOrderItem(Orders order, Product product, Integer quantity, BigDecimal price) {
        OrderItem oi = new OrderItem();
        oi.setOrder(order);
        oi.setProduct(product);
        oi.setQuantity(quantity);
        oi.setPrice(price);
        return oi;
    }

    public static List<OrderItem> johnsOrderItems() {
        Orders order = johnsOrder();
        return List.of(
                orderItem(1L, order, apple(), 2, new BigDecimal("2.00")),
                orderItem(2L, order, milk(), 6, new BigDecimal("5.26")),
                orderItem(3L, order, bread(), 4, new BigDecimal("1.00")));
    }

    public static Payment payment(Long id, Orders order, String method, String status, BigDecimal amount) {
        Payment p = new Payment();
        p.setId(id);
        p.setOrder(order);
        p.setPaymentMethod(method);
        p.setStatus(status);
        p.setAmount(amount);
        return p;
    }

    public static Payment newPayment(Orders order, String method, String status, BigDecimal amount) {
        Payment p = new Payment();
        p.setOrder(order);
        p.setPaymentMethod(method);
        p.setStatus(status);
        p.setAmount(amount);
        return p;
    }

    public static List<Payment> johnsPayments() {
        Orders order = johnsOrder();
        return List.of(
                payment(1L, order, "CREDIT_CARD", "Decline", new BigDecimal("28.26")),
                payment(2L, order, "CREDIT_CARD", "SUCCESS", new BigDecimal("28.26")));
    }
}
