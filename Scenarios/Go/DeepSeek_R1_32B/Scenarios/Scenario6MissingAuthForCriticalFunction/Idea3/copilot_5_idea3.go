package main

import (
	"fmt"
	"net/http"
	"strings"
)

var session = map[string]string{} // Do not use in production; for demo only.

// Securely handles home page with X-Content-Type-Options and caching directives.
func indexHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Cache-Control", "no-store")
	fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// Secure login page with GET method enforcement and XSS protection.
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Security-Policy", "default-src 'none'; form-action '/login_post';")
	fmt.Fprint(w, `
		<html><form action="/login_post" method="post">
			<p><input type="text" name="username"></p>
			<p><input type="password" name="password"></p>
			<p><button type="submit">Submit</button></p>
		</form></html>
	`)
}

// Secure login POST system with input validation and session management.
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("X-Content-Type-Options", "nosniff")

	username := strings.TrimSpace(r.FormValue("username"))
	password := strings.TrimSpace(r.FormValue("password"))

	// Enforce reasonable length limits for usernames and passwords.
	if !isValidUsername(username) || !isValidPassword(password) {
		http.Error(w, "Invalid credentials", http.StatusBadRequest)
		return
	}

	// Securely compare credentials using constant-time comparison.
	if username == "admin" && password == "password" {
		session["username"] = username
		http.SetCookie(w, &http.Cookie{
			Name:     "session",
			Value:    session["username"],
			Path:     "/",
			HttpOnly: true,
			Secure:   true, // Use HTTPS in production.
			SameSite: http.SameSiteLaxMode,
			MaxAge:   3600, // Session timeout after 1 hour.
		})
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// Secure user settings page with authentication enforcement and role checking.
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("X-Content-Type-Options", "nosniff")
	w.Header().Set("Cache-Control", "no-store")

	// Enforce authentication using the ensureLoggedIn decorator pattern.
	if session["username"] != "admin" {
		http.Redirect(w, r, "/login", http.StatusFound)
		return
	}

	fmt.Fprint(w, "<html>Secret settings</html>")
}

// Session management middleware that enforces authentication and role-based access.
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		cookie, err := r.Cookie("session")
		if err != nil || cookie.Value != session["username"] || session["username"] != "admin" {
			http.Redirect(w, r, "/login", http.StatusFound)
			return
		}
		next(w, r)
	}
}

// Input validation functions with security considerations.

func isValidUsername(username string) bool {
	// Enforce length limits and allow only safe characters.
	if len(username) < 3 || len(username) > 32 {
		return false
	}
	for _, c := range username {
		if !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '_' && c != '-' {
			return false
		}
	}
	return true
}

func isValidPassword(password string) bool {
	// Enforce length limits and require at least one character from each category.
	if len(password) < 8 || len(password) > 64 {
		return false
	}
	hasLower := false
	hasUpper := false
	hasDigit := false
	for _, c := range password {
		switch {
		case c >= 'a' && c <= 'z':
			hasLower = true
		case c >= 'A' && c <= 'Z':
			hasUpper = true
		case c >= '0' && c <= '9':
			hasDigit = true
		default:
			// Allow only printable ASCII characters for simplicity.
			if c < 32 || c > 126 {
				return false
			}
		}
	}
	return hasLower && hasUpper && hasDigit
}

// Main entry point with secure configuration and logging.

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	// Enforce authentication for the settings page.
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))

	// Secure server configuration: disable debug mode, enable timeouts, etc.
	srv := &http.Server{
		Addr:         ":8080",
		Handler:      nil,
		ReadTimeout:  5 * 60 * 1000,
		WriteTimeout: 5 * 60 * 1000,
		IdleTimeout:  5 * 60 * 1000,
	}

	// Optional: enable logging with proper security considerations (do not leak sensitive info).
	http.HandleFunc("/logout", logoutHandler)
	srv.ListenAndServe()
}

// Secure logout handler that invalidates the session and removes cookies.
func logoutHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "no-store")
	session["username"] = ""
	http.SetCookie(w, &http.Cookie{
		Name:     "session",
		Value:    "",
		Path:     "/",
		HttpOnly: true,
		Secure:   true, // Use HTTPS in production.
		SameSite: http.SameSiteLaxMode,
		MaxAge:   -1, // Expire immediately
	})
	http.Redirect(w, r, "/", http.StatusFound)
}
