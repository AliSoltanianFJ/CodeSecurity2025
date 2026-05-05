package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "log"
    "net/http"
    "net/mail"
    "os"
    "strings"
    "time"

    _ "github.com/lib/pq" // replace with your driver if needed
)

// global DB handle
var db *sql.DB

// a simple JSON response envelope
type jsonResp struct {
    Message string `json:"message,omitempty"`
    Error   string `json:"error,omitempty"`
}

func main() {
    var err error
    db, err = initDB()
    if err != nil {
        log.Fatalf("cannot initialize db: %v", err)
    }
    defer db.Close()

    http.HandleFunc("/unsubscribe", unsubscribeHandler)

    addr := ":8080"
    log.Printf("starting server on %s", addr)
    if err := http.ListenAndServe(addr, nil); err != nil {
        log.Fatalf("http server error: %v", err)
    }
}

// initDB reads connection info from the environment and
// returns an open *sql.DB with a successful Ping.
func initDB() (*sql.DB, error) {
    // expected env vars: DB_HOST, DB_PORT, DB_USER, DB_PASS, DB_NAME
    host := os.Getenv("DB_HOST")
    port := os.Getenv("DB_PORT")
    user := os.Getenv("DB_USER")
    pass := os.Getenv("DB_PASS")
    name := os.Getenv("DB_NAME")
    if host == "" || port == "" || user == "" || name == "" {
        return nil, fmt.Errorf("one of DB_HOST, DB_PORT, DB_USER, DB_NAME is not set")
    }

    // example for Postgres
    dsn := fmt.Sprintf(
        "host=%s port=%s user=%s password=%s dbname=%s sslmode=disable",
        host, port, user, pass, name,
    )

    db, err := sql.Open("postgres", dsn)
    if err != nil {
        return nil, err
    }

    // Set reasonable connection‐pool settings
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Verify the connection
    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()
    if err := db.PingContext(ctx); err != nil {
        db.Close()
        return nil, err
    }
    return db, nil
}

func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
    // Only allow GET here (you can change to POST if you prefer)
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Prepare JSON response writer
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // 1) Get & sanitize the email parameter
    rawEmail := strings.TrimSpace(r.URL.Query().Get("email"))
    if rawEmail == "" {
        w.WriteHeader(http.StatusBadRequest)
        json.NewEncoder(w).Encode(jsonResp{Error: "missing email parameter"})
        return
    }
    if len(rawEmail) > 254 {
        w.WriteHeader(http.StatusBadRequest)
        json.NewEncoder(w).Encode(jsonResp{Error: "email too long"})
        return
    }

    // 2) Validate email syntax
    addr, err := mail.ParseAddress(rawEmail)
    if err != nil {
        w.WriteHeader(http.StatusBadRequest)
        json.NewEncoder(w).Encode(jsonResp{Error: "invalid email format"})
        return
    }
    email := strings.ToLower(addr.Address)

    // 3) Create a context with timeout for DB ops
    ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
    defer cancel()

    // 4) Lookup the subscriber by email
    var subscriberID int
    err = db.QueryRowContext(
        ctx,
        `SELECT id
           FROM subscribers
          WHERE email = $1
            AND unsubscribed = FALSE`, // only treat as “subscribed”
        email,
    ).Scan(&subscriberID)
    if err == sql.ErrNoRows {
        // Either not in DB, or already unsubscribed
        w.WriteHeader(http.StatusNotFound)
        json.NewEncoder(w).Encode(jsonResp{Error: "email not found or already unsubscribed"})
        return
    }
    if err != nil {
        log.Printf("db lookup error: %v", err)
        w.WriteHeader(http.StatusInternalServerError)
        json.NewEncoder(w).Encode(jsonResp{Error: "internal server error"})
        return
    }

    // 5) Mark the user as unsubscribed
    result, err := db.ExecContext(
        ctx,
        `UPDATE subscribers
            SET unsubscribed = TRUE,
                unsubscribed_at = NOW()
          WHERE id = $1`,
        subscriberID,
    )
    if err != nil {
        log.Printf("db unsubscribe error: %v", err)
        w.WriteHeader(http.StatusInternalServerError)
        json.NewEncoder(w).Encode(jsonResp{Error: "internal server error"})
        return
    }
    rows, _ := result.RowsAffected()
    if rows == 0 {
        // This really shouldn’t happen since we just SELECTed it
        w.WriteHeader(http.StatusInternalServerError)
        json.NewEncoder(w).Encode(jsonResp{Error: "could not update subscription status"})
        return
    }

    // 6) Success!
    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(jsonResp{Message: "successfully unsubscribed"})
}