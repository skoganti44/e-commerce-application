package main

import (
	"database/sql"
	"net/http"
	"strconv"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
)

func (a *App) FetchCart(c *gin.Context) {
	uid, ok := queryInt(c, "userid")
	if !ok {
		writeError(c, badRequest("userid is required"))
		return
	}
	out, err := a.fetchCartByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) fetchCartByUserID(userid int) (any, error) {
	carts, err := a.DB.FindCartsByUserID(userid)
	if err != nil {
		return nil, err
	}
	if len(carts) == 0 {
		return nil, notFound("No cart found for userId: " + strconv.Itoa(userid))
	}
	items, err := a.DB.FindCartItemsByUserID(userid)
	if err != nil {
		return nil, err
	}

	itemTotals := NewObject()
	subtotal := decimal.Zero
	totalQty := 0
	for _, ci := range items {
		var product *Product
		if ci.ProductID != nil {
			product, _ = a.DB.FindProductByID(*ci.ProductID)
		}
		base := decimal.Zero
		if product != nil && product.Price != nil {
			base = *product.Price
		}
		sAdd := addon(sweetenerAddon, ci.SweetenerType)
		fAdd := addon(flourAddon, ci.FlourType)
		unit := base.Add(sAdd).Add(fAdd)
		qty := 0
		if ci.Quantity != nil {
			qty = *ci.Quantity
		}
		line := unit.Mul(decimal.NewFromInt(int64(qty)))

		per := NewObject().
			Put("basePrice", base).
			Put("sweetenerAddon", sAdd).
			Put("flourAddon", fAdd).
			Put("unitPrice", unit).
			Put("lineTotal", line)
		itemTotals.Put(strconv.FormatInt(ci.ID, 10), per)

		subtotal = subtotal.Add(line)
		totalQty += qty
	}

	totals := NewObject().
		Put("subtotal", subtotal).
		Put("itemCount", len(items)).
		Put("totalQuantity", totalQty)

	return NewObject().
		Put("cart", carts).
		Put("items", items).
		Put("itemTotals", itemTotals).
		Put("totals", totals), nil
}

func (a *App) AddToCart(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	userid, ok := asInt(body["userid"])
	if !ok {
		writeError(c, badRequest("userid is required"))
		return
	}
	productID, ok := asInt64(body["productId"])
	if !ok {
		writeError(c, badRequest("productId is required"))
		return
	}
	qty := 1
	if v, ok := asInt(body["quantity"]); ok {
		qty = v
	}
	sweetenerRaw := asStr(body["sweetenerType"])
	flourRaw := asStr(body["flourType"])
	var sweetenerPercent *int
	if v, ok := body["sweetenerPercent"]; ok && v != nil {
		if n, ok := asInt(v); ok {
			sweetenerPercent = &n
		}
	}
	if err := a.addToCart(userid, productID, qty, sweetenerRaw, sweetenerPercent, flourRaw); err != nil {
		writeError(c, err)
		return
	}
	out, err := a.fetchCartByUserID(userid)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) addToCart(userid int, productID int64, quantity int,
	sweetenerRaw string, sweetenerPercent *int, flourRaw string) error {
	if quantity <= 0 {
		return badRequest("quantity must be positive")
	}
	user, err := a.DB.FindUserByID(userid)
	if err != nil {
		return err
	}
	if user == nil {
		return badRequest("User not found: " + strconv.Itoa(userid))
	}
	product, err := a.DB.FindProductByID(productID)
	if err != nil {
		return err
	}
	if product == nil {
		return badRequest("Product not found: " + strconv.FormatInt(productID, 10))
	}
	if product.Stock != nil && *product.Stock < quantity {
		return badRequest("Only " + strconv.Itoa(*product.Stock) + " in stock")
	}

	sweetener := normalizeCode(sweetenerRaw)
	flour := normalizeCode(flourRaw)
	if sweetener != "" && !allowedSweeteners.Has(sweetener) {
		return badRequest("Invalid sweetener: " + sweetenerRaw)
	}
	if flour != "" && !allowedFlours.Has(flour) {
		return badRequest("Invalid flour: " + flourRaw)
	}
	if sweetenerPercent != nil && (*sweetenerPercent < 0 || *sweetenerPercent > 100) {
		return badRequest("sweetenerPercent must be between 0 and 100")
	}

	carts, err := a.DB.FindCartsByUserID(userid)
	if err != nil {
		return err
	}
	var cart *Cart
	if len(carts) == 0 {
		uid := userid
		cart = &Cart{UserID: &uid, CreatedAt: sql.NullTime{Time: time.Now(), Valid: true}}
		if cart, err = a.DB.SaveCart(cart); err != nil {
			return err
		}
	} else {
		cart = &carts[0]
	}

	isCustom := sweetener != "" || flour != "" || sweetenerPercent != nil

	if !isCustom {
		existing, err := a.DB.FindCartItemsByUserID(userid)
		if err != nil {
			return err
		}
		for i := range existing {
			ci := &existing[i]
			if ci.ProductID != nil && *ci.ProductID == productID &&
				ci.SweetenerType == nil && ci.FlourType == nil && ci.SweetenerPercent == nil {
				newQty := quantity
				if ci.Quantity != nil {
					newQty += *ci.Quantity
				}
				ci.Quantity = &newQty
				if _, err := a.DB.SaveCartItem(ci); err != nil {
					return err
				}
				return nil
			}
		}
	}

	cartID := cart.ID
	pid := productID
	q := quantity
	item := &CartItem{
		CartID:    &cartID,
		ProductID: &pid,
		Quantity:  &q,
	}
	if sweetener != "" {
		s := sweetener
		item.SweetenerType = &s
	}
	if flour != "" {
		f := flour
		item.FlourType = &f
	}
	item.SweetenerPercent = sweetenerPercent
	if _, err := a.DB.SaveCartItem(item); err != nil {
		return err
	}
	return nil
}

func (a *App) UpdateCartItem(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	userid, ok := asInt(body["userid"])
	if !ok {
		writeError(c, badRequest("userid is required"))
		return
	}
	itemID, ok := asInt64(body["cartItemId"])
	if !ok {
		writeError(c, badRequest("cartItemId is required"))
		return
	}
	qty := 0
	if v, ok := asInt(body["quantity"]); ok {
		qty = v
	}
	item, err := a.resolveOwnedCartItem(userid, itemID)
	if err != nil {
		writeError(c, err)
		return
	}
	if qty <= 0 {
		if err := a.DB.DeleteCartItem(item.ID); err != nil {
			writeError(c, err)
			return
		}
	} else {
		if item.ProductID != nil {
			product, _ := a.DB.FindProductByID(*item.ProductID)
			if product != nil && product.Stock != nil && *product.Stock < qty {
				writeError(c, badRequest("Only "+strconv.Itoa(*product.Stock)+" in stock"))
				return
			}
		}
		item.Quantity = &qty
		if _, err := a.DB.SaveCartItem(item); err != nil {
			writeError(c, err)
			return
		}
	}
	out, err := a.fetchCartByUserID(userid)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) RemoveCartItem(c *gin.Context) {
	uid, ok := queryInt(c, "userid")
	if !ok {
		writeError(c, badRequest("userid is required"))
		return
	}
	itemRaw := c.Query("itemId")
	itemID, err := strconv.ParseInt(itemRaw, 10, 64)
	if err != nil {
		writeError(c, badRequest("itemId is required"))
		return
	}
	item, err := a.resolveOwnedCartItem(uid, itemID)
	if err != nil {
		writeError(c, err)
		return
	}
	if err := a.DB.DeleteCartItem(item.ID); err != nil {
		writeError(c, err)
		return
	}
	out, err := a.fetchCartByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) resolveOwnedCartItem(userid int, itemID int64) (*CartItem, error) {
	item, err := a.DB.FindCartItemByID(itemID)
	if err != nil {
		return nil, err
	}
	if item == nil {
		return nil, badRequest("Cart item not found: " + strconv.FormatInt(itemID, 10))
	}
	if item.CartID == nil {
		return nil, badRequest("Cart item does not belong to user " + strconv.Itoa(userid))
	}
	carts, err := a.DB.FindCartsByUserID(userid)
	if err != nil {
		return nil, err
	}
	owned := false
	for _, cart := range carts {
		if cart.ID == *item.CartID {
			owned = true
			break
		}
	}
	if !owned {
		return nil, badRequest("Cart item does not belong to user " + strconv.Itoa(userid))
	}
	return item, nil
}
