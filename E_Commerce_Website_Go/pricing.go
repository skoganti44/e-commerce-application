package main

import "github.com/shopspring/decimal"

// Mirrors the pricing table in UserService.java.

var allowedSweeteners = newStrSet("CANE_SUGAR", "BROWN_SUGAR", "MAPLE_SYRUP", "JAGGERY", "HONEY")
var allowedFlours = newStrSet("FINGER_MILLET", "BAJRA_MILLET", "LITTLE_MILLET",
	"SORGHUM", "WHOLE_WHEAT", "ALL_PURPOSE")

var sweetenerAddon = map[string]decimal.Decimal{
	"CANE_SUGAR":  decimal.NewFromInt(1),
	"BROWN_SUGAR": decimal.NewFromInt(1),
	"JAGGERY":     decimal.NewFromInt(2),
	"MAPLE_SYRUP": decimal.NewFromInt(3),
	"HONEY":       decimal.NewFromInt(3),
}

var flourAddon = map[string]decimal.Decimal{
	"ALL_PURPOSE":   decimal.NewFromInt(1),
	"WHOLE_WHEAT":   decimal.NewFromInt(2),
	"FINGER_MILLET": decimal.NewFromInt(5),
	"BAJRA_MILLET":  decimal.NewFromInt(5),
	"LITTLE_MILLET": decimal.NewFromInt(5),
	"SORGHUM":       decimal.NewFromInt(5),
}

func addon(table map[string]decimal.Decimal, code *string) decimal.Decimal {
	if code == nil {
		return decimal.Zero
	}
	if v, ok := table[*code]; ok {
		return v
	}
	return decimal.Zero
}

func computeUnitPrice(p *Product, sweetener, flour *string) decimal.Decimal {
	base := decimal.Zero
	if p != nil && p.Price != nil {
		base = *p.Price
	}
	return base.Add(addon(sweetenerAddon, sweetener)).Add(addon(flourAddon, flour))
}
