package com.example.groceryapi.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;

import com.example.groceryapi.model.Cart;
import com.example.groceryapi.model.CartItem;
import com.example.groceryapi.model.Role;
import com.example.groceryapi.model.UserRole;
import com.example.groceryapi.model.Users;
import com.example.groceryapi.repository.Repository;
import com.example.groceryapi.testdata.TestData;

@ExtendWith(MockitoExtension.class)
public class userServiceTest {

    @Mock
    private Repository repository;

    @InjectMocks
    private userService userService;

    @Test
    public void testFetchUsers_ReturnsUserList() {
        when(repository.findAllUsers()).thenReturn(TestData.users());

        List<Users> result = userService.fetchUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getname()).isEqualTo("John");
        assertThat(result.get(1).getname()).isEqualTo("Jane");
        verify(repository, times(1)).findAllUsers();
    }

    @Test
    public void testFetchUsers_ReturnsEmptyList() {
        when(repository.findAllUsers()).thenReturn(Collections.emptyList());

        List<Users> result = userService.fetchUsers();

        assertThat(result).isEmpty();
        verify(repository, times(1)).findAllUsers();
    }

    @Test
    public void testFetchUsers_VerifiesCorrectFields() {
        Users expected = TestData.johnMarch();
        when(repository.findAllUsers()).thenReturn(List.of(expected));

        List<Users> result = userService.fetchUsers();

        assertThat(result).hasSize(1);
        Users returnedUser = result.get(0);
        assertThat(returnedUser.getuserid()).isEqualTo(expected.getuserid());
        assertThat(returnedUser.getname()).isEqualTo(expected.getname());
        assertThat(returnedUser.getemail()).isEqualTo(expected.getemail());
        assertThat(returnedUser.getpassword()).isEqualTo(expected.getpassword());
        assertThat(returnedUser.getcreatedat()).isEqualTo(expected.getcreatedat());
    }

    @Test
    public void testFetchRoles_ReturnsRoleList() {
        when(repository.findAllRoles()).thenReturn(List.of(TestData.alice(), TestData.bob()));

        List<Role> result = userService.fetchRoles(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getFullName()).isEqualTo("Alice Smith");
        assertThat(result.get(1).getFullName()).isEqualTo("Bob Johnson");
        verify(repository, times(1)).findAllRoles();
    }

    @Test
    public void testFetchRoles_ReturnsEmptyList() {
        when(repository.findAllRoles()).thenReturn(Collections.emptyList());

        List<Role> result = userService.fetchRoles(null);

        assertThat(result).isEmpty();
        verify(repository, times(1)).findAllRoles();
    }

    @Test
    public void testFetchRoles_FilterByDepartment() {
        Role joe = TestData.joeJonnas();
        when(repository.findRolesByDepartment(TestData.FRESH_PRODUCTS)).thenReturn(List.of(joe));

        List<Role> result = userService.fetchRoles(TestData.FRESH_PRODUCTS);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDepartment()).isEqualTo(TestData.FRESH_PRODUCTS);
        verify(repository, times(1)).findRolesByDepartment(TestData.FRESH_PRODUCTS);
    }

    @Test
    public void testFetchRoles_BlankDepartment_ReturnsAll() {
        when(repository.findAllRoles()).thenReturn(List.of(TestData.alice()));

        List<Role> result = userService.fetchRoles("");

        assertThat(result).hasSize(1);
        verify(repository, times(1)).findAllRoles();
    }

    @Test
    public void testFetchRoles_VerifiesCorrectFields() {
        Role expected = TestData.alice();
        when(repository.findAllRoles()).thenReturn(List.of(expected));

        List<Role> result = userService.fetchRoles(null);

        assertThat(result).hasSize(1);
        Role returned = result.get(0);
        assertThat(returned.getId()).isEqualTo(expected.getId());
        assertThat(returned.getFullName()).isEqualTo(expected.getFullName());
        assertThat(returned.getRole()).isEqualTo(expected.getRole());
        assertThat(returned.getDepartment()).isEqualTo(expected.getDepartment());
    }

    @Test
    public void testFetchUserRoles_NullParams_ReturnsAll() {
        when(repository.findAllUserRoles()).thenReturn(TestData.userRoles());

        List<UserRole> result = userService.fetchUserRoles(null, null);

        assertThat(result).hasSize(2);
        verify(repository, times(1)).findAllUserRoles();
        verify(repository, never()).findUserRolesByUserId(org.mockito.ArgumentMatchers.anyInt());
        verify(repository, never()).findUserRolesByRoleId(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void testFetchUserRoles_ByUserId() {
        when(repository.findUserRolesByUserId(1)).thenReturn(List.of(TestData.johnAsManager()));

        List<UserRole> result = userService.fetchUserRoles(1, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getname()).isEqualTo("John");
        assertThat(result.get(0).getRole().getFullName()).isEqualTo("Joe Jonnas");
        verify(repository, times(1)).findUserRolesByUserId(1);
        verify(repository, never()).findAllUserRoles();
        verify(repository, never()).findUserRolesByRoleId(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void testFetchUserRoles_ByRoleId() {
        when(repository.findUserRolesByRoleId(2)).thenReturn(List.of(TestData.janeAsEngineer()));

        List<UserRole> result = userService.fetchUserRoles(null, 2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getname()).isEqualTo("Jane");
        assertThat(result.get(0).getRole().getFullName()).isEqualTo("Bob Johnson");
        verify(repository, times(1)).findUserRolesByRoleId(2);
        verify(repository, never()).findAllUserRoles();
        verify(repository, never()).findUserRolesByUserId(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void testFetchUserRoles_UserIdTakesPriorityOverRoleId() {
        when(repository.findUserRolesByUserId(1)).thenReturn(List.of(TestData.johnAsManager()));

        List<UserRole> result = userService.fetchUserRoles(1, 2);

        assertThat(result).hasSize(1);
        verify(repository, times(1)).findUserRolesByUserId(1);
        verify(repository, never()).findUserRolesByRoleId(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    public void testFetchUserRoles_ReturnsEmptyList() {
        when(repository.findAllUserRoles()).thenReturn(Collections.emptyList());

        List<UserRole> result = userService.fetchUserRoles(null, null);

        assertThat(result).isEmpty();
        verify(repository, times(1)).findAllUserRoles();
    }

    // ========== fetchCartByUserId — POSITIVE scenarios ==========

    @Test
    @SuppressWarnings("unchecked")
    public void testFetchCartByUserId_ReturnsCartAndItems() {
        Cart cart = TestData.johnsCart();
        List<CartItem> items = TestData.johnsCartItems();
        when(repository.findCartsByUserId(1)).thenReturn(List.of(cart));
        when(repository.findCartItemsByUserId(1)).thenReturn(items);

        Map<String, Object> result = userService.fetchCartByUserId(1);

        assertThat(result).containsKeys("cart", "items");
        assertThat((List<Cart>) result.get("cart")).hasSize(1);
        assertThat((List<CartItem>) result.get("items")).hasSize(3);
        verify(repository, times(1)).findCartsByUserId(1);
        verify(repository, times(1)).findCartItemsByUserId(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFetchCartByUserId_CartExistsButNoItems() {
        when(repository.findCartsByUserId(1)).thenReturn(List.of(TestData.johnsCart()));
        when(repository.findCartItemsByUserId(1)).thenReturn(Collections.emptyList());

        Map<String, Object> result = userService.fetchCartByUserId(1);

        assertThat((List<Cart>) result.get("cart")).hasSize(1);
        assertThat((List<CartItem>) result.get("items")).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFetchCartByUserId_VerifiesItemFields() {
        Cart cart = TestData.johnsCart();
        when(repository.findCartsByUserId(1)).thenReturn(List.of(cart));
        when(repository.findCartItemsByUserId(1)).thenReturn(TestData.johnsCartItems());

        Map<String, Object> result = userService.fetchCartByUserId(1);
        List<CartItem> items = (List<CartItem>) result.get("items");

        assertThat(items.get(0).getProduct().getName()).isEqualTo("Apple");
        assertThat(items.get(0).getQuantity()).isEqualTo(3);
        assertThat(items.get(1).getProduct().getName()).isEqualTo("Milk");
        assertThat(items.get(1).getQuantity()).isEqualTo(6);
        assertThat(items.get(2).getProduct().getName()).isEqualTo("Bread");
        assertThat(items.get(2).getQuantity()).isEqualTo(10);
    }

    // ========== fetchCartByUserId — NEGATIVE scenarios ==========

    @Test
    public void testFetchCartByUserId_NoCartForUser_ThrowsException() {
        when(repository.findCartsByUserId(99)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> userService.fetchCartByUserId(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No cart found for userId: 99");

        verify(repository, times(1)).findCartsByUserId(99);
        verify(repository, never()).findCartItemsByUserId(org.mockito.ArgumentMatchers.anyInt());
    }
}
