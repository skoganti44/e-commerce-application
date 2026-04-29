package com.example.groceryapi.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.groceryapi.model.DeliveryIssue;
import com.example.groceryapi.model.DeliveryTrip;
import com.example.groceryapi.model.DiscountCampaign;
import com.example.groceryapi.model.RefundRequest;
import com.example.groceryapi.model.OrderItem;
import com.example.groceryapi.model.Orders;
import com.example.groceryapi.model.Payment;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.Supply;
import com.example.groceryapi.model.Task;
import com.example.groceryapi.model.User;
import com.example.groceryapi.repository.Repository;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private Repository repository;

    @InjectMocks
    private UserService service;

    private Supply flour;

    @BeforeEach
    public void setUp() {
        flour = newSupply(1L, "Finger Millet Flour", "flour", "kg",
                new BigDecimal("5.0"),   // inStock
                new BigDecimal("0"),     // currentStock
                new BigDecimal("2.0"),   // threshold
                "received",              // orderStatus
                new BigDecimal("0"));    // requestedQty
    }

    // ---------- computeUnitPrice: pricing business logic ----------

    @Test
    public void computeUnitPrice_basePrice_whenNoAddons() {
        Product p = newProduct("Cookie", new BigDecimal("10.00"));

        BigDecimal unit = UserService.computeUnitPrice(p, null, null);

        assertThat(unit).isEqualByComparingTo("10.00");
    }

    @Test
    public void computeUnitPrice_addsSweetenerAndFlourAddons() {
        Product p = newProduct("Cookie", new BigDecimal("10.00"));

        BigDecimal unit = UserService.computeUnitPrice(p, "HONEY", "FINGER_MILLET");

        // 10 + 3 (HONEY) + 5 (FINGER_MILLET) = 18
        assertThat(unit).isEqualByComparingTo("18.00");
    }

    @Test
    public void computeUnitPrice_ignoresUnknownAddonCodes() {
        Product p = newProduct("Cookie", new BigDecimal("10.00"));

        BigDecimal unit = UserService.computeUnitPrice(p, "UNKNOWN", "UNKNOWN");

        assertThat(unit).isEqualByComparingTo("10.00");
    }

    // ---------- register: positive ----------

    @Test
    public void register_customer_savesUserAndCustomerRole() {
        when(repository.findUserByEmail("a@b.com")).thenReturn(Optional.empty());
        when(repository.saveUser(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setuserid(42);
            return u;
        });
        when(repository.findRoleByName("customer")).thenReturn(Optional.of(customerRole()));

        User saved = service.register("Alice", "a@b.com", "pw", "customer", null);

        assertThat(saved.getuserid()).isEqualTo(42);
        assertThat(saved.getname()).isEqualTo("Alice");
        verify(repository).saveUserRole(any());
    }

    @Test
    public void register_employee_requiresValidDepartment() {
        when(repository.findUserByEmail(any())).thenReturn(Optional.empty());
        when(repository.saveUser(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.findRoleByRoleAndDepartment("employee", "kitchen"))
                .thenReturn(Optional.empty());
        when(repository.saveRole(any())).thenAnswer(inv -> inv.getArgument(0));

        User saved = service.register("Chef", "c@b.com", "pw", "employee", "kitchen");

        assertThat(saved.getname()).isEqualTo("Chef");
        verify(repository).saveUserRole(any());
    }

    // ---------- register: negative ----------

    @Test
    public void register_blankName_throws() {
        assertThatThrownBy(() -> service.register("", "a@b.com", "pw", "customer", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    public void register_blankEmail_throws() {
        assertThatThrownBy(() -> service.register("Alice", "", "pw", "customer", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    public void register_blankPassword_throws() {
        assertThatThrownBy(() -> service.register("Alice", "a@b.com", "", "customer", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @Test
    public void register_employeeMissingDept_throws() {
        assertThatThrownBy(() -> service.register("Chef", "c@b.com", "pw", "employee", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("department");
    }

    @Test
    public void register_employeeInvalidDept_throws() {
        assertThatThrownBy(() -> service.register("Chef", "c@b.com", "pw", "employee", "martian"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid department");
    }

    @Test
    public void register_duplicateEmail_throws() {
        when(repository.findUserByEmail("taken@b.com"))
                .thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> service.register("A", "taken@b.com", "pw", "customer", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");
    }

    // ---------- login: positive + negative ----------

    @Test
    public void login_withCorrectCredentials_returnsUser() {
        User existing = new User();
        existing.setuserid(7);
        existing.setname("Alice");
        existing.setemail("a@b.com");
        existing.setpassword("pw");
        when(repository.findUserByEmail("a@b.com")).thenReturn(Optional.of(existing));

        User loggedIn = service.login("a@b.com", "pw");

        assertThat(loggedIn.getuserid()).isEqualTo(7);
    }

    @Test
    public void login_unknownEmail_throws() {
        when(repository.findUserByEmail("nope@b.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("nope@b.com", "pw"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid email or password");
    }

    @Test
    public void login_wrongPassword_throws() {
        User existing = new User();
        existing.setpassword("correct");
        when(repository.findUserByEmail("a@b.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.login("a@b.com", "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid email or password");
    }

    @Test
    public void login_nullEmailOrPassword_throws() {
        assertThatThrownBy(() -> service.login(null, "pw"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.login("a@b.com", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- fetchSupplies ----------

    @Test
    public void fetchSupplies_seedsWhenEmpty_thenReturnsNonReceived() {
        when(repository.countSupplies()).thenReturn(0L, 28L);
        Supply waiting = newSupply(2L, "Honey", "sweetener", "l",
                new BigDecimal("1.0"), new BigDecimal("0"), new BigDecimal("0.5"),
                "waiting", new BigDecimal("3.0"));
        when(repository.findAllSupplies()).thenReturn(List.of(flour, waiting));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        List<Map<String, Object>> result = service.fetchSupplies();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name")).isEqualTo("Honey");
        assertThat(result.get(0).get("orderStatus")).isEqualTo("waiting");
    }

    @Test
    public void fetchSupplies_migratesLegacyCurrentStockToInStock() {
        Supply legacy = new Supply();
        legacy.setId(9L);
        legacy.setName("Old Flour");
        legacy.setUnit("kg");
        legacy.setCategory("flour");
        legacy.setCurrentStock(new BigDecimal("4.5"));
        // inStock, requestedQty, orderStatus all null — triggers migration
        when(repository.countSupplies()).thenReturn(1L);
        when(repository.findAllSupplies()).thenReturn(List.of(legacy));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        service.fetchSupplies();

        assertThat(legacy.getInStock()).isEqualByComparingTo("4.5");
        assertThat(legacy.getCurrentStock()).isEqualByComparingTo("0");
        assertThat(legacy.getRequestedQty()).isEqualByComparingTo("0");
        assertThat(legacy.getOrderStatus()).isEqualTo("received");
    }

    // ---------- fetchInStockSupplies ----------

    @Test
    public void fetchInStockSupplies_onlyIncludesPositiveInStock() {
        Supply empty = newSupply(3L, "Empty", "flour", "kg",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                "received", BigDecimal.ZERO);
        when(repository.countSupplies()).thenReturn(2L);
        when(repository.findAllSupplies()).thenReturn(List.of(flour, empty));

        List<Map<String, Object>> result = service.fetchInStockSupplies();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name")).isEqualTo("Finger Millet Flour");
    }

    // ---------- saveSupply ----------

    @Test
    public void saveSupply_createsNewWhenIdAbsent() {
        when(repository.findSupplyByName("Vanilla")).thenReturn(Optional.empty());
        when(repository.saveSupply(any())).thenAnswer(inv -> {
            Supply s = inv.getArgument(0);
            s.setId(99L);
            return s;
        });

        Map<String, Object> body = new HashMap<>();
        body.put("name", "Vanilla");
        body.put("unit", "ml");
        body.put("category", "flavour");
        body.put("inStock", "500");
        body.put("threshold", "100");

        Map<String, Object> out = service.saveSupply(body);

        assertThat(out.get("id")).isEqualTo(99L);
        assertThat(out.get("unit")).isEqualTo("ml");
        assertThat(out.get("category")).isEqualTo("flavour");
        assertThat(out.get("orderStatus")).isEqualTo("received");
    }

    @Test
    public void saveSupply_missingName_throws() {
        Map<String, Object> body = Map.of("unit", "kg");

        assertThatThrownBy(() -> service.saveSupply(body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    public void saveSupply_missingUnit_throws() {
        Map<String, Object> body = Map.of("name", "X");

        assertThatThrownBy(() -> service.saveSupply(body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unit");
    }

    @Test
    public void saveSupply_invalidUnit_throws() {
        Map<String, Object> body = Map.of("name", "X", "unit", "bucket");

        assertThatThrownBy(() -> service.saveSupply(body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid unit");
    }

    @Test
    public void saveSupply_invalidCategory_throws() {
        Map<String, Object> body = Map.of(
                "name", "X", "unit", "kg", "category", "spaceship");

        assertThatThrownBy(() -> service.saveSupply(body))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid category");
    }

    @Test
    public void saveSupply_nullBody_throws() {
        assertThatThrownBy(() -> service.saveSupply(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- updateSupplyOrderStatuses ----------

    @Test
    public void updateStatus_received_movesDeliveredIntoPantryAndResetsRequested() {
        Supply s = newSupply(5L, "Honey", "sweetener", "l",
                new BigDecimal("2.0"),   // pantry already has 2
                new BigDecimal("3.0"),   // 3 delivered
                new BigDecimal("0.5"),
                "waiting",
                new BigDecimal("3.0"));
        when(repository.findSupplyById(5L)).thenReturn(Optional.of(s));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateSupplyOrderStatuses(List.of(
                Map.of("id", 5, "orderStatus", "received")));

        assertThat(s.getInStock()).isEqualByComparingTo("5.0");
        assertThat(s.getCurrentStock()).isEqualByComparingTo("0");
        assertThat(s.getRequestedQty()).isEqualByComparingTo("0");
        assertThat(s.getOrderStatus()).isEqualTo("received");
        assertThat(s.getRequestedAt()).isNull();
    }

    @Test
    public void updateStatus_waitingWithQty_setsRequestedQtyAndStamp() {
        when(repository.findSupplyById(1L)).thenReturn(Optional.of(flour));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateSupplyOrderStatuses(List.of(
                Map.of("id", 1, "orderStatus", "waiting", "requestedQty", 4)));

        assertThat(flour.getOrderStatus()).isEqualTo("waiting");
        assertThat(flour.getRequestedQty()).isEqualByComparingTo("4");
        assertThat(flour.getRequestedAt()).isNotNull();
    }

    @Test
    public void updateStatus_negativeQty_clampsToZero() {
        when(repository.findSupplyById(1L)).thenReturn(Optional.of(flour));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateSupplyOrderStatuses(List.of(
                Map.of("id", 1, "orderStatus", "urgency", "requestedQty", -5)));

        assertThat(flour.getRequestedQty()).isEqualByComparingTo("0");
        assertThat(flour.getOrderStatus()).isEqualTo("urgency");
    }

    @Test
    public void updateStatus_invalidStatus_throws() {
        List<Map<String, Object>> updates = List.of(
                Map.of("id", 1, "orderStatus", "pending"));

        assertThatThrownBy(() -> service.updateSupplyOrderStatuses(updates))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid orderStatus");
    }

    @Test
    public void updateStatus_emptyList_throws() {
        assertThatThrownBy(() -> service.updateSupplyOrderStatuses(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.updateSupplyOrderStatuses(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- requestMoreSupply ----------

    @Test
    public void requestMoreSupply_positiveQty_setsRequestAndStatus() {
        when(repository.findSupplyById(1L)).thenReturn(Optional.of(flour));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        service.requestMoreSupply(1L, new BigDecimal("3"), "urgency");

        assertThat(flour.getRequestedQty()).isEqualByComparingTo("3");
        assertThat(flour.getOrderStatus()).isEqualTo("urgency");
        assertThat(flour.getRequestedAt()).isNotNull();
    }

    @Test
    public void requestMoreSupply_nullUrgency_defaultsToWaiting() {
        when(repository.findSupplyById(1L)).thenReturn(Optional.of(flour));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        service.requestMoreSupply(1L, new BigDecimal("2"), null);

        assertThat(flour.getOrderStatus()).isEqualTo("waiting");
    }

    @Test
    public void requestMoreSupply_receivedUrgency_coercedToWaiting() {
        when(repository.findSupplyById(1L)).thenReturn(Optional.of(flour));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        service.requestMoreSupply(1L, new BigDecimal("2"), "received");

        assertThat(flour.getOrderStatus()).isEqualTo("waiting");
    }

    @Test
    public void requestMoreSupply_zeroOrNegativeQty_throws() {
        assertThatThrownBy(() ->
                service.requestMoreSupply(1L, BigDecimal.ZERO, "waiting"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                service.requestMoreSupply(1L, new BigDecimal("-1"), "waiting"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() ->
                service.requestMoreSupply(1L, null, "waiting"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void requestMoreSupply_unknownId_throws() {
        when(repository.findSupplyById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.requestMoreSupply(404L, new BigDecimal("1"), "waiting"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supply not found");
    }

    // ---------- fulfillSupply ----------

    @Test
    public void fulfillSupply_incrementsCurrentStock() {
        flour.setCurrentStock(new BigDecimal("1"));
        when(repository.findSupplyById(1L)).thenReturn(Optional.of(flour));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        service.fulfillSupply(1L, new BigDecimal("4"), "delivered");

        assertThat(flour.getCurrentStock()).isEqualByComparingTo("5");
        assertThat(flour.getNotes()).isEqualTo("delivered");
    }

    @Test
    public void fulfillSupply_nonPositiveQty_throws() {
        assertThatThrownBy(() -> service.fulfillSupply(1L, BigDecimal.ZERO, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.fulfillSupply(1L, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- adjustSupplyStock ----------

    @Test
    public void adjustSupplyStock_positiveDelta_increments() {
        flour.setCurrentStock(new BigDecimal("2"));
        when(repository.findSupplyById(1L)).thenReturn(Optional.of(flour));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        service.adjustSupplyStock(1L, new BigDecimal("3"), null);

        assertThat(flour.getCurrentStock()).isEqualByComparingTo("5");
    }

    @Test
    public void adjustSupplyStock_clampsToZero() {
        flour.setCurrentStock(new BigDecimal("1"));
        when(repository.findSupplyById(1L)).thenReturn(Optional.of(flour));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        service.adjustSupplyStock(1L, new BigDecimal("-5"), null);

        assertThat(flour.getCurrentStock()).isEqualByComparingTo("0");
    }

    @Test
    public void adjustSupplyStock_nullDelta_throws() {
        assertThatThrownBy(() -> service.adjustSupplyStock(1L, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- seedSuppliesIfEmpty ----------

    @Test
    public void seedSupplies_whenEmpty_seedsAll() {
        when(repository.countSupplies()).thenReturn(0L);
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.seedSuppliesIfEmpty();

        assertThat(((Number) result.get("seeded")).intValue()).isGreaterThan(20);
        verify(repository, times(((Number) result.get("seeded")).intValue()))
                .saveSupply(any());
    }

    @Test
    public void seedSupplies_whenNotEmpty_skips() {
        when(repository.countSupplies()).thenReturn(5L);

        Map<String, Object> result = service.seedSuppliesIfEmpty();

        assertThat(result.get("seeded")).isEqualTo(0);
        assertThat(result.get("existing")).isEqualTo(5L);
        verify(repository, never()).saveSupply(any());
    }

    // ---------- fetchRoleNamesForUser ----------

    @Test
    public void fetchRoleNamesForUser_returnsLowercaseDistinct() {
        Role r1 = new Role(); r1.setRole("Customer");
        Role r2 = new Role(); r2.setRole("customer"); // duplicate casing
        Role r3 = new Role(); r3.setRole("EMPLOYEE");
        when(repository.findRolesByUserId(1)).thenReturn(List.of(r1, r2, r3));

        List<String> roles = service.fetchRoleNamesForUser(1);

        assertThat(roles).containsExactlyInAnyOrder("customer", "employee");
    }

    @Test
    public void fetchRoleNamesForUser_filtersBlankRoles() {
        Role r1 = new Role(); r1.setRole("");
        Role r2 = new Role(); r2.setRole(null);
        when(repository.findRolesByUserId(1)).thenReturn(new ArrayList<>(List.of(r1, r2)));

        assertThat(service.fetchRoleNamesForUser(1)).isEmpty();
    }

    // ---------- test helpers ----------

    private static Supply newSupply(Long id, String name, String category, String unit,
                                    BigDecimal inStock, BigDecimal currentStock,
                                    BigDecimal threshold, String status,
                                    BigDecimal requestedQty) {
        Supply s = new Supply();
        s.setId(id);
        s.setName(name);
        s.setCategory(category);
        s.setUnit(unit);
        s.setInStock(inStock);
        s.setCurrentStock(currentStock);
        s.setThreshold(threshold);
        s.setOrderStatus(status);
        s.setRequestedQty(requestedQty);
        return s;
    }

    private static Product newProduct(String name, BigDecimal price) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(price);
        return p;
    }

    private static Product productWithId(long id, String name, BigDecimal price) {
        Product p = newProduct(name, price);
        p.setId(id);
        return p;
    }

    private static Map<String, Object> saleItem(long productId, int quantity) {
        Map<String, Object> m = new HashMap<>();
        m.put("productId", productId);
        m.put("quantity", quantity);
        return m;
    }

    // ---------- recordCounterSale: walk-in POS ----------

    @Test
    public void recordCounterSale_cash_computesTotalAndChange() {
        Product cookie = productWithId(1L, "Cookie", new BigDecimal("10.00"));
        when(repository.findProductById(1L)).thenReturn(Optional.of(cookie));
        when(repository.saveOrder(any())).thenAnswer(inv -> {
            com.example.groceryapi.model.Orders o = inv.getArgument(0);
            o.setId(101L);
            return o;
        });
        when(repository.savePayment(any())).thenAnswer(inv -> {
            com.example.groceryapi.model.Payment p = inv.getArgument(0);
            p.setId(202L);
            return p;
        });

        Map<String, Object> result = service.recordCounterSale(
                List.of(saleItem(1L, 3)),
                "CASH",
                new BigDecimal("50.00"),
                "Anita",
                null);

        assertThat(result.get("orderId")).isEqualTo(101L);
        assertThat(result.get("paymentId")).isEqualTo(202L);
        assertThat((BigDecimal) result.get("totalAmount")).isEqualByComparingTo("30.00");
        assertThat((BigDecimal) result.get("changeDue")).isEqualByComparingTo("20.00");
        assertThat(result.get("paymentMethod")).isEqualTo("CASH");
        assertThat(result.get("paymentStatus")).isEqualTo("SUCCESS");
        assertThat(result.get("status")).isEqualTo("CONFIRMED");
        assertThat(result.get("channel")).isEqualTo("instore");
        verify(repository, times(1)).saveOrder(any());
        verify(repository, times(1)).saveOrderItem(any());
        verify(repository, times(1)).savePayment(any());
    }

    @Test
    public void recordCounterSale_card_doesNotRequireCashGiven() {
        Product cake = productWithId(2L, "Cake", new BigDecimal("100.00"));
        when(repository.findProductById(2L)).thenReturn(Optional.of(cake));
        when(repository.saveOrder(any())).thenAnswer(inv -> {
            com.example.groceryapi.model.Orders o = inv.getArgument(0);
            o.setId(11L);
            return o;
        });
        when(repository.savePayment(any())).thenAnswer(inv -> {
            com.example.groceryapi.model.Payment p = inv.getArgument(0);
            p.setId(22L);
            return p;
        });

        Map<String, Object> result = service.recordCounterSale(
                List.of(saleItem(2L, 1)),
                "CREDIT_CARD",
                null,
                null,
                null);

        assertThat((BigDecimal) result.get("totalAmount")).isEqualByComparingTo("100.00");
        assertThat((BigDecimal) result.get("changeDue")).isEqualByComparingTo("0");
        assertThat(result.get("paymentMethod")).isEqualTo("CREDIT_CARD");
    }

    @Test
    public void recordCounterSale_emptyItems_throws() {
        assertThatThrownBy(() -> service.recordCounterSale(
                List.of(), "CASH", new BigDecimal("10"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one item");
        verify(repository, never()).saveOrder(any());
    }

    @Test
    public void recordCounterSale_nullItems_throws() {
        assertThatThrownBy(() -> service.recordCounterSale(
                null, "CASH", new BigDecimal("10"), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void recordCounterSale_invalidPaymentMethod_throws() {
        assertThatThrownBy(() -> service.recordCounterSale(
                List.of(saleItem(1L, 1)), "BITCOIN", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid payment method");
        verify(repository, never()).findProductById(any(Long.class));
    }

    @Test
    public void recordCounterSale_zeroQuantity_throws() {
        assertThatThrownBy(() -> service.recordCounterSale(
                List.of(saleItem(1L, 0)), "CASH", new BigDecimal("10"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    @Test
    public void recordCounterSale_unknownProduct_throws() {
        when(repository.findProductById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.recordCounterSale(
                List.of(saleItem(99L, 1)), "CASH", new BigDecimal("10"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
        verify(repository, never()).saveOrder(any());
    }

    @Test
    public void recordCounterSale_cashLessThanTotal_throws() {
        Product cookie = productWithId(1L, "Cookie", new BigDecimal("10.00"));
        when(repository.findProductById(1L)).thenReturn(Optional.of(cookie));

        assertThatThrownBy(() -> service.recordCounterSale(
                List.of(saleItem(1L, 3)),
                "CASH",
                new BigDecimal("20.00"),
                null,
                null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cash given must be at least the total");
        verify(repository, never()).saveOrder(any());
    }

    @Test
    public void recordCounterSale_cashWithoutCashGiven_throws() {
        Product cookie = productWithId(1L, "Cookie", new BigDecimal("10.00"));
        when(repository.findProductById(1L)).thenReturn(Optional.of(cookie));

        assertThatThrownBy(() -> service.recordCounterSale(
                List.of(saleItem(1L, 1)), "CASH", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void recordCounterSale_customerName_appearsInCustomerNotes() {
        Product cookie = productWithId(1L, "Cookie", new BigDecimal("5.00"));
        when(repository.findProductById(1L)).thenReturn(Optional.of(cookie));
        final com.example.groceryapi.model.Orders[] saved = new com.example.groceryapi.model.Orders[1];
        when(repository.saveOrder(any())).thenAnswer(inv -> {
            com.example.groceryapi.model.Orders o = inv.getArgument(0);
            o.setId(7L);
            saved[0] = o;
            return o;
        });
        when(repository.savePayment(any())).thenAnswer(inv -> {
            com.example.groceryapi.model.Payment p = inv.getArgument(0);
            p.setId(8L);
            return p;
        });

        service.recordCounterSale(
                List.of(saleItem(1L, 1)),
                "CASH",
                new BigDecimal("5.00"),
                "Bob",
                "no nuts");

        assertThat(saved[0].getCustomerNotes()).contains("Bob").contains("no nuts");
        assertThat(saved[0].getChannel()).isEqualTo("instore");
        assertThat(saved[0].getKitchenStatus()).isEqualTo("pending");
        assertThat(saved[0].getUser()).isNull();
    }

    private static Role customerRole() {
        Role r = new Role();
        r.setRole("customer");
        r.setFullName("Customer");
        return r;
    }

    // ---------- salesAnalytics ----------

    private static Orders order(long id, BigDecimal total, String channel, String status, LocalDateTime createdAt) {
        Orders o = new Orders();
        o.setId(id);
        o.setTotalAmount(total);
        o.setChannel(channel);
        o.setStatus(status);
        o.setCreatedAt(createdAt);
        return o;
    }

    private static Payment payment(Orders o, String method, String status, BigDecimal amount) {
        Payment p = new Payment();
        p.setOrder(o);
        p.setPaymentMethod(method);
        p.setStatus(status);
        p.setAmount(amount);
        return p;
    }

    private static OrderItem orderItem(Orders o, Product product, int qty, BigDecimal unitPrice) {
        OrderItem oi = new OrderItem();
        oi.setOrder(o);
        oi.setProduct(product);
        oi.setQuantity(qty);
        oi.setPrice(unitPrice);
        return oi;
    }

    @Test
    public void salesAnalytics_emptyDb_returnsZeroTotalsAndDailyTrendForEveryDay() {
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of());
        when(repository.findOrderItemsByOrderIds(any())).thenReturn(List.of());
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of());

        Map<String, Object> r = service.salesAnalytics("2026-04-01", "2026-04-03");

        assertThat(r.get("from")).isEqualTo("2026-04-01");
        assertThat(r.get("to")).isEqualTo("2026-04-03");
        assertThat((BigDecimal) r.get("totalRevenue")).isEqualByComparingTo("0");
        assertThat(r.get("orderCount")).isEqualTo(0);
        assertThat((BigDecimal) r.get("avgOrderValue")).isEqualByComparingTo("0");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) r.get("dailyTrend");
        assertThat(trend).hasSize(3);
        assertThat(trend.get(0).get("date")).isEqualTo("2026-04-01");
        assertThat(trend.get(2).get("date")).isEqualTo("2026-04-03");
    }

    @Test
    public void salesAnalytics_aggregatesRevenueOrderCountAvg() {
        Orders o1 = order(1L, new BigDecimal("100.00"), "online",  "CONFIRMED", LocalDateTime.of(2026, 4, 2, 10, 0));
        Orders o2 = order(2L, new BigDecimal("50.00"),  "instore", "CONFIRMED", LocalDateTime.of(2026, 4, 2, 12, 0));
        Orders o3 = order(3L, new BigDecimal("250.00"), "online",  "CANCELLED", LocalDateTime.of(2026, 4, 3, 9, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(o1, o2, o3));
        when(repository.findOrderItemsByOrderIds(any())).thenReturn(List.of());
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of());

        Map<String, Object> r = service.salesAnalytics("2026-04-01", "2026-04-05");

        assertThat((BigDecimal) r.get("totalRevenue")).isEqualByComparingTo("400.00");
        assertThat(r.get("orderCount")).isEqualTo(3);
        assertThat((BigDecimal) r.get("avgOrderValue")).isEqualByComparingTo("133.33");
    }

    @Test
    public void salesAnalytics_groupsRevenueByChannel() {
        Orders o1 = order(1L, new BigDecimal("100"), "online",  "CONFIRMED", LocalDateTime.of(2026, 4, 2, 10, 0));
        Orders o2 = order(2L, new BigDecimal("60"),  "instore", "CONFIRMED", LocalDateTime.of(2026, 4, 2, 11, 0));
        Orders o3 = order(3L, new BigDecimal("40"),  "online",  "CONFIRMED", LocalDateTime.of(2026, 4, 3, 10, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(o1, o2, o3));
        when(repository.findOrderItemsByOrderIds(any())).thenReturn(List.of());
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of());

        Map<String, Object> r = service.salesAnalytics("2026-04-01", "2026-04-05");

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> byChannel = (Map<String, BigDecimal>) r.get("revenueByChannel");
        assertThat(byChannel.get("online")).isEqualByComparingTo("140");
        assertThat(byChannel.get("instore")).isEqualByComparingTo("60");
    }

    @Test
    public void salesAnalytics_groupsRevenueByPaymentMethod_ignoresFailedPayments() {
        Orders o1 = order(1L, new BigDecimal("100"), "online", "CONFIRMED", LocalDateTime.of(2026, 4, 2, 10, 0));
        Orders o2 = order(2L, new BigDecimal("50"),  "online", "CONFIRMED", LocalDateTime.of(2026, 4, 2, 11, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(o1, o2));
        when(repository.findOrderItemsByOrderIds(any())).thenReturn(List.of());
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of(
                payment(o1, "CREDIT_CARD", "SUCCESS", new BigDecimal("100")),
                payment(o2, "CASH",        "FAILED",  new BigDecimal("50"))
        ));

        Map<String, Object> r = service.salesAnalytics("2026-04-01", "2026-04-05");

        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> byMethod = (Map<String, BigDecimal>) r.get("revenueByPaymentMethod");
        assertThat(byMethod).containsKey("CREDIT_CARD");
        assertThat(byMethod.get("CREDIT_CARD")).isEqualByComparingTo("100");
        assertThat(byMethod).doesNotContainKey("CASH");
    }

    @Test
    public void salesAnalytics_countsOrdersByStatus() {
        Orders o1 = order(1L, new BigDecimal("10"), "online", "CONFIRMED", LocalDateTime.of(2026, 4, 2, 10, 0));
        Orders o2 = order(2L, new BigDecimal("10"), "online", "CONFIRMED", LocalDateTime.of(2026, 4, 2, 11, 0));
        Orders o3 = order(3L, new BigDecimal("10"), "online", "CANCELLED", LocalDateTime.of(2026, 4, 2, 12, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(o1, o2, o3));
        when(repository.findOrderItemsByOrderIds(any())).thenReturn(List.of());
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of());

        Map<String, Object> r = service.salesAnalytics("2026-04-01", "2026-04-05");

        @SuppressWarnings("unchecked")
        Map<String, Integer> byStatus = (Map<String, Integer>) r.get("ordersByStatus");
        assertThat(byStatus.get("CONFIRMED")).isEqualTo(2);
        assertThat(byStatus.get("CANCELLED")).isEqualTo(1);
    }

    @Test
    public void salesAnalytics_topProducts_sortedByRevenueDescAndLimited() {
        Orders o = order(1L, new BigDecimal("0"), "online", "CONFIRMED", LocalDateTime.of(2026, 4, 2, 10, 0));
        Product cookie = productWithId(1L, "Cookie", new BigDecimal("10"));
        Product cake   = productWithId(2L, "Cake",   new BigDecimal("100"));
        Product bread  = productWithId(3L, "Bread",  new BigDecimal("50"));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(o));
        when(repository.findOrderItemsByOrderIds(any())).thenReturn(List.of(
                orderItem(o, cookie, 5, new BigDecimal("10")),   // 50
                orderItem(o, cake,   2, new BigDecimal("100")),  // 200
                orderItem(o, bread,  3, new BigDecimal("50"))    // 150
        ));
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of());

        Map<String, Object> r = service.salesAnalytics("2026-04-01", "2026-04-05");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> top = (List<Map<String, Object>>) r.get("topProducts");
        assertThat(top).hasSize(3);
        assertThat(top.get(0).get("name")).isEqualTo("Cake");
        assertThat(top.get(1).get("name")).isEqualTo("Bread");
        assertThat(top.get(2).get("name")).isEqualTo("Cookie");
        assertThat((BigDecimal) top.get(0).get("revenue")).isEqualByComparingTo("200");
    }

    @Test
    public void salesAnalytics_dailyTrend_fillsGapsWithZero() {
        Orders o1 = order(1L, new BigDecimal("80"), "online", "CONFIRMED", LocalDateTime.of(2026, 4, 1, 10, 0));
        Orders o2 = order(2L, new BigDecimal("20"), "online", "CONFIRMED", LocalDateTime.of(2026, 4, 3, 14, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(o1, o2));
        when(repository.findOrderItemsByOrderIds(any())).thenReturn(List.of());
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of());

        Map<String, Object> r = service.salesAnalytics("2026-04-01", "2026-04-03");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) r.get("dailyTrend");
        assertThat(trend).hasSize(3);
        assertThat((BigDecimal) trend.get(0).get("revenue")).isEqualByComparingTo("80");
        assertThat((BigDecimal) trend.get(1).get("revenue")).isEqualByComparingTo("0");
        assertThat((BigDecimal) trend.get(2).get("revenue")).isEqualByComparingTo("20");
        assertThat(trend.get(1).get("orderCount")).isEqualTo(0);
    }

    @Test
    public void salesAnalytics_defaultsToLast30Days_whenNoDatesGiven() {
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of());
        when(repository.findOrderItemsByOrderIds(any())).thenReturn(List.of());
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of());

        Map<String, Object> r = service.salesAnalytics(null, null);

        LocalDate today = LocalDate.now();
        assertThat(r.get("from")).isEqualTo(today.minusDays(29).toString());
        assertThat(r.get("to")).isEqualTo(today.toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) r.get("dailyTrend");
        assertThat(trend).hasSize(30);
    }

    @Test
    public void salesAnalytics_invalidFromDate_throws() {
        assertThatThrownBy(() -> service.salesAnalytics("not-a-date", "2026-04-05"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("from");
    }

    @Test
    public void salesAnalytics_invalidToDate_throws() {
        assertThatThrownBy(() -> service.salesAnalytics("2026-04-01", "garbage"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("to");
    }

    @Test
    public void salesAnalytics_fromAfterTo_throws() {
        assertThatThrownBy(() -> service.salesAnalytics("2026-04-10", "2026-04-01"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'from'");
    }

    // ---------- Tasks: createTask / listTasks / updateTaskStatus ----------

    private static User userWithId(int id, String name) {
        User u = new User();
        u.setuserid(id);
        u.setname(name);
        return u;
    }

    private static Task taskOf(long id, String dept, String status, String priority,
                               User creator, User assignee) {
        Task t = new Task();
        t.setId(id);
        t.setAssignedToDepartment(dept);
        t.setStatus(status);
        t.setPriority(priority);
        t.setCreatedBy(creator);
        t.setAssignedToUser(assignee);
        t.setTitle("t" + id);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    public void createTask_savesAndReturnsTaskMap_withDefaultPriorityAndOpenStatus() {
        User creator = userWithId(1, "Sara Sales");
        when(repository.findUserById(1)).thenReturn(Optional.of(creator));
        when(repository.saveTask(any())).thenAnswer(inv -> {
            Task t = inv.getArgument(0);
            t.setId(99L);
            return t;
        });

        Map<String, Object> result = service.createTask(
                1, "kitchen", null, "Bake 200 cookies",
                "for ABC Tech", null, "2026-05-01", null);

        assertThat(result.get("id")).isEqualTo(99L);
        assertThat(result.get("title")).isEqualTo("Bake 200 cookies");
        assertThat(result.get("assignedToDepartment")).isEqualTo("kitchen");
        assertThat(result.get("priority")).isEqualTo("normal");
        assertThat(result.get("status")).isEqualTo("open");
        assertThat(result.get("createdByName")).isEqualTo("Sara Sales");
        assertThat(result.get("dueDate")).isEqualTo("2026-05-01");
        verify(repository, times(1)).saveTask(any());
    }

    @Test
    public void createTask_blankTitle_throws() {
        assertThatThrownBy(() -> service.createTask(
                1, "kitchen", null, "  ", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("title");
        verify(repository, never()).saveTask(any());
    }

    @Test
    public void createTask_titleTooLong_throws() {
        String longTitle = "x".repeat(201);
        assertThatThrownBy(() -> service.createTask(
                1, "kitchen", null, longTitle, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("200");
    }

    @Test
    public void createTask_invalidDepartment_throws() {
        assertThatThrownBy(() -> service.createTask(
                1, "marketing", null, "title", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("assignedToDepartment");
    }

    @Test
    public void createTask_invalidPriority_throws() {
        assertThatThrownBy(() -> service.createTask(
                1, "kitchen", null, "title", null, "yesterday", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("priority");
    }

    @Test
    public void createTask_invalidDueDate_throws() {
        assertThatThrownBy(() -> service.createTask(
                1, "kitchen", null, "title", null, "high", "not-a-date", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dueDate");
    }

    @Test
    public void createTask_unknownCreator_throws() {
        when(repository.findUserById(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createTask(
                1, "kitchen", null, "title", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Creator");
    }

    @Test
    public void createTask_unknownAssignee_throws() {
        when(repository.findUserById(1)).thenReturn(Optional.of(userWithId(1, "Sara")));
        when(repository.findUserById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createTask(
                1, "kitchen", 99, "title", null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Assignee");
    }

    @Test
    public void createTask_unknownRelatedOrder_throws() {
        when(repository.findUserById(1)).thenReturn(Optional.of(userWithId(1, "Sara")));
        when(repository.findOrderById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createTask(
                1, "kitchen", null, "title", null, null, null, 404L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Related order");
    }

    @Test
    public void listTasks_byDepartment_filtersAndMaps() {
        User sara = userWithId(1, "Sara");
        when(repository.findTasksByDepartment("kitchen")).thenReturn(List.of(
                taskOf(1L, "kitchen", "open",        "high",   sara, null),
                taskOf(2L, "kitchen", "in_progress", "normal", sara, null)
        ));
        List<Map<String, Object>> result = service.listTasks("kitchen", null, null);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("assignedToDepartment")).isEqualTo("kitchen");
    }

    @Test
    public void listTasks_byDepartmentAndStatus_filtersBoth() {
        User sara = userWithId(1, "Sara");
        when(repository.findTasksByDepartment("bakery")).thenReturn(List.of(
                taskOf(1L, "bakery", "open", "normal", sara, null),
                taskOf(2L, "bakery", "done", "normal", sara, null),
                taskOf(3L, "bakery", "open", "high",   sara, null)
        ));
        List<Map<String, Object>> result = service.listTasks("bakery", null, "open");
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(m -> "open".equals(m.get("status")));
    }

    @Test
    public void listTasks_byCreatedBy_returnsOnlyMine() {
        User sara = userWithId(1, "Sara");
        when(repository.findTasksByCreatedBy(1)).thenReturn(List.of(
                taskOf(10L, "kitchen", "open", "normal", sara, null)
        ));
        List<Map<String, Object>> result = service.listTasks(null, 1, null);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("id")).isEqualTo(10L);
    }

    @Test
    public void listTasks_invalidDepartment_throws() {
        assertThatThrownBy(() -> service.listTasks("space", null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void listTasks_invalidStatus_throws() {
        assertThatThrownBy(() -> service.listTasks("kitchen", null, "burning"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void updateTaskStatus_open_to_inProgress_succeeds_andSetsUpdatedAt() {
        User sara = userWithId(1, "Sara");
        Task t = taskOf(5L, "kitchen", "open", "normal", sara, null);
        when(repository.findTaskById(5L)).thenReturn(Optional.of(t));
        when(repository.saveTask(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.updateTaskStatus(5L, "in_progress", null, null);

        assertThat(result.get("status")).isEqualTo("in_progress");
        assertThat(t.getUpdatedAt()).isNotNull();
        assertThat(t.getCompletedAt()).isNull();
    }

    @Test
    public void updateTaskStatus_done_setsCompletedAtAndCompletedBy_andResolutionNotes() {
        User sara = userWithId(1, "Sara");
        User baker = userWithId(2, "Bella");
        Task t = taskOf(5L, "kitchen", "in_progress", "normal", sara, null);
        when(repository.findTaskById(5L)).thenReturn(Optional.of(t));
        when(repository.findUserById(2)).thenReturn(Optional.of(baker));
        when(repository.saveTask(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.updateTaskStatus(5L, "done", 2, "delivered");

        assertThat(result.get("status")).isEqualTo("done");
        assertThat(result.get("completedByName")).isEqualTo("Bella");
        assertThat(result.get("resolutionNotes")).isEqualTo("delivered");
        assertThat(t.getCompletedAt()).isNotNull();
    }

    @Test
    public void updateTaskStatus_alreadyDone_throws() {
        User sara = userWithId(1, "Sara");
        Task t = taskOf(5L, "kitchen", "done", "normal", sara, null);
        when(repository.findTaskById(5L)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.updateTaskStatus(5L, "in_progress", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already done");
        verify(repository, never()).saveTask(any());
    }

    @Test
    public void updateTaskStatus_invalidStatus_throws() {
        assertThatThrownBy(() -> service.updateTaskStatus(5L, "wibble", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
    }

    @Test
    public void updateTaskStatus_unknownTask_throws() {
        when(repository.findTaskById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateTaskStatus(999L, "in_progress", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Task not found");
    }

    // ---------- Delivery: trips, issues, shift summary ----------

    private static Orders readyOnlineOrder(long id) {
        Orders o = new Orders();
        o.setId(id);
        o.setChannel("online");
        o.setKitchenStatus("done");
        o.setStatus("confirmed");
        return o;
    }

    private static DeliveryTrip tripOf(long id, Orders order, User driver, String status) {
        DeliveryTrip t = new DeliveryTrip();
        t.setId(id);
        t.setOrder(order);
        t.setDriver(driver);
        t.setStatus(status);
        t.setCreatedAt(LocalDateTime.now());
        return t;
    }

    @Test
    public void pickUpTrip_readyOnlineOrder_createsTrip_andUpdatesOrderStatus() {
        Orders o = readyOnlineOrder(101L);
        User driver = userWithId(7, "Dan Driver");
        when(repository.findOrderById(101L)).thenReturn(Optional.of(o));
        when(repository.findTripByOrderId(101L)).thenReturn(Optional.empty());
        when(repository.findUserById(7)).thenReturn(Optional.of(driver));
        when(repository.saveOrder(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveTrip(any())).thenAnswer(inv -> {
            DeliveryTrip t = inv.getArgument(0);
            t.setId(500L);
            return t;
        });

        Map<String, Object> result = service.pickUpTrip(101L, 7);

        assertThat(result.get("id")).isEqualTo(500L);
        assertThat(result.get("status")).isEqualTo("picked_up");
        assertThat(result.get("orderId")).isEqualTo(101L);
        assertThat(result.get("driverName")).isEqualTo("Dan Driver");
        assertThat(o.getKitchenStatus()).isEqualTo("picked_up");
    }

    @Test
    public void pickUpTrip_orderNotFound_throws() {
        when(repository.findOrderById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.pickUpTrip(404L, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
        verify(repository, never()).saveTrip(any());
    }

    @Test
    public void pickUpTrip_instoreOrder_rejected() {
        Orders o = readyOnlineOrder(1L);
        o.setChannel("instore");
        when(repository.findOrderById(1L)).thenReturn(Optional.of(o));
        assertThatThrownBy(() -> service.pickUpTrip(1L, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("online");
    }

    @Test
    public void pickUpTrip_kitchenNotDone_rejected() {
        Orders o = readyOnlineOrder(1L);
        o.setKitchenStatus("preparing");
        when(repository.findOrderById(1L)).thenReturn(Optional.of(o));
        assertThatThrownBy(() -> service.pickUpTrip(1L, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kitchen");
    }

    @Test
    public void pickUpTrip_existingActiveTrip_rejected() {
        Orders o = readyOnlineOrder(1L);
        when(repository.findOrderById(1L)).thenReturn(Optional.of(o));
        when(repository.findTripByOrderId(1L))
                .thenReturn(Optional.of(tripOf(50L, o, userWithId(7, "Dan"), "out_for_delivery")));
        assertThatThrownBy(() -> service.pickUpTrip(1L, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    public void pickUpTrip_unknownDriver_throws() {
        Orders o = readyOnlineOrder(1L);
        when(repository.findOrderById(1L)).thenReturn(Optional.of(o));
        when(repository.findTripByOrderId(1L)).thenReturn(Optional.empty());
        when(repository.findUserById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.pickUpTrip(1L, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Driver not found");
    }

    @Test
    public void markOutForDelivery_fromPickedUp_setsStatusAndOtp() {
        Orders o = readyOnlineOrder(1L);
        o.setKitchenStatus("picked_up");
        User driver = userWithId(7, "Dan");
        DeliveryTrip t = tripOf(10L, o, driver, "picked_up");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        when(repository.saveOrder(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveTrip(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.markOutForDelivery(10L, 7);

        assertThat(result.get("status")).isEqualTo("out_for_delivery");
        assertThat((String) result.get("otpCode")).hasSize(4);
        assertThat(o.getKitchenStatus()).isEqualTo("out_for_delivery");
    }

    @Test
    public void markOutForDelivery_wrongStatus_throws() {
        DeliveryTrip t = tripOf(10L, readyOnlineOrder(1L), userWithId(7, "Dan"), "delivered");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.markOutForDelivery(10L, 7))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("picked_up");
    }

    @Test
    public void markOutForDelivery_wrongDriver_throws() {
        DeliveryTrip t = tripOf(10L, readyOnlineOrder(1L), userWithId(7, "Dan"), "picked_up");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.markOutForDelivery(10L, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not assigned to you");
    }

    @Test
    public void markDelivered_withMatchingOtp_andCod_setsStatusAndCollectsCash() {
        Orders o = readyOnlineOrder(1L);
        o.setKitchenStatus("out_for_delivery");
        DeliveryTrip t = tripOf(10L, o, userWithId(7, "Dan"), "out_for_delivery");
        t.setOtpCode("1234");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        when(repository.saveTrip(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveOrder(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.markDelivered(10L, 7,
                "1234", null,
                new BigDecimal("250.00"), new BigDecimal("20.00"), new BigDecimal("3.5"),
                "left at door");

        assertThat(result.get("status")).isEqualTo("delivered");
        assertThat((BigDecimal) result.get("codAmount")).isEqualByComparingTo("250.00");
        assertThat((BigDecimal) result.get("tipAmount")).isEqualByComparingTo("20.00");
        assertThat(o.getStatus()).isEqualTo("delivered");
        assertThat(o.getKitchenStatus()).isEqualTo("delivered");
    }

    @Test
    public void markDelivered_withPhotoOnly_succeeds() {
        Orders o = readyOnlineOrder(1L);
        DeliveryTrip t = tripOf(10L, o, userWithId(7, "Dan"), "out_for_delivery");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        when(repository.saveTrip(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveOrder(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.markDelivered(10L, 7,
                null, "https://photos/proof.jpg", null, null, null, null);

        assertThat(result.get("status")).isEqualTo("delivered");
        assertThat(result.get("photoProofUrl")).isEqualTo("https://photos/proof.jpg");
    }

    @Test
    public void markDelivered_noProof_throws() {
        DeliveryTrip t = tripOf(10L, readyOnlineOrder(1L), userWithId(7, "Dan"), "out_for_delivery");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.markDelivered(10L, 7,
                null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Proof");
    }

    @Test
    public void markDelivered_otpMismatch_throws() {
        DeliveryTrip t = tripOf(10L, readyOnlineOrder(1L), userWithId(7, "Dan"), "out_for_delivery");
        t.setOtpCode("1234");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.markDelivered(10L, 7,
                "9999", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OTP");
    }

    @Test
    public void markDelivered_negativeCod_throws() {
        DeliveryTrip t = tripOf(10L, readyOnlineOrder(1L), userWithId(7, "Dan"), "out_for_delivery");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.markDelivered(10L, 7,
                null, "u", new BigDecimal("-1"), null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("COD");
    }

    @Test
    public void markDelivered_notOutForDelivery_throws() {
        DeliveryTrip t = tripOf(10L, readyOnlineOrder(1L), userWithId(7, "Dan"), "picked_up");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.markDelivered(10L, 7,
                "1234", null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("out_for_delivery");
    }

    @Test
    public void markTripFailed_validReason_setsFailed_andOrderDeliveryFailed() {
        Orders o = readyOnlineOrder(1L);
        DeliveryTrip t = tripOf(10L, o, userWithId(7, "Dan"), "out_for_delivery");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        when(repository.saveTrip(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveOrder(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.markTripFailed(10L, 7,
                "customer_not_home", "no answer at door");

        assertThat(result.get("status")).isEqualTo("failed");
        assertThat(result.get("failureReason")).isEqualTo("customer_not_home");
        assertThat(o.getKitchenStatus()).isEqualTo("delivery_failed");
    }

    @Test
    public void markTripFailed_invalidReason_throws() {
        assertThatThrownBy(() -> service.markTripFailed(10L, 7, "bored", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
    }

    @Test
    public void markTripFailed_blankReason_throws() {
        assertThatThrownBy(() -> service.markTripFailed(10L, 7, "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }

    @Test
    public void markTripFailed_alreadyDelivered_throws() {
        DeliveryTrip t = tripOf(10L, readyOnlineOrder(1L), userWithId(7, "Dan"), "delivered");
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        assertThatThrownBy(() -> service.markTripFailed(10L, 7, "refused", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delivered");
    }

    @Test
    public void listTripsForDriver_active_filtersToInProgress() {
        User driver = userWithId(7, "Dan");
        when(repository.findActiveTripsByDriver(7)).thenReturn(List.of(
                tripOf(1L, readyOnlineOrder(1L), driver, "picked_up"),
                tripOf(2L, readyOnlineOrder(2L), driver, "out_for_delivery")
        ));
        List<Map<String, Object>> r = service.listTripsForDriver(7, "active");
        assertThat(r).hasSize(2);
    }

    @Test
    public void listTripsForDriver_byStatus_delivered() {
        User driver = userWithId(7, "Dan");
        when(repository.findTripsByDriver(7)).thenReturn(List.of(
                tripOf(1L, readyOnlineOrder(1L), driver, "delivered"),
                tripOf(2L, readyOnlineOrder(2L), driver, "failed"),
                tripOf(3L, readyOnlineOrder(3L), driver, "delivered")
        ));
        List<Map<String, Object>> r = service.listTripsForDriver(7, "delivered");
        assertThat(r).hasSize(2);
    }

    @Test
    public void listTripsForDriver_invalidStatus_throws() {
        assertThatThrownBy(() -> service.listTripsForDriver(7, "wibble"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
    }

    @Test
    public void logDeliveryIssue_validVehicle_savesIssue() {
        User driver = userWithId(7, "Dan");
        when(repository.findUserById(7)).thenReturn(Optional.of(driver));
        when(repository.saveIssue(any())).thenAnswer(inv -> {
            DeliveryIssue i = inv.getArgument(0);
            i.setId(11L);
            return i;
        });
        Map<String, Object> r = service.logDeliveryIssue(7,
                "vehicle_breakdown", "tire flat near sector 5", null);
        assertThat(r.get("id")).isEqualTo(11L);
        assertThat(r.get("issueType")).isEqualTo("vehicle_breakdown");
        assertThat(r.get("driverName")).isEqualTo("Dan");
    }

    @Test
    public void logDeliveryIssue_withTrip_linksTrip() {
        User driver = userWithId(7, "Dan");
        DeliveryTrip t = tripOf(10L, readyOnlineOrder(1L), driver, "out_for_delivery");
        when(repository.findUserById(7)).thenReturn(Optional.of(driver));
        when(repository.findTripById(10L)).thenReturn(Optional.of(t));
        when(repository.saveIssue(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> r = service.logDeliveryIssue(7, "traffic_delay", "jam", 10L);
        assertThat(r.get("tripId")).isEqualTo(10L);
    }

    @Test
    public void logDeliveryIssue_blankDescription_throws() {
        assertThatThrownBy(() -> service.logDeliveryIssue(7, "accident", "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Description");
    }

    @Test
    public void logDeliveryIssue_invalidType_throws() {
        assertThatThrownBy(() -> service.logDeliveryIssue(7, "alien", "x", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("type");
    }

    @Test
    public void logDeliveryIssue_descriptionTooLong_throws() {
        String long501 = "x".repeat(501);
        assertThatThrownBy(() -> service.logDeliveryIssue(7, "accident", long501, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    public void logDeliveryIssue_unknownDriver_throws() {
        when(repository.findUserById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.logDeliveryIssue(99, "accident", "x", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Driver not found");
    }

    @Test
    public void shiftSummary_aggregatesDeliveredFailedCodTipsDistance() {
        User driver = userWithId(7, "Dan");
        DeliveryTrip d1 = tripOf(1L, readyOnlineOrder(1L), driver, "delivered");
        d1.setCodAmount(new BigDecimal("100"));
        d1.setTipAmount(new BigDecimal("20"));
        d1.setDistanceKm(new BigDecimal("3.0"));
        DeliveryTrip d2 = tripOf(2L, readyOnlineOrder(2L), driver, "delivered");
        d2.setCodAmount(new BigDecimal("50"));
        d2.setTipAmount(new BigDecimal("0"));
        d2.setDistanceKm(new BigDecimal("2.5"));
        DeliveryTrip f1 = tripOf(3L, readyOnlineOrder(3L), driver, "failed");
        f1.setFailureReason("customer_not_home");
        DeliveryTrip f2 = tripOf(4L, readyOnlineOrder(4L), driver, "failed");
        f2.setFailureReason("refused");
        DeliveryTrip inflight = tripOf(5L, readyOnlineOrder(5L), driver, "out_for_delivery");
        when(repository.findTripsByDriverInRange(eq(7), any(), any()))
                .thenReturn(List.of(d1, d2, f1, f2, inflight));

        Map<String, Object> r = service.shiftSummary(7, "2026-04-27", "2026-04-27");

        assertThat(r.get("totalTrips")).isEqualTo(5);
        assertThat(r.get("delivered")).isEqualTo(2L);
        assertThat(r.get("failed")).isEqualTo(2L);
        assertThat(r.get("inFlight")).isEqualTo(1L);
        assertThat((BigDecimal) r.get("codCollected")).isEqualByComparingTo("150");
        assertThat((BigDecimal) r.get("tipsTotal")).isEqualByComparingTo("20");
        assertThat((BigDecimal) r.get("distanceKm")).isEqualByComparingTo("5.5");
        @SuppressWarnings("unchecked")
        Map<String, Long> reasons = (Map<String, Long>) r.get("failuresByReason");
        assertThat(reasons).containsEntry("customer_not_home", 1L)
                           .containsEntry("refused", 1L);
    }

    @Test
    public void shiftSummary_defaultsToToday_whenDatesBlank() {
        when(repository.findTripsByDriverInRange(eq(7), any(), any())).thenReturn(List.of());
        Map<String, Object> r = service.shiftSummary(7, null, null);
        assertThat(r.get("totalTrips")).isEqualTo(0);
        assertThat(r.get("from")).isEqualTo(LocalDate.now().toString());
        assertThat(r.get("to")).isEqualTo(LocalDate.now().toString());
    }

    @Test
    public void shiftSummary_invalidDate_throws() {
        assertThatThrownBy(() -> service.shiftSummary(7, "not-a-date", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO");
    }

    @Test
    public void shiftSummary_fromAfterTo_throws() {
        assertThatThrownBy(() -> service.shiftSummary(7, "2026-04-27", "2026-04-26"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before");
    }

    // ---------- Management aggregations: ops / orders / deliveries / pnl / staff ----------

    private static Orders orderInPipeline(long id, String channel, String kitchenStatus, LocalDateTime createdAt) {
        Orders o = new Orders();
        o.setId(id);
        o.setChannel(channel);
        o.setKitchenStatus(kitchenStatus);
        o.setCreatedAt(createdAt);
        o.setStatus("confirmed");
        return o;
    }

    private static DeliveryTrip outForDeliveryTrip(long id, User driver, LocalDateTime outAt) {
        DeliveryTrip t = new DeliveryTrip();
        t.setId(id);
        t.setDriver(driver);
        t.setStatus("out_for_delivery");
        t.setOutAt(outAt);
        t.setCreatedAt(outAt);
        return t;
    }

    @Test
    public void managementOps_countsKitchenAndDelivery_andDetectsBreaches() {
        LocalDateTime stale = LocalDateTime.now().minusMinutes(45); // beyond 30 min SLA
        LocalDateTime fresh = LocalDateTime.now().minusMinutes(5);
        when(repository.findOrdersInPipeline()).thenReturn(List.of(
                orderInPipeline(1L, "online", "preparing", stale),
                orderInPipeline(2L, "online", "ready", fresh),
                orderInPipeline(3L, "instore", "done", fresh)
        ));
        User driver = userWithId(7, "Dan");
        DeliveryTrip stuck = outForDeliveryTrip(20L, driver, LocalDateTime.now().minusMinutes(75));
        DeliveryTrip ok    = outForDeliveryTrip(21L, driver, LocalDateTime.now().minusMinutes(10));
        DeliveryTrip pickedUp = tripOf(22L, readyOnlineOrder(99L), driver, "picked_up");
        when(repository.findAllTrips()).thenReturn(List.of(stuck, ok, pickedUp));

        Map<String, Object> r = service.managementOps();

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Long>> kitchen = (Map<String, Map<String, Long>>) r.get("kitchenQueue");
        assertThat(kitchen.get("online").get("preparing")).isEqualTo(1L);
        assertThat(kitchen.get("online").get("ready")).isEqualTo(1L);
        assertThat(kitchen.get("instore").get("done")).isEqualTo(1L);

        @SuppressWarnings("unchecked")
        Map<String, Long> dlv = (Map<String, Long>) r.get("deliveryInFlight");
        assertThat(dlv.get("pickedUp")).isEqualTo(1L);
        assertThat(dlv.get("outForDelivery")).isEqualTo(2L);
        assertThat(dlv.get("total")).isEqualTo(3L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breaches = (List<Map<String, Object>>) r.get("breaches");
        // 1 kitchen (stale order) + 1 delivery (stuck out_for_delivery)
        assertThat(breaches).hasSize(2);
        assertThat(breaches).extracting(b -> b.get("type"))
                .containsExactlyInAnyOrder("kitchen", "delivery");
    }

    @Test
    public void managementOps_emptyDb_returnsZeroCountsAndNoBreaches() {
        when(repository.findOrdersInPipeline()).thenReturn(List.of());
        when(repository.findAllTrips()).thenReturn(List.of());
        Map<String, Object> r = service.managementOps();
        @SuppressWarnings("unchecked")
        Map<String, Long> dlv = (Map<String, Long>) r.get("deliveryInFlight");
        assertThat(dlv.get("total")).isEqualTo(0L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> breaches = (List<Map<String, Object>>) r.get("breaches");
        assertThat(breaches).isEmpty();
    }

    @Test
    public void managementOrdersAudit_aggregatesByChannelAndPaymentMethod() {
        Orders o1 = order(1L, new BigDecimal("100"), "online", "delivered", LocalDateTime.of(2026, 4, 27, 10, 0));
        Orders o2 = order(2L, new BigDecimal("250"), "online", "delivered", LocalDateTime.of(2026, 4, 27, 11, 0));
        Orders o3 = order(3L, new BigDecimal("80"),  "instore", "delivered", LocalDateTime.of(2026, 4, 27, 12, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(o1, o2, o3));
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of(
                payment(o1, "card", "captured", new BigDecimal("100")),
                payment(o2, "upi",  "captured", new BigDecimal("250")),
                payment(o3, "cash", "captured", new BigDecimal("80"))
        ));

        Map<String, Object> r = service.managementOrdersAudit("2026-04-27", "2026-04-27", null, null);

        assertThat(r.get("count")).isEqualTo(3);
        assertThat((BigDecimal) r.get("totalRevenue")).isEqualByComparingTo("430");
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> byChannel = (Map<String, BigDecimal>) r.get("revenueByChannel");
        assertThat(byChannel.get("online")).isEqualByComparingTo("350");
        assertThat(byChannel.get("instore")).isEqualByComparingTo("80");
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> byPm = (Map<String, BigDecimal>) r.get("revenueByPaymentMethod");
        assertThat(byPm.get("cash")).isEqualByComparingTo("80");
        assertThat(byPm.get("card")).isEqualByComparingTo("100");
        assertThat(byPm.get("upi")).isEqualByComparingTo("250");
    }

    @Test
    public void managementOrdersAudit_filterByPaymentMethod_cashOnly() {
        Orders o1 = order(1L, new BigDecimal("100"), "online", "delivered", LocalDateTime.of(2026, 4, 27, 10, 0));
        Orders o2 = order(2L, new BigDecimal("80"),  "instore", "delivered", LocalDateTime.of(2026, 4, 27, 12, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(o1, o2));
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of(
                payment(o1, "card", "captured", new BigDecimal("100")),
                payment(o2, "cash", "captured", new BigDecimal("80"))
        ));

        Map<String, Object> r = service.managementOrdersAudit("2026-04-27", "2026-04-27", null, "cash");
        assertThat(r.get("count")).isEqualTo(1);
        assertThat((BigDecimal) r.get("totalRevenue")).isEqualByComparingTo("80");
    }

    @Test
    public void managementOrdersAudit_filterByChannel_instoreOnly() {
        Orders o1 = order(1L, new BigDecimal("100"), "online", "delivered", LocalDateTime.of(2026, 4, 27, 10, 0));
        Orders o2 = order(2L, new BigDecimal("80"),  "instore", "delivered", LocalDateTime.of(2026, 4, 27, 12, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(o1, o2));
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of());
        Map<String, Object> r = service.managementOrdersAudit("2026-04-27", "2026-04-27", "instore", null);
        assertThat(r.get("count")).isEqualTo(1);
    }

    @Test
    public void managementOrdersAudit_invalidDate_throws() {
        assertThatThrownBy(() -> service.managementOrdersAudit("not-a-date", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void managementOrdersAudit_fromAfterTo_throws() {
        assertThatThrownBy(() -> service.managementOrdersAudit("2026-04-27", "2026-04-26", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before");
    }

    @Test
    public void managementDeliveriesAudit_filterByDriverAndStatus() {
        User danny = userWithId(7, "Dan");
        User priya = userWithId(8, "Priya");
        DeliveryTrip d1 = tripOf(1L, readyOnlineOrder(1L), danny, "delivered");
        DeliveryTrip d2 = tripOf(2L, readyOnlineOrder(2L), danny, "failed");
        DeliveryTrip d3 = tripOf(3L, readyOnlineOrder(3L), priya, "delivered");
        when(repository.findTripsInRange(any(), any())).thenReturn(List.of(d1, d2, d3));

        Map<String, Object> r = service.managementDeliveriesAudit("2026-04-27", "2026-04-27", 7, null);
        assertThat(r.get("count")).isEqualTo(2);
        assertThat(r.get("delivered")).isEqualTo(1L);
        assertThat(r.get("failed")).isEqualTo(1L);
    }

    @Test
    public void managementDeliveriesAudit_invalidDate_throws() {
        assertThatThrownBy(() -> service.managementDeliveriesAudit("xyz", null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void managementDayPnl_aggregatesRevenueAndPaymentMix_andDeliveryCodTips() {
        Orders o1 = order(1L, new BigDecimal("100"), "online", "delivered", LocalDateTime.of(2026, 4, 27, 10, 0));
        Orders o2 = order(2L, new BigDecimal("250"), "instore", "delivered", LocalDateTime.of(2026, 4, 27, 11, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(o1, o2));
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of(
                payment(o1, "upi",  "captured", new BigDecimal("100")),
                payment(o2, "cash", "captured", new BigDecimal("250"))
        ));
        User driver = userWithId(7, "Dan");
        DeliveryTrip d1 = tripOf(1L, readyOnlineOrder(1L), driver, "delivered");
        d1.setCodAmount(new BigDecimal("100"));
        d1.setTipAmount(new BigDecimal("20"));
        when(repository.findTripsInRange(any(), any())).thenReturn(List.of(d1));

        Map<String, Object> r = service.managementDayPnl("2026-04-27");

        assertThat((BigDecimal) r.get("onlineRevenue")).isEqualByComparingTo("100");
        assertThat((BigDecimal) r.get("counterRevenue")).isEqualByComparingTo("250");
        assertThat((BigDecimal) r.get("totalRevenue")).isEqualByComparingTo("350");
        assertThat((BigDecimal) r.get("codCollected")).isEqualByComparingTo("100");
        assertThat((BigDecimal) r.get("tipsCollected")).isEqualByComparingTo("20");
        assertThat((BigDecimal) r.get("grossInflow")).isEqualByComparingTo("370");
        @SuppressWarnings("unchecked")
        Map<String, Object> byPm = (Map<String, Object>) r.get("revenueByPaymentMethod");
        assertThat((BigDecimal) byPm.get("cash")).isEqualByComparingTo("250");
        assertThat((BigDecimal) byPm.get("upi")).isEqualByComparingTo("100");
    }

    @Test
    public void managementDayPnl_emptyDay_returnsZeros() {
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of());
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of());
        when(repository.findTripsInRange(any(), any())).thenReturn(List.of());
        Map<String, Object> r = service.managementDayPnl("2026-04-27");
        assertThat(r.get("orderCount")).isEqualTo(0L);
        assertThat((BigDecimal) r.get("totalRevenue")).isEqualByComparingTo("0");
        assertThat((BigDecimal) r.get("net")).isEqualByComparingTo("0");
    }

    @Test
    public void managementDayPnl_invalidDate_throws() {
        assertThatThrownBy(() -> service.managementDayPnl("xyz"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO");
    }

    @Test
    public void managementStaffPerformance_aggregatesDriversAndStaffByDept_andSales() {
        User dan = userWithId(7, "Dan");
        User bella = userWithId(2, "Bella");
        User sara  = userWithId(1, "Sara");
        DeliveryTrip d1 = tripOf(1L, readyOnlineOrder(1L), dan, "delivered");
        d1.setCodAmount(new BigDecimal("100"));
        d1.setTipAmount(new BigDecimal("10"));
        d1.setDistanceKm(new BigDecimal("3"));
        DeliveryTrip d2 = tripOf(2L, readyOnlineOrder(2L), dan, "failed");
        when(repository.findTripsInRange(any(), any())).thenReturn(List.of(d1, d2));

        Task t1 = taskOf(11L, "bakery", "done", "normal", sara, bella);
        t1.setCompletedBy(bella);
        t1.setCompletedAt(LocalDateTime.of(2026, 4, 27, 11, 0));
        when(repository.findTasksCompletedInRange(any(), any())).thenReturn(List.of(t1));

        Task t2 = taskOf(12L, "kitchen", "open", "normal", sara, null);
        when(repository.findTasksCreatedInRange(any(), any())).thenReturn(List.of(t2));

        Map<String, Object> r = service.managementStaffPerformance("2026-04-27", "2026-04-27");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> drivers = (List<Map<String, Object>>) r.get("drivers");
        assertThat(drivers).hasSize(1);
        assertThat(drivers.get(0).get("trips")).isEqualTo(2L);
        assertThat(drivers.get(0).get("delivered")).isEqualTo(1L);
        assertThat(drivers.get(0).get("failed")).isEqualTo(1L);
        assertThat((BigDecimal) drivers.get(0).get("cod")).isEqualByComparingTo("100");

        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> byDept =
                (Map<String, List<Map<String, Object>>>) r.get("staffByDepartment");
        assertThat(byDept.get("bakery")).hasSize(1);
        assertThat(byDept.get("bakery").get(0).get("tasksCompleted")).isEqualTo(1L);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sales = (List<Map<String, Object>>) r.get("salesActivity");
        assertThat(sales).hasSize(1);
        assertThat(sales.get(0).get("tasksCreated")).isEqualTo(1L);
    }

    @Test
    public void managementStaffPerformance_invalidDate_throws() {
        assertThatThrownBy(() -> service.managementStaffPerformance("xyz", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- Refund / cancellation / damage write-off requests ----------

    private static RefundRequest pendingRequest(long id, Orders order, User raisedBy,
                                                 String type, BigDecimal amount) {
        RefundRequest r = new RefundRequest();
        r.setId(id);
        r.setOrder(order);
        r.setRaisedBy(raisedBy);
        r.setRequestType(type);
        r.setReason("test reason");
        r.setAmount(amount);
        r.setStatus("pending");
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    @Test
    public void raiseRefundRequest_validRefund_savesPendingRow() {
        Orders order = order(101L, new BigDecimal("500"), "online", "delivered",
                LocalDateTime.now());
        User customer = userWithId(50, "Asha");
        when(repository.findOrderById(101L)).thenReturn(Optional.of(order));
        when(repository.findUserById(50)).thenReturn(Optional.of(customer));
        when(repository.saveRefundRequest(any())).thenAnswer(inv -> {
            RefundRequest r = inv.getArgument(0);
            r.setId(7L);
            return r;
        });

        Map<String, Object> result = service.raiseRefundRequest(
                101L, 50, "refund", "Cake arrived damaged", new BigDecimal("100"));

        assertThat(result.get("id")).isEqualTo(7L);
        assertThat(result.get("status")).isEqualTo("pending");
        assertThat(result.get("requestType")).isEqualTo("refund");
        assertThat(result.get("raisedByName")).isEqualTo("Asha");
        verify(repository).saveRefundRequest(any());
    }

    @Test
    public void raiseRefundRequest_invalidType_throws() {
        assertThatThrownBy(() -> service.raiseRefundRequest(
                1L, 1, "freebie", "x", new BigDecimal("1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestType");
    }

    @Test
    public void raiseRefundRequest_blankReason_throws() {
        assertThatThrownBy(() -> service.raiseRefundRequest(
                1L, 1, "refund", "  ", new BigDecimal("1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason");
    }

    @Test
    public void raiseRefundRequest_negativeAmount_throws() {
        assertThatThrownBy(() -> service.raiseRefundRequest(
                1L, 1, "refund", "x", new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount");
    }

    @Test
    public void raiseRefundRequest_unknownOrder_throws() {
        when(repository.findOrderById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.raiseRefundRequest(
                404L, 1, "refund", "x", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    public void listRefundRequests_filterByPending_returnsOnlyPending() {
        when(repository.findRefundRequestsByStatus("pending")).thenReturn(List.of(
                pendingRequest(1L, order(1L, new BigDecimal("100"), "online", "delivered",
                        LocalDateTime.now()), userWithId(1, "Asha"), "refund", new BigDecimal("100"))
        ));
        List<Map<String, Object>> result = service.listRefundRequests("pending");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("status")).isEqualTo("pending");
    }

    @Test
    public void listRefundRequests_invalidStatus_throws() {
        assertThatThrownBy(() -> service.listRefundRequests("wibble"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void decideRefundRequest_approveRefund_marksOrderRefunded() {
        Orders order = order(101L, new BigDecimal("500"), "online", "delivered",
                LocalDateTime.now());
        RefundRequest r = pendingRequest(7L, order, userWithId(50, "Asha"),
                "refund", new BigDecimal("500"));
        User maya = userWithId(99, "Maya");
        when(repository.findRefundRequestById(7L)).thenReturn(Optional.of(r));
        when(repository.findUserById(99)).thenReturn(Optional.of(maya));
        when(repository.saveRefundRequest(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveOrder(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.decideRefundRequest(7L, 99, "approved", "ok");
        assertThat(result.get("status")).isEqualTo("approved");
        assertThat(result.get("decidedByName")).isEqualTo("Maya");
        assertThat(order.getStatus()).isEqualTo("refunded");
    }

    @Test
    public void decideRefundRequest_approveCancellation_marksOrderCancelled() {
        Orders order = order(101L, new BigDecimal("500"), "online", "confirmed",
                LocalDateTime.now());
        RefundRequest r = pendingRequest(8L, order, userWithId(50, "Asha"),
                "cancellation", new BigDecimal("500"));
        when(repository.findRefundRequestById(8L)).thenReturn(Optional.of(r));
        when(repository.findUserById(99)).thenReturn(Optional.of(userWithId(99, "Maya")));
        when(repository.saveRefundRequest(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repository.saveOrder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.decideRefundRequest(8L, 99, "approved", null);
        assertThat(order.getStatus()).isEqualTo("cancelled");
        assertThat(order.getKitchenStatus()).isEqualTo("cancelled");
    }

    @Test
    public void decideRefundRequest_reject_doesNotChangeOrder() {
        Orders order = order(101L, new BigDecimal("500"), "online", "delivered",
                LocalDateTime.now());
        RefundRequest r = pendingRequest(9L, order, userWithId(50, "Asha"),
                "refund", new BigDecimal("500"));
        when(repository.findRefundRequestById(9L)).thenReturn(Optional.of(r));
        when(repository.findUserById(99)).thenReturn(Optional.of(userWithId(99, "Maya")));
        when(repository.saveRefundRequest(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.decideRefundRequest(9L, 99, "rejected", "denied");
        assertThat(result.get("status")).isEqualTo("rejected");
        assertThat(order.getStatus()).isEqualTo("delivered");
        verify(repository, never()).saveOrder(any());
    }

    @Test
    public void decideRefundRequest_alreadyDecided_throws() {
        Orders order = order(101L, new BigDecimal("500"), "online", "delivered",
                LocalDateTime.now());
        RefundRequest r = pendingRequest(7L, order, userWithId(50, "Asha"),
                "refund", new BigDecimal("500"));
        r.setStatus("approved");
        when(repository.findRefundRequestById(7L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> service.decideRefundRequest(7L, 99, "rejected", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already");
    }

    @Test
    public void decideRefundRequest_invalidDecision_throws() {
        assertThatThrownBy(() -> service.decideRefundRequest(7L, 99, "maybe", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decision");
    }

    @Test
    public void decideRefundRequest_unknownRequest_throws() {
        when(repository.findRefundRequestById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.decideRefundRequest(404L, 99, "approved", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // ---------- Corporate / catering order sign-off ----------

    @Test
    public void flagOrderForApproval_marksFlagAndAwaiting() {
        Orders order = order(101L, new BigDecimal("5000"), "online", "confirmed",
                LocalDateTime.now());
        order.setKitchenStatus("pending");
        when(repository.findOrderById(101L)).thenReturn(Optional.of(order));
        when(repository.saveOrder(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.flagOrderForApproval(101L, "5000 cookies for ABC");
        assertThat(result.get("requiresApproval")).isEqualTo(true);
        assertThat(result.get("approvalStatus")).isEqualTo("pending");
        assertThat(order.getKitchenStatus()).isEqualTo("awaiting_approval");
    }

    @Test
    public void flagOrderForApproval_unknownOrder_throws() {
        when(repository.findOrderById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.flagOrderForApproval(404L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Order not found");
    }

    @Test
    public void flagOrderForApproval_alreadyDecided_throws() {
        Orders order = order(101L, new BigDecimal("5000"), "online", "confirmed",
                LocalDateTime.now());
        order.setRequiresApproval(true);
        order.setApprovalStatus("approved");
        when(repository.findOrderById(101L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> service.flagOrderForApproval(101L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already");
    }

    @Test
    public void decideOrderApproval_approve_releasesToKitchen() {
        Orders order = order(101L, new BigDecimal("5000"), "online", "confirmed",
                LocalDateTime.now());
        order.setRequiresApproval(true);
        order.setApprovalStatus("pending");
        order.setKitchenStatus("awaiting_approval");
        when(repository.findOrderById(101L)).thenReturn(Optional.of(order));
        when(repository.findUserById(99)).thenReturn(Optional.of(userWithId(99, "Maya")));
        when(repository.saveOrder(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.decideOrderApproval(101L, 99, "approved", "go ahead");
        assertThat(result.get("approvalStatus")).isEqualTo("approved");
        assertThat(order.getKitchenStatus()).isEqualTo("pending");
    }

    @Test
    public void decideOrderApproval_reject_cancelsOrder() {
        Orders order = order(101L, new BigDecimal("5000"), "online", "confirmed",
                LocalDateTime.now());
        order.setRequiresApproval(true);
        order.setApprovalStatus("pending");
        when(repository.findOrderById(101L)).thenReturn(Optional.of(order));
        when(repository.findUserById(99)).thenReturn(Optional.of(userWithId(99, "Maya")));
        when(repository.saveOrder(any())).thenAnswer(inv -> inv.getArgument(0));

        service.decideOrderApproval(101L, 99, "rejected", "too short notice");
        assertThat(order.getStatus()).isEqualTo("cancelled");
        assertThat(order.getKitchenStatus()).isEqualTo("cancelled");
    }

    @Test
    public void decideOrderApproval_invalidDecision_throws() {
        assertThatThrownBy(() -> service.decideOrderApproval(101L, 99, "maybe", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decision");
    }

    @Test
    public void decideOrderApproval_notFlagged_throws() {
        Orders order = order(101L, new BigDecimal("5000"), "online", "confirmed",
                LocalDateTime.now());
        when(repository.findOrderById(101L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> service.decideOrderApproval(101L, 99, "approved", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not flagged");
    }

    @Test
    public void decideOrderApproval_alreadyDecided_throws() {
        Orders order = order(101L, new BigDecimal("5000"), "online", "confirmed",
                LocalDateTime.now());
        order.setRequiresApproval(true);
        order.setApprovalStatus("approved");
        when(repository.findOrderById(101L)).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> service.decideOrderApproval(101L, 99, "rejected", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already");
    }

    @Test
    public void listOrdersPendingApproval_returnsMappedRows() {
        Orders order = order(101L, new BigDecimal("5000"), "online", "confirmed",
                LocalDateTime.now());
        order.setRequiresApproval(true);
        order.setApprovalStatus("pending");
        when(repository.findOrdersPendingApproval()).thenReturn(List.of(order));
        List<Map<String, Object>> result = service.listOrdersPendingApproval();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("orderId")).isEqualTo(101L);
        assertThat(result.get(0).get("approvalStatus")).isEqualTo("pending");
    }

    // ---------- Discount / promo campaign approval ----------

    private static DiscountCampaign pendingCampaign(long id, String name,
                                                     BigDecimal pct, User proposer) {
        DiscountCampaign d = new DiscountCampaign();
        d.setId(id);
        d.setName(name);
        d.setDiscountPercent(pct);
        d.setStatus("pending");
        d.setProposedBy(proposer);
        d.setStartsOn(LocalDate.now());
        d.setEndsOn(LocalDate.now());
        d.setCreatedAt(LocalDateTime.now());
        return d;
    }

    @Test
    public void proposeDiscountCampaign_validInput_savesPendingRow() {
        User sara = userWithId(1, "Sara");
        when(repository.findUserById(1)).thenReturn(Optional.of(sara));
        when(repository.saveDiscountCampaign(any())).thenAnswer(inv -> {
            DiscountCampaign d = inv.getArgument(0);
            d.setId(50L);
            return d;
        });

        Map<String, Object> result = service.proposeDiscountCampaign(
                1, "20% off Cakes", "Cakes", new BigDecimal("20"),
                "2026-04-28", "2026-04-30");
        assertThat(result.get("id")).isEqualTo(50L);
        assertThat(result.get("status")).isEqualTo("pending");
        assertThat(result.get("name")).isEqualTo("20% off Cakes");
        assertThat(result.get("proposedByName")).isEqualTo("Sara");
    }

    @Test
    public void proposeDiscountCampaign_blankName_throws() {
        assertThatThrownBy(() -> service.proposeDiscountCampaign(
                1, "  ", null, new BigDecimal("10"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    public void proposeDiscountCampaign_invalidPercent_throws() {
        assertThatThrownBy(() -> service.proposeDiscountCampaign(
                1, "test", null, new BigDecimal("150"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("discountPercent");
    }

    @Test
    public void proposeDiscountCampaign_negativePercent_throws() {
        assertThatThrownBy(() -> service.proposeDiscountCampaign(
                1, "test", null, new BigDecimal("-5"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("discountPercent");
    }

    @Test
    public void proposeDiscountCampaign_invalidDate_throws() {
        assertThatThrownBy(() -> service.proposeDiscountCampaign(
                1, "test", null, new BigDecimal("10"), "not-a-date", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO");
    }

    @Test
    public void proposeDiscountCampaign_endsBeforeStarts_throws() {
        assertThatThrownBy(() -> service.proposeDiscountCampaign(
                1, "test", null, new BigDecimal("10"),
                "2026-04-28", "2026-04-27"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before");
    }

    @Test
    public void proposeDiscountCampaign_unknownProposer_throws() {
        when(repository.findUserById(404)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.proposeDiscountCampaign(
                404, "test", null, new BigDecimal("10"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Proposer");
    }

    @Test
    public void listDiscountCampaigns_pending_returnsMatching() {
        when(repository.findDiscountCampaignsByStatus("pending")).thenReturn(List.of(
                pendingCampaign(1L, "20% Cakes", new BigDecimal("20"), userWithId(1, "Sara"))
        ));
        List<Map<String, Object>> result = service.listDiscountCampaigns("pending");
        assertThat(result).hasSize(1);
    }

    @Test
    public void listDiscountCampaigns_invalidStatus_throws() {
        assertThatThrownBy(() -> service.listDiscountCampaigns("wibble"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void decideDiscountCampaign_approve_setsStatusAndDecidedBy() {
        DiscountCampaign d = pendingCampaign(50L, "20% Cakes", new BigDecimal("20"),
                userWithId(1, "Sara"));
        when(repository.findDiscountCampaignById(50L)).thenReturn(Optional.of(d));
        when(repository.findUserById(99)).thenReturn(Optional.of(userWithId(99, "Maya")));
        when(repository.saveDiscountCampaign(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.decideDiscountCampaign(50L, 99, "approved", "great");
        assertThat(result.get("status")).isEqualTo("approved");
        assertThat(result.get("decidedByName")).isEqualTo("Maya");
    }

    @Test
    public void decideDiscountCampaign_alreadyDecided_throws() {
        DiscountCampaign d = pendingCampaign(50L, "x", new BigDecimal("10"),
                userWithId(1, "Sara"));
        d.setStatus("approved");
        when(repository.findDiscountCampaignById(50L)).thenReturn(Optional.of(d));
        assertThatThrownBy(() -> service.decideDiscountCampaign(50L, 99, "rejected", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already");
    }

    @Test
    public void decideDiscountCampaign_invalidDecision_throws() {
        assertThatThrownBy(() -> service.decideDiscountCampaign(50L, 99, "maybe", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void decideDiscountCampaign_unknown_throws() {
        when(repository.findDiscountCampaignById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.decideDiscountCampaign(404L, 99, "approved", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // ---------- Multi-team supply requests ----------

    @Test
    public void requestMoreSupplyByTeam_validBakeryRequest_savesWithTeam() {
        when(repository.findSupplyById(1L)).thenReturn(Optional.of(flour));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.requestMoreSupplyByTeam(
                1L, new BigDecimal("5"), "urgency", "bakery");

        assertThat(result.get("requestedByTeam")).isEqualTo("bakery");
        assertThat(result.get("orderStatus")).isEqualTo("urgency");
    }

    @Test
    public void requestMoreSupplyByTeam_defaultsToKitchenWhenTeamBlank() {
        when(repository.findSupplyById(1L)).thenReturn(Optional.of(flour));
        when(repository.saveSupply(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> result = service.requestMoreSupplyByTeam(
                1L, new BigDecimal("5"), null, "");

        assertThat(result.get("requestedByTeam")).isEqualTo("kitchen");
    }

    @Test
    public void requestMoreSupplyByTeam_invalidTeam_throws() {
        assertThatThrownBy(() -> service.requestMoreSupplyByTeam(
                1L, new BigDecimal("5"), null, "marketing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("team");
    }

    @Test
    public void requestMoreSupplyByTeam_negativeQty_throws() {
        assertThatThrownBy(() -> service.requestMoreSupplyByTeam(
                1L, new BigDecimal("-1"), null, "kitchen"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requestedQty");
    }

    @Test
    public void requestMoreSupplyByTeam_unknownSupply_throws() {
        when(repository.findSupplyById(404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.requestMoreSupplyByTeam(
                404L, new BigDecimal("5"), null, "kitchen"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Supply not found");
    }

    // ---------- Cash reconciliation ----------

    @Test
    public void cashReconciliation_aggregatesCounterCashAndCod_andComputesVariance() {
        Orders cashOrder = order(1L, new BigDecimal("100"), "instore", "completed",
                LocalDateTime.of(2026, 4, 27, 10, 0));
        Orders cardOrder = order(2L, new BigDecimal("250"), "instore", "completed",
                LocalDateTime.of(2026, 4, 27, 11, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(cashOrder, cardOrder));
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of(
                payment(cashOrder, "cash", "captured", new BigDecimal("100")),
                payment(cardOrder, "card", "captured", new BigDecimal("250"))
        ));
        DeliveryTrip cod = tripOf(1L, readyOnlineOrder(1L), userWithId(7, "Dan"), "delivered");
        cod.setCodAmount(new BigDecimal("75"));
        when(repository.findTripsInRange(any(), any())).thenReturn(List.of(cod));

        Map<String, Object> r = service.cashReconciliation(
                "2026-04-27", new BigDecimal("500"), new BigDecimal("675"));

        assertThat((BigDecimal) r.get("openingFloat")).isEqualByComparingTo("500");
        assertThat((BigDecimal) r.get("counterCash")).isEqualByComparingTo("100");
        assertThat((BigDecimal) r.get("counterCard")).isEqualByComparingTo("250");
        assertThat((BigDecimal) r.get("codCollected")).isEqualByComparingTo("75");
        assertThat((BigDecimal) r.get("expectedCashInDrawer")).isEqualByComparingTo("675");
        assertThat((BigDecimal) r.get("variance")).isEqualByComparingTo("0");
        assertThat(r.get("balanced")).isEqualTo(true);
    }

    @Test
    public void cashReconciliation_short_drawer_marksUnbalancedWithNegativeVariance() {
        Orders cashOrder = order(1L, new BigDecimal("100"), "instore", "completed",
                LocalDateTime.of(2026, 4, 27, 10, 0));
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of(cashOrder));
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of(
                payment(cashOrder, "cash", "captured", new BigDecimal("100"))
        ));
        when(repository.findTripsInRange(any(), any())).thenReturn(List.of());

        Map<String, Object> r = service.cashReconciliation(
                "2026-04-27", BigDecimal.ZERO, new BigDecimal("80"));
        assertThat((BigDecimal) r.get("variance")).isEqualByComparingTo("-20");
        assertThat(r.get("balanced")).isEqualTo(false);
    }

    @Test
    public void cashReconciliation_invalidDate_throws() {
        assertThatThrownBy(() -> service.cashReconciliation("xyz", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISO");
    }

    @Test
    public void cashReconciliation_negativeFloat_throws() {
        assertThatThrownBy(() -> service.cashReconciliation(
                "2026-04-27", new BigDecimal("-1"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("openingFloat");
    }

    @Test
    public void cashReconciliation_negativeCounted_throws() {
        assertThatThrownBy(() -> service.cashReconciliation(
                "2026-04-27", BigDecimal.ZERO, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("countedCash");
    }

    @Test
    public void cashReconciliation_noCountedCash_returnsNullVarianceNotBalanced() {
        when(repository.findOrdersInRange(any(), any())).thenReturn(List.of());
        when(repository.findPaymentsByOrderIds(any())).thenReturn(List.of());
        when(repository.findTripsInRange(any(), any())).thenReturn(List.of());
        Map<String, Object> r = service.cashReconciliation("2026-04-27", null, null);
        assertThat(r.get("variance")).isNull();
        assertThat(r.get("balanced")).isEqualTo(false);
    }
}
