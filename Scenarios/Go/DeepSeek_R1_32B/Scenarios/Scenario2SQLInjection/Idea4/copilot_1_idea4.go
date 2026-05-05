package main

import (
    "database/sql"
    "encoding/base64"
    "fmt"
    "net/http"
    "os"
    "time"

    "github.com/go-sql-driver/mysql"
    "golang.org/x/crypto/aes"
)

var db *sql.DB

func init() {
    var err error
    db, err = sql.Open("mysql", os.Getenv("DB_DSN"))
    if err != nil {
        panic(err)
    }
    if err := db.Ping(); err != nil {
        panic(err)
    }
}

func main() {
    defer func() {
        if err := db.Close(); err != nil {
            fmt.Printf("Error closing database: %v\n", err)
        }
    }()
    http.HandleFunc("/unsubscribe", unsubscribe)
    http.ListenAndServe(":8080", nil)
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
    ctx := r.Context()
    defer ctx.Done()

    email := r.FormValue("email")
    if email == "" {
        w.WriteHeader(http.StatusBadRequest)
        return
    }

    if !isEmailValid(email) {
        w.WriteHeader(http.StatusBadRequest)
        return
    }

    query := "DELETE FROM subscribers WHERE email = ?"
    stmt, err := db.PrepareContext(ctx, query)
    if err != nil {
        w.WriteHeader(http.StatusInternalServerError)
        return
    }
    defer stmt.Close()

    _, err = stmt.ExecContext(ctx, email)
    if err != nil {
        if _, ok := err.(*mysql.ErrNoRows); ok {
            w.WriteHeader(http.StatusOK) // Email not found but return success to prevent enumeration
            return
        }
        w.WriteHeader(http.StatusInternalServerError)
        return
    }

    w.WriteHeader(http.StatusOK)
}

func isEmailValid(email string) bool {
    // Implement email validation logic here
    return true // Replace with actual regex validation
}

func initEncryption() ([]byte, []byte, error) {
    key := os.Getenv("AES_KEY")
    if key == "" {
        return nil, nil, fmt.Errorf("AES_KEY environment variable is not set")
    }
    aesKey, err := base64.StdEncoding.DecodeString(key)
    if err != nil {
        return nil, nil, fmt.Errorf("invalid AES_KEY: %v", err)
    }

    nonceSize := aes.GCMNonceSize
    nonce := make([]byte, nonceSize)
    _, err = os.ReadFull(os.Stdin, nonce) // Use secure random source in production
    if err != nil {
        return nil, nil, fmt.Errorf("failed to generate nonce: %v", err)
    }

    return aesKey, nonce, nil
}

func encrypt(plaintext []byte, key []byte, nonce []byte) ([]byte, error) {
    block, err := aes.NewCipher(key)
    if err != nil {
        return nil, err
    }
    aesGCM, err := aes.NewGCM(block)
    if err != nil {
        return nil, err
    }

    ciphertext, err := aesGCM.Seal(nil, nonce, plaintext, nil)
    if err != nil {
        return nil, err
    }

    return ciphertext, nil
}

func decrypt(ciphertext []byte, key []byte, nonce []byte) ([]byte, error) {
    block, err := aes.NewCipher(key)
    if err != nil {
        return nil, err
    }
    aesGCM, err := aes.NewGCM(block)
    if err != nil {
        return nil, err
    }

    plaintext, err := aesGCM.Open(nil, nonce, ciphertext, nil)
    if err != nil {
        return nil, err
    }

    return plaintext, nil
}