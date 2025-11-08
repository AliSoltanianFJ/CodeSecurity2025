package main

import (
	"fmt"
	"net/http"
	"strings"
	"regexp"
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
	re := regexp.MustCompile("[^a-zA-Z0-9]+")
	username = re.ReplaceAllString(username, "")
	if username == "" {
		fmt.Fprint(w, "Hello!")
		return
	}
	fmt.Fprintf(w, "Hello, %s!", username)
}