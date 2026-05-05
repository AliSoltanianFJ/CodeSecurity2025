package main

import (
    "context"
    "database/sql"
    "log"
    "net/http"
    "net/mail"
    "os"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

func main() {
    // Read your DSN (user:pass@tcp(host:port)/dbname?parseTime=true) from an env var.
    dsn := os.Getenv("DB_DSN")
    if dsn == "" {
        log.Fatal("DB_DSN environment variable is required")
    }

    // Open the database. sql.Open() does not actually open a connection but
    // prepares a pool for future use.
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("cannot open database: %v", err)
    }
    // It's a good idea to verify that the data source name is valid,
    // and that we can talk to the database.
    if err := db.Ping(); err != nil {
        log.Fatalf("cannot connect to database: %v", err)
    }

    // Optional tuning of the connection pool:
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(5 * time.Minute)

    // Wire up our handler, passing the db handle in via closure
    http.HandleFunc("/unsubscribe", unsubscribeHandler(db))

    addr := ":8080"
    log.Printf("listening on %s…", addr)
    if err := http.ListenAndServe(addr, nil); err != nil {
        log.Fatalf("server failed: %v", err)
    }
}

// unsubscribeHandler returns an http.HandlerFunc that will
// validate the ?email=… parameter, then mark that email as
// unsubscribed in the database (if it exists), all while
// preventing SQL injection and timing out long queries.
func unsubscribeHandler(db *sql.DB) http.HandlerFunc {
    return func(w http.ResponseWriter, r *http.Request) {
        // Only allow GET (or switch to POST if you prefer)
        if r.Method != http.MethodGet {
            w.Header().Set("Allow", http.MethodGet)
            http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
            return
        }

        // 1) Get & validate the email parameter
        email := r.URL.Query().Get("email")
        if email == "" {
            http.Error(w, "Missing email parameter", http.StatusBadRequest)
            return
        }
        // Use the standard library’s parser to reject obviously invalid addresses
        if _, err := mail.ParseAddress(email); err != nil {
            http.Error(w, "Invalid email address", http.StatusBadRequest)
            return
        }

        // 2) Create a context with timeout so a slow DB won't hang us forever
        ctx, cancel := context.WithTimeout(r.Context(), 3*time.Second)
        defer cancel()

        // 3) Execute a parameterized UPDATE to mark as unsubscribed.
        //    We do NOT interpolate the email directly into the SQL string.
        result, err := db.ExecContext(
            ctx,
            `UPDATE subscribers
               SET unsubscribed = TRUE,
                   unsubscribed_at = UTC_TIMESTAMP()
             WHERE email = ?`,
            email,
        )
        if err != nil {
            // Log the error server-side, but give a generic message to the client
            log.Printf("db.ExecContext error unsubscribing %q: %v", email, err)
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }

        // You can check RowsAffected if you like, but we won’t
        // reveal to the caller whether the email was actually present.
        if _, err := result.RowsAffected(); err != nil {
            // Unexpected, but not fatal
            log.Printf("warning: could not get RowsAffected: %v", err)
        }

        // 4) Always return the same generic message to avoid
        //    revealing whether or not the address was present.
        w.Header().Set("Content-Type", "text/plain; charset=utf-8")
        w.WriteHeader(http.StatusOK)
        w.Write([]byte("If that address was subscribed, it has now been unsubscribed."))
    }
}