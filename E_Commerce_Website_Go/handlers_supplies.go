package main

import (
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
)

var allowedSupplyCategories = newStrSet("flour", "sweetener", "dairy", "egg", "nut_seed",
	"flavour", "leavening", "packaging", "cleaning", "other")
var allowedSupplyUnits = newStrSet("kg", "g", "l", "ml", "pcs", "box", "pack")
var allowedSupplyOrderStatuses = newStrSet("received", "waiting", "urgency")
var allowedRequestTeams = newStrSet("kitchen", "bakery", "sales", "delivery", "management")

func (a *App) FetchSupplies(c *gin.Context) {
	if _, err := a.seedSuppliesIfEmpty(); err != nil {
		writeError(c, err)
		return
	}
	if err := a.migrateLegacySupplyStock(); err != nil {
		writeError(c, err)
		return
	}
	supplies, err := a.DB.FindAllSupplies()
	if err != nil {
		writeError(c, err)
		return
	}
	out := []any{}
	for _, s := range supplies {
		st := "received"
		if s.OrderStatus != nil {
			st = strings.ToLower(*s.OrderStatus)
		}
		if st == "received" {
			continue
		}
		out = append(out, toSupplyMap(&s))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) FetchInStockSupplies(c *gin.Context) {
	if _, err := a.seedSuppliesIfEmpty(); err != nil {
		writeError(c, err)
		return
	}
	if err := a.migrateLegacySupplyStock(); err != nil {
		writeError(c, err)
		return
	}
	supplies, err := a.DB.FindAllSupplies()
	if err != nil {
		writeError(c, err)
		return
	}
	out := []any{}
	for _, s := range supplies {
		in := decimal.Zero
		if s.InStock != nil {
			in = *s.InStock
		}
		if in.Sign() <= 0 {
			continue
		}
		out = append(out, toSupplyMap(&s))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) AdjustSupplyStock(c *gin.Context) {
	id, err := pathInt64(c, "supplyId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	delta, err := toDecimal(body["delta"])
	if err != nil {
		writeError(c, err)
		return
	}
	if delta == nil {
		writeError(c, badRequest("delta is required"))
		return
	}
	s, err := a.DB.FindSupplyByID(id)
	if err != nil {
		writeError(c, err)
		return
	}
	if s == nil {
		writeError(c, badRequest("Supply not found: "+strconv.FormatInt(id, 10)))
		return
	}
	cur := decimal.Zero
	if s.CurrentStock != nil {
		cur = *s.CurrentStock
	}
	next := cur.Add(*delta)
	if next.Sign() < 0 {
		next = decimal.Zero
	}
	s.CurrentStock = &next
	if note := strings.TrimSpace(asStr(body["note"])); note != "" {
		s.Notes = &note
	}
	saved, err := a.DB.SaveSupply(s)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, toSupplyMap(saved))
}

func (a *App) SaveSupply(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	if body == nil {
		writeError(c, badRequest("Request body is required"))
		return
	}
	var id *int64
	if v, ok := asInt64(body["id"]); ok {
		id = &v
	}
	name := strings.TrimSpace(asStr(body["name"]))
	if name == "" {
		writeError(c, badRequest("name is required"))
		return
	}
	unit := strings.TrimSpace(asStr(body["unit"]))
	if unit == "" {
		writeError(c, badRequest("unit is required"))
		return
	}
	unitLc := strings.ToLower(unit)
	if !allowedSupplyUnits.Has(unitLc) {
		writeError(c, badRequest("Invalid unit. Allowed: "+strings.Join(allowedSupplyUnits.SortedKeys(), ", ")))
		return
	}
	categoryRaw := strings.TrimSpace(asStr(body["category"]))
	categoryLc := "other"
	if categoryRaw != "" {
		categoryLc = strings.ToLower(categoryRaw)
	}
	if !allowedSupplyCategories.Has(categoryLc) {
		writeError(c, badRequest("Invalid category. Allowed: "+strings.Join(allowedSupplyCategories.SortedKeys(), ", ")))
		return
	}
	inStock, err := toDecimal(body["inStock"])
	if err != nil {
		writeError(c, err)
		return
	}
	if inStock == nil {
		inStock, err = toDecimal(body["currentStock"])
		if err != nil {
			writeError(c, err)
			return
		}
	}
	threshold, err := toDecimal(body["threshold"])
	if err != nil {
		writeError(c, err)
		return
	}
	notes := trimToNil(body["notes"])

	var supply *Supply
	if id != nil {
		supply, err = a.DB.FindSupplyByID(*id)
		if err != nil {
			writeError(c, err)
			return
		}
		if supply == nil {
			writeError(c, badRequest("Supply not found: "+strconv.FormatInt(*id, 10)))
			return
		}
	} else {
		supply, err = a.DB.FindSupplyByName(name)
		if err != nil {
			writeError(c, err)
			return
		}
		if supply == nil {
			supply = &Supply{}
		}
	}
	supply.Name = name
	supply.Unit = unitLc
	supply.Category = &categoryLc
	if inStock != nil {
		supply.InStock = inStock
	} else if supply.InStock == nil {
		z := decimal.Zero
		supply.InStock = &z
	}
	if supply.CurrentStock == nil {
		z := decimal.Zero
		supply.CurrentStock = &z
	}
	if supply.RequestedQty == nil {
		z := decimal.Zero
		supply.RequestedQty = &z
	}
	if supply.OrderStatus == nil {
		def := "received"
		supply.OrderStatus = &def
	}
	if threshold == nil {
		z := decimal.Zero
		supply.Threshold = &z
	} else {
		supply.Threshold = threshold
	}
	supply.Notes = notes
	saved, err := a.DB.SaveSupply(supply)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, toSupplyMap(saved))
}

func (a *App) BulkUpdateSupplyStatuses(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	updatesRaw, _ := body["updates"].([]any)
	if len(updatesRaw) == 0 {
		writeError(c, badRequest("updates is required"))
		return
	}
	out := []any{}
	for _, raw := range updatesRaw {
		row, ok := raw.(map[string]any)
		if !ok || row["id"] == nil {
			continue
		}
		id, ok := asInt64(row["id"])
		if !ok {
			continue
		}
		statusRaw := asStr(row["orderStatus"])
		status := strings.ToLower(strings.TrimSpace(statusRaw))
		if status == "" || !allowedSupplyOrderStatuses.Has(status) {
			writeError(c, badRequest("Invalid orderStatus for supply "+strconv.FormatInt(id, 10)+": "+statusRaw))
			return
		}
		requestedQty, err := toDecimal(row["requestedQty"])
		if err != nil {
			writeError(c, err)
			return
		}
		s, err := a.DB.FindSupplyByID(id)
		if err != nil {
			writeError(c, err)
			return
		}
		if s == nil {
			writeError(c, badRequest("Supply not found: "+strconv.FormatInt(id, 10)))
			return
		}
		prev := "received"
		if s.OrderStatus != nil {
			prev = *s.OrderStatus
		}
		if status == "received" {
			delivered := decimal.Zero
			if s.CurrentStock != nil {
				delivered = *s.CurrentStock
			}
			pantry := decimal.Zero
			if s.InStock != nil {
				pantry = *s.InStock
			}
			next := pantry.Add(delivered)
			s.InStock = &next
			z := decimal.Zero
			s.CurrentStock = &z
			zr := decimal.Zero
			s.RequestedQty = &zr
			s.RequestedAt.Valid = false
		} else {
			if requestedQty != nil {
				if requestedQty.Sign() < 0 {
					z := decimal.Zero
					s.RequestedQty = &z
				} else {
					s.RequestedQty = requestedQty
				}
			}
			if status != prev {
				s.RequestedAt = nullTimeFrom(time.Now())
			}
		}
		s.OrderStatus = &status
		saved, err := a.DB.SaveSupply(s)
		if err != nil {
			writeError(c, err)
			return
		}
		out = append(out, toSupplyMap(saved))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) RequestMoreSupply(c *gin.Context) {
	id, err := pathInt64(c, "supplyId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	requestedQty, err := toDecimal(body["requestedQty"])
	if err != nil {
		writeError(c, err)
		return
	}
	if requestedQty == nil {
		writeError(c, badRequest("requestedQty is required"))
		return
	}
	urgency := strings.TrimSpace(asStr(body["urgency"]))
	out, err := a.requestSupplyByTeam(id, requestedQty, urgency, "")
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) RequestSupplyByTeam(c *gin.Context) {
	id, err := pathInt64(c, "supplyId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	requestedQty, err := toDecimal(body["requestedQty"])
	if err != nil {
		writeError(c, err)
		return
	}
	urgency := strings.TrimSpace(asStr(body["urgency"]))
	team := strings.TrimSpace(asStr(body["team"]))
	out, err := a.requestSupplyByTeam(id, requestedQty, urgency, team)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) requestSupplyByTeam(supplyID int64, requestedQty *decimal.Decimal, urgency, teamRaw string) (*Object, error) {
	if requestedQty == nil || requestedQty.Sign() <= 0 {
		return nil, badRequest("requestedQty must be positive")
	}
	status := "waiting"
	if urgency != "" {
		status = strings.ToLower(urgency)
	}
	if !allowedSupplyOrderStatuses.Has(status) || status == "received" {
		status = "waiting"
	}
	team := "kitchen"
	if teamRaw != "" {
		team = strings.ToLower(teamRaw)
	}
	if !allowedRequestTeams.Has(team) {
		return nil, badRequest("team must be one of: " + strings.Join(allowedRequestTeams.SortedKeys(), ", "))
	}
	s, err := a.DB.FindSupplyByID(supplyID)
	if err != nil {
		return nil, err
	}
	if s == nil {
		return nil, badRequest("Supply not found: " + strconv.FormatInt(supplyID, 10))
	}
	s.RequestedQty = requestedQty
	s.OrderStatus = &status
	s.RequestedByTeam = &team
	s.RequestedAt = nullTimeFrom(time.Now())
	saved, err := a.DB.SaveSupply(s)
	if err != nil {
		return nil, err
	}
	return toSupplyMap(saved), nil
}

func (a *App) FetchSupplyRequests(c *gin.Context) {
	supplies, err := a.DB.FindAllSupplies()
	if err != nil {
		writeError(c, err)
		return
	}
	out := []any{}
	for _, s := range supplies {
		st := "received"
		if s.OrderStatus != nil {
			st = *s.OrderStatus
		}
		if strings.EqualFold(st, "received") {
			continue
		}
		out = append(out, toSupplyMap(&s))
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) FulfillSupply(c *gin.Context) {
	id, err := pathInt64(c, "supplyId")
	if err != nil {
		writeError(c, err)
		return
	}
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	receivedQty, err := toDecimal(body["receivedQty"])
	if err != nil {
		writeError(c, err)
		return
	}
	if receivedQty == nil || receivedQty.Sign() <= 0 {
		writeError(c, badRequest("receivedQty must be positive"))
		return
	}
	s, err := a.DB.FindSupplyByID(id)
	if err != nil {
		writeError(c, err)
		return
	}
	if s == nil {
		writeError(c, badRequest("Supply not found: "+strconv.FormatInt(id, 10)))
		return
	}
	cur := decimal.Zero
	if s.CurrentStock != nil {
		cur = *s.CurrentStock
	}
	next := cur.Add(*receivedQty)
	s.CurrentStock = &next
	if note := strings.TrimSpace(asStr(body["note"])); note != "" {
		s.Notes = &note
	}
	saved, err := a.DB.SaveSupply(s)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, toSupplyMap(saved))
}

func (a *App) SeedSupplies(c *gin.Context) {
	out, err := a.seedSuppliesIfEmpty()
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) seedSuppliesIfEmpty() (*Object, error) {
	count, err := a.DB.CountSupplies()
	if err != nil {
		return nil, err
	}
	if count > 0 {
		return NewObject().Put("seeded", 0).Put("existing", count), nil
	}
	seeds := defaultSupplySeeds()
	for _, s := range seeds {
		if _, err := a.DB.SaveSupply(s); err != nil {
			return nil, err
		}
	}
	return NewObject().Put("seeded", len(seeds)).Put("existing", int64(0)), nil
}

func (a *App) migrateLegacySupplyStock() error {
	supplies, err := a.DB.FindAllSupplies()
	if err != nil {
		return err
	}
	for i := range supplies {
		s := &supplies[i]
		changed := false
		if s.InStock == nil {
			cur := decimal.Zero
			if s.CurrentStock != nil {
				cur = *s.CurrentStock
			}
			s.InStock = &cur
			z := decimal.Zero
			s.CurrentStock = &z
			changed = true
		}
		if s.RequestedQty == nil {
			z := decimal.Zero
			s.RequestedQty = &z
			changed = true
		}
		if s.OrderStatus == nil {
			def := "received"
			s.OrderStatus = &def
			changed = true
		}
		if changed {
			if _, err := a.DB.SaveSupply(s); err != nil {
				return err
			}
		}
	}
	return nil
}

func defaultSupplySeeds() []*Supply {
	mk := func(name, unit, cat, current, threshold string) *Supply {
		curD, _ := decimal.NewFromString(current)
		thD, _ := decimal.NewFromString(threshold)
		zero := decimal.Zero
		def := "received"
		catCopy := cat
		return &Supply{
			Name:         name,
			Unit:         unit,
			Category:     &catCopy,
			CurrentStock: &zero,
			InStock:      &curD,
			RequestedQty: &zero,
			Threshold:    &thD,
			OrderStatus:  &def,
		}
	}
	return []*Supply{
		mk("Finger Millet (Ragi) Flour", "kg", "flour", "10.0", "3.0"),
		mk("Bajra Millet Flour", "kg", "flour", "6.0", "2.0"),
		mk("Little Millet Flour", "kg", "flour", "5.0", "2.0"),
		mk("Sorghum (Jowar) Flour", "kg", "flour", "5.0", "2.0"),
		mk("Whole Wheat Flour", "kg", "flour", "12.0", "4.0"),
		mk("All-Purpose Flour", "kg", "flour", "10.0", "3.0"),
		mk("Cane Sugar", "kg", "sweetener", "8.0", "2.0"),
		mk("Brown Sugar", "kg", "sweetener", "4.0", "1.0"),
		mk("Jaggery", "kg", "sweetener", "5.0", "1.5"),
		mk("Honey", "l", "sweetener", "3.0", "1.0"),
		mk("Maple Syrup", "l", "sweetener", "2.0", "0.5"),
		mk("Butter (unsalted)", "kg", "dairy", "4.0", "1.0"),
		mk("Milk", "l", "dairy", "6.0", "2.0"),
		mk("Curd / Yogurt", "kg", "dairy", "2.0", "0.5"),
		mk("Eggs", "pcs", "egg", "60", "24"),
		mk("Baking Powder", "g", "leavening", "500", "150"),
		mk("Baking Soda", "g", "leavening", "500", "150"),
		mk("Yeast (instant)", "g", "leavening", "250", "50"),
		mk("Vanilla Essence", "ml", "flavour", "500", "100"),
		mk("Cocoa Powder", "kg", "flavour", "1.5", "0.5"),
		mk("Salt", "kg", "flavour", "2.0", "0.5"),
		mk("Almonds", "kg", "nut_seed", "1.5", "0.5"),
		mk("Cashews", "kg", "nut_seed", "1.5", "0.5"),
		mk("Sesame Seeds", "kg", "nut_seed", "1.0", "0.3"),
		mk("Cookie Boxes (small)", "pcs", "packaging", "120", "40"),
		mk("Cake Boxes", "pcs", "packaging", "60", "20"),
		mk("Paper Liners", "pack", "packaging", "10", "3"),
		mk("Parchment Paper", "pack", "packaging", "6", "2"),
	}
}

func toSupplyMap(s *Supply) *Object {
	cur := decimal.Zero
	if s.CurrentStock != nil {
		cur = *s.CurrentStock
	}
	in := decimal.Zero
	if s.InStock != nil {
		in = *s.InStock
	}
	req := decimal.Zero
	if s.RequestedQty != nil {
		req = *s.RequestedQty
	}
	thr := decimal.Zero
	if s.Threshold != nil {
		thr = *s.Threshold
	}
	out := in.Sign() <= 0
	low := !out && in.Cmp(thr) <= 0
	orderStatus := "received"
	if s.OrderStatus != nil {
		orderStatus = *s.OrderStatus
	}
	return NewObject().
		Put("id", s.ID).
		Put("name", s.Name).
		Put("unit", s.Unit).
		Put("category", strOrNil(s.Category)).
		Put("currentStock", cur).
		Put("inStock", in).
		Put("requestedQty", req).
		Put("threshold", thr).
		Put("notes", strOrNil(s.Notes)).
		Put("lowStock", low).
		Put("outOfStock", out).
		Put("orderStatus", orderStatus).
		Put("requestedByTeam", strOrNil(s.RequestedByTeam)).
		Put("requestedAt", isoOrNil(s.RequestedAt)).
		Put("updatedAt", isoOrNil(s.UpdatedAt))
}
