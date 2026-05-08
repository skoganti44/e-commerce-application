package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"
)

// Seed credentials from sql/02_data.sql. These must exist in the test DB.
const (
	customerEmail   = "alice@example.com"
	customerPass    = "Abc@1234"
	customerUserID  = 1
	bakeryEmail     = "bob@example.com"
	bakeryPass      = "Bob@1234"
	bakeryUserID    = 2
	salesEmail      = "carol@example.com"
	salesPass       = "Car@1234"
	salesUserID     = 3
	kitchenEmail    = "alexpin12@gmail.com"
	kitchenPass     = "Abc@1234"
	kitchenUserID   = 4
	deliveryEmail   = "dan.baker@example.com"
	deliveryPass    = "Dan@1234"
	deliveryUserID  = 5
	managerEmail    = "eva.sales@example.com"
	managerPass     = "Eva@1234"
	managerUserID   = 6
)

// do executes a request through the in-memory Gin router and returns the recorder.
func do(t *testing.T, method, path string, body any, headers map[string]string) *httptest.ResponseRecorder {
	t.Helper()
	var buf io.Reader
	if body != nil {
		switch b := body.(type) {
		case string:
			buf = strings.NewReader(b)
		case []byte:
			buf = bytes.NewReader(b)
		default:
			j, err := json.Marshal(body)
			if err != nil {
				t.Fatalf("marshal body: %v", err)
			}
			buf = bytes.NewReader(j)
		}
	}
	req := httptest.NewRequest(method, path, buf)
	if body != nil {
		req.Header.Set("Content-Type", "application/json")
	}
	for k, v := range headers {
		req.Header.Set(k, v)
	}
	w := httptest.NewRecorder()
	testApp.Router.ServeHTTP(w, req)
	return w
}

// auth returns a header map that authorises the request as the given seeded user.
func auth(t *testing.T, email, password string) map[string]string {
	t.Helper()
	w := do(t, http.MethodPost, "/login", map[string]string{
		"email":    email,
		"password": password,
	}, nil)
	if w.Code != http.StatusOK {
		t.Fatalf("login as %s: status=%d body=%s", email, w.Code, w.Body.String())
	}
	var resp struct {
		Token string `json:"token"`
	}
	mustDecode(t, w, &resp)
	return map[string]string{"Authorization": "Bearer " + resp.Token}
}

func mustDecode(t *testing.T, w *httptest.ResponseRecorder, into any) {
	t.Helper()
	if err := json.Unmarshal(w.Body.Bytes(), into); err != nil {
		t.Fatalf("decode response: %v\nbody=%s", err, w.Body.String())
	}
}

// assertStatus fails the test if the recorder doesn't carry the expected status.
func assertStatus(t *testing.T, w *httptest.ResponseRecorder, want int) {
	t.Helper()
	if w.Code != want {
		t.Fatalf("expected status %d, got %d. body=%s", want, w.Code, w.Body.String())
	}
}

// assertErrorContains decodes a {"error": "..."} body and checks the message.
func assertErrorContains(t *testing.T, w *httptest.ResponseRecorder, substr string) {
	t.Helper()
	var resp map[string]any
	if err := json.Unmarshal(w.Body.Bytes(), &resp); err != nil {
		t.Fatalf("decode error body: %v\nbody=%s", err, w.Body.String())
	}
	msg, _ := resp["error"].(string)
	if !strings.Contains(strings.ToLower(msg), strings.ToLower(substr)) {
		t.Fatalf("expected error containing %q, got %q", substr, msg)
	}
}

// uniqueEmail produces a stable but distinct email per invocation so register
// tests don't collide with existing rows.
func uniqueEmail(prefix string) string {
	return fmt.Sprintf("%s+%d@example.test", prefix, time.Now().UnixNano())
}
