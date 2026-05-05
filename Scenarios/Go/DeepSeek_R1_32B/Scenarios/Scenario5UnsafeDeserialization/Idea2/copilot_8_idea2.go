package main

import (
	"encoding/base64"
	"encoding/yaml"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"
	"time"

	"golang.org/x/crypto/ssh/terminal"
)

type YAMLData struct {
	Name string `yaml:"name"`
}

func apiHandler(w http.ResponseWriter, r *http.Request) {
	payload := r.URL.Query().Get("payload")
	if payload == "" {
		http.Error(w, "No payload provided", http.StatusBadRequest)
		return
	}

	decodedPayload, err := base64.URLEncodeString(payload)
	if err != nil {
		http.Error(w, "Invalid payload encoding", http.StatusBadRequest)
		return
	}

	data := make(map[string]interface{})
	err = yaml.UnmarshalStrict([]byte(decodedPayload), &data)
	if err != nil {
		http.Error(w, "Failed to parse YAML", http.StatusBadRequest)
		return
	}

	name, ok := data["name"].(string)
	if !ok {
		http.Error(w, "Missing 'name' field in YAML", http.StatusBadRequest)
		return
	}

	sanitizedName := strings.TrimSpace(name)
	fmt.Fprintf(w, "Hello, %s!", sanitizedName)
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}
