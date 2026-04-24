package com.example.groceryapi.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.groceryapi.service.UserService;
import com.example.groceryapi.testdata.TestData;

@WebMvcTest(Controller.class)
public class ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    public void testFetchUsers_ReturnsUserList() throws Exception {
        when(userService.fetchUsers()).thenReturn(TestData.users());

        mockMvc.perform(get("/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("John"))
                .andExpect(jsonPath("$[0].email").value("john@example.com"))
                .andExpect(jsonPath("$[1].name").value("Jane"))
                .andExpect(jsonPath("$[1].email").value("jane@example.com"));
    }

    @Test
    public void testFetchUsers_ReturnsEmptyList() throws Exception {
        when(userService.fetchUsers()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/users").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testFetchUsers_WithoutAcceptHeader_Returns406() throws Exception {
        mockMvc.perform(get("/users").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    public void testFetchRoles_ReturnsRoleList() throws Exception {
        when(userService.fetchRoles(null)).thenReturn(List.of(TestData.alice(), TestData.bob()));

        mockMvc.perform(get("/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Alice Smith"))
                .andExpect(jsonPath("$[0].role").value("Manager"))
                .andExpect(jsonPath("$[0].department").value(TestData.SALES))
                .andExpect(jsonPath("$[1].fullName").value("Bob Johnson"))
                .andExpect(jsonPath("$[1].role").value("Engineer"))
                .andExpect(jsonPath("$[1].department").value(TestData.IT));
    }

    @Test
    public void testFetchRoles_ReturnsEmptyList() throws Exception {
        when(userService.fetchRoles(null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testFetchRoles_WithoutAcceptHeader_Returns406() throws Exception {
        mockMvc.perform(get("/roles").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    @Test
    public void testFetchRoles_FilterByDepartment() throws Exception {
        when(userService.fetchRoles(TestData.FRESH_PRODUCTS)).thenReturn(List.of(TestData.joeJonnas()));

        mockMvc.perform(get("/roles").param("department", TestData.FRESH_PRODUCTS).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Joe Jonnas"))
                .andExpect(jsonPath("$[0].department").value(TestData.FRESH_PRODUCTS));
    }

    @Test
    public void testFetchUserRoles_ReturnsAll() throws Exception {
        when(userService.fetchUserRoles(null, null)).thenReturn(TestData.userRoles());

        mockMvc.perform(get("/userRoles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].user.name").value("John"))
                .andExpect(jsonPath("$[0].role.fullName").value("Joe Jonnas"))
                .andExpect(jsonPath("$[1].user.name").value("Jane"))
                .andExpect(jsonPath("$[1].role.fullName").value("Bob Johnson"));
    }

    @Test
    public void testFetchUserRoles_FilterByUserId() throws Exception {
        when(userService.fetchUserRoles(1, null)).thenReturn(List.of(TestData.johnAsManager()));

        mockMvc.perform(get("/userRoles").param("userid", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].user.userid").value(1))
                .andExpect(jsonPath("$[0].user.name").value("John"))
                .andExpect(jsonPath("$[0].role.fullName").value("Joe Jonnas"))
                .andExpect(jsonPath("$[0].role.department").value(TestData.FRESH_PRODUCTS));
    }

    @Test
    public void testFetchUserRoles_FilterByRoleId() throws Exception {
        when(userService.fetchUserRoles(null, 2)).thenReturn(List.of(TestData.janeAsEngineer()));

        mockMvc.perform(get("/userRoles").param("roleid", "2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].user.name").value("Jane"))
                .andExpect(jsonPath("$[0].role.fullName").value("Bob Johnson"))
                .andExpect(jsonPath("$[0].role.department").value(TestData.IT));
    }

    @Test
    public void testFetchUserRoles_ReturnsEmptyList() throws Exception {
        when(userService.fetchUserRoles(null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/userRoles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testFetchUserRoles_WithoutAcceptHeader_Returns406() throws Exception {
        mockMvc.perform(get("/userRoles").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    // ========== /cart endpoint — POSITIVE scenarios ==========

    @Test
    public void testFetchCart_ReturnsCartWithItems() throws Exception {
        when(userService.fetchCartByUserId(1)).thenReturn(Map.of(
                "cart", List.of(TestData.johnsCart()),
                "items", TestData.johnsCartItems()));

        mockMvc.perform(get("/cart").param("userid", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart.length()").value(1))
                .andExpect(jsonPath("$.cart[0].id").value(1))
                .andExpect(jsonPath("$.cart[0].user.userid").value(1))
                .andExpect(jsonPath("$.cart[0].user.name").value("John"))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].product.name").value("Apple"))
                .andExpect(jsonPath("$.items[0].product.price").value(2.00))
                .andExpect(jsonPath("$.items[1].quantity").value(6))
                .andExpect(jsonPath("$.items[1].product.name").value("Milk"))
                .andExpect(jsonPath("$.items[2].quantity").value(10))
                .andExpect(jsonPath("$.items[2].product.name").value("Bread"));
    }

    @Test
    public void testFetchCart_CartExistsButNoItems() throws Exception {
        when(userService.fetchCartByUserId(1)).thenReturn(Map.of(
                "cart", List.of(TestData.johnsCart()),
                "items", Collections.emptyList()));

        mockMvc.perform(get("/cart").param("userid", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cart.length()").value(1))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    // ========== /cart endpoint — NEGATIVE scenarios ==========

    @Test
    public void testFetchCart_UserHasNoCart_Returns404() throws Exception {
        when(userService.fetchCartByUserId(99))
                .thenThrow(new IllegalArgumentException("No cart found for userId: 99"));

        mockMvc.perform(get("/cart").param("userid", "99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No cart found for userId: 99"));
    }

    @Test
    public void testFetchCart_MissingUseridParam_Returns400() throws Exception {
        mockMvc.perform(get("/cart").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFetchCart_InvalidUseridFormat_Returns400() throws Exception {
        mockMvc.perform(get("/cart").param("userid", "abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFetchCart_WithoutAcceptHeader_Returns406() throws Exception {
        mockMvc.perform(get("/cart").param("userid", "1").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    // ========== /orders endpoint — POSITIVE scenarios ==========

    @Test
    public void testFetchOrders_CustomerWithOrders_Returns200() throws Exception {
        when(userService.fetchOrdersForCustomer(1)).thenReturn(Map.of(
                "orders", List.of(TestData.johnsOrder()),
                "items", TestData.johnsOrderItems()));

        mockMvc.perform(get("/orders").param("userid", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(1))
                .andExpect(jsonPath("$.orders[0].id").value(1))
                .andExpect(jsonPath("$.orders[0].status").value("PLACED"))
                .andExpect(jsonPath("$.orders[0].totalAmount").value(1049.99))
                .andExpect(jsonPath("$.orders[0].user.name").value("John"))
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].product.name").value("Apple"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].price").value(2.00));
    }

    @Test
    public void testFetchOrders_CustomerWithNoOrders_Returns200EmptyArrays() throws Exception {
        when(userService.fetchOrdersForCustomer(1)).thenReturn(Map.of(
                "orders", Collections.emptyList(),
                "items", Collections.emptyList()));

        mockMvc.perform(get("/orders").param("userid", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders.length()").value(0))
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    // ========== /orders endpoint — NEGATIVE scenarios ==========

    @Test
    public void testFetchOrders_UserNotFound_Returns404() throws Exception {
        when(userService.fetchOrdersForCustomer(99))
                .thenThrow(new IllegalArgumentException("User not found: 99"));

        mockMvc.perform(get("/orders").param("userid", "99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: 99"));
    }

    @Test
    public void testFetchOrders_UserNotCustomer_Returns403() throws Exception {
        when(userService.fetchOrdersForCustomer(2))
                .thenThrow(new SecurityException("User 2 is not a customer"));

        mockMvc.perform(get("/orders").param("userid", "2").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("User 2 is not a customer"));
    }

    @Test
    public void testFetchOrders_MissingUseridParam_Returns400() throws Exception {
        mockMvc.perform(get("/orders").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFetchOrders_InvalidUseridFormat_Returns400() throws Exception {
        mockMvc.perform(get("/orders").param("userid", "abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFetchOrders_WithoutAcceptHeader_Returns406() throws Exception {
        mockMvc.perform(get("/orders").param("userid", "1").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    // ========== /payments endpoint — POSITIVE scenarios ==========

    @Test
    public void testFetchPayments_DefaultFilter_ReturnsFilteredPayments() throws Exception {
        when(userService.fetchPaymentsByUserId(1, false))
                .thenReturn(List.of(TestData.johnsPayments().get(1)));

        mockMvc.perform(get("/payments").param("userid", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                .andExpect(jsonPath("$[0].paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$[0].amount").value(28.26))
                .andExpect(jsonPath("$[0].order.id").value(1));
    }

    @Test
    public void testFetchPayments_IncludeAllTrue_ReturnsAllPayments() throws Exception {
        when(userService.fetchPaymentsByUserId(1, true)).thenReturn(TestData.johnsPayments());

        mockMvc.perform(get("/payments")
                        .param("userid", "1")
                        .param("includeAll", "true")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("Decline"))
                .andExpect(jsonPath("$[1].status").value("SUCCESS"));
    }

    @Test
    public void testFetchPayments_UserHasNoPayments_ReturnsEmptyList() throws Exception {
        when(userService.fetchPaymentsByUserId(1, false)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/payments").param("userid", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ========== /payments endpoint — NEGATIVE scenarios ==========

    @Test
    public void testFetchPayments_UserNotFound_Returns404() throws Exception {
        when(userService.fetchPaymentsByUserId(99, false))
                .thenThrow(new IllegalArgumentException("User not found: 99"));

        mockMvc.perform(get("/payments").param("userid", "99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: 99"));
    }

    @Test
    public void testFetchPayments_MissingUseridParam_Returns400() throws Exception {
        mockMvc.perform(get("/payments").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFetchPayments_InvalidUseridFormat_Returns400() throws Exception {
        mockMvc.perform(get("/payments").param("userid", "abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testFetchPayments_WithoutAcceptHeader_Returns406() throws Exception {
        mockMvc.perform(get("/payments").param("userid", "1").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isNotAcceptable());
    }

    // ========== POST /product — POSITIVE scenarios ==========

    @Test
    @SuppressWarnings("unchecked")
    public void testSaveProduct_ValidRequest_Returns200() throws Exception {
        when(userService.saveProduct(any(Map.class))).thenReturn(List.of(TestData.apple()));

        String body = "{" +
                "\"userId\":1," +
                "\"name\":\"Apple\"," +
                "\"description\":\"Fresh apple\"," +
                "\"items\":[{" +
                "\"category\":{\"categoryName\":\"Fruits\",\"type\":\"Fresh\"}," +
                "\"price\":\"2.00\",\"stock\":50,\"imageUrl\":\"http://example.com/a.jpg\"" +
                "}]}";

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Apple"))
                .andExpect(jsonPath("$[0].price").value(2.00))
                .andExpect(jsonPath("$[0].stock").value(50));
    }

    // ========== POST /product — NEGATIVE scenarios ==========

    @Test
    @SuppressWarnings("unchecked")
    public void testSaveProduct_UserIdNull_Returns400() throws Exception {
        when(userService.saveProduct(any(Map.class)))
                .thenThrow(new IllegalArgumentException("userId is required"));

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Apple\",\"description\":\"d\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSaveProduct_UserNotFound_Returns400() throws Exception {
        when(userService.saveProduct(any(Map.class)))
                .thenThrow(new IllegalArgumentException("User not found: 99"));

        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":99,\"name\":\"Apple\",\"description\":\"d\",\"items\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveProduct_MissingBody_Returns400() throws Exception {
        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveProduct_UnsupportedMediaType_Returns415() throws Exception {
        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<product/>"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ========== POST /products — POSITIVE scenarios ==========

    @Test
    @SuppressWarnings("unchecked")
    public void testSaveProducts_ValidRequest_Returns200() throws Exception {
        when(userService.saveProducts(any(Map.class)))
                .thenReturn(List.of(TestData.apple(), TestData.milk()));

        String body = "{\"userId\":1,\"products\":[" +
                "{\"name\":\"Apple\",\"description\":\"d\",\"items\":[" +
                "{\"category\":{\"categoryName\":\"Fruits\",\"type\":\"Fresh\"}," +
                "\"price\":\"2.00\",\"stock\":50,\"imageUrl\":\"u1\"}]}," +
                "{\"name\":\"Milk\",\"description\":\"d\",\"items\":[" +
                "{\"category\":{\"categoryName\":\"Dairy\",\"type\":\"Fresh\"}," +
                "\"price\":\"5.26\",\"stock\":30,\"imageUrl\":\"u2\"}]}" +
                "]}";

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Apple"))
                .andExpect(jsonPath("$[1].name").value("Milk"));
    }

    // ========== POST /products — NEGATIVE scenarios ==========

    @Test
    @SuppressWarnings("unchecked")
    public void testSaveProducts_UserIdNull_Returns400() throws Exception {
        when(userService.saveProducts(any(Map.class)))
                .thenThrow(new IllegalArgumentException("userId is required"));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"products\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSaveProducts_UserNotFound_Returns400() throws Exception {
        when(userService.saveProducts(any(Map.class)))
                .thenThrow(new IllegalArgumentException("User not found: 99"));

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":99,\"products\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveProducts_MissingBody_Returns400() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testSaveProducts_UnsupportedMediaType_Returns415() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<products/>"))
                .andExpect(status().isUnsupportedMediaType());
    }

    // ========== DELETE /cleanup — POSITIVE scenarios ==========

    @Test
    public void testCleanup_ValidUser_Returns200WithCounts() throws Exception {
        when(userService.cleanupForUser(1)).thenReturn(Map.of(
                "userId", 1,
                "archived", 2,
                "paymentsDeleted", 1,
                "orderItemsDeleted", 2,
                "ordersDeleted", 1));

        mockMvc.perform(delete("/cleanup").param("userid", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.archived").value(2))
                .andExpect(jsonPath("$.paymentsDeleted").value(1))
                .andExpect(jsonPath("$.orderItemsDeleted").value(2))
                .andExpect(jsonPath("$.ordersDeleted").value(1));
    }

    @Test
    public void testCleanup_UserWithNoOrders_Returns200WithZeros() throws Exception {
        when(userService.cleanupForUser(1)).thenReturn(Map.of(
                "userId", 1,
                "archived", 0,
                "paymentsDeleted", 0,
                "orderItemsDeleted", 0,
                "ordersDeleted", 0));

        mockMvc.perform(delete("/cleanup").param("userid", "1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(0));
    }

    // ========== DELETE /cleanup — NEGATIVE scenarios ==========

    @Test
    public void testCleanup_UserNotFound_Returns404() throws Exception {
        when(userService.cleanupForUser(99))
                .thenThrow(new IllegalArgumentException("User not found: 99"));

        mockMvc.perform(delete("/cleanup").param("userid", "99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: 99"));
    }

    @Test
    public void testCleanup_MissingUseridParam_Returns400() throws Exception {
        mockMvc.perform(delete("/cleanup").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testCleanup_InvalidUseridFormat_Returns400() throws Exception {
        mockMvc.perform(delete("/cleanup").param("userid", "abc").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
