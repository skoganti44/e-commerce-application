// package com.example.groceryapi.service;

// import java.util.Collections;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;

// import static org.assertj.core.api.Assertions.assertThat;
// import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.ArgumentMatchers.any;
// import static org.mockito.Mockito.times;
// import static org.mockito.Mockito.verify;
// import static org.mockito.Mockito.when;

// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import static org.mockito.Mockito.never;

// import com.example.groceryapi.model.Cart;
// import com.example.groceryapi.model.CartItem;
// import com.example.groceryapi.model.Category;
// import com.example.groceryapi.model.OrderItem;
// import com.example.groceryapi.model.Orders;
// import com.example.groceryapi.model.Payment;
// import com.example.groceryapi.model.Product;
// import com.example.groceryapi.model.ProductAvailable;
// import com.example.groceryapi.model.ProductImage;
// import com.example.groceryapi.model.Role;
// import com.example.groceryapi.model.UserRole;
// import com.example.groceryapi.model.Users;
// import com.example.groceryapi.repository.Repository;
// import com.example.groceryapi.testdata.TestData;

// @ExtendWith(MockitoExtension.class)
// public class userServiceTest {

// @Mock
// private Repository repository;

// @InjectMocks
// private userService userService;

// @Test
// public void testFetchUsers_ReturnsUserList() {
// when(repository.findAllUsers()).thenReturn(TestData.users());

// List<Users> result = userService.fetchUsers();

// assertThat(result).hasSize(2);
// assertThat(result.get(0).getname()).isEqualTo("John");
// assertThat(result.get(1).getname()).isEqualTo("Jane");
// verify(repository, times(1)).findAllUsers();
// }

// @Test
// public void testFetchUsers_ReturnsEmptyList() {
// when(repository.findAllUsers()).thenReturn(Collections.emptyList());

// List<Users> result = userService.fetchUsers();

// assertThat(result).isEmpty();
// verify(repository, times(1)).findAllUsers();
// }

// @Test
// public void testFetchUsers_VerifiesCorrectFields() {
// Users expected = TestData.johnMarch();
// when(repository.findAllUsers()).thenReturn(List.of(expected));

// List<Users> result = userService.fetchUsers();

// assertThat(result).hasSize(1);
// Users returnedUser = result.get(0);
// assertThat(returnedUser.getuserid()).isEqualTo(expected.getuserid());
// assertThat(returnedUser.getname()).isEqualTo(expected.getname());
// assertThat(returnedUser.getemail()).isEqualTo(expected.getemail());
// assertThat(returnedUser.getpassword()).isEqualTo(expected.getpassword());
// assertThat(returnedUser.getcreatedat()).isEqualTo(expected.getcreatedat());
// }

// @Test
// public void testFetchRoles_ReturnsRoleList() {
// when(repository.findAllRoles()).thenReturn(List.of(TestData.alice(),
// TestData.bob()));

// List<Role> result = userService.fetchRoles(null);

// assertThat(result).hasSize(2);
// assertThat(result.get(0).getFullName()).isEqualTo("Alice Smith");
// assertThat(result.get(1).getFullName()).isEqualTo("Bob Johnson");
// verify(repository, times(1)).findAllRoles();
// }

// @Test
// public void testFetchRoles_ReturnsEmptyList() {
// when(repository.findAllRoles()).thenReturn(Collections.emptyList());

// List<Role> result = userService.fetchRoles(null);

// assertThat(result).isEmpty();
// verify(repository, times(1)).findAllRoles();
// }

// @Test
// public void testFetchRoles_FilterByDepartment() {
// Role joe = TestData.joeJonnas();
// when(repository.findRolesByDepartment(TestData.FRESH_PRODUCTS)).thenReturn(List.of(joe));

// List<Role> result = userService.fetchRoles(TestData.FRESH_PRODUCTS);

// assertThat(result).hasSize(1);
// assertThat(result.get(0).getDepartment()).isEqualTo(TestData.FRESH_PRODUCTS);
// verify(repository, times(1)).findRolesByDepartment(TestData.FRESH_PRODUCTS);
// }

// @Test
// public void testFetchRoles_BlankDepartment_ReturnsAll() {
// when(repository.findAllRoles()).thenReturn(List.of(TestData.alice()));

// List<Role> result = userService.fetchRoles("");

// assertThat(result).hasSize(1);
// verify(repository, times(1)).findAllRoles();
// }

// @Test
// public void testFetchRoles_VerifiesCorrectFields() {
// Role expected = TestData.alice();
// when(repository.findAllRoles()).thenReturn(List.of(expected));

// List<Role> result = userService.fetchRoles(null);

// assertThat(result).hasSize(1);
// Role returned = result.get(0);
// assertThat(returned.getId()).isEqualTo(expected.getId());
// assertThat(returned.getFullName()).isEqualTo(expected.getFullName());
// assertThat(returned.getRole()).isEqualTo(expected.getRole());
// assertThat(returned.getDepartment()).isEqualTo(expected.getDepartment());
// }

// @Test
// public void testFetchUserRoles_NullParams_ReturnsAll() {
// when(repository.findAllUserRoles()).thenReturn(TestData.userRoles());

// List<UserRole> result = userService.fetchUserRoles(null, null);

// assertThat(result).hasSize(2);
// verify(repository, times(1)).findAllUserRoles();
// verify(repository,
// never()).findUserRolesByUserId(org.mockito.ArgumentMatchers.anyInt());
// verify(repository,
// never()).findUserRolesByRoleId(org.mockito.ArgumentMatchers.anyInt());
// }

// @Test
// public void testFetchUserRoles_ByUserId() {
// when(repository.findUserRolesByUserId(1)).thenReturn(List.of(TestData.johnAsManager()));

// List<UserRole> result = userService.fetchUserRoles(1, null);

// assertThat(result).hasSize(1);
// assertThat(result.get(0).getUser().getname()).isEqualTo("John");
// assertThat(result.get(0).getRole().getFullName()).isEqualTo("Joe Jonnas");
// verify(repository, times(1)).findUserRolesByUserId(1);
// verify(repository, never()).findAllUserRoles();
// verify(repository,
// never()).findUserRolesByRoleId(org.mockito.ArgumentMatchers.anyInt());
// }

// @Test
// public void testFetchUserRoles_ByRoleId() {
// when(repository.findUserRolesByRoleId(2)).thenReturn(List.of(TestData.janeAsEngineer()));

// List<UserRole> result = userService.fetchUserRoles(null, 2);

// assertThat(result).hasSize(1);
// assertThat(result.get(0).getUser().getname()).isEqualTo("Jane");
// assertThat(result.get(0).getRole().getFullName()).isEqualTo("Bob Johnson");
// verify(repository, times(1)).findUserRolesByRoleId(2);
// verify(repository, never()).findAllUserRoles();
// verify(repository,
// never()).findUserRolesByUserId(org.mockito.ArgumentMatchers.anyInt());
// }

// @Test
// public void testFetchUserRoles_UserIdTakesPriorityOverRoleId() {
// when(repository.findUserRolesByUserId(1)).thenReturn(List.of(TestData.johnAsManager()));

// List<UserRole> result = userService.fetchUserRoles(1, 2);

// assertThat(result).hasSize(1);
// verify(repository, times(1)).findUserRolesByUserId(1);
// verify(repository,
// never()).findUserRolesByRoleId(org.mockito.ArgumentMatchers.anyInt());
// }

// @Test
// public void testFetchUserRoles_ReturnsEmptyList() {
// when(repository.findAllUserRoles()).thenReturn(Collections.emptyList());

// List<UserRole> result = userService.fetchUserRoles(null, null);

// assertThat(result).isEmpty();
// verify(repository, times(1)).findAllUserRoles();
// }

// // ========== fetchCartByUserId — POSITIVE scenarios ==========

// @Test
// @SuppressWarnings("unchecked")
// public void testFetchCartByUserId_ReturnsCartAndItems() {
// Cart cart = TestData.johnsCart();
// List<CartItem> items = TestData.johnsCartItems();
// when(repository.findCartsByUserId(1)).thenReturn(List.of(cart));
// when(repository.findCartItemsByUserId(1)).thenReturn(items);

// Map<String, Object> result = userService.fetchCartByUserId(1);

// assertThat(result).containsKeys("cart", "items");
// assertThat((List<Cart>) result.get("cart")).hasSize(1);
// assertThat((List<CartItem>) result.get("items")).hasSize(3);
// verify(repository, times(1)).findCartsByUserId(1);
// verify(repository, times(1)).findCartItemsByUserId(1);
// }

// @Test
// @SuppressWarnings("unchecked")
// public void testFetchCartByUserId_CartExistsButNoItems() {
// when(repository.findCartsByUserId(1)).thenReturn(List.of(TestData.johnsCart()));
// when(repository.findCartItemsByUserId(1)).thenReturn(Collections.emptyList());

// Map<String, Object> result = userService.fetchCartByUserId(1);

// assertThat((List<Cart>) result.get("cart")).hasSize(1);
// assertThat((List<CartItem>) result.get("items")).isEmpty();
// }

// @Test
// @SuppressWarnings("unchecked")
// public void testFetchCartByUserId_VerifiesItemFields() {
// Cart cart = TestData.johnsCart();
// when(repository.findCartsByUserId(1)).thenReturn(List.of(cart));
// when(repository.findCartItemsByUserId(1)).thenReturn(TestData.johnsCartItems());

// Map<String, Object> result = userService.fetchCartByUserId(1);
// List<CartItem> items = (List<CartItem>) result.get("items");

// assertThat(items.get(0).getProduct().getName()).isEqualTo("Apple");
// assertThat(items.get(0).getQuantity()).isEqualTo(3);
// assertThat(items.get(1).getProduct().getName()).isEqualTo("Milk");
// assertThat(items.get(1).getQuantity()).isEqualTo(6);
// assertThat(items.get(2).getProduct().getName()).isEqualTo("Bread");
// assertThat(items.get(2).getQuantity()).isEqualTo(10);
// }

// // ========== fetchCartByUserId — NEGATIVE scenarios ==========

// @Test
// public void testFetchCartByUserId_NoCartForUser_ThrowsException() {
// when(repository.findCartsByUserId(99)).thenReturn(Collections.emptyList());

// assertThatThrownBy(() -> userService.fetchCartByUserId(99))
// .isInstanceOf(IllegalArgumentException.class)
// .hasMessageContaining("No cart found for userId: 99");

// verify(repository, times(1)).findCartsByUserId(99);
// verify(repository,
// never()).findCartItemsByUserId(org.mockito.ArgumentMatchers.anyInt());
// }

// // ========== fetchOrdersForCustomer — POSITIVE scenarios ==========

// @Test
// @SuppressWarnings("unchecked")
// public void testFetchOrdersForCustomer_ValidCustomer_ReturnsOrdersAndItems()
// {
// Users john = TestData.john();
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(john));
// when(repository.findRolesByUserId(1)).thenReturn(List.of(TestData.customerRole()));
// when(repository.findOrdersByUserId(1)).thenReturn(List.of(TestData.johnsOrder()));
// when(repository.findOrderItemsByUserId(1)).thenReturn(TestData.johnsOrderItems());

// Map<String, Object> result = userService.fetchOrdersForCustomer(1);

// assertThat(result).containsKeys("orders", "items");
// assertThat((List<Orders>) result.get("orders")).hasSize(1);
// assertThat((List<OrderItem>) result.get("items")).hasSize(3);
// verify(repository, times(1)).findUserById(1);
// verify(repository, times(1)).findRolesByUserId(1);
// verify(repository, times(1)).findOrdersByUserId(1);
// verify(repository, times(1)).findOrderItemsByUserId(1);
// }

// @Test
// @SuppressWarnings("unchecked")
// public void
// testFetchOrdersForCustomer_CustomerWithNoOrders_ReturnsEmptyArrays() {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findRolesByUserId(1)).thenReturn(List.of(TestData.customerRole()));
// when(repository.findOrdersByUserId(1)).thenReturn(Collections.emptyList());
// when(repository.findOrderItemsByUserId(1)).thenReturn(Collections.emptyList());

// Map<String, Object> result = userService.fetchOrdersForCustomer(1);

// assertThat((List<Orders>) result.get("orders")).isEmpty();
// assertThat((List<OrderItem>) result.get("items")).isEmpty();
// }

// @Test
// public void testFetchOrdersForCustomer_RoleMatchIsCaseInsensitive() {
// Role customerUpper = TestData.role(99, "Jane", "CUSTOMER", TestData.SALES);
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findRolesByUserId(1)).thenReturn(List.of(customerUpper));
// when(repository.findOrdersByUserId(1)).thenReturn(Collections.emptyList());
// when(repository.findOrderItemsByUserId(1)).thenReturn(Collections.emptyList());

// Map<String, Object> result = userService.fetchOrdersForCustomer(1);

// assertThat(result).containsKeys("orders", "items");
// }

// @Test
// public void testFetchOrdersForCustomer_OneOfMultipleRolesIsCustomer_Allowed()
// {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findRolesByUserId(1)).thenReturn(
// List.of(TestData.managerRole(), TestData.customerRole()));
// when(repository.findOrdersByUserId(1)).thenReturn(Collections.emptyList());
// when(repository.findOrderItemsByUserId(1)).thenReturn(Collections.emptyList());

// Map<String, Object> result = userService.fetchOrdersForCustomer(1);

// assertThat(result).containsKeys("orders", "items");
// }

// // ========== fetchOrdersForCustomer — NEGATIVE scenarios ==========

// @Test
// public void testFetchOrdersForCustomer_UserNotFound_ThrowsIllegalArgument() {
// when(repository.findUserById(99)).thenReturn(java.util.Optional.empty());

// assertThatThrownBy(() -> userService.fetchOrdersForCustomer(99))
// .isInstanceOf(IllegalArgumentException.class)
// .hasMessageContaining("User not found: 99");

// verify(repository, times(1)).findUserById(99);
// verify(repository,
// never()).findRolesByUserId(org.mockito.ArgumentMatchers.anyInt());
// verify(repository,
// never()).findOrdersByUserId(org.mockito.ArgumentMatchers.anyInt());
// }

// @Test
// public void
// testFetchOrdersForCustomer_UserHasNoRoles_ThrowsSecurityException() {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findRolesByUserId(1)).thenReturn(Collections.emptyList());

// assertThatThrownBy(() -> userService.fetchOrdersForCustomer(1))
// .isInstanceOf(SecurityException.class)
// .hasMessageContaining("is not a customer");

// verify(repository,
// never()).findOrdersByUserId(org.mockito.ArgumentMatchers.anyInt());
// }

// @Test
// public void
// testFetchOrdersForCustomer_UserIsNotCustomer_ThrowsSecurityException() {
// when(repository.findUserById(2)).thenReturn(java.util.Optional.of(TestData.jane()));
// when(repository.findRolesByUserId(2)).thenReturn(List.of(TestData.managerRole()));

// assertThatThrownBy(() -> userService.fetchOrdersForCustomer(2))
// .isInstanceOf(SecurityException.class)
// .hasMessageContaining("User 2 is not a customer");

// verify(repository,
// never()).findOrdersByUserId(org.mockito.ArgumentMatchers.anyInt());
// verify(repository,
// never()).findOrderItemsByUserId(org.mockito.ArgumentMatchers.anyInt());
// }

// // ========== fetchPaymentsByUserId — POSITIVE scenarios ==========

// @Test
// public void testFetchPaymentsByUserId_IncludeAll_ReturnsAllPayments() {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findPaymentsByUserId(1)).thenReturn(TestData.johnsPayments());

// List<Payment> result = userService.fetchPaymentsByUserId(1, true);

// assertThat(result).hasSize(2);
// assertThat(result.get(0).getPaymentMethod()).isEqualTo("CREDIT_CARD");
// assertThat(result.get(0).getStatus()).isEqualTo("Decline");
// assertThat(result.get(1).getStatus()).isEqualTo("SUCCESS");
// verify(repository, times(1)).findUserById(1);
// verify(repository, times(1)).findPaymentsByUserId(1);
// }

// @Test
// public void
// testFetchPaymentsByUserId_DefaultFilter_HidesFailedWhenSuccessExists() {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findPaymentsByUserId(1)).thenReturn(TestData.johnsPayments());

// List<Payment> result = userService.fetchPaymentsByUserId(1, false);

// assertThat(result).hasSize(1);
// assertThat(result.get(0).getStatus()).isEqualTo("SUCCESS");
// }

// @Test
// public void testFetchPaymentsByUserId_DefaultFilter_NoSuccess_KeepsAll() {
// Orders order = TestData.johnsOrder();
// List<Payment> onlyFailed = List.of(
// TestData.payment(1L, order, "CREDIT_CARD", "Decline", new
// java.math.BigDecimal("28.26")),
// TestData.payment(2L, order, "CREDIT_CARD", "Pending", new
// java.math.BigDecimal("28.26")));
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findPaymentsByUserId(1)).thenReturn(onlyFailed);

// List<Payment> result = userService.fetchPaymentsByUserId(1, false);

// assertThat(result).hasSize(2);
// assertThat(result.get(0).getStatus()).isEqualTo("Decline");
// assertThat(result.get(1).getStatus()).isEqualTo("Pending");
// }

// @Test
// public void
// testFetchPaymentsByUserId_DefaultFilter_MultipleOrders_FiltersPerOrder() {
// Orders order1 = TestData.order(1L, TestData.john(), new
// java.math.BigDecimal("50.00"), "PLACED");
// Orders order2 = TestData.order(2L, TestData.john(), new
// java.math.BigDecimal("30.00"), "PLACED");
// List<Payment> mixed = List.of(
// TestData.payment(1L, order1, "CREDIT_CARD", "Decline", new
// java.math.BigDecimal("50.00")),
// TestData.payment(2L, order1, "CREDIT_CARD", "SUCCESS", new
// java.math.BigDecimal("50.00")),
// TestData.payment(3L, order2, "CREDIT_CARD", "Pending", new
// java.math.BigDecimal("30.00")));
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findPaymentsByUserId(1)).thenReturn(mixed);

// List<Payment> result = userService.fetchPaymentsByUserId(1, false);

// assertThat(result).hasSize(2);
// assertThat(result.get(0).getStatus()).isEqualTo("SUCCESS");
// assertThat(result.get(1).getStatus()).isEqualTo("Pending");
// }

// @Test
// public void testFetchPaymentsByUserId_UserHasNoPayments_ReturnsEmptyList() {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findPaymentsByUserId(1)).thenReturn(Collections.emptyList());

// List<Payment> result = userService.fetchPaymentsByUserId(1, false);

// assertThat(result).isEmpty();
// }

// // ========== fetchPaymentsByUserId — NEGATIVE scenarios ==========

// @Test
// public void testFetchPaymentsByUserId_UserNotFound_ThrowsIllegalArgument() {
// when(repository.findUserById(99)).thenReturn(java.util.Optional.empty());

// assertThatThrownBy(() -> userService.fetchPaymentsByUserId(99, false))
// .isInstanceOf(IllegalArgumentException.class)
// .hasMessageContaining("User not found: 99");

// verify(repository, times(1)).findUserById(99);
// verify(repository,
// never()).findPaymentsByUserId(org.mockito.ArgumentMatchers.anyInt());
// }

// // ========== saveProduct — POSITIVE scenarios ==========

// @Test
// public void testSaveProduct_ValidRequest_ReturnsProduct() {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.saveCategory(any(Category.class))).thenAnswer(inv -> {
// Category c = inv.getArgument(0);
// c.setId(100L);
// return c;
// });
// when(repository.saveProduct(any(Product.class))).thenAnswer(inv -> {
// Product p = inv.getArgument(0);
// p.setId(10L);
// return p;
// });
// when(repository.saveProductImage(any(ProductImage.class))).thenAnswer(inv ->
// inv.getArgument(0));

// Map<String, Object> request = Map.of(
// "userId", 1,
// "name", "Apple",
// "description", "Fresh apple",
// "items", List.of(Map.of(
// "category", Map.of("categoryName", "Fruits", "type", "Fresh"),
// "price", "2.00",
// "stock", 50,
// "imageUrl", "http://example.com/apple.jpg")));

// List<Product> result = userService.saveProduct(request);

// assertThat(result).hasSize(1);
// assertThat(result.get(0).getName()).isEqualTo("Apple");
// assertThat(result.get(0).getPrice()).isEqualByComparingTo("2.00");
// assertThat(result.get(0).getStock()).isEqualTo(50);
// assertThat(result.get(0).getCreatedBy().getname()).isEqualTo("John");
// verify(repository, times(1)).findUserById(1);
// verify(repository, times(1)).saveCategory(any(Category.class));
// verify(repository, times(1)).saveProduct(any(Product.class));
// verify(repository, times(1)).saveProductImage(any(ProductImage.class));
// }

// @Test
// public void testSaveProduct_MultipleItems_ReturnsMultipleProducts() {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.saveCategory(any(Category.class))).thenAnswer(inv ->
// inv.getArgument(0));
// when(repository.saveProduct(any(Product.class))).thenAnswer(inv ->
// inv.getArgument(0));
// when(repository.saveProductImage(any(ProductImage.class))).thenAnswer(inv ->
// inv.getArgument(0));

// Map<String, Object> request = Map.of(
// "userId", 1,
// "name", "Apple",
// "description", "Fresh apple",
// "items", List.of(
// Map.of("category", Map.of("categoryName", "Fruits", "type", "Fresh"),
// "price", "2.00", "stock", 50, "imageUrl", "url1"),
// Map.of("category", Map.of("categoryName", "Fruits", "type", "Organic"),
// "price", "3.00", "stock", 30, "imageUrl", "url2")));

// List<Product> result = userService.saveProduct(request);

// assertThat(result).hasSize(2);
// verify(repository, times(2)).saveCategory(any(Category.class));
// verify(repository, times(2)).saveProduct(any(Product.class));
// verify(repository, times(2)).saveProductImage(any(ProductImage.class));
// }

// // ========== saveProduct — NEGATIVE scenarios ==========

// @Test
// public void testSaveProduct_UserIdNull_ThrowsIllegalArgument() {
// Map<String, Object> request = new HashMap<>();
// request.put("name", "Apple");
// request.put("description", "desc");
// request.put("items", Collections.emptyList());

// assertThatThrownBy(() -> userService.saveProduct(request))
// .isInstanceOf(IllegalArgumentException.class)
// .hasMessageContaining("userId is required");

// verify(repository,
// never()).findUserById(org.mockito.ArgumentMatchers.anyInt());
// verify(repository, never()).saveProduct(any(Product.class));
// }

// @Test
// public void testSaveProduct_UserNotFound_ThrowsIllegalArgument() {
// when(repository.findUserById(99)).thenReturn(java.util.Optional.empty());

// Map<String, Object> request = Map.of(
// "userId", 99,
// "name", "Apple",
// "description", "desc",
// "items", Collections.emptyList());

// assertThatThrownBy(() -> userService.saveProduct(request))
// .isInstanceOf(IllegalArgumentException.class)
// .hasMessageContaining("User not found: 99");

// verify(repository, times(1)).findUserById(99);
// verify(repository, never()).saveProduct(any(Product.class));
// }

// // ========== saveProducts — POSITIVE scenarios ==========

// @Test
// public void testSaveProducts_ValidRequest_MultipleProducts() {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.saveCategory(any(Category.class))).thenAnswer(inv ->
// inv.getArgument(0));
// when(repository.saveProduct(any(Product.class))).thenAnswer(inv ->
// inv.getArgument(0));
// when(repository.saveProductImage(any(ProductImage.class))).thenAnswer(inv ->
// inv.getArgument(0));

// Map<String, Object> request = Map.of(
// "userId", 1,
// "products", List.of(
// Map.of("name", "Apple", "description", "Fresh apple",
// "items", List.of(Map.of(
// "category", Map.of("categoryName", "Fruits", "type", "Fresh"),
// "price", "2.00", "stock", 50, "imageUrl", "url1"))),
// Map.of("name", "Milk", "description", "Fresh milk",
// "items", List.of(Map.of(
// "category", Map.of("categoryName", "Dairy", "type", "Fresh"),
// "price", "5.00", "stock", 30, "imageUrl", "url2")))));

// List<Product> result = userService.saveProducts(request);

// assertThat(result).hasSize(2);
// assertThat(result).extracting(Product::getName).containsExactly("Apple",
// "Milk");
// verify(repository, times(1)).findUserById(1);
// verify(repository, times(2)).saveProduct(any(Product.class));
// }

// @Test
// public void testSaveProducts_EmptyProductList_ReturnsEmpty() {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));

// Map<String, Object> request = Map.of(
// "userId", 1,
// "products", Collections.emptyList());

// List<Product> result = userService.saveProducts(request);

// assertThat(result).isEmpty();
// verify(repository, never()).saveProduct(any(Product.class));
// }

// // ========== saveProducts — NEGATIVE scenarios ==========

// @Test
// public void testSaveProducts_UserIdNull_ThrowsIllegalArgument() {
// Map<String, Object> request = new HashMap<>();
// request.put("products", Collections.emptyList());

// assertThatThrownBy(() -> userService.saveProducts(request))
// .isInstanceOf(IllegalArgumentException.class)
// .hasMessageContaining("userId is required");

// verify(repository,
// never()).findUserById(org.mockito.ArgumentMatchers.anyInt());
// }

// @Test
// public void testSaveProducts_UserNotFound_ThrowsIllegalArgument() {
// when(repository.findUserById(99)).thenReturn(java.util.Optional.empty());

// Map<String, Object> request = Map.of(
// "userId", 99,
// "products", Collections.emptyList());

// assertThatThrownBy(() -> userService.saveProducts(request))
// .isInstanceOf(IllegalArgumentException.class)
// .hasMessageContaining("User not found: 99");

// verify(repository, times(1)).findUserById(99);
// verify(repository, never()).saveProduct(any(Product.class));
// }

// // ========== cleanupForUser — POSITIVE scenarios ==========

// @Test
// public void testCleanupForUser_ArchivesUnsoldProducts_DeletesAll() {
// Orders order = TestData.johnsOrder();
// List<OrderItem> items = List.of(
// TestData.orderItem(1L, order, TestData.apple(), 2, new
// java.math.BigDecimal("2.00")),
// TestData.orderItem(2L, order, TestData.milk(), 6, new
// java.math.BigDecimal("5.26")));
// List<Payment> payments = List.of(
// TestData.payment(1L, order, "CREDIT_CARD", "Decline", new
// java.math.BigDecimal("28.26")));

// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findOrderItemsByUserId(1)).thenReturn(items);
// when(repository.findPaymentsByUserId(1)).thenReturn(payments);
// when(repository.findImagesByProductId(org.mockito.ArgumentMatchers.anyLong()))
// .thenReturn(Collections.emptyList());
// when(repository.saveProductAvailable(any(ProductAvailable.class)))
// .thenAnswer(inv -> inv.getArgument(0));
// when(repository.deletePaymentsByUserId(1)).thenReturn(1);
// when(repository.deleteOrderItemsByUserId(1)).thenReturn(2);
// when(repository.deleteOrdersByUserId(1)).thenReturn(1);

// Map<String, Object> result = userService.cleanupForUser(1);

// assertThat(result).containsEntry("userId", 1);
// assertThat(result).containsEntry("archived", 2);
// assertThat(result).containsEntry("paymentsDeleted", 1);
// assertThat(result).containsEntry("orderItemsDeleted", 2);
// assertThat(result).containsEntry("ordersDeleted", 1);
// verify(repository,
// times(2)).saveProductAvailable(any(ProductAvailable.class));
// }

// @Test
// public void testCleanupForUser_SoldItemsNotArchived() {
// Orders order = TestData.johnsOrder();
// List<OrderItem> items = List.of(
// TestData.orderItem(1L, order, TestData.apple(), 2, new
// java.math.BigDecimal("2.00")),
// TestData.orderItem(2L, order, TestData.milk(), 6, new
// java.math.BigDecimal("5.26")));
// List<Payment> payments = List.of(
// TestData.payment(1L, order, "CREDIT_CARD", "SUCCESS", new
// java.math.BigDecimal("28.26")));

// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findOrderItemsByUserId(1)).thenReturn(items);
// when(repository.findPaymentsByUserId(1)).thenReturn(payments);
// when(repository.deletePaymentsByUserId(1)).thenReturn(1);
// when(repository.deleteOrderItemsByUserId(1)).thenReturn(2);
// when(repository.deleteOrdersByUserId(1)).thenReturn(1);

// Map<String, Object> result = userService.cleanupForUser(1);

// assertThat(result).containsEntry("archived", 0);
// verify(repository,
// never()).saveProductAvailable(any(ProductAvailable.class));
// }

// @Test
// public void testCleanupForUser_MixedOrders_ArchivesOnlyUnsold() {
// Orders soldOrder = TestData.order(1L, TestData.john(), new
// java.math.BigDecimal("10.00"), "PLACED");
// Orders unsoldOrder = TestData.order(2L, TestData.john(), new
// java.math.BigDecimal("5.00"), "PLACED");
// List<OrderItem> items = List.of(
// TestData.orderItem(1L, soldOrder, TestData.apple(), 1, new
// java.math.BigDecimal("2.00")),
// TestData.orderItem(2L, unsoldOrder, TestData.bread(), 1, new
// java.math.BigDecimal("1.00")));
// List<Payment> payments = List.of(
// TestData.payment(1L, soldOrder, "CREDIT_CARD", "SUCCESS", new
// java.math.BigDecimal("10.00")),
// TestData.payment(2L, unsoldOrder, "CREDIT_CARD", "Decline", new
// java.math.BigDecimal("5.00")));

// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findOrderItemsByUserId(1)).thenReturn(items);
// when(repository.findPaymentsByUserId(1)).thenReturn(payments);
// when(repository.findImagesByProductId(org.mockito.ArgumentMatchers.anyLong()))
// .thenReturn(Collections.emptyList());
// when(repository.saveProductAvailable(any(ProductAvailable.class)))
// .thenAnswer(inv -> inv.getArgument(0));
// when(repository.deletePaymentsByUserId(1)).thenReturn(2);
// when(repository.deleteOrderItemsByUserId(1)).thenReturn(2);
// when(repository.deleteOrdersByUserId(1)).thenReturn(2);

// Map<String, Object> result = userService.cleanupForUser(1);

// assertThat(result).containsEntry("archived", 1);
// verify(repository,
// times(1)).saveProductAvailable(any(ProductAvailable.class));
// }

// @Test
// public void testCleanupForUser_DuplicateProductAcrossOrders_ArchivedOnce() {
// Orders o1 = TestData.order(1L, TestData.john(), new
// java.math.BigDecimal("2.00"), "PLACED");
// Orders o2 = TestData.order(2L, TestData.john(), new
// java.math.BigDecimal("4.00"), "PLACED");
// List<OrderItem> items = List.of(
// TestData.orderItem(1L, o1, TestData.apple(), 1, new
// java.math.BigDecimal("2.00")),
// TestData.orderItem(2L, o2, TestData.apple(), 2, new
// java.math.BigDecimal("2.00")));

// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findOrderItemsByUserId(1)).thenReturn(items);
// when(repository.findPaymentsByUserId(1)).thenReturn(Collections.emptyList());
// when(repository.findImagesByProductId(org.mockito.ArgumentMatchers.anyLong()))
// .thenReturn(Collections.emptyList());
// when(repository.saveProductAvailable(any(ProductAvailable.class)))
// .thenAnswer(inv -> inv.getArgument(0));
// when(repository.deletePaymentsByUserId(1)).thenReturn(0);
// when(repository.deleteOrderItemsByUserId(1)).thenReturn(2);
// when(repository.deleteOrdersByUserId(1)).thenReturn(2);

// Map<String, Object> result = userService.cleanupForUser(1);

// assertThat(result).containsEntry("archived", 1);
// verify(repository,
// times(1)).saveProductAvailable(any(ProductAvailable.class));
// }

// @Test
// public void testCleanupForUser_NoOrders_ReturnsZeroCounts() {
// when(repository.findUserById(1)).thenReturn(java.util.Optional.of(TestData.john()));
// when(repository.findOrderItemsByUserId(1)).thenReturn(Collections.emptyList());
// when(repository.findPaymentsByUserId(1)).thenReturn(Collections.emptyList());
// when(repository.deletePaymentsByUserId(1)).thenReturn(0);
// when(repository.deleteOrderItemsByUserId(1)).thenReturn(0);
// when(repository.deleteOrdersByUserId(1)).thenReturn(0);

// Map<String, Object> result = userService.cleanupForUser(1);

// assertThat(result).containsEntry("archived", 0);
// assertThat(result).containsEntry("paymentsDeleted", 0);
// assertThat(result).containsEntry("orderItemsDeleted", 0);
// assertThat(result).containsEntry("ordersDeleted", 0);
// verify(repository,
// never()).saveProductAvailable(any(ProductAvailable.class));
// }

// // ========== cleanupForUser — NEGATIVE scenarios ==========

// @Test
// public void testCleanupForUser_UserNotFound_ThrowsIllegalArgument() {
// when(repository.findUserById(99)).thenReturn(java.util.Optional.empty());

// assertThatThrownBy(() -> userService.cleanupForUser(99))
// .isInstanceOf(IllegalArgumentException.class)
// .hasMessageContaining("User not found: 99");

// verify(repository,
// never()).findOrderItemsByUserId(org.mockito.ArgumentMatchers.anyInt());
// verify(repository,
// never()).deletePaymentsByUserId(org.mockito.ArgumentMatchers.anyInt());
// }
// }
