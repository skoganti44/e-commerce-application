package main

import (
	"log"
	"os"
	"strconv"
	"time"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
)

type App struct {
	DB     *DB
	JWT    *JwtUtil
	Router *gin.Engine
}

func main() {
	cfg := loadConfig()

	db, err := OpenDB(cfg.DBUrl)
	if err != nil {
		log.Fatalf("db open: %v", err)
	}
	defer db.Close()

	if err := db.EnsureSchema(cfg.SQLDir); err != nil {
		log.Fatalf("db schema: %v", err)
	}

	jwt := NewJwtUtil(cfg.JwtSecret, cfg.JwtExpirationMs)

	app := &App{DB: db, JWT: jwt}

	r := gin.Default()
	r.Use(cors.New(cors.Config{
		AllowOrigins:     []string{"http://localhost:5173"},
		AllowMethods:     []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowHeaders:     []string{"*"},
		ExposeHeaders:    []string{"Authorization"},
		AllowCredentials: true,
		MaxAge:           12 * time.Hour,
	}))
	r.Use(JwtMiddleware(jwt))
	r.Use(AuthGate())

	app.Router = r
	app.RegisterRoutes()

	port := cfg.Port
	log.Printf("E_Commerce_Website_Go listening on :%s", port)
	if err := r.Run(":" + port); err != nil {
		log.Fatal(err)
	}
}

type Config struct {
	DBUrl           string
	JwtSecret       string
	JwtExpirationMs int64
	Port            string
	SQLDir          string
}

func loadConfig() Config {
	c := Config{
		DBUrl:           getenv("DB_URL", "postgres://postgres:Disney%401701@localhost:5432/ecommercedb?sslmode=disable"),
		JwtSecret:       getenv("JWT_SECRET", "ChangeThisToALongRandomStringAtLeast32CharsLong!!"),
		JwtExpirationMs: 86400000,
		Port:            getenv("PORT", "8080"),
		SQLDir:          getenv("SQL_DIR", "sql"),
	}
	if v := os.Getenv("JWT_EXPIRATION_MS"); v != "" {
		if n, err := strconv.ParseInt(v, 10, 64); err == nil {
			c.JwtExpirationMs = n
		}
	}
	return c
}

func getenv(k, def string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return def
}
