package main

import (
	"fmt"
	"net/http"

	"github.com/gorilla/sessions"
)

var store = sessions.NewCookieStore([]byte("your-secure-key"))

func main() {
	middleWare := MiddlewareStack{
		Handlers: []HandlerWithMiddleware{
			{Pattern: "/", Handler: indexHandler},
			{Pattern: "/login", Handler: loginHandler},
			{Pattern: "/login_post", Handler: doLoginHandler},
			{Pattern: "/settings", Handler: settingsHandler, Middlewares: []func(http.HandlerFunc) http.HandlerFunc{ensureLoggedIn}},
		},
	}

	http.HandleFunc("/", middleWare.Handle)
	http.ListenAndServe(":8080", nil)
}

type MiddlewareStack struct {
	Handlers []HandlerWithMiddleware
}

type HandlerWithMiddleware struct {
	Pattern     string
	Handler     http.HandlerFunc
	Middlewares []func(http.HandlerFunc) http.HandlerFunc
	Store       *sessions.CookieStore
}

func (m *MiddlewareStack) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	for _, handler := range m.Handlers {
		if r.URL.Path == handler.Pattern {
			m.applyMiddlewares(handler)
			handler.Handler(w, r)
			return
		}
	}
	http.NotFound(w, r)
}

func (m *MiddlewareStack) Handle(rw http.ResponseWriter, req *http.Request) {
	for _, h := range m.Handlers {
		if h.Pattern == req.URL.Path {
			m.applyMiddlewares(h)
			h.Handler(rw, req)
			return
		}
	}
	http.NotFound(rw, req)
}

func (m *MiddlewareStack) applyMiddlewares(handler HandlerWithMiddleware) http.HandlerFunc {
	next := handler.Handler
	for _, middleware := range handler.Middlewares {
		next = middleware(next)
	}
	handler.Handler = next
}

// ensureLoggedIn decorator equivalent
func ensureLoggedIn(store *sessions.CookieStore) func(http.HandlerFunc) http.HandlerFunc {
	return func(next http.HandlerFunc) http.HandlerFunc {
		return func(w http.ResponseWriter, r *http.Request) {
			session, err := store.Get(r, "session")
			if err != nil {
				http.Error(w, "Error retrieving session", http.StatusInternalServerError)
				return
			}
			if username, ok := session.Values["username"].(string); !ok || username == "" {
				http.Redirect(w, r, "/login", http.StatusFound)
				return
			}
			next(w, r)
		}
	}
}

// home page
func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, `<html>Hello! <a href='./login'>Login here</a></html>`)
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
		session, err := store.Get(r, "session")
		if err != nil {
			http.Error(w, "Error creating session", http.StatusInternalServerError)
			return
		}
		session.Values["username"] = username
		session.Save(r.Context(), w)
		http.Redirect(w, r, "/settings", http.StatusFound)
	} else {
		fmt.Fprint(w, "Bad username or password")
	}
}

// user settings page
func settingsHandler(w http.ResponseWriter, r *http.Request) {
	session := r.Context().Value("session").(*sessions.Session)
	username := session.Values["username"].(string)
	fmt.Fprintf(w, "<html>Secret settings - Welcome %s</html>", username)
}
