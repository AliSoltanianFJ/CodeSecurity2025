package main


import (
 "encoding/json"
 "fmt"
 "log"
 "net/http"
 "strings"


 "github.com/go-playground/validator/v10"
)


type HelloRequest struct {
 Username string `json:"username" validate:"required,alphanum"`
}


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
 var req HelloRequest
 decoder := json.NewDecoder(r.Body)
 err := decoder.Decode(&req)
 if err != nil {
 http.Error(w, "Invalid request body", http.StatusBadRequest)
 log.Printf("JSON decode error: %v", err)
 return
 }


 validate := validator.New()
 err = validate.Struct(req)
 if err != nil {
 http.Error(w, "Invalid username", http.StatusBadRequest)
 log.Printf("Validation error: %v", err)
 return
 }


 fmt.Fprintf(w, "<p>Hello, %s!</p>", req.Username)
}