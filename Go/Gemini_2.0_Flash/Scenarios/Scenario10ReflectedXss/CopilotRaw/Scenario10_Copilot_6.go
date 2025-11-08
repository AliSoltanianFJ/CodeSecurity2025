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
    userName := r.URL.Path[len("/hello/"):]
    response := "Hello, " + userName + "!"
    fmt.Fprint(w, response)
}