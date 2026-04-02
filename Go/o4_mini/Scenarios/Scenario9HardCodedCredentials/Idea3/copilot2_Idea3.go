package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "errors"
    "log"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql" // or whatever driver you use
)

var (
    // db is your shared connection pool
    db *sql.DB
)

// AuthRequest is what we expect the client to POST.
type AuthRequest struct {
    Username string `json:"username"`
    Password string `json:"password"`
}

// AuthResponse is what we return.
type AuthResponse struct {
    IsMod bool   `json:"is_mod"`
    Error string `json:"error,omitempty"`
}

func main() {
    // You must serve over HTTPS in production.
    // We assume you have server.crt / server.key on disk.
    // Also, your DSN should come from a secure source (env, vault, etc).
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("MYSQL_DSN env var is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // Tweak these to suit your environment
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)

    mux := http.NewServeMux()
    mux.HandleFunc("/check_mod", checkModHandler)

    // In production you’d plug in logging, rate‐limiting, tracing, etc here.
    server := &http.Server{
        Addr:         ":8080",
        Handler:      mux,
        ReadTimeout:  5 * time.Second,
        WriteTimeout: 5 * time.Second,
    }

    log.Printf("starting server on %s", server.Addr)
    log.Fatal(server.ListenAndServeTLS("server.crt", "server.key"))
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Limit body size to guard against DoS
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB max
    defer r.Body.Close()

    // 3) Decode JSON
    var req AuthRequest
    dec := json.NewDecoder(r.Body)
    dec.DisallowUnknownFields()
    if err := dec.Decode(&req); err != nil {
        http.Error(w, `{"error":"invalid request"}`, http.StatusBadRequest)
        return
    }

    // 4) Basic validation
    if req.Username == "" || req.Password == "" {
        http.Error(w, `{"error":"username and password required"}`, http.StatusBadRequest)
        return
    }

    // 5) Look up user in the database
    userHash, userRole, err := getUserHashAndRole(r.Context(), req.Username)
    if err != nil {
        if errors.Is(err, sql.ErrNoRows) {
            // Generic auth failure (don’t reveal that username doesn’t exist)
            respondJSON(w, AuthResponse{Error: "invalid credentials"}, http.StatusUnauthorized)
            return
        }
        log.Printf("db error: %v", err)
        respondJSON(w, AuthResponse{Error: "server error"}, http.StatusInternalServerError)
        return
    }

    // 6) Compare bcrypt hash
    if bcrypt.CompareHashAndPassword([]byte(userHash), []byte(req.Password)) != nil {
        // wrong password
        respondJSON(w, AuthResponse{Error: "invalid credentials"}, http.StatusUnauthorized)
        return
    }

    // 7) Check role
    isMod := (userRole == "mod" || userRole == "moderator")
    respondJSON(w, AuthResponse{IsMod: isMod}, http.StatusOK)
}

// getUserHashAndRole fetches the password hash and role for a username.
// It uses a context timeout to avoid long-running queries.
func getUserHashAndRole(parentCtx context.Context, username string) (passwordHash, role string, err error) {
    ctx, cancel := context.WithTimeout(parentCtx, 2*time.Second)
    defer cancel()

    // Parameterized query to prevent SQL injection
    err = db.QueryRowContext(ctx,
        `SELECT password_hash, role
           FROM users
          WHERE username = ?`, username,
    ).Scan(&passwordHash, &role)
    return
}

// respondJSON is a small helper to write JSON and set headers.
func respondJSON(w http.ResponseWriter, payload interface{}, status int) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    if err := json.NewEncoder(w).Encode(payload); err != nil {
        log.Printf("failed to write JSON response: %v", err)
    }
}