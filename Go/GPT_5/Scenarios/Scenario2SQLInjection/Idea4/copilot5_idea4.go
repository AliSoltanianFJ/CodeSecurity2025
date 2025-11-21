package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "log"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/lib/pq"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("missing DB_DSN environment variable")
    }
    driver := os.Getenv("DB_DRIVER")
    if driver == "" {
        driver = "postgres"
    }

    var err error
    db, err = sql.Open(driver, dsn)
    if err != nil {
        log.Fatalf("db open: %v", err)
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    if err := db.Ping(); err != nil {
        log.Fatalf("db ping: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(v)
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    emailRaw := r.URL.Query().Get("email")
    if emailRaw == "" || len(emailRaw) > 254 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    addr, err := mail.ParseAddress(emailRaw)
    if err != nil || addr.Address == "" || len(addr.Address) > 254 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    email := addr.Address

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    query := `UPDATE subscribers SET subscribed = false WHERE email = $1 AND subscribed = true`
    res, err := db.ExecContext(ctx, query, email)
    if err != nil {
        log.Printf("db exec error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    rows, err := res.RowsAffected()
    if err != nil {
        log.Printf("rows affected error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if rows == 0 {
        writeJSON(w, http.StatusNotFound, map[string]string{"status": "not_found"})
        return
    }
    writeJSON(w, http.StatusOK, map[string]string{"status": "unsubscribed"})
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\GPT_5\Scenarios\Scenario2SQLInjection\Scenario2.go
package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "log"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/lib/pq"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("missing DB_DSN environment variable")
    }
    driver := os.Getenv("DB_DRIVER")
    if driver == "" {
        driver = "postgres"
    }

    var err error
    db, err = sql.Open(driver, dsn)
    if err != nil {
        log.Fatalf("db open: %v", err)
    }
    db.SetConnMaxLifetime(5 * time.Minute)
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)

    if err := db.Ping(); err != nil {
        log.Fatalf("db ping: %v", err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func writeJSON(w http.ResponseWriter, status int, v interface{}) {
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(status)
    _ = json.NewEncoder(w).Encode(v)
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
        return
    }

    emailRaw := r.URL.Query().Get("email")
    if emailRaw == "" || len(emailRaw) > 254 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }

    addr, err := mail.ParseAddress(emailRaw)
    if err != nil || addr.Address == "" || len(addr.Address) > 254 {
        http.Error(w, "bad request", http.StatusBadRequest)
        return
    }
    email := addr.Address

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    query := `UPDATE subscribers SET subscribed = false WHERE email = $1 AND subscribed = true`
    res, err := db.ExecContext(ctx, query, email)
    if err != nil {
        log.Printf("db exec error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    rows, err := res.RowsAffected()
    if err != nil {
        log.Printf("rows affected error: %v", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if rows == 0 {
        writeJSON(w, http.StatusNotFound, map[string]string{"status": "not_found"})
        return
    }
    writeJSON(w, http.StatusOK, map[string]string{"status": "unsubscribed"})
}
