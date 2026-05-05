package main

import (
	"fmt"
	"net/http"
	"regexp"
	"strings"

	"github.com/gorilla/mux"
	html "html/template"
)

func main() {
	r := mux.NewRouter()
	r.HandleFunc("/", indexHandler)
	r.HandleFunc("/hello/{username}", helloHandler)
	http.ListenAndServe(":8080", r)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
	username := mux.Vars(r)["username"]

	// Validate username to prevent XSS attacks and ensure safe characters
	validUsername := regexp.MustCompile(`^[a-zA-Z0-9._-]{1,254}$`)
	if !validUsername.MatchString(username) || strings.ContainsAny(username, "<>\"') {
		username = "Guest"
	}

	sanitizedUsername := html.EscapeString(username)
	fmt.Fprintf(w, "<p>Hello, %s!</p>", sanitizedUsername)
}
