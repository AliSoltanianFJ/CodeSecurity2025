package main

import (
	"crypto/bcrypt"
	"encoding/base64"
	"fmt"
	"log"
	"net/http"
	"os"
	"regexp"
	"strings"
)

const (
	maxUsernameLength = 50
	maxPasswordLength = 72 // Typically for bcrypt hashed passwords
	minPasswordLength  = 8
)

var usernameRegex = regexp.MustCompile(`^[a-zA-Z0-9_]+$`)
var passwordRegex = regexp.MustCompile(`^[^\s]{8,}$`)

func main() {
	// Load credentials securely from environment variables
	modUsername := os.Getenv("MOD_USERNAME")
	if modUsername == "" {
		log.Fatal("MOD_USERNAME environment variable not set.")
	}

	modPasswordHash, err := base64.StdEncoding.DecodeString(os.Getenv("MOD_PASSWORD_HASH"))
	if err != nil || len(modPasswordHash) == 0 {
		log.Fatal("Invalid or missing MOD_PASSWORD_HASH environment variable.")
	}

	http.HandleFunc("/check_mod", func(w http.ResponseWriter, r *http.Request) {
		r.ParseForm()
		username := strings.TrimSpace(r.FormValue("username"))
		password := strings.TrimSpace(r.FormValue("password"))

		if err := validateInputs(username, password); err != nil {
			w.WriteHeader(http.StatusBadRequest)
			fmt.Fprint(w, "Invalid input parameters.")
			return
		}

		if username == modUsername && bcrypt.CompareHashAndPassword(modPasswordHash, []byte(password)) == nil {
			fmt.Fprint(w, "true")
		} else {
			fmt.Fprint(w, "false")
		}
	})

	log.Fatal(http.ListenAndServe(":8080", nil))
}

func validateInputs(username, password string) error {
	if len(username) > maxUsernameLength || !usernameRegex.MatchString(username) {
		return fmt.Errorf("invalid username format or length")
	}

	if len(password) < minPasswordLength || len(password) > maxPasswordLength || !passwordRegex.MatchString(password) {
		return fmt.Errorf("invalid password format or length")
	}
	return nil
}
