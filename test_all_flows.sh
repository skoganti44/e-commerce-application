#!/bin/bash
# Verify ALL positive flows. A response counts as PASS when:
#   • it starts with [ or { (valid JSON), AND
#   • does not contain "error":  / "trace": / "status":4 / "status":5

BASE="http://localhost:8080"
PASS=0
FAIL=0
declare -a OK
declare -a NOK

ck() {
  local label="$1"
  local body="$2"
  local first
  first="$(printf '%s' "$body" | head -c 1)"
  if [ "$first" != "[" ] && [ "$first" != "{" ]; then
    NOK+=("FAIL | $label | not JSON: $(printf '%s' "$body" | head -c 120)")
    FAIL=$((FAIL+1)); return
  fi
  if printf '%s' "$body" | grep -q '"error":\|"trace":\|"status":[45][0-9][0-9]'; then
    NOK+=("FAIL | $label | error JSON: $(printf '%s' "$body" | head -c 160)")
    FAIL=$((FAIL+1)); return
  fi
  OK+=("PASS | $label")
  PASS=$((PASS+1))
}

echo "============================================================"
echo " POSITIVE-FLOW TEST — every role, against live backend"
echo "============================================================"

# ---------- 1. AUTH ----------
echo "[1] AUTH — login each role"
declare -A IDS
for cred in \
  "customer@dhati.local|Customer@123|customer" \
  "bakery@dhati.local|Bakery@123|bakery" \
  "kitchen@dhati.local|Kitchen@123|kitchen" \
  "sales@dhati.local|Sales@123|sales" \
  "delivery@dhati.local|Delivery@123|delivery" \
  "management@dhati.local|Manage@123|management" \
  "admin@dhati.local|Admin@123|admin"
do
  IFS='|' read -r email pw label <<< "$cred"
  resp=$(curl -s -X POST "$BASE/login" -H "Content-Type: application/json" \
         -d "{\"email\":\"$email\",\"password\":\"$pw\"}")
  ck "Login: $label ($email)" "$resp"
  uid=$(printf '%s' "$resp" | grep -oE '"userid":[0-9]+' | head -1 | grep -oE '[0-9]+')
  IDS[$label]=$uid
done

CUSTOMER_ID=${IDS[customer]}
DRIVER_ID=${IDS[delivery]}
MGR_ID=${IDS[management]}
SALES_ID=${IDS[sales]}
echo "    customer=$CUSTOMER_ID driver=$DRIVER_ID manager=$MGR_ID sales=$SALES_ID"

# ---------- 2. CUSTOMER ----------
echo "[2] CUSTOMER flow"
ck "GET /products"                        "$(curl -s $BASE/products -H 'Accept: application/json')"
ck "GET /roles"                           "$(curl -s $BASE/roles -H 'Accept: application/json')"
ck "GET /users"                           "$(curl -s $BASE/users -H 'Accept: application/json')"

# Pick a real customer id that has orders (Alice = userid 1 from seed). We'll find any user that has orders.
# Try userid 1 first (Alice from seeded delivery scenarios) — those orders persisted.
ck "GET /orders?userid=1"                 "$(curl -s "$BASE/orders?userid=1" -H 'Accept: application/json')"
ck "GET /orders?userid=$CUSTOMER_ID"      "$(curl -s "$BASE/orders?userid=$CUSTOMER_ID" -H 'Accept: application/json')"
ck "GET /payments?userid=1"               "$(curl -s "$BASE/payments?userid=1" -H 'Accept: application/json')"
ck "GET /shipping-address?userid=1"       "$(curl -s "$BASE/shipping-address?userid=1" -H 'Accept: application/json')"

# Cart: customer 16 may not have cart yet — that's expected for a fresh customer, not a bug.
# Test by creating one: add an item.
PROD=$(curl -s "$BASE/products" -H 'Accept: application/json')
PROD_ID=$(printf '%s' "$PROD" | grep -oE '"id":[0-9]+' | head -1 | grep -oE '[0-9]+')
echo "    using productId=$PROD_ID for cart-add test"
CART_ADD=$(curl -s -X POST "$BASE/cart/add" -H 'Content-Type: application/json' \
  -d "{\"userid\":$CUSTOMER_ID,\"productId\":$PROD_ID,\"quantity\":1}")
ck "POST /cart/add (customer)"            "$CART_ADD"
ck "GET /cart?userid=$CUSTOMER_ID after add" "$(curl -s "$BASE/cart?userid=$CUSTOMER_ID" -H 'Accept: application/json')"

# ---------- 3. KITCHEN ----------
echo "[3] KITCHEN flows"
ck "GET /kitchen/online-orders"           "$(curl -s $BASE/kitchen/online-orders -H 'Accept: application/json')"
ck "GET /kitchen/instore-orders"          "$(curl -s $BASE/kitchen/instore-orders -H 'Accept: application/json')"
ck "GET /kitchen/daily-stock"             "$(curl -s $BASE/kitchen/daily-stock -H 'Accept: application/json')"
ck "GET /kitchen/supplies"                "$(curl -s $BASE/kitchen/supplies -H 'Accept: application/json')"
ck "GET /kitchen/in-stock"                "$(curl -s $BASE/kitchen/in-stock -H 'Accept: application/json')"

# ---------- 4. BAKERY ----------
echo "[4] BAKERY flow"
ck "GET /bakery/instore-orders"           "$(curl -s $BASE/bakery/instore-orders -H 'Accept: application/json')"

# ---------- 5. SALES ----------
echo "[5] SALES flows"
ck "GET /sales/analytics"                 "$(curl -s $BASE/sales/analytics)"
ck "GET /tasks"                           "$(curl -s $BASE/tasks)"
ck "GET /tasks?department=kitchen"        "$(curl -s "$BASE/tasks?department=kitchen")"
ck "GET /tasks?createdByUserId=$SALES_ID" "$(curl -s "$BASE/tasks?createdByUserId=$SALES_ID")"

# ---------- 6. DELIVERY ----------
echo "[6] DELIVERY flows"
ck "GET /delivery/online-orders"          "$(curl -s $BASE/delivery/online-orders -H 'Accept: application/json')"
ck "GET /delivery/trips?driverId=$DRIVER_ID" "$(curl -s "$BASE/delivery/trips?driverId=$DRIVER_ID")"
ck "GET /delivery/issues?driverId=$DRIVER_ID" "$(curl -s "$BASE/delivery/issues?driverId=$DRIVER_ID")"
ck "GET /delivery/summary?driverUserId=$DRIVER_ID" "$(curl -s "$BASE/delivery/summary?driverUserId=$DRIVER_ID")"

# ---------- 7. MANAGEMENT ----------
echo "[7] MANAGEMENT flows"
ck "GET /management/ops"                  "$(curl -s $BASE/management/ops)"
ck "GET /management/orders-audit"         "$(curl -s $BASE/management/orders-audit)"
ck "GET /management/deliveries-audit"     "$(curl -s $BASE/management/deliveries-audit)"
ck "GET /management/day-pnl"              "$(curl -s $BASE/management/day-pnl)"
ck "GET /management/cash-reconciliation"  "$(curl -s $BASE/management/cash-reconciliation)"
ck "GET /management/staff-performance"    "$(curl -s $BASE/management/staff-performance)"
ck "GET /management/supply-requests"      "$(curl -s $BASE/management/supply-requests)"
ck "GET /orders/pending-approval"         "$(curl -s $BASE/orders/pending-approval)"
ck "GET /refund-requests"                 "$(curl -s $BASE/refund-requests)"
ck "GET /discount-campaigns"              "$(curl -s $BASE/discount-campaigns)"

# ---------- summary ----------
echo
echo "============================================================"
echo " RESULT — PASS=$PASS  FAIL=$FAIL"
echo "============================================================"
for r in "${OK[@]}";  do echo " $r"; done
for r in "${NOK[@]}"; do echo " $r"; done
echo
if [ "$FAIL" -eq 0 ]; then echo "ALL POSITIVE FLOWS WORKING ✓"; else echo "$FAIL FLOW(S) FAILED ✗"; fi
