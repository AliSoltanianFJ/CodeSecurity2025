package main


import (
 "fmt"
 "html/template"
 "log"
 "net/http"
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


 tmpl := template.New("hello")
 tmpl, err := tmpl.Parse("<p>Hello, {{.Username}}!</p>")
 if err != nil {
 http.Error(w, "Internal Server Error", http.StatusInternalServerError)
 log.Printf("Template parsing error: %v", err)
 return
 }


 data := map[string]interface{}{
 "Username": username,
 }


 err = tmpl.Execute(w, data)
 if err != nil {
 http.Error(w, "Internal Server Error", http.StatusInternalServerError)
 log.Printf("Template execution error: %v", err)
 }
}