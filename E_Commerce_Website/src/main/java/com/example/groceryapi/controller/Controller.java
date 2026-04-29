//A Controller is the entry point of your API. It receives HTTP requests from clients (Postman, browser, frontend) and returns responses.
package com.example.groceryapi.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.groceryapi.model.Payment;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.ShippingAddress;
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.model.User;
import com.example.groceryapi.service.UserService;

@RestController
public class Controller {
    @Autowired
    private UserService userService;

    @GetMapping(value = "/users", headers = "Accept=application/json")
    public List<User> fetchUsers() {
        return userService.fetchUsers();
    }

    @PostMapping(value = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            User saved = userService.register(
                    body.get("name"),
                    body.get("email"),
                    body.get("password"),
                    body.get("userType"),
                    body.get("department"));
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
            User user = userService.login(body.get("email"), body.get("password"));
            return ResponseEntity.ok(toPublicUser(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toPublicUser(User u) {
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("userid", u.getuserid());
        out.put("name", u.getname());
        out.put("email", u.getemail());
        out.put("createdat", u.getcreatedat() == null ? "" : u.getcreatedat().toString());
        out.put("roles", userService.fetchRoleNamesForUser(u.getuserid()));
        out.put("departments", userService.fetchDepartmentsForUser(u.getuserid()));
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

    @GetMapping(value = "/kitchen/online-orders", headers = "Accept=application/json")
    public ResponseEntity<List<Map<String, Object>>> fetchKitchenOnlineOrders() {
        return ResponseEntity.ok(userService.fetchKitchenOrders("online"));
    }

    @GetMapping(value = "/kitchen/instore-orders", headers = "Accept=application/json")
    public ResponseEntity<List<Map<String, Object>>> fetchKitchenInStoreOrders() {
        return ResponseEntity.ok(userService.fetchKitchenOrders("instore"));
    }

    @GetMapping(value = "/kitchen/daily-stock", headers = "Accept=application/json")
    public ResponseEntity<List<Map<String, Object>>> fetchDailyStock() {
        return ResponseEntity.ok(userService.fetchDailyStock(null));
    }

    @GetMapping(value = "/delivery/online-orders", headers = "Accept=application/json")
    public ResponseEntity<List<Map<String, Object>>> fetchDeliveryOnlineOrders() {
        return ResponseEntity.ok(
                userService.fetchKitchenOrders("online", List.of("done")));
    }

    @GetMapping(value = "/bakery/instore-orders", headers = "Accept=application/json")
    public ResponseEntity<List<Map<String, Object>>> fetchBakeryInStoreOrders() {
        return ResponseEntity.ok(
                userService.fetchKitchenOrders("instore", List.of("done")));
    }

    @PostMapping(value = "/kitchen/daily-stock/{stockId}/adjust",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> adjustDailyStockPrepared(
            @PathVariable("stockId") long stockId,
            @RequestBody Map<String, Object> body) {
        try {
            int delta = body.get("delta") == null
                    ? 0 : ((Number) body.get("delta")).intValue();
            return ResponseEntity.ok(
                    userService.adjustDailyStockPrepared(stockId, delta));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @GetMapping(value = "/kitchen/supplies", headers = "Accept=application/json")
    public ResponseEntity<List<Map<String, Object>>> fetchSupplies() {
        return ResponseEntity.ok(userService.fetchSupplies());
    }

    @GetMapping(value = "/kitchen/in-stock", headers = "Accept=application/json")
    public ResponseEntity<List<Map<String, Object>>> fetchInStockSupplies() {
        return ResponseEntity.ok(userService.fetchInStockSupplies());
    }

    @PostMapping(value = "/kitchen/supplies/{supplyId}/request",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> requestMoreSupply(
            @PathVariable("supplyId") long supplyId,
            @RequestBody Map<String, Object> body) {
        try {
            Object rawQty = body.get("requestedQty");
            BigDecimal qty;
            if (rawQty instanceof Number n) {
                qty = new BigDecimal(n.toString());
            } else if (rawQty != null) {
                qty = new BigDecimal(rawQty.toString().trim());
            } else {
                throw new IllegalArgumentException("requestedQty is required");
            }
            String urgency = (String) body.get("urgency");
            return ResponseEntity.ok(
                    userService.requestMoreSupply(supplyId, qty, urgency));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @PostMapping(value = "/kitchen/supplies",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> saveSupply(@RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(userService.saveSupply(body));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @PostMapping(value = "/kitchen/supplies/{supplyId}/adjust",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> adjustSupplyStock(
            @PathVariable("supplyId") long supplyId,
            @RequestBody Map<String, Object> body) {
        try {
            Object rawDelta = body.get("delta");
            BigDecimal delta;
            if (rawDelta instanceof Number n) {
                delta = new BigDecimal(n.toString());
            } else if (rawDelta != null) {
                delta = new BigDecimal(rawDelta.toString().trim());
            } else {
                throw new IllegalArgumentException("delta is required");
            }
            String note = (String) body.get("note");
            return ResponseEntity.ok(userService.adjustSupplyStock(supplyId, delta, note));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @PostMapping(value = "/kitchen/supplies/bulk-status",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> bulkUpdateSupplyStatuses(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> updates =
                    (List<Map<String, Object>>) body.get("updates");
            return ResponseEntity.ok(userService.updateSupplyOrderStatuses(updates));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @GetMapping(value = "/management/supply-requests",
            headers = "Accept=application/json")
    public ResponseEntity<List<Map<String, Object>>> fetchSupplyRequests() {
        return ResponseEntity.ok(userService.fetchRequestedSupplies());
    }

    @PostMapping(value = "/management/supplies/{supplyId}/fulfill",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> fulfillSupply(
            @PathVariable("supplyId") long supplyId,
            @RequestBody Map<String, Object> body) {
        try {
            Object rawQty = body.get("receivedQty");
            BigDecimal qty;
            if (rawQty instanceof Number n) {
                qty = new BigDecimal(n.toString());
            } else if (rawQty != null) {
                qty = new BigDecimal(rawQty.toString().trim());
            } else {
                throw new IllegalArgumentException("receivedQty is required");
            }
            String note = (String) body.get("note");
            return ResponseEntity.ok(userService.fulfillSupply(supplyId, qty, note));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @PostMapping(value = "/kitchen/supplies/seed", produces = "application/json")
    public ResponseEntity<?> seedSupplies() {
        try {
            return ResponseEntity.ok(userService.seedSuppliesIfEmpty());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/kitchen/order/{orderId}/status",
            consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> updateKitchenOrderStatus(
            @PathVariable("orderId") long orderId,
            @RequestBody Map<String, Object> body) {
        try {
            String status = (String) body.get("kitchenStatus");
            String reason = (String) body.get("reason");
            return ResponseEntity.ok(
                    userService.updateKitchenOrderStatus(orderId, status, reason));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
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
            String customerNotes = (String) body.get("customerNotes");
            Map<String, Object> result = userService.checkout(
                    userid, paymentMethod, addressBody, cardLast4, customerNotes);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @PostMapping(value = "/counter/sale", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> recordCounterSale(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
            String paymentMethod = (String) body.get("paymentMethod");
            Object cashRaw = body.get("cashGiven");
            java.math.BigDecimal cashGiven = null;
            if (cashRaw instanceof Number) {
                cashGiven = java.math.BigDecimal.valueOf(((Number) cashRaw).doubleValue());
            } else if (cashRaw instanceof String && !((String) cashRaw).isBlank()) {
                cashGiven = new java.math.BigDecimal((String) cashRaw);
            }
            String customerName = (String) body.get("customerName");
            String customerNotes = (String) body.get("customerNotes");
            Map<String, Object> result = userService.recordCounterSale(
                    items, paymentMethod, cashGiven, customerName, customerNotes);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @GetMapping(value = "/sales/analytics", produces = "application/json")
    public ResponseEntity<?> salesAnalytics(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            return ResponseEntity.ok(userService.salesAnalytics(from, to));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/tasks", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> createTask(@RequestBody Map<String, Object> body) {
        try {
            int createdById = ((Number) body.get("createdByUserId")).intValue();
            Integer assignedUserId = body.get("assignedToUserId") == null
                    ? null : ((Number) body.get("assignedToUserId")).intValue();
            Long relatedOrderId = body.get("relatedOrderId") == null
                    ? null : ((Number) body.get("relatedOrderId")).longValue();
            Map<String, Object> created = userService.createTask(
                    createdById,
                    (String) body.get("assignedToDepartment"),
                    assignedUserId,
                    (String) body.get("title"),
                    (String) body.get("description"),
                    (String) body.get("priority"),
                    (String) body.get("dueDate"),
                    relatedOrderId);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid task payload" : e.getMessage()));
        }
    }

    @GetMapping(value = "/tasks", produces = "application/json")
    public ResponseEntity<?> listTasks(
            @RequestParam(required = false) String department,
            @RequestParam(required = false) Integer createdByUserId,
            @RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(userService.listTasks(department, createdByUserId, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/tasks/{taskId}/status", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> updateTaskStatus(@PathVariable long taskId, @RequestBody Map<String, Object> body) {
        try {
            Integer actingUserId = body.get("actingUserId") == null
                    ? null : ((Number) body.get("actingUserId")).intValue();
            Map<String, Object> updated = userService.updateTaskStatus(
                    taskId,
                    (String) body.get("status"),
                    actingUserId,
                    (String) body.get("resolutionNotes"));
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid status payload" : e.getMessage()));
        }
    }

    @PostMapping(value = "/delivery/trips", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> pickUpTrip(@RequestBody Map<String, Object> body) {
        try {
            long orderId = ((Number) body.get("orderId")).longValue();
            int driverId = ((Number) body.get("driverId")).intValue();
            return ResponseEntity.ok(userService.pickUpTrip(orderId, driverId));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid pickup payload" : e.getMessage()));
        }
    }

    @PostMapping(value = "/delivery/trips/{tripId}/out", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> markOutForDelivery(@PathVariable long tripId, @RequestBody Map<String, Object> body) {
        try {
            int driverId = ((Number) body.get("driverId")).intValue();
            return ResponseEntity.ok(userService.markOutForDelivery(tripId, driverId));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @PostMapping(value = "/delivery/trips/{tripId}/deliver", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> markDelivered(@PathVariable long tripId, @RequestBody Map<String, Object> body) {
        try {
            int driverId = ((Number) body.get("driverId")).intValue();
            String otp = (String) body.get("otp");
            String photoUrl = (String) body.get("photoUrl");
            String notes = (String) body.get("notes");
            BigDecimal cod = toBigDecimal(body.get("codAmount"));
            BigDecimal tip = toBigDecimal(body.get("tipAmount"));
            BigDecimal dist = toBigDecimal(body.get("distanceKm"));
            return ResponseEntity.ok(
                    userService.markDelivered(tripId, driverId, otp, photoUrl, cod, tip, dist, notes));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid delivery payload" : e.getMessage()));
        }
    }

    @PostMapping(value = "/delivery/trips/{tripId}/fail", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> markTripFailed(@PathVariable long tripId, @RequestBody Map<String, Object> body) {
        try {
            int driverId = ((Number) body.get("driverId")).intValue();
            String reason = (String) body.get("reason");
            String notes = (String) body.get("notes");
            return ResponseEntity.ok(userService.markTripFailed(tripId, driverId, reason, notes));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @GetMapping(value = "/delivery/trips", produces = "application/json")
    public ResponseEntity<?> listTrips(
            @RequestParam int driverId,
            @RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(userService.listTripsForDriver(driverId, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/delivery/issues", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> logIssue(@RequestBody Map<String, Object> body) {
        try {
            int driverId = ((Number) body.get("driverId")).intValue();
            String issueType = (String) body.get("issueType");
            String description = (String) body.get("description");
            Long tripId = body.get("tripId") == null ? null : ((Number) body.get("tripId")).longValue();
            return ResponseEntity.ok(userService.logDeliveryIssue(driverId, issueType, description, tripId));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid issue payload" : e.getMessage()));
        }
    }

    @GetMapping(value = "/delivery/issues", produces = "application/json")
    public ResponseEntity<?> listIssues(@RequestParam int driverId) {
        try {
            return ResponseEntity.ok(userService.listIssuesForDriver(driverId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/delivery/summary", produces = "application/json")
    public ResponseEntity<?> shiftSummary(
            @RequestParam int driverId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            return ResponseEntity.ok(userService.shiftSummary(driverId, from, to));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/management/ops", produces = "application/json")
    public ResponseEntity<?> managementOps() {
        try {
            return ResponseEntity.ok(userService.managementOps());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/management/orders-audit", produces = "application/json")
    public ResponseEntity<?> managementOrdersAudit(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String paymentMethod) {
        try {
            return ResponseEntity.ok(
                    userService.managementOrdersAudit(from, to, channel, paymentMethod));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/management/deliveries-audit", produces = "application/json")
    public ResponseEntity<?> managementDeliveriesAudit(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) Integer driverId,
            @RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(
                    userService.managementDeliveriesAudit(from, to, driverId, status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/management/day-pnl", produces = "application/json")
    public ResponseEntity<?> managementDayPnl(@RequestParam(required = false) String date) {
        try {
            return ResponseEntity.ok(userService.managementDayPnl(date));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/supplies/{supplyId}/request",
                 consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> requestSupplyByTeam(@PathVariable long supplyId,
                                                  @RequestBody Map<String, Object> body) {
        try {
            BigDecimal qty = toBigDecimal(body.get("requestedQty"));
            String urgency = (String) body.get("urgency");
            String team = (String) body.get("team");
            return ResponseEntity.ok(
                    userService.requestMoreSupplyByTeam(supplyId, qty, urgency, team));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @PostMapping(value = "/discount-campaigns",
                 consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> proposeDiscountCampaign(@RequestBody Map<String, Object> body) {
        try {
            int proposedById = ((Number) body.get("proposedByUserId")).intValue();
            BigDecimal discountPercent = toBigDecimal(body.get("discountPercent"));
            return ResponseEntity.ok(userService.proposeDiscountCampaign(
                    proposedById,
                    (String) body.get("name"),
                    (String) body.get("categoryFilter"),
                    discountPercent,
                    (String) body.get("startsOn"),
                    (String) body.get("endsOn")));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid campaign payload" : e.getMessage()));
        }
    }

    @GetMapping(value = "/discount-campaigns", produces = "application/json")
    public ResponseEntity<?> listDiscountCampaigns(
            @RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(userService.listDiscountCampaigns(status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/discount-campaigns/{id}/decision",
                 consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> decideDiscountCampaign(@PathVariable long id,
                                                     @RequestBody Map<String, Object> body) {
        try {
            int managerId = ((Number) body.get("managerUserId")).intValue();
            String decision = (String) body.get("decision");
            String notes = (String) body.get("notes");
            return ResponseEntity.ok(
                    userService.decideDiscountCampaign(id, managerId, decision, notes));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid decision payload" : e.getMessage()));
        }
    }

    @PostMapping(value = "/orders/{orderId}/flag-approval",
                 consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> flagOrderForApproval(@PathVariable long orderId,
                                                   @RequestBody Map<String, Object> body) {
        try {
            String notes = body == null ? null : (String) body.get("notes");
            return ResponseEntity.ok(userService.flagOrderForApproval(orderId, notes));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @GetMapping(value = "/orders/pending-approval", produces = "application/json")
    public ResponseEntity<?> listOrdersPendingApproval() {
        try {
            return ResponseEntity.ok(userService.listOrdersPendingApproval());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/orders/{orderId}/approval-decision",
                 consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> decideOrderApproval(@PathVariable long orderId,
                                                  @RequestBody Map<String, Object> body) {
        try {
            int managerId = ((Number) body.get("managerUserId")).intValue();
            String decision = (String) body.get("decision");
            String notes = (String) body.get("notes");
            return ResponseEntity.ok(
                    userService.decideOrderApproval(orderId, managerId, decision, notes));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid request" : e.getMessage()));
        }
    }

    @PostMapping(value = "/refund-requests", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> raiseRefundRequest(@RequestBody Map<String, Object> body) {
        try {
            long orderId = ((Number) body.get("orderId")).longValue();
            int raisedById = ((Number) body.get("raisedByUserId")).intValue();
            String type = (String) body.get("requestType");
            String reason = (String) body.get("reason");
            BigDecimal amount = toBigDecimal(body.get("amount"));
            return ResponseEntity.ok(
                    userService.raiseRefundRequest(orderId, raisedById, type, reason, amount));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid refund payload" : e.getMessage()));
        }
    }

    @GetMapping(value = "/refund-requests", produces = "application/json")
    public ResponseEntity<?> listRefundRequests(@RequestParam(required = false) String status) {
        try {
            return ResponseEntity.ok(userService.listRefundRequests(status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/refund-requests/{id}/decision",
                 consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> decideRefundRequest(@PathVariable long id,
                                                  @RequestBody Map<String, Object> body) {
        try {
            int managerId = ((Number) body.get("managerUserId")).intValue();
            String decision = (String) body.get("decision");
            String notes = (String) body.get("notes");
            return ResponseEntity.ok(
                    userService.decideRefundRequest(id, managerId, decision, notes));
        } catch (IllegalArgumentException | NullPointerException | ClassCastException e) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    e.getMessage() == null ? "Invalid decision payload" : e.getMessage()));
        }
    }

    @GetMapping(value = "/management/cash-reconciliation", produces = "application/json")
    public ResponseEntity<?> cashReconciliation(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) BigDecimal openingFloat,
            @RequestParam(required = false) BigDecimal countedCash) {
        try {
            return ResponseEntity.ok(
                    userService.cashReconciliation(date, openingFloat, countedCash));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/management/staff-performance", produces = "application/json")
    public ResponseEntity<?> managementStaffPerformance(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to) {
        try {
            return ResponseEntity.ok(userService.managementStaffPerformance(from, to));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static BigDecimal toBigDecimal(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Number) return BigDecimal.valueOf(((Number) raw).doubleValue());
        if (raw instanceof String s && !s.isBlank()) return new BigDecimal(s);
        return null;
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
