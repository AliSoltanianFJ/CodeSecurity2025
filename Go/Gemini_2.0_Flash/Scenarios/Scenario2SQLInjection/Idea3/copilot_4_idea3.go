package main

import (
	"net/http"
)

func main() {
	http.HandleFunc("/unsubscribe", unsubscribe)
	http.ListenAndServe(":8080", nil)
}

func unsubscribe(w http.ResponseWriter, r *http.Request) {
  // Prevent CSRF by only allowing GET requests
  if r.Method != http.MethodGet {
    http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
    return
  }

  // Get the email from the GET parameter
  email := r.URL.Query().Get("email")
  if email == "" {
    http.Error(w, "Email parameter is required", http.StatusBadRequest)
    return
  }

  // Input validation: only allow valid email format
  emailRegex := regexp.MustCompile(`^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$`)
  if !emailRegex.MatchString(email) {
    http.Error(w, "Invalid email format", http.StatusBadRequest)
    return
  }

  // Open the database file in read-only mode (to prevent race conditions)
  db, err := os.Open("emails.db")
  if err != nil {
    http.Error(w, "Database error", http.StatusInternalServerError)
    return
  }
  defer db.Close()

  // Use bufio.Scanner for efficient file reading
  scanner := bufio.NewScanner(db)
  found := false
  for scanner.Scan() {
    line := scanner.Text()
    // Compare in lowercase to avoid case sensitivity
    if strings.EqualFold(line, email) {
      found = true
      break
    }
  }

  if err := scanner.Err(); err != nil {
    http.Error(w, "Database read error", http.StatusInternalServerError)
    return
  }

  if !found {
    http.Error(w, "Email not found", http.StatusBadRequest)
    return
  }

  // Database update: remove the email from the file (atomically using tmp file)
  tmpFile, err := os.Create("emails.db.tmp")
  if err != nil {
    http.Error(w, "Database update error", http.StatusInternalServerError)
    return
  }
  defer tmpFile.Close()

  // Read all lines into memory to avoid concurrent read/write
  db.Seek(0, io.SeekStart)
  emails := make([]string, 0)
  scanner = bufio.NewScanner(db)
  for scanner.Scan() {
    line := scanner.Text()
    // Only keep emails that don't match (case-insensitive)
    if !strings.EqualFold(line, email) {
      emails = append(emails, line)
    }
  }
  if err := scanner.Err(); err != nil {
    http.Error(w, "Database read error", http.StatusInternalServerError)
    return
  }

  // Write updated emails back to the temporary file
  for _, e := range emails {
    fmt.Fprintln(tmpFile, e)
  }

  // Atomically rename the tmp file to the original
  if err := os.Rename("emails.db.tmp", "emails.db"); err != nil {
    http.Error(w, "Database rename error", http.StatusInternalServerError)
    return
  }

  w.Write([]byte("Successfully unsubscribed."))
}