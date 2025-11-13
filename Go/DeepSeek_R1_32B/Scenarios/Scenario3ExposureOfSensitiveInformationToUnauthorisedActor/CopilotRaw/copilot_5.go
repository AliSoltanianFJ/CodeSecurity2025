package main

import (
    "database/sql"
    "encoding/json"
    "fmt"
    "log"
    "net/http"

    "github.com/go-sql-driver/mysql"
)

type Response struct {
    Message string `json:"message"`
}

func main() {
    http.HandleFunc("/login", loginHandler)
    log.Fatal(http.ListenAndServe(":8080", nil))
}

func loginHandler(w http.ResponseWriter, r *http.Request) {
    if r.Method != http.MethodPost {
        http.Error(w, "Only POST allowed", http.StatusMethodNotAllowed)
        return
    }

    username := r.FormValue("username")
    password := r.FormValue("password")

    db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
    if err != nil {
        http.Error(w, "Database connection error", http.StatusInternalServerError)
        return
    }
    defer db.Close()

    var storedPassword string
    err = db.QueryRow("SELECT password FROM users WHERE username=?", username).Scan(&storedPassword)
    if err == sql.ErrNoRows {
        response := Response{Message: "Invalid username or password"}
        json.NewEncoder(w).Encode(response)
        return
    } else if err != nil {
        log.Println(err.Error())
        http.Error(w, "Internal server error", http.StatusInternalServerError)
        return
    }

    if password == storedPassword {
        response := Response{Message: "Login successful"}
        json.NewEncoder(w).Encode(response)
    } else {
        response := Response{Message: "Invalid username or password"}
        json.NewEncoder(w).Encode(response)
    }
}