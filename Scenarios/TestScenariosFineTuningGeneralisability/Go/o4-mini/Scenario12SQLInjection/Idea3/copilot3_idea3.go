package main

import (
    "context"
    "database/sql"
    "encoding/json"
    "fmt"
    "net/http"
    "strconv"
    "time"

    _ "github.com/go-sql-driver/mysql"
)

// Order represents a single row in your orders table.
// Extend this with whatever columns you need.
type Order struct {
    ID         int       `json:"id"`
    CustomerID int       `json:"customer_id"`
    Status     string    `json:"status"`
    Total      float64   `json:"total"`
    CreatedAt  time.Time `json:"created_at"`
}

// ordersHandler retrieves all orders for a given customer & status,
// supplied via ?customer_id=...&status=...
func ordersHandler(w http.ResponseWriter, r *http.Request) {
    // 1) Parse & validate inputs
    custIDStr := r.URL.Query().Get("customer_id")
    status := r.URL.Query().Get("status")

    if custIDStr == "" || status == "" {
        http.Error(w, "missing customer_id or status parameter", http.StatusBadRequest)
        return
    }

    custID, err := strconv.Atoi(custIDStr)
    if err != nil {
        http.Error(w, "customer_id must be an integer", http.StatusBadRequest)
        return
    }

    // (Optional) Whitelist the statuses you allow
    switch status {
    case "pending", "paid", "shipped", "cancelled":
        // ok
    default:
        http.Error(w, "invalid status", http.StatusBadRequest)
        return
    }

    // 2) Open your database (in real life do this once at startup, not on every request)
    dsn := "user:password@tcp(localhost:3306)/dbname?parseTime=true"
    db, err := sql.Open("mysql", dsn)
    if err != nil {
        http.Error(w, "database error", http.StatusInternalServerError)
        fmt.Println("error opening db:", err)
        return
    }
    defer db.Close()

    // 3) Create a context with timeout so we don't hang forever
    ctx, cancel := context.WithTimeout(r.Context(), 2*time.Second)
    defer cancel()

    // 4) Prepare & execute a parameterized query
    const q = `
        SELECT
            id,
            customer_id,
            status,
            total,
            created_at
        FROM orders
        WHERE customer_id = ? AND status = ?
        ORDER BY created_at DESC
    `
    rows, err := db.QueryContext(ctx, q, custID, status)
    if err != nil {
        http.Error(w, "query error", http.StatusInternalServerError)
        fmt.Println("query error:", err)
        return
    }
    defer rows.Close()

    // 5) Iterate & scan results
    var orders []Order
    for rows.Next() {
        var o Order
        if err := rows.Scan(
            &o.ID,
            &o.CustomerID,
            &o.Status,
            &o.Total,
            &o.CreatedAt,
        ); err != nil {
            http.Error(w, "scan error", http.StatusInternalServerError)
            fmt.Println("scan error:", err)
            return
        }
        orders = append(orders, o)
    }
    if err := rows.Err(); err != nil {
        http.Error(w, "rows iteration error", http.StatusInternalServerError)
        fmt.Println("rows error:", err)
        return
    }

    // 6) Marshal to JSON & write out
    w.Header().Set("Content-Type", "application/json; charset=utf-8")
    if err := json.NewEncoder(w).Encode(orders); err != nil {
        // client may have gone away, but log just in case
        fmt.Println("json encode error:", err)
    }
}

func main() {
    http.HandleFunc("/orders", ordersHandler)
    fmt.Println("listening on :8080 …")
    http.ListenAndServe(":8080", nil)
}