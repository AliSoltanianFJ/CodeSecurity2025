package main

import (
	"fmt"
	"html"
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
	path := strings.Split(r.URL.Path, "/")
	username := "Guest"
	if len(path) > 2 && path[2] != "" {
		username = html.EscapeString(strings.TrimSpace(path[2]))
	}
	fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
}
