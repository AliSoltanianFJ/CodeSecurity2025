package main

import (
    "database/sql"
    "encoding/json"
    "fmt"
    "log"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

// Order represents one row in the "orders" table
type Order struct {
    ID          int     `json:"id"`
    CustomerID  int     `json:"customer_id"`
    Status      string  `json:"status"`
    TotalAmount float64 `json:"total_amount"`
}

var db *sql.DB

func init() {
    var err error
    // Adjust the DSN below to your username, password, hostname, port and database name.
    // We include parseTime=true in case you have DATETIME/TIMESTAMP columns.
    dsn := "user:password@tcp(127.0.0.1:3306)/dbname?parseTime=true"
    db, err = sql.Open("mysql", dsn)
    if err != nil {
        log.Fatalf("error opening database: %v", err)
    }
    // Optional but recommended:
    db.SetMaxOpenConns(25)
    db.SetMaxIdleConns(25)
    db.SetConnMaxLifetime(0)

    // Verify that the data source name is correct.
    if err = db.Ping(); err != nil {
        log.Fatalf("error pinging database: %v", err)
    }
}

func ordersHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Read & validate query parameters
    customerID := r.URL.Query().Get("customer_id")
    status := r.URL.Query().Get("status")

    if customerID == "" || status == "" {
        http.Error(w, "Missing required query parameters: customer_id and status", http.StatusBadRequest)
        return
    }

    fmt.Println("Processing order search")
    fmt.Println("Retrieving matching orders")

    // 2) Prepare and execute a parameterized query
    const q = `
        SELECT
            id,
            customer_id,
            status,
            total_amount
        FROM orders
        WHERE customer_id = ? AND status = ?
    `
    rows, err := db.Query(q, customerID, status)
    if err != nil {
        log.Printf("query error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    // 3) Loop through the result set
    var orders []Order
    for rows.Next() {
        var o Order
        if err := rows.Scan(&o.ID, &o.CustomerID, &o.Status, &o.TotalAmount); err != nil {
            log.Printf("scan error: %v", err)
            http.Error(w, "Internal server error", http.StatusInternalServerError)
            return
        }
        orders = append(orders, o)
    }
    if err := rows.Err(); err != nil {
        log.Printf("rows iteration error: %v", err)
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    // 4) Marshal to JSON and write out
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    if err := json.NewEncoder(w).Encode(orders); err != nil {
        log.Printf("json encode error: %v", err)
        // Note: at this point headers are already written, so we can't change the status code.
    }
}

func main() {
    http.HandleFunc("/orders", ordersHandler)
    log.Println("Listening on :8080")
    log.Fatal(http.ListenAndServe(":8080", nil))
}