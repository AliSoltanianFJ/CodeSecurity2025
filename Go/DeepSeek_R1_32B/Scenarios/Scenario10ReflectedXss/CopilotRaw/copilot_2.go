package main

import (
	"fmt"
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
	path := r.URL.Path parts := splitPath(path) 
	if len(parts) < 3 { fmt.Fprint(w, "Invalid path") return } 
	username := parts[2] fmt.Fprintf(w, "Hello, %s!", username) }

func splitPath(path string) []string { return strings.Split(path, "/") }
