//A Service is the business logic layer.
//It sits between the Controller and the Repository. Think of it as a manager
// — the controller (receptionist) takes the request, passes it to the service (manager), who decides what to do and asks the repository (database clerk) for data
package com.example.groceryapi.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.groceryapi.model.Role;
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
}
