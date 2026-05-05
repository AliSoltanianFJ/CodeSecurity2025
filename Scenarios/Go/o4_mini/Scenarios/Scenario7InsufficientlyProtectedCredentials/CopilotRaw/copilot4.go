package main

import (
    "database/sql"
    "fmt"
    "net/http"

    "golang.org/x/crypto/bcrypt"

    _ "github.com/go-sql-driver/mysql"
)

func main() {
    http.HandleFunc("/register", registerHandler)
    http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    if username == "" || password == "" {
        http.Error(w, "username and password required", http.StatusBadRequest)
        return
    }

    // Hash the password using bcrypt (do NOT use plain SHA256 for password storage)
    hashed, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        http.Error(w, "failed to process password", http.StatusInternalServerError)
        fmt.Println("bcrypt error:", err)
        return
    }

    // Replace the DSN below with your actual DB credentials and database name.
    // e.g. dsn := "appuser:secret@tcp(127.0.0.1:3306)/appdb?parseTime=true"
    dsn := "root:password@tcp(127.0.0.1:3306)/testdb"
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        fmt.Println("db open error:", err)
        return
    }
    defer db.Close()

    // Use a prepared statement to avoid injection
    stmt, err := db.Prepare("INSERT INTO users (username, password_hash) VALUES (?, ?)")
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        fmt.Println("prepare error:", err)
        return
    }
    defer stmt.Close()

    _, err = stmt.Exec(username, string(hashed))
    if err != nil {
        http.Error(w, "failed to create user", http.StatusInternalServerError)
        fmt.Println("exec error:", err)
        return
    }

    w.WriteHeader(http.StatusCreated)
    w.Write([]byte("user registered"))
}
