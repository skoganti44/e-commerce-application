package main

import (
	"database/sql"
	"errors"
	"strings"
)

// User store ---------------------------------------------------------------

func (db *DB) FindAllUsers() ([]User, error) {
	rows, err := db.Query(`SELECT userid, name, email, password, createdat FROM "User" ORDER BY userid ASC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []User{}
	for rows.Next() {
		var u User
		if err := rows.Scan(&u.UserID, &u.Name, &u.Email, &u.Password, &u.CreatedAt); err != nil {
			return nil, err
		}
		out = append(out, u)
	}
	return out, rows.Err()
}

func (db *DB) FindUserByID(id int) (*User, error) {
	row := db.QueryRow(`SELECT userid, name, email, password, createdat FROM "User" WHERE userid=$1`, id)
	var u User
	if err := row.Scan(&u.UserID, &u.Name, &u.Email, &u.Password, &u.CreatedAt); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &u, nil
}

func (db *DB) FindUserByEmail(email string) (*User, error) {
	row := db.QueryRow(`SELECT userid, name, email, password, createdat FROM "User" WHERE email=$1 LIMIT 1`, email)
	var u User
	if err := row.Scan(&u.UserID, &u.Name, &u.Email, &u.Password, &u.CreatedAt); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &u, nil
}

// SaveUser inserts a new user or updates an existing one. Returns the saved row.
func (db *DB) SaveUser(u *User) (*User, error) {
	if u.UserID == 0 {
		row := db.QueryRow(
			`INSERT INTO "User"(name, email, password, createdat) VALUES($1, $2, $3, COALESCE($4, CURRENT_TIMESTAMP))
			 RETURNING userid, createdat`,
			nullStr(u.Name), nullStr(u.Email), nullStr(u.Password), nullableTime(u.CreatedAt),
		)
		if err := row.Scan(&u.UserID, &u.CreatedAt); err != nil {
			return nil, err
		}
		return u, nil
	}
	_, err := db.Exec(
		`UPDATE "User" SET name=$1, email=$2, password=$3 WHERE userid=$4`,
		nullStr(u.Name), nullStr(u.Email), nullStr(u.Password), u.UserID,
	)
	return u, err
}

func nullableTime(v sql.NullTime) interface{} {
	if !v.Valid {
		return nil
	}
	return v.Time
}

// Role store ---------------------------------------------------------------

func (db *DB) FindAllRoles() ([]Role, error) {
	rows, err := db.Query(`SELECT id, "fullName", role, department FROM "Role" ORDER BY id ASC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Role{}
	for rows.Next() {
		var r Role
		var fn sql.NullString
		var role sql.NullString
		if err := rows.Scan(&r.ID, &fn, &role, &r.Department); err != nil {
			return nil, err
		}
		if fn.Valid {
			r.FullName = fn.String
		}
		if role.Valid {
			r.Role = role.String
		}
		out = append(out, r)
	}
	return out, rows.Err()
}

func (db *DB) FindRolesByDepartment(dept string) ([]Role, error) {
	rows, err := db.Query(`SELECT id, "fullName", role, department FROM "Role" WHERE department=$1 ORDER BY id ASC`, dept)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Role{}
	for rows.Next() {
		var r Role
		var fn, role sql.NullString
		if err := rows.Scan(&r.ID, &fn, &role, &r.Department); err != nil {
			return nil, err
		}
		if fn.Valid {
			r.FullName = fn.String
		}
		if role.Valid {
			r.Role = role.String
		}
		out = append(out, r)
	}
	return out, rows.Err()
}

func (db *DB) FindRoleByName(name string) (*Role, error) {
	row := db.QueryRow(`SELECT id, "fullName", role, department FROM "Role" WHERE LOWER(role)=LOWER($1) LIMIT 1`, name)
	var r Role
	var fn, role sql.NullString
	if err := row.Scan(&r.ID, &fn, &role, &r.Department); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	if fn.Valid {
		r.FullName = fn.String
	}
	if role.Valid {
		r.Role = role.String
	}
	return &r, nil
}

func (db *DB) FindRoleByRoleAndDepartment(role, dept string) (*Role, error) {
	row := db.QueryRow(
		`SELECT id, "fullName", role, department FROM "Role" WHERE LOWER(role)=LOWER($1) AND LOWER(department)=LOWER($2) LIMIT 1`,
		role, dept,
	)
	var r Role
	var fn, rl sql.NullString
	if err := row.Scan(&r.ID, &fn, &rl, &r.Department); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	if fn.Valid {
		r.FullName = fn.String
	}
	if rl.Valid {
		r.Role = rl.String
	}
	return &r, nil
}

func (db *DB) SaveRole(r *Role) (*Role, error) {
	if r.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO "Role"("fullName", role, department) VALUES($1, $2, $3) RETURNING id`,
			r.FullName, r.Role, r.Department,
		)
		if err := row.Scan(&r.ID); err != nil {
			return nil, err
		}
		return r, nil
	}
	_, err := db.Exec(
		`UPDATE "Role" SET "fullName"=$1, role=$2, department=$3 WHERE id=$4`,
		r.FullName, r.Role, r.Department, r.ID,
	)
	return r, err
}

// UserRole store -----------------------------------------------------------

func (db *DB) FindAllUserRoles() ([]UserRole, error) {
	rows, err := db.Query(`SELECT userroleid, userid, roleid FROM "userRole" ORDER BY userroleid ASC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []UserRole{}
	for rows.Next() {
		var ur UserRole
		if err := rows.Scan(&ur.UserRoleID, &ur.UserID, &ur.RoleID); err != nil {
			return nil, err
		}
		out = append(out, ur)
	}
	return out, rows.Err()
}

func (db *DB) FindUserRolesByUserID(userid int) ([]UserRole, error) {
	rows, err := db.Query(`SELECT userroleid, userid, roleid FROM "userRole" WHERE userid=$1 ORDER BY userroleid ASC`, userid)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []UserRole{}
	for rows.Next() {
		var ur UserRole
		if err := rows.Scan(&ur.UserRoleID, &ur.UserID, &ur.RoleID); err != nil {
			return nil, err
		}
		out = append(out, ur)
	}
	return out, rows.Err()
}

func (db *DB) FindUserRolesByRoleID(roleid int) ([]UserRole, error) {
	rows, err := db.Query(`SELECT userroleid, userid, roleid FROM "userRole" WHERE roleid=$1 ORDER BY userroleid ASC`, roleid)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []UserRole{}
	for rows.Next() {
		var ur UserRole
		if err := rows.Scan(&ur.UserRoleID, &ur.UserID, &ur.RoleID); err != nil {
			return nil, err
		}
		out = append(out, ur)
	}
	return out, rows.Err()
}

func (db *DB) SaveUserRole(ur *UserRole) (*UserRole, error) {
	if ur.UserRoleID == 0 {
		row := db.QueryRow(
			`INSERT INTO "userRole"(userid, roleid) VALUES($1, $2) RETURNING userroleid`,
			ur.UserID, ur.RoleID,
		)
		if err := row.Scan(&ur.UserRoleID); err != nil {
			return nil, err
		}
		return ur, nil
	}
	_, err := db.Exec(
		`UPDATE "userRole" SET userid=$1, roleid=$2 WHERE userroleid=$3`,
		ur.UserID, ur.RoleID, ur.UserRoleID,
	)
	return ur, err
}

// Roles for a user ---------------------------------------------------------

func (db *DB) FindRolesByUserID(userid int) ([]Role, error) {
	rows, err := db.Query(
		`SELECT r.id, r."fullName", r.role, r.department
		   FROM "userRole" ur JOIN "Role" r ON r.id = ur.roleid
		  WHERE ur.userid=$1
		  ORDER BY r.id ASC`,
		userid,
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Role{}
	for rows.Next() {
		var r Role
		var fn, rl sql.NullString
		if err := rows.Scan(&r.ID, &fn, &rl, &r.Department); err != nil {
			return nil, err
		}
		if fn.Valid {
			r.FullName = fn.String
		}
		if rl.Valid {
			r.Role = rl.String
		}
		out = append(out, r)
	}
	return out, rows.Err()
}

func (db *DB) RoleNamesForUser(userid int) ([]string, error) {
	roles, err := db.FindRolesByUserID(userid)
	if err != nil {
		return nil, err
	}
	in := make([]string, 0, len(roles))
	for _, r := range roles {
		if strings.TrimSpace(r.Role) != "" {
			in = append(in, r.Role)
		}
	}
	return dedupLower(in), nil
}

func (db *DB) DepartmentsForUser(userid int) ([]string, error) {
	roles, err := db.FindRolesByUserID(userid)
	if err != nil {
		return nil, err
	}
	in := make([]string, 0, len(roles))
	for _, r := range roles {
		if r.Department != nil && strings.TrimSpace(*r.Department) != "" {
			in = append(in, *r.Department)
		}
	}
	return dedupLower(in), nil
}
