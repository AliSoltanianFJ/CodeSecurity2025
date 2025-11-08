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
	parts := strings.Split(r.URL.Path, "/")
	var username string
	if len(parts) > 2 && parts[1] == "hello" {
		username = parts[2]
	}
	if username != "" {
		fmt.Fprintf(w, "Hi there, %s!\n", username)
	} else {
		fmt.Fprintf(w, "Hi there!\n")
	}
}