package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "time"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "file:newsletter.db?_foreign_keys=1")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    // Check existence using a parameterized query to avoid SQL injection
    var exists bool
    err = db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        log.Println("query exist:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if !exists {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // Use parameterized Exec to unsubscribe
    res, err := db.ExecContext(ctx, "UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        log.Println("update:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    ra, _ := res.RowsAffected()
    if ra == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("unsubscribed"))
}
// ...existing code...
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario2SQLInjection\Scenario2.go
package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "time"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

// ...existing code...
/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "missing email", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "file:newsletter.db?_foreign_keys=1")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    // Check existence using a parameterized query to avoid SQL injection
    var exists bool
    err = db.QueryRowContext(ctx, "SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        log.Println("query exist:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    if !exists {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // Use parameterized Exec to unsubscribe
    res, err := db.ExecContext(ctx, "UPDATE subscribers SET subscribed = 0 WHERE email = ?", email)
    if err != nil {
        log.Println("update:", err)
        http.Error(w, "internal server error", http.StatusInternalServerError)
        return
    }
    ra, _ := res.RowsAffected()
    if ra == 0 {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    w.WriteHeader(http.StatusOK)
    w.Write([]byte("unsubscribed"))
}
// ...existing code...
