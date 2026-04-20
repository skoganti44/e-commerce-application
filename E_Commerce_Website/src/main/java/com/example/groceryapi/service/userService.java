//A Service is the business logic layer.
//It sits between the Controller and the Repository. Think of it as a manager
// — the controller (receptionist) takes the request, passes it to the service (manager), who decides what to do and asks the repository (database clerk) for data
package com.example.groceryapi.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.groceryapi.dto.ProductRequest;
import com.example.groceryapi.dto.ProductsRequest;
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

    public List<Product> saveProduct(ProductRequest request) {
        Users creator = resolveUser(request.getUserId());
        return saveItems(request.getName(), request.getDescription(), request.getItems(), creator);
    }

    public List<Product> saveProducts(ProductsRequest request) {
        Users creator = resolveUser(request.getUserId());
        List<Product> saved = new ArrayList<>();
        for (ProductsRequest.ProductEntry entry : request.getProducts()) {
            saved.addAll(saveItems(entry.getName(), entry.getDescription(), entry.getItems(), creator));
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
                                    List<ProductRequest.Item> items, Users creator) {
        List<Product> saved = new ArrayList<>();
        for (ProductRequest.Item item : items) {
            Category category = new Category();
            category.setName(item.getCategory().getCategoryName());
            category.setDescription(item.getCategory().getType());
            category = repository.saveCategory(category);

            Product product = new Product();
            product.setName(name);
            product.setDescription(description);
            product.setPrice(item.getPrice());
            product.setStock(item.getStock());
            product.setCategory(category);
            product.setCreatedBy(creator);
            product = repository.saveProduct(product);

            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageUrl(item.getImageUrl());
            repository.saveProductImage(image);

            saved.add(product);
        }
        return saved;
    }
}
