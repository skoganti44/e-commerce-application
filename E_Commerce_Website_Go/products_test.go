package main

import (
	"net/http"
	"testing"
)

// ---------- GET /products (public) ----------

func TestProducts_PublicAccess_OK(t *testing.T) {
	w := do(t, http.MethodGet, "/products", nil, nil) // no Authorization header
	assertStatus(t, w, http.StatusOK)
	var products []map[string]any
	mustDecode(t, w, &products)
	if len(products) == 0 {
		t.Fatal("expected at least the seeded products")
	}
	first := products[0]
	for _, key := range []string{"id", "name", "price", "stock", "category"} {
		if _, ok := first[key]; !ok {
			t.Fatalf("expected key %q in product, got %v", key, first)
		}
	}
}

// ---------- POST /product (employee only) ----------

func TestSaveProduct_AsEmployee_OK(t *testing.T) {
	body := map[string]any{
		"userId":      bakeryUserID,
		"name":        "Test Cookie Family",
		"description": "Created by tests",
		"items": []map[string]any{{
			"category": map[string]any{"categoryName": "Cookies", "type": "savoury"},
			"price":    25,
			"stock":    10,
			"imageUrl": "https://example.test/img.png",
		}},
	}
	w := do(t, http.MethodPost, "/product", body, auth(t, bakeryEmail, bakeryPass))
	assertStatus(t, w, http.StatusOK)
}

func TestSaveProduct_AsCustomer_Forbidden(t *testing.T) {
	body := map[string]any{
		"userId": customerUserID,
		"name":   "Sneak",
		"items": []map[string]any{{
			"category": map[string]any{"categoryName": "X", "type": "y"},
			"price":    1,
			"stock":    1,
		}},
	}
	w := do(t, http.MethodPost, "/product", body, auth(t, customerEmail, customerPass))
	assertStatus(t, w, http.StatusForbidden)
	assertErrorContains(t, w, "only employees")
}

func TestSaveProduct_MissingUserID_BadRequest(t *testing.T) {
	body := map[string]any{
		"name":  "x",
		"items": []map[string]any{{"category": map[string]any{"categoryName": "C", "type": "t"}, "price": 1, "stock": 1}},
	}
	w := do(t, http.MethodPost, "/product", body, auth(t, bakeryEmail, bakeryPass))
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "userid is required")
}

func TestSaveProduct_Unauthorized(t *testing.T) {
	w := do(t, http.MethodPost, "/product", map[string]any{}, nil)
	assertStatus(t, w, http.StatusUnauthorized)
}

// ---------- POST /products (bulk) ----------

func TestSaveProducts_Bulk_OK(t *testing.T) {
	body := map[string]any{
		"userId": bakeryUserID,
		"products": []map[string]any{{
			"name":        "Bulk Test Pack",
			"description": "From tests",
			"items": []map[string]any{{
				"category": map[string]any{"categoryName": "Bulk", "type": "test"},
				"price":    11,
				"stock":    5,
			}},
		}},
	}
	w := do(t, http.MethodPost, "/products", body, auth(t, bakeryEmail, bakeryPass))
	assertStatus(t, w, http.StatusOK)
}

func TestSaveProducts_AsCustomer_Forbidden(t *testing.T) {
	body := map[string]any{
		"userId":   customerUserID,
		"products": []map[string]any{},
	}
	w := do(t, http.MethodPost, "/products", body, auth(t, customerEmail, customerPass))
	assertStatus(t, w, http.StatusForbidden)
}
