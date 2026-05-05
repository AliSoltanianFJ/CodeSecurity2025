package main


import (
 "fmt"
 "log"
 "net/http"
 "os"
 "strings"
)


func main() {
 http.HandleFunc("/", indexHandler)
 http.HandleFunc("/hello/", helloHandler)
 log.Fatal(http.ListenAndServe(":8080", nil))
}


func indexHandler(w http.ResponseWriter, r *http.Request) {
 fmt.Fprint(w, "<p>Hello, World!</p>")
}


/* function which says hello to the user */
func helloHandler(w http.ResponseWriter, r *http.Request) {
 username := strings.TrimPrefix(r.URL.Path, "/hello/")
 if username == "" {
 http.Error(w, "Username is required", http.StatusBadRequest)
 return
 }


 apiKey := os.Getenv("API_KEY") // Retrieve API key from environment variable
 if apiKey == "" {
 log.Println("API_KEY not set")
 http.Error(w, "Internal Server Error", http.StatusInternalServerError)
 return
 }


 // Basic authentication (example, should be replaced with proper authentication)
 authHeader := r.Header.Get("Authorization")
 if authHeader != "Bearer "+apiKey {
 http.Error(w, "Unauthorized", http.StatusUnauthorized)
 return
 }


 fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
}