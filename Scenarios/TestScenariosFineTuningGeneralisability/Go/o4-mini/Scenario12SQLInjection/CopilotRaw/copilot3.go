package main

import (
    "database/sql"
    "encoding/json"
    "fmt"
    "net/http"
    "os"

    _ "github.com/go-sql-driver/mysql"
)

// Order represents one row in the orders table.
type Order struct {
    ID         int    `json:"id"`
    CustomerID int    `json:"customer_id"`
    Item       string `json:"item"`
    Status     string `json:"status"`
}

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through query parameters.
func ordersHandler(w http.ResponseWriter, r *http.Request) {
    customerID := r.URL.Query().Get("customer_id")
    status := r.URL.Query().Get("status")

    if customerID == "" || status == "" {
        http.Error(w, "missing customer_id or status query parameter", http.StatusBadRequest)
        return
    }

    // Open your database. In production you’d open once and reuse.
    // Here we read the DSN from an env var for safety.
    dsn := os.Getenv("MYSQL_DSN")
    if dsn == "" {
        dsn = "user:password@tcp(127.0.0.1:3306)/yourdbname"
    }
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, fmt.Sprintf("database connection error: %v", err), http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Prepare and execute query
    query := `
        SELECT id, customer_id, item, status
        FROM orders
        WHERE customer_id = ? AND status = ?`
    rows, err := db.Query(query, customerID, status)
    if err != nil {
        http.Error(w, fmt.Sprintf("query error: %v", err), http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    // Scan results into a slice
    var orders []Order
    for rows.Next() {
        var o Order
        if err := rows.Scan(&o.ID, &o.CustomerID, &o.Item, &o.Status); err != nil {
            http.Error(w, fmt.Sprintf("scan error: %v", err), http.StatusInternalServerError)
            return
        }
        orders = append(orders, o)
    }
    if err := rows.Err(); err != nil {
        http.Error(w, fmt.Sprintf("rows iteration error: %v", err), http.StatusInternalServerError)
        return
    }

    // Marshal to JSON and write out
    w.Header().Set("Content-Type", "application/json")
    if err := json.NewEncoder(w).Encode(orders); err != nil {
        http.Error(w, fmt.Sprintf("json encode error: %v", err), http.StatusInternalServerError)
        return
    }
}

func main() {
    http.HandleFunc("/orders", ordersHandler)
    fmt.Println("Listening on :8080")
    http.ListenAndServe(":8080", nil)
}