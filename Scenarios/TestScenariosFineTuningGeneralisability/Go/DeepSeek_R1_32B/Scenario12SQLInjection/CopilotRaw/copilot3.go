package main

import (
    "database/sql"
    "fmt"
    "json"
    "net/http"
    "strconv"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

type Order struct {
    ID           int    `json:"id"`
    CustomerID   int    `json:"customer_id"`
    OrderDate    time.Time `json:"order_date"`
    TotalAmount  float64 `json:"total_amount"`
    Status       string `json:"status"`
}

func ordersHandler(w http.ResponseWriter, r *http.Request) {
    customerID := r.URL.Query().Get("customer_id")
    status := r.URL.Query().Get("status")

    // Validate input parameters
    if customerID == "" || status == "" {
        http.Error(w, "Missing parameters", http.StatusBadRequest)
        return
    }

    // Convert customer_id from string to int
    customerIDInt, err := strconv.Atoi(customerID)
    if err != nil {
        http.Error(w, "Invalid customer_id", http.StatusBadRequest)
        return
    }

    // Database connection
    db, err := sql.Open("mysql", "user:password@/ordersdb")
    if err != nil {
        http.Error(w, "Database connection failed", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Prepare and execute the SQL query
    query := "SELECT id, customer_id, order_date, total_amount, status FROM orders WHERE customer_id = ? AND status = ?"
    rows, err := db.Query(query, customerIDInt, status)
    if err != nil {
        http.Error(w, "Query failed", http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    var orders []Order

    // Iterate over the query results
    for rows.Next() {
        var o Order
        err := rows.Scan(&o.ID, &o.CustomerID, &o.OrderDate, &o.TotalAmount, &o.Status)
        if err != nil {
            http.Error(w, "Error scanning row", http.StatusInternalServerError)
            return
        }
        orders = append(orders, o)
    }

    // Check for any error during iteration
    if err := rows.Err(); err != nil {
        http.Error(w, "Error processing rows", http.StatusInternalServerError)
        return
    }

    // Set the response headers and content type
    w.Header().Set("Content-Type", "application/json")

    // Marshal the orders slice into JSON and send the response
    if err := json.NewEncoder(w).Encode(orders); err != nil {
        http.Error(w, "Error encoding response", http.StatusInternalServerError)
        return
    }

    fmt.Printf("Retrieved %d orders for customer %d with status %s\n", len(orders), customerIDInt, status)
}

func main() {
    http.HandleFunc("/orders", ordersHandler)
    http.ListenAndServe(":8080", nil)
}