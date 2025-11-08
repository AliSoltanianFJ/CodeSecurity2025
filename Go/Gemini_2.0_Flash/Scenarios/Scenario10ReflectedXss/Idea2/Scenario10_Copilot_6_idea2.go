package main

import (
	"fmt"
	"net/http"
	"strings"
	"unicode"
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
	var validUsername strings.Builder
	for _, r := range username {
		if unicode.IsLetter(r) || unicode.IsDigit(r) {
			validUsername.WriteRune(r)
		}
	}
	username = validUsername.String()
	if username == "" {
		fmt.Fprint(w, "Hello!")
		return
	}
	fmt.Fprintf(w, "Hello, %s!", username)
}