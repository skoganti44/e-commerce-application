package com.example.groceryapi.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.groceryapi.model.Users;
import com.example.groceryapi.repository.UserRepository;

@Service
public class userService {

    private final UserRepository userRepository;

    public userService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<Users> fetchUsers() {
        return userRepository.findAll();
    }
}
