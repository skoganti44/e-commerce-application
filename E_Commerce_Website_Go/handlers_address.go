package main

import (
	"net/http"
	"strconv"

	"github.com/gin-gonic/gin"
)

func (a *App) SaveShippingAddress(c *gin.Context) {
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
	saved, err := a.DB.SaveShippingAddress(address)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, toPublicAddress(saved))
}

func (a *App) GetLatestShippingAddress(c *gin.Context) {
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
	addr, err := a.DB.FindLatestShippingAddressByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	if addr == nil {
		c.JSON(http.StatusOK, NewObject())
		return
	}
	c.JSON(http.StatusOK, toPublicAddress(addr))
}

func buildAddress(body map[string]any) (*ShippingAddress, error) {
	if body == nil {
		return nil, badRequest("Address is required")
	}
	a := &ShippingAddress{}
	a.FullName = trimToNil(body["fullName"])
	a.Phone = trimToNil(body["phone"])
	a.Line1 = trimToNil(body["line1"])
	a.Line2 = trimToNil(body["line2"])
	a.Landmark = trimToNil(body["landmark"])
	a.City = trimToNil(body["city"])
	a.State = trimToNil(body["state"])
	a.Pincode = trimToNil(body["pincode"])
	a.Country = trimToNil(body["country"])
	a.Instructions = trimToNil(body["instructions"])
	t := trimToNil(body["addressType"])
	if t == nil {
		def := "HOME"
		a.AddressType = &def
	} else {
		upper := normalizeCode(*t)
		a.AddressType = &upper
	}
	return a, nil
}

func validateAddress(a *ShippingAddress) error {
	if a.FullName == nil {
		return badRequest("fullName is required")
	}
	if a.Phone == nil || !phoneRe.MatchString(*a.Phone) {
		return badRequest("phone must be 10 digits")
	}
	if a.Line1 == nil {
		return badRequest("line1 is required")
	}
	if a.City == nil {
		return badRequest("city is required")
	}
	if a.State == nil {
		return badRequest("state is required")
	}
	if a.Pincode == nil || !pinRe.MatchString(*a.Pincode) {
		return badRequest("ZIP must be 5 digits or ZIP+4 (e.g. 12345 or 12345-6789)")
	}
	return nil
}

func toPublicAddress(a *ShippingAddress) *Object {
	o := NewObject().
		Put("id", a.ID).
		Put("fullName", strOrNil(a.FullName)).
		Put("phone", strOrNil(a.Phone)).
		Put("line1", strOrNil(a.Line1)).
		Put("line2", strOrNil(a.Line2)).
		Put("landmark", strOrNil(a.Landmark)).
		Put("city", strOrNil(a.City)).
		Put("state", strOrNil(a.State)).
		Put("pincode", strOrNil(a.Pincode)).
		Put("country", strOrNil(a.Country)).
		Put("instructions", strOrNil(a.Instructions)).
		Put("addressType", strOrNil(a.AddressType))
	if a.OrderID != nil {
		o.Put("orderId", *a.OrderID)
	} else {
		o.Put("orderId", nil)
	}
	o.Put("createdAt", isoOrNil(a.CreatedAt))
	return o
}

func strOrNil(p *string) any {
	if p == nil {
		return nil
	}
	return *p
}
