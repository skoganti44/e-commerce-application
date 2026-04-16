//A Repository is the layer that talks to the database
package com.example.groceryapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.groceryapi.model.Users;

@Repository
// @Repository==> Marks as a spring bean, autodetection by spring
public interface UserRepository extends JpaRepository<Users, Integer> {
}
