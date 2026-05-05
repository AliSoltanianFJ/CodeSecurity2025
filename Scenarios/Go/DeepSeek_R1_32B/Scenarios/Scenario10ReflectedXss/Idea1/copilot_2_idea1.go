package main

import (
	"fmt"
	"html"
	"net/http"
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
	pathParts := r.URL.Path.Split("/")
	username := "Guest"
	if len(pathParts) > 2 {
		username = pathParts[2]
		username = html.EscapeString(username)
		username = fmt.Sprintf("%v", username)
		username = username[:10] // limit the length to prevent potential issues
	}
	fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
}
