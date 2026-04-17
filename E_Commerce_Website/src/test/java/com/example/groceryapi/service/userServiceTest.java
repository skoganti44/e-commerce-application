package com.example.groceryapi.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.Users;
import com.example.groceryapi.repository.RoleRepository;
import com.example.groceryapi.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
public class userServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private userService userService;

    @Test
    public void testFetchUsers_ReturnsUserList() {
        Users user1 = new Users();
        user1.setuserid(1);
        user1.setname("John");
        user1.setemail("john@example.com");
        user1.setpassword("pass123");
        user1.setcreatedat(LocalDateTime.of(2025, 1, 1, 10, 0));

        Users user2 = new Users();
        user2.setuserid(2);
        user2.setname("Jane");
        user2.setemail("jane@example.com");
        user2.setpassword("pass456");
        user2.setcreatedat(LocalDateTime.of(2025, 2, 1, 12, 0));

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        List<Users> result = userService.fetchUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getname()).isEqualTo("John");
        assertThat(result.get(1).getname()).isEqualTo("Jane");
        verify(userRepository, times(1)).findAll();
    }

    @Test
    public void testFetchUsers_ReturnsEmptyList() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<Users> result = userService.fetchUsers();

        assertThat(result).isEmpty();
        verify(userRepository, times(1)).findAll();
    }

    @Test
    public void testFetchUsers_VerifiesCorrectFields() {
        Users user = new Users();
        user.setuserid(1);
        user.setname("John");
        user.setemail("john@example.com");
        user.setpassword("pass123");
        user.setcreatedat(LocalDateTime.of(2025, 3, 15, 9, 30));

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<Users> result = userService.fetchUsers();

        assertThat(result).hasSize(1);
        Users returnedUser = result.get(0);
        assertThat(returnedUser.getuserid()).isEqualTo(1);
        assertThat(returnedUser.getname()).isEqualTo("John");
        assertThat(returnedUser.getemail()).isEqualTo("john@example.com");
        assertThat(returnedUser.getpassword()).isEqualTo("pass123");
        assertThat(returnedUser.getcreatedat()).isEqualTo(LocalDateTime.of(2025, 3, 15, 9, 30));
    }

    @Test
    public void testFetchRoles_ReturnsRoleList() {
        Role role1 = new Role();
        role1.setId(1);
        role1.setFullName("Alice Smith");
        role1.setRole("Manager");
        role1.setDepartment("Sales");

        Role role2 = new Role();
        role2.setId(2);
        role2.setFullName("Bob Johnson");
        role2.setRole("Engineer");
        role2.setDepartment("IT");

        when(roleRepository.findAll()).thenReturn(Arrays.asList(role1, role2));

        List<Role> result = userService.fetchRoles();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFullName()).isEqualTo("Alice Smith");
        assertThat(result.get(1).getFullName()).isEqualTo("Bob Johnson");
        verify(roleRepository, times(1)).findAll();
    }

    @Test
    public void testFetchRoles_ReturnsEmptyList() {
        when(roleRepository.findAll()).thenReturn(Collections.emptyList());

        List<Role> result = userService.fetchRoles();

        assertThat(result).isEmpty();
        verify(roleRepository, times(1)).findAll();
    }

    @Test
    public void testFetchRoles_VerifiesCorrectFields() {
        Role role = new Role();
        role.setId(1);
        role.setFullName("Alice Smith");
        role.setRole("Manager");
        role.setDepartment("Sales");

        when(roleRepository.findAll()).thenReturn(List.of(role));

        List<Role> result = userService.fetchRoles();

        assertThat(result).hasSize(1);
        Role returned = result.get(0);
        assertThat(returned.getId()).isEqualTo(1);
        assertThat(returned.getFullName()).isEqualTo("Alice Smith");
        assertThat(returned.getRole()).isEqualTo("Manager");
        assertThat(returned.getDepartment()).isEqualTo("Sales");
    }
}
