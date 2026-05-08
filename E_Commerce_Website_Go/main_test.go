package main

import (
	"log"
	"os"
	"testing"

	"github.com/gin-gonic/gin"
)

// testApp is initialized in TestMain and shared across the suite.
var testApp *App

// TestMain wires the app once for the whole package: open DB, build router,
// register routes. We do not start a real HTTP server — handlers are exercised
// via httptest.NewRecorder against testApp.Router.ServeHTTP.
//
// Tests require a reachable Postgres with the existing ecommercedb schema.
// Override the connection via DB_URL.
func TestMain(m *testing.M) {
	gin.SetMode(gin.TestMode)

	cfg := loadConfig()
	db, err := OpenDB(cfg.DBUrl)
	if err != nil {
		log.Printf("SKIP: cannot open DB at %s: %v", cfg.DBUrl, err)
		log.Printf("SKIP: set DB_URL to a reachable Postgres with the ecommercedb schema to run tests")
		os.Exit(0)
	}
	defer db.Close()

	if err := db.EnsureSchema(cfg.SQLDir); err != nil {
		log.Fatalf("test bootstrap: %v", err)
	}

	jwt := NewJwtUtil(cfg.JwtSecret, cfg.JwtExpirationMs)
	r := gin.New()
	r.Use(JwtMiddleware(jwt))
	r.Use(AuthGate())
	testApp = &App{DB: db, JWT: jwt, Router: r}
	testApp.RegisterRoutes()

	os.Exit(m.Run())
}
