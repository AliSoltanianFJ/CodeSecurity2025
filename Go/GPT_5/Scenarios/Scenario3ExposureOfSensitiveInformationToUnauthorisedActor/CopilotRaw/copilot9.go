package main

import (
    "database/sql"
    "log"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/bcrypt"
)

// ...existing code...

func main() {
    http.HandleFunc("/login", loginHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

// ...existing code...

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        log.Println("db open:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        log.Println("db ping:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    var storedHash string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&storedHash)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Println("query row:", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if bcrypt.CompareHashAndPassword([]byte(storedHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    w.WriteHeader(http.StatusOK)
    _, _ = w.Write([]byte("Login successful"))
}
