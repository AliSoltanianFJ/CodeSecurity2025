package main

import (
	"fmt"
	"net/http"
	"strings"
	"encoding/json"
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
    username = html.EscapeString(username)

    response := map[string]string{"message": fmt.Sprintf("Hello, %s!", username)}
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response)
}