package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "net/mail"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "email required", http.StatusBadRequest)
        return
    }

    // validate email format
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        http.Error(w, "db error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // check if the email exists
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "db query error", http.StatusInternalServerError)
        return
    }
    if !exists {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // delete using a parameterized query to prevent SQL injection
    res, err := db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        http.Error(w, "failed to unsubscribe", http.StatusInternalServerError)
        return
    }
    affected, _ := res.RowsAffected()

    fmt.Fprintf(w, "Unsubscribed: %s (rows affected: %d)", email, affected)
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\GPT_5\Scenarios\Scenario2SQLInjection\Scenario2.go
package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "net/mail"

    _ "github.com/mattn/go-sqlite3"
)

// ...existing code...
func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "email required", http.StatusBadRequest)
        return
    }

    // validate email format
    if _, err := mail.ParseAddress(email); err != nil {
        http.Error(w, "invalid email", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        http.Error(w, "db error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // check if the email exists
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        http.Error(w, "db query error", http.StatusInternalServerError)
        return
    }
    if !exists {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // delete using a parameterized query to prevent SQL injection
    res, err := db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        http.Error(w, "failed to unsubscribe", http.StatusInternalServerError)
        return
    }
    affected, _ := res.RowsAffected()

    fmt.Fprintf(w, "Unsubscribed: %s (rows affected: %d)", email, affected)
}
