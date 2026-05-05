const express = require("express");
const mysql = require("mysql");
const crypto = require("crypto");
const bcrypt = require('bcrypt');

const app = express();
app.use(express.urlencoded({ extended: true }));

// Security headers
app.use((req, res, next) => {
  res.setHeader('X-Frame-Options', 'DENY');
  res.setHeader('Content-Security-Policy', "default-src 'self'");
  res.setHeader('X-XSS-Protection', '1; mode=block');
  next();
});

// Database connection pool
const db = mysql.createPool({
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    insecureAuth: false,
    multipleStatements: false
});

app.post("/register", async (req, res) => {
    try {
        const username = req.body.username;
        const password = req.body.password;

        // Validate input lengths
        if (!username || !password) {
            return res.status(400).json({ error: "Username and password are required" });
        }

        if (username.length < 3 || username.length > 20) {
            return res.status(400).json({ error: "Username must be between 3-20 characters" });
        }

        if (password.length < 8) {
            return res.status(400).json({ error: "Password must be at least 8 characters" });
        }

        // Sanitize username input
        const sanitizedUsername = db.escape(username);

        // Generate salt and hash password
        const saltRounds = 12;
        const salt = bcrypt.genSaltSync(saltRounds);
        const hashedPassword = await bcrypt.hash(password, salt);

        // Check if username already exists
        const checkQuery = "SELECT COUNT(*) as count FROM users WHERE username = ?";
        const [checkResult] = await new Promise((resolve, reject) => {
            db.query(checkQuery, [username], (err, results) => {
                if (err) reject(err);
                else resolve(results);
            });
        });

        if (checkResult.count > 0) {
            return res.status(409).json({ error: "Username already exists" });
        }

        // Insert new user
        const insertQuery = `
            INSERT INTO users (username, password_hash)
            VALUES (${sanitizedUsername}, ?)
        `;

        await new Promise((resolve, reject) => {
            db.query(insertQuery, [hashedPassword], (err, results) => {
                if (err) reject(err);
                else resolve(results);
            });
        });

        res.status(201).json({ success: true, message: "User registered successfully" });

    } catch (error) {
        console.error("Registration error:", error);
        res.status(500).json({ 
            error: "An error occurred during registration",
            // Never expose internal errors to the client
            details: process.env.NODE_ENV === 'development' ? error.message : undefined
        });
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});

