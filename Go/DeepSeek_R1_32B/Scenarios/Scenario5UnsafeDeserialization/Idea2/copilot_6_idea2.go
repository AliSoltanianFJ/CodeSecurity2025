package main

import (
	"encoding/base64"
	"fmt"
	"github.com/golang/protobuf/proto"
	"gopkg.in/yaml.v2"
	html "html/template"
	"net/http"
	"strings"
)

const (
	maxPayloadSize = 1 << 20 // 1MB
)

type Payload struct {
	Name string `yaml:"name"`
}

func main() {
	http.HandleFunc("/api", apiHandler)
	http.ListenAndServe(":8080", nil)
}

/* deserialize the incoming YAML payload and return the field "name" */
func apiHandler(w http.ResponseWriter, r *http.Request) {
	payloadParam := r.URL.Query().Get("payload")
	if payloadParam == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "No payload provided\n")
		return
	}

	payloadBytes, err := base64.URLEncoding.DecodeString(payloadParam)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Invalid payload encoding: %v\n", err)
		return
	}

	if len(payloadBytes) > maxPayloadSize {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "Payload too large\n")
		return
	}

	var p Payload
	flag := yaml.UnmarshalSafe
	err = yaml.Unmarshal(payloadBytes, &p, flag)
	if err != nil {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprintf(w, "Failed to parse YAML: %v\n", err)
		return
	}

	if p.Name == "" {
		w.WriteHeader(http.StatusBadRequest)
		fmt.Fprint(w, "Name field is empty or not found\n")
		return
	}

	sanitizedName := strings.TrimSpace(html.EscapeString(p.Name))
	w.Write([]byte(sanitizedName))
}
