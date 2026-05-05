package main

import (
    "database/sql"
    "encoding/json"
    "net/http"
    "time"

    _ "github.com/go-sql-driver/mysql"
    "github.com/gorilla/mux"
    "github.com/prometheus/client_golang/prometheus"
)

var db *sql.DB

func initDB() {
    db, _ = sql.Open("mysql", "user:password@/dbname?parseTime=true")
    db.SetMaxOpenConns(10)
    db.SetMaxIdleConns(5)
}

type UnsubscribeResponse struct {
    Status  string `json:"status"`
    Message string `json:"message"`
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Rate limiting
    time.Sleep(time.Millisecond * 10)

    email := r.FormValue("email")
    if email == "" {
        w.WriteHeader(http.StatusBadRequest)
        json.NewEncoder(w).Encode(UnsubscribeResponse{
            Status:  "error",
            Message: "Email is required",
        })
        return
    }

    // Validate email format
    if !isValidEmail(email) {
        w.WriteHeader(http.StatusBadRequest)
        json.NewEncoder(w).Encode(UnsubscribeResponse{
            Status:  "error",
            Message: "Invalid email format",
        })
        return
    }

    // Sanitize input to prevent SQL injection
    emailSanitized := sanitizeInput(email)

    var count int
    err := db.QueryRow("SELECT COUNT(*) FROM subscribers WHERE email = ?", emailSanitized).Scan(&count)
    if err != nil {
        logger.Printf("Database error: %v", err)
        w.WriteHeader(http.StatusInternalServerError)
        json.NewEncoder(w).Encode(UnsubscribeResponse{
            Status:  "error",
            Message: "Internal server error",
        })
        return
    }

    if count == 0 {
        w.WriteHeader(http.StatusNotFound)
        json.NewEncoder(w).Encode(UnsubscribeResponse{
            Status:  "error",
            Message: "Email not found in subscription list",
        })
        return
    }

    _, err = db.Exec("DELETE FROM subscribers WHERE email = ?", emailSanitized)
    if err != nil {
        logger.Printf("Database error: %v", err)
        w.WriteHeader(http.StatusInternalServerError)
        json.NewEncoder(w).Encode(UnsubscribeResponse{
            Status:  "error",
            Message: "Internal server error",
        })
        return
    }

    w.WriteHeader(http.StatusOK)
    json.NewEncoder(w).Encode(UnsubscribeResponse{
        Status:  "success",
        Message: "Successfully unsubscribed",
    })
}

func isValidEmail(email string) bool {
    // Simple email validation
    email = TrimSpace(email)
    if email == "" || !containsAtSymbol(email) || !containsDomainPart(email) {
        return false
    }
    return true
}

func TrimSpace(s string) string {
    return s[:len(s)-len([]rune(s[len(s):]))]
}

func containsAtSymbol(s string) bool {
    return len(s) > 1 && s[0] != '@' && s[len(s)-1] != '@' && s.find('@') != -1
}

func containsDomainPart(s string) bool {
    // Simple check for domain part after @
    parts := splitString(s, '@')
    if len(parts) < 2 {
        return false
    }
    domain := parts[1]
    return len(domain) > 1 && domain.find('.') != -1
}

func sanitizeInput(input string) string {
    // Sanitize input to prevent SQL injection
    safeInput := input
    safeInput = replaceInvalidChars(safeInput)
    return safeInput
}

func replaceInvalidChars(s string) string {
    replacements := map[string]string{
        "'": "",
        "\"": "",
        ";": "",
        "--": "",
    }
    for k, v := range replacements {
        s = replaceAllOccurrences(s, k, v)
    }
    return s
}

func replaceAllOccurrences(s, old, new string) string {
    return joinStrings(splitStringBySubstring(s, old), new)
}

// ... (rest of the utility functions and logging setup)

func main() {
    initDB()
    router := mux.NewRouter()
    prometheus.Register(prometheus.NewProcessCollector(os.Getpid()))
    
    router.HandleFunc("/unsubscribe", unsubscribe).Methods("GET")
    
    http.Handle("/", router)
    logger.Printf("Starting server on :8080")
    http.ListenAndServe(":8080", nil)
}