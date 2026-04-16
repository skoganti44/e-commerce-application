//A Controller is the entry point of your API. It receives HTTP requests from clients (Postman, browser, frontend) and returns responses. 
package com.example.groceryapi.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.groceryapi.model.Users;
import com.example.groceryapi.service.userService;

@RestController // does 2things; 1 @controller(Tells Spring "this class handles HTTP requests)
// 2 @ResponseBody(Automatically converts return values to JSON)
public class userController {
    @Autowired // create a userService object and inject it here automatically.
    private userService userService;

    // GetMapping ==> This method handles HTTP GET requests only
    // "/users"==> url to get data http://localhost:8080/users
    @GetMapping(value = "/users", headers = "Accept=application/json")
    public List<Users> fetchUsers() {
        return userService.fetchUsers();
    }
}
