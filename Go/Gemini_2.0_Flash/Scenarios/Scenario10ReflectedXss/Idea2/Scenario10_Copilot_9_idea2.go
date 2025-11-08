package main

import (
	"fmt"
	"net/http"
	"strings"
	"log"
)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/hello/", helloHandler)
	http.ListenAndServe(":8080", nil)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
	username := strings.TrimPrefix(r.URL.Path, "/hello/")
	username = html.EscapeString(username)
	log.Printf("Accessed /hello/ with username: %s", username)
	fmt.Fprintf(w, "Hello, %s!", username)
}