package main

import (
	"net/http"
	"strings"

	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
)

func (a *App) FetchProducts(c *gin.Context) {
	products, err := a.DB.FindAllProducts()
	if err != nil {
		writeError(c, err)
		return
	}
	out := make([]any, 0, len(products))
	for i := range products {
		p := &products[i]
		row := NewObject().
			Put("id", p.ID).
			Put("name", strOrEmpty(p.Name)).
			Put("description", strOrEmpty(p.Description)).
			Put("price", p.Price).
			Put("stock", p.Stock)
		if p.CategoryID != nil {
			cat, _ := a.DB.FindCategoryByID(*p.CategoryID)
			if cat != nil {
				row.Put("category", NewObject().
					Put("id", cat.ID).
					Put("name", strOrEmpty(cat.Name)))
			} else {
				row.Put("category", nil)
			}
		} else {
			row.Put("category", nil)
		}
		images, _ := a.DB.FindImagesByProductID(p.ID)
		var imgURL any
		if len(images) > 0 {
			imgURL = strOrEmpty(images[0].ImageURL)
		}
		row.Put("imageUrl", imgURL)
		row.Put("supportedFlours", splitCSV(strOrEmpty(p.SupportedFlours)))
		row.Put("supportedSweeteners", splitCSV(strOrEmpty(p.SupportedSweeteners)))
		out = append(out, row)
	}
	c.JSON(http.StatusOK, out)
}

func (a *App) SaveProduct(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	creator, err := a.requireEmployeeFromBody(body)
	if err != nil {
		writeError(c, err)
		return
	}
	name := asStr(body["name"])
	description := asStr(body["description"])
	rawItems, _ := body["items"].([]any)
	items := toMapSlice(rawItems)
	saved, err := a.saveItems(name, description, items, creator)
	if err != nil {
		writeError(c, err)
		return
	}
	c.JSON(http.StatusOK, saved)
}

func (a *App) SaveProducts(c *gin.Context) {
	body, err := bindGenericMap(c)
	if err != nil {
		writeError(c, err)
		return
	}
	creator, err := a.requireEmployeeFromBody(body)
	if err != nil {
		writeError(c, err)
		return
	}
	rawProducts, _ := body["products"].([]any)
	products := toMapSlice(rawProducts)
	saved := []*Product{}
	for _, p := range products {
		name := asStr(p["name"])
		description := asStr(p["description"])
		rawItems, _ := p["items"].([]any)
		items := toMapSlice(rawItems)
		out, err := a.saveItems(name, description, items, creator)
		if err != nil {
			writeError(c, err)
			return
		}
		saved = append(saved, out...)
	}
	c.JSON(http.StatusOK, saved)
}

func (a *App) requireEmployeeFromBody(body map[string]any) (*User, error) {
	uid, ok := asInt(body["userId"])
	if !ok {
		return nil, badRequest("userId is required")
	}
	user, err := a.DB.FindUserByID(uid)
	if err != nil {
		return nil, err
	}
	if user == nil {
		return nil, badRequest("User not found: " + itoa(uid))
	}
	roles, err := a.DB.RoleNamesForUser(uid)
	if err != nil {
		return nil, err
	}
	hasNonCustomer := false
	for _, r := range roles {
		if r != "customer" {
			hasNonCustomer = true
			break
		}
	}
	if !hasNonCustomer {
		return nil, forbidden("Only employees can add items to sell")
	}
	return user, nil
}

func (a *App) saveItems(name, description string, items []map[string]any, creator *User) ([]*Product, error) {
	saved := []*Product{}
	for _, item := range items {
		catRaw, _ := item["category"].(map[string]any)
		var catID *int64
		if catRaw != nil {
			catName, _ := catRaw["categoryName"].(string)
			catType, _ := catRaw["type"].(string)
			cat := &Category{Name: &catName, Description: &catType}
			savedCat, err := a.DB.SaveCategory(cat)
			if err != nil {
				return nil, err
			}
			id := savedCat.ID
			catID = &id
		}

		var price *decimal.Decimal
		if d, err := toDecimal(item["price"]); err != nil {
			return nil, err
		} else {
			price = d
		}
		stockInt, _ := asInt(item["stock"])
		stockVal := stockInt

		flours, err := normalizeCodeList(item["supportedFlours"], allowedFlours, "flour")
		if err != nil {
			return nil, err
		}
		sweeteners, err := normalizeCodeList(item["supportedSweeteners"], allowedSweeteners, "sweetener")
		if err != nil {
			return nil, err
		}

		nameCopy := name
		descCopy := description
		creatorID := creator.UserID

		p := &Product{
			Name:                &nameCopy,
			Description:         &descCopy,
			Price:               price,
			Stock:               &stockVal,
			CategoryID:          catID,
			CreatedByUserID:     &creatorID,
			SupportedFlours:     stringPtrOrNil(flours),
			SupportedSweeteners: stringPtrOrNil(sweeteners),
		}
		savedP, err := a.DB.SaveProduct(p)
		if err != nil {
			return nil, err
		}

		imgURL, _ := item["imageUrl"].(string)
		imgURLCopy := imgURL
		pid := savedP.ID
		if _, err := a.DB.SaveProductImage(&ProductImage{ProductID: &pid, ImageURL: &imgURLCopy}); err != nil {
			return nil, err
		}
		saved = append(saved, savedP)
	}
	return saved, nil
}

func normalizeCodeList(raw any, allowed StrSet, label string) (string, error) {
	if raw == nil {
		return "", nil
	}
	var values []string
	switch x := raw.(type) {
	case []any:
		for _, v := range x {
			if v == nil {
				continue
			}
			values = append(values, asStr(v))
		}
	case string:
		values = strings.Split(x, ",")
	default:
		return "", nil
	}
	out := []string{}
	seen := map[string]struct{}{}
	for _, v := range values {
		code := normalizeCode(v)
		if code == "" {
			continue
		}
		if !allowed.Has(code) {
			return "", badRequest("Invalid " + label + ": " + code)
		}
		if _, dup := seen[code]; dup {
			continue
		}
		seen[code] = struct{}{}
		out = append(out, code)
	}
	return strings.Join(out, ","), nil
}

func toMapSlice(in []any) []map[string]any {
	out := make([]map[string]any, 0, len(in))
	for _, v := range in {
		if m, ok := v.(map[string]any); ok {
			out = append(out, m)
		}
	}
	return out
}

func stringPtrOrNil(s string) *string {
	if s == "" {
		return nil
	}
	v := s
	return &v
}

func itoa(i int) string {
	// Tiny helper to avoid importing strconv in many files.
	if i == 0 {
		return "0"
	}
	neg := false
	if i < 0 {
		neg = true
		i = -i
	}
	buf := [20]byte{}
	n := len(buf)
	for i > 0 {
		n--
		buf[n] = byte('0' + i%10)
		i /= 10
	}
	if neg {
		n--
		buf[n] = '-'
	}
	return string(buf[n:])
}
