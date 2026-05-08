package main

import (
	"database/sql"
	"errors"
	"time"
)

// Tasks --------------------------------------------------------------------

const taskCols = `id, assigned_department, completed_at, created_at, description, due_date,
	priority, related_order_id, resolution_notes, status, title, updated_at,
	assigned_user_id, completed_by_user_id, created_by_user_id`

func scanTask(s scanner, t *Task) error {
	return s.Scan(&t.ID, &t.AssignedDepartment, &t.CompletedAt, &t.CreatedAt, &t.Description,
		&t.DueDate, &t.Priority, &t.RelatedOrderID, &t.ResolutionNotes, &t.Status,
		&t.Title, &t.UpdatedAt, &t.AssignedUserID, &t.CompletedByUserID, &t.CreatedByUserID)
}

func (db *DB) SaveTask(t *Task) (*Task, error) {
	if t.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO tasks(assigned_department, completed_at, created_at, description, due_date,
			   priority, related_order_id, resolution_notes, status, title, updated_at,
			   assigned_user_id, completed_by_user_id, created_by_user_id)
			 VALUES($1,$2,COALESCE($3,CURRENT_TIMESTAMP),$4,$5,$6,$7,$8,$9,$10,
			        COALESCE($11,CURRENT_TIMESTAMP),$12,$13,$14)
			 RETURNING id, created_at, updated_at`,
			t.AssignedDepartment, nullableTime(t.CompletedAt), nullableTime(t.CreatedAt),
			t.Description, t.DueDate, t.Priority, t.RelatedOrderID, t.ResolutionNotes,
			t.Status, t.Title, nullableTime(t.UpdatedAt),
			t.AssignedUserID, t.CompletedByUserID, t.CreatedByUserID,
		)
		return t, row.Scan(&t.ID, &t.CreatedAt, &t.UpdatedAt)
	}
	_, err := db.Exec(
		`UPDATE tasks SET assigned_department=$1, completed_at=$2, description=$3, due_date=$4,
		   priority=$5, related_order_id=$6, resolution_notes=$7, status=$8, title=$9,
		   updated_at=COALESCE($10, CURRENT_TIMESTAMP), assigned_user_id=$11, completed_by_user_id=$12,
		   created_by_user_id=$13 WHERE id=$14`,
		t.AssignedDepartment, nullableTime(t.CompletedAt), t.Description, t.DueDate,
		t.Priority, t.RelatedOrderID, t.ResolutionNotes, t.Status, t.Title,
		nullableTime(t.UpdatedAt), t.AssignedUserID, t.CompletedByUserID, t.CreatedByUserID, t.ID,
	)
	return t, err
}

func (db *DB) FindTaskByID(id int64) (*Task, error) {
	row := db.QueryRow(`SELECT `+taskCols+` FROM tasks WHERE id=$1`, id)
	var t Task
	if err := scanTask(row, &t); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &t, nil
}

func (db *DB) FindAllTasks() ([]Task, error) {
	return db.tasksQuery(`SELECT `+taskCols+` FROM tasks ORDER BY created_at DESC`)
}

func (db *DB) FindTasksByDepartment(dept string) ([]Task, error) {
	return db.tasksQuery(`SELECT `+taskCols+` FROM tasks WHERE LOWER(assigned_department)=LOWER($1) ORDER BY created_at DESC`, dept)
}

func (db *DB) FindTasksByCreatedBy(userid int) ([]Task, error) {
	return db.tasksQuery(`SELECT `+taskCols+` FROM tasks WHERE created_by_user_id=$1 ORDER BY created_at DESC`, userid)
}

func (db *DB) FindTasksCompletedInRange(from, to time.Time) ([]Task, error) {
	return db.tasksQuery(
		`SELECT `+taskCols+` FROM tasks
		   WHERE LOWER(status)='done' AND completed_at >= $1 AND completed_at < $2`,
		from, to)
}

func (db *DB) FindTasksCreatedInRange(from, to time.Time) ([]Task, error) {
	return db.tasksQuery(
		`SELECT `+taskCols+` FROM tasks WHERE created_at >= $1 AND created_at < $2`,
		from, to)
}

func (db *DB) tasksQuery(q string, args ...interface{}) ([]Task, error) {
	rows, err := db.Query(q, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []Task{}
	for rows.Next() {
		var t Task
		if err := scanTask(rows, &t); err != nil {
			return nil, err
		}
		out = append(out, t)
	}
	return out, rows.Err()
}

// Discount campaigns --------------------------------------------------------

const campaignCols = `id, category_filter, created_at, decided_at, decision_notes,
	discount_percent, ends_on, name, starts_on, status, decided_by_user_id, proposed_by_user_id`

func scanCampaign(s scanner, d *DiscountCampaign) error {
	return s.Scan(&d.ID, &d.CategoryFilter, &d.CreatedAt, &d.DecidedAt, &d.DecisionNotes,
		&d.DiscountPercent, &d.EndsOn, &d.Name, &d.StartsOn, &d.Status,
		&d.DecidedByUserID, &d.ProposedByUserID)
}

func (db *DB) SaveCampaign(d *DiscountCampaign) (*DiscountCampaign, error) {
	if d.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO discount_campaigns(category_filter, created_at, decided_at, decision_notes,
			   discount_percent, ends_on, name, starts_on, status, decided_by_user_id, proposed_by_user_id)
			 VALUES($1,COALESCE($2,CURRENT_TIMESTAMP),$3,$4,$5,$6,$7,$8,$9,$10,$11)
			 RETURNING id, created_at`,
			d.CategoryFilter, nullableTime(d.CreatedAt), nullableTime(d.DecidedAt),
			d.DecisionNotes, d.DiscountPercent, d.EndsOn, d.Name, d.StartsOn, d.Status,
			d.DecidedByUserID, d.ProposedByUserID,
		)
		return d, row.Scan(&d.ID, &d.CreatedAt)
	}
	_, err := db.Exec(
		`UPDATE discount_campaigns SET category_filter=$1, decided_at=$2, decision_notes=$3,
		   discount_percent=$4, ends_on=$5, name=$6, starts_on=$7, status=$8,
		   decided_by_user_id=$9, proposed_by_user_id=$10 WHERE id=$11`,
		d.CategoryFilter, nullableTime(d.DecidedAt), d.DecisionNotes,
		d.DiscountPercent, d.EndsOn, d.Name, d.StartsOn, d.Status,
		d.DecidedByUserID, d.ProposedByUserID, d.ID,
	)
	return d, err
}

func (db *DB) FindCampaignByID(id int64) (*DiscountCampaign, error) {
	row := db.QueryRow(`SELECT `+campaignCols+` FROM discount_campaigns WHERE id=$1`, id)
	var d DiscountCampaign
	if err := scanCampaign(row, &d); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &d, nil
}

func (db *DB) FindAllCampaigns() ([]DiscountCampaign, error) {
	return db.campaignQuery(`SELECT `+campaignCols+` FROM discount_campaigns ORDER BY created_at DESC`)
}

func (db *DB) FindCampaignsByStatus(status string) ([]DiscountCampaign, error) {
	return db.campaignQuery(
		`SELECT `+campaignCols+` FROM discount_campaigns WHERE LOWER(status)=LOWER($1) ORDER BY created_at DESC`,
		status)
}

func (db *DB) campaignQuery(q string, args ...interface{}) ([]DiscountCampaign, error) {
	rows, err := db.Query(q, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []DiscountCampaign{}
	for rows.Next() {
		var d DiscountCampaign
		if err := scanCampaign(rows, &d); err != nil {
			return nil, err
		}
		out = append(out, d)
	}
	return out, rows.Err()
}

// Refund requests ----------------------------------------------------------

const refundCols = `id, amount, created_at, decided_at, decision_notes, reason, request_type,
	status, decided_by_user_id, order_id, raised_by_user_id`

func scanRefund(s scanner, r *RefundRequest) error {
	return s.Scan(&r.ID, &r.Amount, &r.CreatedAt, &r.DecidedAt, &r.DecisionNotes, &r.Reason,
		&r.RequestType, &r.Status, &r.DecidedByUserID, &r.OrderID, &r.RaisedByUserID)
}

func (db *DB) SaveRefund(r *RefundRequest) (*RefundRequest, error) {
	if r.ID == 0 {
		row := db.QueryRow(
			`INSERT INTO refund_requests(amount, created_at, decided_at, decision_notes, reason,
			   request_type, status, decided_by_user_id, order_id, raised_by_user_id)
			 VALUES($1,COALESCE($2,CURRENT_TIMESTAMP),$3,$4,$5,$6,$7,$8,$9,$10) RETURNING id, created_at`,
			r.Amount, nullableTime(r.CreatedAt), nullableTime(r.DecidedAt), r.DecisionNotes,
			r.Reason, r.RequestType, r.Status, r.DecidedByUserID, r.OrderID, r.RaisedByUserID,
		)
		return r, row.Scan(&r.ID, &r.CreatedAt)
	}
	_, err := db.Exec(
		`UPDATE refund_requests SET amount=$1, decided_at=$2, decision_notes=$3, reason=$4,
		   request_type=$5, status=$6, decided_by_user_id=$7, order_id=$8, raised_by_user_id=$9
		   WHERE id=$10`,
		r.Amount, nullableTime(r.DecidedAt), r.DecisionNotes, r.Reason,
		r.RequestType, r.Status, r.DecidedByUserID, r.OrderID, r.RaisedByUserID, r.ID,
	)
	return r, err
}

func (db *DB) FindRefundByID(id int64) (*RefundRequest, error) {
	row := db.QueryRow(`SELECT `+refundCols+` FROM refund_requests WHERE id=$1`, id)
	var r RefundRequest
	if err := scanRefund(row, &r); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil
		}
		return nil, err
	}
	return &r, nil
}

func (db *DB) FindAllRefunds() ([]RefundRequest, error) {
	return db.refundQuery(`SELECT ` + refundCols + ` FROM refund_requests ORDER BY created_at DESC`)
}

func (db *DB) FindRefundsByStatus(status string) ([]RefundRequest, error) {
	return db.refundQuery(
		`SELECT `+refundCols+` FROM refund_requests WHERE LOWER(status)=LOWER($1) ORDER BY created_at DESC`,
		status)
}

func (db *DB) refundQuery(q string, args ...interface{}) ([]RefundRequest, error) {
	rows, err := db.Query(q, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	out := []RefundRequest{}
	for rows.Next() {
		var r RefundRequest
		if err := scanRefund(rows, &r); err != nil {
			return nil, err
		}
		out = append(out, r)
	}
	return out, rows.Err()
}
