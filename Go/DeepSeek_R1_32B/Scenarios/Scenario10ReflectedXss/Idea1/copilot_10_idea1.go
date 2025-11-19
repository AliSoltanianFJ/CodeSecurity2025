package main

import (
	"fmt"
	"net/http"
	"strings"

	"github.com/gorilla/mux"
	html "html/template"
)

func main() {
	r := mux.NewRouter()
	r.HandleFunc("/", indexHandler)
	r.HandleFunc("/hello/{username}", helloHandler).Methods("GET")
	http.ListenAndServe(":8080", r)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	username := vars["username"]
	if username == "" {
		fmt.Fprintf(w, "<p>Please provide a valid username in the format /hello/yourname</p>")
		return
	}
	safeUsername := html.HTMLEscapeString(strings.TrimSpace(username))
	greeting := fmt.Sprintf("<p>Hello, %s!</p>", safeUsername)
	fmt.Fprint(w, greeting)
}
