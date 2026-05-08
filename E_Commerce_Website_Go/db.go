package main

import (
	"database/sql"
	"errors"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"

	_ "github.com/lib/pq"
)

type DB struct {
	*sql.DB
}

// OpenDB opens a Postgres connection pool, retries the initial ping
// until the database is reachable, and returns the wrapper.
func OpenDB(url string) (*DB, error) {
	conn, err := sql.Open("postgres", url)
	if err != nil {
		return nil, fmt.Errorf("sql.Open: %w", err)
	}
	conn.SetMaxOpenConns(20)
	conn.SetMaxIdleConns(10)
	conn.SetConnMaxLifetime(30 * time.Minute)
	conn.SetConnMaxIdleTime(5 * time.Minute)

	if err := pingWithRetry(conn, 30, 2*time.Second); err != nil {
		_ = conn.Close()
		return nil, err
	}
	log.Printf("DB: connected (%s)", redactURL(url))
	return &DB{conn}, nil
}

func pingWithRetry(conn *sql.DB, attempts int, wait time.Duration) error {
	var lastErr error
	for i := 1; i <= attempts; i++ {
		if err := conn.Ping(); err == nil {
			return nil
		} else {
			lastErr = err
			log.Printf("DB: ping attempt %d/%d failed: %v (retrying in %s)", i, attempts, err, wait)
			time.Sleep(wait)
		}
	}
	return fmt.Errorf("DB: unreachable after %d attempts: %w", attempts, lastErr)
}

// EnsureSchema verifies the core tables exist. If they don't and AUTO_INIT_DB
// is set, it runs the .sql files in sql/ in lexical order. Otherwise it logs
// a warning and lets the caller decide what to do.
func (db *DB) EnsureSchema(sqlDir string) error {
	exists, err := db.coreTablesExist()
	if err != nil {
		return fmt.Errorf("schema probe: %w", err)
	}
	if exists {
		log.Printf("DB: schema looks good (core tables present)")
		return nil
	}

	if os.Getenv("AUTO_INIT_DB") != "true" {
		log.Printf("DB: core tables missing. Set AUTO_INIT_DB=true to run %s/*.sql automatically.",
			sqlDir)
		log.Printf("DB: or psql in:  cat %s/01_schema.sql %s/02_data.sql | psql ...", sqlDir, sqlDir)
		return nil
	}

	files, err := listSQLFiles(sqlDir)
	if err != nil {
		return fmt.Errorf("list sql files: %w", err)
	}
	if len(files) == 0 {
		return fmt.Errorf("AUTO_INIT_DB=true but %s has no .sql files", sqlDir)
	}
	for _, f := range files {
		log.Printf("DB: applying %s", f)
		if err := db.execSQLFile(f); err != nil {
			return fmt.Errorf("apply %s: %w", filepath.Base(f), err)
		}
	}
	log.Printf("DB: schema bootstrap complete")
	return nil
}

func (db *DB) coreTablesExist() (bool, error) {
	const q = `
		SELECT
		  EXISTS (SELECT 1 FROM information_schema.tables
		           WHERE table_schema='public' AND table_name='User') AND
		  EXISTS (SELECT 1 FROM information_schema.tables
		           WHERE table_schema='public' AND table_name='Role') AND
		  EXISTS (SELECT 1 FROM information_schema.tables
		           WHERE table_schema='public' AND table_name='Products')
	`
	var ok bool
	err := db.QueryRow(q).Scan(&ok)
	if err != nil {
		return false, err
	}
	return ok, nil
}

func listSQLFiles(dir string) ([]string, error) {
	if dir == "" {
		return nil, errors.New("sql dir not set")
	}
	entries, err := os.ReadDir(dir)
	if err != nil {
		return nil, err
	}
	out := []string{}
	for _, e := range entries {
		if e.IsDir() {
			continue
		}
		name := e.Name()
		if !strings.HasSuffix(strings.ToLower(name), ".sql") {
			continue
		}
		out = append(out, filepath.Join(dir, name))
	}
	sort.Strings(out)
	return out, nil
}

func (db *DB) execSQLFile(path string) error {
	body, err := os.ReadFile(path)
	if err != nil {
		return err
	}
	// Postgres pq driver supports executing multi-statement scripts directly.
	_, err = db.Exec(string(body))
	return err
}

// redactURL hides the password from logs.
func redactURL(url string) string {
	at := strings.LastIndex(url, "@")
	if at < 0 {
		return url
	}
	scheme := strings.Index(url, "://")
	if scheme < 0 {
		return url
	}
	creds := url[scheme+3 : at]
	colon := strings.Index(creds, ":")
	if colon < 0 {
		return url
	}
	user := creds[:colon]
	return url[:scheme+3] + user + ":***" + url[at:]
}

// nullStr / nullableTime live near the connection because the store files
// use them as scan-safe wrappers around optional values.

func nullStr(p *string) interface{} {
	if p == nil {
		return nil
	}
	return *p
}

func nullInt(p *int) interface{} {
	if p == nil {
		return nil
	}
	return *p
}

func nullInt64(p *int64) interface{} {
	if p == nil {
		return nil
	}
	return *p
}
