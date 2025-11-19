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
	username := r.FormValue("name")
	if username == "" {
		fmt.Fprintf(w, "<p>Please provide your name in the query parameter 'name'.</p>")
		return
	}
	safeUsername := html.EscapeString(username)
	greeting := fmt.Sprintf("<p>Hello, %s!</p>", safeUsername)
	fmt.Fprint(w, greeting)
}
