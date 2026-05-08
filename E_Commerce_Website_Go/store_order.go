package main

import (
	"database/sql"
	"errors"
	"strings"
	"time"

	"github.com/lib/pq"
)

func scanOrder(s scanner, o *Order) error {
	return s.Scan(&o.ID, &o.UserID, &o.TotalAmount, &o.Status, &o.CreatedAt, &o.Channel,
		&o.KitchenStatus, &o.CustomerNotes, &o.KitchenNotes, &o.ApprovalNotes, &o.ApprovalStatus,
		&o.ApprovedAt, &o.RequiresApproval, &o.ApprovedByUserID)
}

type scanner interface {
	Scan(dest ...interface{}) error
}

const orderCols = `id, user_id, total_amount, status, created_at, channel, kitchen_status,
		customer_notes, kitchen_notes, approval_notes, approval_status, approved_at,
		requires_approval, approved_by_user_id`

func (db *DB) FindOrderByID(id int64) (*Order, error) {
	row := db.QueryRow(`SELECT `+orderCols+` FROM "Orders" WHERE id=$1`, id)
	var o Order
	if err := scanOrder(row, &o); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &o, nil
}

func (db *DB) SaveOrder(o *Order) (*Order, error) {
	if o.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO "Orders"(user_id, total_amount, status, created_at, channel, kitchen_status,
			   customer_notes, kitchen_notes, approval_notes, approval_status, approved_at,
			   requires_approval, approved_by_user_id)
			 VALUES($1,$2,$3,COALESCE($4,CURRENT_TIMESTAMP),$5,$6,$7,$8,$9,$10,$11,$12,$13)
			 RETURNING id, created_at`,
			o.UserID, o.TotalAmount, o.Status, nullableTime(o.CreatedAt), o.Channel, o.KitchenStatus,
			o.CustomerNotes, o.KitchenNotes, o.ApprovalNotes, o.ApprovalStatus,
			nullableTime(o.ApprovedAt), o.RequiresApproval, o.ApprovedByUserID,
		)
		return o, row.Scan(&o.ID, &o.CreatedAt)
	}
	_, err := db.Exec(
		`UPDATE "Orders" SET user_id=$1, total_amount=$2, status=$3, channel=$4, kitchen_status=$5,
		   customer_notes=$6, kitchen_notes=$7, approval_notes=$8, approval_status=$9,
		   approved_at=$10, requires_approval=$11, approved_by_user_id=$12 WHERE id=$13`,
		o.UserID, o.TotalAmount, o.Status, o.Channel, o.KitchenStatus,
		o.CustomerNotes, o.KitchenNotes, o.ApprovalNotes, o.ApprovalStatus,
		nullableTime(o.ApprovedAt), o.RequiresApproval, o.ApprovedByUserID, o.ID,
	)
	return o, err
}

func (db *DB) FindOrdersByUserID(userid int) ([]Order, error) {
	rows, err := db.Query(`SELECT `+orderCols+` FROM "Orders" WHERE user_id=$1 ORDER BY id ASC`, userid)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Order{}
	for rows.Next() {
		var o Order
		if err := scanOrder(rows, &o); err != nil {
			return nil, err
		}
		out = append(out, o)
	}
	return out, rows.Err()
}

func (db *DB) FindOrdersInRange(from, to time.Time) ([]Order, error) {
	rows, err := db.Query(`SELECT `+orderCols+` FROM "Orders" WHERE created_at >= $1 AND created_at < $2
		ORDER BY created_at ASC`, from, to)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Order{}
	for rows.Next() {
		var o Order
		if err := scanOrder(rows, &o); err != nil {
			return nil, err
		}
		out = append(out, o)
	}
	return out, rows.Err()
}

func (db *DB) FindOrdersByChannelAndKitchenStatuses(channel string, statuses []string) ([]Order, error) {
	rows, err := db.Query(
		`SELECT `+orderCols+` FROM "Orders"
		  WHERE LOWER(channel)=LOWER($1)
		    AND LOWER(COALESCE(kitchen_status, 'pending')) = ANY($2)
		  ORDER BY created_at ASC`,
		channel, pq.Array(lowerAll(statuses)),
	)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Order{}
	for rows.Next() {
		var o Order
		if err := scanOrder(rows, &o); err != nil {
			return nil, err
		}
		out = append(out, o)
	}
	return out, rows.Err()
}

func lowerAll(in []string) []string {
	out := make([]string, len(in))
	for i, s := range in {
		out[i] = strings.ToLower(s)
	}
	return out
}

func (db *DB) FindOrdersInPipeline() ([]Order, error) {
	rows, err := db.Query(
		`SELECT ` + orderCols + ` FROM "Orders"
		  WHERE LOWER(COALESCE(kitchen_status,'pending')) IN
		        ('pending','preparing','ready','done','picked_up','out_for_delivery')
		  ORDER BY created_at ASC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Order{}
	for rows.Next() {
		var o Order
		if err := scanOrder(rows, &o); err != nil {
			return nil, err
		}
		out = append(out, o)
	}
	return out, rows.Err()
}

func (db *DB) FindOrdersPendingApproval() ([]Order, error) {
	rows, err := db.Query(
		`SELECT ` + orderCols + ` FROM "Orders"
		  WHERE requires_approval = TRUE
		    AND (approval_status IS NULL OR LOWER(approval_status)='pending')
		  ORDER BY created_at ASC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Order{}
	for rows.Next() {
		var o Order
		if err := scanOrder(rows, &o); err != nil {
			return nil, err
		}
		out = append(out, o)
	}
	return out, rows.Err()
}

func (db *DB) DeleteOrdersByUserID(userid int) (int, error) {
	res, err := db.Exec(`DELETE FROM "Orders" WHERE user_id=$1`, userid)
	if err != nil {
		return 0, err
	}
	n, _ := res.RowsAffected()
	return int(n), nil
}

// OrderItem ----------------------------------------------------------------

const orderItemCols = `id, order_id, product_id, quantity, price, customization,
		flour_type, sweetener_percent, sweetener_type`

func scanOrderItem(s scanner, oi *OrderItem) error {
	return s.Scan(&oi.ID, &oi.OrderID, &oi.ProductID, &oi.Quantity, &oi.Price,
		&oi.Customization, &oi.FlourType, &oi.SweetenerPercent, &oi.SweetenerType)
}

func (db *DB) SaveOrderItem(oi *OrderItem) (*OrderItem, error) {
	if oi.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO order_items(order_id, product_id, quantity, price, customization,
			   flour_type, sweetener_percent, sweetener_type)
			 VALUES($1,$2,$3,$4,$5,$6,$7,$8) RETURNING id`,
			oi.OrderID, oi.ProductID, oi.Quantity, oi.Price, oi.Customization,
			oi.FlourType, oi.SweetenerPercent, oi.SweetenerType,
		)
		return oi, row.Scan(&oi.ID)
	}
	_, err := db.Exec(
		`UPDATE order_items SET order_id=$1, product_id=$2, quantity=$3, price=$4,
		  customization=$5, flour_type=$6, sweetener_percent=$7, sweetener_type=$8
		  WHERE id=$9`,
		oi.OrderID, oi.ProductID, oi.Quantity, oi.Price, oi.Customization,
		oi.FlourType, oi.SweetenerPercent, oi.SweetenerType, oi.ID,
	)
	return oi, err
}

func (db *DB) FindOrderItemsByOrderIDs(ids []int64) ([]OrderItem, error) {
	if len(ids) == 0 {
		return []OrderItem{}, nil
	}
	rows, err := db.Query(`SELECT `+orderItemCols+` FROM order_items WHERE order_id = ANY($1) ORDER BY id ASC`,
		pq.Array(ids))
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []OrderItem{}
	for rows.Next() {
		var oi OrderItem
		if err := scanOrderItem(rows, &oi); err != nil {
			return nil, err
		}
		out = append(out, oi)
	}
	return out, rows.Err()
}

func (db *DB) FindOrderItemsByUserID(userid int) ([]OrderItem, error) {
	rows, err := db.Query(
		`SELECT `+orderItemCols+` FROM order_items
		  WHERE order_id IN (SELECT id FROM "Orders" WHERE user_id=$1)
		  ORDER BY id ASC`, userid)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []OrderItem{}
	for rows.Next() {
		var oi OrderItem
		if err := scanOrderItem(rows, &oi); err != nil {
			return nil, err
		}
		out = append(out, oi)
	}
	return out, rows.Err()
}

func (db *DB) DeleteOrderItemsByUserID(userid int) (int, error) {
	res, err := db.Exec(
		`DELETE FROM order_items WHERE order_id IN
		   (SELECT id FROM "Orders" WHERE user_id=$1)`, userid)
	if err != nil {
		return 0, err
	}
	n, _ := res.RowsAffected()
	return int(n), nil
}

// Payment ------------------------------------------------------------------

const paymentCols = `id, order_id, payment_method, status, amount, created_at`

func scanPayment(s scanner, p *Payment) error {
	return s.Scan(&p.ID, &p.OrderID, &p.PaymentMethod, &p.Status, &p.Amount, &p.CreatedAt)
}

func (db *DB) SavePayment(p *Payment) (*Payment, error) {
	if p.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO payments(order_id, payment_method, status, amount, created_at)
			 VALUES($1,$2,$3,$4,COALESCE($5,CURRENT_TIMESTAMP)) RETURNING id, created_at`,
			p.OrderID, p.PaymentMethod, p.Status, p.Amount, nullableTime(p.CreatedAt),
		)
		return p, row.Scan(&p.ID, &p.CreatedAt)
	}
	_, err := db.Exec(
		`UPDATE payments SET order_id=$1, payment_method=$2, status=$3, amount=$4 WHERE id=$5`,
		p.OrderID, p.PaymentMethod, p.Status, p.Amount, p.ID,
	)
	return p, err
}

func (db *DB) FindPaymentsByUserID(userid int) ([]Payment, error) {
	rows, err := db.Query(
		`SELECT `+paymentCols+` FROM payments
		  WHERE order_id IN (SELECT id FROM "Orders" WHERE user_id=$1)
		  ORDER BY id ASC`, userid)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Payment{}
	for rows.Next() {
		var p Payment
		if err := scanPayment(rows, &p); err != nil {
			return nil, err
		}
		out = append(out, p)
	}
	return out, rows.Err()
}

func (db *DB) FindPaymentsByOrderIDs(ids []int64) ([]Payment, error) {
	if len(ids) == 0 {
		return []Payment{}, nil
	}
	rows, err := db.Query(`SELECT `+paymentCols+` FROM payments WHERE order_id = ANY($1) ORDER BY id ASC`,
		pq.Array(ids))
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Payment{}
	for rows.Next() {
		var p Payment
		if err := scanPayment(rows, &p); err != nil {
			return nil, err
		}
		out = append(out, p)
	}
	return out, rows.Err()
}

func (db *DB) DeletePaymentsByUserID(userid int) (int, error) {
	res, err := db.Exec(
		`DELETE FROM payments WHERE order_id IN
		   (SELECT id FROM "Orders" WHERE user_id=$1)`, userid)
	if err != nil {
		return 0, err
	}
	n, _ := res.RowsAffected()
	return int(n), nil
}
