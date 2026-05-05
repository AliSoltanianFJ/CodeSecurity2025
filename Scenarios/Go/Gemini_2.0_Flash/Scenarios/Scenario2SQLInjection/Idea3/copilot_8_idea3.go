package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
// Define a struct to hold user data
type User struct {
    ID    int
    Email string
}

// db is a simplified in-memory database
var db = make(map[int]User)
var nextID = 1

// registerUser adds a new user to the database
func registerUser(email string) error {
    // Ensure email is not empty
    if email == "" {
        return errors.New("email cannot be empty")
    }
    // Ensure email is valid
    if !isValidEmail(email) {
        return errors.New("email is invalid")
    }
    // Prevent duplicate emails
    for _, user := range db {
        if user.Email == email {
            return errors.New("email already registered")
        }
    }
    // Add user to db
    user := User{
        ID:    nextID,
        Email: email,
    }
    db[nextID] = user
    nextID++
    return nil
}

// isValidEmail checks if the given email is valid
func isValidEmail(email string) bool {
    // Basic email validation (only for demonstration)
    match, _ := regexp.MatchString(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`, email)
    return match
}

// unsubscribe removes a user by email from the database
func unsubscribe(w http.ResponseWriter, r *http.Request) {
    // Parse email from GET variable
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "Email parameter is required", http.StatusBadRequest)
        return
    }
    // Find user by email
    userID := -1
    for id, user := range db {
        if user.Email == email {
            userID = id
            break
        }
    }
    // User not found
    if userID == -1 {
        http.Error(w, "Email not found", http.StatusNotFound)
        return
    }
    // Remove user from db
    delete(db, userID)
    fmt.Fprintf(w, "Successfully unsubscribed email: %s", email)
}

func main() {
    // Register sample user for demonstration
    _ = registerUser("sample@example.com")

    http.HandleFunc("/register", func(w http.ResponseWriter, r *http.Request) {
        if r.Method != http.MethodPost {
            http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
            return
        }
        // Parse email from form
        if err := r.ParseForm(); err != nil {
            http.Error(w, "Invalid form data", http.StatusBadRequest)
            return
        }
        email := r.FormValue("email")
        if err := registerUser(email); err != nil {
            http.Error(w, err.Error(), http.StatusBadRequest)
            return
        }
        fmt.Fprintf(w, "Successfully registered email: %s", email)
    })

    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}