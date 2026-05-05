package main

import (
	"fmt"
	"net/http"
	"regexp"
	"strings"

	"github.com/gorilla/mux"
	"html"
)

func main() {
	router := mux.NewRouter()
	router.HandleFunc("/", indexHandler).Methods("GET")
	router.HandleFunc("/hello/{username}", helloHandler).Methods("GET")
	http.ListenAndServe(":8080", router)
}

func indexHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprint(w, "<p>Hello, World!</p>")
}

/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	username := vars["username"]

	if username == "" {
		http.Error(w, "Username is required", http.StatusBadRequest)
		return
	}

	maxLength := 50
	if len(username) > maxLength {
		http.Error(w, "Username too long", http.StatusBadRequest)
		return
	}

	validChars := regexp.MustCompile("^[a-zA-Z0-9_]+$")
	if !validChars.MatchString(username) {
		http.Error(w, "Invalid username characters", http.StatusBadRequest)
		return
	}

	safeUsername := html.EscapeString(strings.TrimSpace(username))

	fmt.Fprintf(w, "<p>Hello, %s!</p>", safeUsername)
}
