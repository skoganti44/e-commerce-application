# E_Commerce_Website_Go

Go re-implementation of the Java Spring Boot service in
[../E_Commerce_Website](../E_Commerce_Website). It exposes the same 63 HTTP
endpoints, talks to the same Postgres schema, and issues compatible JWTs so
the existing [../ecommerce-frontend](../ecommerce-frontend) can point at this
service unchanged by switching the API base URL.

---

## Quick start (verified working)

These are the exact steps used to bring the service up successfully on
Windows 11 / PowerShell. They reuse the existing `ecommercedb` Postgres that
the Java app is already populating.

### 1. Install Go

Download Go 1.22 or newer from <https://go.dev/dl/> and run the installer.
Open a **new** PowerShell window and verify:

```powershell
go version
# go version go1.26.x windows/amd64
```

### 2. cd into the project

```powershell
cd "d:\Sonika\DA udemy\JAVA\e-commerce application\E_Commerce_Website_Go"
```

### 3. Download dependencies (first time only)

```powershell
go mod tidy
```

This pulls Gin, lib/pq, golang-jwt, shopspring/decimal, and their transitive
deps. Takes ~30 s on a clean cache.

### 4. Set environment variables

```powershell
$env:DB_URL     = "postgres://postgres:Disney%401701@localhost:5432/ecommercedb?sslmode=disable"
$env:JWT_SECRET = "ChangeThisToALongRandomStringAtLeast32CharsLong!!"
$env:PORT       = "8081"          # 8080 is taken by the Java app
$env:GIN_MODE   = "release"       # quieter logs; omit for verbose
```

> The `@` in the password must be URL-encoded as `%40` inside `DB_URL`.

> **Important:** the Java service also defaults to `:8080`. Either stop it
> first, or use `PORT=8081` like above so both can run side-by-side.

### 5. Build and run

Two ways; pick one:

```powershell
# A. compile then run the binary
go build .
.\e_commerce_website_go.exe

# B. compile + run in a single step (handy during development)
go run .
```

You should see logs like:

```
DB: connected (postgres://postgres:***@localhost:5432/ecommercedb?sslmode=disable)
DB: schema looks good (core tables present)
E_Commerce_Website_Go listening on :8081
```

### 6. Smoke-test the API

In another PowerShell window:

```powershell
# 6a. Public endpoint - no auth required
Invoke-RestMethod -Uri http://localhost:8081/products

# 6b. Login as a seeded user (alice@example.com / Abc@1234 from sql/02_data.sql)
$login = Invoke-RestMethod `
    -Uri http://localhost:8081/login `
    -Method POST `
    -ContentType "application/json" `
    -Body '{"email":"alice@example.com","password":"Abc@1234"}'
$token = $login.token
$login.user        # -> { userid, name, email, roles, departments, ... }

# 6c. Authenticated endpoint
Invoke-RestMethod `
    -Uri http://localhost:8081/users `
    -Headers @{ Authorization = "Bearer $token" }
```

If step 6a returns a JSON array and step 6c returns the user list, you're
fully wired up.

### 7. Stop the server

In the window running the server: `Ctrl+C`.

If it's running detached, find and kill it by port:

```powershell
$pid = (Get-NetTCPConnection -LocalPort 8081).OwningProcess
Stop-Process -Id $pid
```

---

## Running with Docker (alternative)

No local Go install or local Postgres needed — Docker handles both.

```powershell
cd "d:\Sonika\DA udemy\JAVA\e-commerce application\E_Commerce_Website_Go"
docker compose up --build
```

This brings up Postgres 16 (seeded from `./sql/` on first boot) plus the Go
app on `http://localhost:8080`. Stop with `Ctrl+C`. Wipe the seeded data with
`docker compose down -v` so the next start re-initializes.

---

## Stack

- Go 1.22 + [Gin](https://github.com/gin-gonic/gin) HTTP router
- `database/sql` + [`lib/pq`](https://github.com/lib/pq) for Postgres
- [`golang-jwt/jwt`](https://github.com/golang-jwt/jwt) (HS256, mirrors the
  Spring filter)
- [`shopspring/decimal`](https://github.com/shopspring/decimal) for money math
- CORS via `gin-contrib/cors` (default origin `http://localhost:5173`)

## File layout

```
main.go                  bootstrap, config, router wiring
db.go                    Postgres pool, ping retry, optional schema bootstrap
models.go                entity structs that map to Postgres tables
jwt.go                   JWT util + auth middleware (mirrors JwtUtil/JwtFilter)
routes.go                all 63 route registrations
helpers.go               JSON helpers, type coercion, validation, ordered Object
pricing.go               sweetener / flour add-ons (matches UserService)
store_*.go               raw-SQL data access, one file per aggregate
handlers_*.go            HTTP handlers, one file per domain
json_marshalers.go       custom MarshalJSON for entity types
sql/                     schema and seed data (self-contained copies)
Dockerfile               multi-stage build to a static binary
docker-compose.yml       app + Postgres with auto-seeded volume
```

## Database

The Go service is self-contained: schema and seed data live in
[./sql/](./sql/), so this folder doesn't reach into the Java project at
runtime.

```
sql/
  01_schema.sql               core tables, sequences, FKs
  02_data.sql                 baseline seed (users, roles, products)
  kitchen_seed.sql            kitchen demo data
  discount_cakes_cookies.sql  category-level discount sample
```

On startup the app:

1. Opens a `database/sql` pool (`lib/pq`), tuned to 20 max connections.
2. Pings the DB up to 30 times (2 s apart) — survives a slow Postgres start.
3. Probes for the core tables (`User`, `Role`, `Products`).
   - Present → continue.
   - Missing **and** `AUTO_INIT_DB=true` → executes every `*.sql` in
     `SQL_DIR` (default `sql/`) in lexical order.
   - Missing **and** `AUTO_INIT_DB` not set → logs a warning with the psql
     command to run, then continues so you can inspect the DB.
4. Logs a redacted form of the connection URL (password masked).

To bootstrap an empty database from the local SQL files:

```powershell
$env:AUTO_INIT_DB = "true"
go run .
# logs "DB: applying sql/01_schema.sql" then 02_data.sql, etc.
# unset after the first run
Remove-Item Env:\AUTO_INIT_DB
```

## Environment

| Variable | Default | Notes |
| --- | --- | --- |
| `DB_URL` | `postgres://postgres:Disney%401701@localhost:5432/ecommercedb?sslmode=disable` | URL-encoded password (`@` → `%40`) |
| `JWT_SECRET` | `ChangeThisToALongRandomStringAtLeast32CharsLong!!` | Same default as the Java service |
| `JWT_EXPIRATION_MS` | `86400000` | 24 hours |
| `PORT` | `8080` | HTTP listen port |
| `SQL_DIR` | `sql` | Directory of init `*.sql` files |
| `AUTO_INIT_DB` | _unset_ | Set to `true` to auto-apply `SQL_DIR/*.sql` when core tables are missing |
| `GIN_MODE` | _unset_ | Set to `release` for quieter framework logs |

## Endpoints (63 total — mirror Java `Controller.java`)

Auth & users
- `POST /register`
- `POST /login`
- `GET /users`
- `GET /roles?department=`
- `GET /userRoles?userid=&roleid=`

Products
- `GET /products` (public)
- `POST /product`
- `POST /products`

Cart
- `GET /cart?userid=`
- `POST /cart/add`
- `POST /cart/item/update`
- `DELETE /cart/item?userid=&itemId=`

Shipping & checkout
- `POST /shipping-address`
- `GET /shipping-address?userid=`
- `POST /checkout`
- `POST /counter/sale`

Orders & payments
- `GET /orders?userid=`
- `GET /payments?userid=&includeAll=`
- `DELETE /cleanup?userid=`

Sales
- `GET /sales/analytics?from=&to=`

Kitchen
- `GET /kitchen/online-orders`
- `GET /kitchen/instore-orders`
- `GET /kitchen/daily-stock`
- `GET /delivery/online-orders`
- `GET /bakery/instore-orders`
- `POST /kitchen/order/{orderId}/status`
- `POST /kitchen/daily-stock/{stockId}/adjust`
- `GET /kitchen/supplies`
- `GET /kitchen/in-stock`
- `POST /kitchen/supplies`
- `POST /kitchen/supplies/{supplyId}/request`
- `POST /kitchen/supplies/{supplyId}/adjust`
- `POST /kitchen/supplies/bulk-status`
- `POST /kitchen/supplies/seed`

Tasks
- `POST /tasks`
- `GET /tasks?department=&createdByUserId=&status=`
- `POST /tasks/{taskId}/status`

Delivery
- `POST /delivery/trips`
- `POST /delivery/trips/{tripId}/out`
- `POST /delivery/trips/{tripId}/deliver`
- `POST /delivery/trips/{tripId}/fail`
- `GET /delivery/trips?driverId=&status=`
- `POST /delivery/issues`
- `GET /delivery/issues?driverId=`
- `GET /delivery/summary?driverId=&from=&to=`

Supplies (cross-team)
- `POST /supplies/{supplyId}/request`

Management
- `GET /management/ops`
- `GET /management/orders-audit?from=&to=&channel=&paymentMethod=`
- `GET /management/deliveries-audit?from=&to=&driverId=&status=`
- `GET /management/day-pnl?date=`
- `GET /management/staff-performance?from=&to=`
- `GET /management/cash-reconciliation?date=&openingFloat=&countedCash=`
- `GET /management/supply-requests`
- `POST /management/supplies/{supplyId}/fulfill`

Order approvals
- `POST /orders/{orderId}/flag-approval`
- `GET /orders/pending-approval`
- `POST /orders/{orderId}/approval-decision`

Refunds
- `POST /refund-requests`
- `GET /refund-requests?status=`
- `POST /refund-requests/{id}/decision`

Discount campaigns
- `POST /discount-campaigns`
- `GET /discount-campaigns?status=`
- `POST /discount-campaigns/{id}/decision`

## Differences from the Java service

- Passwords are stored and compared as plaintext, matching the existing Java
  behaviour and the seeded data in `02_data.sql`. Do not deploy without
  switching to bcrypt/argon2.
- JWT format: HS256 with claims `{sub, userid, roles, departments, iat, exp}` —
  byte-for-byte compatible with the Spring `JwtUtil`.
- Unauthenticated requests get a `401` JSON body (`{"error":"unauthorized"}`)
  instead of the empty Spring response.
- Some entity-level JSON keys use `snake_case` because they map directly to
  Postgres column names (the Java side relies on JPA getters and produces a
  similar shape). The aggregated/composite responses (`/cart`, `/kitchen/*`,
  `/management/*`) match the Java field names exactly.

## Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| `go: command not found` | Go isn't installed, or this terminal predates the install. Install from <https://go.dev/dl/> and open a new PowerShell. |
| `pq: password authentication failed` | The `@` in `Disney@1701` must be URL-encoded as `%40` inside `DB_URL`. |
| `bind: address already in use` on `:8080` | The Java app is already running. Set `$env:PORT = "8081"`. |
| `DB: ping attempt 1/30 failed` (loop) | Postgres isn't up yet, wrong host/port, or wrong credentials. The app retries for ~60 s; fix the URL and it'll connect. |
| `core tables missing` | Either point at the existing `ecommercedb`, or set `$env:AUTO_INIT_DB = "true"` before `go run .` to apply `sql/*.sql`. |
| Browser frontend gets CORS errors | The app allows `http://localhost:5173` by default. Edit the `cors.Config` in [main.go](main.go) for other origins. |
| `401 unauthorized` on every protected endpoint | Login first via `POST /login`, then send the returned token as `Authorization: Bearer <token>`. |
