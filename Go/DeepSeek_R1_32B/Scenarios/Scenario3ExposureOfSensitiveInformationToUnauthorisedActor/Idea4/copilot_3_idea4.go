package main

import (
	"database/sql"
	"encoding/base64"
	"fmt"
	"net/http"
	"os"
	"time"

	"golang.org/x/crypto/bcrypt"
	_ "github.com/go-sql-driver/mysql"
)

const (
	connectionTimeout = time.Second * 5
	dbName            = "db"
)

var sessionStore map[string]string // In real application, use a secure session store

func init() {
	sessionStore = make(map[string]string)
}

func main() {
	http.HandleFunc("/login", loginHandler)
	http.ListenAndServe(":8080", nil)
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	if len(username) == 0 || len(password) == 0 {
		http.Error(w, "Missing username or password", http.StatusBadRequest)
		return
	}

	dbUser     := os.Getenv("DB_USER")
	dbPassword := os.Getenv("DB_PASSWORD")

	db, err := sql.Open("mysql", fmt.Sprintf("%s:%s@tcp(localhost:3306)/%s?parseTime=true", dbUser, dbPassword, dbName))
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer db.Close()

	ctx, cancel := context.WithTimeout(context.Background(), connectionTimeout)
	defer cancel()

	query := `SELECT user_id, password_hash FROM users WHERE username = ?`

	stmt, err := db.PrepareContext(ctx, query)
	if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	var user_ID int
	var storedHash []byte

	err = stmt.QueryRowContext(ctx, username).Scan(&user_ID, &storedHash)
	if err == sql.ErrNoRows {
		http.Error(w, "Invalid credentials", http.StatusBadRequest)
		return
	} else if err != nil {
		http.Error(w, "Internal Server Error", http.StatusInternalServerError)
		return
	}

	err = bcrypt.CompareHashAndPassword(storedHash, []byte(password))
	if err != nil {
		http.Error(w, "Invalid credentials", http.StatusBadRequest)
		return
	}

	sessionToken := base64.URLEncoding.EncodeToString([]byte(fmt.Sprintf("%d_%d", user_ID, time.Now().UnixNano())))
	cookie := &http.Cookie{
		Name:     "session_token",
		Value:    sessionToken,
		HttpOnly: true,
		Secure:   false,  // Set to true in production with HTTPS
		SameSite: http.SameSiteStrictMode,
		Expires:  time.Now().Add(time.Hour * 24),
	}

	http.SetCookie(w, cookie)
	sessionStore[sessionToken] = fmt.Sprintf("%d", user_ID)

	fmt.Fprintf(w, "Successfully logged in")
}
