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

import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.Supply;
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

    private static Role customerRole() {
        Role r = new Role();
        r.setRole("customer");
        r.setFullName("Customer");
        return r;
    }
}
