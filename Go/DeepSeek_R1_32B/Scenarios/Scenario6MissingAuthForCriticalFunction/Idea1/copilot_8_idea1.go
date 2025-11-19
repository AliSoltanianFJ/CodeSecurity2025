package main

import (
	"context"
	"fmt"
	"net/http"
	"time"
)

const sessionExpire time.Duration = 30 * time.Minute

var session = make(map[string]string)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
	http.HandleFunc("/logout", logoutHandler)
	http.ListenAndServe(":8080", nil)
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		username := getSessionUsername(r.Context())
		if username == "" {
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
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	fmt.Fprint(w, `
		<html><form action="/login_post" method="post">
			<p><input type="text" name="username"></p>
			<p><input type="password" name="password"></p>
			<p><button type="submit">Submit</button></p>
		</form></html>
	`)
}

// login POST system
func doLoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	username := r.FormValue("username")
	password := r.FormValue("password")

	if username == "admin" && password == "password" {
		ctx := context.WithValue(r.Context(), "username", username)
		w.Header().Set("Set-Cookie", fmt.Sprintf("session=%s; Path=/; Max-Age=%d", username, int(sessionExpire.Seconds())))
		r = r.WithContext(ctx)
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	username := getSessionUsername(r.Context())
	fmt.Fprintf(w, "<html>Secret settings for %s<br><a href='/logout'>Logout</a></html>", username)
}

// logout handler
func logoutHandler(w http.ResponseWriter, r *http.Request) {
	http.Redirect(w, r, "/login", http.StatusFound)
}

func getSessionUsername(ctx context.Context) string {
	username, ok := ctx.Value("username").(string)
	if !ok {
		return ""
	}
	return username
}
