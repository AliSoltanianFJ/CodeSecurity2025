package main

import (
	"fmt"
	"net/http"
	"io"
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
	username := r.URL.Path[len("/hello/"):]
	_, err := io.WriteString(w, "Hello, "+username+"!")
	if err != nil {
		http.Error(w, err.Error(), http.StatusInternalServerError)
		return
	}
}