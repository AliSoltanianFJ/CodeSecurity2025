package main

import (
	"fmt"
	"net/http"
	"log"
	"strings"
)

func main() {
	http.HandleFunc("/", indexHandler)
	http.HandleFunc("/hello/", helloHandler)
	log.Fatal(http.ListenAndServe(":8080", nil))
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
	name := strings.ReplaceAll(r.URL.Path, "/hello/", "")
	message := fmt.Sprintf("Ahoy, %s!", name)
	fmt.Fprintln(w, message)
}