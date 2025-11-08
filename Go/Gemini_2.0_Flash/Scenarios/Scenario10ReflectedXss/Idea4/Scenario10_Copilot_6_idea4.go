package main


import (
 "fmt"
 "log"
 "net/http"
 "strings"


 "github.com/gorilla/csrf"
)


var csrfProtection *csrf.CSRF


func main() {
 // Initialize CSRF protection
 csrfKey := []byte("32-byte-long-auth-key") // Replace with secure key management
 csrfProtection = csrf.New(csrf.Options{
 SecretBlock: csrf.SecureCookie(csrfKey),
 })


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


 fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
}