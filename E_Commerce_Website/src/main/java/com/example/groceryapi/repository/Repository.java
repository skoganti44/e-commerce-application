package com.example.groceryapi.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.groceryapi.model.Category;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.ProductImage;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.model.Users;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
@Transactional
public class Repository {

    @PersistenceContext
    private EntityManager em;

    public List<Users> findAllUsers() {
        return em.createQuery("SELECT u FROM Users u", Users.class).getResultList();
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
        em.createQuery("DELETE FROM Users").executeUpdate();
    }

    public List<Role> findAllRoles() {
        return em.createQuery("SELECT r FROM Role r", Role.class).getResultList();
    }

    public List<Role> findRolesByDepartment(String department) {
        return em.createQuery("SELECT r FROM Role r WHERE r.department = :department", Role.class)
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
        em.createQuery("DELETE FROM Role").executeUpdate();
    }

    public List<UserRole> findAllUserRoles() {
        return em.createQuery("SELECT ur FROM UserRole ur", UserRole.class).getResultList();
    }

    public List<UserRole> findUserRolesByUserId(int userid) {
        return em.createQuery("SELECT ur FROM UserRole ur WHERE ur.user.userid = :userid", UserRole.class)
                .setParameter("userid", userid)
                .getResultList();
    }

    public List<UserRole> findUserRolesByRoleId(int roleid) {
        return em.createQuery("SELECT ur FROM UserRole ur WHERE ur.role.id = :roleid", UserRole.class)
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
        em.createQuery("DELETE FROM UserRole").executeUpdate();
    }

    public Category saveCategory(Category category) {
        if (category.getId() == null) {
            em.persist(category);
            return category;
        }
        return em.merge(category);
    }

    public List<Category> findAllCategories() {
        return em.createQuery("SELECT c FROM Category c", Category.class).getResultList();
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
        return em.createQuery("SELECT p FROM Product p", Product.class).getResultList();
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
        return em.createQuery("SELECT pi FROM ProductImage pi WHERE pi.product.id = :pid", ProductImage.class)
                .setParameter("pid", productId)
                .getResultList();
    }
}
