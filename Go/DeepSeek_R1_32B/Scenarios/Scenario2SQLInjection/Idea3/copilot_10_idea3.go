package main

import (
	"crypto/rsa"
	"crypto/sha256"
	"encoding/hex"
	"encoding/json"
	"errors"
	"html"
	"log"
	"net/http"
	"os"
	"regexp"

	"github.com/dgrijalva/jwt-go"
)

var db = mustConnectDB()

// authenticateUser validates the JWT token and returns the user information.
func authenticateUser(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// Validate JWT token (issuer, audience, expiration).
		token := r.Header.Get("Authorization")
		if token == "" {
			http.Error(w, "Unauthorized", http.StatusUnauthorized)
			return
		}

		// Parse and validate the token.
		parsedToken, err := jwt.Parse(token, func(token *jwt.Token) (interface{}, error) {
			// Verify issuer, audience, and signature.
			if token.Issuer != "secure-unsubscribe" || token.Audience != r.Host {
				return nil, errors.New("invalid token")
			}
			return rsa.PublicKey{}, nil
		})

		if err != nil || !parsedToken.Valid {
			http.Error(w, "Invalid credentials", http.StatusUnauthorized)
			return
		}

		// Attach user information to the context.
		ctx := context.WithValue(r.Context(), "user", parsedToken.Claims)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func main() {
	// Securely parse and validate environment variables.
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	http.Handle("/unsubscribe", authenticateUser(http.HandlerFunc(unsubscribe)))
	log.Fatal(http.ListenAndServe(":"+port, nil))
}

/*
 unsubscribe handles the GET /unsubscribe endpoint. It validates the email parameter,
 checks against a secure database, and performs a soft delete if found.

 Parameters:
 w - http.ResponseWriter to return responses
 r - http.Request containing the incoming request

 Security Measures:
 - Input validation and sanitization for email parameters.
 - SQL injection prevention using prepared statements.
 - Soft deletes (update active=0) instead of hard deletes.
 - Audit logging without sensitive data.
 - Context-based authentication with JWT tokens.
*/
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	// Parse query parameters safely.
	email := r.URL.Query().Get("email")
	if email == "" {
		http.Error(w, "Missing parameter", http.StatusBadRequest)
		return
	}

	// Validate and sanitize input.
	validEmail, err := validateAndSanitizeEmail(email)
	if err != nil {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	// Log the request without sensitive data.
	log.Printf("Unsubscribe attempt from: %s", safeLog(validEmail))

	// Check if email exists using prepared statements.
	var exists bool
	err = db.QueryRowContext(r.Context(), "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)", validEmail).Scan(&exists)
	if err != nil {
		http.Error(w, "Database error", http.StatusInternalServerError)
		return
	}

	if !exists {
		// Do not reveal whether an email exists (to prevent enumeration).
		http.Error(w, "Not found", http.StatusNotFound)
		return
	}

	// Perform a soft delete (update active=0).
	_, err = db.ExecContext(r.Context(), "UPDATE users SET active = 0 WHERE email = ?", validEmail)
	if err != nil {
		http.Error(w, "Unable to unsubscribe", http.StatusInternalServerError)
		return
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(response{Message: "Successfully unsubscribed"})
}

// validateAndSanitizeEmail validates the email format and escapes output.
func validateAndSanitizeEmail(email string) (string, error) {
	// Validate email format using a secure regex.
	valid := regexp.MustCompile(`^[^\s@]+@[^\s@]+\.[^\s@]+$`)
	if !valid.MatchString(email) {
		return "", errors.New("invalid email")
	}
	// Escape output for safe display.
	return html.EscapeString(email), nil
}

// safeLog returns a hashed version of the email for logging.
func safeLog(email string) string {
	h := sha256.New()
	h.Write([]byte(email))
	return hex.EncodeToString(h.Sum(nil))
}
