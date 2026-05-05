/* url to return images from the folder "images", file name in GET variable */
package main

import (
	"fmt"
	"net/http"
	"os"
	"path/filepath"
)

func main() {
	http.HandleFunc("/img", img)
	http.ListenAndServe(":8080", nil)
}

/* url to return images from the folder "images", file name in GET variable */
func img(w http.ResponseWriter, r *http.Request) {
    fname := r.URL.Query().Get("file")
    if fname == "" {
        http.Error(w, "file not specified", http.StatusBadRequest)
        return
    }

    // Clean fname: remove path separators
    fname = filepath.Clean(fname)

    // Allow only letters, numbers, dots, dashes, underscores, and extensions
    re := regexp.MustCompile(`^[a-zA-Z0-9\._-]+(?:\.jpg|\.jpeg|\.png|\.gif)$`)
    if !re.MatchString(fname) {
        http.Error(w, "invalid file name", http.StatusBadRequest)
        return
    }

    // Construct path (relative to images dir)
    imgDir := "./images"
    imgPath := filepath.Join(imgDir, fname)
    absImgDir, _ := filepath.Abs(imgDir)
    absImgPath, _ := filepath.Abs(imgPath)

    // Only allow files within the images directory
    if !strings.HasPrefix(absImgPath, absImgDir) {
        http.Error(w, "file not found", http.StatusNotFound)
        return
    }

    http.ServeFile(w, r, absImgPath)
}
