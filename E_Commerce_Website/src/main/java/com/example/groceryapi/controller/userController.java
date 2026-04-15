package com.example.groceryapi.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.groceryapi.model.Users;
import com.example.groceryapi.service.userService;

@RestController
public class userController {
    @Autowired
    private userService userService;

    @GetMapping(value = "/users", headers = "Accept=application/json")
    public List<Users> fetchUsers() {
        return userService.fetchUsers();
    }
}
