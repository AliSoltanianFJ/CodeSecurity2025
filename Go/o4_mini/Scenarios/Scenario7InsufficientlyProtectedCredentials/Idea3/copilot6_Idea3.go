package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "strings"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
    mysqlErr "github.com/go-sql-driver/mysql"
)

var db *sql.DB

func main() {
    // load your DSN from env or config
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("MYSQL_DSN not set")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // set sensible limits on the connection pool
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Ping to catch bad credentials / downed server at startup
    if err = db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    // wrap your default mux in a server with timeouts
    srv := &http.Server{
        Addr:         ":8080",
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 10 * time.Second,
        IdleTimeout:  120 * time.Second,
        Handler:      http.DefaultServeMux,
    }

    http.HandleFunc("/register", registerHandler)

    log.Printf("Listening on %s…", srv.Addr)
    if err := srv.ListenAndServe(); err != nil && err != http.ErrServerClosed {
        log.Fatalf("ListenAndServe: %v", err)
    }
}

type apiResponse struct {
    Error   string `json:"error,omitempty"`
    Message string `json:"message,omitempty"`
}

func writeJSON(w http.ResponseWriter, status int, resp apiResponse) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(resp)
}

func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        writeJSON(w, http.StatusMethodNotAllowed, apiResponse{Error: "method not allowed"})
        return
    }

    // protect from overly large bodies (e.g. megabytes)
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB
    if err := r.ParseForm(); err != nil {
        writeJSON(w, http.StatusBadRequest, apiResponse{Error: "invalid form data"})
        return
    }

    // pull and sanitize inputs
    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // basic validation
    if err := validateCredentials(username, password); err != nil {
        writeJSON(w, http.StatusBadRequest, apiResponse{Error: err.Error()})
        return
    }

    // hash the password with bcrypt (includes a salt)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        log.Printf("bcrypt.GenerateFromPassword: %v", err)
        writeJSON(w, http.StatusInternalServerError, apiResponse{Error: "internal server error"})
        return
    }

    // insert into the database
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    query := `INSERT INTO users (username, password_hash) VALUES (?, ?)`
    if _, err := db.ExecContext(ctx, query, username, hashed); err != nil {
        // detect duplicate‐key error
        if me, ok := err.(*mysqlErr.MySQLError); ok && me.Number == 1062 {
            writeJSON(w, http.StatusConflict, apiResponse{Error: "username already exists"})
            return
        }
        log.Printf("db.ExecContext: %v", err)
        writeJSON(w, http.StatusInternalServerError, apiResponse{Error: "internal server error"})
        return
    }

    writeJSON(w, http.StatusCreated, apiResponse{Message: "user registered"})
}

// validateCredentials applies minimal policies—you can extend
func validateCredentials(username, password string) error {
    if len(username) < 3 || len(username) > 30 {
        return errors.New("username must be between 3 and 30 characters")
    }
    if len(password) < 8 {
        return errors.New("password must be at least 8 characters")
    }
    // optional: enforce complexity (digit, uppercase, symbol, etc.)
    // if !someRegex.MatchString(password) {
    //     return errors.New("password must contain …")
    // }
    return nil
}