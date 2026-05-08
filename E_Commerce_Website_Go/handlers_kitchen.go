package main

import (
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
)

var kitchenAllowedStatuses = []string{"pending", "baking", "ready", "done", "cancelled"}
var kitchenVisibleStatuses = []string{"pending", "baking", "ready"}

func (a *App) FetchKitchenOnlineOrders(c *gin.Context) {
	out, err := a.fetchKitchenOrders("online", kitchenVisibleStatuses)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) FetchKitchenInStoreOrders(c *gin.Context) {
	out, err := a.fetchKitchenOrders("instore", kitchenVisibleStatuses)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) FetchDeliveryOnlineOrders(c *gin.Context) {
	out, err := a.fetchKitchenOrders("online", []string{"done"})
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) FetchBakeryInStoreOrders(c *gin.Context) {
	out, err := a.fetchKitchenOrders("instore", []string{"done"})
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) fetchKitchenOrders(channel string, statuses []string) ([]any, error) {
	if channel == "" {
		channel = "online"
	}
	channel = strings.ToLower(channel)
	if len(statuses) == 0 {
		statuses = kitchenVisibleStatuses
	}
	lc := make([]string, len(statuses))
	for i, s := range statuses {
		lc[i] = strings.ToLower(s)
	}
	orders, err := a.DB.FindOrdersByChannelAndKitchenStatuses(channel, lc)
	if err != nil {
		return nil, err
	}
	if len(orders) == 0 {
		return []any{}, nil
	}
	ids := make([]int64, 0, len(orders))
	for _, o := range orders {
		ids = append(ids, o.ID)
	}
	items, err := a.DB.FindOrderItemsByOrderIDs(ids)
	if err != nil {
		return nil, err
	}
	itemsByOrder := map[int64][]OrderItem{}
	for _, oi := range items {
		if oi.OrderID == nil {
			continue
		}
		itemsByOrder[*oi.OrderID] = append(itemsByOrder[*oi.OrderID], oi)
	}
	out := make([]any, 0, len(orders))
	for _, o := range orders {
		out = append(out, a.toKitchenOrderMap(&o, itemsByOrder[o.ID]))
	}
	return out, nil
}

func (a *App) toKitchenOrderMap(o *Order, items []OrderItem) *Object {
	customerName := "Walk-in"
	if o.UserID != nil {
		if u, err := a.DB.FindUserByID(*o.UserID); err == nil && u != nil {
			customerName = strOrEmpty(u.Name)
		}
	}
	kitchen := "pending"
	if o.KitchenStatus != nil {
		kitchen = *o.KitchenStatus
	}
	itemMaps := []any{}
	for _, oi := range items {
		var productID any
		productName := "Unknown"
		if oi.ProductID != nil {
			productID = *oi.ProductID
			if p, _ := a.DB.FindProductByID(*oi.ProductID); p != nil {
				productName = strOrEmpty(p.Name)
			}
		}
		itemMaps = append(itemMaps, NewObject().
			Put("itemId", oi.ID).
			Put("productId", productID).
			Put("productName", productName).
			Put("quantity", oi.Quantity).
			Put("price", oi.Price).
			Put("customization", strOrNil(oi.Customization)).
			Put("sweetenerType", strOrNil(oi.SweetenerType)).
			Put("sweetenerPercent", oi.SweetenerPercent).
			Put("flourType", strOrNil(oi.FlourType)))
	}
	return NewObject().
		Put("orderId", o.ID).
		Put("customerName", customerName).
		Put("channel", strOrNil(o.Channel)).
		Put("kitchenStatus", kitchen).
		Put("status", strOrNil(o.Status)).
		Put("totalAmount", o.TotalAmount).
		Put("customerNotes", strOrNil(o.CustomerNotes)).
		Put("kitchenNotes", strOrNil(o.KitchenNotes)).
		Put("createdAt", isoOrNil(o.CreatedAt)).
		Put("items", itemMaps)
}

func (a *App) UpdateKitchenOrderStatus(c *gin.Context) {
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
	statusRaw := strings.TrimSpace(asStr(body["kitchenStatus"]))
	if statusRaw == "" {
		writeError(c, badRequest("Status is required"))
		return
	}
	normalized := strings.ToLower(statusRaw)
	allowed := false
	for _, s := range kitchenAllowedStatuses {
		if s == normalized {
			allowed = true
			break
		}
	}
	if !allowed {
		writeError(c, badRequest("Invalid kitchen status: "+statusRaw+". Allowed: "+strings.Join(kitchenAllowedStatuses, ", ")))
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
	order.KitchenStatus = &normalized
	if reason := strings.TrimSpace(asStr(body["reason"])); reason != "" {
		order.KitchenNotes = &reason
	}
	saved, err := a.DB.SaveOrder(order)
	if err != nil {
		writeError(c, err)
		return
	}
	items, _ := a.DB.FindOrderItemsByOrderIDs([]int64{saved.ID})
	c.JSON(http.StatusOK, a.toKitchenOrderMap(saved, items))
}

// Daily stock --------------------------------------------------------------

func (a *App) FetchDailyStock(c *gin.Context) {
	day := time.Now().Format("2006-01-02")
	rows, err := a.DB.FindDailyStockByDate(day)
	if err != nil {
		writeError(c, err)
		return
	}
	out := []any{}
	for _, ds := range rows {
		productName := "Unknown"
		var productID any
		if ds.ProductID != nil {
			productID = *ds.ProductID
			if p, _ := a.DB.FindProductByID(*ds.ProductID); p != nil {
				productName = strOrEmpty(p.Name)
			}
		}
		target := 0
		if ds.TargetCount != nil {
			target = *ds.TargetCount
		}
		prepared := 0
		if ds.PreparedCount != nil {
			prepared = *ds.PreparedCount
		}
		remaining := target - prepared
		if remaining < 0 {
			remaining = 0
		}
		var stockDate any
		if ds.StockDate != nil {
			stockDate = ds.StockDate.Format("2006-01-02")
		}
		out = append(out, NewObject().
			Put("id", ds.ID).
			Put("productId", productID).
			Put("productName", productName).
			Put("targetCount", ds.TargetCount).
			Put("preparedCount", prepared).
			Put("remaining", remaining).
			Put("stockDate", stockDate))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) AdjustDailyStockPrepared(c *gin.Context) {
	stockID, err := pathInt64(c, "stockId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	delta := 0
	if v, ok := asInt(body["delta"]); ok {
		delta = v
	}
	ds, err := a.DB.FindDailyStockByID(stockID)
	if err != nil {
		writeError(c, err)
		return
	}
	if ds == nil {
		writeError(c, badRequest("Daily stock row not found: "+strconv.FormatInt(stockID, 10)))
		return
	}
	cur := 0
	if ds.PreparedCount != nil {
		cur = *ds.PreparedCount
	}
	next := cur + delta
	if next < 0 {
		next = 0
	}
	ds.PreparedCount = &next
	saved, err := a.DB.SaveDailyStock(ds)
	if err != nil {
		writeError(c, err)
		return
	}
	productName := "Unknown"
	var productID any
	if saved.ProductID != nil {
		productID = *saved.ProductID
		if p, _ := a.DB.FindProductByID(*saved.ProductID); p != nil {
			productName = strOrEmpty(p.Name)
		}
	}
	target := 0
	if saved.TargetCount != nil {
		target = *saved.TargetCount
	}
	remaining := target - next
	if remaining < 0 {
		remaining = 0
	}
	var stockDate any
	if saved.StockDate != nil {
		stockDate = saved.StockDate.Format("2006-01-02")
	}
	c.JSON(http.StatusOK, NewObject().
		Put("id", saved.ID).
		Put("productId", productID).
		Put("productName", productName).
		Put("targetCount", saved.TargetCount).
		Put("preparedCount", saved.PreparedCount).
		Put("remaining", remaining).
		Put("stockDate", stockDate))
}
