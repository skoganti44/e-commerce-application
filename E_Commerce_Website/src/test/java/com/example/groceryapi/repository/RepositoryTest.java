package com.example.groceryapi.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.Users;

@DataJpaTest
@Import(Repository.class)
public class RepositoryTest {

    @Autowired
    private Repository repository;

    private Users newUser(String name, String email) {
        Users u = new Users();
        u.setname(name);
        u.setemail(email);
        u.setpassword("pass123");
        u.setcreatedat(LocalDateTime.now());
        return u;
    }

    private Role newRole(String fullName, String role, String department) {
        Role r = new Role();
        r.setFullName(fullName);
        r.setRole(role);
        r.setDepartment(department);
        return r;
    }

    @Test
    public void testSaveUser() {
        Users saved = repository.saveUser(newUser("John", "john@example.com"));

        assertThat(saved.getuserid()).isGreaterThan(0);
        assertThat(saved.getname()).isEqualTo("John");
        assertThat(saved.getemail()).isEqualTo("john@example.com");
    }

    @Test
    public void testFindAllUsers() {
        repository.saveUser(newUser("John", "john@example.com"));
        repository.saveUser(newUser("Jane", "jane@example.com"));

        List<Users> users = repository.findAllUsers();

        assertThat(users).hasSize(2);
        assertThat(users).extracting(Users::getname).containsExactlyInAnyOrder("John", "Jane");
    }

    @Test
    public void testFindUserById() {
        Users persisted = repository.saveUser(newUser("John", "john@example.com"));

        Optional<Users> found = repository.findUserById(persisted.getuserid());

        assertThat(found).isPresent();
        assertThat(found.get().getname()).isEqualTo("John");
    }

    @Test
    public void testDeleteUserById() {
        Users persisted = repository.saveUser(newUser("John", "john@example.com"));

        repository.deleteUserById(persisted.getuserid());

        assertThat(repository.findUserById(persisted.getuserid())).isEmpty();
    }

    @Test
    public void testFindAllUsers_Empty() {
        assertThat(repository.findAllUsers()).isEmpty();
    }

    @Test
    public void testSaveRole() {
        Role saved = repository.saveRole(newRole("Alice Smith", "Manager", "Sales"));

        assertThat(saved.getId()).isGreaterThan(0);
        assertThat(saved.getFullName()).isEqualTo("Alice Smith");
        assertThat(saved.getRole()).isEqualTo("Manager");
        assertThat(saved.getDepartment()).isEqualTo("Sales");
    }

    @Test
    public void testFindAllRoles() {
        repository.saveRole(newRole("Alice Smith", "Manager", "Sales"));
        repository.saveRole(newRole("Bob Johnson", "Engineer", "IT"));

        List<Role> roles = repository.findAllRoles();

        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(Role::getFullName).containsExactlyInAnyOrder("Alice Smith", "Bob Johnson");
    }

    @Test
    public void testFindRoleById() {
        Role persisted = repository.saveRole(newRole("Alice Smith", "Manager", "Sales"));

        Optional<Role> found = repository.findRoleById(persisted.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Alice Smith");
    }

    @Test
    public void testDeleteRoleById() {
        Role persisted = repository.saveRole(newRole("Alice Smith", "Manager", "Sales"));

        repository.deleteRoleById(persisted.getId());

        assertThat(repository.findRoleById(persisted.getId())).isEmpty();
    }

    @Test
    public void testFindAllRoles_Empty() {
        assertThat(repository.findAllRoles()).isEmpty();
    }

    @Test
    public void testFindRolesByDepartment() {
        repository.saveRole(newRole("Alice Smith", "Manager", "Fresh Products"));
        repository.saveRole(newRole("Bob Johnson", "Engineer", "IT"));
        repository.saveRole(newRole("Carol White", "Lead", "Fresh Products"));

        List<Role> roles = repository.findRolesByDepartment("Fresh Products");

        assertThat(roles).hasSize(2);
        assertThat(roles).extracting(Role::getFullName)
                .containsExactlyInAnyOrder("Alice Smith", "Carol White");
    }

    @Test
    public void testFindRolesByDepartment_NoMatch() {
        repository.saveRole(newRole("Alice Smith", "Manager", "Sales"));

        assertThat(repository.findRolesByDepartment("Nonexistent")).isEmpty();
    }
}
