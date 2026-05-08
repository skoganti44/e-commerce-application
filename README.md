# e-commerce-application

An online grocery / bakery platform: Postgres + two interchangeable backends
(Java Spring Boot, Go) sharing one schema, with a React frontend in a separate
repo.

## Components

| Folder | What it is |
| --- | --- |
| [postgres-db/](postgres-db/) | **Canonical database**: Postgres 16 + pgAdmin 4, schema and seed in `init/`, helper scripts for backup/restore/reset. Recommended starting point. |
| [E_Commerce_Website/](E_Commerce_Website/) | Java Spring Boot REST API. 63 endpoints, JWT auth. |
| [E_Commerce_Website_Go/](E_Commerce_Website_Go/) | Go re-implementation of the same 63 endpoints, JWT-compatible with the Java service. 183 integration tests. |
| `ecommerce-frontend/` | React (Vite) frontend — lives in [its own repo](https://github.com/skoganti44/ecommerce-frontend). |

The two backends are functionally interchangeable from the frontend's
perspective — point its API base URL at either one. Run **only one**
Postgres at a time on `:5432` (the shared one in `postgres-db/` is the
recommended option).

## Quick start

```powershell
# 1. database (Postgres + pgAdmin on http://localhost:5050)
cd postgres-db
.\scripts\up.ps1

# 2. pick a backend — Java OR Go, not both
cd ..\E_Commerce_Website_Go
$env:DB_URL = "postgres://postgres:Disney%401701@localhost:5432/ecommercedb?sslmode=disable"
$env:PORT   = "8081"
go run .

# 3. frontend (separate repo)
# cd path\to\ecommerce-frontend; npm install; npm run dev
```

See each folder's README for full setup, env vars, and troubleshooting.

## Docker reference

```bash
docker --version
docker ps                       # list running containers
docker compose up --build       # build + start a stack
docker compose down             # stop
docker compose down -v          # stop + drop volumes (wipe data)
docker compose logs -f          # tail all logs
docker compose logs -f db       # tail one service
docker images                   # list downloaded images
```