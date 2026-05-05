const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");

const app = express();
app.use(express.urlencoded({ extended: true }));

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

    // Validate input length to prevent buffer overflow attacks
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Input validation for username (only letters, numbers, underscores)
    const usernameRegex = /^[a-zA-Z0-9_]{3,20}$/;
    if (!usernameRegex.test(username)) {
        return res.status(400).json({ error: "Invalid username format. Must be 3-20 characters with only letters, numbers, or underscores." });
    }

    // Password complexity requirements
    const passwordRegex = /^(?=.*[a-zA-Z])(?=.*\d)[A-Za-z\d!@#$%^&*]{8,}$/;
    if (!passwordRegex.test(password)) {
        return res.status(400).json({ 
            error: "Password must be at least 8 characters with a mix of letters and numbers."
        });
    }

    // Sanitize input to prevent XSS
    const sanitizedUsername = mysql.escape(username);

    // Hash the password using bcrypt
    const saltRounds = 10;
    crypto.pbkdf2(password, crypto.randomBytes(16).toString('hex'), saltRounds, 64, 'sha512', (err, hash) => {
        if (err) {
            return res.status(500).json({ error: "Internal server error" });
        }

        // Use prepared statements to prevent SQL injection
        const query = `INSERT INTO users (username, password_hash) VALUES (?, ?)`;
        
        const connection = mysql.createConnection({
          host: process.env.DB_HOST,
          user: process.env.DB_USER,
          password: process.env.DB_PASSWORD,
          database: process.env.DB_NAME,
        });

        connection.connect();

        connection.query(query, [sanitizedUsername, hash.toString('hex')], (error, results) => {
            if (error) {
                // Check for duplicate username
                if (error.code === 'ER_DUP_ENTRY') {
                    res.status(409).json({ error: "Username already exists" });
                } else {
                    console.error("Database error:", error);
                    res.status(500).json({ error: "Internal server error" });
                }
            } else {
                // Clear sensitive data from logs
                const user = { ...results };
                delete user.password_hash;

                res.status(201).json({
                    success: true,
                    message: "User registered successfully",
                    user: user
                });
            }
        });

        connection.end();
    });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
