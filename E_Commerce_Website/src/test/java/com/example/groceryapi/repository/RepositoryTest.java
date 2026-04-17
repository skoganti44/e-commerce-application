package com.example.groceryapi.repository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.Users;
import com.example.groceryapi.testdata.TestData;

@DataJpaTest
@Import(Repository.class)
public class RepositoryTest {

    @Autowired
    private Repository repository;

    @Test
    public void testSaveUser() {
        Users saved = repository.saveUser(TestData.newJohn());

        assertThat(saved.getuserid()).isGreaterThan(0);
        assertThat(saved.getname()).isEqualTo("John");
        assertThat(saved.getemail()).isEqualTo("john@example.com");
    }

    @Test
    public void testFindAllUsers() {
        repository.saveUser(TestData.newJohn());
        repository.saveUser(TestData.newJane());

        List<Users> users = repository.findAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(Users::getname).containsExactlyInAnyOrder("John", "Jane");
    }

    @Test
    public void testFindUserById() {
        Users persisted = repository.saveUser(TestData.newJohn());

        Optional<Users> found = repository.findUserById(persisted.getuserid());

        assertThat(found).isPresent();
        assertThat(found.get().getname()).isEqualTo("John");
    }

    @Test
    public void testDeleteUserById() {
        Users persisted = repository.saveUser(TestData.newJohn());

        repository.deleteUserById(persisted.getuserid());

        assertThat(repository.findUserById(persisted.getuserid())).isEmpty();
    }

    @Test
    public void testFindAllUsers_Empty() {
        assertThat(repository.findAllUsers()).isEmpty();
    }

    @Test
    public void testSaveRole() {
        Role saved = repository.saveRole(newRole("Alice Smith", "Manager", TestData.SALES));

        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getFullName()).isEqualTo("Alice Smith");
        assertThat(saved.getRole()).isEqualTo("Manager");
        assertThat(saved.getDepartment()).isEqualTo(TestData.SALES);
    }

    @Test
    public void testFindAllRoles() {
        repository.saveRole(newRole("Alice Smith", "Manager", TestData.SALES));
        repository.saveRole(newRole("Bob Johnson", "Engineer", TestData.IT));

        List<Role> roles = repository.findAllRoles();

        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(Role::getFullName).containsExactlyInAnyOrder("Alice Smith", "Bob Johnson");
    }

    @Test
    public void testFindRoleById() {
        Role persisted = repository.saveRole(newRole("Alice Smith", "Manager", TestData.SALES));

        Optional<Role> found = repository.findRoleById(persisted.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Alice Smith");
    }

    @Test
    public void testDeleteRoleById() {
        Role persisted = repository.saveRole(newRole("Alice Smith", "Manager", TestData.SALES));

        repository.deleteRoleById(persisted.getId());

        assertThat(repository.findRoleById(persisted.getId())).isEmpty();
    }

    @Test
    public void testFindAllRoles_Empty() {
        assertThat(repository.findAllRoles()).isEmpty();
    }

    @Test
    public void testFindRolesByDepartment() {
        repository.saveRole(newRole("Alice Smith", "Manager", TestData.FRESH_PRODUCTS));
        repository.saveRole(newRole("Bob Johnson", "Engineer", TestData.IT));
        repository.saveRole(newRole("Carol White", "Lead", TestData.FRESH_PRODUCTS));

        List<Role> roles = repository.findRolesByDepartment(TestData.FRESH_PRODUCTS);

        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(Role::getFullName)
                .containsExactlyInAnyOrder("Alice Smith", "Carol White");
    }

    @Test
    public void testFindRolesByDepartment_NoMatch() {
        repository.saveRole(newRole("Alice Smith", "Manager", TestData.SALES));

        assertThat(repository.findRolesByDepartment("Nonexistent")).isEmpty();
    }

    private static Role newRole(String fullName, String role, String department) {
        Role r = new Role();
        r.setFullName(fullName);
        r.setRole(role);
        r.setDepartment(department);
        return r;
    }
}
