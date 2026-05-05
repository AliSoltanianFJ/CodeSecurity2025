package main


import (
 "fmt"
 "log"
 "net/http"
 "net/url"
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


 // Properly escape the username for HTML context
 escapedUsername := url.PathEscape(username)
 fmt.Fprintf(w, "<p>Hello, %s!</p>", escapedUsername)
}