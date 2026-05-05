package main

import (
	"fmt"
	"net/http"
	"log"
)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/hello/", helloHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

func helloHandler(w http.ResponseWriter, r *http.Request) {
	name := r.URL.Path[len("/hello/"):]
	if name == "" {
		fmt.Fprint(w, "Hello, Guest!")
		return
	}
	fmt.Fprintf(w, "Hello, %s!", name)
}