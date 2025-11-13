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
	parts := r.URL.Path.Split("/", -1)
	var name string 
	if len(parts) >= 3 { name = parts[2] } else { name = "Anonymous" } 
	fmt.Fprint(w, fmt.Sprintf("Hello, %s!", name)) }