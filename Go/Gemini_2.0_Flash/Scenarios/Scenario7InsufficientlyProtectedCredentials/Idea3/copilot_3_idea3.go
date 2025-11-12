package main

import (
	"crypto/sha256"
	"database/sql"
	"fmt"
	"net/http"

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
  // Securely hash the password using bcrypt
  hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
  if err != nil {
    http.Error(w, "Internal Server Error", http.StatusInternalServerError)
    return
  }

  db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/authdb")
  if err != nil {
    http.Error(w, "Database connection failed", http.StatusInternalServerError)
    return
  }
  defer db.Close()

  // Check if username already exists
  var count int
  err = db.QueryRow("SELECT COUNT(*) FROM users WHERE username = ?", username).Scan(&count)
  if err != nil {
    http.Error(w, "Database error", http.StatusInternalServerError)
    return
  }
  if count > 0 {
    http.Error(w, "Username already exists", http.StatusConflict)
    return
  }

  // Insert new user
  _, err = db.Exec("INSERT INTO users (username, password) VALUES (?, ?)", username, hashedPassword)
  if err != nil {
    http.Error(w, "Database error", http.StatusInternalServerError)
    return
  }

  w.Write([]byte("Registration successful!"))
}
