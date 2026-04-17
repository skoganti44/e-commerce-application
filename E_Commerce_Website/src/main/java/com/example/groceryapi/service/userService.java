//A Service is the business logic layer.
//It sits between the Controller and the Repository. Think of it as a manager
// — the controller (receptionist) takes the request, passes it to the service (manager), who decides what to do and asks the repository (database clerk) for data
package com.example.groceryapi.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.Users;
import com.example.groceryapi.repository.RoleRepository;
import com.example.groceryapi.repository.UserRepository;

@Service
public class userService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public userService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public List<Users> fetchUsers() {
        return userRepository.findAll();
    }

    public List<Role> fetchRoles() {
        return roleRepository.findAll();
    }
}
