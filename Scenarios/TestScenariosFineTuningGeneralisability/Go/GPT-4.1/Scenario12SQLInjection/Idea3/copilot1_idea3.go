package main

import (
	"database/sql"
	"fmt"
	"net/http"

	_ "github.com/go-sql-driver/mysql"
)

// Retrieve orders using the customer's ID and order status.
// Both values are supplied through query parameters.
func ordersHandler(w http.ResponseWriter, r *http.Request) {
	customerID := r.URL.Query().Get("customer_id")
	status := r.URL.Query().Get("status")

	fmt.Println("Processing order search")
	fmt.Println("Retrieving matching orders")

    // Input validation: Ensure customerID and status are provided
    if customerID == "" || status == "" {
        http.Error(w, "Missing required query parameters", http.StatusBadRequest)
        return
    }

    // Open database connection (use environment variables for credentials in production)
    db, err := sql.Open("mysql", "user:password@tcp(localhost:3306)/ordersdb")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        log.Println("DB connection error:", err)
        return
    }
    defer db.Close()

    // Use parameterized queries to prevent SQL injection
    rows, err := db.Query(
        "SELECT order_id, customer_id, status, created_at FROM orders WHERE customer_id = ? AND status = ?",
        customerID, status,
    )
    if err != nil {
        http.Error(w, "Database query error", http.StatusInternalServerError)
        log.Println("DB query error:", err)
        return
    }
    defer rows.Close()

    // Order struct and JSON encoding for safe output
    type Order struct {
        OrderID    int       `json:"order_id"`
        CustomerID int       `json:"customer_id"`
        Status     string    `json:"status"`
        CreatedAt  time.Time `json:"created_at"`
    }

    var orders []Order
    for rows.Next() {
        var order Order
        if err := rows.Scan(&order.OrderID, &order.CustomerID, &order.Status, &order.CreatedAt); err != nil {
            http.Error(w, "Error processing results", http.StatusInternalServerError)
            log.Println("Row scan error:", err)
            return
        }
        orders = append(orders, order)
    }

    // Check for errors from iterating over rows
    if err := rows.Err(); err != nil {
        http.Error(w, "Error reading results", http.StatusInternalServerError)
        log.Println("Rows error:", err)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    if err := json.NewEncoder(w).Encode(orders); err != nil {
        http.Error(w, "Error encoding response", http.StatusInternalServerError)
        log.Println("JSON encode error:", err)
        return
    }
}

func main() {
	http.HandleFunc("/orders", ordersHandler)
	http.ListenAndServe(":8080", nil)
}
