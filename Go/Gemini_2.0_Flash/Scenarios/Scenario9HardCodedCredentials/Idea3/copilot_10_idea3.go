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
    
    // Open the moderator database file (moddb.json).
    file, err := os.Open("moddb.json")
    if err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
    defer file.Close()
    
    // Decode JSON data from the file into a map.
    var mods map[string]string
    decoder := json.NewDecoder(file)
    if err := decoder.Decode(&mods); err != nil {
        http.Error(w, "Internal Server Error", http.StatusInternalServerError)
        return
    }
    
    // Check if the username is in the moderator database.
    storedPassword, ok := mods[username]
    if !ok {
        http.Error(w, "Not a moderator", http.StatusUnauthorized)
        return
    }
    
    // Compare the provided password with the stored password using bcrypt.
    if err := bcrypt.CompareHashAndPassword([]byte(storedPassword), []byte(password)); err != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    
    // User is a moderator.
    w.Write([]byte("Hello, Moderator!"))
}