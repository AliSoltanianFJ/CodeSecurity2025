package main

import (
    "database/sql"
    "encoding/json"
    "fmt"
    "net/http"
    "strconv"
    _ "github.com/go-sql-driver/mysql"
)

type Order struct {
    ID       int     `json:"id"`
    Customer string   `json:"customer"`
    Status   string   `json:"status"`
    Amount   float64  `json:"amount"`
    Date     string   `json:"date"`
}

func ordersHandler(w http.ResponseWriter, r *http.Request) {
    customerID := r.URL.Query().Get("customer_id")
    status := r.URL.Query().Get("status")

    if customerID == "" {
        http.Error(w, "Missing customer_id parameter", http.StatusBadRequest)
        return
    }

    // Parse customer_id to integer
    customerId, err := strconv.Atoi(customerID)
    if err != nil {
        http.Error(w, "Invalid customer_id", http.StatusBadRequest)
        return
    }

    // Open database connection
    db, err := sql.Open("mysql", "root:password@tcp(localhost:3306)/ordersdb")
    if err != nil {
        http.Error(w, "Error opening database", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    // Build SQL query based on status presence
    var query string
    var args []any

    if status == "" {
        query = "SELECT * FROM orders WHERE customer_id = ?"
        args = []any{customerId}
    } else {
        query = "SELECT * FROM orders WHERE customer_id = ? AND status = ?"
        args = []any{customerId, status}
    }

    // Prepare statement
    stmt, err := db.Prepare(query)
    if err != nil {
        http.Error(w, "Error preparing statement", http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    // Execute query
    rows, err := stmt.Query(args...)
    if err != nil {
        http.Error(w, "Error executing query", http.StatusInternalServerError)
        return
    }
    defer rows.Close()

    // Collect results
    var orders []Order
    for rows.Next() {
        var id int
        var customer string
        var orderStatus string
        var amount float64
        var date string

        err := rows.Scan(&id, &customer, &orderStatus, &amount, &date)
        if err != nil {
            http.Error(w, "Error scanning rows", http.StatusInternalServerError)
            return
        }

        orders = append(orders, Order{
            ID:       id,
            Customer: customer,
            Status:   orderStatus,
            Amount:   amount,
            Date:     date,
        })
    }

    // Check for errors after query iteration
    if err := rows.Err(); err != nil {
        http.Error(w, "Error after query iteration", http.StatusInternalServerError)
        return
    }

    // Marshal to JSON
    jsonBytes, err := json.Marshal(orders)
    if err != nil {
        http.Error(w, "Error marshaling results to JSON", http.StatusInternalServerError)
        return
    }

    // Set content type and write response
    w.Header().Set("Content-Type", "application/json")
    w.Write(jsonBytes)
}

func main() {
    http.HandleFunc("/orders", ordersHandler)
    http.ListenAndServe(":8080", nil)
}