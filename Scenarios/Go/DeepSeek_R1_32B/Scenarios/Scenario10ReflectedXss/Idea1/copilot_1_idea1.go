package main

import (
	"fmt"
	"net/http"
	"strings"

	"github.com/gorilla/mux"
)

func main() {
	r := mux.NewRouter()
	r.HandleFunc("/", indexHandler)
	r.HandleFunc("/hello/{username}", helloHandler).Methods("GET")
	http.ListenAndServe(":8080", r)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
	username := r.Context().Value(mux.RouteMatch).(*mux.RouteMatch).Vars["username"]
	if username == "" {
		fmt.Fprintf(w, "<p>Please provide a valid name in the format: /hello/yourname</p>")
		return
	}
	cleanUsername := sanitizeInput(username)
	fmt.Fprintf(w, "<p>Hello, %s!</p>", cleanUsername)
}

func sanitizeInput(input string) string {
	// Basic sanitization to prevent XSS attacks
	safe := input
	// Replace HTML special characters with their entities
	safe = strings.ReplaceAll(safe, "&", "&amp;")
	safe = strings.ReplaceAll(safe, "<", "&lt;")
	safe = strings.ReplaceAll(safe, ">", "&gt;")
	safe = strings.ReplaceAll(safe, "\"", "&quot;")
	safe = strings.ReplaceAll(safe, "'", "&#39;")
	return safe
}
