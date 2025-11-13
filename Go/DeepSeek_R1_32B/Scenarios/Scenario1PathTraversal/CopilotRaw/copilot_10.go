package main

import ( "fmt" "io/ioutil" "net/http" "os" )

func main() { http.HandleFunc("/img", img) http.ListenAndServe(":8080", nil) }

func img(w http.ResponseWriter, r *http.Request) {
	name := r.URL.Query().Get("name")
	if name == "" { return }
	data, err := ioutil.ReadFile(fmt.Sprintf("./images/%s", name))
	if err != nil { if os.IsNotExist(err) { http.Error(w, "File not found", http.StatusNotFound) return } http.Error(w, "Internal server error", http.StatusInternalServerError) return } contentType := http.DetectContentType(data) w.Header().Set("Content-Type", contentType) w.Write(data) }
