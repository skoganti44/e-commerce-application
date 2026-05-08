package main

import (
	"net/http"
	"strconv"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
)

var allowedPaymentMethods = newStrSet("DEBIT_CARD", "CREDIT_CARD", "GIFT_CARD", "COD")
var allowedCounterPaymentMethods = newStrSet("CASH", "DEBIT_CARD", "CREDIT_CARD", "UPI")

func (a *App) Checkout(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	uid, ok := asInt(body["userid"])
	if !ok {
		writeError(c, badRequest("userid is required"))
		return
	}
	user, err := a.DB.FindUserByID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	if user == nil {
		writeError(c, badRequest("User not found: "+strconv.Itoa(uid)))
		return
	}
	paymentMethod := normalizeCode(asStr(body["paymentMethod"]))
	if paymentMethod == "" || !allowedPaymentMethods.Has(paymentMethod) {
		writeError(c, badRequest("Invalid payment method: "+asStr(body["paymentMethod"])))
		return
	}
	addrBody, _ := body["address"].(map[string]any)
	address, err := buildAddress(addrBody)
	if err != nil {
		writeError(c, err)
		return
	}
	if err := validateAddress(address); err != nil {
		writeError(c, err)
		return
	}
	address.UserID = &uid

	cartItems, err := a.DB.FindCartItemsByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	if len(cartItems) == 0 {
		writeError(c, badRequest("Cart is empty for user: "+strconv.Itoa(uid)))
		return
	}

	totalAmount := decimal.Zero
	type lineCalc struct {
		ci   CartItem
		unit decimal.Decimal
	}
	calcs := []lineCalc{}
	for _, ci := range cartItems {
		var product *Product
		if ci.ProductID != nil {
			product, _ = a.DB.FindProductByID(*ci.ProductID)
		}
		unit := computeUnitPrice(product, ci.SweetenerType, ci.FlourType)
		qty := 0
		if ci.Quantity != nil {
			qty = *ci.Quantity
		}
		totalAmount = totalAmount.Add(unit.Mul(decimal.NewFromInt(int64(qty))))
		calcs = append(calcs, lineCalc{ci: ci, unit: unit})
	}

	statusVal := "CONFIRMED"
	if paymentMethod == "COD" {
		statusVal = "PENDING"
	}
	channel := "online"
	kitchen := "pending"
	order := &Order{
		UserID:        &uid,
		TotalAmount:   &totalAmount,
		Status:        &statusVal,
		Channel:       &channel,
		KitchenStatus: &kitchen,
	}
	if notes := strings.TrimSpace(asStr(body["customerNotes"])); notes != "" {
		order.CustomerNotes = &notes
	}
	saved, err := a.DB.SaveOrder(order)
	if err != nil {
		writeError(c, err)
		return
	}

	for _, lc := range calcs {
		oid := saved.ID
		oi := &OrderItem{
			OrderID:          &oid,
			ProductID:        lc.ci.ProductID,
			Quantity:         lc.ci.Quantity,
			Price:            ptrDec(lc.unit),
			Customization:    lc.ci.Customization,
			SweetenerType:    lc.ci.SweetenerType,
			SweetenerPercent: lc.ci.SweetenerPercent,
			FlourType:        lc.ci.FlourType,
		}
		if _, err := a.DB.SaveOrderItem(oi); err != nil {
			writeError(c, err)
			return
		}
	}

	methodLabel := paymentMethod
	cardLast4 := strings.TrimSpace(asStr(body["cardLast4"]))
	if (paymentMethod == "DEBIT_CARD" || paymentMethod == "CREDIT_CARD") && cardLast4 != "" {
		methodLabel = paymentMethod + " ****" + cardLast4
	}
	payStatus := "SUCCESS"
	if paymentMethod == "COD" {
		payStatus = "PENDING"
	}
	oid := saved.ID
	payment := &Payment{
		OrderID:       &oid,
		Amount:        &totalAmount,
		PaymentMethod: &methodLabel,
		Status:        &payStatus,
	}
	if payment, err = a.DB.SavePayment(payment); err != nil {
		writeError(c, err)
		return
	}

	address.OrderID = &oid
	savedAddr, err := a.DB.SaveShippingAddress(address)
	if err != nil {
		writeError(c, err)
		return
	}

	if _, err := a.DB.DeleteCartItemsByUserID(uid); err != nil {
		writeError(c, err)
		return
	}

	c.JSON(http.StatusOK, NewObject().
		Put("orderId", saved.ID).
		Put("paymentId", payment.ID).
		Put("addressId", savedAddr.ID).
		Put("totalAmount", totalAmount).
		Put("status", strOrEmpty(saved.Status)).
		Put("paymentStatus", strOrEmpty(payment.Status)).
		Put("paymentMethod", strOrEmpty(payment.PaymentMethod)))
}

func ptrDec(d decimal.Decimal) *decimal.Decimal { return &d }

func (a *App) RecordCounterSale(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	rawItems, _ := body["items"].([]any)
	if len(rawItems) == 0 {
		writeError(c, badRequest("At least one item is required"))
		return
	}
	paymentMethod := normalizeCode(asStr(body["paymentMethod"]))
	if paymentMethod == "" || !allowedCounterPaymentMethods.Has(paymentMethod) {
		writeError(c, badRequest("Invalid payment method: "+asStr(body["paymentMethod"])))
		return
	}

	channel := "instore"
	kitchen := "pending"
	statusVal := "CONFIRMED"
	order := &Order{
		Channel:       &channel,
		KitchenStatus: &kitchen,
		Status:        &statusVal,
	}

	customerName := strings.TrimSpace(asStr(body["customerName"]))
	customerNotes := strings.TrimSpace(asStr(body["customerNotes"]))
	notes := ""
	if customerName != "" {
		notes = "Customer: " + customerName
	}
	if customerNotes != "" {
		if notes != "" {
			notes += " — "
		}
		notes += customerNotes
	}
	if notes != "" {
		order.CustomerNotes = &notes
	}

	totalAmount := decimal.Zero
	type lineCalc struct {
		productID        int64
		quantity         int
		unit             decimal.Decimal
		customization    *string
		sweetenerType    *string
		sweetenerPercent *int
		flourType        *string
	}
	calcs := []lineCalc{}
	for _, raw := range rawItems {
		m, ok := raw.(map[string]any)
		if !ok {
			writeError(c, badRequest("Each item needs productId and quantity"))
			return
		}
		pid, okP := asInt64(m["productId"])
		qty, okQ := asInt(m["quantity"])
		if !okP || !okQ {
			writeError(c, badRequest("Each item needs productId and quantity"))
			return
		}
		if qty <= 0 {
			writeError(c, badRequest("Quantity must be positive for product "+strconv.FormatInt(pid, 10)))
			return
		}
		product, err := a.DB.FindProductByID(pid)
		if err != nil {
			writeError(c, err)
			return
		}
		if product == nil {
			writeError(c, badRequest("Product not found: "+strconv.FormatInt(pid, 10)))
			return
		}
		sweetener := trimToNil(m["sweetenerType"])
		flour := trimToNil(m["flourType"])
		unit := computeUnitPrice(product, sweetener, flour)
		totalAmount = totalAmount.Add(unit.Mul(decimal.NewFromInt(int64(qty))))

		var pct *int
		if v, ok := asInt(m["sweetenerPercent"]); ok {
			pct = &v
		}
		calcs = append(calcs, lineCalc{
			productID:        pid,
			quantity:         qty,
			unit:             unit,
			customization:    trimToNil(m["customization"]),
			sweetenerType:    sweetener,
			sweetenerPercent: pct,
			flourType:        flour,
		})
	}

	changeDue := decimal.Zero
	var cashGiven *decimal.Decimal
	if paymentMethod == "CASH" {
		cg, err := toDecimal(body["cashGiven"])
		if err != nil {
			writeError(c, err)
			return
		}
		if cg == nil || cg.LessThan(totalAmount) {
			writeError(c, badRequest("Cash given must be at least the total: "+totalAmount.String()))
			return
		}
		cashGiven = cg
		changeDue = cg.Sub(totalAmount)
	}

	order.TotalAmount = &totalAmount
	saved, err := a.DB.SaveOrder(order)
	if err != nil {
		writeError(c, err)
		return
	}
	for _, lc := range calcs {
		oid := saved.ID
		pid := lc.productID
		qty := lc.quantity
		oi := &OrderItem{
			OrderID:          &oid,
			ProductID:        &pid,
			Quantity:         &qty,
			Price:            ptrDec(lc.unit),
			Customization:    lc.customization,
			SweetenerType:    lc.sweetenerType,
			SweetenerPercent: lc.sweetenerPercent,
			FlourType:        lc.flourType,
		}
		if _, err := a.DB.SaveOrderItem(oi); err != nil {
			writeError(c, err)
			return
		}
	}

	payStatus := "SUCCESS"
	oid := saved.ID
	payment := &Payment{
		OrderID:       &oid,
		Amount:        &totalAmount,
		PaymentMethod: &paymentMethod,
		Status:        &payStatus,
	}
	payment, err = a.DB.SavePayment(payment)
	if err != nil {
		writeError(c, err)
		return
	}

	c.JSON(http.StatusOK, NewObject().
		Put("orderId", saved.ID).
		Put("paymentId", payment.ID).
		Put("totalAmount", totalAmount).
		Put("status", strOrEmpty(saved.Status)).
		Put("paymentMethod", strOrEmpty(payment.PaymentMethod)).
		Put("paymentStatus", strOrEmpty(payment.Status)).
		Put("cashGiven", cashGiven).
		Put("changeDue", changeDue).
		Put("channel", "instore"))
}

// Cleanup ------------------------------------------------------------------

func (a *App) Cleanup(c *gin.Context) {
	uid, ok := queryInt(c, "userid")
	if !ok {
		writeError(c, badRequest("userid is required"))
		return
	}
	user, err := a.DB.FindUserByID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	if user == nil {
		writeError(c, notFound("User not found: "+strconv.Itoa(uid)))
		return
	}
	items, err := a.DB.FindOrderItemsByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	payments, err := a.DB.FindPaymentsByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}

	successOrders := map[int64]struct{}{}
	for _, p := range payments {
		if p.Status != nil && strings.EqualFold(*p.Status, "SUCCESS") && p.OrderID != nil {
			successOrders[*p.OrderID] = struct{}{}
		}
	}

	archivedProducts := map[int64]struct{}{}
	archived := 0
	for _, oi := range items {
		if oi.OrderID == nil {
			continue
		}
		if _, isSuccess := successOrders[*oi.OrderID]; isSuccess {
			continue
		}
		if oi.ProductID == nil {
			continue
		}
		if _, ok := archivedProducts[*oi.ProductID]; ok {
			continue
		}
		archivedProducts[*oi.ProductID] = struct{}{}
		product, err := a.DB.FindProductByID(*oi.ProductID)
		if err != nil || product == nil {
			continue
		}
		var imgURL *string
		images, _ := a.DB.FindImagesByProductID(product.ID)
		if len(images) > 0 {
			imgURL = images[0].ImageURL
		}
		var priceI interface{}
		if product.Price != nil {
			priceI = *product.Price
		}
		_ = a.DB.SaveProductAvailable(product.Name, product.Description, &priceI, product.Stock,
			product.CategoryID, product.CreatedByUserID, imgURL)
		archived++
	}

	paymentsDeleted, err := a.DB.DeletePaymentsByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	itemsDeleted, err := a.DB.DeleteOrderItemsByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	ordersDeleted, err := a.DB.DeleteOrdersByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, NewObject().
		Put("userId", uid).
		Put("archived", archived).
		Put("paymentsDeleted", paymentsDeleted).
		Put("orderItemsDeleted", itemsDeleted).
		Put("ordersDeleted", ordersDeleted))
}
