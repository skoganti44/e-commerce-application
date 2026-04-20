//A Service is the business logic layer.
//It sits between the Controller and the Repository. Think of it as a manager
// — the controller (receptionist) takes the request, passes it to the service (manager), who decides what to do and asks the repository (database clerk) for data
package com.example.groceryapi.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.groceryapi.model.Category;
import com.example.groceryapi.model.Product;
import com.example.groceryapi.model.ProductImage;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.model.Users;
import com.example.groceryapi.repository.Repository;

@Service
public class userService {

    private final Repository repository;

    public userService(Repository repository) {
        this.repository = repository;
    }

    public List<Users> fetchUsers() {
        return repository.findAllUsers();
    }

    public List<Role> fetchRoles(String department) {
        if (department == null || department.isBlank()) {
            return repository.findAllRoles();
        }
        return repository.findRolesByDepartment(department);
    }

    public List<UserRole> fetchUserRoles(Integer userid, Integer roleid) {
        if (userid != null) {
            return repository.findUserRolesByUserId(userid);
        }
        if (roleid != null) {
            return repository.findUserRolesByRoleId(roleid);
        }
        return repository.findAllUserRoles();
    }

    public List<Product> saveProduct(Map<String, Object> request) {
        Users creator = resolveUser((Integer) request.get("userId"));
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        List<Map<String, Object>> items = (List<Map<String, Object>>) request.get("items");
        return saveItems(name, description, items, creator);
    }

    public List<Product> saveProducts(Map<String, Object> request) {
        Users creator = resolveUser((Integer) request.get("userId"));
        List<Map<String, Object>> products = (List<Map<String, Object>>) request.get("products");
        List<Product> saved = new ArrayList<>();
        for (Map<String, Object> p : products) {
            String name = (String) p.get("name");
            String description = (String) p.get("description");
            List<Map<String, Object>> items = (List<Map<String, Object>>) p.get("items");
            saved.addAll(saveItems(name, description, items, creator));
        }
        return saved;
    }

    private Users resolveUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        return repository.findUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    }

    private List<Product> saveItems(String name, String description,
                                    List<Map<String, Object>> items, Users creator) {
        List<Product> saved = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> cat = (Map<String, Object>) item.get("category");

            Category category = new Category();
            category.setName((String) cat.get("categoryName"));
            category.setDescription((String) cat.get("type"));
            category = repository.saveCategory(category);

            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(new BigDecimal(item.get("price").toString()));
            product.setStock(((Number) item.get("stock")).intValue());
            product.setCategory(category);
            product.setCreatedBy(creator);
            product = repository.saveProduct(product);

            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl((String) item.get("imageUrl"));
            repository.saveProductImage(image);

            saved.add(product);
        }
        return saved;
    }
}
