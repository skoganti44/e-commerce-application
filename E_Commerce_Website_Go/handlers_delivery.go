package main

import (
	"math/rand"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
)

var allowedTripStatuses = newStrSet("picked_up", "out_for_delivery", "delivered", "failed")
var terminalTripStatuses = newStrSet("delivered", "failed")
var allowedFailureReasons = newStrSet("customer_not_home", "refused", "damaged", "wrong_address", "other")
var allowedIssueTypes = newStrSet("vehicle_breakdown", "traffic_delay", "accident", "other")

func (a *App) PickUpTrip(c *gin.Context) {
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
	driverID, ok := asInt(body["driverId"])
	if !ok {
		writeError(c, badRequest("driverId is required"))
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
	channel := ""
	if order.Channel != nil {
		channel = strings.ToLower(*order.Channel)
	}
	if channel != "online" {
		writeError(c, badRequest("Only online orders can be delivered"))
		return
	}
	kitchen := ""
	if order.KitchenStatus != nil {
		kitchen = strings.ToLower(*order.KitchenStatus)
	}
	if kitchen != "done" {
		writeError(c, badRequest("Order is not ready for pickup (kitchen status must be 'done')"))
		return
	}
	existing, err := a.DB.FindTripByOrderID(orderID)
	if err != nil {
		writeError(c, err)
		return
	}
	if existing != nil {
		s := ""
		if existing.Status != nil {
			s = strings.ToLower(*existing.Status)
		}
		if s != "failed" {
			writeError(c, badRequest("A trip already exists for this order (status: "+s+")"))
			return
		}
	}
	driver, err := a.DB.FindUserByID(driverID)
	if err != nil {
		writeError(c, err)
		return
	}
	if driver == nil {
		writeError(c, badRequest("Driver not found: "+strconv.Itoa(driverID)))
		return
	}
	now := time.Now()
	status := "picked_up"
	driverIDcp := driverID
	oid := orderID
	trip := &DeliveryTrip{
		OrderID:      &oid,
		DriverUserID: &driverIDcp,
		Status:       &status,
		PickedUpAt:   nullTimeFrom(now),
		CreatedAt:    nullTimeFrom(now),
		UpdatedAt:    nullTimeFrom(now),
	}
	pickedUp := "picked_up"
	order.KitchenStatus = &pickedUp
	if _, err := a.DB.SaveOrder(order); err != nil {
		writeError(c, err)
		return
	}
	saved, err := a.DB.SaveTrip(trip)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toTripMap(saved))
}

func (a *App) MarkOutForDelivery(c *gin.Context) {
	tripID, err := pathInt64(c, "tripId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	driverID, ok := asInt(body["driverId"])
	if !ok {
		writeError(c, badRequest("driverId is required"))
		return
	}
	trip, err := a.loadDriverTrip(tripID, driverID)
	if err != nil {
		writeError(c, err)
		return
	}
	s := ""
	if trip.Status != nil {
		s = strings.ToLower(*trip.Status)
	}
	if s != "picked_up" {
		writeError(c, badRequest("Trip must be in 'picked_up' state to go out for delivery (was: "+s+")"))
		return
	}
	now := time.Now()
	out := "out_for_delivery"
	trip.Status = &out
	trip.OutAt = nullTimeFrom(now)
	trip.UpdatedAt = nullTimeFrom(now)
	if trip.OtpCode == nil || strings.TrimSpace(*trip.OtpCode) == "" {
		otp := generateOtp()
		trip.OtpCode = &otp
	}
	if trip.OrderID != nil {
		if order, _ := a.DB.FindOrderByID(*trip.OrderID); order != nil {
			ks := "out_for_delivery"
			order.KitchenStatus = &ks
			_, _ = a.DB.SaveOrder(order)
		}
	}
	saved, err := a.DB.SaveTrip(trip)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toTripMap(saved))
}

func (a *App) MarkDelivered(c *gin.Context) {
	tripID, err := pathInt64(c, "tripId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	driverID, ok := asInt(body["driverId"])
	if !ok {
		writeError(c, badRequest("driverId is required"))
		return
	}
	trip, err := a.loadDriverTrip(tripID, driverID)
	if err != nil {
		writeError(c, err)
		return
	}
	s := ""
	if trip.Status != nil {
		s = strings.ToLower(*trip.Status)
	}
	if s != "out_for_delivery" {
		writeError(c, badRequest("Trip must be 'out_for_delivery' to deliver (was: "+s+")"))
		return
	}
	otp := strings.TrimSpace(asStr(body["otp"]))
	photo := strings.TrimSpace(asStr(body["photoUrl"]))
	hasPhoto := photo != ""
	hasOtp := otp != ""
	if !hasPhoto && !hasOtp {
		writeError(c, badRequest("Proof of delivery is required: enter the customer OTP or upload a photo"))
		return
	}
	if hasOtp && trip.OtpCode != nil && strings.TrimSpace(*trip.OtpCode) != "" && *trip.OtpCode != otp {
		writeError(c, badRequest("OTP does not match"))
		return
	}
	cod, err := toDecimal(body["codAmount"])
	if err != nil {
		writeError(c, err)
		return
	}
	tip, err := toDecimal(body["tipAmount"])
	if err != nil {
		writeError(c, err)
		return
	}
	dist, err := toDecimal(body["distanceKm"])
	if err != nil {
		writeError(c, err)
		return
	}
	if cod != nil && cod.Sign() < 0 {
		writeError(c, badRequest("COD amount cannot be negative"))
		return
	}
	if tip != nil && tip.Sign() < 0 {
		writeError(c, badRequest("Tip cannot be negative"))
		return
	}
	if dist != nil && dist.Sign() < 0 {
		writeError(c, badRequest("Distance cannot be negative"))
		return
	}

	now := time.Now()
	delivered := "delivered"
	trip.Status = &delivered
	trip.DeliveredAt = nullTimeFrom(now)
	trip.UpdatedAt = nullTimeFrom(now)
	if hasPhoto {
		trip.PhotoProofURL = &photo
	}
	if cod != nil && cod.Sign() > 0 {
		trip.CodAmount = cod
		trip.CodCollectedAt = nullTimeFrom(now)
	}
	if tip != nil {
		trip.TipAmount = tip
	}
	if dist != nil {
		trip.DistanceKm = dist
	}
	if notes := strings.TrimSpace(asStr(body["notes"])); notes != "" {
		trip.Notes = &notes
	}
	if trip.OrderID != nil {
		if order, _ := a.DB.FindOrderByID(*trip.OrderID); order != nil {
			ks := "delivered"
			st := "delivered"
			order.KitchenStatus = &ks
			order.Status = &st
			_, _ = a.DB.SaveOrder(order)
		}
	}
	saved, err := a.DB.SaveTrip(trip)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toTripMap(saved))
}

func (a *App) MarkTripFailed(c *gin.Context) {
	tripID, err := pathInt64(c, "tripId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	driverID, ok := asInt(body["driverId"])
	if !ok {
		writeError(c, badRequest("driverId is required"))
		return
	}
	reasonRaw := strings.TrimSpace(asStr(body["reason"]))
	if reasonRaw == "" {
		writeError(c, badRequest("Failure reason is required"))
		return
	}
	reason := strings.ToLower(reasonRaw)
	if !allowedFailureReasons.Has(reason) {
		writeError(c, badRequest("Failure reason must be one of: "+strings.Join(allowedFailureReasons.SortedKeys(), ", ")))
		return
	}
	trip, err := a.loadDriverTrip(tripID, driverID)
	if err != nil {
		writeError(c, err)
		return
	}
	s := ""
	if trip.Status != nil {
		s = strings.ToLower(*trip.Status)
	}
	if terminalTripStatuses.Has(s) {
		writeError(c, badRequest("Trip is already "+s+" and cannot be failed"))
		return
	}
	now := time.Now()
	failed := "failed"
	trip.Status = &failed
	trip.FailedAt = nullTimeFrom(now)
	trip.UpdatedAt = nullTimeFrom(now)
	trip.FailureReason = &reason
	if notes := strings.TrimSpace(asStr(body["notes"])); notes != "" {
		trip.Notes = &notes
	}
	if trip.OrderID != nil {
		if order, _ := a.DB.FindOrderByID(*trip.OrderID); order != nil {
			ks := "delivery_failed"
			order.KitchenStatus = &ks
			_, _ = a.DB.SaveOrder(order)
		}
	}
	saved, err := a.DB.SaveTrip(trip)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toTripMap(saved))
}

func (a *App) ListTrips(c *gin.Context) {
	driverID, ok := queryInt(c, "driverId")
	if !ok {
		writeError(c, badRequest("driverId is required"))
		return
	}
	statusFilter := strings.TrimSpace(c.Query("status"))
	if statusFilter != "" {
		s := strings.ToLower(statusFilter)
		if s != "active" && !allowedTripStatuses.Has(s) {
			writeError(c, badRequest("status must be 'active' or one of: "+strings.Join(allowedTripStatuses.SortedKeys(), ", ")))
			return
		}
		if s == "active" {
			trips, err := a.DB.FindActiveTripsByDriver(driverID)
			if err != nil {
				writeError(c, err)
				return
			}
			c.JSON(http.StatusOK, a.toTripList(trips))
			return
		}
		trips, err := a.DB.FindTripsByDriver(driverID)
		if err != nil {
			writeError(c, err)
			return
		}
		filtered := []DeliveryTrip{}
		for _, t := range trips {
			st := ""
			if t.Status != nil {
				st = strings.ToLower(*t.Status)
			}
			if st == s {
				filtered = append(filtered, t)
			}
		}
		c.JSON(http.StatusOK, a.toTripList(filtered))
		return
	}
	trips, err := a.DB.FindTripsByDriver(driverID)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toTripList(trips))
}

func (a *App) toTripList(trips []DeliveryTrip) []any {
	out := make([]any, 0, len(trips))
	for i := range trips {
		out = append(out, a.toTripMap(&trips[i]))
	}
	return out
}

func (a *App) LogIssue(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	driverID, ok := asInt(body["driverId"])
	if !ok {
		writeError(c, badRequest("driverId is required"))
		return
	}
	issueRaw := strings.TrimSpace(asStr(body["issueType"]))
	if issueRaw == "" {
		writeError(c, badRequest("Issue type is required"))
		return
	}
	itype := strings.ToLower(issueRaw)
	if !allowedIssueTypes.Has(itype) {
		writeError(c, badRequest("Issue type must be one of: "+strings.Join(allowedIssueTypes.SortedKeys(), ", ")))
		return
	}
	desc := strings.TrimSpace(asStr(body["description"]))
	if desc == "" {
		writeError(c, badRequest("Description is required"))
		return
	}
	if len(desc) > 500 {
		writeError(c, badRequest("Description must be at most 500 characters"))
		return
	}
	driver, err := a.DB.FindUserByID(driverID)
	if err != nil {
		writeError(c, err)
		return
	}
	if driver == nil {
		writeError(c, badRequest("Driver not found: "+strconv.Itoa(driverID)))
		return
	}
	driverCopy := driverID
	issue := &DeliveryIssue{
		DriverUserID: &driverCopy,
		IssueType:    &itype,
		Description:  &desc,
		ReportedAt:   nullTimeFrom(time.Now()),
	}
	if v, ok := asInt64(body["tripId"]); ok {
		t, err := a.DB.FindTripByID(v)
		if err != nil {
			writeError(c, err)
			return
		}
		if t == nil {
			writeError(c, badRequest("Trip not found: "+strconv.FormatInt(v, 10)))
			return
		}
		issue.TripID = &v
	}
	saved, err := a.DB.SaveIssue(issue)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, a.toIssueMap(saved))
}

func (a *App) ListIssues(c *gin.Context) {
	driverID, ok := queryInt(c, "driverId")
	if !ok {
		writeError(c, badRequest("driverId is required"))
		return
	}
	issues, err := a.DB.FindIssuesByDriver(driverID)
	if err != nil {
		writeError(c, err)
		return
	}
	out := make([]any, 0, len(issues))
	for i := range issues {
		out = append(out, a.toIssueMap(&issues[i]))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) ShiftSummary(c *gin.Context) {
	driverID, ok := queryInt(c, "driverId")
	if !ok {
		writeError(c, badRequest("driverId is required"))
		return
	}
	from, to, err := parseDateRange(c.Query("from"), c.Query("to"))
	if err != nil {
		writeError(c, err)
		return
	}
	fromTs := startOfDay(from)
	toTs := startOfDay(to.Add(24 * time.Hour))
	trips, err := a.DB.FindTripsByDriverInRange(driverID, fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	var delivered, failed int
	cod := decimal.Zero
	tips := decimal.Zero
	dist := decimal.Zero
	failuresByReason := map[string]int{}
	for _, t := range trips {
		st := ""
		if t.Status != nil {
			st = strings.ToLower(*t.Status)
		}
		switch st {
		case "delivered":
			delivered++
			if t.CodAmount != nil {
				cod = cod.Add(*t.CodAmount)
			}
		case "failed":
			failed++
			if t.FailureReason != nil {
				failuresByReason[*t.FailureReason]++
			}
		}
		if t.TipAmount != nil {
			tips = tips.Add(*t.TipAmount)
		}
		if t.DistanceKm != nil {
			dist = dist.Add(*t.DistanceKm)
		}
	}
	inFlight := len(trips) - delivered - failed
	failMap := NewObject()
	for k, v := range failuresByReason {
		failMap.Put(k, v)
	}
	c.JSON(http.StatusOK, NewObject().
		Put("driverId", driverID).
		Put("from", dateOnly(from)).
		Put("to", dateOnly(to)).
		Put("totalTrips", len(trips)).
		Put("delivered", delivered).
		Put("failed", failed).
		Put("inFlight", inFlight).
		Put("codCollected", cod).
		Put("tipsTotal", tips).
		Put("distanceKm", dist).
		Put("failuresByReason", failMap))
}

func (a *App) loadDriverTrip(tripID int64, driverID int) (*DeliveryTrip, error) {
	trip, err := a.DB.FindTripByID(tripID)
	if err != nil {
		return nil, err
	}
	if trip == nil {
		return nil, badRequest("Trip not found: " + strconv.FormatInt(tripID, 10))
	}
	if trip.DriverUserID == nil || *trip.DriverUserID != driverID {
		return nil, badRequest("This trip is not assigned to you")
	}
	return trip, nil
}

var rng = rand.New(rand.NewSource(time.Now().UnixNano()))

func generateOtp() string {
	n := rng.Intn(9000) + 1000
	return strconv.Itoa(n)
}

func (a *App) toTripMap(t *DeliveryTrip) *Object {
	m := NewObject().
		Put("id", t.ID).
		Put("status", strOrNil(t.Status))
	var orderID any
	var orderTotal any
	var customerName any
	if t.OrderID != nil {
		orderID = *t.OrderID
		if o, _ := a.DB.FindOrderByID(*t.OrderID); o != nil {
			orderTotal = o.TotalAmount
			if o.UserID != nil {
				if u, _ := a.DB.FindUserByID(*o.UserID); u != nil {
					customerName = strOrEmpty(u.Name)
				}
			}
		}
	}
	m.Put("orderId", orderID).
		Put("orderTotal", orderTotal).
		Put("customerName", customerName)
	if t.OrderID != nil {
		if sa, _ := a.DB.FindShippingAddressByOrderID(*t.OrderID); sa != nil {
			var b strings.Builder
			if sa.Line1 != nil {
				b.WriteString(*sa.Line1)
			}
			if sa.Line2 != nil && strings.TrimSpace(*sa.Line2) != "" {
				b.WriteString(", ")
				b.WriteString(*sa.Line2)
			}
			if sa.City != nil {
				b.WriteString(", ")
				b.WriteString(*sa.City)
			}
			if sa.Pincode != nil {
				b.WriteString(" ")
				b.WriteString(*sa.Pincode)
			}
			m.Put("shippingAddress", b.String())
			m.Put("customerPhone", strOrNil(sa.Phone))
		}
	}
	m.Put("driverId", t.DriverUserID)
	if t.DriverUserID != nil {
		if u, _ := a.DB.FindUserByID(*t.DriverUserID); u != nil {
			m.Put("driverName", strOrEmpty(u.Name))
		} else {
			m.Put("driverName", nil)
		}
	} else {
		m.Put("driverName", nil)
	}
	m.Put("otpCode", strOrNil(t.OtpCode)).
		Put("photoProofUrl", strOrNil(t.PhotoProofURL)).
		Put("codAmount", t.CodAmount).
		Put("codCollectedAt", isoOrNil(t.CodCollectedAt)).
		Put("tipAmount", t.TipAmount).
		Put("distanceKm", t.DistanceKm).
		Put("failureReason", strOrNil(t.FailureReason)).
		Put("notes", strOrNil(t.Notes)).
		Put("pickedUpAt", isoOrNil(t.PickedUpAt)).
		Put("outAt", isoOrNil(t.OutAt)).
		Put("deliveredAt", isoOrNil(t.DeliveredAt)).
		Put("failedAt", isoOrNil(t.FailedAt)).
		Put("createdAt", isoOrNil(t.CreatedAt)).
		Put("updatedAt", isoOrNil(t.UpdatedAt))
	return m
}

func (a *App) toIssueMap(i *DeliveryIssue) *Object {
	m := NewObject().Put("id", i.ID)
	if i.DriverUserID != nil {
		m.Put("driverId", *i.DriverUserID)
		if u, _ := a.DB.FindUserByID(*i.DriverUserID); u != nil {
			m.Put("driverName", strOrEmpty(u.Name))
		} else {
			m.Put("driverName", nil)
		}
	} else {
		m.Put("driverId", nil).Put("driverName", nil)
	}
	if i.TripID != nil {
		m.Put("tripId", *i.TripID)
	} else {
		m.Put("tripId", nil)
	}
	m.Put("issueType", strOrNil(i.IssueType)).
		Put("description", strOrNil(i.Description)).
		Put("reportedAt", isoOrNil(i.ReportedAt)).
		Put("resolvedAt", isoOrNil(i.ResolvedAt))
	return m
}
