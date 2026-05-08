package main

import (
	"database/sql"
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

var allowedDepartments = newStrSet("bakery", "sales", "kitchen", "delivery", "management")

func (a *App) FetchUsers(c *gin.Context) {
	users, err := a.DB.FindAllUsers()
	if err != nil {
		writeError(c, err)
		return
	}
	out := make([]any, 0, len(users))
	for i := range users {
		out = append(out, a.toPublicUser(&users[i]))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) Register(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	name := strings.TrimSpace(asStr(body["name"]))
	email := strings.TrimSpace(asStr(body["email"]))
	password := asStr(body["password"])
	rawType := strings.ToLower(strings.TrimSpace(asStr(body["userType"])))
	department := strings.TrimSpace(asStr(body["department"]))

	if name == "" {
		writeError(c, badRequest("name is required"))
		return
	}
	if email == "" {
		writeError(c, badRequest("email is required"))
		return
	}
	if password == "" {
		writeError(c, badRequest("password is required"))
		return
	}
	normalizedType := "employee"
	if rawType == "customer" {
		normalizedType = "customer"
	}
	var normalizedDept string
	if normalizedType == "employee" {
		if department == "" {
			writeError(c, badRequest("department is required for employees"))
			return
		}
		normalizedDept = strings.ToLower(department)
		if !allowedDepartments.Has(normalizedDept) {
			writeError(c, badRequest("Invalid department. Allowed: "+strings.Join(allowedDepartments.SortedKeys(), ", ")))
			return
		}
	}

	existing, err := a.DB.FindUserByEmail(email)
	if err != nil {
		writeError(c, err)
		return
	}
	if existing != nil {
		writeError(c, conflict("email already registered: "+email))
		return
	}

	user := &User{
		Name:      &name,
		Email:     &email,
		Password:  &password,
		CreatedAt: sql.NullTime{Time: time.Now(), Valid: true},
	}
	saved, err := a.DB.SaveUser(user)
	if err != nil {
		writeError(c, err)
		return
	}

	var role *Role
	if normalizedType == "customer" {
		role, err = a.DB.FindRoleByName("customer")
		if err != nil {
			writeError(c, err)
			return
		}
		if role == nil {
			role, err = a.DB.SaveRole(&Role{Role: "customer", FullName: "Customer"})
			if err != nil {
				writeError(c, err)
				return
			}
		}
	} else {
		role, err = a.DB.FindRoleByRoleAndDepartment("employee", normalizedDept)
		if err != nil {
			writeError(c, err)
			return
		}
		if role == nil {
			deptCopy := normalizedDept
			role, err = a.DB.SaveRole(&Role{
				Role:       "employee",
				FullName:   "Employee - " + capitalize(normalizedDept),
				Department: &deptCopy,
			})
			if err != nil {
				writeError(c, err)
				return
			}
		}
	}

	if _, err := a.DB.SaveUserRole(&UserRole{UserID: saved.UserID, RoleID: role.ID}); err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toPublicUser(saved))
}

func (a *App) Login(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	email := asStr(body["email"])
	password := asStr(body["password"])
	if email == "" || password == "" {
		c.JSON(401, gin.H{"error": "email and password are required"})
		return
	}
	u, err := a.DB.FindUserByEmail(email)
	if err != nil {
		writeError(c, err)
		return
	}
	if u == nil || u.Password == nil || *u.Password != password {
		c.JSON(401, gin.H{"error": "invalid email or password"})
		return
	}
	roles, err := a.DB.RoleNamesForUser(u.UserID)
	if err != nil {
		writeError(c, err)
		return
	}
	depts, err := a.DB.DepartmentsForUser(u.UserID)
	if err != nil {
		writeError(c, err)
		return
	}
	emailVal := strOrEmpty(u.Email)
	token, err := a.JWT.GenerateToken(u.UserID, emailVal, roles, depts)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, gin.H{"token": token, "user": a.toPublicUser(u)})
}

func (a *App) toPublicUser(u *User) *Object {
	roles, _ := a.DB.RoleNamesForUser(u.UserID)
	depts, _ := a.DB.DepartmentsForUser(u.UserID)
	return NewObject().
		Put("userid", u.UserID).
		Put("name", strOrEmpty(u.Name)).
		Put("email", strOrEmpty(u.Email)).
		Put("createdat", isoOrEmpty(u.CreatedAt)).
		Put("roles", roles).
		Put("departments", depts)
}

func (a *App) FetchRoles(c *gin.Context) {
	dept := strings.TrimSpace(c.Query("department"))
	var roles []Role
	var err error
	if dept == "" {
		roles, err = a.DB.FindAllRoles()
	} else {
		roles, err = a.DB.FindRolesByDepartment(dept)
	}
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, roles)
}

func (a *App) FetchUserRoles(c *gin.Context) {
	uid, hasUser := queryInt(c, "userid")
	rid, hasRole := queryInt(c, "roleid")
	var out []UserRole
	var err error
	switch {
	case hasUser:
		out, err = a.DB.FindUserRolesByUserID(uid)
	case hasRole:
		out, err = a.DB.FindUserRolesByRoleID(rid)
	default:
		out, err = a.DB.FindAllUserRoles()
	}
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

// Shared helpers -----------------------------------------------------------

func asStr(v any) string {
	if v == nil {
		return ""
	}
	if s, ok := v.(string); ok {
		return s
	}
	return ""
}

func strOrEmpty(p *string) string {
	if p == nil {
		return ""
	}
	return *p
}

func isoOrEmpty(t sql.NullTime) string {
	if !t.Valid {
		return ""
	}
	return t.Time.Format("2006-01-02T15:04:05.000000")
}

func isoOrNil(t sql.NullTime) any {
	if !t.Valid {
		return nil
	}
	return t.Time.Format("2006-01-02T15:04:05.000000")
}
