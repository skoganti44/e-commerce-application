package com.example.groceryapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.model.Users;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
@Transactional
public class Repository {

    private static final String SELECT_ALL_USERS = "SELECT u FROM Users u";
    private static final String DELETE_ALL_USERS = "DELETE FROM Users";

    private static final String SELECT_ALL_ROLES = "SELECT r FROM Role r";
    private static final String SELECT_ROLES_BY_DEPARTMENT = "SELECT r FROM Role r WHERE r.department = :department";
    private static final String DELETE_ALL_ROLES = "DELETE FROM Role";

    private static final String SELECT_ALL_USER_ROLES = "SELECT ur FROM UserRole ur";
    private static final String SELECT_USER_ROLES_BY_USER_ID = "SELECT ur FROM UserRole ur WHERE ur.user.userid = :userid";
    private static final String SELECT_USER_ROLES_BY_ROLE_ID = "SELECT ur FROM UserRole ur WHERE ur.role.id = :roleid";
    private static final String DELETE_ALL_USER_ROLES = "DELETE FROM UserRole";

    private static final String SELECT_ALL_CATEGORIES = "SELECT c FROM Category c";

    private static final String SELECT_ALL_PRODUCTS = "SELECT p FROM Product p";

    private static final String SELECT_IMAGES_BY_PRODUCT_ID = "SELECT pi FROM ProductImage pi WHERE pi.product.id = :pid";

    private static final String SELECT_CART_BY_USER_ID = "SELECT c FROM Cart c WHERE c.user.userid = :userid";
    private static final String SELECT_CART_ITEMS_BY_USER_ID = "SELECT ci FROM CartItem ci WHERE ci.cart.user.userid = :userid";

    private static final String SELECT_ORDERS_BY_USER_ID = "SELECT o FROM Orders o WHERE o.user.userid = :userid";
    private static final String SELECT_ORDER_ITEMS_BY_USER_ID = "SELECT oi FROM OrderItem oi WHERE oi.order.user.userid = :userid";
    private static final String SELECT_ROLES_BY_USER_ID = "SELECT ur.role FROM UserRole ur WHERE ur.user.userid = :userid";

    private static final String SELECT_PAYMENTS_BY_USER_ID = "SELECT p FROM Payment p WHERE p.order.user.userid = :userid";

    private static final String DELETE_PAYMENTS_BY_USER_ID = "DELETE FROM Payment p WHERE p.order.id IN (SELECT o.id FROM Orders o WHERE o.user.userid = :userid)";
    private static final String DELETE_ORDER_ITEMS_BY_USER_ID = "DELETE FROM OrderItem oi WHERE oi.order.id IN (SELECT o.id FROM Orders o WHERE o.user.userid = :userid)";
    private static final String DELETE_ORDERS_BY_USER_ID = "DELETE FROM Orders o WHERE o.user.userid = :userid";

    @PersistenceContext
    private EntityManager em;

    public List<Users> findAllUsers() {
        return em.createQuery(SELECT_ALL_USERS, Users.class).getResultList();
    }

    public Optional<Users> findUserById(int id) {
        return Optional.ofNullable(em.find(Users.class, id));
    }

    public Users saveUser(Users user) {
        if (user.getuserid() == 0) {
            em.persist(user);
            return user;
        }
        return em.merge(user);
    }

    public void deleteUserById(int id) {
        Users user = em.find(Users.class, id);
        if (user != null) {
            em.remove(user);
        }
    }

    public void deleteAllUsers() {
        em.createQuery(DELETE_ALL_USERS).executeUpdate();
    }

    public List<Role> findAllRoles() {
        return em.createQuery(SELECT_ALL_ROLES, Role.class).getResultList();
    }

    public List<Role> findRolesByDepartment(String department) {
        return em.createQuery(SELECT_ROLES_BY_DEPARTMENT, Role.class)
                .setParameter("department", department)
                .getResultList();
    }

    public Optional<Role> findRoleById(int id) {
        return Optional.ofNullable(em.find(Role.class, id));
    }

    public Role saveRole(Role role) {
        if (role.getId() == 0) {
            em.persist(role);
            return role;
        }
        return em.merge(role);
    }

    public void deleteRoleById(int id) {
        Role role = em.find(Role.class, id);
        if (role != null) {
            em.remove(role);
        }
    }

    public void deleteAllRoles() {
        em.createQuery(DELETE_ALL_ROLES).executeUpdate();
    }

    public List<UserRole> findAllUserRoles() {
        return em.createQuery(SELECT_ALL_USER_ROLES, UserRole.class).getResultList();
    }

    public List<UserRole> findUserRolesByUserId(int userid) {
        return em.createQuery(SELECT_USER_ROLES_BY_USER_ID, UserRole.class)
                .setParameter("userid", userid)
                .getResultList();
    }

    public List<UserRole> findUserRolesByRoleId(int roleid) {
        return em.createQuery(SELECT_USER_ROLES_BY_ROLE_ID, UserRole.class)
                .setParameter("roleid", roleid)
                .getResultList();
    }

    public UserRole saveUserRole(UserRole userRole) {
        if (userRole.getUserroleid() == 0) {
            em.persist(userRole);
            return userRole;
        }
        return em.merge(userRole);
    }

    public void deleteAllUserRoles() {
        em.createQuery(DELETE_ALL_USER_ROLES).executeUpdate();
    }

    public Category saveCategory(Category category) {
        if (category.getId() == null) {
            em.persist(category);
            return category;
        }
        return em.merge(category);
    }

    public List<Category> findAllCategories() {
        return em.createQuery(SELECT_ALL_CATEGORIES, Category.class).getResultList();
    }

    public Product saveProduct(Product product) {
        if (product.getId() == null) {
            em.persist(product);
            em.flush();
            em.refresh(product);
            return product;
        }
        return em.merge(product);
    }

    public List<Product> findAllProducts() {
        return em.createQuery(SELECT_ALL_PRODUCTS, Product.class).getResultList();
    }

    public Optional<Product> findProductById(long id) {
        return Optional.ofNullable(em.find(Product.class, id));
    }

    public ProductImage saveProductImage(ProductImage image) {
        if (image.getId() == null) {
            em.persist(image);
            return image;
        }
        return em.merge(image);
    }

    public List<ProductImage> findImagesByProductId(long productId) {
        return em.createQuery(SELECT_IMAGES_BY_PRODUCT_ID, ProductImage.class)
                .setParameter("pid", productId)
                .getResultList();
    }

    public Cart saveCart(Cart cart) {
        if (cart.getId() == null) {
            em.persist(cart);
            return cart;
        }
        return em.merge(cart);
    }

    public CartItem saveCartItem(CartItem item) {
        if (item.getId() == null) {
            em.persist(item);
            return item;
        }
        return em.merge(item);
    }

    public List<Cart> findCartsByUserId(int userid) {
        return em.createQuery(SELECT_CART_BY_USER_ID, Cart.class)
                .setParameter("userid", userid)
                .getResultList();
    }

    public List<CartItem> findCartItemsByUserId(int userid) {
        return em.createQuery(SELECT_CART_ITEMS_BY_USER_ID, CartItem.class)
                .setParameter("userid", userid)
                .getResultList();
    }

    public Orders saveOrder(Orders order) {
        if (order.getId() == null) {
            em.persist(order);
            return order;
        }
        return em.merge(order);
    }

    public OrderItem saveOrderItem(OrderItem item) {
        if (item.getId() == null) {
            em.persist(item);
            return item;
        }
        return em.merge(item);
    }

    public List<Orders> findOrdersByUserId(int userid) {
        return em.createQuery(SELECT_ORDERS_BY_USER_ID, Orders.class)
                .setParameter("userid", userid)
                .getResultList();
    }

    public List<OrderItem> findOrderItemsByUserId(int userid) {
        return em.createQuery(SELECT_ORDER_ITEMS_BY_USER_ID, OrderItem.class)
                .setParameter("userid", userid)
                .getResultList();
    }

    public List<Role> findRolesByUserId(int userid) {
        return em.createQuery(SELECT_ROLES_BY_USER_ID, Role.class)
                .setParameter("userid", userid)
                .getResultList();
    }

    public Payment savePayment(Payment payment) {
        if (payment.getId() == null) {
            em.persist(payment);
            return payment;
        }
        return em.merge(payment);
    }

    public List<Payment> findPaymentsByUserId(int userid) {
        return em.createQuery(SELECT_PAYMENTS_BY_USER_ID, Payment.class)
                .setParameter("userid", userid)
                .getResultList();
    }

    public ProductAvailable saveProductAvailable(ProductAvailable pa) {
        if (pa.getId() == null) {
            em.persist(pa);
            return pa;
        }
        return em.merge(pa);
    }

    public int deletePaymentsByUserId(int userid) {
        return em.createQuery(DELETE_PAYMENTS_BY_USER_ID)
                .setParameter("userid", userid)
                .executeUpdate();
    }

    public int deleteOrderItemsByUserId(int userid) {
        return em.createQuery(DELETE_ORDER_ITEMS_BY_USER_ID)
                .setParameter("userid", userid)
                .executeUpdate();
    }

    public int deleteOrdersByUserId(int userid) {
        return em.createQuery(DELETE_ORDERS_BY_USER_ID)
                .setParameter("userid", userid)
                .executeUpdate();
    }
}
