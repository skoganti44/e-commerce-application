package main

import (
	"encoding/json"
	"strconv"
)

// MarshalJSON for User mirrors Java's serialization order:
// userid, name, email, password, createdat.
func (u User) MarshalJSON() ([]byte, error) {
	o := NewObject().
		Put("userid", u.UserID).
		Put("name", strOrNil(u.Name)).
		Put("email", strOrNil(u.Email))
	if u.Password != nil {
		o.Put("password", *u.Password)
	} else {
		o.Put("password", nil)
	}
	o.Put("createdat", isoOrNil(u.CreatedAt))
	return json.Marshal(o)
}

func (c Cart) MarshalJSON() ([]byte, error) {
	o := NewObject().Put("id", c.ID)
	if c.UserID != nil {
		o.Put("userid", *c.UserID)
	} else {
		o.Put("userid", nil)
	}
	o.Put("createdat", isoOrNil(c.CreatedAt))
	return json.Marshal(o)
}

func (ci CartItem) MarshalJSON() ([]byte, error) {
	o := NewObject().Put("id", ci.ID)
	if ci.CartID != nil {
		o.Put("cart_id", *ci.CartID)
	} else {
		o.Put("cart_id", nil)
	}
	if ci.ProductID != nil {
		o.Put("product_id", *ci.ProductID)
	} else {
		o.Put("product_id", nil)
	}
	o.Put("quantity", ci.Quantity).
		Put("customization", strOrNil(ci.Customization)).
		Put("flour_type", strOrNil(ci.FlourType)).
		Put("sweetener_percent", ci.SweetenerPercent).
		Put("sweetener_type", strOrNil(ci.SweetenerType))
	return json.Marshal(o)
}

func (o Order) MarshalJSON() ([]byte, error) {
	out := NewObject().Put("id", o.ID)
	if o.UserID != nil {
		out.Put("user_id", *o.UserID)
	} else {
		out.Put("user_id", nil)
	}
	out.Put("total_amount", o.TotalAmount).
		Put("status", strOrNil(o.Status)).
		Put("created_at", isoOrNil(o.CreatedAt)).
		Put("channel", strOrNil(o.Channel)).
		Put("kitchen_status", strOrNil(o.KitchenStatus)).
		Put("customer_notes", strOrNil(o.CustomerNotes)).
		Put("kitchen_notes", strOrNil(o.KitchenNotes)).
		Put("approval_notes", strOrNil(o.ApprovalNotes)).
		Put("approval_status", strOrNil(o.ApprovalStatus)).
		Put("approved_at", isoOrNil(o.ApprovedAt)).
		Put("requires_approval", o.RequiresApproval)
	if o.ApprovedByUserID != nil {
		out.Put("approved_by_user_id", *o.ApprovedByUserID)
	} else {
		out.Put("approved_by_user_id", nil)
	}
	return json.Marshal(out)
}

func (oi OrderItem) MarshalJSON() ([]byte, error) {
	out := NewObject().Put("id", oi.ID)
	if oi.OrderID != nil {
		out.Put("order_id", *oi.OrderID)
	} else {
		out.Put("order_id", nil)
	}
	if oi.ProductID != nil {
		out.Put("product_id", *oi.ProductID)
	} else {
		out.Put("product_id", nil)
	}
	out.Put("quantity", oi.Quantity).
		Put("price", oi.Price).
		Put("customization", strOrNil(oi.Customization)).
		Put("flour_type", strOrNil(oi.FlourType)).
		Put("sweetener_percent", oi.SweetenerPercent).
		Put("sweetener_type", strOrNil(oi.SweetenerType))
	return json.Marshal(out)
}

func (p Payment) MarshalJSON() ([]byte, error) {
	out := NewObject().Put("id", p.ID)
	if p.OrderID != nil {
		out.Put("order_id", *p.OrderID)
	} else {
		out.Put("order_id", nil)
	}
	out.Put("payment_method", strOrNil(p.PaymentMethod)).
		Put("status", strOrNil(p.Status)).
		Put("amount", p.Amount).
		Put("created_at", isoOrNil(p.CreatedAt))
	return json.Marshal(out)
}

func (sa ShippingAddress) MarshalJSON() ([]byte, error) {
	out := NewObject().Put("id", sa.ID).
		Put("address_type", strOrNil(sa.AddressType)).
		Put("city", strOrNil(sa.City)).
		Put("country", strOrNil(sa.Country)).
		Put("created_at", isoOrNil(sa.CreatedAt)).
		Put("full_name", strOrNil(sa.FullName)).
		Put("instructions", strOrNil(sa.Instructions)).
		Put("landmark", strOrNil(sa.Landmark)).
		Put("line1", strOrNil(sa.Line1)).
		Put("line2", strOrNil(sa.Line2)).
		Put("phone", strOrNil(sa.Phone)).
		Put("pincode", strOrNil(sa.Pincode)).
		Put("state", strOrNil(sa.State))
	if sa.OrderID != nil {
		out.Put("order_id", *sa.OrderID)
	} else {
		out.Put("order_id", nil)
	}
	if sa.UserID != nil {
		out.Put("user_id", *sa.UserID)
	} else {
		out.Put("user_id", nil)
	}
	return json.Marshal(out)
}

// stringID lets us key Object by an int as if it were a JSON object key.
func stringID(id int64) string { return strconv.FormatInt(id, 10) }
