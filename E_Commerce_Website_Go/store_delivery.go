package main

import (
	"database/sql"
	"errors"
	"time"
)

const tripCols = `id, cod_amount, cod_collected_at, created_at, delivered_at, distance_km,
	failed_at, failure_reason, notes, otp_code, out_at, photo_proof_url, picked_up_at,
	status, tip_amount, updated_at, driver_user_id, order_id`

func scanTrip(s scanner, t *DeliveryTrip) error {
	return s.Scan(&t.ID, &t.CodAmount, &t.CodCollectedAt, &t.CreatedAt, &t.DeliveredAt,
		&t.DistanceKm, &t.FailedAt, &t.FailureReason, &t.Notes, &t.OtpCode, &t.OutAt,
		&t.PhotoProofURL, &t.PickedUpAt, &t.Status, &t.TipAmount, &t.UpdatedAt,
		&t.DriverUserID, &t.OrderID)
}

func (db *DB) SaveTrip(t *DeliveryTrip) (*DeliveryTrip, error) {
	if t.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO delivery_trips(cod_amount, cod_collected_at, created_at, delivered_at, distance_km,
			   failed_at, failure_reason, notes, otp_code, out_at, photo_proof_url, picked_up_at,
			   status, tip_amount, updated_at, driver_user_id, order_id)
			 VALUES($1,$2,COALESCE($3,CURRENT_TIMESTAMP),$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,
			        COALESCE($15,CURRENT_TIMESTAMP),$16,$17)
			 RETURNING id, created_at, updated_at`,
			t.CodAmount, nullableTime(t.CodCollectedAt), nullableTime(t.CreatedAt),
			nullableTime(t.DeliveredAt), t.DistanceKm,
			nullableTime(t.FailedAt), t.FailureReason, t.Notes, t.OtpCode, nullableTime(t.OutAt),
			t.PhotoProofURL, nullableTime(t.PickedUpAt), t.Status, t.TipAmount,
			nullableTime(t.UpdatedAt), t.DriverUserID, t.OrderID,
		)
		return t, row.Scan(&t.ID, &t.CreatedAt, &t.UpdatedAt)
	}
	_, err := db.Exec(
		`UPDATE delivery_trips SET cod_amount=$1, cod_collected_at=$2, delivered_at=$3,
		   distance_km=$4, failed_at=$5, failure_reason=$6, notes=$7, otp_code=$8,
		   out_at=$9, photo_proof_url=$10, picked_up_at=$11, status=$12, tip_amount=$13,
		   updated_at=COALESCE($14, CURRENT_TIMESTAMP), driver_user_id=$15, order_id=$16
		   WHERE id=$17`,
		t.CodAmount, nullableTime(t.CodCollectedAt), nullableTime(t.DeliveredAt),
		t.DistanceKm, nullableTime(t.FailedAt), t.FailureReason, t.Notes, t.OtpCode,
		nullableTime(t.OutAt), t.PhotoProofURL, nullableTime(t.PickedUpAt), t.Status, t.TipAmount,
		nullableTime(t.UpdatedAt), t.DriverUserID, t.OrderID, t.ID,
	)
	return t, err
}

func (db *DB) FindTripByID(id int64) (*DeliveryTrip, error) {
	row := db.QueryRow(`SELECT `+tripCols+` FROM delivery_trips WHERE id=$1`, id)
	var t DeliveryTrip
	if err := scanTrip(row, &t); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &t, nil
}

func (db *DB) FindTripByOrderID(orderID int64) (*DeliveryTrip, error) {
	row := db.QueryRow(`SELECT `+tripCols+` FROM delivery_trips WHERE order_id=$1 ORDER BY id DESC LIMIT 1`, orderID)
	var t DeliveryTrip
	if err := scanTrip(row, &t); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &t, nil
}

func (db *DB) FindTripsByDriver(driverID int) ([]DeliveryTrip, error) {
	rows, err := db.Query(`SELECT `+tripCols+` FROM delivery_trips WHERE driver_user_id=$1 ORDER BY created_at DESC`, driverID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []DeliveryTrip{}
	for rows.Next() {
		var t DeliveryTrip
		if err := scanTrip(rows, &t); err != nil {
			return nil, err
		}
		out = append(out, t)
	}
	return out, rows.Err()
}

func (db *DB) FindActiveTripsByDriver(driverID int) ([]DeliveryTrip, error) {
	rows, err := db.Query(
		`SELECT `+tripCols+` FROM delivery_trips
		   WHERE driver_user_id=$1 AND LOWER(status) IN ('picked_up','out_for_delivery')
		   ORDER BY created_at ASC`, driverID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []DeliveryTrip{}
	for rows.Next() {
		var t DeliveryTrip
		if err := scanTrip(rows, &t); err != nil {
			return nil, err
		}
		out = append(out, t)
	}
	return out, rows.Err()
}

func (db *DB) FindTripsByDriverInRange(driverID int, from, to time.Time) ([]DeliveryTrip, error) {
	rows, err := db.Query(
		`SELECT `+tripCols+` FROM delivery_trips
		   WHERE driver_user_id=$1 AND created_at >= $2 AND created_at < $3
		   ORDER BY created_at ASC`, driverID, from, to)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []DeliveryTrip{}
	for rows.Next() {
		var t DeliveryTrip
		if err := scanTrip(rows, &t); err != nil {
			return nil, err
		}
		out = append(out, t)
	}
	return out, rows.Err()
}

func (db *DB) FindTripsInRange(from, to time.Time) ([]DeliveryTrip, error) {
	rows, err := db.Query(
		`SELECT `+tripCols+` FROM delivery_trips WHERE created_at >= $1 AND created_at < $2 ORDER BY created_at DESC`,
		from, to)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []DeliveryTrip{}
	for rows.Next() {
		var t DeliveryTrip
		if err := scanTrip(rows, &t); err != nil {
			return nil, err
		}
		out = append(out, t)
	}
	return out, rows.Err()
}

func (db *DB) FindAllTrips() ([]DeliveryTrip, error) {
	rows, err := db.Query(`SELECT ` + tripCols + ` FROM delivery_trips ORDER BY created_at DESC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []DeliveryTrip{}
	for rows.Next() {
		var t DeliveryTrip
		if err := scanTrip(rows, &t); err != nil {
			return nil, err
		}
		out = append(out, t)
	}
	return out, rows.Err()
}

// Issues -------------------------------------------------------------------

const issueCols = `id, description, issue_type, reported_at, resolved_at, driver_user_id, trip_id`

func scanIssue(s scanner, i *DeliveryIssue) error {
	return s.Scan(&i.ID, &i.Description, &i.IssueType, &i.ReportedAt, &i.ResolvedAt,
		&i.DriverUserID, &i.TripID)
}

func (db *DB) SaveIssue(i *DeliveryIssue) (*DeliveryIssue, error) {
	if i.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO delivery_issues(description, issue_type, reported_at, resolved_at,
			   driver_user_id, trip_id)
			 VALUES($1,$2,COALESCE($3,CURRENT_TIMESTAMP),$4,$5,$6) RETURNING id, reported_at`,
			i.Description, i.IssueType, nullableTime(i.ReportedAt), nullableTime(i.ResolvedAt),
			i.DriverUserID, i.TripID,
		)
		return i, row.Scan(&i.ID, &i.ReportedAt)
	}
	_, err := db.Exec(
		`UPDATE delivery_issues SET description=$1, issue_type=$2, reported_at=$3, resolved_at=$4,
		  driver_user_id=$5, trip_id=$6 WHERE id=$7`,
		i.Description, i.IssueType, nullableTime(i.ReportedAt), nullableTime(i.ResolvedAt),
		i.DriverUserID, i.TripID, i.ID,
	)
	return i, err
}

func (db *DB) FindIssuesByDriver(driverID int) ([]DeliveryIssue, error) {
	rows, err := db.Query(`SELECT `+issueCols+` FROM delivery_issues WHERE driver_user_id=$1 ORDER BY reported_at DESC`, driverID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []DeliveryIssue{}
	for rows.Next() {
		var i DeliveryIssue
		if err := scanIssue(rows, &i); err != nil {
			return nil, err
		}
		out = append(out, i)
	}
	return out, rows.Err()
}
