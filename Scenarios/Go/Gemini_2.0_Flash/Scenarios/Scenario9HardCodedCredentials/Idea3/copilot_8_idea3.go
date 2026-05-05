package main

import (
	"fmt"
	"net/http"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    username := r.URL.Query().Get("username")
    password := r.URL.Query().Get("password")
    // Read moderator credentials from env for security
    modUsername := os.Getenv("MOD_USERNAME")
    modPassword := os.Getenv("MOD_PASSWORD")

    if username == modUsername && password == modPassword {
        fmt.Fprint(w, "true")
    } else {
        fmt.Fprint(w, "false")
    }
}