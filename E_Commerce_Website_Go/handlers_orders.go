package main

import (
	"net/http"
	"strconv"
	"strings"

	"github.com/gin-gonic/gin"
)

func (a *App) FetchOrders(c *gin.Context) {
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
	roles, err := a.DB.RoleNamesForUser(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	isCustomer := false
	for _, r := range roles {
		if strings.EqualFold(r, "customer") {
			isCustomer = true
			break
		}
	}
	if !isCustomer {
		writeError(c, forbidden("User "+strconv.Itoa(uid)+" is not a customer"))
		return
	}
	orders, err := a.DB.FindOrdersByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	items, err := a.DB.FindOrderItemsByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, NewObject().Put("orders", orders).Put("items", items))
}

func (a *App) FetchPayments(c *gin.Context) {
	uid, ok := queryInt(c, "userid")
	if !ok {
		writeError(c, badRequest("userid is required"))
		return
	}
	includeAll := queryBool(c, "includeAll", false)
	user, err := a.DB.FindUserByID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	if user == nil {
		writeError(c, notFound("User not found: "+strconv.Itoa(uid)))
		return
	}
	payments, err := a.DB.FindPaymentsByUserID(uid)
	if err != nil {
		writeError(c, err)
		return
	}
	if !includeAll {
		payments = filterPaymentsKeepSuccess(payments)
	}
	c.JSON(http.StatusOK, payments)
}

func filterPaymentsKeepSuccess(payments []Payment) []Payment {
	groups := map[int64][]Payment{}
	order := []int64{}
	for _, p := range payments {
		var oid int64
		if p.OrderID != nil {
			oid = *p.OrderID
		}
		if _, ok := groups[oid]; !ok {
			order = append(order, oid)
		}
		groups[oid] = append(groups[oid], p)
	}
	out := []Payment{}
	for _, oid := range order {
		group := groups[oid]
		hasSuccess := false
		for _, p := range group {
			if p.Status != nil && strings.EqualFold(*p.Status, "SUCCESS") {
				hasSuccess = true
				break
			}
		}
		if hasSuccess {
			for _, p := range group {
				if p.Status != nil && strings.EqualFold(*p.Status, "SUCCESS") {
					out = append(out, p)
				}
			}
		} else {
			out = append(out, group...)
		}
	}
	return out
}
