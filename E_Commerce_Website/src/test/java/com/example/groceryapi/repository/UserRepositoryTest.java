package com.example.groceryapi.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.example.groceryapi.model.Users;

@DataJpaTest
public class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testSaveUser() {
        Users user = new Users();
        user.setname("John");
        user.setemail("john@example.com");
        user.setpassword("pass123");
        user.setcreatedat(LocalDateTime.now());

        Users savedUser = userRepository.save(user);

        assertThat(savedUser.getuserid()).isGreaterThan(0);
        assertThat(savedUser.getname()).isEqualTo("John");
        assertThat(savedUser.getemail()).isEqualTo("john@example.com");
    }

    @Test
    public void testFindAll() {
        Users user1 = new Users();
        user1.setname("John");
        user1.setemail("john@example.com");
        user1.setpassword("pass123");
        user1.setcreatedat(LocalDateTime.now());

        Users user2 = new Users();
        user2.setname("Jane");
        user2.setemail("jane@example.com");
        user2.setpassword("pass456");
        user2.setcreatedat(LocalDateTime.now());

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        List<Users> users = userRepository.findAll();

        assertThat(users).hasSize(2);
        assertThat(users.get(0).getname()).isEqualTo("John");
        assertThat(users.get(1).getname()).isEqualTo("Jane");
    }

    @Test
    public void testFindById() {
        Users user = new Users();
        user.setname("John");
        user.setemail("john@example.com");
        user.setpassword("pass123");
        user.setcreatedat(LocalDateTime.now());

        Users persisted = entityManager.persist(user);
        entityManager.flush();

        Optional<Users> found = userRepository.findById(persisted.getuserid());

        assertThat(found).isPresent();
        assertThat(found.get().getname()).isEqualTo("John");
        assertThat(found.get().getemail()).isEqualTo("john@example.com");
    }

    @Test
    public void testDeleteUser() {
        Users user = new Users();
        user.setname("John");
        user.setemail("john@example.com");
        user.setpassword("pass123");
        user.setcreatedat(LocalDateTime.now());

        Users persisted = entityManager.persist(user);
        entityManager.flush();

        userRepository.deleteById(persisted.getuserid());

        Optional<Users> deleted = userRepository.findById(persisted.getuserid());
        assertThat(deleted).isEmpty();
    }

    @Test
    public void testFindAll_EmptyRepository() {
        List<Users> users = userRepository.findAll();
        assertThat(users).isEmpty();
    }
}
