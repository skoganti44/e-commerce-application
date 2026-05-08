package main

import (
	"database/sql"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/shopspring/decimal"
)

// JSON helper types --------------------------------------------------------

// Object wraps map[string]any to preserve the LinkedHashMap-like ordering used by
// the Java service. Since map iteration is unordered in Go, we use a slice of
// key/value pairs and emit them in insertion order via custom MarshalJSON.
type Object struct {
	keys   []string
	values map[string]any
}

func NewObject() *Object {
	return &Object{values: map[string]any{}}
}

func (o *Object) Put(k string, v any) *Object {
	if _, ok := o.values[k]; !ok {
		o.keys = append(o.keys, k)
	}
	o.values[k] = v
	return o
}

func (o *Object) Get(k string) any { return o.values[k] }

func (o *Object) MarshalJSON() ([]byte, error) {
	var b strings.Builder
	b.WriteByte('{')
	for i, k := range o.keys {
		if i > 0 {
			b.WriteByte(',')
		}
		kj, _ := json.Marshal(k)
		b.Write(kj)
		b.WriteByte(':')
		vj, err := json.Marshal(o.values[k])
		if err != nil {
			return nil, err
		}
		b.Write(vj)
	}
	b.WriteByte('}')
	return []byte(b.String()), nil
}

// Error helpers ------------------------------------------------------------

type httpErr struct {
	status int
	msg    string
}

func (e *httpErr) Error() string { return e.msg }
func badRequest(msg string) error { return &httpErr{status: 400, msg: msg} }
func notFound(msg string) error   { return &httpErr{status: 404, msg: msg} }
func conflict(msg string) error   { return &httpErr{status: 409, msg: msg} }
func forbidden(msg string) error  { return &httpErr{status: 403, msg: msg} }
func unauthorized(msg string) error { return &httpErr{status: 401, msg: msg} }

func writeError(c *gin.Context, err error) {
	if he, ok := err.(*httpErr); ok {
		c.JSON(he.status, gin.H{"error": he.msg})
		return
	}
	c.JSON(http.StatusInternalServerError, gin.H{"error": err.Error()})
}

// Body parsing helpers -----------------------------------------------------

func bindJSON(c *gin.Context, dst any) error {
	if err := c.ShouldBindJSON(dst); err != nil {
		return badRequest("invalid request body: " + err.Error())
	}
	return nil
}

func bindGenericMap(c *gin.Context) (map[string]any, error) {
	var body map[string]any
	if c.Request.ContentLength == 0 {
		return map[string]any{}, nil
	}
	if err := c.ShouldBindJSON(&body); err != nil {
		if errors.Is(err, io.EOF) {
			return map[string]any{}, nil
		}
		return nil, badRequest("invalid request body: " + err.Error())
	}
	if body == nil {
		body = map[string]any{}
	}
	return body, nil
}

// Conversions --------------------------------------------------------------

func asInt(v any) (int, bool) {
	switch x := v.(type) {
	case nil:
		return 0, false
	case int:
		return x, true
	case int64:
		return int(x), true
	case float64:
		return int(x), true
	case json.Number:
		n, err := x.Int64()
		if err == nil {
			return int(n), true
		}
		return 0, false
	case string:
		s := strings.TrimSpace(x)
		if s == "" {
			return 0, false
		}
		n, err := strconv.Atoi(s)
		if err != nil {
			return 0, false
		}
		return n, true
	}
	return 0, false
}

func asInt64(v any) (int64, bool) {
	switch x := v.(type) {
	case nil:
		return 0, false
	case int:
		return int64(x), true
	case int64:
		return x, true
	case float64:
		return int64(x), true
	case json.Number:
		n, err := x.Int64()
		return n, err == nil
	case string:
		s := strings.TrimSpace(x)
		if s == "" {
			return 0, false
		}
		n, err := strconv.ParseInt(s, 10, 64)
		return n, err == nil
	}
	return 0, false
}

func asString(v any) (string, bool) {
	if v == nil {
		return "", false
	}
	if s, ok := v.(string); ok {
		return s, true
	}
	return fmt.Sprintf("%v", v), true
}

func trimToNil(v any) *string {
	if v == nil {
		return nil
	}
	s := strings.TrimSpace(fmt.Sprintf("%v", v))
	if s == "" {
		return nil
	}
	return &s
}

func trimToEmpty(v any) string {
	p := trimToNil(v)
	if p == nil {
		return ""
	}
	return *p
}

func toDecimal(v any) (*decimal.Decimal, error) {
	if v == nil {
		return nil, nil
	}
	switch x := v.(type) {
	case decimal.Decimal:
		return &x, nil
	case float64:
		d := decimal.NewFromFloat(x)
		return &d, nil
	case int:
		d := decimal.NewFromInt(int64(x))
		return &d, nil
	case int64:
		d := decimal.NewFromInt(x)
		return &d, nil
	case json.Number:
		d, err := decimal.NewFromString(string(x))
		if err != nil {
			return nil, badRequest("Invalid number: " + string(x))
		}
		return &d, nil
	case string:
		s := strings.TrimSpace(x)
		if s == "" {
			return nil, nil
		}
		d, err := decimal.NewFromString(s)
		if err != nil {
			return nil, badRequest("Invalid number: " + s)
		}
		return &d, nil
	}
	return nil, badRequest(fmt.Sprintf("Invalid number: %v", v))
}

func decOrZero(d *decimal.Decimal) decimal.Decimal {
	if d == nil {
		return decimal.Zero
	}
	return *d
}

// Date parsing -------------------------------------------------------------

func parseDateOrDefault(s string, fallback time.Time, label string) (time.Time, error) {
	if strings.TrimSpace(s) == "" {
		return fallback, nil
	}
	t, err := time.Parse("2006-01-02", strings.TrimSpace(s))
	if err != nil {
		return time.Time{}, badRequest("Invalid '" + label + "' date (expected YYYY-MM-DD)")
	}
	return t, nil
}

func parseDateRange(fromStr, toStr string) (time.Time, time.Time, error) {
	now := startOfDay(time.Now())
	from := now
	to := now
	if strings.TrimSpace(fromStr) != "" {
		t, err := time.Parse("2006-01-02", strings.TrimSpace(fromStr))
		if err != nil {
			return time.Time{}, time.Time{}, badRequest("Dates must be ISO format yyyy-MM-dd")
		}
		from = t
	}
	if strings.TrimSpace(toStr) != "" {
		t, err := time.Parse("2006-01-02", strings.TrimSpace(toStr))
		if err != nil {
			return time.Time{}, time.Time{}, badRequest("Dates must be ISO format yyyy-MM-dd")
		}
		to = t
	} else {
		to = from
	}
	if from.After(to) {
		return time.Time{}, time.Time{}, badRequest("'from' must be on or before 'to'")
	}
	return from, to, nil
}

func startOfDay(t time.Time) time.Time {
	return time.Date(t.Year(), t.Month(), t.Day(), 0, 0, 0, 0, t.Location())
}

func nullTimeFrom(t time.Time) sql.NullTime {
	return sql.NullTime{Time: t, Valid: true}
}

func dateOnly(t time.Time) string { return t.Format("2006-01-02") }

func dateTimeISO(p sql.NullTime) any {
	if !p.Valid {
		return nil
	}
	return p.Time.Format("2006-01-02T15:04:05.000000")
}

func nullTimeOrNil(p sql.NullTime) any {
	return dateTimeISO(p)
}

// Misc ---------------------------------------------------------------------

var phoneRe = regexp.MustCompile(`^\d{10}$`)
var pinRe = regexp.MustCompile(`^\d{5}(-\d{4})?$`)

func capitalize(s string) string {
	if s == "" {
		return ""
	}
	return strings.ToUpper(s[:1]) + s[1:]
}

func dedupLower(in []string) []string {
	seen := map[string]struct{}{}
	out := []string{}
	for _, s := range in {
		s = strings.TrimSpace(strings.ToLower(s))
		if s == "" {
			continue
		}
		if _, ok := seen[s]; ok {
			continue
		}
		seen[s] = struct{}{}
		out = append(out, s)
	}
	return out
}

func normalizeCode(s string) string {
	t := strings.TrimSpace(s)
	if t == "" {
		return ""
	}
	return strings.ToUpper(t)
}

func splitCSV(s string) []string {
	if strings.TrimSpace(s) == "" {
		return []string{}
	}
	parts := strings.Split(s, ",")
	out := []string{}
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p != "" {
			out = append(out, p)
		}
	}
	return out
}

// Path param helper

func pathInt64(c *gin.Context, name string) (int64, error) {
	raw := c.Param(name)
	n, err := strconv.ParseInt(raw, 10, 64)
	if err != nil {
		return 0, badRequest("invalid path parameter: " + name)
	}
	return n, nil
}

func queryInt(c *gin.Context, name string) (int, bool) {
	raw := c.Query(name)
	if raw == "" {
		return 0, false
	}
	n, err := strconv.Atoi(raw)
	if err != nil {
		return 0, false
	}
	return n, true
}

func queryBool(c *gin.Context, name string, def bool) bool {
	raw := strings.ToLower(c.Query(name))
	if raw == "" {
		return def
	}
	return raw == "true" || raw == "1" || raw == "yes"
}

// Set helpers --------------------------------------------------------------

type StrSet map[string]struct{}

func newStrSet(values ...string) StrSet {
	s := StrSet{}
	for _, v := range values {
		s[v] = struct{}{}
	}
	return s
}

func (s StrSet) Has(v string) bool { _, ok := s[v]; return ok }

func (s StrSet) SortedKeys() []string {
	out := make([]string, 0, len(s))
	for k := range s {
		out = append(out, k)
	}
	// stable simple sort
	for i := 1; i < len(out); i++ {
		for j := i; j > 0 && out[j-1] > out[j]; j-- {
			out[j-1], out[j] = out[j], out[j-1]
		}
	}
	return out
}
