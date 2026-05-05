package main

import (
	"crypto/bcrypt"
	"fmt"
	"net/http"
	"time"

	"github.com/gorilla/csrf"
	"github.com/gorilla/sessions"
)

var sessionStore = sessions.NewCookieStore([]byte("your-secure-key-here"))
var userDB = map[string]string{
	"admin": hashedPassword,
}
const (
	hashedPassword = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi" // bcrypt hash for "password"
)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/login", loginHandler)
	http.HandleFunc("/login_post", doLoginHandler)
	http.HandleFunc("/settings", ensureLoggedIn(settingsHandler))
	csrf.Init(nil)
	http.ListenAndServe(":8080", nil)
}

func ensureLoggedIn(next http.HandlerFunc) http.HandlerFunc {
	return func(w http.ResponseWriter, r *http.Request) {
		session, err := sessionStore.Get(r, "session")
		if err != nil || session.Values["username"] == nil {
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
	csrfToken := csrf.Token(r)
	fmt.Fprintf(w, `
		<html>
			<form action="/login_post" method="post">
				%s
				<p><input type="text" name="username" placeholder="Username"></p>
				<p><input type="password" name="password" placeholder="Password"></p>
				<p><button type="submit">Submit</button></p>
			</form>
		</html>`, csrfToken)
}

func doLoginHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}
	username := r.FormValue("username")
	password := r.FormValue("password")

	if username == "" || password == "" {
		http.Error(w, "Username and password required", http.StatusBadRequest)
		return
	}

	hashedPwd, ok := userDB[username]
	if !ok {
		fmt.Fprint(w, `<html>Invalid credentials. <a href='/login'>Try again</a></html>`)
		return
	}

	err := bcrypt.CompareHashAndPassword([]byte(hashedPwd), []byte(password))
	if err != nil {
		fmt.Fprint(w, `<html>Invalid credentials. <a href='/login'>Try again</a></html>`)
		return
	}

	session, err := sessionStore.Get(r, "session")
	if err != nil {
		http.Error(w, "Session error", http.StatusInternalServerError)
		return
	}
	session.Values["username"] = username
	session.Options.MaxAge = 3600 // 1 hour
	err = session.Save(r, w)
	if err != nil {
		http.Error(w, "Session save error", http.StatusInternalServerError)
		return
	}

	http.Redirect(w, r, "/settings", http.StatusFound)
}

func settingsHandler(w http.ResponseWriter, r *http.Request) {
	session, err := sessionStore.Get(r, "session")
	if err != nil || session.Values["username"] == nil {
		http.Error(w, "Unauthorized", http.StatusForbidden)
		return
	}
	username := session.Values["username"].(string)
	fmt.Fprintf(w, `<html>Welcome to settings, %s! <a href='/'>Home</a></html>`, username)
}
