package com.example.groceryapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.groceryapi.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
}
