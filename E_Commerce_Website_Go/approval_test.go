package main

import (
	"fmt"
	"net/http"
	"testing"
)

// ---------- Order approval flow ----------

func TestFlagOrderForApproval_OK(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/flag-approval", orderID),
		map[string]any{"notes": "needs review"}, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestFlagOrderForApproval_AlreadyDecided_BadRequest(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	// First flag → success.
	_ = do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/flag-approval", orderID),
		map[string]any{"notes": "first"}, headers)
	// Decide it.
	_ = do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/approval-decision", orderID),
		map[string]any{"managerUserId": managerUserID, "decision": "approved"}, headers)
	// Re-flag → should error.
	w := do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/flag-approval", orderID),
		map[string]any{"notes": "again"}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "already has decision")
}

func TestFlagOrderForApproval_UnknownOrder_BadRequest(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodPost, "/orders/9999999/flag-approval",
		map[string]any{}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "order not found")
}

func TestListPendingApproval_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/orders/pending-approval", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestDecideOrderApproval_Approved_OK(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	_ = do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/flag-approval", orderID),
		map[string]any{}, headers)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/approval-decision", orderID),
		map[string]any{"managerUserId": managerUserID, "decision": "approved"}, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestDecideOrderApproval_Rejected_OK(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	_ = do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/flag-approval", orderID),
		map[string]any{}, headers)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/approval-decision", orderID),
		map[string]any{"managerUserId": managerUserID, "decision": "rejected"}, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestDecideOrderApproval_NotFlagged_BadRequest(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/approval-decision", orderID),
		map[string]any{"managerUserId": managerUserID, "decision": "approved"}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "not flagged for approval")
}

func TestDecideOrderApproval_InvalidDecision_BadRequest(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	_ = do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/flag-approval", orderID),
		map[string]any{}, headers)
	w := do(t, http.MethodPost,
		fmt.Sprintf("/orders/%d/approval-decision", orderID),
		map[string]any{"managerUserId": managerUserID, "decision": "maybe"}, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- Refund requests ----------

func TestRaiseRefundRequest_OK(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	body := map[string]any{
		"orderId":        orderID,
		"raisedByUserId": managerUserID,
		"requestType":    "refund",
		"reason":         "customer complaint",
		"amount":         50,
	}
	w := do(t, http.MethodPost, "/refund-requests", body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestRaiseRefundRequest_InvalidType_BadRequest(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	body := map[string]any{
		"orderId": orderID, "raisedByUserId": managerUserID,
		"requestType": "alien_credit", "reason": "x", "amount": 10,
	}
	w := do(t, http.MethodPost, "/refund-requests", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "requesttype")
}

func TestRaiseRefundRequest_NegativeAmount_BadRequest(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	body := map[string]any{
		"orderId": orderID, "raisedByUserId": managerUserID,
		"requestType": "refund", "reason": "x", "amount": -1,
	}
	w := do(t, http.MethodPost, "/refund-requests", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "amount")
}

func TestRaiseRefundRequest_MissingReason_BadRequest(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	body := map[string]any{
		"orderId": orderID, "raisedByUserId": managerUserID,
		"requestType": "refund", "amount": 1,
	}
	w := do(t, http.MethodPost, "/refund-requests", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "reason is required")
}

func TestListRefundRequests_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/refund-requests", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestListRefundRequests_BadStatus_BadRequest(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/refund-requests?status=alien", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestDecideRefundRequest_OK(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodPost, "/refund-requests", map[string]any{
		"orderId": orderID, "raisedByUserId": managerUserID,
		"requestType": "refund", "reason": "test", "amount": 10,
	}, headers)
	assertStatus(t, w, http.StatusOK)
	var rr map[string]any
	mustDecode(t, w, &rr)
	id := int64(rr["id"].(float64))

	w = do(t, http.MethodPost, fmt.Sprintf("/refund-requests/%d/decision", id),
		map[string]any{"managerUserId": managerUserID, "decision": "approved"}, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestDecideRefundRequest_AlreadyDecided_BadRequest(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodPost, "/refund-requests", map[string]any{
		"orderId": orderID, "raisedByUserId": managerUserID,
		"requestType": "refund", "reason": "test", "amount": 10,
	}, headers)
	var rr map[string]any
	mustDecode(t, w, &rr)
	id := int64(rr["id"].(float64))

	_ = do(t, http.MethodPost, fmt.Sprintf("/refund-requests/%d/decision", id),
		map[string]any{"managerUserId": managerUserID, "decision": "approved"}, headers)
	w = do(t, http.MethodPost, fmt.Sprintf("/refund-requests/%d/decision", id),
		map[string]any{"managerUserId": managerUserID, "decision": "rejected"}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "already")
}

func TestDecideRefundRequest_InvalidDecision_BadRequest(t *testing.T) {
	orderID := makeKitchenOrder(t)
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodPost, "/refund-requests", map[string]any{
		"orderId": orderID, "raisedByUserId": managerUserID,
		"requestType": "refund", "reason": "test", "amount": 10,
	}, headers)
	var rr map[string]any
	mustDecode(t, w, &rr)
	id := int64(rr["id"].(float64))

	w = do(t, http.MethodPost, fmt.Sprintf("/refund-requests/%d/decision", id),
		map[string]any{"managerUserId": managerUserID, "decision": "magic"}, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- Discount campaigns ----------

func TestProposeCampaign_OK(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"proposedByUserId": salesUserID,
		"name":             fmt.Sprintf("Test Campaign %d", randInt()),
		"discountPercent":  10,
		"startsOn":         "2030-01-01",
		"endsOn":           "2030-01-31",
	}
	w := do(t, http.MethodPost, "/discount-campaigns", body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestProposeCampaign_OutOfRangePercent_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"proposedByUserId": salesUserID,
		"name":             "x",
		"discountPercent":  150,
	}
	w := do(t, http.MethodPost, "/discount-campaigns", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "discountpercent")
}

func TestProposeCampaign_EndsBeforeStarts_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"proposedByUserId": salesUserID,
		"name":             "x",
		"discountPercent":  10,
		"startsOn":         "2030-02-01",
		"endsOn":           "2030-01-01",
	}
	w := do(t, http.MethodPost, "/discount-campaigns", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "endson")
}

func TestProposeCampaign_MissingName_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"proposedByUserId": salesUserID,
		"discountPercent":  10,
	}
	w := do(t, http.MethodPost, "/discount-campaigns", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "name is required")
}

func TestListCampaigns_OK(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/discount-campaigns", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestListCampaigns_BadStatus_BadRequest(t *testing.T) {
	headers := auth(t, managerEmail, managerPass)
	w := do(t, http.MethodGet, "/discount-campaigns?status=alien", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestDecideCampaign_OK(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"proposedByUserId": salesUserID,
		"name":             fmt.Sprintf("Decide-OK %d", randInt()),
		"discountPercent":  5,
	}
	w := do(t, http.MethodPost, "/discount-campaigns", body, headers)
	assertStatus(t, w, http.StatusOK)
	var c map[string]any
	mustDecode(t, w, &c)
	id := int64(c["id"].(float64))

	mgr := auth(t, managerEmail, managerPass)
	w = do(t, http.MethodPost, fmt.Sprintf("/discount-campaigns/%d/decision", id),
		map[string]any{"managerUserId": managerUserID, "decision": "approved"}, mgr)
	assertStatus(t, w, http.StatusOK)
}

func TestDecideCampaign_Twice_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"proposedByUserId": salesUserID,
		"name":             fmt.Sprintf("Decide-Twice %d", randInt()),
		"discountPercent":  5,
	}
	w := do(t, http.MethodPost, "/discount-campaigns", body, headers)
	var c map[string]any
	mustDecode(t, w, &c)
	id := int64(c["id"].(float64))

	mgr := auth(t, managerEmail, managerPass)
	_ = do(t, http.MethodPost, fmt.Sprintf("/discount-campaigns/%d/decision", id),
		map[string]any{"managerUserId": managerUserID, "decision": "approved"}, mgr)
	w = do(t, http.MethodPost, fmt.Sprintf("/discount-campaigns/%d/decision", id),
		map[string]any{"managerUserId": managerUserID, "decision": "rejected"}, mgr)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestDecideCampaign_BadDecision_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"proposedByUserId": salesUserID,
		"name":             fmt.Sprintf("BadDecide %d", randInt()),
		"discountPercent":  5,
	}
	w := do(t, http.MethodPost, "/discount-campaigns", body, headers)
	var c map[string]any
	mustDecode(t, w, &c)
	id := int64(c["id"].(float64))

	mgr := auth(t, managerEmail, managerPass)
	w = do(t, http.MethodPost, fmt.Sprintf("/discount-campaigns/%d/decision", id),
		map[string]any{"managerUserId": managerUserID, "decision": "magic"}, mgr)
	assertStatus(t, w, http.StatusBadRequest)
}
