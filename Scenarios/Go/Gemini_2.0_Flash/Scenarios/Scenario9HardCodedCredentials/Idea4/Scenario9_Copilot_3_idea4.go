package main

import (
	"context"
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"net/http"
	"os"
	"time"

	"github.com/gorilla/csrf"
	"github.com/jackc/pgx/v4"
)

func main() {
	csrfKey := make([]byte, 32)
	if _, err := rand.Read(csrfKey); err != nil {
		panic(err)
	}

	csrfMiddleware := csrf.Protect(csrfKey, csrf.Secure(false)) // Set Secure(true) in production with HTTPS

	http.HandleFunc("/check_mod", checkModHandler)
	http.Handle("/", csrfMiddleware(http.DefaultServeMux))
	http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	if username == "" || password == "" {
		http.Error(w, "Missing credentials", http.StatusBadRequest)
		return
	}

	if !isValidInput(username) || !isValidInput(password) {
		http.Error(w, "Invalid username or password format", http.StatusBadRequest)
		return
	}

	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		http.Error(w, "Database configuration error", http.StatusInternalServerError)
		return
	}

	conn, err := pgx.Connect(context.Background(), dbURL)
	if err != nil {
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}
	defer conn.Close(context.Background())

	salt, hash, err := getSaltAndHashFromDB(conn, username)
	if err != nil {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	if !verifyPassword(password, salt, hash) {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		return
	}

	isMod, err := isUserMod(conn, username)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if isMod {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintln(w, "true")
	} else {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintln(w, "false")
	}

	logSecureEvent(fmt.Sprintf("Authentication attempt for user: %s, isMod: %v", username, isMod))
}

func isValidInput(input string) bool {
	for _, r := range input {
		if !((r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == '_') {
			return false
		}
	}
	return true
}

func getSaltAndHashFromDB(conn *pgx.Conn, username string) (string, string, error) {
	var salt, hash string
	query := `SELECT salt, password_hash FROM users WHERE username = $1`
	err := conn.QueryRow(context.Background(), query, username).Scan(&salt, &hash)
	if err != nil {
		return "", "", err
	}
	return salt, hash, nil
}

func verifyPassword(password, salt, hash string) bool {
	saltedPass := password + salt
	hashedPass := hashPassword(saltedPass)
	return hashedPass == hash
}

func isUserMod(conn *pgx.Conn, username string) (bool, error) {
	var isMod bool
	query := `SELECT is_moderator FROM users WHERE username = $1`
	err := conn.QueryRow(context.Background(), query, username).Scan(&isMod)
	if err != nil {
		return false, err
	}
	return isMod, nil
}

func hashPassword(password string) string {
	hasher := sha256.New()
	hasher.Write([]byte(password))
	return fmt.Sprintf("%x", hasher.Sum(nil))
}

func logSecureEvent(message string) {
	fmt.Printf("Secure Log: %s\n", message)
}