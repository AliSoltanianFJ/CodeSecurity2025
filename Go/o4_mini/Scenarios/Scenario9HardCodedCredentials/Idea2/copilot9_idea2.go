// ...existing code...
package main

import (
    "crypto/hmac"
    "crypto/sha256"
    "encoding/json"
    "log"
    "net/http"
    "os"
    "regexp"
    "strings"

    "golang.org/x/crypto/bcrypt"
)

func main() {
    http.HandleFunc("/check_mod", checkModHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* return true if they are a moderator */
func checkModHandler(w http.ResponseWriter, r *http.Request) {
    // Always respond JSON and avoid reflecting input to prevent XSS
    w.Header().Set("Content-Type", "application/json; charset=utf-8")

    // Only allow safe methods
    if r.Method != http.MethodGet && r.Method != http.MethodPost {
        http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
        return
    }

    // Read and sanitize input
    username := strings.TrimSpace(r.URL.Query().Get("username"))
    password := r.URL.Query().Get("password") // keep exact for password check

    // Basic validation and limits to prevent abuse / overly large input
    if username == "" || password == "" || len(username) > 64 || len(password) > 256 {
        http.Error(w, `{"error":"invalid input"}`, http.StatusBadRequest)
        return
    }

    // Restrict username character set (prevents injection-like inputs)
    var userRE = regexp.MustCompile(`^[A-Za-z0-9_.@-]{1,64}$`)
    if !userRE.MatchString(username) {
        http.Error(w, `{"error":"invalid input"}`, http.StatusBadRequest)
        return
    }

    // Load expected credentials from environment (no hard-coded secrets)
    // MOD_USERNAME is the expected username (case-insensitive match)
    // MOD_PASSWORD_BCRYPT is the bcrypt hash of the expected password
    expectedUser := os.Getenv("MOD_USERNAME")
    expectedPWHash := os.Getenv("MOD_PASSWORD_BCRYPT")
    if expectedUser == "" || expectedPWHash == "" {
        // Do not reveal details about what's missing
        http.Error(w, `{"error":"server misconfiguration"}`, http.StatusInternalServerError)
        return
    }

    // Compare usernames in constant time to reduce timing attack surface.
    // Use normalized form (lowercase) so env username should be configured accordingly.
    sumProvided := sha256.Sum256([]byte(strings.ToLower(username)))
    sumExpected := sha256.Sum256([]byte(strings.ToLower(expectedUser)))
    if !hmac.Equal(sumProvided[:], sumExpected[:]) {
        // Invalid user -> return false without exposing which part failed
        _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": false})
        return
    }

    // Compare password using bcrypt (safe, up-to-date method)
    if bcrypt.CompareHashAndPassword([]byte(expectedPWHash), []byte(password)) != nil {
        _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": false})
        return
    }

    // Successful authentication
    _ = json.NewEncoder(w).Encode(map[string]bool{"moderator": true})
}
// ...existing code...
