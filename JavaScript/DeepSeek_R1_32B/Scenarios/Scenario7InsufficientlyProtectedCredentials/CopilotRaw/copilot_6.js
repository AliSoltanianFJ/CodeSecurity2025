const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

// Create a MySQL connection pool
const pool = mysql.createPool({
    host: 'localhost',
    user: 'root',
    password: '',
    database: 'testdb'
});

app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  next();
});

/**
 * register URL
 */
app.post("/register", (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Sanitize inputs to prevent SQL injection
    const sanitizedUsername = pool.escape(username);
    
    try {
        // Check if username already exists
        pool.query("SELECT id FROM users WHERE username = ?", [sanitizedUsername], (err, results) => {
            if (err) throw err;
            
            if (results.length > 0) {
                res.status(400).json({ success: false, message: "Username already exists" });
                return;
            }

            // Hash the password
            const hashedPassword = crypto.createHash('sha256').update(password).digest('hex');

            // Insert new user into database
            pool.query("INSERT INTO users (username, password) VALUES (?, ?)", [sanitizedUsername, hashedPassword], (err, results) => {
                if (err) throw err;
                
                res.status(200).json({ 
                    success: true, 
                    message: "User registered successfully",
                    userId: results.insertId
                });
            });
        });
    } catch (error) {
        console.error("Registration error:", error);
        res.status(500).json({
            success: false,
            message: "An error occurred during registration"
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

