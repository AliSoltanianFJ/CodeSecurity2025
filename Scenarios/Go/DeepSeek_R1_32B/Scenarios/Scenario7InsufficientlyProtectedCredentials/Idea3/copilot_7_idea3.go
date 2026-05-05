package main

import (
	"crypto/bcrypt"
	"database/sql"
	"fmt"
	"html"
	"net/http"
	"regexp"
	"strings"

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

	username := strings.TrimSpace(r.FormValue("username"))
	password := strings.TrimSpace(r.FormValue("password"))

	// Input validation
	if username == "" || password == "" {
		http.Error(w, "Username and password required", http.StatusBadRequest)
		return
	}
	if len(username) < 3 || len(username) > 32 {
		http.Error(w, "Username must be 3-32 characters long", http.StatusBadRequest)
		return
	}
	// Allow only letters, numbers, underscores in username
	matched, _ := regexp.MatchString(`^[a-zA-Z0-9_]+$`, username)
	if !matched {
		http.Error(w, "Invalid username characters", http.StatusBadRequest)
		return
	}

	// Secure password hashing using bcrypt
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Database connection (update DSN as appropriate)
	db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/securedb")
	if err != nil {
		http.Error(w, "Database connection error", http.StatusInternalServerError)
		defer db.Close()
		return
	}
	defer db.Close()

	// Prevent SQL injection with parameterized queries
	_, err = db.Exec("INSERT INTO users (username, password_hash) VALUES (?, ?)", username, hashedPassword)
	if err != nil {
		http.Error(w, "Error registering user", http.StatusInternalServerError)
		return
	}

	// Avoid XSS by escaping username in response
	fmt.Fprintf(w, "Registration successful! Welcome, %s.", html.EscapeString(username))
}
