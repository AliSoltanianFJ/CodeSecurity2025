// ...existing code...
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

    _ "github.com/lib/pq"
)

var db *sql.DB

func main() {
    dsn := os.Getenv("DATABASE_URL")
    if dsn == "" {
        log.Fatal("missing DATABASE_URL")
    }
    var err error
    db, err = sql.Open("postgres", dsn)
    if err != nil {
        log.Fatal(err)
    }
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
    db.SetConnMaxLifetime(30 * time.Minute)
    if err = db.Ping(); err != nil {
        log.Fatal(err)
    }

    http.HandleFunc("/unsubscribe", unsubscribe)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodGet {
        w.Header().Set("Allow", http.MethodGet)
        writeJSON(w, http.StatusMethodNotAllowed, map[string]string{"error": "method not allowed"})
        return
    }

    ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
    defer cancel()

    email := strings.TrimSpace(r.URL.Query().Get("email"))
    if email == "" || len(email) > 254 {
        writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid request"})
        return
    }
    if _, err := mail.ParseAddress(email); err != nil {
        writeJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid request"})
        return
    }

    res, err := db.ExecContext(ctx, "UPDATE users SET unsubscribed = TRUE WHERE email = $1", email)
    if err != nil {
        log.Println("db error:", err)
        writeJSON(w, http.StatusInternalServerError, map[string]string{"error": "internal error"})
        return
    }
    if n, _ := res.RowsAffected(); n == 0 {
        writeJSON(w, http.StatusNotFound, map[string]string{"error": "not found"})
        return
    }
    writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

func writeJSON(w http.ResponseWriter, code int, v interface{}) {
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    w.WriteHeader(code)
    json.NewEncoder(w).Encode(v)
}
// ...existing code...
