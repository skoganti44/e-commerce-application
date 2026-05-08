package main

import (
	"net/http"
	"testing"
)

// ---------- GET /sales/analytics ----------

func TestSalesAnalytics_DefaultRange_OK(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	w := do(t, http.MethodGet, "/sales/analytics", nil, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	for _, key := range []string{"totalRevenue", "orderCount", "avgOrderValue", "dailyTrend", "topProducts"} {
		if _, ok := resp[key]; !ok {
			t.Fatalf("expected key %q, got %v", key, resp)
		}
	}
}

func TestSalesAnalytics_CustomRange_OK(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	w := do(t, http.MethodGet, "/sales/analytics?from=2025-01-01&to=2026-12-31", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestSalesAnalytics_FromAfterTo_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	w := do(t, http.MethodGet, "/sales/analytics?from=2030-12-31&to=2030-01-01", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "must be on or before")
}

func TestSalesAnalytics_BadDate_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	w := do(t, http.MethodGet, "/sales/analytics?from=oof", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestSalesAnalytics_Unauthorized(t *testing.T) {
	w := do(t, http.MethodGet, "/sales/analytics", nil, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}

// ---------- GET /management/ops ----------

func TestManagementOps_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/ops", nil, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	for _, key := range []string{"kitchenQueue", "deliveryInFlight", "breaches", "kitchenSlaMinutes", "deliverySlaMinutes"} {
		if _, ok := resp[key]; !ok {
			t.Fatalf("expected key %q in management ops, got %v", key, resp)
		}
	}
}

func TestManagementOps_Unauthorized(t *testing.T) {
	w := do(t, http.MethodGet, "/management/ops", nil, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}

// ---------- GET /management/orders-audit ----------

func TestOrdersAudit_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/orders-audit", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestOrdersAudit_ChannelFilter_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/orders-audit?channel=online", nil, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	if rows, ok := resp["orders"].([]any); ok {
		for _, r := range rows {
			row := r.(map[string]any)
			if row["channel"] != "online" {
				t.Fatalf("expected only online, got %v", row)
			}
		}
	}
}

func TestOrdersAudit_PaymentMethodFilter_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/orders-audit?paymentMethod=cash", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestOrdersAudit_BadDate_BadRequest(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/orders-audit?from=garbage", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- GET /management/deliveries-audit ----------

func TestDeliveriesAudit_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/deliveries-audit", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestDeliveriesAudit_StatusFilter_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/deliveries-audit?status=delivered", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestDeliveriesAudit_BadDate_BadRequest(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/deliveries-audit?from=oof", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- GET /management/day-pnl ----------

func TestDayPnl_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/day-pnl", nil, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	for _, key := range []string{"date", "orderCount", "totalRevenue", "grossInflow", "net"} {
		if _, ok := resp[key]; !ok {
			t.Fatalf("expected key %q, got %v", key, resp)
		}
	}
}

func TestDayPnl_BadDate_BadRequest(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/day-pnl?date=oof", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- GET /management/staff-performance ----------

func TestStaffPerformance_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/staff-performance", nil, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	for _, key := range []string{"drivers", "staffByDepartment", "salesActivity"} {
		if _, ok := resp[key]; !ok {
			t.Fatalf("expected key %q, got %v", key, resp)
		}
	}
}

func TestStaffPerformance_FromAfterTo_BadRequest(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/staff-performance?from=2030-12-31&to=2030-01-01", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- GET /management/cash-reconciliation ----------

func TestCashReconciliation_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/cash-reconciliation", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestCashReconciliation_WithCounted_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet,
		"/management/cash-reconciliation?openingFloat=100&countedCash=500", nil, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	if _, ok := resp["variance"]; !ok {
		t.Fatalf("expected variance key, got %v", resp)
	}
}

func TestCashReconciliation_NegativeOpening_BadRequest(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet,
		"/management/cash-reconciliation?openingFloat=-1", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "openingfloat")
}

func TestCashReconciliation_BadDate_BadRequest(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet,
		"/management/cash-reconciliation?date=oof", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}
