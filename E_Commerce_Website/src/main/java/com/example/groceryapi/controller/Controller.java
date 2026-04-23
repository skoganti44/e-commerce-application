//A Controller is the entry point of your API. It receives HTTP requests from clients (Postman, browser, frontend) and returns responses.
package com.example.groceryapi.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.groceryapi.model.Payment;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.ShippingAddress;
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.model.Users;
import com.example.groceryapi.service.userService;

@RestController
public class Controller {
    @Autowired
    private userService userService;

    @GetMapping(value = "/users", headers = "Accept=application/json")
    public List<Users> fetchUsers() {
        return userService.fetchUsers();
    }

    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            Users saved = userService.register(
                    body.get("name"),
                    body.get("email"),
                    body.get("password"),
                    body.get("userType"));
            return ResponseEntity.ok(toPublicUser(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/login", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            Users user = userService.login(body.get("email"), body.get("password"));
            return ResponseEntity.ok(toPublicUser(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toPublicUser(Users u) {
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("userid", u.getuserid());
        out.put("name", u.getname());
        out.put("email", u.getemail());
        out.put("createdat", u.getcreatedat() == null ? "" : u.getcreatedat().toString());
        out.put("roles", userService.fetchRoleNamesForUser(u.getuserid()));
        return out;
    }

    @GetMapping(value = "/roles", headers = "Accept=application/json")
    public List<Role> fetchRoles(@RequestParam(required = false) String department) {
        return userService.fetchRoles(department);
    }

    @GetMapping(value = "/userRoles", headers = "Accept=application/json")
    public List<UserRole> fetchUserRoles(
            @RequestParam(required = false) Integer userid,
            @RequestParam(required = false) Integer roleid) {
        return userService.fetchUserRoles(userid, roleid);
    }

    @GetMapping(value = "/cart", headers = "Accept=application/json")
    public ResponseEntity<Map<String, Object>> fetchCart(@RequestParam int userid) {
        try {
            return ResponseEntity.ok(userService.fetchCartByUserId(userid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/cart/add", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> addToCart(@RequestBody Map<String, Object> body) {
        try {
            int userid = ((Number) body.get("userid")).intValue();
            long productId = ((Number) body.get("productId")).longValue();
            int quantity = body.get("quantity") == null ? 1 : ((Number) body.get("quantity")).intValue();
            String sweetenerType = (String) body.get("sweetenerType");
            Integer sweetenerPercent = body.get("sweetenerPercent") == null
                    ? null
                    : ((Number) body.get("sweetenerPercent")).intValue();
            String flourType = (String) body.get("flourType");
            userService.addToCart(userid, productId, quantity,
                    sweetenerType, sweetenerPercent, flourType);
            return ResponseEntity.ok(userService.fetchCartByUserId(userid));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @PostMapping(value = "/cart/item/update", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> updateCartItem(@RequestBody Map<String, Object> body) {
        try {
            int userid = ((Number) body.get("userid")).intValue();
            Long cartItemId = body.get("cartItemId") == null
                    ? null : ((Number) body.get("cartItemId")).longValue();
            int quantity = body.get("quantity") == null ? 0 : ((Number) body.get("quantity")).intValue();
            userService.updateCartItemQuantity(userid, cartItemId, quantity);
            return ResponseEntity.ok(userService.fetchCartByUserId(userid));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @DeleteMapping(value = "/cart/item", produces = "application/json")
    public ResponseEntity<?> removeCartItem(@RequestParam int userid,
                                            @RequestParam("itemId") Long itemId) {
        try {
            userService.removeCartItem(userid, itemId);
            return ResponseEntity.ok(userService.fetchCartByUserId(userid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/orders", headers = "Accept=application/json")
    public ResponseEntity<Map<String, Object>> fetchOrders(@RequestParam int userid) {
        try {
            return ResponseEntity.ok(userService.fetchOrdersForCustomer(userid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/payments", headers = "Accept=application/json")
    public ResponseEntity<?> fetchPayments(
            @RequestParam int userid,
            @RequestParam(required = false, defaultValue = "false") boolean includeAll) {
        try {
            List<Payment> payments = userService.fetchPaymentsByUserId(userid, includeAll);
            return ResponseEntity.ok(payments);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/products", headers = "Accept=application/json")
    public List<Map<String, Object>> fetchProducts() {
        return userService.fetchAllProducts();
    }

    @PostMapping(value = "/product", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> saveProduct(@RequestBody Map<String, Object> request) {
        try {
            return ResponseEntity.ok(userService.saveProduct(request));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/products", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> saveProducts(@RequestBody Map<String, Object> request) {
        try {
            return ResponseEntity.ok(userService.saveProducts(request));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping(value = "/cleanup", produces = "application/json")
    public ResponseEntity<Map<String, Object>> cleanup(@RequestParam int userid) {
        try {
            return ResponseEntity.ok(userService.cleanupForUser(userid));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/shipping-address", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> saveShippingAddress(@RequestBody Map<String, Object> body) {
        try {
            int userid = ((Number) body.get("userid")).intValue();
            @SuppressWarnings("unchecked")
            Map<String, Object> addressBody = (Map<String, Object>) body.get("address");
            ShippingAddress saved = userService.saveShippingAddress(userid, addressBody);
            return ResponseEntity.ok(toPublicAddress(saved));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @GetMapping(value = "/shipping-address", headers = "Accept=application/json")
    public ResponseEntity<?> getLatestShippingAddress(@RequestParam int userid) {
        try {
            ShippingAddress a = userService.findLatestShippingAddress(userid);
            if (a == null) {
                return ResponseEntity.ok(Map.of());
            }
            return ResponseEntity.ok(toPublicAddress(a));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/checkout", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> checkout(@RequestBody Map<String, Object> body) {
        try {
            int userid = ((Number) body.get("userid")).intValue();
            String paymentMethod = (String) body.get("paymentMethod");
            @SuppressWarnings("unchecked")
            Map<String, Object> addressBody = (Map<String, Object>) body.get("address");
            String cardLast4 = (String) body.get("cardLast4");
            Map<String, Object> result = userService.checkout(userid, paymentMethod, addressBody, cardLast4);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    private Map<String, Object> toPublicAddress(ShippingAddress a) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("fullName", a.getFullName());
        m.put("phone", a.getPhone());
        m.put("line1", a.getLine1());
        m.put("line2", a.getLine2());
        m.put("landmark", a.getLandmark());
        m.put("city", a.getCity());
        m.put("state", a.getState());
        m.put("pincode", a.getPincode());
        m.put("country", a.getCountry());
        m.put("instructions", a.getInstructions());
        m.put("addressType", a.getAddressType());
        m.put("orderId", a.getOrder() == null ? null : a.getOrder().getId());
        m.put("createdAt", a.getCreatedAt() == null ? null : a.getCreatedAt().toString());
        return m;
    }
}
