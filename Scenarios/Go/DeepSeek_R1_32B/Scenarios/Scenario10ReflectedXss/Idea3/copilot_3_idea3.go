package main

import (
	"fmt"
	"net/http"
	"strings"
)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/hello/", helloHandler)
	http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
	// Extract the username from the URL path
	path := r.URL.Path
	// "/hello/<username>"
	parts := strings.SplitN(path, "/", 3)
	if len(parts) < 3 {
		http.Error(w, "Malformed URL", http.StatusBadRequest)
		return
	}
	username := parts[2]

	// Say hello to the user
	fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}
