package main

import (
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

var allowedTaskDepartments = newStrSet("bakery", "kitchen", "delivery", "management", "sales")
var allowedTaskPriorities = newStrSet("low", "normal", "high", "urgent")
var allowedTaskStatuses = newStrSet("open", "in_progress", "done", "cancelled")
var terminalTaskStatuses = newStrSet("done", "cancelled")

func (a *App) CreateTask(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	createdBy, ok := asInt(body["createdByUserId"])
	if !ok {
		writeError(c, badRequest("createdByUserId is required"))
		return
	}
	var assignedUserID *int
	if v, ok := asInt(body["assignedToUserId"]); ok {
		assignedUserID = &v
	}
	var relatedOrderID *int64
	if v, ok := asInt64(body["relatedOrderId"]); ok {
		relatedOrderID = &v
	}
	title := strings.TrimSpace(asStr(body["title"]))
	if title == "" {
		writeError(c, badRequest("title is required"))
		return
	}
	if len(title) > 200 {
		writeError(c, badRequest("title must be at most 200 characters"))
		return
	}
	deptRaw := strings.TrimSpace(asStr(body["assignedToDepartment"]))
	dept := strings.ToLower(deptRaw)
	if !allowedTaskDepartments.Has(dept) {
		writeError(c, badRequest("assignedToDepartment must be one of: "+strings.Join(allowedTaskDepartments.SortedKeys(), ", ")))
		return
	}
	priority := strings.ToLower(strings.TrimSpace(asStr(body["priority"])))
	if priority == "" {
		priority = "normal"
	}
	if !allowedTaskPriorities.Has(priority) {
		writeError(c, badRequest("priority must be one of: "+strings.Join(allowedTaskPriorities.SortedKeys(), ", ")))
		return
	}
	var dueDate *time.Time
	if v := strings.TrimSpace(asStr(body["dueDate"])); v != "" {
		t, err := time.Parse("2006-01-02", v)
		if err != nil {
			writeError(c, badRequest("Invalid dueDate (expected YYYY-MM-DD)"))
			return
		}
		dueDate = &t
	}
	creator, err := a.DB.FindUserByID(createdBy)
	if err != nil {
		writeError(c, err)
		return
	}
	if creator == nil {
		writeError(c, badRequest("Creator user not found: "+strconv.Itoa(createdBy)))
		return
	}
	if assignedUserID != nil {
		u, err := a.DB.FindUserByID(*assignedUserID)
		if err != nil {
			writeError(c, err)
			return
		}
		if u == nil {
			writeError(c, badRequest("Assignee user not found: "+strconv.Itoa(*assignedUserID)))
			return
		}
	}
	if relatedOrderID != nil {
		o, err := a.DB.FindOrderByID(*relatedOrderID)
		if err != nil {
			writeError(c, err)
			return
		}
		if o == nil {
			writeError(c, badRequest("Related order not found: "+strconv.FormatInt(*relatedOrderID, 10)))
			return
		}
	}
	now := time.Now()
	createdByCp := createdBy
	descRaw := strings.TrimSpace(asStr(body["description"]))
	var desc *string
	if descRaw != "" {
		desc = &descRaw
	}
	statusVal := "open"
	t := &Task{
		AssignedDepartment: &dept,
		AssignedUserID:     assignedUserID,
		Title:              &title,
		Description:        desc,
		Priority:           &priority,
		Status:             &statusVal,
		DueDate:            dueDate,
		RelatedOrderID:     relatedOrderID,
		CreatedAt:          nullTimeFrom(now),
		UpdatedAt:          nullTimeFrom(now),
		CreatedByUserID:    &createdByCp,
	}
	saved, err := a.DB.SaveTask(t)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toTaskMap(saved))
}

func (a *App) ListTasks(c *gin.Context) {
	dept := strings.TrimSpace(c.Query("department"))
	createdBy, hasCreated := queryInt(c, "createdByUserId")
	statusFilter := strings.TrimSpace(c.Query("status"))

	var tasks []Task
	var err error
	switch {
	case dept != "":
		d := strings.ToLower(dept)
		if !allowedTaskDepartments.Has(d) {
			writeError(c, badRequest("department must be one of: "+strings.Join(allowedTaskDepartments.SortedKeys(), ", ")))
			return
		}
		tasks, err = a.DB.FindTasksByDepartment(d)
	case hasCreated:
		tasks, err = a.DB.FindTasksByCreatedBy(createdBy)
	default:
		tasks, err = a.DB.FindAllTasks()
	}
	if err != nil {
		writeError(c, err)
		return
	}
	if statusFilter != "" {
		s := strings.ToLower(statusFilter)
		if !allowedTaskStatuses.Has(s) {
			writeError(c, badRequest("status must be one of: "+strings.Join(allowedTaskStatuses.SortedKeys(), ", ")))
			return
		}
		filtered := []Task{}
		for _, t := range tasks {
			if t.Status != nil && strings.EqualFold(*t.Status, s) {
				filtered = append(filtered, t)
			}
		}
		tasks = filtered
	}
	out := make([]any, 0, len(tasks))
	for i := range tasks {
		out = append(out, a.toTaskMap(&tasks[i]))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) UpdateTaskStatus(c *gin.Context) {
	taskID, err := pathInt64(c, "taskId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	statusRaw := strings.TrimSpace(asStr(body["status"]))
	if statusRaw == "" {
		writeError(c, badRequest("status is required"))
		return
	}
	newStatus := strings.ToLower(statusRaw)
	if !allowedTaskStatuses.Has(newStatus) {
		writeError(c, badRequest("status must be one of: "+strings.Join(allowedTaskStatuses.SortedKeys(), ", ")))
		return
	}
	t, err := a.DB.FindTaskByID(taskID)
	if err != nil {
		writeError(c, err)
		return
	}
	if t == nil {
		writeError(c, badRequest("Task not found: "+strconv.FormatInt(taskID, 10)))
		return
	}
	cur := "open"
	if t.Status != nil {
		cur = strings.ToLower(*t.Status)
	}
	if terminalTaskStatuses.Has(cur) {
		writeError(c, badRequest("Task is already "+cur+" and cannot change status"))
		return
	}
	t.Status = &newStatus
	now := time.Now()
	t.UpdatedAt = nullTimeFrom(now)
	if terminalTaskStatuses.Has(newStatus) {
		t.CompletedAt = nullTimeFrom(now)
		if v, ok := asInt(body["actingUserId"]); ok {
			u, err := a.DB.FindUserByID(v)
			if err != nil {
				writeError(c, err)
				return
			}
			if u == nil {
				writeError(c, badRequest("Acting user not found: "+strconv.Itoa(v)))
				return
			}
			vCp := v
			t.CompletedByUserID = &vCp
		}
		if notes := strings.TrimSpace(asStr(body["resolutionNotes"])); notes != "" {
			t.ResolutionNotes = &notes
		}
	}
	saved, err := a.DB.SaveTask(t)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toTaskMap(saved))
}

func (a *App) toTaskMap(t *Task) *Object {
	m := NewObject().
		Put("id", t.ID).
		Put("title", strOrNil(t.Title)).
		Put("description", strOrNil(t.Description)).
		Put("assignedToDepartment", strOrNil(t.AssignedDepartment))
	if t.AssignedUserID != nil {
		m.Put("assignedToUserId", *t.AssignedUserID)
		if u, _ := a.DB.FindUserByID(*t.AssignedUserID); u != nil {
			m.Put("assignedToUserName", strOrEmpty(u.Name))
		} else {
			m.Put("assignedToUserName", nil)
		}
	} else {
		m.Put("assignedToUserId", nil).Put("assignedToUserName", nil)
	}
	if t.CreatedByUserID != nil {
		m.Put("createdByUserId", *t.CreatedByUserID)
		if u, _ := a.DB.FindUserByID(*t.CreatedByUserID); u != nil {
			m.Put("createdByName", strOrEmpty(u.Name))
		} else {
			m.Put("createdByName", nil)
		}
	} else {
		m.Put("createdByUserId", nil).Put("createdByName", nil)
	}
	m.Put("priority", strOrNil(t.Priority)).
		Put("status", strOrNil(t.Status))
	if t.DueDate != nil {
		m.Put("dueDate", t.DueDate.Format("2006-01-02"))
	} else {
		m.Put("dueDate", nil)
	}
	if t.RelatedOrderID != nil {
		m.Put("relatedOrderId", *t.RelatedOrderID)
	} else {
		m.Put("relatedOrderId", nil)
	}
	m.Put("createdAt", isoOrNil(t.CreatedAt)).
		Put("updatedAt", isoOrNil(t.UpdatedAt)).
		Put("completedAt", isoOrNil(t.CompletedAt))
	if t.CompletedByUserID != nil {
		if u, _ := a.DB.FindUserByID(*t.CompletedByUserID); u != nil {
			m.Put("completedByName", strOrEmpty(u.Name))
		} else {
			m.Put("completedByName", nil)
		}
	} else {
		m.Put("completedByName", nil)
	}
	m.Put("resolutionNotes", strOrNil(t.ResolutionNotes))
	return m
}
