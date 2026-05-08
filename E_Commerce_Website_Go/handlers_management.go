package main

import (
	"net/http"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
)

const kitchenSlaMinutes = 30
const deliverySlaMinutes = 60

func (a *App) ManagementOps(c *gin.Context) {
	pipeline, err := a.DB.FindOrdersInPipeline()
	if err != nil {
		writeError(c, err)
		return
	}
	kitchenByChannel := NewObject().
		Put("online", emptyKitchenCounts()).
		Put("instore", emptyKitchenCounts())

	breaches := []any{}
	now := time.Now()

	mergeKitchen := func(channel, status string) {
		obj, _ := kitchenByChannel.Get(channel).(*Object)
		if obj == nil {
			obj = emptyKitchenCounts()
			kitchenByChannel.Put(channel, obj)
		}
		cur, _ := obj.Get(status).(int64)
		obj.Put(status, cur+1)
	}

	for _, o := range pipeline {
		channel := "online"
		if o.Channel != nil {
			channel = strings.ToLower(*o.Channel)
		}
		ks := "pending"
		if o.KitchenStatus != nil {
			ks = strings.ToLower(*o.KitchenStatus)
		}
		mergeKitchen(channel, ks)

		if o.CreatedAt.Valid && isKitchenSide(ks) {
			mins := int64(now.Sub(o.CreatedAt.Time).Minutes())
			if mins >= kitchenSlaMinutes {
				b := NewObject().
					Put("type", "kitchen").
					Put("orderId", o.ID).
					Put("status", ks).
					Put("channel", channel).
					Put("ageMinutes", mins)
				if o.UserID != nil {
					if u, _ := a.DB.FindUserByID(*o.UserID); u != nil {
						b.Put("customerName", strOrEmpty(u.Name))
					} else {
						b.Put("customerName", nil)
					}
				} else {
					b.Put("customerName", nil)
				}
				breaches = append(breaches, b)
			}
		}
	}

	trips, err := a.DB.FindAllTrips()
	if err != nil {
		writeError(c, err)
		return
	}
	var pickedUp, outForDelivery int64
	for _, t := range trips {
		st := ""
		if t.Status != nil {
			st = strings.ToLower(*t.Status)
		}
		switch st {
		case "picked_up":
			pickedUp++
		case "out_for_delivery":
			outForDelivery++
			var since time.Time
			if t.OutAt.Valid {
				since = t.OutAt.Time
			} else if t.CreatedAt.Valid {
				since = t.CreatedAt.Time
			} else {
				continue
			}
			mins := int64(now.Sub(since).Minutes())
			if mins >= deliverySlaMinutes {
				b := NewObject().
					Put("type", "delivery").
					Put("tripId", t.ID)
				if t.OrderID != nil {
					b.Put("orderId", *t.OrderID)
				} else {
					b.Put("orderId", nil)
				}
				b.Put("status", st).Put("ageMinutes", mins)
				if t.DriverUserID != nil {
					if u, _ := a.DB.FindUserByID(*t.DriverUserID); u != nil {
						b.Put("driverName", strOrEmpty(u.Name))
					} else {
						b.Put("driverName", nil)
					}
				} else {
					b.Put("driverName", nil)
				}
				breaches = append(breaches, b)
			}
		}
	}

	dlv := NewObject().
		Put("pickedUp", pickedUp).
		Put("outForDelivery", outForDelivery).
		Put("total", pickedUp+outForDelivery)

	c.JSON(http.StatusOK, NewObject().
		Put("kitchenQueue", kitchenByChannel).
		Put("deliveryInFlight", dlv).
		Put("breaches", breaches).
		Put("kitchenSlaMinutes", kitchenSlaMinutes).
		Put("deliverySlaMinutes", deliverySlaMinutes).
		Put("asOf", now.Format("2006-01-02T15:04:05.000000")))
}

func emptyKitchenCounts() *Object {
	o := NewObject()
	for _, s := range []string{"pending", "preparing", "ready", "done", "picked_up", "out_for_delivery"} {
		o.Put(s, int64(0))
	}
	return o
}

func isKitchenSide(s string) bool {
	if s == "" {
		return true
	}
	for _, k := range []string{"pending", "preparing", "ready", "done"} {
		if k == s {
			return true
		}
	}
	return false
}

func (a *App) ManagementOrdersAudit(c *gin.Context) {
	from, to, err := parseDateRange(c.Query("from"), c.Query("to"))
	if err != nil {
		writeError(c, err)
		return
	}
	fromTs := startOfDay(from)
	toTs := startOfDay(to.Add(24 * time.Hour))
	orders, err := a.DB.FindOrdersInRange(fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	channelFilter := strings.TrimSpace(c.Query("channel"))
	if channelFilter != "" && !strings.EqualFold(channelFilter, "all") {
		f := strings.ToLower(channelFilter)
		filtered := []Order{}
		for _, o := range orders {
			if o.Channel != nil && strings.EqualFold(*o.Channel, f) {
				filtered = append(filtered, o)
			}
		}
		orders = filtered
	}
	ids := make([]int64, 0, len(orders))
	for _, o := range orders {
		ids = append(ids, o.ID)
	}
	payments, err := a.DB.FindPaymentsByOrderIDs(ids)
	if err != nil {
		writeError(c, err)
		return
	}
	paymentByOrder := map[int64]Payment{}
	for _, p := range payments {
		if p.OrderID != nil {
			paymentByOrder[*p.OrderID] = p
		}
	}

	paymentFilter := strings.TrimSpace(c.Query("paymentMethod"))
	if paymentFilter != "" && !strings.EqualFold(paymentFilter, "all") {
		pm := strings.ToLower(paymentFilter)
		filtered := []Order{}
		for _, o := range orders {
			method := ""
			if p, ok := paymentByOrder[o.ID]; ok && p.PaymentMethod != nil {
				method = strings.ToLower(*p.PaymentMethod)
			}
			if pm == method {
				filtered = append(filtered, o)
			}
		}
		orders = filtered
	}

	totalRevenue := decimal.Zero
	byChannel := NewObject()
	byPaymentMethod := NewObject()
	countByPaymentMethod := NewObject()
	rows := []any{}

	mergeDec := func(o *Object, k string, amt decimal.Decimal) {
		cur, _ := o.Get(k).(decimal.Decimal)
		o.Put(k, cur.Add(amt))
	}
	mergeCount := func(o *Object, k string) {
		cur, _ := o.Get(k).(int64)
		o.Put(k, cur+1)
	}
	for _, o := range orders {
		amt := decOrZero(o.TotalAmount)
		totalRevenue = totalRevenue.Add(amt)
		channel := "unknown"
		if o.Channel != nil {
			channel = strings.ToLower(*o.Channel)
		}
		mergeDec(byChannel, channel, amt)

		method := "unknown"
		var p *Payment
		if pp, ok := paymentByOrder[o.ID]; ok {
			p = &pp
			if pp.PaymentMethod != nil {
				method = strings.ToLower(*pp.PaymentMethod)
			}
		}
		mergeDec(byPaymentMethod, method, amt)
		mergeCount(countByPaymentMethod, method)

		row := NewObject().Put("orderId", o.ID)
		var customerName any
		if o.UserID != nil {
			if u, _ := a.DB.FindUserByID(*o.UserID); u != nil {
				customerName = strOrEmpty(u.Name)
			}
		}
		row.Put("customerName", customerName).
			Put("channel", channel).
			Put("status", strOrNil(o.Status)).
			Put("kitchenStatus", strOrNil(o.KitchenStatus)).
			Put("totalAmount", amt).
			Put("paymentMethod", method)
		if p != nil {
			row.Put("paymentStatus", strOrNil(p.Status))
		} else {
			row.Put("paymentStatus", nil)
		}
		row.Put("createdAt", isoOrNil(o.CreatedAt))
		rows = append(rows, row)
	}

	c.JSON(http.StatusOK, NewObject().
		Put("from", dateOnly(from)).
		Put("to", dateOnly(to)).
		Put("count", len(orders)).
		Put("totalRevenue", totalRevenue).
		Put("revenueByChannel", byChannel).
		Put("revenueByPaymentMethod", byPaymentMethod).
		Put("countByPaymentMethod", countByPaymentMethod).
		Put("orders", rows))
}

func (a *App) ManagementDeliveriesAudit(c *gin.Context) {
	from, to, err := parseDateRange(c.Query("from"), c.Query("to"))
	if err != nil {
		writeError(c, err)
		return
	}
	fromTs := startOfDay(from)
	toTs := startOfDay(to.Add(24 * time.Hour))
	trips, err := a.DB.FindTripsInRange(fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	if v, ok := queryInt(c, "driverId"); ok {
		filtered := []DeliveryTrip{}
		for _, t := range trips {
			if t.DriverUserID != nil && *t.DriverUserID == v {
				filtered = append(filtered, t)
			}
		}
		trips = filtered
	}
	statusFilter := strings.TrimSpace(c.Query("status"))
	if statusFilter != "" && !strings.EqualFold(statusFilter, "all") {
		s := strings.ToLower(statusFilter)
		filtered := []DeliveryTrip{}
		for _, t := range trips {
			if t.Status != nil && strings.EqualFold(*t.Status, s) {
				filtered = append(filtered, t)
			}
		}
		trips = filtered
	}

	var delivered, failed int
	cod := decimal.Zero
	tips := decimal.Zero
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
		}
		if t.TipAmount != nil {
			tips = tips.Add(*t.TipAmount)
		}
	}
	inFlight := len(trips) - delivered - failed
	c.JSON(http.StatusOK, NewObject().
		Put("from", dateOnly(from)).
		Put("to", dateOnly(to)).
		Put("count", len(trips)).
		Put("delivered", delivered).
		Put("failed", failed).
		Put("inFlight", inFlight).
		Put("codCollected", cod).
		Put("tipsTotal", tips).
		Put("trips", a.toTripList(trips)))
}

func (a *App) ManagementDayPnl(c *gin.Context) {
	day, err := parseDateOrDefault(c.Query("date"), startOfDay(time.Now()), "date")
	if err != nil {
		writeError(c, err)
		return
	}
	fromTs := startOfDay(day)
	toTs := startOfDay(day.Add(24 * time.Hour))
	orders, err := a.DB.FindOrdersInRange(fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	ids := make([]int64, 0, len(orders))
	for _, o := range orders {
		ids = append(ids, o.ID)
	}
	payments, err := a.DB.FindPaymentsByOrderIDs(ids)
	if err != nil {
		writeError(c, err)
		return
	}
	payByOrder := map[int64]Payment{}
	for _, p := range payments {
		if p.OrderID != nil {
			payByOrder[*p.OrderID] = p
		}
	}
	onlineRevenue := decimal.Zero
	counterRevenue := decimal.Zero
	cashRevenue := decimal.Zero
	cardRevenue := decimal.Zero
	upiRevenue := decimal.Zero
	var onlineCount, counterCount int64
	for _, o := range orders {
		amt := decOrZero(o.TotalAmount)
		channel := ""
		if o.Channel != nil {
			channel = strings.ToLower(*o.Channel)
		}
		switch channel {
		case "online":
			onlineRevenue = onlineRevenue.Add(amt)
			onlineCount++
		case "instore", "pos":
			counterRevenue = counterRevenue.Add(amt)
			counterCount++
		}
		method := ""
		if p, ok := payByOrder[o.ID]; ok && p.PaymentMethod != nil {
			method = strings.ToLower(*p.PaymentMethod)
		}
		switch method {
		case "cash":
			cashRevenue = cashRevenue.Add(amt)
		case "card":
			cardRevenue = cardRevenue.Add(amt)
		case "upi":
			upiRevenue = upiRevenue.Add(amt)
		}
	}
	trips, err := a.DB.FindTripsInRange(fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	cod := decimal.Zero
	tips := decimal.Zero
	for _, t := range trips {
		if t.Status != nil && strings.EqualFold(*t.Status, "delivered") && t.CodAmount != nil {
			cod = cod.Add(*t.CodAmount)
		}
		if t.TipAmount != nil {
			tips = tips.Add(*t.TipAmount)
		}
	}
	totalRevenue := onlineRevenue.Add(counterRevenue)
	grossInflow := totalRevenue.Add(tips)

	byPm := NewObject().
		Put("cash", cashRevenue).
		Put("card", cardRevenue).
		Put("upi", upiRevenue)

	c.JSON(http.StatusOK, NewObject().
		Put("date", dateOnly(day)).
		Put("orderCount", onlineCount+counterCount).
		Put("onlineOrderCount", onlineCount).
		Put("counterOrderCount", counterCount).
		Put("onlineRevenue", onlineRevenue).
		Put("counterRevenue", counterRevenue).
		Put("totalRevenue", totalRevenue).
		Put("revenueByPaymentMethod", byPm).
		Put("codCollected", cod).
		Put("tipsCollected", tips).
		Put("grossInflow", grossInflow).
		Put("supplierSpend", decimal.Zero).
		Put("refunds", decimal.Zero).
		Put("net", grossInflow))
}

func (a *App) ManagementStaffPerformance(c *gin.Context) {
	from, to, err := parseDateRange(c.Query("from"), c.Query("to"))
	if err != nil {
		writeError(c, err)
		return
	}
	fromTs := startOfDay(from)
	toTs := startOfDay(to.Add(24 * time.Hour))

	trips, err := a.DB.FindTripsInRange(fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	driverStats := NewObject()
	for _, t := range trips {
		if t.DriverUserID == nil {
			continue
		}
		key := strconvI(*t.DriverUserID)
		row, _ := driverStats.Get(key).(*Object)
		if row == nil {
			name := ""
			if u, _ := a.DB.FindUserByID(*t.DriverUserID); u != nil {
				name = strOrEmpty(u.Name)
			}
			row = NewObject().
				Put("userId", *t.DriverUserID).
				Put("name", name).
				Put("trips", int64(0)).
				Put("delivered", int64(0)).
				Put("failed", int64(0)).
				Put("cod", decimal.Zero).
				Put("tips", decimal.Zero).
				Put("distanceKm", decimal.Zero)
			driverStats.Put(key, row)
		}
		row.Put("trips", row.Get("trips").(int64)+1)
		s := ""
		if t.Status != nil {
			s = strings.ToLower(*t.Status)
		}
		switch s {
		case "delivered":
			row.Put("delivered", row.Get("delivered").(int64)+1)
		case "failed":
			row.Put("failed", row.Get("failed").(int64)+1)
		}
		if t.CodAmount != nil && s == "delivered" {
			row.Put("cod", row.Get("cod").(decimal.Decimal).Add(*t.CodAmount))
		}
		if t.TipAmount != nil {
			row.Put("tips", row.Get("tips").(decimal.Decimal).Add(*t.TipAmount))
		}
		if t.DistanceKm != nil {
			row.Put("distanceKm", row.Get("distanceKm").(decimal.Decimal).Add(*t.DistanceKm))
		}
	}

	doneTasks, err := a.DB.FindTasksCompletedInRange(fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	staffByDept := NewObject()
	for _, t := range doneTasks {
		if t.CompletedByUserID == nil {
			continue
		}
		dept := "unknown"
		if t.AssignedDepartment != nil {
			dept = strings.ToLower(*t.AssignedDepartment)
		}
		uid := *t.CompletedByUserID
		deptObj, _ := staffByDept.Get(dept).(*Object)
		if deptObj == nil {
			deptObj = NewObject()
			staffByDept.Put(dept, deptObj)
		}
		key := strconvI(uid)
		row, _ := deptObj.Get(key).(*Object)
		if row == nil {
			name := ""
			if u, _ := a.DB.FindUserByID(uid); u != nil {
				name = strOrEmpty(u.Name)
			}
			row = NewObject().
				Put("userId", uid).
				Put("name", name).
				Put("tasksCompleted", int64(0))
			deptObj.Put(key, row)
		}
		row.Put("tasksCompleted", row.Get("tasksCompleted").(int64)+1)
	}

	createdTasks, err := a.DB.FindTasksCreatedInRange(fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	salesStats := NewObject()
	for _, t := range createdTasks {
		if t.CreatedByUserID == nil {
			continue
		}
		uid := *t.CreatedByUserID
		key := strconvI(uid)
		row, _ := salesStats.Get(key).(*Object)
		if row == nil {
			name := ""
			if u, _ := a.DB.FindUserByID(uid); u != nil {
				name = strOrEmpty(u.Name)
			}
			row = NewObject().
				Put("userId", uid).
				Put("name", name).
				Put("tasksCreated", int64(0))
			salesStats.Put(key, row)
		}
		row.Put("tasksCreated", row.Get("tasksCreated").(int64)+1)
	}

	deptOut := NewObject()
	deptKeys := staffByDept.keys
	for _, dept := range deptKeys {
		obj, _ := staffByDept.Get(dept).(*Object)
		list := []any{}
		for _, k := range obj.keys {
			list = append(list, obj.Get(k))
		}
		deptOut.Put(dept, list)
	}
	driversList := []any{}
	for _, k := range driverStats.keys {
		driversList = append(driversList, driverStats.Get(k))
	}
	salesList := []any{}
	for _, k := range salesStats.keys {
		salesList = append(salesList, salesStats.Get(k))
	}

	c.JSON(http.StatusOK, NewObject().
		Put("from", dateOnly(from)).
		Put("to", dateOnly(to)).
		Put("drivers", driversList).
		Put("staffByDepartment", deptOut).
		Put("salesActivity", salesList))
}

func (a *App) CashReconciliation(c *gin.Context) {
	day, err := parseDateOrDefault(c.Query("date"), startOfDay(time.Now()), "date")
	if err != nil {
		writeError(c, err)
		return
	}
	openingFloat, err := toDecimal(c.Query("openingFloat"))
	if err != nil {
		writeError(c, err)
		return
	}
	if openingFloat != nil && openingFloat.Sign() < 0 {
		writeError(c, badRequest("openingFloat cannot be negative"))
		return
	}
	countedCash, err := toDecimal(c.Query("countedCash"))
	if err != nil {
		writeError(c, err)
		return
	}
	if countedCash != nil && countedCash.Sign() < 0 {
		writeError(c, badRequest("countedCash cannot be negative"))
		return
	}
	fromTs := startOfDay(day)
	toTs := startOfDay(day.Add(24 * time.Hour))
	orders, err := a.DB.FindOrdersInRange(fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	ids := make([]int64, 0, len(orders))
	for _, o := range orders {
		ids = append(ids, o.ID)
	}
	payments, err := a.DB.FindPaymentsByOrderIDs(ids)
	if err != nil {
		writeError(c, err)
		return
	}
	payByOrder := map[int64]Payment{}
	for _, p := range payments {
		if p.OrderID != nil {
			payByOrder[*p.OrderID] = p
		}
	}
	counterCash := decimal.Zero
	counterCard := decimal.Zero
	counterUpi := decimal.Zero
	var counterCashCount int64
	for _, o := range orders {
		channel := ""
		if o.Channel != nil {
			channel = strings.ToLower(*o.Channel)
		}
		if channel != "instore" && channel != "pos" {
			continue
		}
		method := ""
		if p, ok := payByOrder[o.ID]; ok && p.PaymentMethod != nil {
			method = strings.ToLower(*p.PaymentMethod)
		}
		amt := decOrZero(o.TotalAmount)
		switch method {
		case "cash":
			counterCash = counterCash.Add(amt)
			counterCashCount++
		case "card":
			counterCard = counterCard.Add(amt)
		case "upi":
			counterUpi = counterUpi.Add(amt)
		}
	}
	trips, err := a.DB.FindTripsInRange(fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	cod := decimal.Zero
	for _, t := range trips {
		if t.Status != nil && strings.EqualFold(*t.Status, "delivered") && t.CodAmount != nil {
			cod = cod.Add(*t.CodAmount)
		}
	}
	opening := decimal.Zero
	if openingFloat != nil {
		opening = *openingFloat
	}
	expected := opening.Add(counterCash).Add(cod)
	var variance any
	balanced := false
	if countedCash != nil {
		v := countedCash.Sub(expected)
		variance = v
		balanced = v.Sign() == 0
	}
	c.JSON(http.StatusOK, NewObject().
		Put("date", dateOnly(day)).
		Put("openingFloat", opening).
		Put("counterCash", counterCash).
		Put("counterCashCount", counterCashCount).
		Put("counterCard", counterCard).
		Put("counterUpi", counterUpi).
		Put("codCollected", cod).
		Put("expectedCashInDrawer", expected).
		Put("countedCash", countedCash).
		Put("variance", variance).
		Put("balanced", balanced))
}

// SalesAnalytics: 30-day default analytics (rolling).
func (a *App) SalesAnalytics(c *gin.Context) {
	today := startOfDay(time.Now())
	from, err := parseDateOrDefault(c.Query("from"), today.AddDate(0, 0, -29), "from")
	if err != nil {
		writeError(c, err)
		return
	}
	to, err := parseDateOrDefault(c.Query("to"), today, "to")
	if err != nil {
		writeError(c, err)
		return
	}
	if from.After(to) {
		writeError(c, badRequest("'from' date must be on or before 'to' date"))
		return
	}
	fromTs := startOfDay(from)
	toTs := startOfDay(to.Add(24 * time.Hour))
	orders, err := a.DB.FindOrdersInRange(fromTs, toTs)
	if err != nil {
		writeError(c, err)
		return
	}
	ids := make([]int64, 0, len(orders))
	for _, o := range orders {
		ids = append(ids, o.ID)
	}
	items, err := a.DB.FindOrderItemsByOrderIDs(ids)
	if err != nil {
		writeError(c, err)
		return
	}
	payments, err := a.DB.FindPaymentsByOrderIDs(ids)
	if err != nil {
		writeError(c, err)
		return
	}
	totalRevenue := decimal.Zero
	for _, o := range orders {
		totalRevenue = totalRevenue.Add(decOrZero(o.TotalAmount))
	}
	orderCount := len(orders)
	avg := decimal.Zero
	if orderCount > 0 {
		avg = totalRevenue.DivRound(decimal.NewFromInt(int64(orderCount)), 2)
	}

	revByChannel := NewObject()
	for _, o := range orders {
		key := "online"
		if o.Channel != nil && strings.TrimSpace(*o.Channel) != "" {
			key = strings.ToLower(*o.Channel)
		}
		cur, _ := revByChannel.Get(key).(decimal.Decimal)
		revByChannel.Put(key, cur.Add(decOrZero(o.TotalAmount)))
	}
	revByPm := NewObject()
	for _, p := range payments {
		if p.Status != nil && strings.EqualFold(*p.Status, "FAILED") {
			continue
		}
		key := "UNKNOWN"
		if p.PaymentMethod != nil {
			key = strings.ToUpper(*p.PaymentMethod)
		}
		cur, _ := revByPm.Get(key).(decimal.Decimal)
		revByPm.Put(key, cur.Add(decOrZero(p.Amount)))
	}
	byStatus := NewObject()
	for _, o := range orders {
		key := "UNKNOWN"
		if o.Status != nil {
			key = strings.ToUpper(*o.Status)
		}
		cur, _ := byStatus.Get(key).(int64)
		byStatus.Put(key, cur+1)
	}

	type prodAgg struct {
		name string
		qty  int64
		rev  decimal.Decimal
	}
	pAgg := map[int64]*prodAgg{}
	for _, oi := range items {
		if oi.ProductID == nil {
			continue
		}
		pid := *oi.ProductID
		agg := pAgg[pid]
		if agg == nil {
			name := ""
			if p, _ := a.DB.FindProductByID(pid); p != nil {
				name = strOrEmpty(p.Name)
			}
			agg = &prodAgg{name: name, rev: decimal.Zero}
			pAgg[pid] = agg
		}
		qty := 0
		if oi.Quantity != nil {
			qty = *oi.Quantity
		}
		unit := decOrZero(oi.Price)
		agg.qty += int64(qty)
		agg.rev = agg.rev.Add(unit.Mul(decimal.NewFromInt(int64(qty))))
	}
	type prodRow struct {
		pid int64
		agg *prodAgg
	}
	rows := make([]prodRow, 0, len(pAgg))
	for k, v := range pAgg {
		rows = append(rows, prodRow{pid: k, agg: v})
	}
	for i := 0; i < len(rows); i++ {
		for j := i + 1; j < len(rows); j++ {
			if rows[j].agg.rev.GreaterThan(rows[i].agg.rev) {
				rows[i], rows[j] = rows[j], rows[i]
			}
		}
	}
	topN := 5
	if len(rows) < topN {
		topN = len(rows)
	}
	topProducts := []any{}
	for i := 0; i < topN; i++ {
		topProducts = append(topProducts, NewObject().
			Put("productId", rows[i].pid).
			Put("name", rows[i].agg.name).
			Put("quantitySold", rows[i].agg.qty).
			Put("revenue", rows[i].agg.rev))
	}

	dailyTrend := []any{}
	dayRevenue := map[string]decimal.Decimal{}
	dayCount := map[string]int{}
	for d := startOfDay(from); !d.After(startOfDay(to)); d = d.AddDate(0, 0, 1) {
		k := dateOnly(d)
		dayRevenue[k] = decimal.Zero
		dayCount[k] = 0
	}
	for _, o := range orders {
		if !o.CreatedAt.Valid {
			continue
		}
		k := dateOnly(o.CreatedAt.Time)
		if _, ok := dayRevenue[k]; !ok {
			continue
		}
		dayRevenue[k] = dayRevenue[k].Add(decOrZero(o.TotalAmount))
		dayCount[k]++
	}
	for d := startOfDay(from); !d.After(startOfDay(to)); d = d.AddDate(0, 0, 1) {
		k := dateOnly(d)
		dailyTrend = append(dailyTrend, NewObject().
			Put("date", k).
			Put("revenue", dayRevenue[k]).
			Put("orderCount", dayCount[k]))
	}

	c.JSON(http.StatusOK, NewObject().
		Put("from", dateOnly(from)).
		Put("to", dateOnly(to)).
		Put("totalRevenue", totalRevenue).
		Put("orderCount", orderCount).
		Put("avgOrderValue", avg).
		Put("revenueByChannel", revByChannel).
		Put("revenueByPaymentMethod", revByPm).
		Put("ordersByStatus", byStatus).
		Put("topProducts", topProducts).
		Put("dailyTrend", dailyTrend))
}

func strconvI(i int) string {
	return itoa(i)
}
