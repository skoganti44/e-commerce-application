package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"testing"
)

// ---------- POST /cart/add ----------

func TestCartAdd_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	body := map[string]any{
		"userid":    customerUserID,
		"productId": 1,
		"quantity":  2,
	}
	w := do(t, http.MethodPost, "/cart/add", body, headers)
	assertStatus(t, w, http.StatusOK)
	cleanCart(t, headers, customerUserID)
}

func TestCartAdd_WithCustomization_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	body := map[string]any{
		"userid":           customerUserID,
		"productId":        1,
		"quantity":         1,
		"sweetenerType":    "JAGGERY",
		"sweetenerPercent": 50,
	}
	w := do(t, http.MethodPost, "/cart/add", body, headers)
	assertStatus(t, w, http.StatusOK)
	cleanCart(t, headers, customerUserID)
}

func TestCartAdd_InvalidSweetener_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	body := map[string]any{
		"userid":        customerUserID,
		"productId":     1,
		"quantity":      1,
		"sweetenerType": "MOLASSES_OF_DOOM",
	}
	w := do(t, http.MethodPost, "/cart/add", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "invalid sweetener")
}

func TestCartAdd_NegativeQuantity_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	body := map[string]any{"userid": customerUserID, "productId": 1, "quantity": -1}
	w := do(t, http.MethodPost, "/cart/add", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "quantity must be positive")
}

func TestCartAdd_UnknownProduct_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	body := map[string]any{"userid": customerUserID, "productId": 999999, "quantity": 1}
	w := do(t, http.MethodPost, "/cart/add", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "product not found")
}

func TestCartAdd_UnknownUser_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	body := map[string]any{"userid": 999999, "productId": 1, "quantity": 1}
	w := do(t, http.MethodPost, "/cart/add", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "user not found")
}

func TestCartAdd_Unauthorized(t *testing.T) {
	w := do(t, http.MethodPost, "/cart/add", map[string]any{}, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}

// ---------- GET /cart ----------

func TestCart_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	addCartItem(t, headers, customerUserID, 1, 1)
	w := do(t, http.MethodGet, fmt.Sprintf("/cart?userid=%d", customerUserID), nil, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	if resp["totals"] == nil {
		t.Fatalf("expected totals key, got %v", resp)
	}
	cleanCart(t, headers, customerUserID)
}

func TestCart_NoCart_NotFound(t *testing.T) {
	// userid=2 (bob) is an employee — no cart row.
	headers := auth(t, bakeryEmail, bakeryPass)
	w := do(t, http.MethodGet, "/cart?userid=999999", nil, headers)
	assertStatus(t, w, http.StatusNotFound)
}

// ---------- POST /cart/item/update ----------

func TestCartUpdate_QuantityChange_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	itemID := addCartItem(t, headers, customerUserID, 2, 1)

	body := map[string]any{
		"userid":     customerUserID,
		"cartItemId": itemID,
		"quantity":   3,
	}
	w := do(t, http.MethodPost, "/cart/item/update", body, headers)
	assertStatus(t, w, http.StatusOK)
	cleanCart(t, headers, customerUserID)
}

func TestCartUpdate_ZeroQuantity_RemovesItem(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	itemID := addCartItem(t, headers, customerUserID, 2, 1)

	body := map[string]any{
		"userid":     customerUserID,
		"cartItemId": itemID,
		"quantity":   0,
	}
	w := do(t, http.MethodPost, "/cart/item/update", body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestCartUpdate_UnknownItem_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	body := map[string]any{
		"userid":     customerUserID,
		"cartItemId": 999999999,
		"quantity":   1,
	}
	w := do(t, http.MethodPost, "/cart/item/update", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "cart item not found")
}

func TestCartUpdate_MissingItemID_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	body := map[string]any{
		"userid":   customerUserID,
		"quantity": 1,
	}
	w := do(t, http.MethodPost, "/cart/item/update", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "cartItemId is required")
}

// ---------- DELETE /cart/item ----------

func TestCartRemove_OK(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	itemID := addCartItem(t, headers, customerUserID, 3, 1)

	url := fmt.Sprintf("/cart/item?userid=%d&itemId=%d", customerUserID, itemID)
	w := do(t, http.MethodDelete, url, nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestCartRemove_MissingItemID_BadRequest(t *testing.T) {
	headers := auth(t, customerEmail, customerPass)
	w := do(t, http.MethodDelete, "/cart/item?userid=1", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestCartRemove_Unauthorized(t *testing.T) {
	w := do(t, http.MethodDelete, "/cart/item?userid=1&itemId=1", nil, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}

// ---------- helpers ----------

// addCartItem adds a single item to the customer's cart and returns the cart_items.id.
func addCartItem(t *testing.T, headers map[string]string, userid int, productID, qty int) int64 {
	t.Helper()
	body := map[string]any{"userid": userid, "productId": productID, "quantity": qty}
	w := do(t, http.MethodPost, "/cart/add", body, headers)
	if w.Code != http.StatusOK {
		t.Fatalf("seed cart item: status=%d body=%s", w.Code, w.Body.String())
	}
	// Fetch the cart and grab the latest item.
	w = do(t, http.MethodGet, fmt.Sprintf("/cart?userid=%d", userid), nil, headers)
	if w.Code != http.StatusOK {
		t.Fatalf("read cart: %s", w.Body.String())
	}
	var resp struct {
		Items []struct {
			ID int64 `json:"id"`
		} `json:"items"`
	}
	mustDecode(t, w, &resp)
	if len(resp.Items) == 0 {
		t.Fatalf("no items in cart after add")
	}
	return resp.Items[len(resp.Items)-1].ID
}

// cleanCart deletes every cart_items row owned by the user (best effort).
func cleanCart(t *testing.T, headers map[string]string, userid int) {
	t.Helper()
	w := do(t, http.MethodGet, fmt.Sprintf("/cart?userid=%d", userid), nil, headers)
	if w.Code != http.StatusOK {
		return
	}
	var resp struct {
		Items []struct {
			ID int64 `json:"id"`
		} `json:"items"`
	}
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		return
	}
	for _, it := range resp.Items {
		_ = do(t, http.MethodDelete,
			fmt.Sprintf("/cart/item?userid=%d&itemId=%d", userid, it.ID), nil, headers)
	}
}
