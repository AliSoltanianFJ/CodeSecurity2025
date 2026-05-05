package main

import (
	"crypto/sha256"
	"database/sql"
	"encoding/hex"
	"fmt"
	"net/http"
	"os"

	"github.com/pkg/errors"
	"golang.org/x/text/secure/salt"
	"golang.org/x/text/transform"
	"golang.org/x/text/unicode/norm"

	_ "github.com/go-sql-driver/mysql"
)

func main() {
	db, err := sql.Open("mysql", os.Getenv("DB_DSN"))
	if err != nil {
		panic(err)
	}
	defer db.Close()

	http.HandleFunc("/register", func(w http.ResponseWriter, r *http.Request) {
		registerHandler(db, w, r)
	})
	http.ListenAndServe(":8080", nil)
}

/* register url */
func registerHandler(db *sql.DB, w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")

	// Validate input
	if username == "" || password == "" {
		http.Error(w, "Username and password are required", http.StatusBadRequest)
		return
	}

	// Sanitize username
	sanitizedUsername := sanitizeInput(username)

	// Hash the password with salt
	saltBytes, err := salt.Generate(32) // Generate a random 32-byte salt
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	hashedPassword := hashPassword([]byte(password), saltBytes)

	query := `
        INSERT INTO users (username, password_hash, salt)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id)`

	stmt, err := db.Prepare(query)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer stmt.Close()

	_, err = stmt.Exec(sanitizedUsername, hex.EncodeToString(hashedPassword), hex.EncodeToString(saltBytes))
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			http.Error(w, "Username already exists", http.StatusConflict)
		} else {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
		}
		return
	}

	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, "Registration successful")
}

func sanitizeInput(input string) string {
	transformer := transform.Chain(norm.NFC,
		transform.Blacklist(
			'\u0000', // Null character
			'\x08',    // Backspace
			'\x0B',    // Vertical tab
			'\x0C',    // Form feed
			'\x1C',    // File separator
			'\x1D',    // Group separator
			'\x1E',    // Record separator
			'\x1F',    // Unit separator
		),
	)
	result, _, err := transform.String(transformer, input)
	if err != nil {
		return ""
	}
	return result
}

func hashPassword(password []byte, salt []byte) []byte {
	hash := sha256.Sum256(append(password, salt...))
	return hash[:]
}
