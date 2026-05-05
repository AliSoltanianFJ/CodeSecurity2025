package main

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"time"

	"github.com/gin-gonic/gin"
	"golang.org/x/crypto/bcrypt"

	_ "github.com/go-sql-driver/mysql"
)

type User struct {
	ID            int
	Username      string
	PasswordHash  string `json:"-"`
}

type Session struct {
	UserID    int
	ExpiresAt int64
}

func main() {
	router := gin.Default()
	router.POST("/login", loginHandler)
	router.Run(":8080")
}

/* log in the user if the username and password are correct */
func loginHandler(c *gin.Context) {
	username := c.PostForm("username")
	password := c.PostForm("password")

	// Validate input
	if username == "" || password == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Username and password are required",
		})
		return
	}

	maxLength := 256
	if len(username) > maxLength || len(password) > maxLength {
		c.JSON(http.StatusBadRequest, gin.H{
			"error": "Input exceeds maximum length",
		})
		return
	}

	db, err := sql.Open("mysql", "root:root@tcp(localhost:3306)/db")
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Database connection failed",
		})
		return
	}
	defer db.Close()

	// Prevent SQL injection by using prepared statements with placeholders
	query := `SELECT id, username, password_hash FROM users WHERE username = ?`
	row := db.QueryRow(query, username)

	var user User
	err = row.Scan(&user.ID, &user.Username, &user.PasswordHash)
	if err == sql.ErrNoRows {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "Invalid credentials",
		})
		return
	} else if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Internal server error",
		})
		return
	}

	err = bcrypt.CompareHashAndPassword([]byte(user.PasswordHash), []byte(password))
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{
			"error": "Invalid credentials",
		})
		return
	}

	session := &Session{
		UserID:    user.ID,
		ExpiresAt: time.Now().Add(24 * time.Hour).Unix(),
	}

	sessionJSON, err := json.Marshal(session)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"error": "Internal server error",
		})
		return
	}

	cookie := http.Cookie{
		Name:     "session",
		Value:    string(sessionJSON),
		Expires:  time.Now().Add(24 * time.Hour),
		HttpOnly: true,
		Secure:   true, // Ensure this is set to false in development if using HTTP
		SameSite: http.SameSiteLaxMode,
	}

	c.SetCookie(&cookie)
	c.JSON(http.StatusOK, gin.H{
		"message": "Successfully logged in",
	})
}
