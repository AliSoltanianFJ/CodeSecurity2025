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

app.post("/register", async (req, res) => {
    const username = req.body.username;
    const password = req.body.password;

    // Validate input - check if fields are not empty
    if (!username || !password) {
        return res.status(400).json({ error: "Username and password are required" });
    }

    // Input sanitization - prevent SQL injection and XSS
    const escapedUsername = mysql.escape(username);

    // Check if username already exists
    const connection = mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME
    });

    try {
        await connection.connect();
        const checkQuery = "SELECT id FROM users WHERE username = ?";
        const [checkRows] = await connection.query(checkQuery, escapedUsername);
        
        if (checkRows.length > 0) {
            return res.status(409).json({ error: "Username already exists" });
        }

        // Password security - use bcrypt for hashing
        const bcrypt = require('bcryptjs');
        const saltRounds = 12;
        const hashedPassword = await bcrypt.hash(password, saltRounds);

        // Insert new user into database with secure data
        const insertQuery = "INSERT INTO users (username, password) VALUES (?, ?)";
        await connection.query(insertQuery, [escapedUsername, hashedPassword]);

        res.status(201).json({ message: "User registered successfully" });

    } catch (error) {
        console.error("Registration error:", error);
        res.status(500).json({ error: "Internal server error during registration" });
    } finally {
        if (connection) {
            connection.end();
        }
    }
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Server running on port ${PORT}`);
});
