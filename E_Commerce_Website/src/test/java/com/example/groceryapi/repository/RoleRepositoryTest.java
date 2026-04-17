package com.example.groceryapi.repository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.example.groceryapi.model.Role;

@DataJpaTest
public class RoleRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    public void testSaveRole() {
        Role role = new Role();
        role.setFullName("Alice Smith");
        role.setRole("Manager");
        role.setDepartment("Sales");

        Role saved = roleRepository.save(role);

        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getFullName()).isEqualTo("Alice Smith");
        assertThat(saved.getRole()).isEqualTo("Manager");
        assertThat(saved.getDepartment()).isEqualTo("Sales");
    }

    @Test
    public void testFindAll() {
        Role r1 = new Role();
        r1.setFullName("Alice Smith");
        r1.setRole("Manager");
        r1.setDepartment("Sales");

        Role r2 = new Role();
        r2.setFullName("Bob Johnson");
        r2.setRole("Engineer");
        r2.setDepartment("IT");

        entityManager.persist(r1);
        entityManager.persist(r2);
        entityManager.flush();

        List<Role> roles = roleRepository.findAll();

        assertThat(roles).hasSize(2);
        assertThat(roles.get(0).getFullName()).isEqualTo("Alice Smith");
        assertThat(roles.get(1).getFullName()).isEqualTo("Bob Johnson");
    }

    @Test
    public void testFindById() {
        Role role = new Role();
        role.setFullName("Alice Smith");
        role.setRole("Manager");
        role.setDepartment("Sales");

        Role persisted = entityManager.persist(role);
        entityManager.flush();

        Optional<Role> found = roleRepository.findById(persisted.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Alice Smith");
        assertThat(found.get().getRole()).isEqualTo("Manager");
        assertThat(found.get().getDepartment()).isEqualTo("Sales");
    }

    @Test
    public void testDeleteRole() {
        Role role = new Role();
        role.setFullName("Alice Smith");
        role.setRole("Manager");
        role.setDepartment("Sales");

        Role persisted = entityManager.persist(role);
        entityManager.flush();

        roleRepository.deleteById(persisted.getId());

        Optional<Role> deleted = roleRepository.findById(persisted.getId());
        assertThat(deleted).isEmpty();
    }

    @Test
    public void testFindAll_EmptyRepository() {
        List<Role> roles = roleRepository.findAll();
        assertThat(roles).isEmpty();
    }
}
