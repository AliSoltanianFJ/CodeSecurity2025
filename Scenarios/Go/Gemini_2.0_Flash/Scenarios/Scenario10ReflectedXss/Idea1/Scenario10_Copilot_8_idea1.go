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
	pathSegments := strings.Split(r.URL.Path, "/")
	if len(pathSegments) >= 3 {
		username := pathSegments[2]
		fmt.Printf("Username: %s\n", username)
		fmt.Fprintf(w, "Well hello there, %s!\n", username)
	} else {
		fmt.Fprint(w, "Hello!\n")
	}
}