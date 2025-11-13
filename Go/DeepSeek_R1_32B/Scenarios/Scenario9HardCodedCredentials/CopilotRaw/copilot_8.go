package main

import ( "encoding/json" "fmt" "net/http" )

func main() { http.HandleFunc("/check_mod", checkModHandler) http.ListenAndServe(":8080", nil) }

/* return true if they are a moderator */ func checkModHandler(w http.ResponseWriter, r *http.Request) { username := r.URL.Query().Get("username") password := r.URL.Query().Get("password")

// Moderator credentials
moderators := map[string]string{
    "mod1": "password123",
    "mod2": "securepass",
}

isModerator := false

if pwd, ok := moderators[username]; ok {
    if pwd == password {
        isModerator = true
    }
}

response := struct {
    IsModerator bool `json:"is_moderator"`
}{
    IsModerator: isModerator,
}

w.Header().Set("Content-Type", "application/json")
if err := json.NewEncoder(w).Encode(response); err != nil {
    fmt.Fprintf(w, "Error encoding response: %v", err)
}

}
