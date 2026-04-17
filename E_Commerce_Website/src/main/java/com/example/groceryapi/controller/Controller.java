//A Controller is the entry point of your API. It receives HTTP requests from clients (Postman, browser, frontend) and returns responses.
package com.example.groceryapi.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.Users;
import com.example.groceryapi.service.userService;

@RestController
public class Controller {
    @Autowired
    private userService userService;

    @GetMapping(value = "/users", headers = "Accept=application/json")
    public List<Users> fetchUsers() {
        return userService.fetchUsers();
    }

    @GetMapping(value = "/roles", headers = "Accept=application/json")
    public List<Role> fetchRoles(@RequestParam(required = false) String department) {
        return userService.fetchRoles(department);
    }
}
