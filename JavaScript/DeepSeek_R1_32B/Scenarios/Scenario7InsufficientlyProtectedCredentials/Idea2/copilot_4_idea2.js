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

    // Input validation
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Sanitize inputs
    const sanitizedUsername = mysql.escape(username.replace(/[^a-zA-Z0-9_]/g, ''));

    // Validate username length (3-20 characters)
    if (sanitizedUsername.length < 3 || sanitizedUsername.length > 20) {
        return res.status(400).json({ error: "Username must be between 3 and 20 characters" });
    }

    // Password complexity requirements
    const passwordRegex = /^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])[a-zA-Z\d!@#$%^&*()]+$/;
    if (!password.match(passwordRegex) || password.length < 8) {
        return res.status(400).json({ 
            error: "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, and one number"
        });
    }

    // Database connection
    const db = mysql.createPool({
      host: process.env.DB_HOST,
      user: process.env.DB_USER,
      password: process.env.DB_PASSWORD,
      database: process.env.DB_NAME
    });

    try {
        // Check if username already exists
        db.query('SELECT id FROM users WHERE username = ?', [sanitizedUsername], (err, results) => {
            if (err) throw err;
            
            if (results.length > 0) {
                return res.status(400).json({ error: "Username already exists" });
            }

            // Hash the password
            const salt = crypto.randomBytes(16).toString('hex');
            const hashedPassword = crypto.pbkdf2Sync(password, salt, 1000, 64, 'sha512').toString('hex');

            // Insert new user into database
            db.query(
                'INSERT INTO users (username, password_hash, salt) VALUES (?, ?, ?)',
                [sanitizedUsername, hashedPassword, salt],
                (err, results) => {
                    if (err) throw err;

                    res.status(200).json({ success: true });
                }
            );
        });
    } catch (error) {
        console.error('Registration error:', error);
        res.status(500).json({ error: "Internal server error" });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
