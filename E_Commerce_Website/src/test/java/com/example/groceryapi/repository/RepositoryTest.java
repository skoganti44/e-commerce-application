package com.example.groceryapi.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.groceryapi.model.Cart;
import com.example.groceryapi.model.CartItem;
import com.example.groceryapi.model.Category;
import com.example.groceryapi.model.OrderItem;
import com.example.groceryapi.model.Orders;
import com.example.groceryapi.model.Payment;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.ProductAvailable;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.model.User;
import com.example.groceryapi.testdata.TestData;

@DataJpaTest
@Import(Repository.class)
public class RepositoryTest {

    @Autowired
    private Repository repository;

    @Test
    public void testSaveUser() {
        User saved = repository.saveUser(TestData.newJohn());

        assertThat(saved.getuserid()).isGreaterThan(0);
        assertThat(saved.getname()).isEqualTo("John");
        assertThat(saved.getemail()).isEqualTo("john@example.com");
    }

    @Test
    public void testFindAllUsers() {
        repository.saveUser(TestData.newJohn());
        repository.saveUser(TestData.newJane());

        List<User> users = repository.findAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getname).containsExactlyInAnyOrder("John",
                "Jane");
    }

    @Test
    public void testFindUserById() {
        User persisted = repository.saveUser(TestData.newJohn());

        Optional<User> found = repository.findUserById(persisted.getuserid());

        assertThat(found).isPresent();
        assertThat(found.get().getname()).isEqualTo("John");
    }

    @Test
    public void testDeleteUserById() {
        User persisted = repository.saveUser(TestData.newJohn());

        repository.deleteUserById(persisted.getuserid());

        assertThat(repository.findUserById(persisted.getuserid())).isEmpty();
    }

    @Test
    public void testFindAllUsers_Empty() {
        assertThat(repository.findAllUsers()).isEmpty();
    }

    @Test
    public void testSaveRole() {
        Role saved = repository.saveRole(newRole("Alice Smith", "Manager",
                TestData.SALES));

        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getFullName()).isEqualTo("Alice Smith");
        assertThat(saved.getRole()).isEqualTo("Manager");
        assertThat(saved.getDepartment()).isEqualTo(TestData.SALES);
    }

@Test
public void testFindAllRoles() {
repository.saveRole(newRole("Alice Smith", "Manager", TestData.SALES));
repository.saveRole(newRole("Bob Johnson", "Engineer", TestData.IT));

List<Role> roles = repository.findAllRoles();

assertThat(roles).hasSize(2);
assertThat(roles).extracting(Role::getFullName).containsExactlyInAnyOrder("Alice Smith", "Bob Johnson");
}

    @Test
    public void testFindRoleById() {
        Role persisted = repository.saveRole(newRole("Alice Smith", "Manager",
                TestData.SALES));

        Optional<Role> found = repository.findRoleById(persisted.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Alice Smith");
    }

    @Test
    public void testDeleteRoleById() {
        Role persisted = repository.saveRole(newRole("Alice Smith", "Manager",
                TestData.SALES));

        repository.deleteRoleById(persisted.getId());

        assertThat(repository.findRoleById(persisted.getId())).isEmpty();
    }

    @Test
    public void testFindAllRoles_Empty() {
        assertThat(repository.findAllRoles()).isEmpty();
    }

    @Test
    public void testFindRolesByDepartment() {
        repository.saveRole(newRole("Alice Smith", "Manager",
                TestData.FRESH_PRODUCTS));
        repository.saveRole(newRole("Bob Johnson", "Engineer", TestData.IT));
        repository.saveRole(newRole("Carol White", "Lead", TestData.FRESH_PRODUCTS));

        List<Role> roles = repository.findRolesByDepartment(TestData.FRESH_PRODUCTS);

        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(Role::getFullName)
                .containsExactlyInAnyOrder("Alice Smith", "Carol White");
    }

    @Test
    public void testFindRolesByDepartment_NoMatch() {
        repository.saveRole(newRole("Alice Smith", "Manager", TestData.SALES));

        assertThat(repository.findRolesByDepartment("Nonexistent")).isEmpty();
    }

    @Test
    public void testSaveUserRole() {
        User user = repository.saveUser(TestData.newJohn());
        Role role = repository.saveRole(newRole("Alice Smith", "Manager",
                TestData.SALES));

        UserRole saved = repository.saveUserRole(newUserRole(user, role));

        assertThat(saved.getUserroleid()).isGreaterThan(0);
        assertThat(saved.getUser().getname()).isEqualTo("John");
        assertThat(saved.getRole().getFullName()).isEqualTo("Alice Smith");
    }

    @Test
    public void testFindAllUserRoles() {
        User john = repository.saveUser(TestData.newJohn());
        User jane = repository.saveUser(TestData.newJane());
        Role manager = repository.saveRole(newRole("Alice Smith", "Manager",
                TestData.SALES));
        Role engineer = repository.saveRole(newRole("Bob Johnson", "Engineer",
                TestData.IT));
        repository.saveUserRole(newUserRole(john, manager));
        repository.saveUserRole(newUserRole(jane, engineer));

        List<UserRole> userRoles = repository.findAllUserRoles();

        assertThat(userRoles).hasSize(2);
        assertThat(userRoles).extracting(ur -> ur.getUser().getname())
                .containsExactlyInAnyOrder("John", "Jane");
    }

@Test
public void testFindUserRolesByUserId() {
User john = repository.saveUser(TestData.newJohn());
User jane = repository.saveUser(TestData.newJane());
Role manager = repository.saveRole(newRole("Alice Smith", "Manager",
TestData.SALES));
Role engineer = repository.saveRole(newRole("Bob Johnson", "Engineer",
TestData.IT));
repository.saveUserRole(newUserRole(john, manager));
repository.saveUserRole(newUserRole(jane, engineer));

List<UserRole> userRoles =
repository.findUserRolesByUserId(john.getuserid());

assertThat(userRoles).hasSize(1);
assertThat(userRoles.get(0).getUser().getname()).isEqualTo("John");
assertThat(userRoles.get(0).getRole().getFullName()).isEqualTo("Alice Smith");
}

@Test
public void testFindUserRolesByRoleId() {
User john = repository.saveUser(TestData.newJohn());
User jane = repository.saveUser(TestData.newJane());
Role manager = repository.saveRole(newRole("Alice Smith", "Manager",
TestData.SALES));
Role engineer = repository.saveRole(newRole("Bob Johnson", "Engineer",
TestData.IT));
repository.saveUserRole(newUserRole(john, manager));
repository.saveUserRole(newUserRole(jane, engineer));

List<UserRole> userRoles =
repository.findUserRolesByRoleId(engineer.getId());

assertThat(userRoles).hasSize(1);
assertThat(userRoles.get(0).getUser().getname()).isEqualTo("Jane");
assertThat(userRoles.get(0).getRole().getFullName()).isEqualTo("Bob Johnson");
}

    @Test
    public void testFindAllUserRoles_Empty() {
        assertThat(repository.findAllUserRoles()).isEmpty();
    }

    @Test
    public void testFindUserRolesByUserId_NoMatch() {
        assertThat(repository.findUserRolesByUserId(999)).isEmpty();
    }

    @Test
    public void testFindUserRolesByRoleId_NoMatch() {
        assertThat(repository.findUserRolesByRoleId(999)).isEmpty();
    }

    // ========== Cart / CartItem — POSITIVE scenarios ==========

    @Test
    public void testSaveCart() {
        User user = repository.saveUser(TestData.newJohn());

        Cart saved = repository.saveCart(TestData.newCart(user));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getname()).isEqualTo("John");
    }

    @Test
    public void testFindCartsByUserId() {
        User john = repository.saveUser(TestData.newJohn());
        User jane = repository.saveUser(TestData.newJane());
        repository.saveCart(TestData.newCart(john));
        repository.saveCart(TestData.newCart(jane));

        List<Cart> carts = repository.findCartsByUserId(john.getuserid());

        assertThat(carts).hasSize(1);
        assertThat(carts.get(0).getUser().getname()).isEqualTo("John");
    }

    @Test
    public void testSaveCartItem() {
        User user = repository.saveUser(TestData.newJohn());
        Cart cart = repository.saveCart(TestData.newCart(user));
        Product product = persistProduct("Apple", new BigDecimal("2.00"), 50);

        CartItem saved = repository.saveCartItem(TestData.newCartItem(cart, product,
                3));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCart().getId()).isEqualTo(cart.getId());
        assertThat(saved.getProduct().getName()).isEqualTo("Apple");
        assertThat(saved.getQuantity()).isEqualTo(3);
    }

    @Test
    public void testFindCartItemsByUserId() {
        User user = repository.saveUser(TestData.newJohn());
        Cart cart = repository.saveCart(TestData.newCart(user));
        Product apple = persistProduct("Apple", new BigDecimal("2.00"), 50);
        Product milk = persistProduct("Milk", new BigDecimal("5.26"), 30);
        repository.saveCartItem(TestData.newCartItem(cart, apple, 3));
        repository.saveCartItem(TestData.newCartItem(cart, milk, 6));

        List<CartItem> items = repository.findCartItemsByUserId(user.getuserid());

        assertThat(items).hasSize(2);
        assertThat(items).extracting(ci -> ci.getProduct().getName())
                .containsExactlyInAnyOrder("Apple", "Milk");
        assertThat(items).extracting(CartItem::getQuantity)
                .containsExactlyInAnyOrder(3, 6);
    }

    @Test
    public void testFindCartItemsByUserId_OnlyReturnsOwnersItems() {
        User john = repository.saveUser(TestData.newJohn());
        User jane = repository.saveUser(TestData.newJane());
        Cart johnsCart = repository.saveCart(TestData.newCart(john));
        Cart janesCart = repository.saveCart(TestData.newCart(jane));
        Product apple = persistProduct("Apple", new BigDecimal("2.00"), 50);
        repository.saveCartItem(TestData.newCartItem(johnsCart, apple, 3));
        repository.saveCartItem(TestData.newCartItem(janesCart, apple, 10));

        List<CartItem> johnsItems = repository.findCartItemsByUserId(john.getuserid());

        assertThat(johnsItems).hasSize(1);
        assertThat(johnsItems.get(0).getQuantity()).isEqualTo(3);
    }

    // ========== Cart / CartItem — NEGATIVE scenarios ==========

    @Test
    public void testFindCartsByUserId_NoMatch() {
        assertThat(repository.findCartsByUserId(999)).isEmpty();
    }

    @Test
    public void testFindCartItemsByUserId_NoMatch() {
        assertThat(repository.findCartItemsByUserId(999)).isEmpty();
    }

    @Test
    public void testFindCartItemsByUserId_CartExistsButNoItems() {
        User user = repository.saveUser(TestData.newJohn());
        repository.saveCart(TestData.newCart(user));

        List<CartItem> items = repository.findCartItemsByUserId(user.getuserid());

        assertThat(items).isEmpty();
    }

    // ========== Orders / OrderItem — POSITIVE scenarios ==========

    @Test
    public void testSaveOrder() {
        User user = repository.saveUser(TestData.newJohn());

        Orders saved = repository.saveOrder(TestData.newOrder(user, new BigDecimal("1049.99"), "PLACED"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUser().getname()).isEqualTo("John");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("1049.99");
        assertThat(saved.getStatus()).isEqualTo("PLACED");
    }

    @Test
    public void testFindOrdersByUserId() {
        User john = repository.saveUser(TestData.newJohn());
        User jane = repository.saveUser(TestData.newJane());
        repository.saveOrder(TestData.newOrder(john, new BigDecimal("100.00"),
                "PLACED"));
        repository.saveOrder(TestData.newOrder(jane, new BigDecimal("200.00"),
                "PLACED"));

        List<Orders> orders = repository.findOrdersByUserId(john.getuserid());

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getUser().getname()).isEqualTo("John");
    }

    @Test
    public void testSaveOrderItem() {
        User user = repository.saveUser(TestData.newJohn());
        Orders order = repository.saveOrder(TestData.newOrder(user, new BigDecimal("10.00"), "PLACED"));
        Product product = persistProduct("Apple", new BigDecimal("2.00"), 50);

        OrderItem saved = repository.saveOrderItem(
                TestData.newOrderItem(order, product, 2, new BigDecimal("2.00")));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrder().getId()).isEqualTo(order.getId());
        assertThat(saved.getProduct().getName()).isEqualTo("Apple");
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(saved.getPrice()).isEqualByComparingTo("2.00");
    }

    @Test
    public void testFindOrderItemsByUserId() {
        User user = repository.saveUser(TestData.newJohn());
        Orders order = repository.saveOrder(TestData.newOrder(user, new BigDecimal("50.00"), "PLACED"));
        Product apple = persistProduct("Apple", new BigDecimal("2.00"), 50);
        Product milk = persistProduct("Milk", new BigDecimal("5.26"), 30);
        repository.saveOrderItem(TestData.newOrderItem(order, apple, 2, new BigDecimal("2.00")));
        repository.saveOrderItem(TestData.newOrderItem(order, milk, 6, new BigDecimal("5.26")));

        List<OrderItem> items = repository.findOrderItemsByUserId(user.getuserid());

        assertThat(items).hasSize(2);
        assertThat(items).extracting(oi -> oi.getProduct().getName())
                .containsExactlyInAnyOrder("Apple", "Milk");
    }

    @Test
    public void testFindOrderItemsByUserId_OnlyReturnsOwnersItems() {
        User john = repository.saveUser(TestData.newJohn());
        User jane = repository.saveUser(TestData.newJane());
        Orders johnOrder = repository.saveOrder(TestData.newOrder(john, new BigDecimal("10.00"), "PLACED"));
        Orders janeOrder = repository.saveOrder(TestData.newOrder(jane, new BigDecimal("20.00"), "PLACED"));
        Product apple = persistProduct("Apple", new BigDecimal("2.00"), 50);
        repository.saveOrderItem(TestData.newOrderItem(johnOrder, apple, 2, new BigDecimal("2.00")));
        repository.saveOrderItem(TestData.newOrderItem(janeOrder, apple, 10, new BigDecimal("2.00")));

        List<OrderItem> johnsItems = repository.findOrderItemsByUserId(john.getuserid());

        assertThat(johnsItems).hasSize(1);
        assertThat(johnsItems.get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    public void testFindRolesByUserId() {
        User user = repository.saveUser(TestData.newJohn());
        Role customer = repository.saveRole(newRole("John Cust", "customer",
                TestData.SALES));
        repository.saveUserRole(newUserRole(user, customer));

        List<Role> roles = repository.findRolesByUserId(user.getuserid());

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole()).isEqualTo("customer");
    }

    @Test
    public void testFindRolesByUserId_MultipleRoles() {
        User user = repository.saveUser(TestData.newJohn());
        Role manager = repository.saveRole(newRole("John Mgr", "Manager",
                TestData.SALES));
        Role customer = repository.saveRole(newRole("John Cust", "customer",
                TestData.SALES));
        repository.saveUserRole(newUserRole(user, manager));
        repository.saveUserRole(newUserRole(user, customer));

        List<Role> roles = repository.findRolesByUserId(user.getuserid());

        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(Role::getRole)
                .containsExactlyInAnyOrder("Manager", "customer");
    }

    // ========== Orders / OrderItem — NEGATIVE scenarios ==========

    @Test
    public void testFindOrdersByUserId_NoMatch() {
        assertThat(repository.findOrdersByUserId(999)).isEmpty();
    }

    @Test
    public void testFindOrderItemsByUserId_NoMatch() {
        assertThat(repository.findOrderItemsByUserId(999)).isEmpty();
    }

    @Test
    public void testFindRolesByUserId_NoRoles() {
        User user = repository.saveUser(TestData.newJohn());

        assertThat(repository.findRolesByUserId(user.getuserid())).isEmpty();
    }

    // ========== Payment — POSITIVE scenarios ==========

    @Test
    public void testSavePayment() {
        User user = repository.saveUser(TestData.newJohn());
        Orders order = repository.saveOrder(TestData.newOrder(user, new BigDecimal("28.26"), "PLACED"));

        Payment saved = repository.savePayment(
                TestData.newPayment(order, "CREDIT_CARD", "SUCCESS", new BigDecimal("28.26")));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrder().getId()).isEqualTo(order.getId());
        assertThat(saved.getPaymentMethod()).isEqualTo("CREDIT_CARD");
        assertThat(saved.getStatus()).isEqualTo("SUCCESS");
        assertThat(saved.getAmount()).isEqualByComparingTo("28.26");
    }

    @Test
    public void testFindPaymentsByUserId() {
        User user = repository.saveUser(TestData.newJohn());
        Orders order = repository.saveOrder(TestData.newOrder(user, new BigDecimal("28.26"), "PLACED"));
        repository.savePayment(TestData.newPayment(order, "CREDIT_CARD", "Decline",
                new BigDecimal("28.26")));
        repository.savePayment(TestData.newPayment(order, "CREDIT_CARD", "SUCCESS",
                new BigDecimal("28.26")));

        List<Payment> payments = repository.findPaymentsByUserId(user.getuserid());

        assertThat(payments).hasSize(2);
        assertThat(payments).extracting(Payment::getStatus)
                .containsExactlyInAnyOrder("Decline", "SUCCESS");
    }

    @Test
    public void testFindPaymentsByUserId_OnlyReturnsOwnersPayments() {
        User john = repository.saveUser(TestData.newJohn());
        User jane = repository.saveUser(TestData.newJane());
        Orders johnOrder = repository.saveOrder(TestData.newOrder(john, new BigDecimal("10.00"), "PLACED"));
        Orders janeOrder = repository.saveOrder(TestData.newOrder(jane, new BigDecimal("20.00"), "PLACED"));
        repository.savePayment(TestData.newPayment(johnOrder, "CREDIT_CARD",
                "SUCCESS", new BigDecimal("10.00")));
        repository.savePayment(TestData.newPayment(janeOrder, "CREDIT_CARD",
                "SUCCESS", new BigDecimal("20.00")));

        List<Payment> johnsPayments = repository.findPaymentsByUserId(john.getuserid());

        assertThat(johnsPayments).hasSize(1);
        assertThat(johnsPayments.get(0).getAmount()).isEqualByComparingTo("10.00");
    }

    // ========== Payment — NEGATIVE scenarios ==========

    @Test
    public void testFindPaymentsByUserId_NoMatch() {
        assertThat(repository.findPaymentsByUserId(999)).isEmpty();
    }

    @Test
    public void testFindPaymentsByUserId_UserHasOrdersButNoPayments() {
        User user = repository.saveUser(TestData.newJohn());
        repository.saveOrder(TestData.newOrder(user, new BigDecimal("10.00"),
                "PLACED"));

        assertThat(repository.findPaymentsByUserId(user.getuserid())).isEmpty();
    }

    // ========== ProductAvailable / cleanup deletes — POSITIVE scenarios ==========

    @Test
    public void testSaveProductAvailable() {
        User creator = repository.saveUser(TestData.newJohn());
        Product p = persistProduct("Apple", new BigDecimal("2.00"), 50);

        ProductAvailable saved = repository.saveProductAvailable(
                TestData.newProductAvailable(p, creator, "http://img/apple.jpg"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Apple");
        assertThat(saved.getPrice()).isEqualByComparingTo("2.00");
        assertThat(saved.getImageUrl()).isEqualTo("http://img/apple.jpg");
        assertThat(saved.getCreatedBy().getname()).isEqualTo("John");
    }

    @Test
    public void testDeletePaymentsByUserId() {
        User user = repository.saveUser(TestData.newJohn());
        Orders order = repository.saveOrder(TestData.newOrder(user, new BigDecimal("10.00"), "PLACED"));
        repository.savePayment(TestData.newPayment(order, "CREDIT_CARD", "SUCCESS",
                new BigDecimal("10.00")));
        repository.savePayment(TestData.newPayment(order, "CREDIT_CARD", "Decline",
                new BigDecimal("10.00")));

        int deleted = repository.deletePaymentsByUserId(user.getuserid());

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.findPaymentsByUserId(user.getuserid())).isEmpty();
    }

    @Test
    public void testDeletePaymentsByUserId_OnlyDeletesForThatUser() {
        User john = repository.saveUser(TestData.newJohn());
        User jane = repository.saveUser(TestData.newJane());
        Orders johnOrder = repository.saveOrder(TestData.newOrder(john, new BigDecimal("10.00"), "PLACED"));
        Orders janeOrder = repository.saveOrder(TestData.newOrder(jane, new BigDecimal("20.00"), "PLACED"));
        repository.savePayment(TestData.newPayment(johnOrder, "CREDIT_CARD",
                "SUCCESS", new BigDecimal("10.00")));
        repository.savePayment(TestData.newPayment(janeOrder, "CREDIT_CARD",
                "SUCCESS", new BigDecimal("20.00")));

        repository.deletePaymentsByUserId(john.getuserid());

        assertThat(repository.findPaymentsByUserId(john.getuserid())).isEmpty();
        assertThat(repository.findPaymentsByUserId(jane.getuserid())).hasSize(1);
    }

    @Test
    public void testDeleteOrderItemsByUserId() {
        User user = repository.saveUser(TestData.newJohn());
        Orders order = repository.saveOrder(TestData.newOrder(user, new BigDecimal("10.00"), "PLACED"));
        Product apple = persistProduct("Apple", new BigDecimal("2.00"), 50);
        repository.saveOrderItem(TestData.newOrderItem(order, apple, 2, new BigDecimal("2.00")));
        repository.saveOrderItem(TestData.newOrderItem(order, apple, 1, new BigDecimal("2.00")));

        int deleted = repository.deleteOrderItemsByUserId(user.getuserid());

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.findOrderItemsByUserId(user.getuserid())).isEmpty();
    }

    @Test
    public void testDeleteOrdersByUserId() {
        User user = repository.saveUser(TestData.newJohn());
        repository.saveOrder(TestData.newOrder(user, new BigDecimal("10.00"),
                "PLACED"));
        repository.saveOrder(TestData.newOrder(user, new BigDecimal("20.00"),
                "PLACED"));

        int deleted = repository.deleteOrdersByUserId(user.getuserid());

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.findOrdersByUserId(user.getuserid())).isEmpty();
    }

    // ========== NEGATIVE scenarios ==========

    @Test
    public void testDeletePaymentsByUserId_NoMatch() {
        assertThat(repository.deletePaymentsByUserId(999)).isZero();
    }

    @Test
    public void testDeleteOrderItemsByUserId_NoMatch() {
        assertThat(repository.deleteOrderItemsByUserId(999)).isZero();
    }

    @Test
    public void testDeleteOrdersByUserId_NoMatch() {
        assertThat(repository.deleteOrdersByUserId(999)).isZero();
    }

    private Product persistProduct(String name, BigDecimal price, int stock) {
        Category category = new Category();
        category.setName(name + " Category");
        category.setDescription(name + " desc");
        category = repository.saveCategory(category);

        Product p = new Product();
        p.setName(name);
        p.setDescription(name);
        p.setPrice(price);
        p.setStock(stock);
        p.setCategory(category);
        return repository.saveProduct(p);
    }

    private static Role newRole(String fullName, String role, String department) {
        Role r = new Role();
        r.setFullName(fullName);
        r.setRole(role);
        r.setDepartment(department);
        return r;
    }

    private static UserRole newUserRole(User user, Role role) {
        UserRole ur = new UserRole();
        ur.setUser(user);
        ur.setRole(role);
        return ur;
    }
}
