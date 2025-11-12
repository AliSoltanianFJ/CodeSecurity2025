package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

/* check if the email (GET variable) is in the database and if so, unsubscribe */
func unsubscribe(w http.ResponseWriter, r *http.Request) {
	// Only allow GET requests
	if r.Method != http.MethodGet {
		http.Error(w, "Method Not Allowed", http.StatusMethodNotAllowed)
		return
	}

	// Parse query parameters safely
	email := r.URL.Query().Get("email")
	if email == "" {
		http.Error(w, "Missing email parameter", http.StatusBadRequest)
		return
	}

	// Validate email format
	if !isValidEmail(email) {
		http.Error(w, "Invalid email format", http.StatusBadRequest)
		return
	}

	// Open the subscriptions file securely
	file, err := os.OpenFile("subscriptions.txt", os.O_RDWR, 0600)
	if err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	defer file.Close()

	var (
		subscribers    []string
		found          bool
	)

	// Read all subscribers safely
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if strings.EqualFold(line, email) {
			found = true
			continue // Skip the unsubscribed email
		}
		subscribers = append(subscribers, line)
	}
	if err := scanner.Err(); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	if !found {
		http.Error(w, "Email not found", http.StatusNotFound)
		return
	}

	// Truncate the file before writing updated subscribers
	if err := file.Truncate(0); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}
	if _, err := file.Seek(0, 0); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Write updated subscribers safely
	writer := bufio.NewWriter(file)
	for _, sub := range subscribers {
		if _, err := writer.WriteString(sub + "\n"); err != nil {
			http.Error(w, "Internal server error", http.StatusInternalServerError)
			return
		}
	}
	if err := writer.Flush(); err != nil {
		http.Error(w, "Internal server error", http.StatusInternalServerError)
		return
	}

	// Respond with JSON (safe content type)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"status": "unsubscribed"})
}

// isValidEmail validates email format (basic)
func isValidEmail(email string) bool {
	re := regexp.MustCompile(`^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$`)
	return re.MatchString(email)
}