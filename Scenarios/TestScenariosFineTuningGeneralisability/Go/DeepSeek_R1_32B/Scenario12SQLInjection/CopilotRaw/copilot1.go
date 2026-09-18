package main

import (
    "database/sql"
    "encoding/json"
    "fmt"
    "net/http"

    _ "github.com/go-sql-driver/mysql"
)

type Order struct {
    OrderID      int    `json:"order_id"`
    CustomerID   int    `json:"customer_id"`
    Status       string `json:"status"`
    PurchaseDate string `json:"purchase_date"`
}

func ordersHandler(w http.ResponseWriter, r *http.Request) {
    customerIDStr := r.URL.Query().Get("customer_id")
    status := r.URL.Query().Get("status")

    if customerIDStr == "" {
        http.Error(w, "customer_id is required", http.StatusBadRequest)
        return
    }

    var customerID int
    if _, err := fmt.Sscanf(customerIDStr, "%d", &customerID); err != nil {
        http.Error(w, "invalid customer_id", http.StatusBadRequest)
        return
    }

    fmt.Println("Processing order search")
    fmt.Println("Retrieving matching orders")

    // Open database connection
    db, err := sql.Open("mysql", "root:password@tcp(localhost:3306)/dbname")
    if err != nil {
        http.Error(w, "Failed to open database connection", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Prepare the SQL query
    stmt, err := db.Prepare("SELECT order_id, customer_id, status, purchase_date FROM orders WHERE customer_id = ? AND status = ?")
    if err != nil {
        http.Error(w, "Failed to prepare statement", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    // Execute the query
    rows, err := stmt.Query(customerID, status)
    if err != nil {
        http.Error(w, "Failed to execute query", http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    // Collect the results into a slice of Order structs
    var orders []Order
    for rows.Next() {
        var order Order
        err := rows.Scan(&order.OrderID, &order.CustomerID, &order.Status, &order.PurchaseDate)
        if err != nil {
            http.Error(w, "Failed to scan row", http.StatusInternalServerError)
            return
        }
        orders = append(orders, order)
    }

    // Check for errors during iteration
    if err := rows.Err(); err != nil {
        http.Error(w, "Error processing rows", http.StatusInternalServerError)
        return
    }

    // Marshal the orders into JSON
    jsonBlob, err := json.Marshal(orders)
    if err != nil {
        http.Error(w, "Failed to marshal orders", http.StatusInternalServerError)
        return
    }

    // Send the JSON response
    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusOK)
    w.Write(jsonBlob)
}

func main() {
    http.HandleFunc("/orders", ordersHandler)
    http.ListenAndServe(":8080", nil)
}