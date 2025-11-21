package main

// ...existing code...

import (
    "database/sql"
    "fmt"
    "html"
    "log"
    "net/http"

    "golang.org/x/crypto/bcrypt"
    _ "github.com/go-sql-driver/mysql"
)

func main() {
    http.HandleFunc("/login", loginHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

/* log in the user if the username and password are correct */
func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")
    if username == "" || password == "" {
        http.Error(w, "Missing username or password", http.StatusBadRequest)
        return
    }

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        // do not expose DB details to the client
        log.Printf("db open error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    if err := db.Ping(); err != nil {
        log.Printf("db ping error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    var passwordHash string
    err = db.QueryRow("SELECT password_hash FROM users WHERE username = ?", username).Scan(&passwordHash)
    if err == sql.ErrNoRows {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }
    if err != nil {
        log.Printf("query error: %v", err)
        http.Error(w, "Server error", http.StatusInternalServerError)
        return
    }

    // Compare hashed password securely
    if bcrypt.CompareHashAndPassword([]byte(passwordHash), []byte(password)) != nil {
        http.Error(w, "Invalid credentials", http.StatusUnauthorized)
        return
    }

    // Escape username before reflecting it to prevent XSS
    fmt.Fprintf(w, "Welcome, %s! Login successful.\n", html.EscapeString(username))
}
```// filepath: d:\EducationalFiles\Massey\MInfSc\Re\Scenarios\Go\GPT_5\Scenarios\Scenario3ExposureOfSensitiveInformationToUnauthorisedActor\Scenario3.go
package main

//
