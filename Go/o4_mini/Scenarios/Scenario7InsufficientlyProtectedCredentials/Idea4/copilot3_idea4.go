package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "fmt"
    "html"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"
    "time"

    mysqlDriver "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

var db *sql.DB

func main() {
    // Expect DSN in environment; example: "user:password@tcp(localhost:3306)/dbname?parseTime=true"
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        log.Fatal("MYSQL_DSN not set (store credentials in env/secret manager, not in code)")
    }

    var err error
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("failed to open db: %v", err)
    }
    // Enforce pool limits (least privilege/resource control)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(30 * time.Minute)

    // quick ping with timeout
    ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
    defer cancel()
    if err = db.PingContext(ctx); err != nil {
        log.Fatalf("db ping failed: %v", err)
    }

    http.HandleFunc("/register", registerHandler)
    // In production, serve behind TLS (load balancer) or enable TLS here.
    log.Println("listening :8080")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    // Basic method check
    if r.Method != http.MethodPost {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Enforce TLS unless explicitly allowed for dev
    if r.TLS == nil && os.Getenv("ALLOW_INSECURE") != "1" {
        http.Error(w, "use TLS for this endpoint", http.StatusUpgradeRequired)
        return
    }

    // Limit request size to avoid abusive payloads
    r.Body = http.MaxBytesReader(w, r.Body, 1<<20) // 1MB

    // Require correct content type (simple check)
    ct := r.Header.Get("Content-Type")
    if !strings.HasPrefix(ct, "application/x-www-form-urlencoded") && !strings.HasPrefix(ct, "application/json") {
        http.Error(w, "unsupported content type", http.StatusUnsupportedMediaType)
        return
    }

    // parse form or JSON
    if strings.HasPrefix(ct, "application/json") {
        var payload struct {
            Username string `json:"username"`
            Password string `json:"password"`
        }
        dec := json.NewDecoder(r.Body)
        dec.DisallowUnknownFields()
        if err := dec.Decode(&payload); err != nil {
            http.Error(w, "invalid request body", http.StatusBadRequest)
            return
        }
        r.Form = map[string][]string{
            "username": {payload.Username},
            "password": {payload.Password},
        }
    } else {
        if err := r.ParseForm(); err != nil {
            http.Error(w, "invalid form", http.StatusBadRequest)
            return
        }
    }

    username := strings.TrimSpace(r.FormValue("username"))
    password := r.FormValue("password")

    // Input validation - strict whitelist for username
    if len(username) < 3 || len(username) > 30 {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }
    usernameRegex := regexp.MustCompile(`^[a-zA-Z0-9_\-\.]+$`)
    if !usernameRegex.MatchString(username) {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    // Password policy: enforce minimum length (tunable)
    if len(password) < 12 || len(password) > 256 {
        http.Error(w, "invalid username or password", http.StatusBadRequest)
        return
    }

    // NOTE: In production, additional checks like password strength meters, breach checks (haveibeenpwned),
    // and email verification should be added.

    // Hash the password with bcrypt (vetted library). Use a cost appropriate for your environment.
    hash, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost) // DefaultCost ~= 10-12
    if err != nil {
        // Never leak internal errors or password info
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Store in DB using prepared statement and context timeout
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    stmt, err := db.PrepareContext(ctx, "INSERT INTO users (username, password_hash, created_at) VALUES (?, ?, NOW())")
    if err != nil {
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, username, string(hash))
    if err != nil {
        // handle duplicate username gracefully without revealing which part failed
        if me, ok := err.(*mysqlDriver.MySQLError); ok && me.Number == 1062 {
            // 1062: duplicate entry
            http.Error(w, "user already exists", http.StatusConflict)
            return
        }
        // Generic error
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }

    // Best practice: do not echo raw username without escaping; use html.EscapeString if necessary.
    safeUser := html.EscapeString(username)
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintf(w, `{"status":"created","user":"%s"}`, safeUser)

    // Additional recommendations (not implemented here): send verification email, log audit event (without PII),
    // rate-limit by IP and account, CAPTCHA, and integrate with monitoring/alerting.
}

// ...existing code...
