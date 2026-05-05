const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require("bcryptjs"); // Make sure to install this package

const app = express();
app.use(express.urlencoded({ extended: true }));

// Security middleware
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  next();
});

/**
 * register URL
 */
app.post("/register", async (req, res) => {
    try {
        const username = req.body.username;
        const password = req.body.password;

        // Input validation
        if (!username || !password) {
            return res.status(400).json({ error: "Username and password are required" });
        }

        // Sanitize input to prevent XSS and other injection attacks
        const sanitizedUsername = sanitizeInput(username);
        
        // Password requirements:
        // - Minimum 8 characters
        // - Maximum 50 characters
        if (password.length < 8 || password.length > 50) {
            return res.status(400).json({ error: "Password must be between 8 and 50 characters" });
        }

        // Sanitize password to remove any potentially harmful characters
        const sanitizedPassword = sanitizeInput(password);

        // Generate salt and hash password
        const saltRounds = 12;
        const hashedPassword = await bcrypt.hash(sanitizedPassword, saltRounds);

        // Database connection pool (make sure to replace with your actual credentials)
        const dbPool = mysql.createPool({
            host: 'localhost',
            user: 'your_database_user',
            password: 'your_database_password',
            database: 'your_database_name'
        });

        // Prepare SQL statement using prepared statements
        const query = `INSERT INTO users (username, password_hash) VALUES (?, ?)`;

        dbPool.getConnection((err, connection) => {
            if (err) {
                console.error('Database connection error:', err);
                return res.status(500).json({ error: "Internal server error" });
            }

            try {
                // Execute query with sanitized values
                connection.query(query, [sanitizedUsername, hashedPassword], (error, results) => {
                    if (error) {
                        console.error('Database query error:', error);
                        return res.status(500).json({ error: "Internal server error" });
                    }

                    // Check for duplicate username
                    if (error.code === 'ER_DUP_ENTRY') {
                        return res.status(409).json({ error: "Username already exists" });
                    }

                    res.status(201).json({ success: true, message: "User registered successfully!" });
                });
            } catch (err) {
                console.error('Query execution error:', err);
                res.status(500).json({ error: "Internal server error" });
            } finally {
                connection.release();
            }
        });

    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ error: "Internal server error" });
    }
});

// Input sanitization function
function sanitizeInput(input) {
    // Strip any potentially harmful characters
    return input.replace(/[^a-zA-Z0-9_\-\.]/g, '');
}

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

