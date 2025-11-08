package main

import (
	"context"
	"crypto/rand"
	"strings"
	"crypto/sha256"
	"fmt"
	"net/http"
	"encoding/base64"
	"os"
	"strconv"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v4"
	"golang.org/x/crypto/argon2"
)

func main() {
	http.HandleFunc("/check_mod", checkModHandler)
	http.ListenAndServe(":8080", nil)
}

func checkModHandler(w http.ResponseWriter, r *http.Request) {
	sessionToken := r.Header.Get("X-Session-Token")

	if sessionToken == "" {
		http.Error(w, "Missing session token", http.StatusUnauthorized)
		return
	}

	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		http.Error(w, "Database configuration error", http.StatusInternalServerError)
		return
	}

	conn, err := pgx.Connect(context.Background(), dbURL)
	if err != nil {
		http.Error(w, "Database connection failed", http.StatusInternalServerError)
		return
	}
	defer conn.Close(context.Background())

	username, err := getUsernameFromSession(conn, sessionToken)
	if err != nil {
		http.Error(w, "Invalid session token", http.StatusUnauthorized)
		return
	}

	isMod, err := isUserMod(conn, username)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if isMod {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintln(w, "true")
	} else {
		w.WriteHeader(http.StatusOK)
		fmt.Fprintln(w, "false")
	}

	logSecureEvent(fmt.Sprintf("Check mod status for user: %s, isMod: %v", username, isMod))
}

func getUsernameFromSession(conn *pgx.Conn, sessionToken string) (string, error) {
	var username string
	query := `SELECT username FROM sessions WHERE token = $1 AND expiry > NOW()`
	err := conn.QueryRow(context.Background(), query, sessionToken).Scan(&username)
	if err != nil {
		return "", err
	}
	return username, nil
}

func isUserMod(conn *pgx.Conn, username string) (bool, error) {
	var isMod bool
	query := `SELECT is_moderator FROM users WHERE username = $1`
	err := conn.QueryRow(context.Background(), query, username).Scan(&isMod)
	if err != nil {
		return false, err
	}
	return isMod, nil
}

func logSecureEvent(message string) {
	fmt.Printf("Secure Log: %s\n", message)
}