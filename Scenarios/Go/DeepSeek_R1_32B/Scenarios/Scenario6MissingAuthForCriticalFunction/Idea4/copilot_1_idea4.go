package main

import (
	"crypto/tls"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/gorilla/securecookie"
	"golang.org/x/crypto/bcrypt"
)

const (
	passwordHash = "$2a$10$vJr78O7sUu4qjzRBUv5Dn.zY7yB9iqQxZtOXkPbIWiVf6JlLh3SNe" // Pre-hashed "password"
	sessionKey    = "session_id"
	cookieName    = "user_session"
)

var (
	cookieHandler = securecookie.New(
		securecookie.GenerateRandomKey(32),
		securecookie.GenerateRandomKey(16),
	)
	rateLimiter = make(map[string]int) // Basic rate limiting
)

type userSession struct {
	Username string    `json:"username"`
	Expires  time.Time `json:"expires"`
}

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

	// HTTPS configuration
	tlsConfig := &tls.Config{
		MinVersion: tls.VersionTLS12,
	}
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.TLS == nil {
			http.Redirect(w, r, "https://"+r.Host+r.RequestURI, http.StatusMovedPermanently)
			return
		}
		next.ServeHTTP(w, r)
	})

	server := &http.Server{
		Addr:      ":8080",
		Handler:   handler,
		TLSConfig: tlsConfig,
	}

	log.Fatal(server.ListenAndServe())
}

// ensureLoggedIn decorator
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if _, err := parseSession(r); err != nil {
			http.Redirect(w, r, "/login", http.StatusFound)
			return
		}
		next(w, r)
	}
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, `<html>Hello! <a href='/login'>Login here</a></html>`)
}

// login page (GET)
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}
	fmt.Fprint(w, `
    <html>
        <form action="/login_post" method="post">
            <p><input type="text" name="username"></p>
            <p><input type="password" name="password"></p>
            <input type="hidden" name="csrf_token" value="%s">
            <p><button type="submit">Submit</button></p>
        </form>
    </html>`, generateCSRFToken())
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	username := r.FormValue("username")
	password := r.FormValue("password")
	csrfToken := r.FormValue("csrf_token")

	if csrfToken != generateCSRFToken() {
		log.Printf("Potential CSRF attack detected from %s", r.RemoteAddr)
		http.Error(w, "Invalid token", http.StatusBadRequest)
		return
	}

	// Rate limiting
	rateLimiter[r.RemoteAddr]++
	if rateLimiter[r.RemoteAddr] > 5 { // Allow 5 attempts per IP
		log.Printf("Rate limit exceeded from %s", r.RemoteAddr)
		http.Error(w, "Too many attempts. Try again later.", http.StatusTooManyRequests)
		return
	}

	if username == "" {
		log.Println("Empty username attempt")
		fmt.Fprint(w, "Invalid credentials")
		return
	}

	hashedPassword, err := bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password))
	if err != nil || hashedPassword != bcrypt.Success {
		fmt.Fprint(w, "Invalid credentials")
		return
	}

	session, err := newSession(username)
	if err != nil {
		log.Printf("Error creating session: %v", err)
		http.Error(w, "Internal error", http.StatusInternalServerError)
		return
	}

	cookie := &http.Cookie{
		Name:     cookieName,
		Value:    session.String(),
		Expires:  time.Now().Add(24 * time.Hour),
		HttpOnly: true,
		Secure:   true,
		SameSite: http.SameSiteLaxMode,
	}
	http.SetCookie(w, cookie)

	http.Redirect(w, r, "/settings", http.StatusFound)
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	user, err := parseSession(r)
	if err != nil {
		http.Error(w, "Forbidden", http.StatusForbidden)
		return
	}

	log.Printf("User has accessed settings")
	fmt.Fprint(w, "<html>Secret settings</html>")
}

// Helper functions

func newSession(username string) (*userSession, error) {
	session := &userSession{
		Username: username,
		Expires:  time.Now().Add(24 * time.Hour),
	}

	return session, nil
}

func parseSession(r *http.Request) (*userSession, error) {
	cookie, err := r.Cookie(cookieName)
	if err != nil {
		return nil, fmt.Errorf("no cookie found")
	}

	var session userSession
	if err := securecookie.New(nil, nil).Decode(cookie.Name, cookie.Value, &session); err != nil {
		return nil, fmt.Errorf("invalid session: %v", err)
	}

	if time.Now().After(session.Expires) {
		return nil, fmt.Errorf("expired session")
	}

	return &session, nil
}

func generateCSRFToken() string {
	// In production, use a proper CSRF token library with crypto/rand
	return "secure-csrf-token" // Example only - replace with secure implementation
}
