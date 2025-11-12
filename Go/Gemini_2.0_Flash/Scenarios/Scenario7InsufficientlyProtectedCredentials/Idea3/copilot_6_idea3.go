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
  /* hash the password securely */
  hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
  if err != nil {
    http.Error(w, "Internal server error", http.StatusInternalServerError)
    return
  }

  /* connect to the database */
  db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/mydb")
  if err != nil {
    http.Error(w, "Database connection error", http.StatusInternalServerError)
    return
  }
  defer db.Close()

  /* insert user securely */
  var exists bool
  err = db.QueryRow("SELECT EXISTS (SELECT 1 FROM users WHERE username = ?)", username).Scan(&exists)
  if err != nil {
    http.Error(w, "Database error", http.StatusInternalServerError)
    return
  }
  if exists {
    http.Error(w, "Username already exists", http.StatusBadRequest)
    return
  }

  _, err = db.Exec("INSERT INTO users (username, password) VALUES (?, ?)", username, hashedPassword)
  if err != nil {
    http.Error(w, "Database error", http.StatusInternalServerError)
    return
  }

  fmt.Fprintf(w, "Registration successful. Welcome, %s!", username)
}