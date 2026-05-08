package main

import (
	"fmt"
	"net/http"
	"testing"
	"time"
)

// ---------- GET /kitchen/online-orders ----------

func TestKitchenOnlineOrders_OK(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodGet, "/kitchen/online-orders", nil, headers)
	assertStatus(t, w, http.StatusOK)
	var rows []map[string]any
	mustDecode(t, w, &rows)
	for _, r := range rows {
		if r["channel"] != "online" {
			t.Fatalf("expected only online orders, got %v", r)
		}
	}
}

func TestKitchenOnlineOrders_Unauthorized(t *testing.T) {
	w := do(t, http.MethodGet, "/kitchen/online-orders", nil, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}

// ---------- GET /kitchen/instore-orders ----------

func TestKitchenInStoreOrders_OK(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodGet, "/kitchen/instore-orders", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

// ---------- GET /delivery/online-orders, /bakery/instore-orders ----------

func TestDeliveryOnlineOrdersFeed_OK(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodGet, "/delivery/online-orders", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestBakeryInStoreOrdersFeed_OK(t *testing.T) {
	headers := auth(t, bakeryEmail, bakeryPass)
	w := do(t, http.MethodGet, "/bakery/instore-orders", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

// ---------- POST /kitchen/order/{orderId}/status ----------

// makeKitchenOrder produces a fresh online order in 'pending' state for the kitchen tests.
func makeKitchenOrder(t *testing.T) int64 {
	t.Helper()
	headers := auth(t, customerEmail, customerPass)
	addCartItem(t, headers, customerUserID, 1, 1)
	body := map[string]any{
		"userid":        customerUserID,
		"paymentMethod": "DEBIT_CARD",
		"cardLast4":     "9999",
		"address":       validAddress(),
	}
	w := do(t, http.MethodPost, "/checkout", body, headers)
	if w.Code != http.StatusOK {
		t.Fatalf("checkout to seed kitchen order: %s", w.Body.String())
	}
	var resp struct {
		OrderID int64 `json:"orderId"`
	}
	mustDecode(t, w, &resp)
	return resp.OrderID
}

func TestUpdateKitchenStatus_OK(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/kitchen/order/%d/status", orderID),
		map[string]any{"kitchenStatus": "baking"}, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestUpdateKitchenStatus_InvalidStatus_BadRequest(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/kitchen/order/%d/status", orderID),
		map[string]any{"kitchenStatus": "warpdrive"}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "invalid kitchen status")
}

func TestUpdateKitchenStatus_UnknownOrder_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodPost,
		"/kitchen/order/99999999/status",
		map[string]any{"kitchenStatus": "baking"}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "order not found")
}

// ---------- GET /kitchen/daily-stock ----------

func TestDailyStock_OK(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodGet, "/kitchen/daily-stock", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

// ---------- POST /kitchen/daily-stock/{stockId}/adjust ----------

func TestAdjustDailyStock_UnknownStock_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodPost, "/kitchen/daily-stock/99999/adjust",
		map[string]any{"delta": 1}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "daily stock row not found")
}

// ---------- GET /kitchen/supplies, /kitchen/in-stock ----------

func TestSupplies_OK(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodGet, "/kitchen/supplies", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestInStockSupplies_OK(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodGet, "/kitchen/in-stock", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

// ---------- POST /kitchen/supplies/seed ----------

func TestSeedSupplies_OK(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodPost, "/kitchen/supplies/seed", nil, headers)
	assertStatus(t, w, http.StatusOK)
	// Already-seeded → seeded:0 / existing:>0
}

// ---------- POST /kitchen/supplies (create/update) ----------

func TestSaveSupply_New_OK(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	body := map[string]any{
		"name":      fmt.Sprintf("Test Supply %d", randInt()),
		"unit":      "kg",
		"category":  "flour",
		"inStock":   2,
		"threshold": 1,
	}
	w := do(t, http.MethodPost, "/kitchen/supplies", body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestSaveSupply_InvalidUnit_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	body := map[string]any{
		"name": "Bad Unit Supply", "unit": "blarg", "category": "flour",
	}
	w := do(t, http.MethodPost, "/kitchen/supplies", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "invalid unit")
}

func TestSaveSupply_InvalidCategory_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	body := map[string]any{
		"name": "Bad Category Supply", "unit": "kg", "category": "alien",
	}
	w := do(t, http.MethodPost, "/kitchen/supplies", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "invalid category")
}

func TestSaveSupply_MissingName_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	body := map[string]any{"unit": "kg", "category": "flour"}
	w := do(t, http.MethodPost, "/kitchen/supplies", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "name is required")
}

// ---------- POST /kitchen/supplies/{supplyId}/adjust ----------

func TestAdjustSupplyStock_OK(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	id := firstSupplyID(t, headers)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/kitchen/supplies/%d/adjust", id),
		map[string]any{"delta": 1, "note": "test bump"}, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestAdjustSupplyStock_MissingDelta_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	id := firstSupplyID(t, headers)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/kitchen/supplies/%d/adjust", id),
		map[string]any{}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "delta is required")
}

func TestAdjustSupplyStock_UnknownSupply_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodPost, "/kitchen/supplies/99999999/adjust",
		map[string]any{"delta": 1}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "supply not found")
}

// ---------- POST /kitchen/supplies/{supplyId}/request ----------

func TestRequestMoreSupply_OK(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	id := firstSupplyID(t, headers)
	body := map[string]any{"requestedQty": 2.5, "urgency": "waiting"}
	w := do(t, http.MethodPost, fmt.Sprintf("/kitchen/supplies/%d/request", id), body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestRequestMoreSupply_MissingQty_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	id := firstSupplyID(t, headers)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/kitchen/supplies/%d/request", id),
		map[string]any{}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "requestedqty")
}

// ---------- POST /kitchen/supplies/bulk-status ----------

func TestBulkUpdateSupplyStatuses_OK(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	id := firstSupplyID(t, headers)
	body := map[string]any{
		"updates": []map[string]any{{
			"id":          id,
			"orderStatus": "waiting",
		}},
	}
	w := do(t, http.MethodPost, "/kitchen/supplies/bulk-status", body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestBulkUpdateSupplyStatuses_InvalidStatus_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	id := firstSupplyID(t, headers)
	body := map[string]any{
		"updates": []map[string]any{{
			"id":          id,
			"orderStatus": "moonbase",
		}},
	}
	w := do(t, http.MethodPost, "/kitchen/supplies/bulk-status", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "invalid orderstatus")
}

func TestBulkUpdateSupplyStatuses_MissingUpdates_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodPost, "/kitchen/supplies/bulk-status",
		map[string]any{}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "updates is required")
}

// ---------- POST /supplies/{id}/request (cross-team) ----------

func TestRequestSupplyByTeam_OK(t *testing.T) {
	headers := auth(t, bakeryEmail, bakeryPass)
	id := firstSupplyID(t, headers)
	body := map[string]any{"requestedQty": 1, "team": "bakery", "urgency": "waiting"}
	w := do(t, http.MethodPost, fmt.Sprintf("/supplies/%d/request", id), body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestRequestSupplyByTeam_InvalidTeam_BadRequest(t *testing.T) {
	headers := auth(t, kitchenEmail, kitchenPass)
	id := firstSupplyID(t, headers)
	body := map[string]any{"requestedQty": 1, "team": "alien"}
	w := do(t, http.MethodPost, fmt.Sprintf("/supplies/%d/request", id), body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "team")
}

// ---------- GET /management/supply-requests ----------

func TestSupplyRequests_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/management/supply-requests", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

// ---------- POST /management/supplies/{id}/fulfill ----------

func TestFulfillSupply_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	id := firstSupplyID(t, headers)
	body := map[string]any{"receivedQty": 1, "note": "test fulfillment"}
	w := do(t, http.MethodPost, fmt.Sprintf("/management/supplies/%d/fulfill", id), body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestFulfillSupply_MissingQty_BadRequest(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	id := firstSupplyID(t, headers)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/management/supplies/%d/fulfill", id),
		map[string]any{}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "receivedqty")
}

// ---------- helpers ----------

func firstSupplyID(t *testing.T, headers map[string]string) int64 {
	t.Helper()
	// /kitchen/in-stock returns supplies with inStock>0; /kitchen/supplies only
	// returns rows with non-received order_status. Fall back to the seed list
	// via a fresh seed call (idempotent).
	w := do(t, http.MethodGet, "/kitchen/in-stock", nil, headers)
	if w.Code == http.StatusOK {
		var rows []map[string]any
		mustDecode(t, w, &rows)
		if len(rows) > 0 {
			return int64(rows[0]["id"].(float64))
		}
	}
	// re-seed empty database
	_ = do(t, http.MethodPost, "/kitchen/supplies/seed", nil, headers)
	w = do(t, http.MethodGet, "/kitchen/in-stock", nil, headers)
	var rows []map[string]any
	mustDecode(t, w, &rows)
	if len(rows) == 0 {
		t.Fatal("could not find a supply id")
	}
	return int64(rows[0]["id"].(float64))
}

func randInt() int { return int(timeNanoMod10000()) }
func timeNanoMod10000() int64 {
	// Avoid pulling in time here: re-use uniqueEmail's clock indirectly.
	// We can simply read time.Now via uniqueEmail's behaviour; but for clarity:
	return time.Now().UnixNano() % 10000
}
