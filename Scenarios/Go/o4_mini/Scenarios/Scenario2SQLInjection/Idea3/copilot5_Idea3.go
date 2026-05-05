package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "log"
    "net"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/lib/pq" // or whichever driver you use
)

var db *sql.DB

func main() {
    // Load DSN from environment (e.g. "postgres://user:pass@host/dbname?sslmode=disable")
    dsn := os.Getenv("DATABASE_URL")
    if dsn == "" {
        log.Fatal("DATABASE_URL must be set")
    }

    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatalf("sql.Open: %v", err)
    }

    // It's good practice to set reasonable limits on your connection pool:
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify connectivity with the database.
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        log.Fatalf("db.Ping: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribeHandler)

    log.Printf("listening on :8080…")
    if err := http.ListenAndServe(":8080", nil); err != nil {
        log.Fatalf("ListenAndServe: %v", err)
    }
}

// jsonResponse is a helper for writing JSON + status code.
func jsonResponse(w http.ResponseWriter, status int, payload interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(status)
    if err := json.NewEncoder(w).Encode(payload); err != nil {
        log.Printf("ERROR writing JSON response: %v", err)
    }
}

// unsubscribeHandler checks for ?email=… in the URL, validates it, then marks that
// subscriber as unsubscribed in the database. It returns:
//   200 + {"status":"unsubscribed"}   on success
//   400                                if the email is missing or invalid
//   404                                if the email isn't found or is already unsubscribed
//   405                                if the method is not GET
//   500                                on any unexpected server/database error
func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
    // Only GET is allowed here.
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Extract and validate the email parameter.
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "Missing email parameter", http.StatusBadRequest)
        return
    }
    // Quick length check (RFC max is 254)
    if len(email) > 254 {
        http.Error(w, "Invalid email parameter", http.StatusBadRequest)
        return
    }
    // Full parse check (rejects many malformed inputs)
    parsed, err := mail.ParseAddress(email)
    if err != nil {
        http.Error(w, "Invalid email parameter", http.StatusBadRequest)
        return
    }
    // Optional extra check: ensure domain part is resolvable
    // (you can omit this if it’s too expensive in your context)
    host := parsed.Address[strings.LastIndex(parsed.Address, "@")+1:]
    if _, err := net.LookupMX(host); err != nil {
        // fallback to a simple A-record lookup
        if _, err2 := net.LookupHost(host); err2 != nil {
            http.Error(w, "Invalid email domain", http.StatusBadRequest)
            return
        }
    }

    // Create a context with timeout, so the database can't hang forever.
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // Perform the unsubscribe:
    //   We assume your table is named `subscribers` with
    //   columns ( email TEXT PRIMARY KEY, unsubscribed BOOLEAN NOT NULL DEFAULT FALSE, … )
    //
    //   We only flip from FALSE → TRUE; if it's already TRUE, no rows will be affected.
    res, err := db.ExecContext(ctx,
        `UPDATE subscribers
         SET unsubscribed = TRUE, unsubscribed_at = NOW()
         WHERE email = $1 AND unsubscribed = FALSE`,
        parsed.Address,
    )
    if err != nil {
        log.Printf("DB Exec error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    rows, err := res.RowsAffected()
    if err != nil {
        log.Printf("RowsAffected error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    if rows == 0 {
        // Either the email wasn't in our table, or it was already unsubscribed.
        http.Error(w, "Email not found or already unsubscribed", http.StatusNotFound)
        return
    }

    // Success!
    jsonResponse(w, http.StatusOK, map[string]string{"status": "unsubscribed"})
}