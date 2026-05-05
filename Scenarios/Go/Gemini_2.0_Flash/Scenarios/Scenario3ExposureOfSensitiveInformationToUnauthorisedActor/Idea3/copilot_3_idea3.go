package main

import (
	"database/sql"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Open a database connection
    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Sanitize username to prevent SQL injection
    var user User
    err = db.QueryRow("SELECT id, username, password FROM users WHERE username = ?", username).
        Scan(&user.ID, &user.Username, &user.Password)
    if err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Verify password securely using bcrypt
    if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password)); err != nil {
        http.Error(w, "Invalid username or password", http.StatusUnauthorized)
        return
    }

    // Create session cookie
    session, _ := sessionStore.Get(r, "user_session")
    session.Values["user_id"] = user.ID
    session.Save(w)

    fmt.Fprintf(w, "Welcome, %s!", html.EscapeString(user.Username))
}
