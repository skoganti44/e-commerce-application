package main

import (
	"fmt"
	"net/http"
	"testing"
)

// validAddress returns an address payload that passes server-side validation.
func validAddress() map[string]any {
	return map[string]any{
		"fullName":    "Alice Test",
		"phone":       "9876543210",
		"line1":       "1 Test Lane",
		"line2":       "Apt 9",
		"landmark":    "Near park",
		"city":        "Bengaluru",
		"state":       "Karnataka",
		"pincode":     "56010",
		"country":     "India",
		"addressType": "HOME",
	}
}

// ---------- POST /shipping-address ----------

func TestShippingAddress_Save_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	body := map[string]any{
		"userid":  customerUserID,
		"address": validAddress(),
	}
	// pincode "56010" is 5 digits — make it pass
	addr := body["address"].(map[string]any)
	addr["pincode"] = "56010"
	w := do(t, http.MethodPost, "/shipping-address", body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestShippingAddress_BadPhone_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	addr := validAddress()
	addr["phone"] = "12"
	body := map[string]any{"userid": customerUserID, "address": addr}
	w := do(t, http.MethodPost, "/shipping-address", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "phone")
}

func TestShippingAddress_BadPincode_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	addr := validAddress()
	addr["pincode"] = "abc"
	body := map[string]any{"userid": customerUserID, "address": addr}
	w := do(t, http.MethodPost, "/shipping-address", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "zip")
}

func TestShippingAddress_MissingFullName_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	addr := validAddress()
	delete(addr, "fullName")
	body := map[string]any{"userid": customerUserID, "address": addr}
	w := do(t, http.MethodPost, "/shipping-address", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "fullname")
}

func TestShippingAddress_UnknownUser_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	body := map[string]any{"userid": 99999, "address": validAddress()}
	w := do(t, http.MethodPost, "/shipping-address", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- GET /shipping-address ----------

func TestShippingAddress_GetLatest_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	// ensure at least one row exists
	body := map[string]any{"userid": customerUserID, "address": validAddress()}
	_ = do(t, http.MethodPost, "/shipping-address", body, headers)

	w := do(t, http.MethodGet, fmt.Sprintf("/shipping-address?userid=%d", customerUserID), nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestShippingAddress_GetLatest_UnknownUser_NotFound(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	w := do(t, http.MethodGet, "/shipping-address?userid=99999", nil, headers)
	assertStatus(t, w, http.StatusNotFound)
}

// ---------- POST /checkout ----------

func TestCheckout_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	addCartItem(t, headers, customerUserID, 1, 1)

	body := map[string]any{
		"userid":        customerUserID,
		"paymentMethod": "DEBIT_CARD",
		"cardLast4":     "1234",
		"address":       validAddress(),
	}
	w := do(t, http.MethodPost, "/checkout", body, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	if resp["orderId"] == nil || resp["paymentId"] == nil {
		t.Fatalf("expected orderId+paymentId, got %v", resp)
	}
}

func TestCheckout_EmptyCart_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	cleanCart(t, headers, customerUserID) // ensure empty

	body := map[string]any{
		"userid":        customerUserID,
		"paymentMethod": "COD",
		"address":       validAddress(),
	}
	w := do(t, http.MethodPost, "/checkout", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "cart is empty")
}

func TestCheckout_InvalidPaymentMethod_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	addCartItem(t, headers, customerUserID, 1, 1)
	body := map[string]any{
		"userid":        customerUserID,
		"paymentMethod": "BARTER",
		"address":       validAddress(),
	}
	w := do(t, http.MethodPost, "/checkout", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "invalid payment method")
	cleanCart(t, headers, customerUserID)
}

func TestCheckout_MissingAddress_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	addCartItem(t, headers, customerUserID, 1, 1)
	body := map[string]any{
		"userid":        customerUserID,
		"paymentMethod": "COD",
	}
	w := do(t, http.MethodPost, "/checkout", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	cleanCart(t, headers, customerUserID)
}

// ---------- POST /counter/sale ----------

func TestCounterSale_Cash_OK(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"items": []map[string]any{
			{"productId": 1, "quantity": 2},
		},
		"paymentMethod": "CASH",
		"cashGiven":     200,
		"customerName":  "Walk-in Test",
	}
	w := do(t, http.MethodPost, "/counter/sale", body, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	if resp["orderId"] == nil {
		t.Fatalf("expected orderId, got %v", resp)
	}
}

func TestCounterSale_NoItems_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"items":         []map[string]any{},
		"paymentMethod": "CASH",
	}
	w := do(t, http.MethodPost, "/counter/sale", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "at least one item")
}

func TestCounterSale_InsufficientCash_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"items":         []map[string]any{{"productId": 5, "quantity": 1}}, // 699 each
		"paymentMethod": "CASH",
		"cashGiven":     1,
	}
	w := do(t, http.MethodPost, "/counter/sale", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "cash given")
}

func TestCounterSale_InvalidPaymentMethod_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"items":         []map[string]any{{"productId": 1, "quantity": 1}},
		"paymentMethod": "GIFT_CARD", // not in counter list
	}
	w := do(t, http.MethodPost, "/counter/sale", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "invalid payment method")
}

func TestCounterSale_Unauthorized(t *testing.T) {
	w := do(t, http.MethodPost, "/counter/sale", map[string]any{}, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}

// ---------- GET /orders ----------

func TestOrders_Customer_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	w := do(t, http.MethodGet, fmt.Sprintf("/orders?userid=%d", customerUserID), nil, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	if _, ok := resp["orders"]; !ok {
		t.Fatalf("expected orders key, got %v", resp)
	}
}

func TestOrders_NonCustomerUser_Forbidden(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	w := do(t, http.MethodGet, fmt.Sprintf("/orders?userid=%d", bakeryUserID), nil, headers)
	assertStatus(t, w, http.StatusForbidden)
	assertErrorContains(t, w, "not a customer")
}

func TestOrders_UnknownUser_NotFound(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	w := do(t, http.MethodGet, "/orders?userid=99999", nil, headers)
	assertStatus(t, w, http.StatusNotFound)
}

func TestOrders_MissingUserID_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	w := do(t, http.MethodGet, "/orders", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- GET /payments ----------

func TestPayments_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	w := do(t, http.MethodGet, fmt.Sprintf("/payments?userid=%d", customerUserID), nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestPayments_IncludeAll_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	w := do(t, http.MethodGet, fmt.Sprintf("/payments?userid=%d&includeAll=true", customerUserID), nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestPayments_UnknownUser_NotFound(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	w := do(t, http.MethodGet, "/payments?userid=99999", nil, headers)
	assertStatus(t, w, http.StatusNotFound)
}

// ---------- DELETE /cleanup ----------
//
// /cleanup wipes the customer's orders/payments/items. We don't run it against
// our seeded customer (would clobber data). Instead, we register a brand-new
// customer and call cleanup against them — empty result, no harm done.

func TestCleanup_FreshUser_OK(t *testing.T) {
	// register a brand new customer, then cleanup their (non-existent) orders.
	body := map[string]string{
		"name":     "Cleanup Tester",
		"email":    uniqueEmail("cleanup"),
		"password": "x",
		"userType": "customer",
	}
	w := do(t, http.MethodPost, "/register", body, nil)
	assertStatus(t, w, http.StatusOK)
	var u map[string]any
	mustDecode(t, w, &u)
	uid := int(u["userid"].(float64))

	headers := auth(t, body["email"], body["password"])
	w = do(t, http.MethodDelete, fmt.Sprintf("/cleanup?userid=%d", uid), nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestCleanup_UnknownUser_NotFound(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	w := do(t, http.MethodDelete, "/cleanup?userid=99999", nil, headers)
	assertStatus(t, w, http.StatusNotFound)
}
