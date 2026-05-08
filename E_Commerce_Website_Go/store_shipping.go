package main

import (
	"database/sql"
	"errors"
)

const shippingCols = `id, address_type, city, country, created_at, full_name, instructions,
		landmark, line1, line2, phone, pincode, state, order_id, user_id`

func scanShipping(s scanner, a *ShippingAddress) error {
	return s.Scan(&a.ID, &a.AddressType, &a.City, &a.Country, &a.CreatedAt, &a.FullName,
		&a.Instructions, &a.Landmark, &a.Line1, &a.Line2, &a.Phone, &a.Pincode,
		&a.State, &a.OrderID, &a.UserID)
}

func (db *DB) SaveShippingAddress(a *ShippingAddress) (*ShippingAddress, error) {
	if a.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO shipping_addresses(address_type, city, country, created_at, full_name, instructions,
			   landmark, line1, line2, phone, pincode, state, order_id, user_id)
			 VALUES($1,$2,$3,COALESCE($4, CURRENT_TIMESTAMP),$5,$6,$7,$8,$9,$10,$11,$12,$13,$14)
			 RETURNING id, created_at`,
			a.AddressType, a.City, a.Country, nullableTime(a.CreatedAt), a.FullName, a.Instructions,
			a.Landmark, a.Line1, a.Line2, a.Phone, a.Pincode, a.State, a.OrderID, a.UserID,
		)
		return a, row.Scan(&a.ID, &a.CreatedAt)
	}
	_, err := db.Exec(
		`UPDATE shipping_addresses SET address_type=$1, city=$2, country=$3, full_name=$4, instructions=$5,
		   landmark=$6, line1=$7, line2=$8, phone=$9, pincode=$10, state=$11, order_id=$12, user_id=$13
		   WHERE id=$14`,
		a.AddressType, a.City, a.Country, a.FullName, a.Instructions,
		a.Landmark, a.Line1, a.Line2, a.Phone, a.Pincode, a.State, a.OrderID, a.UserID, a.ID,
	)
	return a, err
}

func (db *DB) FindLatestShippingAddressByUserID(userid int) (*ShippingAddress, error) {
	row := db.QueryRow(
		`SELECT `+shippingCols+` FROM shipping_addresses WHERE user_id=$1 ORDER BY id DESC LIMIT 1`, userid)
	var a ShippingAddress
	if err := scanShipping(row, &a); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &a, nil
}

func (db *DB) FindShippingAddressByOrderID(orderID int64) (*ShippingAddress, error) {
	row := db.QueryRow(
		`SELECT `+shippingCols+` FROM shipping_addresses WHERE order_id=$1 ORDER BY id DESC LIMIT 1`, orderID)
	var a ShippingAddress
	if err := scanShipping(row, &a); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &a, nil
}
