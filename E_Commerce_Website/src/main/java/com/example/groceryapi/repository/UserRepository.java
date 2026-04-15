package com.example.groceryapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.groceryapi.model.Users;

@Repository
public interface UserRepository extends JpaRepository<Users, Integer> {
}
