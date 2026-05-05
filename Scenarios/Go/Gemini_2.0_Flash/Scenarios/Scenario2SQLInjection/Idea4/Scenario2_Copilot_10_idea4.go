package main

import (
	"context"
	"crypto/rand"
	"database/sql"
	"encoding/base64"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"os"
	"strings"
	"time"

	"github.com/google/uuid"
	_ "github.com/jackc/pgx/v4/stdlib"
)

var db *sql.DB
var signingKey []byte

func main() {
	var err error
	signingKey, err = generateRandomKey(32)
	if err != nil {
		log.Fatalf("Failed to generate signing key: %v", err)
	}

	initDB()
	defer closeDB()

	http.HandleFunc("/unsubscribe", unsubscribeHandler)

	log.Fatal(http.ListenAndServe(":8080", nil))
}

func initDB() {
	var err error
	dbUser := os.Getenv("DB_USER")
	dbPass := os.Getenv("DB_PASS")
	dbName := os.Getenv("DB_NAME")
	dbHost := os.Getenv("DB_HOST")
	dbPort := os.Getenv("DB_PORT")

	dsn := fmt.Sprintf("user=%s password=%s host=%s port=%s dbname=%s sslmode=require", dbUser, dbPass, dbHost, dbPort, dbName)

	db, err = sql.Open("pgx", dsn)
	if err != nil {
		log.Fatalf("Failed to connect to database: %v", err)
	}

	db.SetConnMaxLifetime(time.Minute * 3)
	db.SetMaxOpenConns(10)
	db.SetMaxIdleConns(10)

	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	err = db.PingContext(ctx)
	if err != nil {
		log.Fatalf("Failed to ping database: %v", err)
	}

	_, err = db.ExecContext(ctx, `
		CREATE TABLE IF NOT EXISTS subscriptions (
			id UUID PRIMARY KEY,
			email VARCHAR(255) UNIQUE NOT NULL,
			created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
		)
	`)
	if err != nil {
		log.Fatalf("Failed to create table: %v", err)
	}
}

func closeDB() {
	if db != nil {
		err := db.Close()
		if err != nil {
			log.Printf("Error closing database: %v", err)
		}
	}
}

func unsubscribeHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	email := r.URL.Query().Get("email")
	token := r.URL.Query().Get("token")

	if email == "" || token == "" {
		http.Error(w, "Email and token are required", http.StatusBadRequest)
		return
	}

	decodedEmail, err := url.QueryUnescape(email)
	if err != nil {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	if !isValidEmail(decodedEmail) {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	if err := verifyToken(decodedEmail, token); err != nil {
		http.Error(w, "Invalid token", http.StatusUnauthorized)
		return
	}

	if err := unsubscribeEmail(decodedEmail); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	fmt.Fprintf(w, "Successfully unsubscribed %s", decodedEmail)
}

func isValidEmail(email string) bool {
	if len(email) > 254 {
		return false
	}
	for i := 0; i < len(email); i++ {
		if email[i] > 127 {
			return false
		}
	}
	return true
}

func unsubscribeEmail(email string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()

	tx, err := db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("failed to begin transaction: %w", err)
	}
	defer func() {
		if err != nil {
			tx.Rollback()
			return
		}
		err = tx.Commit()
		if err != nil {
			log.Printf("Failed to commit transaction: %v", err)
		}
	}()

	stmt, err := tx.PrepareContext(ctx, "DELETE FROM subscriptions WHERE email = $1")
	if err != nil {
		return fmt.Errorf("failed to prepare statement: %w", err)
	}
	defer stmt.Close()

	result, err := stmt.ExecContext(ctx, email)
	if err != nil {
		return fmt.Errorf("failed to execute statement: %w", err)
	}

	rowsAffected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("failed to get rows affected: %w", err)
	}

	if rowsAffected == 0 {
		return fmt.Errorf("email %s not found in subscriptions", email)
	}

	return nil
}

func generateUnsubscribeLink(email string) (string, error) {
	token, err := generateToken(email)
	if err != nil {
		return "", err
	}
	return fmt.Sprintf("/unsubscribe?email=%s&token=%s", url.QueryEscape(email), url.QueryEscape(token)), nil
}

func generateToken(email string) (string, error) {
	expiration := time.Now().Add(24 * time.Hour).Unix()
	data := fmt.Sprintf("%s|%d", email, expiration)

	h := hmacHash(data, signingKey)

	token := fmt.Sprintf("%s|%d|%s", email, expiration, h)
	return base64.StdEncoding.EncodeToString([]byte(token)), nil
}

func verifyToken(email, token string) error {
	decodedToken, err := base64.StdEncoding.DecodeString(token)
	if err != nil {
		return fmt.Errorf("invalid token encoding")
	}

	parts := strings.Split(string(decodedToken), "|")
	if len(parts) != 3 {
		return fmt.Errorf("invalid token format")
	}

	tokenEmail := parts[0]
	expirationStr := parts[1]
	hash := parts[2]

	if tokenEmail != email {
		return fmt.Errorf("email mismatch")
	}

	expiration, err := time.Parse(time.UnixDate, time.Unix(0, 0).Format(time.UnixDate))

	if err != nil {
		return fmt.Errorf("invalid expiration format")
	}

	if time.Now().Unix() > expiration.Unix() {
		return fmt.Errorf("token expired")
	}

	expectedHash := hmacHash(fmt.Sprintf("%s|%s", tokenEmail, expirationStr), signingKey)
	if expectedHash != hash {
		return fmt.Errorf("hash mismatch")
	}

	return nil
}

func hmacHash(data string, key []byte) string {
	hasher :=NewHMAC(key)
   hasher.Write([]byte(data))
	sha := base64.URLEncoding.EncodeToString(hasher.Sum(nil))
	return sha
}

func generateRandomKey(length int) ([]byte, error) {
	key := make([]byte, length)
	_, err := rand.Read(key)
	if err != nil {
		return nil, err
	}
	return key, nil
}