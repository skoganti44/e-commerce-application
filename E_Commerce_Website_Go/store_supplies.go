package main

import (
	"database/sql"
	"errors"
	"strings"
)

const supplyCols = `id, category, current_stock, name, notes, threshold, unit, updated_at,
		order_status, requested_at, in_stock, requested_qty, requested_by_team`

func scanSupply(s scanner, x *Supply) error {
	return s.Scan(&x.ID, &x.Category, &x.CurrentStock, &x.Name, &x.Notes, &x.Threshold, &x.Unit,
		&x.UpdatedAt, &x.OrderStatus, &x.RequestedAt, &x.InStock, &x.RequestedQty, &x.RequestedByTeam)
}

func (db *DB) FindAllSupplies() ([]Supply, error) {
	rows, err := db.Query(`SELECT ` + supplyCols + ` FROM supplies ORDER BY name ASC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Supply{}
	for rows.Next() {
		var s Supply
		if err := scanSupply(rows, &s); err != nil {
			return nil, err
		}
		out = append(out, s)
	}
	return out, rows.Err()
}

func (db *DB) FindSupplyByID(id int64) (*Supply, error) {
	row := db.QueryRow(`SELECT `+supplyCols+` FROM supplies WHERE id=$1`, id)
	var s Supply
	if err := scanSupply(row, &s); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &s, nil
}

func (db *DB) FindSupplyByName(name string) (*Supply, error) {
	if strings.TrimSpace(name) == "" {
		return nil, nil
	}
	row := db.QueryRow(`SELECT `+supplyCols+` FROM supplies WHERE LOWER(name)=LOWER($1) LIMIT 1`,
		strings.TrimSpace(name))
	var s Supply
	if err := scanSupply(row, &s); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &s, nil
}

func (db *DB) CountSupplies() (int64, error) {
	row := db.QueryRow(`SELECT COUNT(*) FROM supplies`)
	var n int64
	return n, row.Scan(&n)
}

func (db *DB) SaveSupply(s *Supply) (*Supply, error) {
	if s.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO supplies(category, current_stock, name, notes, threshold, unit, updated_at,
			   order_status, requested_at, in_stock, requested_qty, requested_by_team)
			 VALUES($1,$2,$3,$4,$5,$6,COALESCE($7, CURRENT_TIMESTAMP),$8,$9,$10,$11,$12) RETURNING id, updated_at`,
			s.Category, s.CurrentStock, s.Name, s.Notes, s.Threshold, s.Unit,
			nullableTime(s.UpdatedAt), s.OrderStatus, nullableTime(s.RequestedAt),
			s.InStock, s.RequestedQty, s.RequestedByTeam,
		)
		return s, row.Scan(&s.ID, &s.UpdatedAt)
	}
	_, err := db.Exec(
		`UPDATE supplies SET category=$1, current_stock=$2, name=$3, notes=$4, threshold=$5,
		   unit=$6, updated_at=CURRENT_TIMESTAMP, order_status=$7, requested_at=$8,
		   in_stock=$9, requested_qty=$10, requested_by_team=$11 WHERE id=$12`,
		s.Category, s.CurrentStock, s.Name, s.Notes, s.Threshold, s.Unit,
		s.OrderStatus, nullableTime(s.RequestedAt), s.InStock, s.RequestedQty,
		s.RequestedByTeam, s.ID,
	)
	if err == nil {
		// reload updated_at
		_ = db.QueryRow(`SELECT updated_at FROM supplies WHERE id=$1`, s.ID).Scan(&s.UpdatedAt)
	}
	return s, err
}

// Daily stock --------------------------------------------------------------

func (db *DB) FindDailyStockByDate(d string) ([]DailyStock, error) {
	rows, err := db.Query(
		`SELECT id, prepared_count, stock_date, target_count, product_id
		   FROM daily_stock WHERE stock_date=$1 ORDER BY id ASC`, d)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []DailyStock{}
	for rows.Next() {
		var ds DailyStock
		if err := rows.Scan(&ds.ID, &ds.PreparedCount, &ds.StockDate, &ds.TargetCount, &ds.ProductID); err != nil {
			return nil, err
		}
		out = append(out, ds)
	}
	return out, rows.Err()
}

func (db *DB) FindDailyStockByID(id int64) (*DailyStock, error) {
	row := db.QueryRow(`SELECT id, prepared_count, stock_date, target_count, product_id FROM daily_stock WHERE id=$1`, id)
	var ds DailyStock
	if err := row.Scan(&ds.ID, &ds.PreparedCount, &ds.StockDate, &ds.TargetCount, &ds.ProductID); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &ds, nil
}

func (db *DB) SaveDailyStock(ds *DailyStock) (*DailyStock, error) {
	if ds.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO daily_stock(prepared_count, stock_date, target_count, product_id)
			 VALUES($1,$2,$3,$4) RETURNING id`,
			ds.PreparedCount, ds.StockDate, ds.TargetCount, ds.ProductID,
		)
		return ds, row.Scan(&ds.ID)
	}
	_, err := db.Exec(
		`UPDATE daily_stock SET prepared_count=$1, stock_date=$2, target_count=$3, product_id=$4 WHERE id=$5`,
		ds.PreparedCount, ds.StockDate, ds.TargetCount, ds.ProductID, ds.ID,
	)
	return ds, err
}
