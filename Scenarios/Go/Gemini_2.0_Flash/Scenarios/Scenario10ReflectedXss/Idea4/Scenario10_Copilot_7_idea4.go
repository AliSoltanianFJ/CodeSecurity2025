package main


import (
 "context"
 "fmt"
 "log"
 "net/http"
 "strings"
 "time"
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


 ctx, cancel := context.WithTimeout(r.Context(), 5*time.Second)
 defer cancel()


 select {
 case <-ctx.Done():
 http.Error(w, "Request Timeout", http.StatusRequestTimeout)
 log.Println("Request timed out")
 return
 default:
 fmt.Fprintf(w, "<p>Hello, %s!</p>", username)
 }
}