package main

import (
	"fmt"
	"io"
	"net/http"
	"strings"

	"github.com/ghodss/yaml" // Using a well-maintained YAML library
)

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

// apiHandler securely deserializes the incoming YAML payload and returns the field "name".
func apiHandler(w http.ResponseWriter, r *http.Request) {
	yamlPayload := r.URL.Query().Get("payload")

	if yamlPayload == "" {
		http.Error(w, "Payload is required", http.StatusBadRequest)
		return
	}

	// 1. Input Validation: Check payload size to prevent excessive resource consumption.
	if len(yamlPayload) > 4096 { // Limit payload size to 4KB
		http.Error(w, "Payload too large", http.StatusBadRequest)
		return
	}

	// 2. Secure Deserialization: Define a strict struct for expected YAML format.
	type Payload struct {
		Name string `json:"name"`
	}

	var payload Payload

	// 3. YAML Decoding: Use a secure YAML library and handle errors properly.
	err := yaml.Unmarshal([]byte(yamlPayload), &payload)
	if err != nil {
		http.Error(w, "Invalid YAML format", http.StatusBadRequest)
		// Secure logging (do NOT include the payload in logs).
		fmt.Println("Error unmarshaling YAML:", err) // Ideally, use a secure logging mechanism.
		return
	}

	// 4. Output Encoding:  Encode the 'name' field to prevent XSS.
	name := payload.Name
	//name = html.EscapeString(name) //Deprecated and insecure

	// 5. Response: Return the extracted name.
	w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	w.WriteHeader(http.StatusOK)
	_, err = io.WriteString(w, name)
	if err != nil {
		fmt.Println("Error writing response:", err) // Secure Logging
		return
	}
}