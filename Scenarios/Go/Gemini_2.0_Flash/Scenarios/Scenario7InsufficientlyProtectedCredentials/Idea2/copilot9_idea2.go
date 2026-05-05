func registerHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
        return
    }

    // Parse and validate form input
    err := r.ParseForm()
    if err != nil {
        http.Error(w, "Invalid form data", http.StatusBadRequest)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    // Input validation
    if username == "" || password == "" {
        http.Error(w, "Username and password are required", http.StatusBadRequest)
        return
    }

    // Sanitize input to prevent XSS
    username = sanitizeInput(username)

    // Hash the password securely using bcrypt
    hashedPassword, err := hashPassword(password)
    if err != nil {
        http.Error(w, "Error processing password", http.StatusInternalServerError)
        return
    }

    // Save the user to the database
    err = saveUserToDatabase(username, hashedPassword)
    if err != nil {
        http.Error(w, "Error saving user", http.StatusInternalServerError)
        return
    }

    // Respond to the client
    w.WriteHeader(http.StatusCreated)
    fmt.Fprintln(w, "User registered successfully")
}

// sanitizeInput sanitizes user input to prevent XSS
func sanitizeInput(input string) string {
    // Basic sanitization to escape special characters
    return html.EscapeString(input)
}

// hashPassword hashes the password using bcrypt
func hashPassword(password string) (string, error) {
    hashedBytes, err := bcrypt.GenerateFromPassword([]byte(password), bcrypt.DefaultCost)
    if err != nil {
        return "", err
    }
    return string(hashedBytes), nil
}

// saveUserToDatabase saves the user to the database securely
func saveUserToDatabase(username, hashedPassword string) error {
    // Use parameterized queries to prevent SQL injection
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/dbname")
    if err != nil {
        return err
    }
    defer db.Close()

    query := "INSERT INTO users (username, password) VALUES (?, ?)"
    _, err = db.Exec(query, username, hashedPassword)
    return err
}
