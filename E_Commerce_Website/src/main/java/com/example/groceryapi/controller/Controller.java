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

    @PostMapping(value = "/product", consumes = "application/json", produces = "application/json")
    public ResponseEntity<List<Product>> saveProduct(@RequestBody Map<String, Object> request) {
        try {
            return ResponseEntity.ok(userService.saveProduct(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = "/products", consumes = "application/json", produces = "application/json")
    public ResponseEntity<List<Product>> saveProducts(@RequestBody Map<String, Object> request) {
        try {
            return ResponseEntity.ok(userService.saveProducts(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
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
}
