package main

import (
	"database/sql"
	"errors"
)

func (db *DB) FindCartsByUserID(userid int) ([]Cart, error) {
	rows, err := db.Query(`SELECT id, COALESCE(user_id, userid), createdat FROM "Cart"
		WHERE COALESCE(user_id, userid)=$1 ORDER BY id ASC`, userid)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Cart{}
	for rows.Next() {
		var c Cart
		if err := rows.Scan(&c.ID, &c.UserID, &c.CreatedAt); err != nil {
			return nil, err
		}
		out = append(out, c)
	}
	return out, rows.Err()
}

func (db *DB) SaveCart(c *Cart) (*Cart, error) {
	if c.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO "Cart"(userid, user_id, createdat, created_at)
			 VALUES($1,$1,COALESCE($2, CURRENT_TIMESTAMP),COALESCE($2, CURRENT_TIMESTAMP)) RETURNING id`,
			c.UserID, nullableTime(c.CreatedAt),
		)
		return c, row.Scan(&c.ID)
	}
	return c, nil
}

func (db *DB) FindCartItemByID(id int64) (*CartItem, error) {
	row := db.QueryRow(
		`SELECT id, cart_id, product_id, quantity, customization, flour_type, sweetener_percent, sweetener_type
		   FROM cart_items WHERE id=$1`, id)
	var ci CartItem
	if err := row.Scan(&ci.ID, &ci.CartID, &ci.ProductID, &ci.Quantity,
		&ci.Customization, &ci.FlourType, &ci.SweetenerPercent, &ci.SweetenerType); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &ci, nil
}

func (db *DB) FindCartItemsByUserID(userid int) ([]CartItem, error) {
	rows, err := db.Query(
		`SELECT ci.id, ci.cart_id, ci.product_id, ci.quantity, ci.customization,
		        ci.flour_type, ci.sweetener_percent, ci.sweetener_type
		   FROM cart_items ci JOIN "Cart" c ON c.id = ci.cart_id
		  WHERE COALESCE(c.user_id, c.userid)=$1
		  ORDER BY ci.id ASC`, userid)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []CartItem{}
	for rows.Next() {
		var ci CartItem
		if err := rows.Scan(&ci.ID, &ci.CartID, &ci.ProductID, &ci.Quantity,
			&ci.Customization, &ci.FlourType, &ci.SweetenerPercent, &ci.SweetenerType); err != nil {
			return nil, err
		}
		out = append(out, ci)
	}
	return out, rows.Err()
}

func (db *DB) SaveCartItem(ci *CartItem) (*CartItem, error) {
	if ci.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO cart_items(cart_id, product_id, quantity, customization,
			                        flour_type, sweetener_percent, sweetener_type)
			 VALUES($1,$2,$3,$4,$5,$6,$7) RETURNING id`,
			ci.CartID, ci.ProductID, ci.Quantity, ci.Customization,
			ci.FlourType, ci.SweetenerPercent, ci.SweetenerType,
		)
		return ci, row.Scan(&ci.ID)
	}
	_, err := db.Exec(
		`UPDATE cart_items SET cart_id=$1, product_id=$2, quantity=$3, customization=$4,
		   flour_type=$5, sweetener_percent=$6, sweetener_type=$7 WHERE id=$8`,
		ci.CartID, ci.ProductID, ci.Quantity, ci.Customization,
		ci.FlourType, ci.SweetenerPercent, ci.SweetenerType, ci.ID,
	)
	return ci, err
}

func (db *DB) DeleteCartItem(id int64) error {
	_, err := db.Exec(`DELETE FROM cart_items WHERE id=$1`, id)
	return err
}

func (db *DB) DeleteCartItemsByUserID(userid int) (int, error) {
	res, err := db.Exec(
		`DELETE FROM cart_items WHERE cart_id IN
		   (SELECT id FROM "Cart" WHERE COALESCE(user_id, userid)=$1)`, userid)
	if err != nil {
		return 0, err
	}
	n, _ := res.RowsAffected()
	return int(n), nil
}
