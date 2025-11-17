package main

import (
	"crypto"
	"database/sql"
	"encoding/json"
	"net/http"

	"github.com/gorilla/sessions"
	_ "github.com/go-sql-driver/mysql"
)

var sessionStore = sessions.NewCookieStore([]byte("your-secure-key"))

func main() {
	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Validate input
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Prevent buffer overflow by limiting input size
	if len(username) > 50 || len(password) > 100 {
		http.Error(w, "Invalid input length", http.StatusBadRequest)
		return
	}

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	// Use prepared statement to prevent SQL injection
	stmt, err := db.Prepare("SELECT id FROM users WHERE username = ? AND password = ? LIMIT 1")
	if err != nil {
		http.Error(w, "Query preparation failed", http.StatusInternalServerError)
		return
	}

	row := stmt.QueryRow(username, password)
	var userID int64

	if err := row.Scan(&userID); err == sql.ErrNoRows {
		response := map[string]string{"error": "Invalid credentials"}
		json.NewEncoder(w).Encode(response)
		return
	} else if err != nil {
		http.Error(w, "Query execution failed", http.StatusInternalServerError)
		return
	}

	// Create session cookie
	session, _ := sessionStore.Create(r, "session-name")
	session.Values["user_id"] = userID
	session.Options = &sessions.Options{
		HttpOnly: true,
		Secure:   true, // Set to false if testing locally without HTTPS
		SameSite: http.SameSiteLaxMode,
		MaxAge:   3600 * 24 * 7, // One week
	}
	if err := session.Save(r, w); err != nil {
		http.Error(w, "Session creation failed", http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	response := map[string]string{"message": "Login successful"}
	json.NewEncoder(w).Encode(response)
}

func mysqlEscape(str string) string {
	// Implement MySQL escape logic here
	return str
}

func hashPassword(password string) string {
	// Implement password hashing logic here
	return password
}
