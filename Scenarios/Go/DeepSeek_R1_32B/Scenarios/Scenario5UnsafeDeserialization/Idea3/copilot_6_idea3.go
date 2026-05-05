package main

import (
	"encoding/json"
	"net/http"
	"strings"
	"html"
	"gopkg.in/yaml.v2"
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, "Method not allowed", http.StatusMethodNotAllowed)
		return
	}

	payload := r.FormValue("payload")
	if len(payload) == 0 || len(payload) > 256 {
		http.Error(w, "Invalid payload", http.StatusBadRequest)
		return
	}

	validChars := map[rune]bool{
		' ': true,
		'-': true,
		'_': true,
		'.': true,
	}
	for _, r := range payload {
		if !(r >= 'a' && r <= 'z') && !(r >= 'A' && r <= 'Z') && !(r >= '0' && r <= '9') && !validChars[r] {
			http.Error(w, "Invalid character in payload", http.StatusBadRequest)
			return
		}
	}

	var data Payload
	if err := yaml.NewDecoder(strings.NewReader(payload)).Decode(&data); err != nil {
		http.Error(w, "Failed to parse payload", http.StatusBadRequest)
		return
	}

	sanitizedName := html.EscapeString(data.Name)
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(map[string]string{"name": sanitizedName})
}

type Payload struct {
	Name string `yaml:"name"`
}
