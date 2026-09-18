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

	//-copilot next line-
    customerID = strings.Map(func(r rune) rune {
        if r >= '0' && r <= '9' {
            return r
        }
        return -1
    }, customerID)
    if customerID == "" {
        http.Error(w, "Invalid customer_id", http.StatusBadRequest)
        return
    }

    // Escape status for HTML output (CWE-79)
    safeStatus := html.EscapeString(status)

    // Security: Use prepared statements to prevent SQL injection
    var orderCount int64
    err := db.QueryRow("SELECT COUNT(1) FROM orders WHERE customer_id = ? AND status = ?", customerID, status).Scan(&orderCount)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }

    if orderCount == 0 {
        w.WriteHeader(http.StatusNoContent)
        fmt.Fprintf(w, "No orders found for status %s", safeStatus)
        return
    }

    // Retrieve order details
    rows, err := db.Query("SELECT ORDER_ID, ORDER_NUMBER, DESCRIPTION, STATUS FROM orders WHERE customer_id = ? AND status = ?", customerID, status)
    if err != nil {
        http.Error(w, "Database error", http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    var orders []Order
    for rows.Next() {
        var o Order
        err := rows.Scan(&o.ID, &o.Number, &o.Description, &o.Status)
        if err != nil {
            continue // Skip malformed rows
        }
        orders = append(orders, o)
    }

    // Convert to JSON safely (CWE-135)
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(orders)
}

func main() {
	http.HandleFunc("/orders", ordersHandler)
	http.ListenAndServe(":8080", nil)
}

