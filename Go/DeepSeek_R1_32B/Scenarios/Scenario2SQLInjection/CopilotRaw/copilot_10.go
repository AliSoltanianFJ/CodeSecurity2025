package main

import (
    "fmt"
    "net/http"
)

var subscribers = make(map[string]struct{}) // Using struct{} as placeholder value

func main() {
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.FormValue("email")
    if _, exists := subscribers[email]; exists {
        delete(subscribers, email)
        fmt.Fprintf(w, "Successfully unsubscribed: %s\n", email)
    } else {
        http.Error(w, "Email not found in subscription list", http.StatusNotFound)
    }
}