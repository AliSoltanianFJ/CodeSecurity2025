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
    username := r.PathValue("username")
    if username == "" {
        http.Error(w, "Username is required", http.StatusBadRequest)
        return
    }
    fmt.Fprintf(w, "<p>Hello, %s!</p>", html.EscapeString(username))
}