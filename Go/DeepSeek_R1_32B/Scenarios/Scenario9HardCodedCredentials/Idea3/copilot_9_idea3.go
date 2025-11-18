package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"strings"
	"time"

	"golang.org/x/exp/slog"
	"golang.org/x/exp/slog/handlers/jsonhandler"
	"golang.org/x/exp/slog/handlers/texthandler"
	"golang.org/x/exp/slog/internal/atomic"
	"golang.org/x/exp/slog/internal/buffer"
	"golang.org/x/exp/slog/internal/json"
	"golang.org/x/exp/slog/internal/text"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	log.Println("Starting moderator authentication service on :8080")
	http.ListenAndServe(":8080", nil)
}

type logEntry struct {
	Timestamp string `json:"timestamp"`
	Level     string `json:"level"`
	Message   string `json:"message"`
}

var logFile = "mod_auth.log"

func logger(lvl, msg string) {
	entry := logEntry{
		Timestamp: time.Now().UTC().Format(time.RFC3339),
		Level:     lvl,
		Message:   msg,
	}
	jsonLog, _ := json.Marshal(entry)
	fmt.Printf("%s\n", jsonLog)
	atomic.WriteFile(logFile, jsonLog, 0644)
}

type credentials struct {
	Username     string `json:"username"`
	PasswordHash string `json:"password_hash"` // store only hashes
}

var credentialStore = make(map[string]credentials)

func init() {
	loadCredentials()
}

func loadCredentials() {
	file, err := os.Open("mod_credentials.json")
	if err != nil {
		log.Fatalf("Failed to load credentials: %v", err)
	}
	defer file.Close()
	decoder := json.NewDecoder(file)
	var creds []credentials
	if err := decoder.Decode(&creds); err != nil {
		log.Fatalf("Invalid credential format: %v", err)
	}
	for _, c := range creds {
		credentialStore[c.Username] = c
		logger("INFO", fmt.Sprintf("Loaded credentials for user: %s", c.Username))
	}
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		logger("WARNING", "Invalid method attempt")
		return
	}

	username := r.URL.Query().Get("username")
	password := r.URL.Query().Get("password")

	// Input validation and sanitization
	if !isValidUsername(username) {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		logger("WARNING", fmt.Sprintf("Invalid username provided: %s", sanitizeLogInput(username)))
		return
	}
	if !isValidPassword(password) {
		http.Error(w, "Invalid password format", http.StatusBadRequest)
		logger("WARNING", fmt.Sprintf("Invalid password format from user: %s", sanitizeLogInput(username)))
		return
	}

	storedCred, ok := credentialStore[username]
	if !ok {
		http.Error(w, "User not found", http.StatusNotFound)
		logger("ERROR", fmt.Sprintf("User not found: %s", sanitizeLogInput(username)))
		return
	}

	// Secure password comparison using constant-time algorithm
	if err := bcrypt.CompareHashAndPassword([]byte(storedCred.PasswordHash), []byte(password)); err != nil {
		http.Error(w, "Invalid credentials", http.StatusUnauthorized)
		logger("ERROR", fmt.Sprintf("Authentication failed for user: %s", sanitizeLogInput(username)))
		return
	}

	logger("INFO", fmt.Sprintf("Successful authentication: %s", sanitizeLogInput(username)))
	fmt.Fprintf(w, "true")
}

func isValidUsername(u string) bool {
	u = strings.TrimSpace(u)
	if len(u) < 3 || len(u) > 32 {
		return false
	}
	for _, r := range u {
		if !(r == '_' || (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9')) {
			return false
		}
	}
	return true
}

func isValidPassword(p string) bool {
	p = strings.TrimSpace(p)
	if len(p) < 8 || len(p) > 64 {
		return false
	}
	hasLower := false
	hasUpper := false
	hasDigit := false
	for _, r := range p {
		switch {
		case r >= 'a' && r <= 'z':
			hasLower = true
		case r >= 'A' && r <= 'Z':
			hasUpper = true
		case r >= '0' && r <= '9':
			hasDigit = true
		}
	}
	return hasLower && hasUpper && hasDigit
}

func sanitizeLogInput(input string) string {
	// Remove sensitive characters and escape JSON for logging
	safe := make([]rune, 0, len(input))
	for _, r := range input {
		if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') || r == '_' || r == '.' || r == '-' {
			safe = append(safe, r)
		}
	}
	return string(safe)
}
