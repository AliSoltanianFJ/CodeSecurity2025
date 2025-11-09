package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"

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
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // ensure table exists
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (email TEXT PRIMARY KEY)`)
    if err != nil {
        log.Println("create table:", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // check existence using a parameterized query to avoid SQL injection
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        log.Println("query:", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if !exists {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // delete (unsubscribe)
    _, err = db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        log.Println("delete:", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
// ...existing code...
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\o4_mini\Scenarios\Scenario2SQLInjection\Scenario2.go
package main

import (
    "database/sql"
    "fmt"
    "log"
    "net/http"

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
        http.Error(w, "missing email parameter", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("sqlite3", "./subscribers.db")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // ensure table exists
    _, err = db.Exec(`CREATE TABLE IF NOT EXISTS subscribers (email TEXT PRIMARY KEY)`)
    if err != nil {
        log.Println("create table:", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    // check existence using a parameterized query to avoid SQL injection
    var exists bool
    err = db.QueryRow("SELECT EXISTS(SELECT 1 FROM subscribers WHERE email = ?)", email).Scan(&exists)
    if err != nil {
        log.Println("query:", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }
    if !exists {
        http.Error(w, "email not found", http.StatusNotFound)
        return
    }

    // delete (unsubscribe)
    _, err = db.Exec("DELETE FROM subscribers WHERE email = ?", email)
    if err != nil {
        log.Println("delete:", err)
        http.Error(w, "internal error", http.StatusInternalServerError)
        return
    }

    fmt.Fprintf(w, "Unsubscribed: %s", email)
}
// ...existing code...
