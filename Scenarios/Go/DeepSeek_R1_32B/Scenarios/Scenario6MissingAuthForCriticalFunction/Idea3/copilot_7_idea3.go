package main

import (
	"fmt"
	"net/http"
)

var session = make(map[string]string)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	// Secure settings handler with multiple layers of protection
	http.HandleFunc("/settings", ensureAuthorized(ensureLoggedIn(restrictedSettingsHandler)))
	http.ListenAndServe(":8080", nil)
}

// Enhanced authentication and authorization system
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if session["username"] == "" || session["api_key"] == "" {
			http.Error(w, "Authentication required", http.StatusForbidden)
			return
		}
		next(w, r)
	}
}

func ensureAuthorized(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
			return
		}
		// Enforce HTTPS in production (remove in development)
		if !isLocalhost(r.Host) && r.URL.Scheme != "https" {
			http.Redirect(w, r, "https://"+r.Host+r.URL.Path, http.StatusMovedPermanently)
			return
		}
		if session["role"] != "Administrator" {
			http.Error(w, "Authorization denied", http.StatusForbidden)
			w.Header().Set("WWW-Authenticate", "Basic realm=\"Secure Settings\"")
			return
		}
		next(w, r)
	}
}

// Home page with security considerations
func indexHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
}

// Login page (GET) with security headers
func loginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	fmt.Fprint(w, `
		<html><form action="/login_post" method="post">
			<p><input type="text" name="username"></p>
			<p><input type="password" name="password"></p>
			<p><button type="submit">Submit</button></p>
		</form></html>
	`)
}

// Login POST system with enhanced security
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Cache-Control", "no-store")
	username := r.FormValue("username")
	password := r.FormValue("password")

	// Simulated secure authentication (use real methods in production)
	if username == "admin" && password == "secure-password-123" {
		session["username"] = username
		session["api_key"] = generateAPIKey()
		session["role"] = "Administrator"
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// Secure settings page with role-based access control
func restrictedSettingsHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Cache-Control", "no-store")
	fmt.Fprintf(w, "<html>Secret settings (only for Administrators)</html>")
}

// Helper to generate API keys (use proper secrets in production)
func generateAPIKey() string {
	return "dev-" + "secure-api-key"
}

// Helper to check if request is local (for development only)
func isLocalhost(host string) bool {
	return host == "localhost" || host == "127.0.0.1"
}
