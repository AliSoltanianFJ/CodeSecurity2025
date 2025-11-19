package main

import (
	"fmt"
	"html"
	"net/http"

	"github.com/gorilla/mux"
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
	username, ok := vars["username"]
	if !ok || username == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "<p>Please provide a valid username.</p>")
		return
	}
	sanitizedUsername := html.EscapeString(username)
	greeting := fmt.Sprintf("<p>Hello, %s!</p>", sanitizedUsername)
	fmt.Fprint(w, greeting)
}
