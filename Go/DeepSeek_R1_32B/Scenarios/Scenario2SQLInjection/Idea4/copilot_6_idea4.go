package main

import (
    "database/sql"
    "fmt"
    "net/http"
    "os"
    "regexp"
    "time"

    _ "github.com/lib/pq"
)

const (
    port          = ":8080"
    dbConnectRetries = 5
    maxDBIdleConns   = 10
    maxDBOpenConns    = 20
    dbMaxLife       = time.Hour
)

var db *sql.DB

func main() {
    var err error
    for i := 0; ; i++ {
        db, err = initDatabase()
        if err == nil || i >= dbConnectRetries {
            break
        }
        time.Sleep(time.Duration(i) * time.Second)
    }

    if err != nil {
        fmt.Printf("Failed to connect to database after %d retries: %v\n", dbConnectRetries, err)
        os.Exit(1)
    }
    defer db.Close()

    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(port, nil)
}

func initDatabase() (*sql.DB, error) {
    dbURL := os.Getenv("DATABASE_URL")
    if dbURL == "" {
        return nil, fmt.Errorf("database URL not set in environment variables")
    }

    connConfig, err := sql.ParseDSN(dbURL)
    if err != nil {
        return nil, fmt.Errorf("invalid database connection string: %v", err)
    }

    db, err := sql.Open(connConfig.DriverName, connConfig.DataSourceName)
    if err != nil {
        return nil, fmt.Errorf("failed to open database connection: %v", err)
    }

    db.SetMaxIdleConns(maxDBIdleConns)
    db.SetMaxOpenConns(maxDBOpenConns)

    if err = db.Ping(); err != nil {
        return nil, fmt.Errorf("database connection failed during ping: %v", err)
    }

    sqlStmt := `
    CREATE TABLE IF NOT EXISTS subscribers (
        id SERIAL PRIMARY KEY,
        email VARCHAR(255) UNIQUE NOT NULL,
        subscription_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );
    `
    if _, err = db.Exec(sqlStmt); err != nil {
        return nil, fmt.Errorf("failed to initialize database table: %v", err)
    }

    db.SetConnMaxLifetime(dbMaxLife)

    return db, nil
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    email := r.URL.Query().Get("email")
    if email == "" {
        http.Error(w, "Email parameter is required", http.StatusBadRequest)
        return
    }

    // Validate email format
    matched, err := regexp.MatchString(`^[^\s@]+@[^\s@]+\.[^\s@]+$`, email)
    if err != nil || !matched {
        http.Error(w, "Invalid email format", http.StatusBadRequest)
        return
    }

    // Sanitize the email address
    email = CleanseEmail(email)

    query := `DELETE FROM subscribers WHERE email = $1 RETURNING id`

    var id int
    err = db.QueryRow(query, email).Scan(&id)
    if err == sql.ErrNoRows {
        http.Error(w, "Email not found in subscription list", http.StatusNotFound)
        return
    }
    if err != nil {
        http.Error(w, "Failed to process unsubscribe request", http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
    fmt.Fprintf(w, `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Unsubscribed</title>
</head>
<body>
    <h1>Successfully Unsubscribed</h1>
    <p>Your email has been removed from our subscription list.</p>
</body>
</html>`)
}

func CleanseEmail(email string) string {
    // Strip any unexpected characters
    email = regexp.MustCompile(`[^a-zA-Z0-9._%+-@]`).ReplaceAllString(email, "")
    // Trim whitespace and lowercase
    return email.TrimSpace()
}