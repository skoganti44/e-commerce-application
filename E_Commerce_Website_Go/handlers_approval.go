package main

import (
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
)

var allowedApprovalDecisions = newStrSet("approved", "rejected")
var allowedRefundTypes = newStrSet("refund", "cancellation", "damage_writeoff")
var allowedRefundDecisions = newStrSet("approved", "rejected")
var allowedCampaignDecisions = newStrSet("approved", "rejected")

func (a *App) FlagOrderForApproval(c *gin.Context) {
	orderID, err := pathInt64(c, "orderId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	order, err := a.DB.FindOrderByID(orderID)
	if err != nil {
		writeError(c, err)
		return
	}
	if order == nil {
		writeError(c, badRequest("Order not found: "+strconv.FormatInt(orderID, 10)))
		return
	}
	yes := true
	order.RequiresApproval = &yes
	if order.ApprovalStatus == nil || strings.EqualFold(*order.ApprovalStatus, "pending") {
		pending := "pending"
		order.ApprovalStatus = &pending
	} else {
		writeError(c, badRequest("Order already has decision: "+*order.ApprovalStatus))
		return
	}
	if notes := strings.TrimSpace(asStr(body["notes"])); notes != "" {
		order.ApprovalNotes = &notes
	}
	if order.KitchenStatus == nil || strings.EqualFold(*order.KitchenStatus, "pending") {
		await := "awaiting_approval"
		order.KitchenStatus = &await
	}
	saved, err := a.DB.SaveOrder(order)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toApprovalMap(saved))
}

func (a *App) ListOrdersPendingApproval(c *gin.Context) {
	orders, err := a.DB.FindOrdersPendingApproval()
	if err != nil {
		writeError(c, err)
		return
	}
	out := make([]any, 0, len(orders))
	for i := range orders {
		out = append(out, a.toApprovalMap(&orders[i]))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) DecideOrderApproval(c *gin.Context) {
	orderID, err := pathInt64(c, "orderId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	decisionRaw := strings.TrimSpace(asStr(body["decision"]))
	if decisionRaw == "" {
		writeError(c, badRequest("decision is required"))
		return
	}
	decision := strings.ToLower(decisionRaw)
	if !allowedApprovalDecisions.Has(decision) {
		writeError(c, badRequest("decision must be one of: "+strings.Join(allowedApprovalDecisions.SortedKeys(), ", ")))
		return
	}
	order, err := a.DB.FindOrderByID(orderID)
	if err != nil {
		writeError(c, err)
		return
	}
	if order == nil {
		writeError(c, badRequest("Order not found: "+strconv.FormatInt(orderID, 10)))
		return
	}
	if order.RequiresApproval == nil || !*order.RequiresApproval {
		writeError(c, badRequest("Order is not flagged for approval"))
		return
	}
	if order.ApprovalStatus != nil && !strings.EqualFold(*order.ApprovalStatus, "pending") {
		writeError(c, badRequest("Order is already "+*order.ApprovalStatus))
		return
	}
	managerID, ok := asInt(body["managerUserId"])
	if !ok {
		writeError(c, badRequest("managerUserId is required"))
		return
	}
	manager, err := a.DB.FindUserByID(managerID)
	if err != nil {
		writeError(c, err)
		return
	}
	if manager == nil {
		writeError(c, badRequest("Manager not found: "+strconv.Itoa(managerID)))
		return
	}
	order.ApprovalStatus = &decision
	managerCp := managerID
	order.ApprovedByUserID = &managerCp
	order.ApprovedAt = nullTimeFrom(time.Now())
	if notes := strings.TrimSpace(asStr(body["notes"])); notes != "" {
		order.ApprovalNotes = &notes
	}
	if decision == "approved" {
		ks := "pending"
		order.KitchenStatus = &ks
	} else {
		st := "cancelled"
		ks := "cancelled"
		order.Status = &st
		order.KitchenStatus = &ks
	}
	saved, err := a.DB.SaveOrder(order)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toApprovalMap(saved))
}

func (a *App) toApprovalMap(o *Order) *Object {
	m := NewObject().
		Put("orderId", o.ID).
		Put("totalAmount", o.TotalAmount).
		Put("channel", strOrNil(o.Channel)).
		Put("status", strOrNil(o.Status)).
		Put("kitchenStatus", strOrNil(o.KitchenStatus))
	if o.UserID != nil {
		if u, _ := a.DB.FindUserByID(*o.UserID); u != nil {
			m.Put("customerName", strOrEmpty(u.Name))
		} else {
			m.Put("customerName", nil)
		}
	} else {
		m.Put("customerName", nil)
	}
	m.Put("requiresApproval", o.RequiresApproval).
		Put("approvalStatus", strOrNil(o.ApprovalStatus)).
		Put("approvalNotes", strOrNil(o.ApprovalNotes))
	if o.ApprovedByUserID != nil {
		m.Put("approvedById", *o.ApprovedByUserID)
		if u, _ := a.DB.FindUserByID(*o.ApprovedByUserID); u != nil {
			m.Put("approvedByName", strOrEmpty(u.Name))
		} else {
			m.Put("approvedByName", nil)
		}
	} else {
		m.Put("approvedById", nil).Put("approvedByName", nil)
	}
	m.Put("approvedAt", isoOrNil(o.ApprovedAt)).
		Put("createdAt", isoOrNil(o.CreatedAt))
	return m
}

// Refund requests ----------------------------------------------------------

func (a *App) RaiseRefundRequest(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	orderID, ok := asInt64(body["orderId"])
	if !ok {
		writeError(c, badRequest("orderId is required"))
		return
	}
	raisedByID, ok := asInt(body["raisedByUserId"])
	if !ok {
		writeError(c, badRequest("raisedByUserId is required"))
		return
	}
	typeRaw := strings.TrimSpace(asStr(body["requestType"]))
	if typeRaw == "" {
		writeError(c, badRequest("requestType is required"))
		return
	}
	rtype := strings.ToLower(typeRaw)
	if !allowedRefundTypes.Has(rtype) {
		writeError(c, badRequest("requestType must be one of: "+strings.Join(allowedRefundTypes.SortedKeys(), ", ")))
		return
	}
	reason := strings.TrimSpace(asStr(body["reason"]))
	if reason == "" {
		writeError(c, badRequest("reason is required"))
		return
	}
	if len(reason) > 500 {
		writeError(c, badRequest("reason must be at most 500 characters"))
		return
	}
	amount, err := toDecimal(body["amount"])
	if err != nil {
		writeError(c, err)
		return
	}
	if amount == nil || amount.Sign() < 0 {
		writeError(c, badRequest("amount must be zero or positive"))
		return
	}
	order, err := a.DB.FindOrderByID(orderID)
	if err != nil {
		writeError(c, err)
		return
	}
	if order == nil {
		writeError(c, badRequest("Order not found: "+strconv.FormatInt(orderID, 10)))
		return
	}
	raiser, err := a.DB.FindUserByID(raisedByID)
	if err != nil {
		writeError(c, err)
		return
	}
	if raiser == nil {
		writeError(c, badRequest("Raiser user not found: "+strconv.Itoa(raisedByID)))
		return
	}
	pending := "pending"
	r := &RefundRequest{
		Amount:         amount,
		Reason:         &reason,
		RequestType:    &rtype,
		Status:         &pending,
		OrderID:        &orderID,
		RaisedByUserID: &raisedByID,
		CreatedAt:      nullTimeFrom(time.Now()),
	}
	saved, err := a.DB.SaveRefund(r)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toRefundMap(saved))
}

func (a *App) ListRefundRequests(c *gin.Context) {
	statusFilter := strings.TrimSpace(c.Query("status"))
	var refunds []RefundRequest
	var err error
	if statusFilter == "" || strings.EqualFold(statusFilter, "all") {
		refunds, err = a.DB.FindAllRefunds()
	} else {
		s := strings.ToLower(statusFilter)
		if s != "pending" && !allowedRefundDecisions.Has(s) {
			writeError(c, badRequest("status must be 'pending', 'approved', 'rejected' or 'all'"))
			return
		}
		refunds, err = a.DB.FindRefundsByStatus(s)
	}
	if err != nil {
		writeError(c, err)
		return
	}
	out := make([]any, 0, len(refunds))
	for i := range refunds {
		out = append(out, a.toRefundMap(&refunds[i]))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) DecideRefundRequest(c *gin.Context) {
	id, err := pathInt64(c, "id")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	decisionRaw := strings.TrimSpace(asStr(body["decision"]))
	if decisionRaw == "" {
		writeError(c, badRequest("decision is required"))
		return
	}
	decision := strings.ToLower(decisionRaw)
	if !allowedRefundDecisions.Has(decision) {
		writeError(c, badRequest("decision must be one of: "+strings.Join(allowedRefundDecisions.SortedKeys(), ", ")))
		return
	}
	r, err := a.DB.FindRefundByID(id)
	if err != nil {
		writeError(c, err)
		return
	}
	if r == nil {
		writeError(c, badRequest("Refund request not found: "+strconv.FormatInt(id, 10)))
		return
	}
	if r.Status == nil || !strings.EqualFold(*r.Status, "pending") {
		writeError(c, badRequest("Refund request is already "+strOrEmpty(r.Status)+" and cannot be changed"))
		return
	}
	managerID, ok := asInt(body["managerUserId"])
	if !ok {
		writeError(c, badRequest("managerUserId is required"))
		return
	}
	manager, err := a.DB.FindUserByID(managerID)
	if err != nil {
		writeError(c, err)
		return
	}
	if manager == nil {
		writeError(c, badRequest("Manager not found: "+strconv.Itoa(managerID)))
		return
	}
	r.Status = &decision
	managerCp := managerID
	r.DecidedByUserID = &managerCp
	if notes := strings.TrimSpace(asStr(body["notes"])); notes != "" {
		r.DecisionNotes = &notes
	} else {
		r.DecisionNotes = nil
	}
	r.DecidedAt = nullTimeFrom(time.Now())

	if decision == "approved" && r.OrderID != nil {
		order, _ := a.DB.FindOrderByID(*r.OrderID)
		if order != nil {
			rt := ""
			if r.RequestType != nil {
				rt = strings.ToLower(*r.RequestType)
			}
			switch rt {
			case "cancellation":
				st := "cancelled"
				ks := "cancelled"
				order.Status = &st
				order.KitchenStatus = &ks
			case "refund", "damage_writeoff":
				st := "refunded"
				order.Status = &st
			}
			_, _ = a.DB.SaveOrder(order)
		}
	}
	saved, err := a.DB.SaveRefund(r)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toRefundMap(saved))
}

func (a *App) toRefundMap(r *RefundRequest) *Object {
	m := NewObject().Put("id", r.ID)
	if r.OrderID != nil {
		m.Put("orderId", *r.OrderID)
		if o, _ := a.DB.FindOrderByID(*r.OrderID); o != nil {
			m.Put("orderTotal", o.TotalAmount)
			if o.UserID != nil {
				if u, _ := a.DB.FindUserByID(*o.UserID); u != nil {
					m.Put("customerName", strOrEmpty(u.Name))
				} else {
					m.Put("customerName", nil)
				}
			} else {
				m.Put("customerName", nil)
			}
		} else {
			m.Put("orderTotal", nil).Put("customerName", nil)
		}
	} else {
		m.Put("orderId", nil).Put("orderTotal", nil).Put("customerName", nil)
	}
	m.Put("requestType", strOrNil(r.RequestType)).
		Put("reason", strOrNil(r.Reason)).
		Put("amount", r.Amount).
		Put("status", strOrNil(r.Status))
	if r.RaisedByUserID != nil {
		m.Put("raisedById", *r.RaisedByUserID)
		if u, _ := a.DB.FindUserByID(*r.RaisedByUserID); u != nil {
			m.Put("raisedByName", strOrEmpty(u.Name))
		} else {
			m.Put("raisedByName", nil)
		}
	} else {
		m.Put("raisedById", nil).Put("raisedByName", nil)
	}
	if r.DecidedByUserID != nil {
		m.Put("decidedById", *r.DecidedByUserID)
		if u, _ := a.DB.FindUserByID(*r.DecidedByUserID); u != nil {
			m.Put("decidedByName", strOrEmpty(u.Name))
		} else {
			m.Put("decidedByName", nil)
		}
	} else {
		m.Put("decidedById", nil).Put("decidedByName", nil)
	}
	m.Put("decisionNotes", strOrNil(r.DecisionNotes)).
		Put("createdAt", isoOrNil(r.CreatedAt)).
		Put("decidedAt", isoOrNil(r.DecidedAt))
	return m
}

// Discount campaigns -------------------------------------------------------

func (a *App) ProposeDiscountCampaign(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	proposerID, ok := asInt(body["proposedByUserId"])
	if !ok {
		writeError(c, badRequest("proposedByUserId is required"))
		return
	}
	name := strings.TrimSpace(asStr(body["name"]))
	if name == "" {
		writeError(c, badRequest("name is required"))
		return
	}
	if len(name) > 200 {
		writeError(c, badRequest("name must be at most 200 characters"))
		return
	}
	pct, err := toDecimal(body["discountPercent"])
	if err != nil {
		writeError(c, err)
		return
	}
	if pct == nil {
		writeError(c, badRequest("discountPercent is required"))
		return
	}
	if pct.Sign() <= 0 || pct.GreaterThan(decimal.NewFromInt(100)) {
		writeError(c, badRequest("discountPercent must be between 0 and 100"))
		return
	}
	now := time.Now()
	startsOn, err := parseDateOrDefault(asStr(body["startsOn"]), startOfDay(now), "startsOn")
	if err != nil {
		writeError(c, err)
		return
	}
	endsOn, err := parseDateOrDefault(asStr(body["endsOn"]), startsOn, "endsOn")
	if err != nil {
		writeError(c, err)
		return
	}
	if endsOn.Before(startsOn) {
		writeError(c, badRequest("endsOn cannot be before startsOn"))
		return
	}
	proposer, err := a.DB.FindUserByID(proposerID)
	if err != nil {
		writeError(c, err)
		return
	}
	if proposer == nil {
		writeError(c, badRequest("Proposer not found: "+strconv.Itoa(proposerID)))
		return
	}
	categoryFilter := trimToNil(body["categoryFilter"])
	pending := "pending"
	d := &DiscountCampaign{
		Name:             &name,
		CategoryFilter:   categoryFilter,
		DiscountPercent:  pct,
		StartsOn:         &startsOn,
		EndsOn:           &endsOn,
		Status:           &pending,
		ProposedByUserID: &proposerID,
		CreatedAt:        nullTimeFrom(now),
	}
	saved, err := a.DB.SaveCampaign(d)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toCampaignMap(saved))
}

func (a *App) ListDiscountCampaigns(c *gin.Context) {
	statusFilter := strings.TrimSpace(c.Query("status"))
	var campaigns []DiscountCampaign
	var err error
	if statusFilter == "" || strings.EqualFold(statusFilter, "all") {
		campaigns, err = a.DB.FindAllCampaigns()
	} else {
		s := strings.ToLower(statusFilter)
		if s != "pending" && !allowedCampaignDecisions.Has(s) {
			writeError(c, badRequest("status must be 'pending', 'approved', 'rejected' or 'all'"))
			return
		}
		campaigns, err = a.DB.FindCampaignsByStatus(s)
	}
	if err != nil {
		writeError(c, err)
		return
	}
	out := make([]any, 0, len(campaigns))
	for i := range campaigns {
		out = append(out, a.toCampaignMap(&campaigns[i]))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) DecideDiscountCampaign(c *gin.Context) {
	id, err := pathInt64(c, "id")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	decisionRaw := strings.TrimSpace(asStr(body["decision"]))
	if decisionRaw == "" {
		writeError(c, badRequest("decision is required"))
		return
	}
	decision := strings.ToLower(decisionRaw)
	if !allowedCampaignDecisions.Has(decision) {
		writeError(c, badRequest("decision must be one of: "+strings.Join(allowedCampaignDecisions.SortedKeys(), ", ")))
		return
	}
	d, err := a.DB.FindCampaignByID(id)
	if err != nil {
		writeError(c, err)
		return
	}
	if d == nil {
		writeError(c, badRequest("Campaign not found: "+strconv.FormatInt(id, 10)))
		return
	}
	if d.Status == nil || !strings.EqualFold(*d.Status, "pending") {
		writeError(c, badRequest("Campaign is already "+strOrEmpty(d.Status)+" and cannot be changed"))
		return
	}
	managerID, ok := asInt(body["managerUserId"])
	if !ok {
		writeError(c, badRequest("managerUserId is required"))
		return
	}
	manager, err := a.DB.FindUserByID(managerID)
	if err != nil {
		writeError(c, err)
		return
	}
	if manager == nil {
		writeError(c, badRequest("Manager not found: "+strconv.Itoa(managerID)))
		return
	}
	d.Status = &decision
	mc := managerID
	d.DecidedByUserID = &mc
	if notes := strings.TrimSpace(asStr(body["notes"])); notes != "" {
		d.DecisionNotes = &notes
	} else {
		d.DecisionNotes = nil
	}
	d.DecidedAt = nullTimeFrom(time.Now())
	saved, err := a.DB.SaveCampaign(d)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toCampaignMap(saved))
}

func (a *App) toCampaignMap(d *DiscountCampaign) *Object {
	m := NewObject().
		Put("id", d.ID).
		Put("name", strOrNil(d.Name)).
		Put("categoryFilter", strOrNil(d.CategoryFilter)).
		Put("discountPercent", d.DiscountPercent)
	if d.StartsOn != nil {
		m.Put("startsOn", d.StartsOn.Format("2006-01-02"))
	} else {
		m.Put("startsOn", nil)
	}
	if d.EndsOn != nil {
		m.Put("endsOn", d.EndsOn.Format("2006-01-02"))
	} else {
		m.Put("endsOn", nil)
	}
	m.Put("status", strOrNil(d.Status))
	if d.ProposedByUserID != nil {
		m.Put("proposedById", *d.ProposedByUserID)
		if u, _ := a.DB.FindUserByID(*d.ProposedByUserID); u != nil {
			m.Put("proposedByName", strOrEmpty(u.Name))
		} else {
			m.Put("proposedByName", nil)
		}
	} else {
		m.Put("proposedById", nil).Put("proposedByName", nil)
	}
	if d.DecidedByUserID != nil {
		m.Put("decidedById", *d.DecidedByUserID)
		if u, _ := a.DB.FindUserByID(*d.DecidedByUserID); u != nil {
			m.Put("decidedByName", strOrEmpty(u.Name))
		} else {
			m.Put("decidedByName", nil)
		}
	} else {
		m.Put("decidedById", nil).Put("decidedByName", nil)
	}
	m.Put("decisionNotes", strOrNil(d.DecisionNotes)).
		Put("createdAt", isoOrNil(d.CreatedAt)).
		Put("decidedAt", isoOrNil(d.DecidedAt))
	return m
}
