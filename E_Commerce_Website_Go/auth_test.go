package main

import (
	"net/http"
	"testing"
)

// ---------- POST /register ----------

func TestRegister_Customer_OK(t *testing.T) {
	body := map[string]string{
		"name":     "Test Customer",
		"email":    uniqueEmail("cust"),
		"password": "TestPass1!",
		"userType": "customer",
	}
	w := do(t, http.MethodPost, "/register", body, nil)
	assertStatus(t, w, http.StatusOK)
	var u map[string]any
	mustDecode(t, w, &u)
	if u["userid"] == nil {
		t.Fatalf("expected userid in response, got %v", u)
	}
	if u["email"] != body["email"] {
		t.Fatalf("expected email echoed back, got %v", u["email"])
	}
}

func TestRegister_Employee_OK(t *testing.T) {
	body := map[string]string{
		"name":       "Test Baker",
		"email":      uniqueEmail("emp"),
		"password":   "TestPass1!",
		"userType":   "employee",
		"department": "bakery",
	}
	w := do(t, http.MethodPost, "/register", body, nil)
	assertStatus(t, w, http.StatusOK)
}

func TestRegister_DuplicateEmail_Conflict(t *testing.T) {
	body := map[string]string{
		"name":     "Dup",
		"email":    customerEmail, // already in seed data
		"password": "x",
		"userType": "customer",
	}
	w := do(t, http.MethodPost, "/register", body, nil)
	assertStatus(t, w, http.StatusConflict)
	assertErrorContains(t, w, "already registered")
}

func TestRegister_MissingName_BadRequest(t *testing.T) {
	body := map[string]string{
		"email":    "noname@example.test",
		"password": "x",
	}
	w := do(t, http.MethodPost, "/register", body, nil)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "name is required")
}

func TestRegister_MissingEmail_BadRequest(t *testing.T) {
	body := map[string]string{"name": "x", "password": "x"}
	w := do(t, http.MethodPost, "/register", body, nil)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "email is required")
}

func TestRegister_MissingPassword_BadRequest(t *testing.T) {
	body := map[string]string{"name": "x", "email": uniqueEmail("nopw")}
	w := do(t, http.MethodPost, "/register", body, nil)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "password is required")
}

func TestRegister_EmployeeMissingDepartment_BadRequest(t *testing.T) {
	body := map[string]string{
		"name":     "x",
		"email":    uniqueEmail("emp"),
		"password": "x",
		"userType": "employee",
	}
	w := do(t, http.MethodPost, "/register", body, nil)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "department is required")
}

func TestRegister_InvalidDepartment_BadRequest(t *testing.T) {
	body := map[string]string{
		"name":       "x",
		"email":      uniqueEmail("emp"),
		"password":   "x",
		"userType":   "employee",
		"department": "moonbase",
	}
	w := do(t, http.MethodPost, "/register", body, nil)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "invalid department")
}

// ---------- POST /login ----------

func TestLogin_OK(t *testing.T) {
	w := do(t, http.MethodPost, "/login", map[string]string{
		"email":    customerEmail,
		"password": customerPass,
	}, nil)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	if resp["token"] == nil {
		t.Fatal("expected token in response")
	}
	user, _ := resp["user"].(map[string]any)
	if user == nil || user["userid"] == nil {
		t.Fatalf("expected user object, got %v", resp)
	}
}

func TestLogin_WrongPassword_Unauthorized(t *testing.T) {
	w := do(t, http.MethodPost, "/login", map[string]string{
		"email":    customerEmail,
		"password": "definitely-not-right",
	}, nil)
	assertStatus(t, w, http.StatusUnauthorized)
	assertErrorContains(t, w, "invalid")
}

func TestLogin_UnknownEmail_Unauthorized(t *testing.T) {
	w := do(t, http.MethodPost, "/login", map[string]string{
		"email":    "nobody@nope.test",
		"password": "x",
	}, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}

func TestLogin_MissingFields_Unauthorized(t *testing.T) {
	w := do(t, http.MethodPost, "/login", map[string]string{}, nil)
	assertStatus(t, w, http.StatusUnauthorized)
	assertErrorContains(t, w, "required")
}

// ---------- GET /users ----------

func TestUsers_Unauthorized(t *testing.T) {
	w := do(t, http.MethodGet, "/users", nil, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}

func TestUsers_OK(t *testing.T) {
	w := do(t, http.MethodGet, "/users", nil, auth(t, customerEmail, customerPass))
	assertStatus(t, w, http.StatusOK)
	var users []map[string]any
	mustDecode(t, w, &users)
	if len(users) < 6 {
		t.Fatalf("expected at least 6 seeded users, got %d", len(users))
	}
}

// ---------- GET /roles ----------

func TestRoles_All_OK(t *testing.T) {
	w := do(t, http.MethodGet, "/roles", nil, auth(t, customerEmail, customerPass))
	assertStatus(t, w, http.StatusOK)
	var roles []map[string]any
	mustDecode(t, w, &roles)
	if len(roles) == 0 {
		t.Fatal("expected at least one role")
	}
}

func TestRoles_FilterByDepartment_OK(t *testing.T) {
	w := do(t, http.MethodGet, "/roles?department=bakery", nil, auth(t, customerEmail, customerPass))
	assertStatus(t, w, http.StatusOK)
	var roles []map[string]any
	mustDecode(t, w, &roles)
	for _, r := range roles {
		if r["department"] != "bakery" {
			t.Fatalf("expected only bakery, got %v", r)
		}
	}
}

func TestRoles_Unauthorized(t *testing.T) {
	w := do(t, http.MethodGet, "/roles", nil, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}

// ---------- GET /userRoles ----------

func TestUserRoles_All_OK(t *testing.T) {
	w := do(t, http.MethodGet, "/userRoles", nil, auth(t, customerEmail, customerPass))
	assertStatus(t, w, http.StatusOK)
}

func TestUserRoles_FilterByUserID_OK(t *testing.T) {
	w := do(t, http.MethodGet, "/userRoles?userid=1", nil, auth(t, customerEmail, customerPass))
	assertStatus(t, w, http.StatusOK)
	var rows []map[string]any
	mustDecode(t, w, &rows)
	for _, row := range rows {
		if int(row["userid"].(float64)) != 1 {
			t.Fatalf("expected only userid=1, got %v", row)
		}
	}
}

func TestUserRoles_FilterByRoleID_OK(t *testing.T) {
	w := do(t, http.MethodGet, "/userRoles?roleid=1", nil, auth(t, customerEmail, customerPass))
	assertStatus(t, w, http.StatusOK)
}

func TestUserRoles_Unauthorized(t *testing.T) {
	w := do(t, http.MethodGet, "/userRoles", nil, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}
