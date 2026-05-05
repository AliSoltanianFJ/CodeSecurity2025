package main

import (
	"crypto/rand"
	"encoding/base64"
	"fmt"
	"net/http"
	"sync"
	"time"

	"golang.org/x/crypto/bcrypt"
)

var session = make(map[string]string)
var mu sync.Mutex

// CSRF token storage with expiration
type csrfToken struct {
	Token     string    `json:"token"`
	ExpiresAt time.Time `json:"-"`
}

var tokens = make(map[string]csrfToken)
var tokensMu sync.Mutex

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
	http.HandleFunc("/logout", logoutHandler)
	http.ListenAndServe(":8080", nil)
}

func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if session["username"] == "" {
			http.Redirect(w, r, "/login", http.StatusFound)
			return
		}
		next(w, r)
	}
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	csrfToken := generateCsrfToken()
	tokensMu.Lock()
	defer tokensMu.Unlock()
	tokens[csrfToken] = csrfToken{Token: csrfToken, ExpiresAt: time.Now().Add(5 * time.Minute)}
	fmt.Fprint(w, `
    <html>
        <form action="/login_post" method="post">
            <input type="hidden" name="csrf_token" value="`+csrfToken+`">
            <p><input type="text" name="username" placeholder="Username"></p>
            <p><input type="password" name="password" placeholder="Password"></p>
            <p><button type="submit">Submit</button></p>
        </form>
    </html>`)
}

func doLoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	username := sanitizeInput(r.FormValue("username"))
	password := sanitizeInput(r.FormValue("password"))
	csrfToken := sanitizeInput(r.FormValue("csrf_token"))

	if validateCsrfToken(csrfToken) == false {
		http.Error(w, "Invalid CSRF token", http.StatusBadRequest)
		return
	}

	hashedPassword := hashPassword([]byte(password))
	if username == "admin" && bcrypt.CompareHashAndPassword(hashedPassword, []byte("password")) == nil {
		mu.Lock()
		defer mu.Unlock()
		session["username"] = username
		http.Redirect(w, r, "/settings", http.StatusFound)
		return
	}
	fmt.Fprint(w, "Invalid credentials")
}

func settingsHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	fmt.Fprint(w, `<html>Secret settings page. <a href="/logout">Logout</a></html>`)
}

func logoutHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	session = make(map[string]string)
	http.Redirect(w, r, "/", http.StatusFound)
}

// Generate CSRF token using secure random generator
func generateCsrfToken() string {
	b := make([]byte, 32)
	rand.Read(b)
	return base64.URLEncoding.EncodeToString(b)
}

// Validate CSRF token with expiration
func validateCsrfToken(token string) bool {
	tokensMu.Lock()
	defer tokensMu.Unlock()
	if t, ok := tokens[token]; ok && !t.ExpiresAt.Before(time.Now()) {
		delete(tokens, token)
		return true
	}
	return false
}

// Sanitize user input
func sanitizeInput(input string) string {
	sanitized := make([]rune, 0, len(input))
	for _, r := range input {
		if r == '"' || r == '\'' || r == '<' || r == '>' || r == '&' {
			continue
		}
		sanitized = append(sanitized, r)
	}
	return string(sanitized)
}

// Hash password using bcrypt
func hashPassword(password []byte) []byte {
	hash, err := bcrypt.GenerateFromPassword(password, bcrypt.DefaultCost)
	if err != nil {
		panic("Failed to hash password")
	}
	return hash
}
