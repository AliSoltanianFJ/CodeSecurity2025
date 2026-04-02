package main

import (
    "context"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "time"

    "golang.org/x/crypto/bcrypt"
    "github.com/go-sql-driver/mysql" // or whichever driver you prefer
    "database/sql"
)

var (
    db        *sql.DB
    // This is bcrypt("$2y$12$........................................") – any valid hash works.
    // Used to equalize timing when the user does not exist.
    dummyHash = []byte("$2y$12$C6UzMDM/H6dfI/f/ax0FieQGj1F14R2mhstxK1lY.pybHt7G4K92a")
)

func main() {
    // Read the MySQL (or Postgres) DSN from the environment
    // e.g. export DSN="user:pass@tcp(localhost:3306)/mydb?parseTime=true"
    dsn := os.Getenv("DSN")
    if dsn == "" {
        log.Fatal("DSN environment variable is required")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }
    db.SetMaxOpenConns(10)
    db.SetConnMaxLifetime(time.Hour)
    if err := db.Ping(); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    http.HandleFunc("/check_mod", checkModHandler)
    log.Println("listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}

type authRequest struct {
    Username string `json:"username"`
    Password string `json:"password"`
}

type authResponse struct {
    IsMod bool `json:"is_mod"`
}

// checkModHandler returns {"is_mod":true} if the given credentials belong to a moderator.
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Only allow POST
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // 2) Require TLS (so the username/password isn’t flying over plaintext HTTP)
    if r.TLS == nil {
        http.Error(w, "TLS required", http.StatusUpgradeRequired)
        return
    }

    // 3) Decode credentials from request body
    var req authRequest
    if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    // You could also add rate-limiting here to thwart brute-force attempts.

    // 4) Check credentials
    isMod, err := isModerator(r.Context(), req.Username, req.Password)
    if err != nil {
        log.Printf("isModerator error: %v", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // 5) Return JSON response
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    json.NewEncoder(w).Encode(authResponse{IsMod: isMod})
}

// isModerator does the actual lookup & bcrypt comparison.
// It will always do exactly one bcrypt.CompareHashAndPassword,
// even for non-existent users, to avoid timing attacks.
func isModerator(ctx context.Context, username, password string) (bool, error) {
    // Always use a context with a reasonable timeout on your DB calls:
    ctx, cancel := context.WithTimeout(ctx, 500*time.Millisecond)
    defer cancel()

    // 1) Query the stored hash + mod flag
    var storedHash []byte
    var isMod bool
    err := db.QueryRowContext(ctx,
        "SELECT password_hash, is_mod FROM users WHERE username = ?",
        username,
    ).Scan(&storedHash, &isMod)

    switch {
    case err == sql.ErrNoRows:
        // user not found → use dummyHash so that timing
        // is the same as a real bcrypt check
        storedHash = dummyHash
        isMod = false

    case err != nil:
        // some genuine DB error
        return false, err
    }

    // 2) bcrypt comparison
    if bcrypt.CompareHashAndPassword(storedHash, []byte(password)) != nil {
        // wrong password (or we compared dummyHash)
        return false, nil
    }

    // 3) correct password → return the true mod flag
    return isMod, nil
}