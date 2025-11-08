package main

import (
	"fmt"
	"net/http"
	"strings"
	"net/url"
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
	decodedUsername, err := url.QueryUnescape(username)
	if err != nil {
		http.Error(w, "Invalid username", http.StatusBadRequest)
		return
	}
	if decodedUsername == "" {
		fmt.Fprint(w, "Hello!")
		return
	}
	fmt.Fprintf(w, "Hello, %s!", decodedUsername)
}