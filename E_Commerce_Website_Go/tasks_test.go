package main

import (
	"fmt"
	"net/http"
	"testing"
)

// ---------- POST /tasks ----------

func TestCreateTask_OK(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"createdByUserId":      salesUserID,
		"assignedToDepartment": "kitchen",
		"title":                "Bake more cookies",
		"description":          "Customer requested",
		"priority":             "high",
	}
	w := do(t, http.MethodPost, "/tasks", body, headers)
	assertStatus(t, w, http.StatusOK)
	var resp map[string]any
	mustDecode(t, w, &resp)
	if resp["id"] == nil {
		t.Fatalf("expected id, got %v", resp)
	}
}

func TestCreateTask_MissingTitle_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"createdByUserId":      salesUserID,
		"assignedToDepartment": "kitchen",
	}
	w := do(t, http.MethodPost, "/tasks", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "title is required")
}

func TestCreateTask_InvalidDepartment_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"createdByUserId":      salesUserID,
		"assignedToDepartment": "moonbase",
		"title":                "x",
	}
	w := do(t, http.MethodPost, "/tasks", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "assignedtodepartment")
}

func TestCreateTask_InvalidPriority_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"createdByUserId":      salesUserID,
		"assignedToDepartment": "kitchen",
		"title":                "x",
		"priority":             "warp",
	}
	w := do(t, http.MethodPost, "/tasks", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "priority")
}

func TestCreateTask_BadDueDate_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"createdByUserId":      salesUserID,
		"assignedToDepartment": "kitchen",
		"title":                "x",
		"dueDate":              "not-a-date",
	}
	w := do(t, http.MethodPost, "/tasks", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestCreateTask_UnknownCreator_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	body := map[string]any{
		"createdByUserId":      9999999,
		"assignedToDepartment": "kitchen",
		"title":                "x",
	}
	w := do(t, http.MethodPost, "/tasks", body, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "creator user not found")
}

// ---------- GET /tasks ----------

func TestListTasks_All_OK(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	w := do(t, http.MethodGet, "/tasks", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestListTasks_FilterByDepartment_OK(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	w := do(t, http.MethodGet, "/tasks?department=kitchen", nil, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestListTasks_InvalidDepartment_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	w := do(t, http.MethodGet, "/tasks?department=alien", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestListTasks_InvalidStatus_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	w := do(t, http.MethodGet, "/tasks?status=quantum", nil, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// ---------- POST /tasks/{taskId}/status ----------

func TestUpdateTaskStatus_OK(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	taskID := makeTask(t, headers, "kitchen", "Status flip target")
	body := map[string]any{
		"status":          "in_progress",
		"actingUserId":    kitchenUserID,
		"resolutionNotes": "started",
	}
	w := do(t, http.MethodPost, fmt.Sprintf("/tasks/%d/status", taskID), body, headers)
	assertStatus(t, w, http.StatusOK)
}

func TestUpdateTaskStatus_TerminalThenChange_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	taskID := makeTask(t, headers, "kitchen", "Cancel target")
	w := do(t, http.MethodPost, fmt.Sprintf("/tasks/%d/status", taskID),
		map[string]any{"status": "cancelled", "actingUserId": kitchenUserID}, headers)
	assertStatus(t, w, http.StatusOK)

	w = do(t, http.MethodPost, fmt.Sprintf("/tasks/%d/status", taskID),
		map[string]any{"status": "open"}, headers)
	assertStatus(t, w, http.StatusBadRequest)
	assertErrorContains(t, w, "already")
}

func TestUpdateTaskStatus_InvalidStatus_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	taskID := makeTask(t, headers, "kitchen", "Bad status target")
	w := do(t, http.MethodPost, fmt.Sprintf("/tasks/%d/status", taskID),
		map[string]any{"status": "warp"}, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

func TestUpdateTaskStatus_UnknownTask_BadRequest(t *testing.T) {
	headers := auth(t, salesEmail, salesPass)
	w := do(t, http.MethodPost, "/tasks/9999999/status",
		map[string]any{"status": "done"}, headers)
	assertStatus(t, w, http.StatusBadRequest)
}

// helper -------------------------------------------------------------------

func makeTask(t *testing.T, headers map[string]string, department, title string) int64 {
	t.Helper()
	body := map[string]any{
		"createdByUserId":      salesUserID,
		"assignedToDepartment": department,
		"title":                title,
	}
	w := do(t, http.MethodPost, "/tasks", body, headers)
	if w.Code != http.StatusOK {
		t.Fatalf("seed task: %s", w.Body.String())
	}
	var resp struct {
		ID int64 `json:"id"`
	}
	mustDecode(t, w, &resp)
	return resp.ID
}
