package main

import (
	"fmt"
	"net/http"
	"testing"
)

// makeDoneOrder produces an online order whose kitchen_status has been moved
// to 'done', i.e. ready for the delivery team to pick up.
func makeDoneOrder(t *testing.T) int64 {
	t.Helper()
	orderID := makeKitchenOrder(t)
	headers := auth(t, kitchenEmail, kitchenPass)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/kitchen/order/%d/status", orderID),
		map[string]any{"kitchenStatus": "done"}, headers)
	if w.Code != http.StatusOK {
		t.Fatalf("move to done: %s", w.Body.String())
	}
	return orderID
}

// ---------- POST /delivery/trips (pickup) ----------

func TestPickUpTrip_OK(t *testing.T) {
	orderID := makeDoneOrder(t)
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodPost, "/delivery/trips",
		map[string]any{"orderId": orderID, "driverId": deliveryUserID}, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestPickUpTrip_OrderNotDone_BadRequest(t *testing.T) {
	orderID := makeKitchenOrder(t) // still 'pending'
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodPost, "/delivery/trips",
		map[string]any{"orderId": orderID, "driverId": deliveryUserID}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "kitchen status must be 'done'")
}

func TestPickUpTrip_UnknownOrder_BadRequest(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodPost, "/delivery/trips",
		map[string]any{"orderId": 9999999, "driverId": deliveryUserID}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "order not found")
}

// ---------- Trip lifecycle: out → deliver / fail ----------

func makeTrip(t *testing.T) int64 {
	t.Helper()
	orderID := makeDoneOrder(t)
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodPost, "/delivery/trips",
		map[string]any{"orderId": orderID, "driverId": deliveryUserID}, headers)
	if w.Code != http.StatusOK {
		t.Fatalf("seed trip: %s", w.Body.String())
	}
	var resp struct {
		ID int64 `json:"id"`
	}
	mustDecode(t, w, &resp)
	return resp.ID
}

func TestMarkOutForDelivery_OK(t *testing.T) {
	tripID := makeTrip(t)
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/delivery/trips/%d/out", tripID),
		map[string]any{"driverId": deliveryUserID}, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestMarkOutForDelivery_WrongDriver_BadRequest(t *testing.T) {
	tripID := makeTrip(t)
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/delivery/trips/%d/out", tripID),
		map[string]any{"driverId": 99}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "not assigned to you")
}

func TestMarkDelivered_OK(t *testing.T) {
	tripID := makeTrip(t)
	headers := auth(t, deliveryEmail, deliveryPass)
	// move to out_for_delivery first
	_ = do(t, http.MethodPost,
		fmt.Sprintf("/delivery/trips/%d/out", tripID),
		map[string]any{"driverId": deliveryUserID}, headers)
	body := map[string]any{
		"driverId":   deliveryUserID,
		"photoUrl":   "https://example.test/proof.jpg",
		"codAmount":  100,
		"tipAmount":  10,
		"distanceKm": 5,
	}
	w := do(t, http.MethodPost,
		fmt.Sprintf("/delivery/trips/%d/deliver", tripID), body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestMarkDelivered_NoProof_BadRequest(t *testing.T) {
	tripID := makeTrip(t)
	headers := auth(t, deliveryEmail, deliveryPass)
	_ = do(t, http.MethodPost,
		fmt.Sprintf("/delivery/trips/%d/out", tripID),
		map[string]any{"driverId": deliveryUserID}, headers)
	body := map[string]any{"driverId": deliveryUserID}
	w := do(t, http.MethodPost,
		fmt.Sprintf("/delivery/trips/%d/deliver", tripID), body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "proof of delivery")
}

func TestMarkDelivered_NotOutForDelivery_BadRequest(t *testing.T) {
	// Right after pickup, status is 'picked_up', not 'out_for_delivery'.
	tripID := makeTrip(t)
	headers := auth(t, deliveryEmail, deliveryPass)
	body := map[string]any{
		"driverId": deliveryUserID,
		"photoUrl": "https://example.test/p.jpg",
	}
	w := do(t, http.MethodPost,
		fmt.Sprintf("/delivery/trips/%d/deliver", tripID), body, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestMarkTripFailed_OK(t *testing.T) {
	tripID := makeTrip(t)
	headers := auth(t, deliveryEmail, deliveryPass)
	body := map[string]any{
		"driverId": deliveryUserID,
		"reason":   "customer_not_home",
		"notes":    "left a note",
	}
	w := do(t, http.MethodPost,
		fmt.Sprintf("/delivery/trips/%d/fail", tripID), body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestMarkTripFailed_BadReason_BadRequest(t *testing.T) {
	tripID := makeTrip(t)
	headers := auth(t, deliveryEmail, deliveryPass)
	body := map[string]any{"driverId": deliveryUserID, "reason": "abducted_by_aliens"}
	w := do(t, http.MethodPost,
		fmt.Sprintf("/delivery/trips/%d/fail", tripID), body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "failure reason must be one of")
}

func TestMarkTripFailed_MissingReason_BadRequest(t *testing.T) {
	tripID := makeTrip(t)
	headers := auth(t, deliveryEmail, deliveryPass)
	body := map[string]any{"driverId": deliveryUserID}
	w := do(t, http.MethodPost,
		fmt.Sprintf("/delivery/trips/%d/fail", tripID), body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "failure reason is required")
}

// ---------- GET /delivery/trips ----------

func TestListTrips_OK(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodGet,
		fmt.Sprintf("/delivery/trips?driverId=%d", deliveryUserID), nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestListTrips_ActiveFilter_OK(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodGet,
		fmt.Sprintf("/delivery/trips?driverId=%d&status=active", deliveryUserID), nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestListTrips_InvalidStatus_BadRequest(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodGet,
		fmt.Sprintf("/delivery/trips?driverId=%d&status=warp", deliveryUserID), nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestListTrips_MissingDriver_BadRequest(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodGet, "/delivery/trips", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- POST /delivery/issues ----------

func TestLogIssue_OK(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	body := map[string]any{
		"driverId":    deliveryUserID,
		"issueType":   "traffic_delay",
		"description": "stuck in traffic",
	}
	w := do(t, http.MethodPost, "/delivery/issues", body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestLogIssue_MissingType_BadRequest(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	body := map[string]any{"driverId": deliveryUserID, "description": "x"}
	w := do(t, http.MethodPost, "/delivery/issues", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestLogIssue_MissingDescription_BadRequest(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	body := map[string]any{"driverId": deliveryUserID, "issueType": "other"}
	w := do(t, http.MethodPost, "/delivery/issues", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- GET /delivery/issues ----------

func TestListIssues_OK(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodGet,
		fmt.Sprintf("/delivery/issues?driverId=%d", deliveryUserID), nil, headers)
	assertStatus(t, w, http.StatusOK)
}

// ---------- GET /delivery/summary ----------

func TestShiftSummary_OK(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodGet,
		fmt.Sprintf("/delivery/summary?driverId=%d", deliveryUserID), nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestShiftSummary_BadDate_BadRequest(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodGet,
		fmt.Sprintf("/delivery/summary?driverId=%d&from=oof", deliveryUserID), nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestShiftSummary_FromAfterTo_BadRequest(t *testing.T) {
	headers := auth(t, deliveryEmail, deliveryPass)
	w := do(t, http.MethodGet,
		fmt.Sprintf("/delivery/summary?driverId=%d&from=2030-01-02&to=2030-01-01", deliveryUserID),
		nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}
